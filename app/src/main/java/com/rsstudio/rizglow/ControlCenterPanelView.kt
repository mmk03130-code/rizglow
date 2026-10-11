package com.rsstudio.rizglow

import android.app.NotificationManager
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

/** The fuzzy control-center panel UI, shared by the in-app Activity and the
 *  swipe-triggered system overlay. Hosts are responsible for adding/removing
 *  this view; tapping outside or "Close" calls [onDismiss]. */
/** @param compact true for the swipe overlay: pure controls only, no header,
 *  no setup pills, no mascot — like the big theme apps. False for in-app. */
class ControlCenterPanelView(
    context: Context,
    private val onDismiss: () -> Unit,
    private val compact: Boolean = false
) : LinearLayout(context) {

    private val audio: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val statusViews = mutableMapOf<String, TextView>()
    private var torchOn = false
    private var torchCamId: String? = null
    private var bubbleBtn: TextView? = null
    private var swipeBtn: TextView? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun canWrite() = Settings.System.canWrite(context)

    /** All startActivity calls funnel through here: NEW_TASK is required when
     *  the host is a Service, harmless when it's an Activity. */
    private fun launch(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor((if (compact) 0x99000000 else 0xCC000000).toInt())
        setPadding(dp(16), dp(if (compact) 24 else 48), dp(16), dp(if (compact) 24 else 48))
        setOnClickListener { onDismiss() } // tap outside dismisses

        val panel = LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundResource(R.drawable.cc_panel_bg)
            setPadding(dp(18), dp(18), dp(18), dp(10))
            setOnClickListener { /* consume: don't dismiss */ }
        }
        val scroll = ScrollView(context).apply {
            layoutParams = LayoutParams(dp(if (compact) 320 else 340), LayoutParams.WRAP_CONTENT)
            addView(panel)
        }
        addView(scroll)

        if (!compact) {
            panel.addView(headerRow())
            panel.addView(setupRow())
        }
        panel.addView(connectivityRow())
        panel.addView(musicCard())
        panel.addView(smallTilesRow())
        panel.addView(slidersRow())
        panel.addView(shortcutsRow())
        if (!compact) {
            val cat = ImageView(context).apply {
                setImageResource(R.drawable.cc_cat)
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(120)
                ).apply { topMargin = dp(4) }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            panel.addView(cat)
        }
    }

    /** Refresh tile states + setup buttons. Hosts call this when showing. */
    fun refresh() {
        refreshTiles()
        refreshSetupBtns()
    }

    private fun setupRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }
        val pill = { text: String, onClick: () -> Unit ->
            TextView(context).apply {
                this.text = text
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(dp(12), dp(8), dp(12), dp(8))
                gravity = Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = dp(20).toFloat()
                    setColor(0x55FFFFFF.toInt())
                }
                layoutParams = LayoutParams(0,
                    LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
                isClickable = true
                setOnClickListener { onClick() }
            }
        }
        row.addView(pill("Tile") { addTilePrompt() })
        val bb = pill("") { toggleBubble() }
        bubbleBtn = bb
        row.addView(bb)
        val sw = pill("") { toggleSwipe() }
        swipeBtn = sw
        (sw.layoutParams as LayoutParams).marginEnd = 0
        row.addView(sw)
        refreshSetupBtns()
        return row
    }

    private fun refreshSetupBtns() {
        bubbleBtn?.text = if (BubbleService.isRunning(context)) "Bubble on" else "Bubble off"
        val on = ControlCenterAccessService.isEnabled(context) &&
            ControlCenterAccessService.isSwipeOpen(context)
        swipeBtn?.text = if (on) "Swipe on" else "Swipe off"
    }

    private fun toggleSwipe() {
        if (!ControlCenterAccessService.isEnabled(context) ||
            !ControlCenterAccessService.isSwipeOpen(context)
        ) {
            launch(Intent(context, SwipeSetupActivity::class.java))
            onDismiss()
            return
        }
        ControlCenterAccessService.setSwipeOpen(context, false)
        Toast.makeText(context, "Swipe-to-open off", Toast.LENGTH_SHORT).show()
        refreshSetupBtns()
    }

    private fun toggleBubble() {
        if (BubbleService.isRunning(context)) {
            context.stopService(Intent(context, BubbleService::class.java))
        } else {
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "Allow \"Display over other apps\", then tap again",
                    Toast.LENGTH_LONG).show()
                launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                return
            }
            context.startService(Intent(context, BubbleService::class.java))
            Toast.makeText(context, "Floating cat is live — drag it anywhere, tap to open",
                Toast.LENGTH_LONG).show()
        }
        refreshSetupBtns()
    }

    private fun addTilePrompt() {
        if (Build.VERSION.SDK_INT >= 33) {
            val sbm = context.getSystemService(StatusBarManager::class.java)
            val cn = ComponentName(context, ControlCenterTileService::class.java)
            val icon = Icon.createWithResource(context, R.drawable.ic_launcher)
            sbm.requestAddTileService(cn, "Rizglow Panel", icon, context.mainExecutor) { result ->
                val msg = when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Tile added!"
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "Tile is already added"
                    else -> "Tile not added"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context,
                "Swipe down → edit tiles (pencil) → drag \"Rizglow Panel\" in",
                Toast.LENGTH_LONG).show()
        }
    }

    // ---------- builders ----------

    private fun headerRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        val title = TextView(context).apply {
            text = "Rizglow Panel"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            setShadowLayer(4f, 0f, 2f, 0x80000000.toInt())
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val close = TextView(context).apply {
            text = "Close"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            isClickable = true
            setOnClickListener { onDismiss() }
        }
        row.addView(title)
        row.addView(close)
        return row
    }

    /** Fuzzy purple tile with white label + status. */
    private fun tile(name: String, label: String, onClick: () -> Unit): LinearLayout {
        val t = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.cc_tile_bg)
            setPadding(dp(4), dp(10), dp(4), dp(10))
            layoutParams = LayoutParams(0, dp(if (compact) 70 else 86), 1f).apply {
                marginEnd = dp(10)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick(); refreshTiles() }
        }
        val tv = TextView(context).apply {
            text = label
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setShadowLayer(4f, 0f, 2f, 0x80000000.toInt())
        }
        val st = TextView(context).apply {
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setShadowLayer(4f, 0f, 2f, 0x80000000.toInt())
        }
        statusViews[name] = st
        t.addView(tv)
        t.addView(st)
        return t
    }

    private fun connectivityRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 0, 0, dp(14))
        }
        row.addView(tile("airplane", "Airplane") {
            launch(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        })
        row.addView(tile("data", "Data") {
            launch(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
        })
        row.addView(tile("wifi", "WiFi") {
            if (Build.VERSION.SDK_INT >= 29) launch(Intent(Settings.Panel.ACTION_WIFI))
            else launch(Intent(Settings.ACTION_WIFI_SETTINGS))
        })
        val last = tile("bt", "Bluetooth") {
            launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        (last.layoutParams as LayoutParams).marginEnd = 0
        row.addView(last)
        return row
    }

    private fun musicCard(): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.cc_card_bg)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        val note = TextView(context).apply {
            text = "\u266B"
            textSize = 22f
            setTextColor(0xFF6B4A8F.toInt())
            setPadding(0, 0, dp(8), 0)
        }
        card.addView(note)
        val label = TextView(context).apply {
            text = "Music"
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF6B4A8F.toInt())
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        card.addView(label)
        val btn = { text: String, key: Int ->
            TextView(context).apply {
                this.text = text
                textSize = 20f
                setTextColor(0xFF6B4A8F.toInt())
                setPadding(dp(14), dp(6), dp(14), dp(6))
                isClickable = true
                setOnClickListener { mediaKey(key) }
            }
        }
        card.addView(btn("|\u25C0", KeyEvent.KEYCODE_MEDIA_PREVIOUS))
        card.addView(btn("\u25B6", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        card.addView(btn("\u25B6|", KeyEvent.KEYCODE_MEDIA_NEXT))
        return card
    }

    private fun mediaKey(keyCode: Int) {
        try {
            val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val up = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audio.dispatchMediaKeyEvent(down)
            audio.dispatchMediaKeyEvent(up)
        } catch (e: Exception) {
            Toast.makeText(context, "No music app responding", Toast.LENGTH_SHORT).show()
        }
    }

    private fun smallTilesRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 0, 0, dp(14))
        }
        row.addView(tile("rotate", "Rotate") { toggleRotate() })
        row.addView(tile("silent", "Silent") { toggleSilent() })
        row.addView(tile("dnd", "Focus") { toggleDnd() })
        val last = tile("alerts", "Alerts") {
            if (!ControlCenterAccessService.openNotifications()) {
                Toast.makeText(context, "Turn \"Swipe\" on first", Toast.LENGTH_SHORT).show()
            } else {
                onDismiss()
            }
        }
        (last.layoutParams as LayoutParams).marginEnd = 0
        row.addView(last)
        return row
    }

    private fun slidersRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }
        row.addView(sliderCard("\u2600", "Brightness", 255,
            init = {
                try {
                    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                } catch (e: Exception) { 128 }
            },
            onChange = {
                if (canWrite()) {
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, it)
                } else {
                    launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                }
            }))
        val spacer = View(context).apply {
            layoutParams = LayoutParams(dp(12), 1)
        }
        row.addView(spacer)
        row.addView(sliderCard("\uD83D\uDD0A", "Volume", audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            init = { audio.getStreamVolume(AudioManager.STREAM_MUSIC) },
            onChange = { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }))
        return row
    }

    private fun sliderCard(icon: String, label: String, max: Int, init: () -> Int, onChange: (Int) -> Unit): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(R.drawable.cc_slider_bg)
            setPadding(dp(8), dp(12), dp(8), dp(12))
            layoutParams = LayoutParams(0, dp(190), 1f)
        }
        card.addView(TextView(context).apply {
            text = icon
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(2))
        })
        card.addView(TextView(context).apply {
            text = label
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF6B4A8F.toInt())
            gravity = Gravity.CENTER
        })
        val sb = SeekBar(context).apply {
            this.max = max
            progress = init().coerceIn(0, max)
            rotation = 270f
            layoutParams = LayoutParams(dp(150), dp(48)).apply {
                topMargin = dp(52)
            }
            progressTintList = ColorStateList.valueOf(0xFF9B6BD3.toInt())
            thumbTintList = ColorStateList.valueOf(0xFF7A4FB5.toInt())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) {
                    if (fromUser) onChange(v)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        card.addView(sb)
        return card
    }

    private fun shortcutsRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 0, 0, dp(6))
        }
        row.addView(tile("torch", "Torch") { setTorch(!torchOn) })
        row.addView(tile("camera", "Camera") {
            IconTargets.resolve(context, "Camera")?.let { launch(it) }
        })
        row.addView(tile("calc", "Calc") {
            IconTargets.resolve(context, "Calculator")?.let { launch(it) }
                ?: Toast.makeText(context, "No calculator found", Toast.LENGTH_SHORT).show()
        })
        val last = tile("settings", "Settings") {
            IconTargets.resolve(context, "Settings")?.let { launch(it) }
        }
        (last.layoutParams as LayoutParams).marginEnd = 0
        row.addView(last)
        return row
    }

    // ---------- actions ----------

    private fun setTorch(on: Boolean) {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = torchCamId ?: cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }?.also { torchCamId = it } ?: run {
                Toast.makeText(context, "No flash on this phone", Toast.LENGTH_SHORT).show()
                return
            }
            cm.setTorchMode(id, on)
            torchOn = on
        } catch (e: Exception) {
            Toast.makeText(context, "Torch unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleRotate() {
        if (!canWrite()) {
            launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
            return
        }
        val cur = try {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION)
        } catch (e: Exception) { 1 }
        Settings.System.putInt(
            context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (cur == 1) 0 else 1
        )
    }

    private fun toggleSilent() {
        audio.ringerMode = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL)
            AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
    }

    private fun toggleDnd() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            Toast.makeText(context, "Allow Do Not Disturb access, then tap again", Toast.LENGTH_LONG).show()
            return
        }
        nm.setInterruptionFilter(
            if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL)
                NotificationManager.INTERRUPTION_FILTER_NONE
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
    }

    private fun refreshTiles() {
        statusViews["torch"]?.text = if (torchOn) "On" else "Off"
        statusViews["silent"]?.text =
            if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) "Off" else "On"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        statusViews["dnd"]?.text =
            if (nm.isNotificationPolicyAccessGranted &&
                nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            ) "On" else "Off"
        if (canWrite()) {
            val rot = try {
                Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION)
            } catch (e: Exception) { 1 }
            statusViews["rotate"]?.text = if (rot == 1) "On" else "Off"
        } else {
            statusViews["rotate"]?.text = "Tap"
        }
        statusViews["airplane"]?.text = ""
        statusViews["data"]?.text = ""
        statusViews["wifi"]?.text = ""
        statusViews["bt"]?.text = ""
        statusViews["camera"]?.text = ""
        statusViews["calc"]?.text = ""
        statusViews["settings"]?.text = ""
        statusViews["alerts"]?.text = ""
    }
}

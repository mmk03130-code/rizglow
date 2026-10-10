package com.rsstudio.rizglow

import android.app.Activity
import android.app.NotificationManager
import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
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

/** Fuzzy floating control center (Themezy-style). Translucent activity so it
 *  floats over whatever the user was doing. Real toggles where Android allows:
 *  torch, brightness, volume, ringer, DND, rotation, music keys. Connectivity
 *  tiles open the system panels (like the reference apps do). */
class ControlCenterActivity : Activity() {

    private lateinit var audio: AudioManager
    private val statusViews = mutableMapOf<String, TextView>()
    private var torchOn = false
    private var torchCamId: String? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun canWrite() = Settings.System.canWrite(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ControlCenterGate.panelOpen = true
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xCC000000.toInt())
            setPadding(dp(16), dp(48), dp(16), dp(48))
            setOnClickListener { finish() } // tap outside dismisses
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.cc_panel_bg)
            setPadding(dp(18), dp(18), dp(18), dp(10))
            setOnClickListener { /* consume: don't dismiss */ }
        }
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(340), LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(panel)
        }
        root.addView(scroll)

        panel.addView(headerRow())
        panel.addView(setupRow())
        panel.addView(connectivityRow())
        panel.addView(musicCard())
        panel.addView(smallTilesRow())
        panel.addView(slidersRow())
        panel.addView(shortcutsRow())
        val cat = ImageView(this).apply {
            setImageResource(R.drawable.cc_cat)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(120)
            ).apply { topMargin = dp(4) }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        panel.addView(cat)

        setContentView(root)
        refreshTiles()
    }

    override fun onResume() {
        super.onResume()
        refreshTiles()
        refreshSetupBtns()
    }

    override fun onDestroy() {
        ControlCenterGate.panelOpen = false
        super.onDestroy()
    }

    private var bubbleBtn: TextView? = null
    private var swipeBtn: TextView? = null

    private fun setupRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(10))
        }
        val pill = { text: String, onClick: () -> Unit ->
            TextView(this).apply {
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
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
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
        (sw.layoutParams as LinearLayout.LayoutParams).marginEnd = 0
        row.addView(sw)
        refreshSetupBtns()
        return row
    }

    private fun refreshSetupBtns() {
        bubbleBtn?.text = if (BubbleService.isRunning(this)) "Bubble on" else "Bubble off"
        val on = ControlCenterAccessService.isEnabled(this) &&
            ControlCenterAccessService.isSwipeOpen(this)
        swipeBtn?.text = if (on) "Swipe on" else "Swipe off"
    }

    private fun toggleSwipe() {
        if (!ControlCenterAccessService.isEnabled(this)) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Swipe to open")
                .setMessage("To open the control center with a swipe down on the status bar, " +
                    "Rizglow uses an Accessibility Service. It only watches for that swipe " +
                    "gesture \u2014 it does not read your screen or collect any data.")
                .setPositiveButton("Allow") { _, _ ->
                    ControlCenterAccessService.setSwipeOpen(this, true)
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    Toast.makeText(this, "Turn on \"Rizglow Panel\", then come back",
                        Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Not now", null)
                .show()
            return
        }
        val cur = ControlCenterAccessService.isSwipeOpen(this)
        ControlCenterAccessService.setSwipeOpen(this, !cur)
        Toast.makeText(this,
            if (!cur) "Swipe down on the status bar to open the panel"
            else "Swipe-to-open off", Toast.LENGTH_SHORT).show()
        refreshSetupBtns()
    }


    private fun toggleBubble() {
        if (BubbleService.isRunning(this)) {
            stopService(Intent(this, BubbleService::class.java))
        } else {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Allow \"Display over other apps\", then tap again",
                    Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                return
            }
            startService(Intent(this, BubbleService::class.java))
            Toast.makeText(this, "Floating cat is live — drag it anywhere, tap to open",
                Toast.LENGTH_LONG).show()
        }
        refreshBubbleBtn()
    }

    private fun addTilePrompt() {
        if (Build.VERSION.SDK_INT >= 33) {
            val sbm = getSystemService(StatusBarManager::class.java)
            val cn = ComponentName(this, ControlCenterTileService::class.java)
            val icon = Icon.createWithResource(this, R.drawable.ic_launcher)
            sbm.requestAddTileService(cn, "Rizglow Panel", icon, mainExecutor) { result ->
                val msg = when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Tile added!"
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "Tile is already added"
                    else -> "Tile not added"
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this,
                "Swipe down → edit tiles (pencil) → drag \"Rizglow Panel\" in",
                Toast.LENGTH_LONG).show()
        }
    }

    // ---------- builders ----------

    private fun headerRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        val title = TextView(this).apply {
            text = "Rizglow Panel"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            setShadowLayer(4f, 0f, 2f, 0x80000000.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val close = TextView(this).apply {
            text = "Close"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            isClickable = true
            setOnClickListener { finish() }
        }
        row.addView(title)
        row.addView(close)
        return row
    }

    /** Fuzzy purple tile with white label + status. */
    private fun tile(name: String, label: String, onClick: () -> Unit): LinearLayout {
        val t = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.cc_tile_bg)
            setPadding(dp(4), dp(10), dp(4), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, dp(86), 1f).apply {
                marginEnd = dp(8)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick(); refreshTiles() }
        }
        val tv = TextView(this).apply {
            text = label
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setShadowLayer(4f, 0f, 2f, 0x80000000.toInt())
        }
        val st = TextView(this).apply {
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
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }
        row.addView(tile("airplane", "Airplane") {
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        })
        row.addView(tile("data", "Data") {
            startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
        })
        row.addView(tile("wifi", "WiFi") {
            if (Build.VERSION.SDK_INT >= 29) startActivity(Intent(Settings.Panel.ACTION_WIFI))
            else startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        })
        val last = tile("bt", "Bluetooth") {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        (last.layoutParams as LinearLayout.LayoutParams).marginEnd = 0
        row.addView(last)
        return row
    }

    private fun musicCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.cc_card_bg)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        val label = TextView(this).apply {
            text = "Music"
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF6B4A8F.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        card.addView(label)
        val btn = { text: String, key: Int ->
            TextView(this).apply {
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
            Toast.makeText(this, "No music app responding", Toast.LENGTH_SHORT).show()
        }
    }

    private fun smallTilesRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }
        row.addView(tile("rotate", "Rotate") { toggleRotate() })
        row.addView(tile("silent", "Silent") { toggleSilent() })
        row.addView(tile("dnd", "Focus") { toggleDnd() })
        val last = tile("alerts", "Alerts") {
            if (!ControlCenterAccessService.openNotifications()) {
                Toast.makeText(this, "Turn \"Swipe\" on first", Toast.LENGTH_SHORT).show()
            } else {
                finish()
            }
        }
        (last.layoutParams as LinearLayout.LayoutParams).marginEnd = 0
        row.addView(last)
        return row
    }

    private fun slidersRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(10))
        }
        row.addView(sliderCard("Brightness", 255,
            init = {
                try {
                    Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                } catch (e: Exception) { 128 }
            },
            onChange = {
                if (canWrite()) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, it)
                } else {
                    startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                }
            }))
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(12), 1)
        }
        row.addView(spacer)
        row.addView(sliderCard("Volume", audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            init = { audio.getStreamVolume(AudioManager.STREAM_MUSIC) },
            onChange = { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }))
        return row
    }

    private fun sliderCard(label: String, max: Int, init: () -> Int, onChange: (Int) -> Unit): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(R.drawable.cc_slider_bg)
            setPadding(dp(8), dp(12), dp(8), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, dp(190), 1f)
        }
        card.addView(TextView(this).apply {
            text = label
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF6B4A8F.toInt())
            gravity = Gravity.CENTER
        })
        val sb = SeekBar(this).apply {
            this.max = max
            progress = init().coerceIn(0, max)
            rotation = 270f
            layoutParams = LinearLayout.LayoutParams(dp(150), dp(48)).apply {
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
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(6))
        }
        row.addView(tile("torch", "Torch") { setTorch(!torchOn) })
        row.addView(tile("camera", "Camera") {
            IconTargets.resolve(this, "Camera")?.let { startActivity(it) }
        })
        row.addView(tile("calc", "Calc") {
            IconTargets.resolve(this, "Calculator")?.let { startActivity(it) }
                ?: Toast.makeText(this, "No calculator found", Toast.LENGTH_SHORT).show()
        })
        val last = tile("settings", "Settings") {
            IconTargets.resolve(this, "Settings")?.let { startActivity(it) }
        }
        (last.layoutParams as LinearLayout.LayoutParams).marginEnd = 0
        row.addView(last)
        return row
    }

    // ---------- actions ----------

    private fun setTorch(on: Boolean) {
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = torchCamId ?: cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }?.also { torchCamId = it } ?: run {
                Toast.makeText(this, "No flash on this phone", Toast.LENGTH_SHORT).show()
                return
            }
            cm.setTorchMode(id, on)
            torchOn = on
        } catch (e: Exception) {
            Toast.makeText(this, "Torch unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleRotate() {
        if (!canWrite()) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
            return
        }
        val cur = try {
            Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION)
        } catch (e: Exception) { 1 }
        Settings.System.putInt(
            contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (cur == 1) 0 else 1
        )
    }

    private fun toggleSilent() {
        audio.ringerMode = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL)
            AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
    }

    private fun toggleDnd() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            Toast.makeText(this, "Allow Do Not Disturb access, then tap again", Toast.LENGTH_LONG).show()
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
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        statusViews["dnd"]?.text =
            if (nm.isNotificationPolicyAccessGranted &&
                nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            ) "On" else "Off"
        if (canWrite()) {
            val rot = try {
                Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION)
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

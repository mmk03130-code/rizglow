package com.rsstudio.rizglow

import android.app.NotificationManager
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private val btnViews = mutableMapOf<String, FrameLayout>()
    private val btnIcons = mutableMapOf<String, ImageView>()
    private val btnState = mutableMapOf<String, () -> Boolean>()
    private var torchOn = false
    private var torchCamId: String? = null
    private var bubbleBtn: TextView? = null
    private var swipeBtn: TextView? = null
    private val theme = PanelThemes.current(context)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun canWrite() = Settings.System.canWrite(context)

    // ---------- auto layout ----------
    /** Panel width already adapts (min 360, 24dp margins); everything inside
     *  scales from it so small phones and large-display mode stay proportional. */
    private val panelW: Int = panelWidthDp(context)
    private val narrow: Boolean = panelW < 300
    /** Tile height proportional to panel width, clamped to sane bounds. */
    private fun tileH(): Int =
        (panelW * if (compact) 0.20f else 0.27f).toInt().coerceIn(56, 96)
    private fun panelPad(): Int = dp(if (narrow) 14 else 18)
    private fun labelSize(): Float = if (narrow) 11.5f else 13f
    private fun statusSize(): Float = if (narrow) 10f else 11f
    /** White text gets a dark shadow; dark text (Ice) gets a soft light one. */
    private fun shadowColor(): Int =
        if (theme.isLight()) 0x40FFFFFF.toInt() else 0x80000000.toInt()

    /** All startActivity calls funnel through here: NEW_TASK is required when
     *  the host is a Service, harmless when it's an Activity.
     *  The panel dismisses first so the new app doesn't open behind it. */
    private fun launch(intent: Intent) {
        onDismiss()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(if (compact) theme.dimCompact else theme.dimFull)
        setPadding(dp(8), dp(if (compact) 16 else 32), dp(8), dp(if (compact) 16 else 32))
        setOnClickListener { onDismiss() } // tap outside dismisses

        val panel = LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundResource(theme.panelBg)
            val pp = panelPad()
            setPadding(pp, pp, pp, dp(10))
            setOnClickListener { /* consume: don't dismiss */ }
        }
        val scroll = ScrollView(context).apply {
            layoutParams = LayoutParams(dp(panelWidthDp(context)), LayoutParams.WRAP_CONTENT)
            isFillViewport = true
            addView(panel.apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            })
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
            setPadding(0, 0, 0, dp(if (compact) 10 else 14))
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
            setTextColor(theme.textOnTile)
            setShadowLayer(4f, 0f, 2f, shadowColor())
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val close = TextView(context).apply {
            text = "Close"
            textSize = 14f
            setTextColor(theme.textOnTile)
            setShadowLayer(4f, 0f, 2f, shadowColor())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            isClickable = true
            setOnClickListener { onDismiss() }
        }
        row.addView(title)
        row.addView(close)
        return row
    }

    /** Glass-orb button with centered icon glyph.
     *  ON = colored glass + dark icon; OFF = dark glass + light icon. */
    private fun tile(name: String, iconRes: Int, isOn: () -> Boolean, onClick: () -> Unit): FrameLayout {
        val size = dp(btnSize())
        val btn = FrameLayout(context).apply {
            setBackgroundResource(if (isOn()) theme.glassOn else theme.glassOff)
            layoutParams = LayoutParams(0, size, 1f).apply {
                marginEnd = dp(8)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick(); refreshTiles() }
        }
        val icon = ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(if (isOn()) theme.iconOnBtn else theme.iconOffBtn)
            val s = dp((btnSize() * 0.42f).toInt())
            layoutParams = FrameLayout.LayoutParams(s, s, Gravity.CENTER)
        }
        btn.addView(icon)
        btnViews[name] = btn
        btnIcons[name] = icon
        btnState[name] = isOn
        return btn
    }

    /** Circular button diameter: fits 4 across with margins. */
    private fun btnSize(): Int {
        val w = panelWidthDp(context)
        return ((w - 32 - 30) / 4).toInt().coerceIn(56, 84)
    }

    private fun connectivityRow(): LinearLayout {
        // Frosted group container holding the 4 connectivity orbs
        val group = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundResource(theme.frostBg)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(if (compact) 10 else 14) }
        }
        val inner = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        inner.addView(tile("airplane", R.drawable.ic_panel_airplane, { false }) {
            launch(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        })
        inner.addView(tile("data", R.drawable.ic_panel_data, { false }) {
            launch(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
        })
        inner.addView(tile("wifi", R.drawable.ic_panel_wifi, { false }) {
            if (Build.VERSION.SDK_INT >= 29) launch(Intent(Settings.Panel.ACTION_WIFI))
            else launch(Intent(Settings.ACTION_WIFI_SETTINGS))
        })
        val last = tile("bt", R.drawable.ic_panel_bt, { false }) {
            launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        (last.layoutParams as LayoutParams).marginEnd = 0
        inner.addView(last)
        group.addView(inner)

        val row = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(0, 0, 0, 0)
        }
        row.addView(group)
        return row
    }

    private fun musicCard(): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(theme.cardBg)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        val note = TextView(context).apply {
            text = "\u266B"
            textSize = 22f
            setTextColor(theme.accent)
            setPadding(0, 0, dp(8), 0)
            setShadowLayer(4f, 0f, 2f, shadowColor())
        }
        card.addView(note)
        val label = TextView(context).apply {
            text = "Music"
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(theme.accent)
            setShadowLayer(4f, 0f, 2f, shadowColor())
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        card.addView(label)
        val btn = { text: String, key: Int ->
            TextView(context).apply {
                this.text = text
                textSize = 20f
                setTextColor(theme.accent)
                setShadowLayer(4f, 0f, 2f, shadowColor())
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
            setPadding(0, 0, 0, dp(if (compact) 10 else 14))
        }
        row.addView(tile("rotate", R.drawable.ic_panel_rotate, { isRotateOn() }) { toggleRotate() })
        row.addView(tile("silent", R.drawable.ic_panel_silent, { isSilentOn() }) { toggleSilent() })
        row.addView(tile("dnd", R.drawable.ic_panel_dnd, { isDndOn() }) { toggleDnd() })
        val last = tile("alerts", R.drawable.ic_panel_alerts, { false }) {
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
            setPadding(0, 0, 0, dp(if (compact) 10 else 14))
        }
        row.addView(sliderCard(R.drawable.ic_panel_brightness, "Brightness", 255,
            init = {
                try {
                    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                } catch (e: Exception) { 128 }
            },
            onChange = {
                if (canWrite()) {
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, it)
                } else {
                    // Explain first, then send to settings — no more "fake" feeling.
                    Toast.makeText(context,
                        "Allow Rizglow to modify system settings to control brightness",
                        Toast.LENGTH_LONG).show()
                    launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                }
            }))
        val spacer = View(context).apply {
            layoutParams = LayoutParams(dp(12), 1)
        }
        row.addView(spacer)
        row.addView(sliderCard(R.drawable.ic_panel_volume, "Volume", audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            init = { audio.getStreamVolume(AudioManager.STREAM_MUSIC) },
            onChange = { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }))
        return row
    }

    private fun sliderCard(iconRes: Int, label: String, max: Int, init: () -> Int, onChange: (Int) -> Unit): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(theme.sliderBg)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            layoutParams = LayoutParams(0, dp(if (compact) 130 else 160), 1f)
        }
        card.addView(ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(theme.accent)
            val s = dp(24)
            layoutParams = LayoutParams(s, s).apply { bottomMargin = dp(2) }
        })
        card.addView(TextView(context).apply {
            text = label
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(theme.accent)
            setShadowLayer(4f, 0f, 2f, shadowColor())
            gravity = Gravity.CENTER
        })
        val slider = VerticalSlider(context).apply {
            this.max = max
            progress = init().coerceIn(0, max)
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f
            ).apply { topMargin = dp(8) }
        }
        slider.onChange = { v -> onChange(v) }
        card.addView(slider)
        return card
    }

    private fun shortcutsRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 0, 0, dp(6))
        }
        row.addView(tile("torch", R.drawable.ic_panel_torch, { torchOn }) { setTorch(!torchOn) })
        row.addView(tile("camera", R.drawable.ic_panel_camera, { false }) {
            IconTargets.resolve(context, "Camera")?.let { launch(it) }
        })
        row.addView(tile("calc", R.drawable.ic_panel_calc, { false }) {
            IconTargets.resolve(context, "Calculator")?.let { launch(it) }
                ?: Toast.makeText(context, "No calculator found", Toast.LENGTH_SHORT).show()
        })
        val last = tile("settings", R.drawable.ic_panel_settings, { false }) {
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

    private fun isRotateOn(): Boolean {
        if (!canWrite()) return false
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 1
        } catch (e: Exception) { false }
    }

    private fun isSilentOn(): Boolean =
        audio.ringerMode != AudioManager.RINGER_MODE_NORMAL

    private fun isDndOn(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted &&
            nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    private fun refreshTiles() {
        for ((name, isOnFn) in btnState) {
            val on = isOnFn()
            btnViews[name]?.setBackgroundResource(if (on) theme.glassOn else theme.glassOff)
            btnIcons[name]?.setColorFilter(if (on) theme.iconOnBtn else theme.iconOffBtn)
        }
    }
}

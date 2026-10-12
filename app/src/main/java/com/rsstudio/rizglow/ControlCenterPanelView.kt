package com.rsstudio.rizglow

import android.app.NotificationManager
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** v3.6 module-grid rebuild.
 *  The panel is now a 2D module grid (AceTools/iOS grammar) instead of a
 *  vertical stack: 2x2 connectivity module + 2x2 music module on top,
 *  a row of 1x1 circular toggles, then two tall 1x2 sliders beside
 *  stacked shortcut circles. Everything is sized from a single grid cell
 *  so the whole panel fits its screen with no scrolling.
 *
 *  Shared by the in-app Activity (full mode) and the swipe-triggered
 *  system overlay (compact mode). Hosts are responsible for adding/removing
 *  this view; tapping outside or "Close" calls [onDismiss]. Call
 *  [playEntrance] after the view is attached for the spring-in animation. */
/** @param compact true for the swipe overlay: pure controls only, no header,
 *  no setup pills, no mascot — like the big theme apps. False for in-app. */
class ControlCenterPanelView(
    context: Context,
    private val onDismiss: () -> Unit,
    private val compact: Boolean = false
) : FrameLayout(context) {

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
    /** Module views in visual order, for the staggered entrance. */
    private val modules = mutableListOf<View>()
    private lateinit var panelCard: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun canWrite() = Settings.System.canWrite(context)

    // ---------- grid math ----------
    // One cell drives every module: 1x1 = cell, 2x2 = 2*cell+gap.
    // Cell is the min of the width fit and the height fit, so the panel
    // always fits its screen with no scrolling, on phones and tablets.
    private val gap: Int = dp(8)
    private val pad: Int = dp(16)
    private val cell: Int = computeCell()
    /** 2-cell module edge, e.g. the 2x2 connectivity/music modules. */
    private val mod2: Int = 2 * cell + gap

    private fun computeCell(): Int {
        val wCell = (dp(panelWidthDp(context)) - 2 * dp(16) - 3 * dp(8)) / 4
        val screenH = resources.displayMetrics.heightPixels
        // chrome: overlay needs only screen margins; in-app also pays for
        // header + setup pills + mascot.
        val chrome = dp(if (compact) 48 else 300)
        val hCell = (screenH - chrome - 2 * dp(16) - 4 * dp(8)) / 5
        return minOf(wCell, hCell).coerceIn(dp(56), dp(88))
    }

    /** White text gets a dark shadow; dark text (Ice) gets a soft light one. */
    private fun shadowColor(): Int =
        if (theme.isLight()) 0x40FFFFFF.toInt() else 0x80000000.toInt()

    // ---------- glass helpers ----------
    /** Theme webp + 1dp white hairline stroke: the glass-edge trick. */
    private fun strokedModule(bgRes: Int): Drawable {
        val bg = context.getDrawable(bgRes)!!
        val stroke = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), 0x33FFFFFF)
            setColor(0x00000000)
        }
        return LayerDrawable(arrayOf(bg, stroke))
    }

    private fun strokedOval(bgRes: Int): Drawable {
        val bg = context.getDrawable(bgRes)!!
        val stroke = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(1), 0x33FFFFFF)
            setColor(0x00000000)
        }
        return LayerDrawable(arrayOf(bg, stroke))
    }

    /** All startActivity calls funnel through here: NEW_TASK is required when
     *  the host is a Service, harmless when it's an Activity.
     *  The panel dismisses first so the new app doesn't open behind it. */
    private fun launch(intent: Intent) {
        onDismiss()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    init {
        // Gradient dim (vignette feel) instead of a flat grey overlay —
        // flat dims are what users call "cheap" vs real blur.
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xB3000000.toInt(), 0x80000000.toInt(), 0xB3000000.toInt())
        )
        setOnClickListener { onDismiss() } // tap outside dismisses

        panelCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
            setOnClickListener { /* consume: don't dismiss */ }
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER
            )
        }
        addView(panelCard)

        if (!compact) {
            panelCard.addView(headerRow())
            panelCard.addView(setupRow())
        }
        panelCard.addView(topModulesRow())
        panelCard.addView(togglesRow())
        panelCard.addView(bottomRow())
        if (!compact) {
            val cat = ImageView(context).apply {
                setImageResource(R.drawable.cc_cat)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(96)
                ).apply { topMargin = dp(8) }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            panelCard.addView(cat)
        }
    }

    /** Spring entrance: card slides up + fades, modules stagger in. */
    fun playEntrance() {
        panelCard.translationY = dp(48).toFloat()
        panelCard.alpha = 0f
        panelCard.animate().translationY(0f).alpha(1f).setDuration(340)
            .setInterpolator(OvershootInterpolator(0.85f)).start()
        modules.forEachIndexed { i, v ->
            v.alpha = 0f
            v.scaleX = 0.92f
            v.scaleY = 0.92f
            v.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(70 + i * 18L).setDuration(230)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }

    /** Refresh tile states + setup buttons. Hosts call this when showing. */
    fun refresh() {
        refreshTiles()
        refreshSetupBtns()
    }

    private fun trackModule(v: View): View {
        modules.add(v)
        return v
    }

    // ---------- rows ----------

    private fun headerRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val title = TextView(context).apply {
            text = "Rizglow Panel"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(theme.textOnTile)
            setShadowLayer(4f, 0f, 2f, shadowColor())
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
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

    private fun setupRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val pill = { text: String, onClick: () -> Unit ->
            TextView(context).apply {
                this.text = text
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(dp(12), dp(8), dp(12), dp(8))
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
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

    /** Row 1: 2x2 connectivity module + 2x2 music module. */
    private fun topModulesRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, gap)
        }
        row.addView(trackModule(connectivityModule()).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = gap
        })
        row.addView(trackModule(musicModule()))
        return row
    }

    /** Row 2: four 1x1 circular toggles. */
    private fun togglesRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, gap)
        }
        val tiles = listOf(
            tile("rotate", R.drawable.ic_panel_rotate, { isRotateOn() }) { toggleRotate() },
            tile("silent", R.drawable.ic_panel_silent, { isSilentOn() }) { toggleSilent() },
            tile("dnd", R.drawable.ic_panel_dnd, { isDndOn() }) { toggleDnd() },
            tile("alerts", R.drawable.ic_panel_alerts, { false }) {
                if (!ControlCenterAccessService.openNotifications()) {
                    Toast.makeText(context, "Turn \"Swipe\" on first", Toast.LENGTH_LONG).show()
                } else {
                    onDismiss()
                }
            }
        )
        tiles.forEachIndexed { i, t ->
            if (i < 3) (t.layoutParams as LinearLayout.LayoutParams).marginEnd = gap
            row.addView(trackModule(t))
        }
        return row
    }

    /** Rows 3-4: two tall 1x2 sliders + two columns of stacked 1x1 shortcuts. */
    private fun bottomRow(): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val b = trackModule(tallSlider(R.drawable.ic_panel_brightness, "Brightness", 255,
            init = {
                try {
                    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                } catch (e: Exception) { 128 }
            },
            onChange = {
                if (canWrite()) {
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, it)
                } else {
                    Toast.makeText(context,
                        "Allow Rizglow to modify system settings to control brightness",
                        Toast.LENGTH_LONG).show()
                    launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                }
            }))
        (b.layoutParams as LinearLayout.LayoutParams).marginEnd = gap
        row.addView(b)
        val v = trackModule(tallSlider(R.drawable.ic_panel_volume, "Volume",
            audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            init = { audio.getStreamVolume(AudioManager.STREAM_MUSIC) },
            onChange = { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }))
        (v.layoutParams as LinearLayout.LayoutParams).marginEnd = gap
        row.addView(v)
        row.addView(shortcutColumn(
            tile("torch", R.drawable.ic_panel_torch, { torchOn }) { setTorch(!torchOn) },
            tile("camera", R.drawable.ic_panel_camera, { false }) {
                IconTargets.resolve(context, "Camera")?.let { launch(it) }
            }
        ).apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = gap })
        row.addView(shortcutColumn(
            tile("calc", R.drawable.ic_panel_calc, { false }) {
                IconTargets.resolve(context, "Calculator")?.let { launch(it) }
                    ?: Toast.makeText(context, "No calculator found", Toast.LENGTH_SHORT).show()
            },
            tile("settings", R.drawable.ic_panel_settings, { false }) {
                IconTargets.resolve(context, "Settings")?.let { launch(it) }
            }
        ))
        return row
    }

    private fun shortcutColumn(top: FrameLayout, bottom: FrameLayout): LinearLayout {
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(cell, mod2)
        }
        (top.layoutParams as LinearLayout.LayoutParams).bottomMargin = gap
        col.addView(trackModule(top))
        col.addView(trackModule(bottom))
        return col
    }

    // ---------- modules ----------

    /** 2x2 frosted connectivity module: four circles in a 2x2 arrangement. */
    private fun connectivityModule(): FrameLayout {
        val inner = dp(14)
        val dia = (mod2 - 2 * inner - gap) / 2
        val group = FrameLayout(context).apply {
            background = strokedModule(theme.frostBg)
            layoutParams = LinearLayout.LayoutParams(mod2, mod2)
        }
        val grid = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(inner, inner, inner, inner)
            layoutParams = FrameLayout.LayoutParams(mod2, mod2)
        }
        val mkRow: () -> LinearLayout = {
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
            }
        }
        val mkTile: (String, Int, () -> Unit) -> FrameLayout = { name, icon, onClick ->
            miniTile(name, icon, { false }, onClick, dia)
        }
        val r1 = mkRow()
        r1.addView(mkTile("airplane", R.drawable.ic_panel_airplane) {
            launch(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }.apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = gap })
        r1.addView(mkTile("data", R.drawable.ic_panel_data) {
            launch(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
        })
        val r2 = mkRow().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = gap
        }
        r2.addView(mkTile("wifi", R.drawable.ic_panel_wifi) {
            if (Build.VERSION.SDK_INT >= 29) launch(Intent(Settings.Panel.ACTION_WIFI))
            else launch(Intent(Settings.ACTION_WIFI_SETTINGS))
        }.apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = gap })
        r2.addView(mkTile("bt", R.drawable.ic_panel_bt) {
            launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        })
        grid.addView(r1)
        grid.addView(r2)
        group.addView(grid)
        return group
    }

    /** 2x2 music module. */
    private fun musicModule(): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = strokedModule(theme.cardBg)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(mod2, mod2)
        }
        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        val note = TextView(context).apply {
            text = "\u266B"
            textSize = 20f
            setTextColor(theme.accent)
            setShadowLayer(4f, 0f, 2f, shadowColor())
            setPadding(0, 0, dp(6), 0)
        }
        top.addView(note)
        val label = TextView(context).apply {
            text = "Music"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(theme.accent)
            setShadowLayer(4f, 0f, 2f, shadowColor())
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        top.addView(label)
        card.addView(top)
        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        val btn = { text: String, key: Int ->
            TextView(context).apply {
                this.text = text
                // Auto-shrink so prev/play/next never clip on narrow cells.
                setAutoSizeTextTypeUniformWithConfiguration(
                    20, 10, 1, android.util.TypedValue.COMPLEX_UNIT_SP
                )
                setTextColor(theme.accent)
                setShadowLayer(4f, 0f, 2f, shadowColor())
                setPadding(dp(6), dp(6), dp(6), dp(6))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                isClickable = true
                setOnClickListener { mediaKey(key) }
            }
        }
        controls.addView(btn("|\u25C0", KeyEvent.KEYCODE_MEDIA_PREVIOUS))
        controls.addView(btn("\u25B6", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        controls.addView(btn("\u25B6|", KeyEvent.KEYCODE_MEDIA_NEXT))
        card.addView(controls)
        return card
    }

    /** 1x2 tall slider module. */
    private fun tallSlider(
        iconRes: Int, label: String, max: Int, init: () -> Int, onChange: (Int) -> Unit
    ): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = strokedModule(theme.sliderBg)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            layoutParams = LinearLayout.LayoutParams(cell, mod2)
        }
        card.addView(ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(theme.accent)
            val s = dp(24)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { bottomMargin = dp(2) }
        })
        card.addView(TextView(context).apply {
            text = label
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(theme.accent)
            setShadowLayer(4f, 0f, 2f, shadowColor())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        })
        val slider = VerticalSlider(context).apply {
            this.max = max
            progress = init().coerceIn(0, max)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        slider.onChange = { v -> onChange(v) }
        card.addView(slider)
        return card
    }

    // ---------- tiles ----------

    /** Glass-orb button with centered icon glyph + press-scale feedback.
     *  ON = colored glass + dark icon; OFF = dark glass + light icon. */
    private fun tile(name: String, iconRes: Int, isOn: () -> Boolean, onClick: () -> Unit): FrameLayout {
        return miniTile(name, iconRes, isOn, onClick, cell)
    }

    private fun miniTile(
        name: String, iconRes: Int, isOn: () -> Boolean, onClick: () -> Unit, sizePx: Int
    ): FrameLayout {
        val btn = FrameLayout(context).apply {
            background = strokedOval(if (isOn()) theme.glassOn else theme.glassOff)
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick(); refreshTiles() }
            setOnTouchListener { v, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN ->
                        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(130).start()
                }
                false
            }
        }
        val icon = ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(if (isOn()) theme.iconOnBtn else theme.iconOffBtn)
            val s = (sizePx * 0.42f).toInt()
            layoutParams = FrameLayout.LayoutParams(s, s, Gravity.CENTER)
        }
        btn.addView(icon)
        btnViews[name] = btn
        btnIcons[name] = icon
        btnState[name] = isOn
        return btn
    }

    // ---------- setup / actions (unchanged behavior) ----------

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
            btnViews[name]?.background = strokedOval(if (on) theme.glassOn else theme.glassOff)
            btnIcons[name]?.setColorFilter(if (on) theme.iconOnBtn else theme.iconOffBtn)
        }
    }
}

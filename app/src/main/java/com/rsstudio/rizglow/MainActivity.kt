package com.rsstudio.rizglow

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import com.rsstudio.rizglow.ads.UnlockManager
import com.rsstudio.rizglow.model.BundleRepository
import com.rsstudio.rizglow.model.IconItem
import com.rsstudio.rizglow.model.RizBundle

/** Main app: Discover (bundles) / Wallpapers (grids) / Icons (sets) / Widgets (soon). */
class MainActivity : Activity() {

    private lateinit var pages: List<View>
    private lateinit var labels: List<TextView>
    private lateinit var tabs: List<LinearLayout>
    private var currentTab = 0

    /** Bundle section anchors for deep-jump: bundleId -> its header view. */
    private val wallpaperAnchors = mutableMapOf<String, View>()
    private val iconAnchors = mutableMapOf<String, View>()

    companion object {
        private const val TAB_WALLPAPERS = 1
        private const val TAB_ICONS = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init Google Mobile Ads once (test ad units until real IDs land).
        com.google.android.gms.ads.MobileAds.initialize(this) {}

        findViewById<TextView>(R.id.btn_about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        pages = listOf(
            findViewById(R.id.page_discover),
            findViewById(R.id.page_wallpapers),
            findViewById(R.id.page_icons),
            findViewById(R.id.page_widgets),
            findViewById(R.id.page_panel)
        )
        labels = listOf(
            findViewById(R.id.label_discover),
            findViewById(R.id.label_wallpapers),
            findViewById(R.id.label_icons),
            findViewById(R.id.label_widgets),
            findViewById(R.id.label_panel)
        )
        tabs = listOf(
            findViewById(R.id.tab_discover),
            findViewById(R.id.tab_wallpapers),
            findViewById(R.id.tab_icons),
            findViewById(R.id.tab_widgets),
            findViewById(R.id.tab_panel)
        )
        tabs.forEachIndexed { i, t -> t.setOnClickListener { selectTab(i) } }

        // Swipe left/right on any page to switch tabs (helps phones where the
        // bottom tabs sit too close to the system back button/gestures).
        val swipeDetector = GestureDetector(this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent,
                    velocityX: Float, velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val dx = e2.x - e1.x
                    val dy = e2.y - e1.y
                    if (abs(dx) > abs(dy) * 1.5f && abs(dx) > dp(80) && abs(velocityX) > 400) {
                        if (dx < 0) selectTab((currentTab + 1).coerceAtMost(4))
                        else selectTab((currentTab - 1).coerceAtLeast(0))
                        return true
                    }
                    return false
                }
            })
        val swipeTouch = View.OnTouchListener { _, e ->
            swipeDetector.onTouchEvent(e)
            false // don't consume: scrolling still works
        }
        pages.forEach { it.setOnTouchListener(swipeTouch) }

        refreshAll()
        selectTab(0)
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun refreshAll() {
        populateDiscover()
        buildWallpapersTab()
        buildIconsTab()
        buildWidgetsTab()
        buildPanelTab()
    }

    private fun selectTab(i: Int) {
        currentTab = i
        pages.forEachIndexed { idx, v -> v.visibility = if (idx == i) View.VISIBLE else View.GONE }
        // Premium segmented pills: selected tab gets a purple-tinted pill + ring.
        tabs.forEachIndexed { idx, t -> t.background = tabPill(idx == i) }
        val text = getColor(R.color.text)
        val muted = getColor(R.color.muted)
        labels.forEachIndexed { idx, l ->
            l.setTextColor(if (idx == i) text else muted)
            l.setTypeface(l.typeface, if (idx == i) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun tabPill(selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            if (selected) {
                setColor(0x2BB388FF.toInt())
                setStroke(dp(1), 0xAAB388FF.toInt())
            } else {
                setColor(0x00000000)
            }
        }
    }

    /** Jump to a tab and smooth-scroll to that bundle's own section —
     *  tapping a bundle opens THAT bundle, never the top of the tab. */
    private fun openBundle(bundleId: String, tab: Int) {
        selectTab(tab)
        val anchors = if (tab == TAB_ICONS) iconAnchors else wallpaperAnchors
        val target = anchors[bundleId] ?: return
        val scroll = findViewById<ScrollView>(
            if (tab == TAB_ICONS) R.id.page_icons else R.id.page_wallpapers)
        val list = findViewById<LinearLayout>(
            if (tab == TAB_ICONS) R.id.icons_list else R.id.wallpapers_list)
        scroll.post {
            scroll.smoothScrollTo(0, (list.top + target.top - dp(12)).coerceAtLeast(0))
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ---------- Discover: full bundle cards ----------

    private fun populateDiscover() {
        val container = findViewById<LinearLayout>(R.id.discover_list)
        container.removeAllViews()
        // NOTE: Panel Themes live in their own Panel tab now — Discover is bundles only.
        val inflater = LayoutInflater.from(this)
        for (b in BundleRepository.bundles) {
            val card = inflater.inflate(R.layout.item_bundle, container, false)
            card.findViewById<TextView>(R.id.card_name).text = b.name
            card.findViewById<TextView>(R.id.card_tagline).text = b.tagline
            // Phone mockup: wallpaper + 4 sample icons.
            val mockupWp = card.findViewById<ImageView>(R.id.card_mockup_wallpaper)
            if (b.wallpapers.isNotEmpty()) {
                mockupWp.setImageResource(b.wallpapers[0])
            } else {
                mockupWp.setBackgroundColor(0xFF201F29.toInt())
            }
            val mockupIcons = card.findViewById<GridLayout>(R.id.card_mockup_icons)
            mockupIcons.removeAllViews()
            for (icon in b.icons.take(4)) {
                val iv = ImageView(this).apply {
                    setImageResource(icon.resId)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = dp(30); height = dp(30)
                        setMargins(dp(3), dp(3), dp(3), dp(3))
                    }
                }
                mockupIcons.addView(iv)
            }
            val counts = card.findViewById<TextView>(R.id.card_counts)
            counts.text = if (b.wallpapers.isEmpty() && b.icons.isEmpty()) {
                "Coming soon"
            } else {
                "${b.wallpapers.size} wallpapers • ${b.icons.size} icons"
            }
            val accent = card.findViewById<View>(R.id.card_accent)
            (accent.background as? GradientDrawable)?.setColor(b.accentColor)
                ?: accent.setBackgroundColor(b.accentColor)

            val unlocked = UnlockManager.isUnlocked(this, b.id)
            val action = card.findViewById<TextView>(R.id.card_action)
            action.text = if (unlocked) "Open" else "🔒 Unlock"
            // Tapping a bundle opens THAT bundle — jump to its own section.
            val openOrUnlock = {
                if (UnlockManager.isUnlocked(this, b.id)) {
                    if (b.wallpapers.isEmpty()) {
                        Toast.makeText(this, "Content arrives in the next drop ✨", Toast.LENGTH_SHORT).show()
                    } else {
                        openBundle(b.id, TAB_WALLPAPERS)
                    }
                } else {
                    showUnlockDialog(b)
                }
            }
            action.setOnClickListener { openOrUnlock() }
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener { openOrUnlock() }
            container.addView(card)
        }
    }

    // ---------- Wallpapers tab: per-bundle grids ----------

    private fun buildWallpapersTab() {
        val container = findViewById<LinearLayout>(R.id.wallpapers_list)
        container.removeAllViews()
        wallpaperAnchors.clear()
        // Favorites shelf first (the "mine" collection).
        val favs = FavoritesManager.all(this)
        if (favs.isNotEmpty()) {
            container.addView(TextView(this).apply {
                text = "❤ Favorites"
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(getColor(R.color.text))
                setPadding(0, 0, 0, dp(10))
            })
            container.addView(wallpaperGrid(favs))
        }
        for (b in BundleRepository.bundles) {
            val header = sectionHeader(b, "wallpapers")
            wallpaperAnchors[b.id] = header
            container.addView(header)
            when {
                b.wallpapers.isEmpty() -> {
                    if (!UnlockManager.isUnlocked(this, b.id))
                        container.addView(lockedCard(b, "wallpapers"))
                    else
                        container.addView(comingSoonView("Wallpapers arriving in the next drop."))
                }
                // Show the goods even when locked — gate the APPLY, not the browse.
                else -> container.addView(wallpaperGrid(b.wallpapers,
                    lockedBundleId = if (UnlockManager.isUnlocked(this, b.id)) null else b.id))
            }
        }
    }

    /** 2-column wallpaper grid; each cell has a heart overlay.
     *  If [lockedBundleId] is set, tapping previews but the Set button unlock-gates. */
    private fun wallpaperGrid(ids: List<Int>, lockedBundleId: String? = null): GridLayout {
        val grid = GridLayout(this).apply { columnCount = 2 }
        for ((idx, resId) in ids.withIndex()) {
            val cell = FrameLayout(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(210)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { openPreview(ids, idx, lockedBundleId) }
            }
            val iv = ImageView(this).apply {
                setImageResource(resId)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            cell.addView(iv)
            val heart = TextView(this).apply {
                text = if (FavoritesManager.isFavorite(this@MainActivity, resId)) "❤" else "🤍"
                textSize = 20f
                setPadding(dp(8), dp(8), dp(8), dp(8))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.END
                )
                isClickable = true
                setOnClickListener {
                    val nowFav = FavoritesManager.toggle(this@MainActivity, resId)
                    text = if (nowFav) "❤" else "🤍"
                    // Refresh the shelf (add/remove) without rebuilding everything.
                    buildWallpapersTab()
                }
            }
            cell.addView(heart)
            grid.addView(cell)
        }
        return grid
    }

    // ---------- Panel tab ----------

    private fun buildPanelTab() {
        val container = findViewById<LinearLayout>(R.id.panel_list)
        container.removeAllViews()

        container.addView(TextView(this).apply {
            text = "Themes"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.text))
            setPadding(0, 0, 0, dp(10))
        })
        val themeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }
        // Panel themes: one card per theme, tap to select + preview.
        for (t in PanelThemes.ALL) {
            val selected = PanelThemes.current(this).id == t.id
            val themeCard = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(140), dp(180)).apply {
                    marginEnd = dp(12)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(16).toFloat()
                    setColor(getColor(R.color.surface))
                    setStroke(dp(if (selected) 3 else 1),
                        if (selected) 0xFFB388FF.toInt() else getColor(R.color.muted))
                }
                clipToOutline = true
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    PanelThemes.select(this@MainActivity, t.id)
                    Toast.makeText(this@MainActivity, "${t.name} applied",
                        Toast.LENGTH_SHORT).show()
                    buildPanelTab() // refresh selection rings
                    startActivity(Intent(this@MainActivity, ControlCenterActivity::class.java))
                }
            }
            themeCard.addView(ImageView(this).apply {
                setImageResource(t.panelBg)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(120)
                )
            })
            themeCard.addView(TextView(this).apply {
                text = "${t.name}\n${if (selected) "✓ Active — tap to preview" else "Tap to apply"}"
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(getColor(R.color.text))
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(4), dp(8), dp(8))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
            })
            themeRow.addView(themeCard)
        }
        themeRow.addView(TextView(this).apply {
            text = "+\nMore soon"
            textSize = 15f
            setTextColor(getColor(R.color.muted))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(140), dp(180))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(getColor(R.color.surface))
                setStroke(dp(1), getColor(R.color.muted))
            }
        })
        container.addView(themeRow)

        container.addView(TextView(this).apply {
            text = "Setup"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.text))
            setPadding(0, 0, 0, dp(10))
        })
        val swipeOn = ControlCenterAccessService.isEnabled(this) &&
            ControlCenterAccessService.isSwipeOpen(this)
        container.addView(setupActionCard(
            if (swipeOn) "Swipe to open: ON" else "Swipe to open: OFF",
            "Swipe down from the top-right corner, below the status bar."
        ) {
            startActivity(Intent(this, SwipeSetupActivity::class.java))
        })
        val bubbleOn = BubbleService.isRunning(this)
        container.addView(setupActionCard(
            if (bubbleOn) "Floating bubble: ON" else "Floating bubble: OFF",
            "A draggable cat bubble that opens the panel anywhere."
        ) {
            if (bubbleOn) {
                stopService(Intent(this, BubbleService::class.java))
            } else {
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Allow \"Display over other apps\", then tap again",
                        Toast.LENGTH_LONG).show()
                    startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    return@setupActionCard
                }
                startService(Intent(this, BubbleService::class.java))
            }
            buildPanelTab()
        })
    }

    private fun setupActionCard(title: String, desc: String, onClick: () -> Unit): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(getColor(R.color.surface))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        card.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.text))
        })
        card.addView(TextView(this).apply {
            text = desc
            textSize = 13f
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(4), 0, 0)
        })
        return card
    }

    // ---------- Widgets tab ----------

    private fun buildWidgetsTab() {
        val container = findViewById<LinearLayout>(R.id.widgets_list)
        container.removeAllViews()
        // Visual preview cards: live mini-render of each widget.
        container.addView(widgetCard(
            "Rizglow Clock", "Live clock + date in your bundle style.",
            0xFFB388FF.toInt(), widgetPreviewClock(0xFFB388FF.toInt())
        ))
        container.addView(widgetCard(
            "Rizglow Battery", "Battery level with tap-to-refresh, in your bundle style.",
            0xFFF5A8C0.toInt(), widgetPreviewBattery(0xFFF5A8C0.toInt())
        ))
        container.addView(widgetCard(
            "Rizglow Date", "Big day + date in your bundle style.",
            0xFF7C6CFF.toInt(), widgetPreviewDate(0xFF7C6CFF.toInt())
        ))
        container.addView(TextView(this).apply {
            text = "To add: long-press your home screen → Widgets → choose a Rizglow widget → pick a bundle style."
            textSize = 13f
            setTextColor(getColor(R.color.muted))
            setPadding(dp(4), dp(8), dp(4), 0)
        })
    }

    private fun widgetCard(
        name: String, desc: String, accent: Int, preview: View
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(getColor(R.color.surface))
                setStroke(dp(1), accent)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
        val previewBox = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96)).apply {
                marginEnd = dp(14)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(getColor(R.color.surface2))
            }
            addView(preview)
        }
        card.addView(previewBox)
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(this).apply {
            text = name
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
        })
        info.addView(TextView(this).apply {
            text = desc
            textSize = 13f
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(4), 0, 0)
        })
        card.addView(info)
        return card
    }

    private fun widgetPreviewClock(accent: Int): TextView {
        val cal = java.util.Calendar.getInstance()
        val hh = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val mm = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
        return TextView(this).apply {
            text = "$hh:$mm"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun widgetPreviewBattery(accent: Int): TextView {
        val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        val pct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return TextView(this).apply {
            text = "🔋$pct%"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun widgetPreviewDate(accent: Int): TextView {
        val day = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
            .format(java.util.Date())
        val date = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            .format(java.util.Date())
        return TextView(this).apply {
            text = "$day\n$date"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    // ---------- Icons tab: per-bundle sets ----------

    private fun buildIconsTab() {
        val container = findViewById<LinearLayout>(R.id.icons_list)
        container.removeAllViews()
        iconAnchors.clear()
        // Launcher banner: tailored guidance (CandyBar pattern).
        val launchers = LauncherDetector.installed(this)
        val banner = TextView(this).apply {
            text = if (launchers.isNotEmpty()) {
                "✓ ${launchers.joinToString(", ")} detected — apply the Rizglow icon pack there for the full themed look."
            } else {
                "Tip: install a free launcher like Lawnchair, then apply the Rizglow icon pack for a fully themed home screen."
            }
            textSize = 13f
            setTextColor(getColor(R.color.text))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(getColor(R.color.surface2))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }
        container.addView(banner)
        for (b in BundleRepository.bundles) {
            val header = sectionHeader(b, "icons")
            iconAnchors[b.id] = header
            container.addView(header)
            when {
                b.icons.isEmpty() -> {
                    if (!UnlockManager.isUnlocked(this, b.id))
                        container.addView(lockedCard(b, "icon set"))
                    else
                        container.addView(comingSoonView("Icon set arriving in the next drop."))
                }
                // Show the icons even when locked — gate the USE, not the browse.
                else -> {
                    val locked = !UnlockManager.isUnlocked(this, b.id)
                    val grid = GridLayout(this).apply { columnCount = 4 }
                    for (icon in b.icons) {
                        val cell = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER_HORIZONTAL
                            layoutParams = GridLayout.LayoutParams().apply {
                                width = 0
                                height = GridLayout.LayoutParams.WRAP_CONTENT
                                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                                setMargins(dp(4), dp(10), dp(4), dp(10))
                            }
                        }
                        val iv = ImageView(this).apply {
                            setImageResource(icon.resId)
                            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
                        }
                        val label = TextView(this).apply {
                            text = icon.label
                            textSize = 11f
                            setTextColor(getColor(R.color.muted))
                            gravity = Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { topMargin = dp(6) }
                        }
                        cell.addView(iv)
                        cell.addView(label)
                        cell.isClickable = true
                        cell.isFocusable = true
                        cell.setOnClickListener {
                            if (locked) {
                                com.rsstudio.rizglow.ads.UnlockManager.promptUnlock(this, b.id)
                            } else {
                                showIconDialog(b.id, icon)
                            }
                        }
                        grid.addView(cell)
                    }
                    container.addView(grid)
                }
            }
        }
    }

    // ---------- shared builders ----------

    private fun sectionHeader(b: RizBundle, kind: String): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(8))
        }
        val name = TextView(this).apply {
            text = b.name
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text))
        }
        val sub = TextView(this).apply {
            text = b.tagline + " • " + kind
            textSize = 13f
            setTextColor(getColor(R.color.muted))
        }
        box.addView(name)
        box.addView(sub)
        return box
    }

    private fun lockedCard(b: RizBundle, kind: String): View {
        return TextView(this).apply {
            text = "🔒 ${b.name} is locked — unlock to browse its $kind."
            textSize = 14f
            setTextColor(getColor(R.color.muted))
            setBackgroundResource(R.drawable.card_bg)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            isClickable = true
            isFocusable = true
            setOnClickListener { showUnlockDialog(b) }
        }
    }

    private fun comingSoonView(msg: String): View {
        return TextView(this).apply {
            text = "✨ $msg"
            textSize = 14f
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(4), 0, dp(16))
        }
    }

    private fun openPreview(ids: List<Int>, index: Int, lockedBundleId: String? = null) {
        val intent = Intent(this, WallpaperPreviewActivity::class.java)
            .putExtra(WallpaperPreviewActivity.EXTRA_RES_IDS, ids.toIntArray())
            .putExtra(WallpaperPreviewActivity.EXTRA_INDEX, index)
        if (lockedBundleId != null)
            intent.putExtra(WallpaperPreviewActivity.EXTRA_LOCKED_BUNDLE, lockedBundleId)
        startActivity(intent)
    }

    /** Legacy single-wallpaper entry (kept for safety). */
    private fun openPreview(resId: Int) {
        openPreview(listOf(resId), 0)
    }

    private fun showIconDialog(bundleId: String, icon: IconItem) {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(8))
        }
        val iv = ImageView(this).apply {
            setImageResource(icon.resId)
            layoutParams = LinearLayout.LayoutParams(dp(110), dp(110))
        }
        val label = TextView(this).apply {
            text = icon.label
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
        val note = TextView(this).apply {
            text = "Add a ${icon.label} shortcut with this icon to your home screen."
            textSize = 13f
            setTextColor(getColor(R.color.muted))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }
        view.addView(iv)
        view.addView(label)
        view.addView(note)

        // Only offer pinning when we can actually find the target app.
        val canPin = resolveTargetIntent(icon.label) != null
        val builder = AlertDialog.Builder(this)
            .setView(view)
            .setNegativeButton("Close", null)
        if (canPin) {
            builder.setPositiveButton("Add to Home screen") { d, _ ->
                d.dismiss()
                createThemedShortcut(icon)
            }
            builder.setNeutralButton("No watermark") { d, _ ->
                d.dismiss()
                requestPinIconWidget(bundleId, icon)
            }
        }
        builder.show()
    }

    /** Resolve the real app behind an icon label on this device. */
    private fun resolveTargetIntent(label: String): Intent? =
        IconTargets.resolve(this, label)

    /** Pin a 1x1 widget that looks like an icon — widgets never get the
     *  system shortcut watermark badge (the Themezy trick).
     *  The system calls us back with the new widget ID; we assign the picked
     *  icon then. FLAG_MUTABLE lets the system attach EXTRA_APPWIDGET_ID. */
    private fun requestPinIconWidget(bundleId: String, icon: IconItem) {
        val awm = getSystemService(AppWidgetManager::class.java)
        if (awm == null || !awm.isRequestPinAppWidgetSupported) {
            Toast.makeText(this, "This launcher can't pin widgets", Toast.LENGTH_SHORT).show()
            return
        }
        val provider = ComponentName(this, IconWidgetProvider::class.java)
        val callbackIntent = Intent(this, WidgetPinReceiver::class.java).apply {
            putExtra("bundleId", bundleId)
            putExtra("label", icon.label)
        }
        val callback = PendingIntent.getBroadcast(
            this,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            callbackIntent,
            PendingIntent.FLAG_MUTABLE
        )
        // Stash the pick first: if the pin callback never arrives (vivo etc.),
        // the widget claims this pending pin when it wakes up unmapped.
        IconWidgetProvider.addPending(this, bundleId, icon.label)
        awm.requestPinAppWidget(provider, null, callback)
    }

    private fun createThemedShortcut(icon: IconItem) {
        val target = resolveTargetIntent(icon.label)
        if (target == null) {
            Toast.makeText(this, "Couldn't find the ${icon.label} app on this phone", Toast.LENGTH_SHORT).show()
            return
        }
        val sm = getSystemService(ShortcutManager::class.java)
        if (sm == null || !sm.isRequestPinShortcutSupported) {
            Toast.makeText(this, "This launcher doesn't support pinned shortcuts", Toast.LENGTH_SHORT).show()
            return
        }
        val info = ShortcutInfo.Builder(this, "rizglow_${icon.label.lowercase()}")
            .setShortLabel(icon.label)
            .setIcon(adaptiveIcon(icon.resId))
            .setIntent(target)
            .build()
        sm.requestPinShortcut(info, null)
        Toast.makeText(this, "Confirm on your home screen ✨", Toast.LENGTH_SHORT).show()
    }

    /** Full-bleed adaptive icon so the launcher masks it cleanly (no white box). */
    private fun adaptiveIcon(resId: Int): Icon {
        val bmp = BitmapFactory.decodeResource(resources, resId)
        return if (bmp != null) Icon.createWithAdaptiveBitmap(bmp)
        else Icon.createWithResource(this, resId)
    }

    private fun showUnlockDialog(b: RizBundle) {
        val what = when {
            b.wallpapers.isNotEmpty() -> "${b.wallpapers.size} wallpapers + ${b.icons.size} icons"
            else -> "the full ${b.name} bundle when its content drops"
        }
        AlertDialog.Builder(this)
            .setTitle("Unlock ${b.name}")
            .setMessage(
                "Watch a short ad to unlock $what.\n\n" +
                "Rewarded ads plug in here soon (AdMob). Dev shortcut below."
            )
            .setPositiveButton("Unlock now (dev)") { d, _ ->
                UnlockManager.unlock(this, b.id)
                d.dismiss()
                refreshAll()
                Toast.makeText(this, "${b.name} unlocked — enjoy the glow ✨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Later", null)
            .show()
    }
}

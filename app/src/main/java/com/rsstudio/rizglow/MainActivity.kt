package com.rsstudio.rizglow

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.GradientDrawable
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.rsstudio.rizglow.ads.UnlockManager
import com.rsstudio.rizglow.model.BundleRepository
import com.rsstudio.rizglow.model.IconItem
import com.rsstudio.rizglow.model.RizBundle

/** Main app: Discover (bundles) / Wallpapers (grids) / Icons (sets) / Widgets (soon). */
class MainActivity : Activity() {

    private lateinit var pages: List<View>
    private lateinit var dots: List<View>
    private lateinit var labels: List<TextView>
    private lateinit var tabs: List<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.btn_about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        pages = listOf(
            findViewById(R.id.page_discover),
            findViewById(R.id.page_wallpapers),
            findViewById(R.id.page_icons),
            findViewById(R.id.page_widgets)
        )
        dots = listOf(
            findViewById(R.id.dot_discover),
            findViewById(R.id.dot_wallpapers),
            findViewById(R.id.dot_icons),
            findViewById(R.id.dot_widgets)
        )
        labels = listOf(
            findViewById(R.id.label_discover),
            findViewById(R.id.label_wallpapers),
            findViewById(R.id.label_icons),
            findViewById(R.id.label_widgets)
        )
        tabs = listOf(
            findViewById(R.id.tab_discover),
            findViewById(R.id.tab_wallpapers),
            findViewById(R.id.tab_icons),
            findViewById(R.id.tab_widgets)
        )
        tabs.forEachIndexed { i, t -> t.setOnClickListener { selectTab(i) } }

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
    }

    private fun selectTab(i: Int) {
        pages.forEachIndexed { idx, v -> v.visibility = if (idx == i) View.VISIBLE else View.GONE }
        dots.forEachIndexed { idx, d -> d.visibility = if (idx == i) View.VISIBLE else View.INVISIBLE }
        val text = getColor(R.color.text)
        val muted = getColor(R.color.muted)
        labels.forEachIndexed { idx, l -> l.setTextColor(if (idx == i) text else muted) }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ---------- Discover: full bundle cards ----------

    private fun populateDiscover() {
        val container = findViewById<LinearLayout>(R.id.discover_list)
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (b in BundleRepository.bundles) {
            val card = inflater.inflate(R.layout.item_bundle, container, false)
            card.findViewById<TextView>(R.id.card_name).text = b.name
            card.findViewById<TextView>(R.id.card_tagline).text = b.tagline
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
            action.setOnClickListener {
                if (UnlockManager.isUnlocked(this, b.id)) {
                    if (b.wallpapers.isEmpty()) {
                        Toast.makeText(this, "Content arrives in the next drop ✨", Toast.LENGTH_SHORT).show()
                    } else {
                        selectTab(1) // jump to its wallpapers
                    }
                } else {
                    showUnlockDialog(b)
                }
            }
            container.addView(card)
        }
    }

    // ---------- Wallpapers tab: per-bundle grids ----------

    private fun buildWallpapersTab() {
        val container = findViewById<LinearLayout>(R.id.wallpapers_list)
        container.removeAllViews()
        for (b in BundleRepository.bundles) {
            container.addView(sectionHeader(b, "wallpapers"))
            when {
                !UnlockManager.isUnlocked(this, b.id) -> {
                    container.addView(lockedCard(b, "wallpapers"))
                }
                b.wallpapers.isEmpty() -> {
                    container.addView(comingSoonView("Wallpapers arriving in the next drop."))
                }
                else -> {
                    val grid = GridLayout(this).apply { columnCount = 2 }
                    for (resId in b.wallpapers) {
                        val iv = ImageView(this).apply {
                            setImageResource(resId)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            layoutParams = GridLayout.LayoutParams().apply {
                                width = 0
                                height = dp(210)
                                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                                setMargins(dp(6), dp(6), dp(6), dp(6))
                            }
                            isClickable = true
                            isFocusable = true
                            setOnClickListener { openPreview(resId) }
                        }
                        grid.addView(iv)
                    }
                    container.addView(grid)
                }
            }
        }
    }

    // ---------- Icons tab: per-bundle sets ----------

    private fun buildIconsTab() {
        val container = findViewById<LinearLayout>(R.id.icons_list)
        container.removeAllViews()
        for (b in BundleRepository.bundles) {
            container.addView(sectionHeader(b, "icons"))
            when {
                !UnlockManager.isUnlocked(this, b.id) -> {
                    container.addView(lockedCard(b, "icon set"))
                }
                b.icons.isEmpty() -> {
                    container.addView(comingSoonView("Icon set arriving in the next drop."))
                }
                else -> {
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
                        cell.setOnClickListener { showIconDialog(b.id, icon) }
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

    private fun openPreview(resId: Int) {
        startActivity(
            Intent(this, WallpaperPreviewActivity::class.java)
                .putExtra(WallpaperPreviewActivity.EXTRA_RES_ID, resId)
        )
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
     *  system shortcut watermark badge (the Themezy trick). */
    private fun requestPinIconWidget(bundleId: String, icon: IconItem) {
        val awm = getSystemService(AppWidgetManager::class.java)
        if (awm == null || !awm.isRequestPinAppWidgetSupported) {
            Toast.makeText(this, "This launcher can't pin widgets", Toast.LENGTH_SHORT).show()
            return
        }
        val provider = ComponentName(this, IconWidgetProvider::class.java)
        val extras = Bundle().apply {
            putString("bundleId", bundleId)
            putString("label", icon.label)
        }
        awm.requestPinAppWidget(provider, extras, null)
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

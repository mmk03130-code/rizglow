package com.rsstudio.rizglow

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.rsstudio.rizglow.model.BundleRepository
import com.rsstudio.rizglow.model.IconItem
import com.rsstudio.rizglow.model.RizBundle

/** Configure a Rizglow icon widget: pick the icon it shows and the app it opens.
 *  Launched automatically when the widget is pinned; can also run standalone
 *  when the user adds the widget manually from the launcher. */
class IconWidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pickedBundle: RizBundle? = null
    private var pickedIcon: IconItem? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish()

        // Pre-pick when launched from Rizglow's "Add without watermark" flow.
        val preLabel = intent?.getStringExtra("label")
        val preBundle = intent?.getStringExtra("bundleId")
        if (preLabel != null && preBundle != null) {
            val b = BundleRepository.bundles.find { it.id == preBundle }
            val icon = b?.icons?.find { it.label == preLabel }
            if (b != null && icon != null) {
                pickedBundle = b
                pickedIcon = icon
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.bg))
            setPadding(dp(20), dp(24), dp(20), dp(20))
        }
        val title = TextView(this).apply {
            text = "Choose icon"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.text))
        }
        val sub = TextView(this).apply {
            text = "This widget looks like an app icon — no watermark badge. Tap an icon to place it."
            textSize = 13f
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(6), 0, dp(12))
        }
        root.addView(title)
        root.addView(sub)

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (b in BundleRepository.bundles) {
            if (b.icons.isEmpty()) continue
            val header = TextView(this).apply {
                text = b.name
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(b.accentColor)
                setPadding(0, dp(14), 0, dp(4))
            }
            list.addView(header)
            val grid = GridLayout(this).apply { columnCount = 4 }
            for (icon in b.icons) {
                if (IconTargets.resolve(this, icon.label) == null) continue
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(4), dp(8), dp(4), dp(8))
                    isClickable = true
                    isFocusable = true
                }
                val iv = ImageView(this).apply {
                    setImageResource(icon.resId)
                    layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
                }
                val tv = TextView(this).apply {
                    text = icon.label
                    textSize = 10f
                    setTextColor(getColor(R.color.muted))
                    gravity = Gravity.CENTER
                    maxLines = 1
                }
                cell.addView(iv)
                cell.addView(tv)
                cell.setOnClickListener { place(b, icon) }
                grid.addView(cell)
            }
            list.addView(grid)
        }
        scroll.addView(list)
        root.addView(scroll)
        setContentView(root)
    }

    private fun place(b: RizBundle, icon: IconItem) {
        IconWidgetProvider.save(this, appWidgetId, b.id, icon.label)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, result)
        finish()
    }
}

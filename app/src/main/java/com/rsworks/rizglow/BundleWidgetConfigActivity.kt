package com.rsworks.rizglow

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.rsworks.rizglow.model.BundleRepository

/** Shared configure screen for the Clock / Battery / Date widgets:
 *  pick which bundle style the widget wears. */
class BundleWidgetConfigActivity : Activity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        val widgetName = when (info?.provider?.className) {
            BatteryWidgetProvider::class.java.name -> "Battery widget"
            DateWidgetProvider::class.java.name -> "Date widget"
            else -> "Clock widget"
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.bg))
            setPadding(dp(20), dp(24), dp(20), dp(20))
        }
        root.addView(TextView(this).apply {
            text = widgetName
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.text))
        })
        root.addView(TextView(this).apply {
            text = "Pick a bundle style for this widget."
            textSize = 13f
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(6), 0, dp(12))
        })

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (b in BundleRepository.bundles) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(16).toFloat()
                    setColor(0xFF17131C.toInt())
                    setStroke(dp(2), b.accentColor)
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(12) }
                layoutParams = lp
                isClickable = true
                isFocusable = true
                gravity = Gravity.CENTER_VERTICAL
            }
            card.addView(TextView(this).apply {
                text = b.name
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(b.accentColor)
            })
            card.addView(TextView(this).apply {
                text = b.tagline
                textSize = 13f
                setTextColor(getColor(R.color.muted))
            })
            card.setOnClickListener {
                WidgetStyle.save(this, appWidgetId, b.id)
                val mgr = AppWidgetManager.getInstance(this)
                when (widgetName) {
                    "Battery widget" -> BatteryWidgetProvider.update(this, mgr, appWidgetId)
                    "Date widget" -> DateWidgetProvider.update(this, mgr, appWidgetId)
                    else -> ClockWidgetProvider.update(this, mgr, appWidgetId)
                }
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                finish()
            }
            list.addView(card)
        }
        scroll.addView(list)
        root.addView(scroll)
        setContentView(root)
    }
}

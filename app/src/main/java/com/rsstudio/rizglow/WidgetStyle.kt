package com.rsstudio.rizglow

import android.content.Context
import android.widget.RemoteViews

/** Bundle styling for the clock / battery / date widgets. */
object WidgetStyle {
    const val PREFS = "rizglow_widgets"
    private const val KEY_PREFIX = "style_"

    data class Style(val bgRes: Int, val accent: Int, val muted: Int)

    fun forBundle(bundleId: String): Style = when (bundleId) {
        "coquette_glow" -> Style(R.drawable.widget_bg_cg, 0xFFE08AAF.toInt(), 0xFF8A6B78.toInt())
        "neon_dusk" -> Style(R.drawable.widget_bg_nd, 0xFF8F83FF.toInt(), 0xFF9AA0B5.toInt())
        "dark_academia" -> Style(R.drawable.widget_bg_da, 0xFFD9A45B.toInt(), 0xFF8A7A64.toInt())
        "old_money" -> Style(R.drawable.widget_bg_om, 0xFF2E4A34.toInt(), 0xFF6B5F4C.toInt())
        "noir_streets" -> Style(R.drawable.widget_bg_ns, 0xFF13BF83.toInt(), 0xFF5F7A72.toInt())
        else -> Style(R.drawable.widget_bg_mv, 0xFFB388FF.toInt(), 0xFF9A8FA8.toInt())
    }

    fun save(context: Context, appWidgetId: Int, bundleId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PREFIX + appWidgetId, bundleId).apply()
    }

    fun load(context: Context, appWidgetId: Int): Style {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + appWidgetId, "midnight_velvet") ?: "midnight_velvet"
        return forBundle(id)
    }

    fun apply(context: Context, views: RemoteViews, appWidgetId: Int) {
        val st = load(context, appWidgetId)
        views.setInt(R.id.widget_root, "setBackgroundResource", st.bgRes)
        views.setTextColor(R.id.widget_big, st.accent)
        views.setTextColor(R.id.widget_sub, st.muted)
    }
}

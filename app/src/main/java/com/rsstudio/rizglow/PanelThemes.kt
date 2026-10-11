package com.rsstudio.rizglow

import android.content.Context

/** A control-center panel theme: background art + text colors.
 *  Ice is a light theme (dark text); the others are dark (white text). */
data class PanelTheme(
    val id: String,
    val name: String,
    val panelBg: Int,
    val tileBg: Int,
    val cardBg: Int,
    val sliderBg: Int,
    val accent: Int,      // music card + slider label color
    val textOnTile: Int,  // tile label/status color
    val dimCompact: Int,  // overlay dim behind panel (swipe mode)
    val dimFull: Int      // overlay dim behind panel (in-app mode)
)

object PanelThemes {
    private const val PREFS = "rizglow_prefs"
    private const val KEY = "panel_theme"

    val MIDNIGHT = PanelTheme(
        id = "midnight", name = "Midnight Velvet",
        panelBg = R.drawable.cc_panel_bg, tileBg = R.drawable.cc_tile_bg,
        cardBg = R.drawable.cc_card_bg, sliderBg = R.drawable.cc_slider_bg,
        accent = 0xFF6B4A8F.toInt(), textOnTile = 0xFFFFFFFF.toInt(),
        dimCompact = 0x99000000.toInt(), dimFull = 0xCC000000.toInt()
    )
    val LIQUID = PanelTheme(
        id = "liquid", name = "Liquid Glass",
        panelBg = R.drawable.cc_panel_bg_liquid, tileBg = R.drawable.cc_tile_bg_liquid,
        cardBg = R.drawable.cc_card_bg_liquid, sliderBg = R.drawable.cc_slider_bg_liquid,
        accent = 0xFFFFD9E0.toInt(), textOnTile = 0xFFFFFFFF.toInt(),
        dimCompact = 0x99000000.toInt(), dimFull = 0xCC000000.toInt()
    )
    val MATCHA = PanelTheme(
        id = "matcha", name = "Matcha",
        panelBg = R.drawable.cc_panel_bg_matcha, tileBg = R.drawable.cc_tile_bg_matcha,
        cardBg = R.drawable.cc_card_bg_matcha, sliderBg = R.drawable.cc_slider_bg_matcha,
        accent = 0xFFFFFFFF.toInt(), textOnTile = 0xFFFFFFFF.toInt(),
        dimCompact = 0x99000000.toInt(), dimFull = 0xCC000000.toInt()
    )
    val ICE = PanelTheme(
        id = "ice", name = "Ice",
        panelBg = R.drawable.cc_panel_bg_ice, tileBg = R.drawable.cc_tile_bg_ice,
        cardBg = R.drawable.cc_card_bg_ice, sliderBg = R.drawable.cc_slider_bg_ice,
        accent = 0xFF3A5A7A.toInt(), textOnTile = 0xFF2A3A4A.toInt(),
        dimCompact = 0xAA000000.toInt(), dimFull = 0xDD000000.toInt()
    )

    val ALL = listOf(MIDNIGHT, LIQUID, MATCHA, ICE)

    fun current(context: Context): PanelTheme {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, MIDNIGHT.id)
        return ALL.firstOrNull { it.id == id } ?: MIDNIGHT
    }

    fun select(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, id).apply()
    }
}

/** True when the theme uses dark text on light tiles (Ice). */
fun PanelTheme.isLight(): Boolean = textOnTile != 0xFFFFFFFF.toInt()

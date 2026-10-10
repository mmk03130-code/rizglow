package com.rsstudio.rizglow.model

import com.rsstudio.rizglow.R

/** Bundle catalog. Midnight Velvet ships with real content; other bundles unlock
 *  now and receive their artwork in the next content drop. */
object BundleRepository {

    val bundles: List<RizBundle> = listOf(
        RizBundle(
            id = "midnight_velvet",
            name = "Midnight Velvet",
            tagline = "Dark, plush & mysterious",
            accentColor = 0xFFB388FF.toInt(),
            wallpapers = listOf(
                R.drawable.wp_mv_cat,
                R.drawable.wp_mv_forest,
                R.drawable.wp_mv_velvet,
                R.drawable.wp_mv_eye,
                R.drawable.wp_mv_nebula,
                R.drawable.wp_mv_orchid
            ),
            icons = listOf(
                IconItem(R.drawable.ic_mv_phone, "Phone"),
                IconItem(R.drawable.ic_mv_messages, "Messages"),
                IconItem(R.drawable.ic_mv_camera, "Camera"),
                IconItem(R.drawable.ic_mv_photos, "Photos"),
                IconItem(R.drawable.ic_mv_clock, "Clock"),
                IconItem(R.drawable.ic_mv_settings, "Settings"),
                IconItem(R.drawable.ic_mv_music, "Music"),
                IconItem(R.drawable.ic_mv_browser, "Browser")
            )
        ),
        RizBundle(
            id = "coquette_glow",
            name = "Coquette Glow",
            tagline = "Soft pink bows & romance",
            accentColor = 0xFFF5A8C0.toInt(),
            wallpapers = emptyList(),
            icons = emptyList()
        ),
        RizBundle(
            id = "neon_dusk",
            name = "Neon Dusk",
            tagline = "City lights after dark",
            accentColor = 0xFF7C6CFF.toInt(),
            wallpapers = emptyList(),
            icons = emptyList()
        )
    )
}

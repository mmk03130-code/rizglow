package com.rsstudio.rizglow.model

/** Phase-1 bundle catalog. Real wallpaper/icon assets land in phase 2. */
object BundleRepository {

    val bundles: List<RizBundle> = listOf(
        RizBundle(
            id = "midnight_velvet",
            name = "Midnight Velvet",
            tagline = "Dark, plush & mysterious",
            wallpaperCount = 12,
            iconCount = 40,
            accentColor = 0xFFB388FF.toInt()
        ),
        RizBundle(
            id = "coquette_glow",
            name = "Coquette Glow",
            tagline = "Soft pink bows & romance",
            wallpaperCount = 12,
            iconCount = 40,
            accentColor = 0xFFF5A8C0.toInt()
        ),
        RizBundle(
            id = "neon_dusk",
            name = "Neon Dusk",
            tagline = "City lights after dark",
            wallpaperCount = 10,
            iconCount = 32,
            accentColor = 0xFF7C6CFF.toInt()
        )
    )
}

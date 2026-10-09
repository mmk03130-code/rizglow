package com.rsstudio.rizglow.model

/** One theming bundle: wallpapers + matching icons, unlockable. */
data class RizBundle(
    val id: String,
    val name: String,
    val tagline: String,
    val wallpaperCount: Int,
    val iconCount: Int,
    val accentColor: Int
)

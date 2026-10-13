package com.rsworks.rizglow.model

data class IconItem(val resId: Int, val label: String)

/** One theming bundle: wallpapers + matching icons, unlockable. */
data class RizBundle(
    val id: String,
    val name: String,
    val tagline: String,
    val accentColor: Int,
    val wallpapers: List<Int>,
    val icons: List<IconItem>
)

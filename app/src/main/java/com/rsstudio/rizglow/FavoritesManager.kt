package com.rsstudio.rizglow

import android.content.Context

/** Hearted wallpapers, persisted. The favorites shelf lives at the top of
 *  the Wallpapers tab — the "mine" collection every top theming app has. */
object FavoritesManager {
    private const val PREFS = "rizglow_favs"
    private const val KEY = "fav_wallpapers"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isFavorite(ctx: Context, resId: Int): Boolean =
        prefs(ctx).getStringSet(KEY, emptySet())?.contains(resId.toString()) == true

    fun toggle(ctx: Context, resId: Int): Boolean {
        val set = prefs(ctx).getStringSet(KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        val id = resId.toString()
        val nowFav = if (set.contains(id)) {
            set.remove(id); false
        } else {
            set.add(id); true
        }
        prefs(ctx).edit().putStringSet(KEY, set).apply()
        return nowFav
    }

    fun all(ctx: Context): List<Int> =
        prefs(ctx).getStringSet(KEY, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.sorted() ?: emptyList()
}

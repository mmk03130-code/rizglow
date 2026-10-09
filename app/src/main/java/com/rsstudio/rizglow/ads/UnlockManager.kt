package com.rsstudio.rizglow.ads

import android.content.Context

/**
 * Monetization seam for Rizglow.
 *
 * Every bundle is either locked or unlocked. The FIRST bundle ("midnight_velvet")
 * is always free so users fall in love; the rest unlock via rewarded ads.
 *
 * AdMob integration (later, free account):
 *   1. Add play-services-ads dependency + AdMob app ID to the manifest.
 *   2. In MainActivity.showUnlockDialog, load a RewardedAd with the bundle's
 *      ad unit ID and call UnlockManager.unlock() in onUserEarnedReward().
 * Nothing else changes — all lock checks already go through here.
 */
object UnlockManager {

    private const val PREFS = "rizglow_unlocks"
    private const val FREE_BUNDLE_ID = "midnight_velvet"

    fun isUnlocked(ctx: Context, bundleId: String): Boolean {
        if (bundleId == FREE_BUNDLE_ID) return true
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key(bundleId), false)
    }

    /** Called after the user earns the reward (or via the dev shortcut for now). */
    fun unlock(ctx: Context, bundleId: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key(bundleId), true).apply()
    }

    private fun key(bundleId: String) = "unlocked_$bundleId"
}

package com.rsstudio.rizglow.ads

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Monetization for Rizglow.
 *
 * Every bundle is either locked or unlocked. The FIRST bundle ("midnight_velvet")
 * is always free; the rest unlock via a rewarded ad — one watched video per
 * bundle, unlocked forever.
 *
 * Ads run on Google's OFFICIAL TEST ad units (see AdUnits.kt), so this build
 * serves test ads only. Swapping in real AdMob IDs later is a drop-in.
 */
object UnlockManager {

    private const val PREFS = "rizglow_unlocks"
    private const val FREE_BUNDLE_ID = "midnight_velvet"

    fun isUnlocked(ctx: Context, bundleId: String): Boolean {
        if (bundleId == FREE_BUNDLE_ID) return true
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key(bundleId), false)
    }

    /** Called after the user earns the reward. Persists across restarts. */
    fun unlock(ctx: Context, bundleId: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key(bundleId), true).apply()
    }

    /**
     * Show the unlock prompt. "Watch ad" loads a rewarded ad for this
     * bundle's slot; a completed view unlocks the bundle permanently.
     * If the ad fails to load (offline etc.), the user is told to retry —
     * no free unlock, no dead end.
     */
    fun promptUnlock(ctx: Context, bundleId: String, onUnlocked: (() -> Unit)? = null) {
        val activity = ctx as? Activity ?: run {
            // Rewarded ads need an Activity to show. Never unlock here —
            // doing so would be a free bypass around the rewarded video.
            // (All current call sites pass an Activity, so this path should
            // never fire; it exists as a guard for future call sites.)
            android.util.Log.w("UnlockManager",
                "promptUnlock called with non-Activity context; unlock refused")
            Toast.makeText(ctx, "Open Rizglow to unlock bundles",
                Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(ctx)
            .setTitle("Unlock this bundle")
            .setMessage("Watch a short ad to unlock all wallpapers and icons in this bundle, forever.")
            .setPositiveButton("Watch ad") { d, _ ->
                d.dismiss()
                Toast.makeText(ctx, "Loading ad…", Toast.LENGTH_SHORT).show()
                loadAndShow(activity, bundleId, onUnlocked)
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun loadAndShow(
        activity: Activity,
        bundleId: String,
        onUnlocked: (() -> Unit)?
    ) {
        val adUnitId = AdUnits.forBundle(bundleId)
        RewardedAd.load(activity, adUnitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdFailedToShowFullScreenContent(e: AdError) {
                            Toast.makeText(activity,
                                "Ad couldn't play — try again", Toast.LENGTH_SHORT).show()
                        }
                    }
                    ad.show(activity, OnUserEarnedRewardListener {
                        unlock(activity, bundleId)
                        Toast.makeText(activity,
                            "Bundle unlocked! 🎉", Toast.LENGTH_SHORT).show()
                        onUnlocked?.invoke()
                    })
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Toast.makeText(activity,
                        "Ad didn't load (check connection) — try again",
                        Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun key(bundleId: String) = "unlocked_$bundleId"
}

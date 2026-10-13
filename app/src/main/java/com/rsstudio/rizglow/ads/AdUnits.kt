package com.rsstudio.rizglow.ads

/**
 * AdMob ad unit slots — one labeled slot per unlock placement.
 *
 * CURRENT STATE: all slots hold Google's OFFICIAL TEST ad unit IDs, so the
 * app serves test ads only. No real impressions, no revenue, no policy risk.
 *
 * GOING LIVE (when the free AdMob account exists):
 *   1. Create one Rewarded ad unit per bundle in the AdMob console.
 *   2. Replace each TEST_REWARDED value below with the real ad unit ID
 *      for that bundle's slot. Nothing else changes — the unlock flow
 *      reads from here.
 *   3. Replace the test App ID in AndroidManifest.xml with the real one.
 */
object AdUnits {

    /** Google's official test App ID (manifest). Replace with real on launch. */
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    /** Google's official test Rewarded ad unit. Replace per-slot on launch. */
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    /**
     * One slot per locked bundle. Swap each value for the real ad unit ID
     * from the AdMob console when going live — the flow needs no rebuild.
     */
    private val bundleSlots: Map<String, String> = mapOf(
        "coquette_glow"  to TEST_REWARDED, // slot: coquette_glow_unlock
        "neon_dusk"      to TEST_REWARDED, // slot: neon_dusk_unlock
        "web_slinger"    to TEST_REWARDED, // slot: web_slinger_unlock
        "neo_tokyo"      to TEST_REWARDED, // slot: neo_tokyo_unlock
        "cherry_bow"     to TEST_REWARDED, // slot: cherry_bow_unlock
        "street_phantom" to TEST_REWARDED, // slot: street_phantom_unlock
        "midnight_academy" to TEST_REWARDED, // slot: midnight_academy_unlock
        "midnight_drive" to TEST_REWARDED, // slot: midnight_drive_unlock
        "ghost_protocol" to TEST_REWARDED, // slot: ghost_protocol_unlock
        "dark_academia" to TEST_REWARDED, // slot: dark_academia_unlock
        "old_money" to TEST_REWARDED, // slot: old_money_unlock
        "noir_streets" to TEST_REWARDED  // slot: noir_streets_unlock
    )

    /** Ad unit ID for a bundle's unlock placement. Falls back to the test ID. */
    fun forBundle(bundleId: String): String =
        bundleSlots[bundleId] ?: TEST_REWARDED

    /** True while every slot still holds a test ID (pre-launch state). */
    fun isTestMode(): Boolean = bundleSlots.values.all { it == TEST_REWARDED }
}

# AdMob Real-ID Swap Guide

**When:** the free AdMob account exists (one-time setup, $0).

**What changes:** 10 small ID swaps. No code flow changes, no rebuild of the
unlock logic.

## Steps

1. **AdMob console → Apps → Add app** (Android, `com.rsstudio.rizglow`).
   Copy the **App ID** (looks like `ca-app-pub-XXXX~YYYY`).

2. **In the app**, open `app/src/main/AndroidManifest.xml` and replace the
   test App ID in the `APPLICATION_ID` meta-data with the real one:
   - Old: `ca-app-pub-3940256099942544~3347511713`
   - New: your real App ID

3. **AdMob console → Ad units → Create** one **Rewarded** ad unit per
   locked bundle (9 total). Name each after its slot:
   - `coquette_glow_unlock`
   - `neon_dusk_unlock`
   - `web_slinger_unlock`
   - `neo_tokyo_unlock`
   - `cherry_bow_unlock`
   - `street_phantom_unlock`
   - `midnight_academy_unlock`
   - `midnight_drive_unlock`
   - `ghost_protocol_unlock`

4. **In the app**, open
   `app/src/main/java/com/rsstudio/rizglow/ads/AdUnits.kt` and replace
   each slot's `TEST_REWARDED` value with that bundle's real ad unit ID
   (looks like `ca-app-pub-XXXX/YYYYYYYYYY`).

5. Build, sign, and publish as usual. Done.

## Notes

- `AdUnits.isTestMode()` returns true while every slot holds a test ID —
  handy for a pre-launch sanity check.
- Test on a real device with your own device registered as a test device
  in AdMob, or you'll generate real impressions during QA.
- Never click your own live ads — that's the fastest way to get the
  AdMob account limited.

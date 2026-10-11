# Free Resources for Rizglow (no credit required)

> Research done 2026-10-11. Every license below was verified from the actual source (repo LICENSE file, official help center, or official license page). **Rule of thumb:** MIT/Apache/BSD/CC0 = safe. MIT/Apache/BSD require shipping the license *text* in the app (an "Open source licenses" screen — standard, not visible credit). CC0/public domain = no strings at all.

## ⭐ Grab these first (top 5)

1. **Pexels** — https://www.pexels.com/ — *Pexels License (verified via help.pexels.com)*: all photos/videos free for commercial use, **no attribution required**. Free API (200 req/hr). → Bulk wallpaper sourcing for new bundles.
2. **Google Material Symbols** — https://github.com/google/material-design-icons — *Apache 2.0 (verified)*: 15,000+ icons, commercial OK, no on-screen credit needed (ship the license text). → Panel tiles, in-app UI icons.
3. **BAOSP Panel** — https://github.com/tech-master33/baosp-panel — *Apache 2.0 (verified from repo)*: floating overlay control panel (torch, volume, speech rate) via SYSTEM_ALERT_WINDOW. → Closest open-source match to our swipe-panel architecture; study/adapt patterns.
4. **mini-time-widget** — https://github.com/bossincrypto/mini-time-widget — *MIT (verified)*: minimalist Kotlin clock widget using RemoteViews + TextClock, per-widget prefs, GitHub Actions releases. → Directly mirrors our widget approach; great reference for new widget types.
5. **Lucide** — https://lucide.dev — *ISC/MIT (verified)*: 1,800+ beautiful consistent icons, no attribution. → Icon pack artwork inspiration + UI icons.

---

## 🖼️ Wallpapers

| Resource | URL | License (verified) | What it offers | Rizglow use |
|---|---|---|---|---|
| Pexels | https://www.pexels.com/ | Pexels License — commercial OK, no attribution (help.pexels.com) | 5M+ photos + videos, free API, curated collections | Bulk-source new wallpaper bundles (aesthetic, abstract, nature) |
| Unsplash | https://unsplash.com/ | Unsplash License — commercial OK, no attribution for direct downloads | 5M+ photos, curated collections | Same as Pexels. ⚠️ Don't use the API in-app (API terms require attribution + download tracking); download directly instead. Also don't re-publish photos as a competing stock service. |
| Public Domain Pictures | https://www.publicdomainpictures.net/ | CC0 — no attribution | Large CC0 photo library | Extra wallpaper sourcing, fully public domain |
| walls-callection | https://github.com/nikichashadow-code/walls-callection | CC0 (repo claims CC0; README warns individual images may vary) | Curated personal wallpaper collection | Browse for style ideas; verify per-image before shipping |
| Pixabay | https://pixabay.com/ | Pixabay License — commercial OK, no attribution (NOT CC0 anymore — verify current terms) | Photos, vectors, illustrations, video | Backup source; vectors useful for bundle artwork |

## 🎨 Icons

| Resource | URL | License (verified) | What it offers | Rizglow use |
|---|---|---|---|---|
| Material Symbols | https://github.com/google/material-design-icons | Apache 2.0 — commercial OK, no UI credit | 15,000+ icons, variable font (weight/fill/size axes) | Panel tiles, settings UI, widget icons |
| Lucide | https://lucide.dev | ISC/MIT — no attribution | 1,800+ consistent premium icons | Icon pack artwork base + in-app icons |
| Tabler Icons | https://tabler.io/icons | MIT — no attribution | 6,100+ icons | Same as Lucide; huge set for bundle variety |
| Phosphor Icons | https://phosphoricons.com | MIT — no attribution | 7,000+ icons, multiple weights | Same; weights useful for matching bundle styles |
| Simple Icons | https://simpleicons.org | CC0 — no attribution | 3,200+ brand/app glyphs (SVG) | Social app icons for the icon pack. ⚠️ Trademark law still applies to logos — using a brand's glyph as its app icon is standard icon-pack practice, but never imply endorsement. |
| public-domain-icons | https://github.com/apancik/public-domain-icons | Public domain (verified from repo) | Cleaned SVG pictograms (AIGA, NPS, public sets) | Generic utility icons, no strings attached |
| CandyBar | https://github.com/zixpo/candybar | Apache 2.0 (verified) | Full icon-pack dashboard library for Android | Reference for icon-pack app architecture (we already have our own, but useful patterns) |
| IconShowcase | https://github.com/jahirfiquitiva/IconShowcase | Apache 2.0 (verified) | Material Design icon-pack showcase dashboard | Same — dashboard/UI patterns for the Icons tab |

## 🎛️ Control center / panel code

| Resource | URL | License (verified) | What it offers | Rizglow use |
|---|---|---|---|---|
| BAOSP Panel | https://github.com/tech-master33/baosp-panel | Apache 2.0 (verified from repo README) | Floating overlay quick-access panel (torch, volume, screen-reader status), SYSTEM_ALERT_WINDOW, CI-built APKs | Study their overlay service patterns; closest open equivalent to our swipe panel |
| kyf42launcher | https://github.com/spavikevik/kyf42launcher | BSD 3-Clause (verified from repo) | Control center w/ torch, brightness (WRITE_SETTINGS), ringer/DND tiles + **NotificationListenerService** panel + glass tile drawables | Reference for real music/notification info via NotificationListenerService (our roadmap item!) and tile state patterns |

## 🧩 Widgets

| Resource | URL | License (verified) | What it offers | Rizglow use |
|---|---|---|---|---|
| mini-time-widget | https://github.com/bossincrypto/mini-time-widget | MIT (verified) | Minimalist clock widget, Kotlin, RemoteViews + TextClock, per-widget prefs, config activity | Template for new clock/date widget variants |
| world-clock-widget | https://github.com/ddobrinskiy/world-clock-widget | MIT (verified) | World clock widget built with Jetpack Glance, Material 3 | Glance-based widget patterns (modern approach) |
| glass-widget | https://github.com/fyildirim-debug/glass-widget | MIT (verified) | Boxless clock + weather widget, zero dependencies, ~880KB APK, Open-Meteo (no API key) | Weather widget idea for later; boxless aesthetic matches theming |
| UniBattery | https://github.com/ianbakeland/unibattery | MIT (verified) | Battery widget for Bluetooth devices, Jetpack Compose | Battery widget patterns beyond the phone battery |
| DashClock | https://github.com/romannurik/dashclock | Apache 2.0 (verified) | Classic extensible lock/home-screen widget by a Google engineer | Widget extension architecture ideas |
| SpotifyNowPlaying | https://github.com/scen/SpotifyNowPlaying | MIT (verified, full text in README) | Minimalist Spotify now-playing home-screen widget | Music widget patterns — relevant when we add the NotificationListener for real track info |

## 📚 General / awesome lists

| Resource | URL | License (verified) | What it offers | Rizglow use |
|---|---|---|---|---|
| free-icon-resources | https://github.com/noahelhadedy/free-icon-resources | CC0 (the list itself; individual sets vary — check each) | Curated list of 150+ free icon resources | Discovery hub when we need a new icon style |
| awesome-cc0 | https://github.com/madjin/awesome-cc0 | CC0 | Curated CC0 art, 3D, textures, fonts | Textures/backgrounds for future bundles |
| design-resources-for-developers | https://github.com/bradtraversy/design-resources-for-developers | Mixed (list only — verify each resource) | Stock photos, patterns, gradient/mesh generators | Pattern/gradient generators for bundle artwork |

---

## 🚫 Rejected (do NOT use without visible credit)

- **Font Awesome Free** — CC BY 4.0 for icons → attribution required. Skip.
- **FoliumCreations/Wallpapers** — CC-BY → attribution required. Skip.
- **mylinuxforwork/wallpaper** — GPL-2.0 → copyleft, wrong fit for an app's asset bundle. Skip.
- **omniclock / 10-bit Clock Widget** — GPL-3.0 → copyleft; can't borrow code into a Play Store app without open-sourcing. Skip.
- **Freepik / Flaticon / Icons8 / Noun Project (free tiers)** — all require visible attribution/backlinks. Skip.

## ⚠️ License-unverified (promising, verify before using)

- **makccr/wallpapers** (https://github.com/makccr/wallpapers) — large 4K wallpaper repo, no license file spotted → verify per-image or skip.
- **modern-clock-android** (https://github.com/dark-witcher/modern-clock-android) — nice clock widget, license not confirmed in this pass → check repo before borrowing.

## Notes for the build

- **MIT/Apache/BSD in-app:** add one "Open source licenses" screen (or About section) with the license texts. That's the entire obligation — no visible credit in the UI.
- **Unsplash/Pexels photos as wallpapers:** download directly (don't embed their APIs); don't sell the raw photos; Pexels/Unsplash both forbid implying endorsement by depicted people/brands.
- **AI-generated art** (our current bundles) remains the safest path for signature styles — these sources are for *expansion* (photos for wallpaper bundles, glyphs for icon packs, code patterns for panels/widgets).

# Responsive Sizing for Rizglow's Floating Panel

**The question:** our swipe panel uses fixed dp sizes (320dp wide, 70dp tiles, 190dp sliders). Does it work on every phone? What breaks where?

**Short answer:** it works on ~95% of phones today, overflows on the smallest (~308dp with large-text accessibility), and looks lost on tablets/foldables. The fix is one formula + a config-change listener. This doc has the math, the reasoning, and the code.

---

## Part 1 — How wide is a phone, really? (verified numbers)

Android layouts don't use pixels — they use **dp** (density-independent pixels). A 1080px-wide screen at 420dpi is only **411dp** wide in layout terms. Here's the real range, verified from current device data:

| Device class | Example | Width in dp |
|---|---|---|
| Smallest realistic phone | 720×1520 at 320dpi, or any phone with Android's "Display size: Large" accessibility setting | **308–360dp** |
| Mainstream Android phone | Pixel 7, Galaxy S23/S24 | **360–412dp** |
| Large / low-density phone | Pixel 8 Pro (360dpi), phones with "Display size: Small" | **430–448dp** |
| Power users (developer "Minimum width") | Any phone, user-tweaked | up to **~500dp+** |
| Foldable closed (outer screen) | Galaxy Z Fold outer | **~443dp** |
| Tablet portrait | 10" tablet | **~800dp** |
| Foldable open (inner screen) | Galaxy Z Fold inner | **~851dp** |

**The key insight:** 99%+ of phones in portrait are between **360dp and 448dp**. Google's own Material 3 guidance says "Compact width (< 600dp) = 99.96% of phones in portrait." But the **308dp edge case is real** — it's what happens when a user turns on Android's large-display accessibility setting, and those users are exactly the ones who need big touch targets.

Sources: [Material 3 window size classes](https://developer.android.com/develop/ui/views/layout/use-window-size-classes?authuser=8), [mobile screen size reference](https://stax.tools/blog/mobile-screen-sizes-guide)

### Material 3 size classes (the industry standard)

Google's official breakpoints — worth memorizing:

| Class | Width | What it means |
|---|---|---|
| **Compact** | < 600dp | Phones in portrait |
| **Medium** | 600–840dp | Tablet portrait, unfolded foldables |
| **Expanded** | 840–1200dp | Tablet landscape |
| **Large / Extra-large** | 1200dp+ | Big tablets, desktops |

Our panel only needs to care about **Compact vs. everything else**.

---

## Part 2 — What breaks with our fixed sizes? (the math)

Current v1.7 values:
- Panel: **320dp** wide, centered, with **16dp** outer padding each side → total footprint **352dp**
- Tiles: **70dp** tall, 4 per row, 10dp gaps
- Sliders: **190dp** tall cards
- Music card: full width

### The overflow test

| Screen width | 352dp footprint | Verdict |
|---|---|---|
| 308dp (large-text accessibility) | **44dp too wide** | ❌ BROKEN — panel gets cut off both sides |
| 360dp (small phone) | fits, 8dp margin each side | ⚠️ Works but cramped — violates the 16dp minimum margin rule |
| 411dp (mainstream) | fits, ~30dp margin each side | ✅ Fine |
| 448dp (large phone) | fits | ✅ Fine |
| 800dp (tablet) | fits, 224dp empty each side | ⚠️ Looks lost — a tiny island in a big dim |
| 851dp (foldable open) | fits | ⚠️ Same "lost" problem |

### Tiles and sliders

- **70dp tiles:** 4 tiles share (320 − 36 padding − 30 gaps) = 254dp → ~63dp each. Fine everywhere the panel fits. No breakage.
- **190dp sliders:** height-only, never overflows horizontally. Fine.
- **The real risk is only the panel width.** Everything inside scales with it (tiles use `weight=1f`).

**Bottom line:** one number is wrong — the fixed 320dp panel width. Fix that one formula and everything else follows.

---

## Part 3 — How the pros make floating panels adapt

### Why overlays are special

Normal apps use **resource qualifiers** (`layout-sw600dp/`) — Android picks the right XML automatically. But our panel is added via `WindowManager.addView()` from a Service. **Resource qualifiers don't apply** — there's no Activity, no resource resolution. We must **compute sizes in code** at the moment we show the panel.

This is exactly what premium floating apps do. The pattern:

1. **Read the real screen size** when the panel is about to show (not once at startup — it can change).
2. **Compute panel width as a function** of screen width: `min(MAX, screenWidth - MARGIN)`.
3. **Recompute on configuration change** (rotation, fold/unfold, multi-window).

### The percentage rule

Premium floating panels almost never use a fixed width. The standard formula:

```
panelWidth = min(360dp, screenWidthDp - 48dp)
```

Why these numbers:
- **screenWidth − 48dp** → guarantees 24dp margin on each side on every phone. On a 308dp screen: 260dp panel. On 411dp: 363dp → capped.
- **360dp cap** → beyond this, a control panel gets unwieldy. Thumbs can't reach across it, and it stops looking like a "panel" and starts looking like a "page." (This is the same reason Material dialogs cap at ~560dp and bottom sheets at ~640dp.)
- On tablets/foldables (600dp+), the cap keeps it at 360dp centered — intentional. A floating control panel should stay thumb-sized, not stretch.

### Getting screen size from a Service (the modern way)

`Display.getMetrics()` is **deprecated** (API 30+) and lies in multi-window. The correct API:

```kotlin
/** Screen width in dp, safe to call from a Service on any API level. */
fun screenWidthDp(context: Context): Int {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val density = context.resources.displayMetrics.density
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // API 30+: WindowMetrics gives the real current bounds.
        val bounds = wm.currentWindowMetrics.bounds
        (bounds.width() / density).toInt()
    } else {
        // API 26-29: DisplayMetrics still works (our minSdk is 26).
        val dm = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        (dm.widthPixels / dm.density).toInt()
    }
}
```

> **Teaching note:** `currentWindowMetrics` (not `maximumWindowMetrics`) is the right one here — it reflects the *current* state, so it updates when a foldable unfolds. `maximumWindowMetrics` returns the largest *possible* size, which would give the wrong answer on a closed foldable.

### The sizing function

```kotlin
/** Panel width: 24dp margins on phones, capped at 360dp on big screens. */
fun panelWidthDp(context: Context): Int {
    val sw = screenWidthDp(context)
    return minOf(360, sw - 48).coerceAtLeast(200) // 200dp absolute floor
}
```

Check it against the table:

| Screen | Panel width | Margins |
|---|---|---|
| 308dp | 260dp | 24dp each side ✅ |
| 360dp | 312dp | 24dp each side ✅ |
| 411dp | 360dp (capped) | ~25dp each side ✅ |
| 800dp tablet | 360dp (capped) | centered ✅ |
| 851dp foldable open | 360dp (capped) | centered ✅ |

Every case works. One formula, zero branches.

---

## Part 4 — Foldables: what actually happens to our overlay

### The scenario

User has our panel open (or the strip armed) on a Galaxy Z Fold. They unfold it. Screen width jumps **443dp → 851dp**.

### What happens to our windows

1. **The strip** (`y = statusBarHeight, 40dp tall, 40% width`): it's positioned with `gravity = TOP | END` and width computed as 40% of widthPixels **at attach time**. After unfolding, 40% was computed against 443dp — the strip stays that pixel width. It's not broken (still top-right), just proportionally narrower. Minor.

2. **The panel overlay** (`MATCH_PARENT`): the *window* automatically stretches to the new screen size — `MATCH_PARENT` windows follow configuration changes. But our **inner card is fixed 320dp**, so on the unfolded 851dp screen it sits as a small centered card. Not broken, just small. With the new formula it becomes 360dp — slightly better, still intentionally compact.

3. **The service gets `onConfigurationChanged()`**: yes — Services receive configuration changes (unlike what some old Stack Overflow answers claim). This is our hook to rebuild or resize.

### What we should do

In `ControlCenterAccessService`:

```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // If the panel is showing, rebuild it at the new size.
    // Simplest correct approach: hide and re-show.
    if (panelView != null) {
        hidePanel()
        showPanel()
    }
    // Recompute strip width (it was 40% of the old width).
    strip?.let {
        try { wm.removeView(it) } catch (e: Exception) {}
        strip = null
        addStrip() // re-adds at 40% of the NEW width
    }
}
```

> **Teaching note:** hide-and-reshow is deliberately simple. The alternative — `wm.updateViewLayout()` with new LayoutParams plus rebuilding the inner card — is more code for zero visible benefit, because the panel is transient (users open it, tap something, dismiss it). Nobody unfolds mid-panel-tap. Simple wins.

**Do we need Jetpack WindowManager / FoldingFeature?** No. That's for apps that do dual-pane layouts across the hinge (Gmail-style). Our panel is a centered floating card — it doesn't care where the hinge is. `onConfigurationChanged` is enough.

---

## Part 5 — Concrete recommendation for Rizglow

### Changes needed (all in code, no XML)

**1. Panel width → computed** (in `showPanel()`):

```kotlin
// OLD: fixed 320dp in ControlCenterPanelView init
// NEW: compute at show time
fun panelWidthDp(context: Context): Int {
    val sw = screenWidthDp(context)
    return minOf(360, sw - 48).coerceAtLeast(200)
}
```

Pass it into `ControlCenterPanelView` as a parameter (or set the card width after construction). The inner ScrollView currently uses `dp(320)` — replace with `dp(panelWidthDp(context))`.

**2. Tiles → already adaptive.** They use `weight = 1f`, so they split whatever width they're given. No change needed. (This was a good original decision.)

**3. Sliders → cap, don't scale.** 190dp tall is fine on all phones. On very short screens (landscape, ~360dp tall), the panel scrolls — acceptable. Optional: reduce to 160dp when `screenHeightDp < 600`. Low priority.

**4. Strip → recompute on config change** (see Part 4 snippet). One small addition to the service.

**5. Dim → keep MATCH_PARENT.** The dim is the full-screen window; it always fits by definition.

### What NOT to do

- ❌ Don't add `layout-sw600dp` XML — it doesn't apply to WindowManager overlays.
- ❌ Don't use `maximumWindowMetrics` — wrong answer on closed foldables.
- ❌ Don't stretch the panel on tablets — a 600dp control panel is unusable. Cap at 360dp.
- ❌ Don't use Jetpack WindowManager just for this — `onConfigurationChanged` covers it.

### Priority order

1. **P0:** `panelWidthDp()` formula — fixes the 308dp overflow (real users, accessibility setting).
2. **P1:** `onConfigurationChanged` in the service — foldables + rotation correctness.
3. **P2:** Height guard for landscape — only if testing shows the panel overflows vertically in landscape.

### Test checklist (no foldable needed)

- [ ] Developer options → **Minimum width: 308dp** → open panel → must fit with margins
- [ ] Minimum width: 411dp (default) → panel should be 360dp centered
- [ ] Rotate to landscape → panel rebuilds, still fits
- [ ] Split-screen mode → panel uses the app's window width, not full screen (this is why `currentWindowMetrics` matters)

---

## Sources

- [Material 3 window size classes](https://developer.android.com/develop/ui/views/layout/use-window-size-classes?authuser=8) — official breakpoints
- [Mobile screen sizes reference](https://stax.tools/blog/mobile-screen-sizes-guide) — real device dp widths
- [Android multi-window metrics](https://developer.android.com/develop/ui/compose/layouts/adaptive/support-multi-window-mode?authuser=3) — `WindowMetrics`, deprecated `Display` methods
- [Real device geometry profiles](https://github.com/mobiumdev/mobium/blob/HEAD/docs/FORMFLUX.md) — Pixel 7/8 Pro, foldables, display-size-large edge case

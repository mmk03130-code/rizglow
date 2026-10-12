# Control Panel Teardown — Findings (2026-10-12)

Research job: decode how top control-center panels are actually built —
layout code, screen behavior, responsive sizing, visual design.
Sources: AceTools APK v1.3.4 decoded via apktool (package
easycontrol.controlcenter.controlpanel.simple), open-source repos
(Smart Edge, Prism, Orbis), iOS Control Center specs, Material 3.

## 1. AceTools — exact numbers from their layout XMLs

**Module grid unit system**
- 1×1 tile = **75dp** square (`FrameLayoutMenuButton`, custom view, icon centered)
- 2×2 module = **152dp** square container (≈ 2×75 + 2dp gap)
- 2×2 connectivity: 152dp container, **18dp corner radius**, 16dp padding,
  four **52dp circles** inside at the four corners (16dp internal gaps)
- Labels: caption2, max 2 lines, 8dp below module

**Module visuals**
- Container fill: flat black @ 60% (`#99000000`) — NO gradient, NO stroke
- Circle buttons: flat black @ 40% (`#66000000`), monochrome glyphs, 14dp icon padding
- The "premium" comes from **real blur behind** the flat fill, not from the fill itself

**Architecture (from earlier teardown)**
- `PanelHolderActivity`, custom `FrameLayoutMenuButton` /
  `ConstraintLayoutMenuButton` — buttons render their own on/off state internally
- Separate iOS 17 / iOS 18 / Samsung style templates; 1×1, 1×2, 2×1, 2×2 forms

## 2. Open-source mechanics (Smart Edge / Prism / Orbis)

- **Full-screen MATCH_PARENT overlay root**, panel card gravity-positioned
  inside it (hugs screen edge) — NOT a panel-sized window. Fixes notch/cutout issues.
- **Runtime height budget**: `screenHeightDp − chromeDp − 24dp margin`,
  enforced with constrainedHeight — never fixed dp heights.
- **RecyclerView + GridLayoutManager** for tiles; dynamic span count.
- **Spring open animation**: stiffness ~400, no-bounce damping, 200ms alpha fade,
  tiles stagger in 18ms apart; press feedback = scale 0.96 + haptic.
- **Glass recipe**: 1dp white hairline stroke (15–30% alpha) + dark gradient,
  over a **frozen screenshot backdrop** (every Android iOS-style CC fakes blur
  this way — no live cross-window blur exists).
- Edge handle: 6×140dp strip, ~16dp swipe threshold, vertical-dominance rejection.

## 3. Design anatomy (iOS CC + M3)

- iOS CC is a **full-screen overlay**, not a floating card. Module grid:
  2×2 connectivity module, wide music module, **tall vertical sliders in the
  right column**, circular toggles.
- **Radius rhythm**: container 20–28dp → inner modules 16–20dp → buttons circular.
  Rule: inner radius = outer radius − padding. Never mix circles + squircles in one row.
- **#1 premium/flat divider**: real blur vs flat grey overlay.
- 8dp spacing rhythm; 12–16dp screen margins; 44dp min touch targets;
  press scale ~0.97; transitions 200–350ms.

## 4. Gap analysis — why Rizglow's panel doesn't feel the same

| # | Them | Us (ControlCenterPanelView) |
|---|------|------------------------------|
| 1 | 2D module grid (75dp units) | Single-column vertical stack of full-width rows — reads as settings page |
| 2 | Fixed viewport, everything visible | ScrollView — scrolling feels app-like, not system-like |
| 3 | Edge-hugging / full-screen overlay | Centered floating card, max 400dp |
| 4 | Flat 60% black + real blur + hairline highlight | Glass drawables (check for hairline stroke) |
| 5 | Spring open, staggered tiles, press scale | No entrance or press motion at all |
| 6 | Tall vertical sliders in right column (iOS) | Horizontal row of two slider cards |
| 7 | Shape-consistent (all circles / all modules) | Circles + full-width cards + slider cards mixed |

## 5. Rebuild spec (proposed v3.6)

1. Restructure panel as **module grid**: 2×2 connectivity module (152dp),
   2×1 music module, right column with two tall vertical sliders,
   grid of circular 1×1 toggles below.
2. **Remove ScrollView**; compute height budget at runtime
   (`screenHeight − chrome − 24dp`); compact mode must fit without scrolling.
3. Overlay root → **full-screen MATCH_PARENT** with panel gravity-positioned
   (keep swipe-from-edge trigger).
4. Glass: verify/add **1dp white hairline stroke** on module backgrounds;
   add frozen-screenshot blur backdrop behind panel.
5. Motion: spring open (stiffness 400), tiles stagger 18ms, press scale 0.96.
6. Keep: per-button internal on/off rendering, 4 themes, vector icons.

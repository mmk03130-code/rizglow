#!/usr/bin/env python3
"""Ingest one Rizglow content bundle from source art into the app tree.

Usage:
  python3 tools/make-bundle.py --id vanilla_lace --code vl \\
      --name "Vanilla Lace" --tagline "Clean-girl neutrals" \\
      --accent 0xFFE8CFA0 --muted 0xFF9A8B7A \\
      --widget-bg "#F5EFE6" --widget-stroke "#E8CFA0" \\
      --src docs/design-research/staging/vanilla_lace

Source layout (inside --src):
  wallpapers/   6 files (any of .webp/.png/.jpg), stems become wp_<code>_<stem>
  icons/        files named <app>.webp/.png/.svg, <app> in the 34 canonical names below

What it does:
  1. Resizes wallpapers to 1080x1920 (center-crop) -> app/src/main/res/drawable-nodpi/wp_<code>_*.webp
  2. Resizes icons to <=512px (keeps 1600px sources from bloating the APK) -> ic_<code>_<app>.webp
  3. Writes app/src/main/res/drawable/widget_bg_<code>.xml
  4. Prints the BundleRepository.kt block + WidgetStyle.kt branch to paste in,
     and saves them to tools/bundle_<code>.kt.txt
  5. Reports bytes added (APK-size watch: 3 launch bundles = ~12MB).

Conventions mirror the existing bundles (mv/cg/nd). Nothing is committed;
paste the Kotlin, build, and push via the normal flow.
"""
import argparse, os, re, shutil, sys, tempfile
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DRAW = REPO / "app/src/main/res/drawable-nodpi"
DRAW_XML = REPO / "app/src/main/res/drawable"

ICON_APPS = [
    ("phone", "Phone"), ("messages", "Messages"), ("camera", "Camera"),
    ("photos", "Photos"), ("clock", "Clock"), ("settings", "Settings"),
    ("music", "Music"), ("browser", "Browser"), ("maps", "Maps"),
    ("mail", "Mail"), ("calendar", "Calendar"), ("contacts", "Contacts"),
    ("calculator", "Calculator"), ("files", "Files"), ("video", "Video"),
    ("games", "Games"), ("recorder", "Recorder"), ("weather", "Weather"),
    ("notes", "Notes"), ("compass", "Compass"), ("reminders", "Reminders"),
    ("health", "Health"), ("wallet", "Wallet"), ("favorites", "Favorites"),
    ("whatsapp", "WhatsApp"), ("whatsapp_business", "WhatsApp Business"),
    ("instagram", "Instagram"), ("tiktok", "TikTok"), ("facebook", "Facebook"),
    ("x", "X"), ("snapchat", "Snapchat"), ("pinterest", "Pinterest"),
    ("telegram", "Telegram"), ("google", "Google"),
]
APP_NAMES = dict(ICON_APPS)

IMG_EXTS = {".webp", ".png", ".jpg", ".jpeg"}

def sanitize(s: str) -> str:
    s = s.lower().strip()
    s = re.sub(r"[^a-z0-9_]+", "_", s).strip("_")
    s = re.sub(r"_+", "_", s)
    if s and s[0].isdigit():
        s = "w" + s
    return s[:40] or "art"

def load_image(path: Path):
    from PIL import Image
    im = Image.open(path).convert("RGB")
    return im

def center_crop_resize(im, tw, th):
    sw, sh = im.size
    scale = max(tw / sw, th / sh)
    im = im.resize((int(sw * scale + 0.5), int(sh * scale + 0.5)))
    x = (im.width - tw) // 2
    y = (im.height - th) // 2
    return im.crop((x, y, x + tw, y + th))

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--id", required=True, help="bundle id, e.g. vanilla_lace")
    ap.add_argument("--code", required=True, help="2-letter drawable prefix, e.g. vl")
    ap.add_argument("--name", required=True)
    ap.add_argument("--tagline", required=True)
    ap.add_argument("--accent", required=True, help="0xFF.... for bundle accent")
    ap.add_argument("--muted", required=True, help="0xFF.... for widget sub text")
    ap.add_argument("--widget-bg", required=True, help="#...... widget bg solid")
    ap.add_argument("--widget-stroke", required=True, help="#...... widget stroke")
    ap.add_argument("--src", required=True, help="staging dir with wallpapers/ and icons/")
    ap.add_argument("--icon-size", type=int, default=512)
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()

    if not re.fullmatch(r"[a-z][a-z0-9_]*", a.id):
        sys.exit("bad --id (lowercase, digits, underscores)")
    if not re.fullmatch(r"[a-z]{2}", a.code):
        sys.exit("bad --code (exactly 2 lowercase letters)")

    src = Path(a.src)
    wp_dir, ic_dir = src / "wallpapers", src / "icons"
    wps = sorted(p for p in wp_dir.iterdir() if p.suffix.lower() in IMG_EXTS) if wp_dir.is_dir() else []
    if len(wps) != 6:
        sys.exit(f"need exactly 6 wallpapers, found {len(wps)} in {wp_dir}")
    icons = {}
    if ic_dir.is_dir():
        for p in ic_dir.iterdir():
            if p.suffix.lower() in IMG_EXTS and p.stem.lower() in APP_NAMES:
                icons[p.stem.lower()] = p

    tmp = Path(tempfile.mkdtemp(prefix="rizbundle_")) if a.dry_run else None
    out_draw = tmp if a.dry_run else DRAW
    out_xml = tmp if a.dry_run else DRAW_XML
    out_draw.mkdir(parents=True, exist_ok=True)
    out_xml.mkdir(parents=True, exist_ok=True)

    total = 0
    wp_res = []
    for p in wps:
        stem = sanitize(p.stem)
        res_name = f"wp_{a.code}_{stem}"
        dest = out_draw / f"{res_name}.webp"
        im = center_crop_resize(load_image(p), 1080, 1920)
        im.save(dest, "WEBP", quality=82, method=6)
        total += dest.stat().st_size
        wp_res.append(res_name)

    ic_res = []
    missing = []
    written_icons = 0
    for app, label in ICON_APPS:
        res_name = f"ic_{a.code}_{app}"
        dest = out_draw / f"{res_name}.webp"
        if app in icons:
            im = load_image(icons[app])
            if max(im.size) > a.icon_size:
                im.thumbnail((a.icon_size, a.icon_size))
            im.save(dest, "WEBP", quality=85, method=6)
            total += dest.stat().st_size
            written_icons += 1
        else:
            missing.append(app)
        ic_res.append((res_name, label))

    xml = (f'<?xml version="1.0" encoding="utf-8"?>\n'
           f'<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
           f'    <corners android:radius="24dp" />\n'
           f'    <solid android:color="{a.widget_bg}" />\n'
           f'    <stroke android:width="2dp" android:color="{a.widget_stroke}" />\n'
           f'</shape>\n')
    (out_xml / f"widget_bg_{a.code}.xml").write_text(xml)

    wp_list = ",\n".join(f"        R.drawable.{r}" for r in wp_res)
    ic_list = ",\n".join(f'        IconItem(R.drawable.{r}, "{lbl}")' for r, lbl in ic_res)
    repo_block = (f"    private val {a.code}Wallpapers = listOf(\n{wp_list}\n    )\n"
                  f"    private val {a.code}Icons = listOf(\n{ic_list}\n    )\n")
    bundle_entry = (f"        RizBundle(\n"
                    f'            id = "{a.id}",\n'
                    f'            name = "{a.name}",\n'
                    f'            tagline = "{a.tagline}",\n'
                    f"            accentColor = {a.accent}.toInt(),\n"
                    f"            wallpapers = {a.code}Wallpapers,\n"
                    f"            icons = {a.code}Icons\n"
                    f"        ),\n")
    widget_branch = (f'        "{a.id}" -> Style(R.drawable.widget_bg_{a.code}, '
                     f"{a.accent}.toInt(), {a.muted}.toInt())\n")

    snippet = ("// ---- 1. Paste into BundleRepository.kt (next to mv/cg/nd blocks) ----\n"
               + repo_block +
               "\n// ---- 2. Add to the bundles list ----\n"
               + bundle_entry +
               "\n// ---- 3. Add to WidgetStyle.forBundle() when-branches ----\n"
               + widget_branch)
    snippet_path = REPO / f"tools/bundle_{a.code}.kt.txt"
    if not a.dry_run:
        snippet_path.write_text(snippet)

    print(snippet)
    print(f"// ---- 4. Files: {len(wp_res)} wallpapers + {written_icons} icons "
          f"+ widget_bg_{a.code}.xml -> +{total/1024:.0f} KB")
    if missing:
        print(f"// NOTE: {len(missing)} icons missing from src/icons/ "
              f"({', '.join(missing)}). Bundle ships without them; add later and re-run.")
    if a.dry_run:
        shutil.rmtree(tmp, ignore_errors=True)
        print("// (dry run: nothing written to the repo)")

if __name__ == "__main__":
    main()

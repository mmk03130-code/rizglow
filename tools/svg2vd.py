#!/usr/bin/env python3
"""Convert Lucide SVGs to Android VectorDrawables."""
import os, re, xml.etree.ElementTree as ET

SRC = os.path.join(os.path.dirname(__file__), "..", "docs/design-research/repo-samples/lucide")
OUT = os.path.join(os.path.dirname(__file__), "..", "app/src/main/res/drawable")

ICONS = {
    "ic_panel_airplane": "plane.svg",
    "ic_panel_data": "signal-high.svg",
    "ic_panel_wifi": "wifi.svg",
    "ic_panel_bt": "bluetooth.svg",
    "ic_panel_rotate": "rotate-cw.svg",
    "ic_panel_silent": "bell-off.svg",
    "ic_panel_dnd": "moon.svg",
    "ic_panel_alerts": "bell.svg",
    "ic_panel_torch": "flashlight.svg",
    "ic_panel_camera": "camera.svg",
    "ic_panel_calc": "calculator.svg",
    "ic_panel_settings": "settings.svg",
}

for vd_name, svg_file in ICONS.items():
    path = os.path.join(SRC, svg_file)
    if not os.path.exists(path):
        print("MISSING", svg_file); continue
    with open(path) as f:
        svg = f.read()
    # extract viewBox and path data
    vb = re.search(r'viewBox="([^"]+)"', svg)
    viewbox = vb.group(1) if vb else "0 0 24 24"
    _, _, vw, vh = viewbox.split()
    paths = re.findall(r'<path[^>]*d="([^"]+)"[^>]*/?>', svg)
    circles = re.findall(r'<circle[^>]*>', svg)
    lines = re.findall(r'<line[^>]*>', svg)

    vd = [f'<vector xmlns:android="http://schemas.android.com/apk/res/android"',
          f'    android:width="24dp" android:height="24dp"',
          f'    android:viewportWidth="{vw}" android:viewportHeight="{vh}">']
    for p in paths:
        vd.append(f'    <path android:fillColor="#00000000" android:strokeColor="#FFFFFF"'
                  f' android:strokeWidth="2" android:strokeLineCap="round"'
                  f' android:strokeLineJoin="round" android:pathData="{p}"/>')
    for c in circles:
        cx = re.search(r'cx="([^"]+)"', c); cy = re.search(r'cy="([^"]+)"', c)
        r = re.search(r'\br="([^"]+)"', c)
        if cx and cy and r:
            # approximate circle as path
            x, y, rr = float(cx.group(1)), float(cy.group(1)), float(r.group(1))
            d = f"M{x-rr},{y} a{rr},{rr} 0 1,0 {2*rr},0 a{rr},{rr} 0 1,0 {-2*rr},0"
            vd.append(f'    <path android:fillColor="#00000000" android:strokeColor="#FFFFFF"'
                      f' android:strokeWidth="2" android:strokeLineCap="round"'
                      f' android:pathData="{d}"/>')
    for ln in lines:
        x1 = re.search(r'x1="([^"]+)"', ln); y1 = re.search(r'y1="([^"]+)"', ln)
        x2 = re.search(r'x2="([^"]+)"', ln); y2 = re.search(r'y2="([^"]+)"', ln)
        if x1 and y1 and x2 and y2:
            d = f"M{x1.group(1)},{y1.group(1)} L{x2.group(1)},{y2.group(1)}"
            vd.append(f'    <path android:fillColor="#00000000" android:strokeColor="#FFFFFF"'
                      f' android:strokeWidth="2" android:strokeLineCap="round"'
                      f' android:pathData="{d}"/>')
    vd.append('</vector>')
    out_path = os.path.join(OUT, vd_name + ".xml")
    with open(out_path, "w") as f:
        f.write("\n".join(vd))
    print("wrote", out_path)

#!/usr/bin/env python3
"""Frosted glass group containers for panel themes.
Semi-transparent rounded rectangles that group related buttons,
like the competitor's connectivity cluster. 900x420 RGBA."""
from PIL import Image, ImageDraw, ImageFilter
import os, math

OUT = os.path.join(os.path.dirname(__file__), "..", "app/src/main/res/drawable-nodpi")

def rounded_mask(size, radius):
    w, h = size
    m = Image.new("L", size, 0)
    d = ImageDraw.Draw(m)
    d.rounded_rectangle([0, 0, w, h], radius=radius, fill=255)
    return m

def frosted_container(size, radius, base_rgb, alpha):
    w, h = size
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    px = img.load()
    # subtle vertical gradient: lighter top
    for y in range(h):
        t = y / max(h-1, 1)
        boost = int(25 * (1 - t))
        for x in range(w):
            r = min(255, base_rgb[0] + boost)
            g = min(255, base_rgb[1] + boost)
            b = min(255, base_rgb[2] + boost)
            px[x, y] = (r, g, b, alpha)
    img.putalpha(rounded_mask(size, radius))

    # top highlight line
    hi = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(hi)
    d.rounded_rectangle([6, 6, w-6, h-6], radius=radius-6,
                       outline=(255, 255, 255, 90), width=3)
    hi = hi.filter(ImageFilter.GaussianBlur(2))
    # soft top glow
    glow = Image.new("RGBA", size, (0, 0, 0, 0))
    dg = ImageDraw.Draw(glow)
    dg.rounded_rectangle([0, 0, w, h//3], radius=radius, fill=(255, 255, 255, 28))
    glow = glow.filter(ImageFilter.GaussianBlur(18))

    img = Image.alpha_composite(img, glow)
    img = Image.alpha_composite(img, hi)

    # outer shadow
    sh = Image.new("RGBA", (w+40, h+40), (0, 0, 0, 0))
    ds = ImageDraw.Draw(sh)
    ds.rounded_rectangle([20, 24, w+20, h+24], radius=radius, fill=(0, 0, 0, 70))
    sh = sh.filter(ImageFilter.GaussianBlur(14))
    base = Image.new("RGBA", (w+40, h+40), (0, 0, 0, 0))
    base = Image.alpha_composite(base, sh)
    base.alpha_composite(img, (20, 16))
    return base

THEMES = {
    "velvet":  ((95, 70, 130), 110),
    "liquid":  ((130, 55, 70), 110),
    "matcha":  ((95, 125, 90), 110),
    "ice":     ((210, 230, 242), 130),
}

for theme, (rgb, alpha) in THEMES.items():
    img = frosted_container((900, 420), 90, rgb, alpha)
    path = os.path.join(OUT, f"cc_frost_{theme}.webp")
    img.save(path, "WEBP", quality=92)
    print("wrote", path)

print("done")

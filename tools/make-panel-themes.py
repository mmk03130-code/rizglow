#!/usr/bin/env python3
"""Generate 3 new panel theme drawable sets: Liquid Glass, Matcha, Ice.
Each theme: panel_bg (1080x1600), tile_bg (420x420), card_bg (900x420), slider_bg (300x700).
All RGBA with proper rounded corners (no black-box bug)."""
from PIL import Image, ImageDraw, ImageFilter
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "app/src/main/res/drawable-nodpi")

def rounded_mask(size, radius):
    w, h = size
    mask = Image.new("L", size, 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, w, h], radius=radius, fill=255)
    return mask

def vgrad(size, top, bottom):
    w, h = size
    img = Image.new("RGBA", size)
    px = img.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        r = int(top[0] + (bottom[0] - top[0]) * t)
        g = int(top[1] + (bottom[1] - top[1]) * t)
        b = int(top[2] + (bottom[2] - top[2]) * t)
        a = int(top[3] + (bottom[3] - top[3]) * t)
        for x in range(w):
            px[x, y] = (r, g, b, a)
    return img

def gloss(size, radius, strength=90, band=0.35):
    """White diagonal gloss highlight fading down."""
    w, h = size
    g = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(g)
    # top highlight ellipse
    d.ellipse([-w * 0.3, -h * 0.7, w * 1.3, h * band], fill=(255, 255, 255, strength))
    g = g.filter(ImageFilter.GaussianBlur(18))
    mask = rounded_mask(size, radius)
    out = Image.new("RGBA", size, (0, 0, 0, 0))
    out.paste(g, (0, 0), mask)
    return out

def inner_edge(size, radius, color, width=6):
    w, h = size
    e = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(e)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=radius, outline=color, width=width)
    e = e.filter(ImageFilter.GaussianBlur(2))
    return e

def make_set(name, base_top, base_bottom, gloss_strength, edge_color, panel_extra=None):
    defs = {
        "panel_bg": ((1080, 1600), 90),
        "tile_bg": ((420, 420), 100),
        "card_bg": ((900, 420), 80),
        "slider_bg": ((300, 700), 120),
    }
    for kind, (size, radius) in defs.items():
        img = vgrad(size, base_top, base_bottom)
        mask = rounded_mask(size, radius)
        img.putalpha(mask)
        img = Image.alpha_composite(img, gloss(size, radius, gloss_strength))
        img = Image.alpha_composite(img, inner_edge(size, radius, edge_color))
        if panel_extra and kind == "panel_bg":
            img = panel_extra(img)
        path = os.path.join(OUT, f"cc_{kind}_{name}.webp")
        img.save(path, "WEBP", quality=92)
        print("wrote", path, img.size)

def vignette(img, color=(0, 0, 0, 70)):
    w, h = img.size
    v = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(v)
    d.ellipse([-w * 0.25, -h * 0.25, w * 1.25, h * 1.25], fill=(0, 0, 0, 0),
              outline=color, width=int(min(w, h) * 0.35))
    v = v.filter(ImageFilter.GaussianBlur(120))
    return Image.alpha_composite(img, v)

# Liquid Glass: deep rose-red
make_set("liquid",
         base_top=(146, 32, 52, 255), base_bottom=(74, 12, 28, 255),
         gloss_strength=70, edge_color=(255, 150, 170, 110),
         panel_extra=vignette)

# Matcha: sage green, softer gloss (matte wax feel)
make_set("matcha",
         base_top=(139, 168, 136, 255), base_bottom=(88, 110, 84, 255),
         gloss_strength=38, edge_color=(210, 228, 200, 120),
         panel_extra=vignette)

# Ice: pale frosted blue-white
make_set("ice",
         base_top=(232, 244, 250, 255), base_bottom=(196, 222, 236, 255),
         gloss_strength=110, edge_color=(255, 255, 255, 160),
         panel_extra=vignette)

print("done")

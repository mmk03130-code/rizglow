#!/usr/bin/env python3
"""Generate circular 3D pressable buttons for panel themes.
Each theme gets: cc_btn_on_<theme>.webp (accent) + cc_btn_off_<theme>.webp (dark).
500x500, RGBA. Physical button look: radial dome gradient, top rim highlight,
bottom inner shadow, soft outer shadow."""
from PIL import Image, ImageDraw, ImageFilter
import os, math

OUT = os.path.join(os.path.dirname(__file__), "..", "app/src/main/res/drawable-nodpi")
SIZE = 500
R = SIZE // 2

def circle_mask(size, radius_pad=0):
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    d.ellipse([radius_pad, radius_pad, size - radius_pad, size - radius_pad], fill=255)
    return m

def radial_dome(size, center_color, edge_color):
    """Bright center-top fading to dark edge = dome."""
    w = h = size
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    cx, cy = w / 2, h * 0.38  # light source above center
    maxd = math.hypot(w / 2, h / 2) * 1.15
    for y in range(h):
        for x in range(w):
            dx, dy = x - cx, y - cy
            if dx * dx + dy * dy > (w / 2) ** 2:
                continue
            t = min(1.0, math.hypot(dx, dy) / maxd * 1.6)
            # ease for softer falloff
            t = t * t * (3 - 2 * t)
            r = int(center_color[0] + (edge_color[0] - center_color[0]) * t)
            g = int(center_color[1] + (edge_color[1] - center_color[1]) * t)
            b = int(center_color[2] + (edge_color[2] - center_color[2]) * t)
            px[x, y] = (r, g, b, 255)
    return img

def rim_highlight(size, color, width=14, top_only=True):
    """Bright arc along top rim = light catching the edge."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pad = width
    # draw arc from ~200deg to ~340deg (top)
    d.arc([pad, pad, size - pad, size - pad], start=200, end=340, fill=color, width=width)
    img = img.filter(ImageFilter.GaussianBlur(6))
    # mask to circle
    img.putalpha(Image.composite(img.split()[3], Image.new("L", img.size, 0), circle_mask(size)))
    return img

def inner_shadow(size, color, width=26):
    """Dark arc along bottom = button depth."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pad = 4
    d.arc([pad, pad, size - pad, size - pad], start=20, end=160, fill=color, width=width)
    img = img.filter(ImageFilter.GaussianBlur(10))
    img.putalpha(Image.composite(img.split()[3], Image.new("L", img.size, 0), circle_mask(size)))
    return img

def outer_shadow(size, blur=18, offset=8):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pad = 26
    d.ellipse([pad, pad + offset, size - pad, size - pad + offset], fill=(0, 0, 0, 110))
    return img.filter(ImageFilter.GaussianBlur(blur))

def make_button(name, center, edge, hi_color, shadow_strength=90):
    base = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    base = Image.alpha_composite(base, outer_shadow(SIZE))
    dome = radial_dome(SIZE, center, edge)
    dome.putalpha(circle_mask(SIZE))
    base = Image.alpha_composite(base, dome)
    base = Image.alpha_composite(base, rim_highlight(SIZE, hi_color))
    base = Image.alpha_composite(base, inner_shadow(SIZE, (0, 0, 0, shadow_strength)))
    path = os.path.join(OUT, f"cc_btn_{name}.webp")
    base.save(path, "WEBP", quality=92)
    print("wrote", path)

THEMES = {
    # name: (on_center, on_edge, off_center, off_edge, highlight)
    "velvet":  ((168, 110, 230, 255), (88, 40, 140, 255),
                (64, 52, 80, 255),   (28, 20, 40, 255),  (220, 180, 255, 160)),
    "liquid":  ((225, 90, 110, 255),  (130, 28, 50, 255),
                (80, 36, 46, 255),    (36, 14, 22, 255),  (255, 170, 185, 160)),
    "matcha":  ((178, 205, 170, 255), (100, 135, 98, 255),
                (70, 90, 66, 255),    (32, 44, 30, 255),  (225, 242, 215, 160)),
    "ice":     ((255, 255, 255, 255), (185, 215, 235, 255),
                (120, 150, 175, 255), (60, 85, 110, 255), (255, 255, 255, 200)),
}

for theme, (oc, oe, fc, fe, hi) in THEMES.items():
    make_button(f"on_{theme}", oc, oe, hi)
    make_button(f"off_{theme}", fc, fe, hi)

print("done")

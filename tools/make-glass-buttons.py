#!/usr/bin/env python3
"""Generate glass-orb buttons for panel themes (v2).
Glassmorphism: semi-transparent fill, strong top-left light catch,
inner bottom glow, thin bright rim. Each theme gets on/off states.
500x500 RGBA webp."""
from PIL import Image, ImageDraw, ImageFilter
import os, math

OUT = os.path.join(os.path.dirname(__file__), "..", "app/src/main/res/drawable-nodpi")
SIZE = 500

def circle_mask(size, pad=0):
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    d.ellipse([pad, pad, size - pad, size - pad], fill=255)
    return m

def glass_orb(size, base_rgb, alpha, glow_rgb):
    """Glass orb: translucent base, light from top-left, inner glow bottom."""
    w = h = size
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    cx, cy = w * 0.35, h * 0.28  # light source top-left
    maxd = math.hypot(w * 0.65, h * 0.72)
    for y in range(h):
        for x in range(w):
            dx, dy = x - w/2, y - h/2
            if dx*dx + dy*dy > (w/2 - 4)**2:
                continue
            # light falloff from top-left
            ld = math.hypot(x - cx, y - cy)
            lt = max(0, 1 - ld / maxd)  # 1 at light, 0 far
            lt = lt * lt
            # base glass color with light boost
            r = int(min(255, base_rgb[0] + lt * 70))
            g = int(min(255, base_rgb[1] + lt * 70))
            b = int(min(255, base_rgb[2] + lt * 70))
            # bottom inner glow
            glow_t = max(0, (y - h * 0.55) / (h * 0.45))
            r = int(min(255, r + glow_rgb[0] * glow_t * 0.35))
            g = int(min(255, g + glow_rgb[1] * glow_t * 0.35))
            b = int(min(255, b + glow_rgb[2] * glow_t * 0.35))
            px[x, y] = (r, g, b, alpha)
    return img

def rim_light(size, color, width=10):
    """Thin bright rim, stronger at top."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pad = width // 2 + 2
    # full thin rim
    d.ellipse([pad, pad, size-pad, size-pad], outline=color, width=width//2)
    # extra bright arc top-left
    d.arc([pad+2, pad+2, size-pad-2, size-pad-2], start=180, end=300,
          fill=(255, 255, 255, 200), width=width)
    img = img.filter(ImageFilter.GaussianBlur(3))
    # mask to circle
    alpha = img.split()[3]
    mask = circle_mask(size, pad)
    img.putalpha(Image.composite(alpha, Image.new("L", img.size, 0), mask))
    return img

def top_highlight(size, strength=90):
    """Soft elliptical highlight at top (glass reflection)."""
    w = h = size
    g = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(g)
    d.ellipse([w*0.15, -h*0.35, w*0.85, h*0.45], fill=(255, 255, 255, strength))
    g = g.filter(ImageFilter.GaussianBlur(22))
    alpha = g.split()[3]
    mask = circle_mask(size)
    g.putalpha(Image.composite(alpha, Image.new("L", g.size, 0), mask))
    return g

def outer_shadow(size, blur=16):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pad = 30
    d.ellipse([pad, pad+10, size-pad, size-pad+10], fill=(0, 0, 0, 100))
    return img.filter(ImageFilter.GaussianBlur(blur))

def make_orb(name, base_rgb, alpha, glow_rgb, rim_color):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    img = Image.alpha_composite(img, outer_shadow(SIZE))
    img = Image.alpha_composite(img, glass_orb(SIZE, base_rgb, alpha, glow_rgb))
    img = Image.alpha_composite(img, top_highlight(SIZE))
    img = Image.alpha_composite(img, rim_light(SIZE, rim_color))
    path = os.path.join(OUT, f"cc_glass_{name}.webp")
    img.save(path, "WEBP", quality=92)
    print("wrote", path)

# (theme, on_base, on_alpha, on_glow, off_base, off_alpha, off_glow, rim)
THEMES = {
    "velvet":  ((150, 100, 210), 175, (180, 130, 255),
                (45, 35, 60), 150, (80, 60, 120),
                (200, 170, 255, 120)),
    "liquid":  ((215, 80, 100), 175, (255, 120, 140),
                (60, 28, 38), 150, (110, 50, 65),
                (255, 160, 175, 120)),
    "matcha":  ((165, 195, 155), 175, (200, 230, 185),
                (50, 70, 48), 150, (90, 120, 85),
                (215, 235, 205, 120)),
    "ice":     ((240, 248, 252), 185, (255, 255, 255),
                (100, 135, 165), 150, (150, 185, 215),
                (255, 255, 255, 150)),
}

for theme, (ob, oa, og, fb, fa, fg, rim) in THEMES.items():
    make_orb(f"on_{theme}", ob, oa, og, rim)
    make_orb(f"off_{theme}", fb, fa, fg, rim)

print("done")

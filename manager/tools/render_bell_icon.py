#!/usr/bin/env python3
"""Render legacy launcher PNGs: white rounded square + black line-art bell.

Mirrors the adaptive-icon vector (drawable/ic_launcher_foreground.xml):
same 24-unit bell geometry, stroke-only, pure black on white.
"""
from PIL import Image, ImageDraw

RES = "app/src/main/res"
SIZES = {
    "ldpi": 36, "mdpi": 48, "hdpi": 72,
    "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192,
}
SS = 8  # supersampling factor

BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)


def cubic(p0, p1, p2, p3, n=64):
    pts = []
    for i in range(n + 1):
        t = i / n
        mt = 1 - t
        x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
        y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
        pts.append((x, y))
    return pts


def arc_pts(cx, cy, r, a0, a1, n=96):
    """Sample a circular arc (degrees, PIL convention: 0=3 o'clock, CW)."""
    import math
    pts = []
    for i in range(n + 1):
        a = math.radians(a0 + (a1 - a0) * i / n)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def dot(d, xy, r, color=BLACK):
    d.ellipse([xy[0] - r, xy[1] - r, xy[0] + r, xy[1] + r], fill=color)


def render(size: int) -> Image.Image:
    W = size * SS
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # White rounded-square background (transparent corners), ~19% radius.
    d.rounded_rectangle([0, 0, W - 1, W - 1], radius=int(W * 0.19), fill=WHITE)

    # Round bell (jingle bell) on a 24-unit grid: circle r=9 centered (12,12),
    # mouth slit chord at y=16, solid clapper ball at (12, 18.6).
    s = (W * 0.52) / 18.6
    ox = W / 2 - 12 * s
    oy = W / 2 - 12 * s

    def P(x, y):
        return (ox + x * s, oy + y * s)

    w = max(1, round(2 * s))  # stroke width = 2 units

    # Bell body: full circle as closed polyline.
    body = arc_pts(*(P(12, 12)), 9 * s, 0, 360)
    d.line(body, fill=BLACK, width=w, joint="curve")

    # Mouth slit: chord of the circle at y=16 (dy=4 -> dx=sqrt(81-16)).
    import math
    dx = math.sqrt(9 * 9 - 4 * 4)
    slit = [P(12 - dx, 16), P(12 + dx, 16)]
    d.line(slit, fill=BLACK, width=w)
    dot(d, slit[0], w / 2)
    dot(d, slit[1], w / 2)

    # Clapper ball: solid circle center (12, 18.6) r=1.9.
    c, r = P(12, 18.6), 1.9 * s
    d.ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], fill=BLACK)

    return img.resize((size, size), Image.LANCZOS)


if __name__ == "__main__":
    preview = render(512)
    preview.save("tools/bell_preview.png")
    for dpi, size in SIZES.items():
        out = f"{RES}/mipmap-{dpi}/ic_launcher.png"
        render(size).save(out)
        print(f"wrote {out} ({size}px)")

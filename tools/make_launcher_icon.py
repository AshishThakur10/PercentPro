#!/usr/bin/env python3
"""
Generates every PercentPro launcher icon asset from one set of geometry values.

Why this exists: the project was created from the Android Studio template and
still shipped the template icon — Android's green (#3DDC84) with the white robot
— which is not the product at all.

Why it is a generator and not hand-written XML: the adaptive icon is drawn by two
VectorDrawables while the pre-API-26 launchers are drawn by ten raster files.
Hand-writing one set and generating the other lets the two drift apart, and the
device then shows a different icon depending on its Android version. Both are
produced here from the same constants so a change to the mark moves every asset
at once.

Geometry is expressed in the 108-unit space of an adaptive icon (Android's own
canvas size). The safe area is the centre 72dp circle; the mark's farthest point
from centre is 34.7 units, comfortably inside it, so no launcher mask clips it.

Run from the project root:
    python3 tools/make_launcher_icon.py
"""
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res")
STORE_GRAPHICS = os.path.join(ROOT, "store/graphics")

# ---------------------------------------------------------------------------
# Brand
# ---------------------------------------------------------------------------

# DeepPurple from ui/theme/Color.kt. Kept as a literal rather than parsed from
# Kotlin so this script has no build-time dependency; the test
# LauncherIconTest asserts the two still agree.
BACKGROUND = "#4A2C8E"
FOREGROUND = "#FFFFFF"

# ---------------------------------------------------------------------------
# Geometry, in the 108-unit adaptive-icon space
# ---------------------------------------------------------------------------

CANVAS = 108          # Android's adaptive-icon canvas
CORNER_RADIUS = 22    # rounded square used by the legacy square launcher icon

MARK_CIRCLE_RADIUS = 10.0
MARK_CIRCLE_STROKE = 7.0
MARK_CIRCLES = [(39.0, 39.0), (69.0, 69.0)]

MARK_LINE = (32.0, 76.0, 76.0, 32.0)   # x1, y1, x2, y2
MARK_LINE_STROKE = 8.5

# Raster size in pixels for each density bucket.
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

PLAY_ICON_SIZE = 512  # Play store listing icon


def _round(value):
    """Render a coordinate without a trailing .0, which some parsers dislike."""
    return f"{value:g}"


# ---------------------------------------------------------------------------
# VectorDrawables (API 26+ adaptive icon, and the themed-icon silhouette)
# ---------------------------------------------------------------------------

def _circle_path(cx, cy, r):
    """A circle as two half arcs, centred on (cx, cy) with radius r.

    Stroking this draws the ring: Android centres a stroke on the path, so a
    stroke width of w gives a ring from r-w/2 to r+w/2.
    """
    return (f"M{_round(cx - r)},{_round(cy)} "
            f"a{_round(r)},{_round(r)} 0 1,0 {_round(2 * r)},0 "
            f"a{_round(r)},{_round(r)} 0 1,0 -{_round(2 * r)},0")


def _mark_paths():
    """The % mark as (pathData, strokeWidth) pairs."""
    paths = [(f"M{_round(cx - MARK_CIRCLE_RADIUS)},{_round(cy)} "
              f"a{_round(MARK_CIRCLE_RADIUS)},{_round(MARK_CIRCLE_RADIUS)} 0 1,0 "
              f"{_round(2 * MARK_CIRCLE_RADIUS)},0 "
              f"a{_round(MARK_CIRCLE_RADIUS)},{_round(MARK_CIRCLE_RADIUS)} 0 1,0 "
              f"-{_round(2 * MARK_CIRCLE_RADIUS)},0",
              MARK_CIRCLE_STROKE)
             for cx, cy in MARK_CIRCLES]
    x1, y1, x2, y2 = MARK_LINE
    paths.append((f"M{_round(x1)},{_round(y1)} L{_round(x2)},{_round(y2)}",
                  MARK_LINE_STROKE))
    return paths


def _vector(path_data_by_element):
    body = "\n".join(path_data_by_element)
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        f'    android:viewportWidth="{CANVAS}"\n'
        f'    android:viewportHeight="{CANVAS}">\n'
        f'{body}\n'
        '</vector>\n'
    )


def write_background_vector():
    path = os.path.join(RES, "drawable", "ic_launcher_background.xml")
    content = _vector([
        f'    <path\n'
        f'        android:fillColor="{BACKGROUND}"\n'
        f'        android:pathData="M0,0h{CANVAS}v{CANVAS}h-{CANVAS}z" />'
    ])
    _write(path, content)
    return path


def _mark_vector(stroke_colour):
    """The % mark as a VectorDrawable of stroked paths.

    One <path> per element so the circles and the slash can carry different
    stroke widths.
    """
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        f'    android:viewportWidth="{CANVAS}"\n'
        f'    android:viewportHeight="{CANVAS}">'
    ]
    for data, width in _mark_paths():
        lines.append(
            '    <path\n'
            '        android:fillColor="#00000000"\n'
            f'        android:strokeColor="{stroke_colour}"\n'
            f'        android:strokeWidth="{_round(width)}"\n'
            '        android:strokeLineCap="round"\n'
            f'        android:pathData="{data}" />'
        )
    lines.append('</vector>\n')
    return "\n".join(lines)


def write_foreground_vector():
    path = os.path.join(RES, "drawable", "ic_launcher_foreground.xml")
    _write(path, _mark_vector(FOREGROUND))
    return path


def write_monochrome_vector():
    """Same mark, single colour.

    Android 13+ tints this layer itself, so it must be a flat silhouette with
    no gradient. Previously <monochrome> pointed at the coloured foreground,
    which meant themed launcher icons also showed the template robot.
    """
    path = os.path.join(RES, "drawable", "ic_launcher_monochrome.xml")
    _write(path, _mark_vector("#FFFFFF"))
    return path


def write_adaptive_xml():
    paths = []
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        path = os.path.join(RES, "mipmap-anydpi-v26", name)
        _write(path,
               '<?xml version="1.0" encoding="utf-8"?>\n'
               '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
               '    <background android:drawable="@drawable/ic_launcher_background" />\n'
               '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
               '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
               '</adaptive-icon>\n')
        paths.append(path)
    return paths


# ---------------------------------------------------------------------------
# Raster fallbacks (API 23-25, where no adaptive icon is available)
# ---------------------------------------------------------------------------

SUPERSAMPLE = 8   # render large, downscale with Lanczos for clean edges


def _magick(shape, size):
    """Renders one icon with ImageMagick and returns the PNG bytes at `size`."""
    s = SUPERSAMPLE
    canvas = CANVAS * s

    def px(v):
        return f"{v * s:.0f}"

    if shape == "square":
        background = (f"fill {BACKGROUND} stroke none roundrectangle "
                      f"0,0 {px(CANVAS)},{px(CANVAS)} {px(CORNER_RADIUS)},{px(CORNER_RADIUS)}")
    elif shape == "full":
        # Square corners, full bleed. The Play listing icon only: Google applies
        # its own mask, so pre-rounding the artwork makes the visible icon
        # smaller than the 512px keyline grid it is measured against.
        background = (f"fill {BACKGROUND} stroke none rectangle "
                      f"0,0 {px(CANVAS)},{px(CANVAS)}")
    else:
        background = (f"fill {BACKGROUND} stroke none circle "
                      f"{px(CANVAS / 2)},{px(CANVAS / 2)} {px(CANVAS / 2)},0")

    # Each draw primitive is its own argv entry: the quotes in ImageMagick's
    # -draw syntax belong to the shell, not to the value.
    draws = [["-draw", background]]
    for cx, cy in MARK_CIRCLES:
        draws.append([
            "-draw",
            f"fill none stroke {FOREGROUND} "
            f"stroke-width {px(MARK_CIRCLE_STROKE)} "
            f"circle {px(cx)},{px(cy)} {px(cx + MARK_CIRCLE_RADIUS)},{px(cy)}"
        ])
    x1, y1, x2, y2 = MARK_LINE
    draws.append([
        "-draw",
        f"fill none stroke {FOREGROUND} "
        f"stroke-width {px(MARK_LINE_STROKE)} stroke-linecap round "
        f"line {px(x1)},{px(y1)} {px(x2)},{px(y2)}"
    ])

    command = (["convert", "-size", f"{canvas}x{canvas}", "xc:none"]
               + [part for draw in draws for part in draw]
               + ["-filter", "Lanczos", "-resize", f"{size}x{size}", "png:-"])
    result = subprocess.run(command, capture_output=True)
    if result.returncode != 0:
        raise SystemExit(f"ImageMagick failed: {result.stderr.decode()[:400]}")
    return result.stdout


def write_legacy_rasters():
    """Ten files: ic_launcher and ic_launcher_round at five densities."""
    written = []
    for density, size in DENSITIES.items():
        folder = os.path.join(RES, f"mipmap-{density}")
        for name, shape in (("ic_launcher", "square"), ("ic_launcher_round", "round")):
            png = _magick(shape, size)
            path = os.path.join(folder, f"{name}.png")
            with open(path, "wb") as handle:
                handle.write(png)
            # The template shipped .webp here. Leaving both behind would mean two
            # definitions of the same resource and an ambiguous density choice.
            stale = path[:-4] + ".webp"
            if os.path.exists(stale):
                os.remove(stale)
            written.append((path, len(png), size))
    return written


def write_play_icon():
    """512x512 Play store listing icon.

    Full square, no pre-rounded corners and no drop shadow: Play applies its own
    mask, and artwork that is pre-rounded ends up smaller than the keyline grid.
    """
    os.makedirs(STORE_GRAPHICS, exist_ok=True)
    path = os.path.join(STORE_GRAPHICS, "ic_launcher_play_512.png")
    png = _magick("full", PLAY_ICON_SIZE)
    with open(path, "wb") as handle:
        handle.write(png)
    return path, len(png)


# ---------------------------------------------------------------------------

def _write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(content)


def main():
    vectors = [write_background_vector(), write_foreground_vector(),
               write_monochrome_vector()] + write_adaptive_xml()
    for path in vectors:
        print(f"  vector   {os.path.relpath(path, ROOT)}")

    for path, length, size in write_legacy_rasters():
        print(f"  raster   {os.path.relpath(path, ROOT)}  {size}x{size}  {length:,} bytes")

    path, length = write_play_icon()
    print(f"  play     {os.path.relpath(path, ROOT)}  "
          f"{PLAY_ICON_SIZE}x{PLAY_ICON_SIZE}  {length:,} bytes")

    kb = length / 1024
    if kb > 1024:
        print(f"WARNING: Play icon is {kb:.0f} KB, over the 1024 KB limit")
    return 0


if __name__ == "__main__":
    sys.exit(main())
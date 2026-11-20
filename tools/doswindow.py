#!/usr/bin/env python3
# Find the DOS video area inside a weston desktop capture of a windowed DOSBox-X
# run, so the Descent dumps can be cropped to their own picture. The seat's
# wallpaper is the STM32MPU/OpenSTLinux scene, whose big solid fields are the
# navy (3,35,75) and the yellow band (255,210,0); DOSBox-X draws neither, so its
# window is the widest "not wallpaper" run a row contains. The window's height
# comes from the same test run down both side border columns, so the wallpaper's
# own white skyline art below the window cannot stretch it. DOSBox-X also paints
# its own menu strip ("Main CPU Video Sound DOS Drive Capture Help") across the
# top inside the window, in flat (63,63,63) grey with (191,191,191) letters and
# a (31,31,31) bottom line, and that strip is not the DOS picture: its bottom
# edge is the first row of the window that carries colours other than those greys.
# Usage: doswindow.py <desktop capture png> ...
WALL = ((3, 35, 75), (255, 210, 0))
GREY = ((63, 63, 63), (31, 31, 31), (191, 191, 191))

def close(p, q, tol):
    return all(abs(a - b) <= tol for a, b in zip(p, q))

def wallpaper(p):
    return any(close(p, c, 16) for c in WALL)

def greyish(p):
    return any(close(p, c, 10) for c in GREY)

def longest_run(flags, gapmax=10):
    best = (0, 0, 0)
    i = 0
    n = len(flags)
    while i < n:
        if not flags[i]:
            i += 1
            continue
        e = j = i
        while j < n:
            if flags[j]:
                e = j
                j += 1
            else:
                k = j
                while k < n and not flags[k]:
                    k += 1
                if k - j <= gapmax and k < n:
                    j = k
                    continue
                break
        if e - i + 1 > best[0]:
            best = (e - i + 1, i, e)
        i = e + 1
    return best

def run_down(px, x, y0, y1, gapmax=14):
    """last row at or below y0 that a gap-tolerant column run still reaches"""
    y = y0
    last = y0
    gap = 0
    while y <= y1:
        if not wallpaper(px[x, y]):
            last = y
            gap = 0
        else:
            gap += 1
            if gap > gapmax:
                break
        y += 1
    return last

from PIL import Image
import sys
from collections import Counter

for path in sys.argv[1:]:
    im = Image.open(path).convert('RGB')
    w, h = im.size
    px = im.load()
    runs = [longest_run([not wallpaper(px[x, y]) for x in range(w)]) for y in range(h)]
    starts = Counter(s for length, s, e in runs if length >= 300)
    ends = Counter(e for length, s, e in runs if length >= 300)
    if not starts:
        print(path, 'no window in this capture')
        continue
    x0 = starts.most_common(1)[0][0]
    x1 = ends.most_common(1)[0][0]
    y0 = None
    for y, (length, s, e) in enumerate(runs):
        if length >= 300 and abs(s - x0) <= 2 and abs(e - x1) <= 2:
            y0 = y
            break
    if y0 is None:
        print(path, 'no window top found')
        continue
    y1 = min(run_down(px, x0 + 1, y0, h - 1), run_down(px, x1 - 1, y0, h - 1))
    ytop = y0
    for y in range(y0, min(y0 + 60, y1)):
        row = [px[x, y] for x in range(x0 + 4, x1 - 3, 5)]
        if not all(greyish(p) for p in row):
            ytop = y
            break
    print('%s window %d,%d..%d,%d = %dx%d  dos video %d,%d..%d,%d = %dx%d' % (
        path, x0, y0, x1, y1, x1 - x0 + 1, y1 - y0 + 1, x0, ytop, x1, y1,
        x1 - x0 + 1, y1 - ytop + 1))

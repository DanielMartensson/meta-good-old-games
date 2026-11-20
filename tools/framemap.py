import sys, glob, hashlib
from PIL import Image, ImageOps, ImageChops

for fn in sorted(glob.glob(sys.argv[1])):
    im = Image.open(fn).convert("RGB")
    im.load()
    w, h = im.size
    print("=== %s  %dx%d  md5 %s ===" % (fn, w, h, hashlib.md5(open(fn, "rb").read()).hexdigest()))
    L = im.convert("L")
    # bounding box of anything non-black, using a threshold
    mask = L.point(lambda v: 255 if v > 6 else 0)
    bbox = mask.getbbox()
    print("non-dark bounding box:", bbox, "  (w,h) of that box:",
          (bbox[2] - bbox[0], bbox[3] - bbox[1]) if bbox else None)
    # per-row and per-column brightness profile, in 16 bands
    for name, strip in (("rows", L.resize((1, 16), Image.Resampling.BOX)), ("cols", L.resize((16, 1), Image.Resampling.BOX))):
        vals = list(strip.getdata())
        print("  %s (16 bands, mean luma):" % name, " ".join("%4d" % v for v in vals))
    rgb = im
    cnt = rgb.getcolors(maxcolors=1 << 22)
    cnt.sort(reverse=True)
    print("  top colors:", cnt[:8])
    # 8x8 blocks: which blocks have any bright content (layout of the frame)
    grid = im.resize((32, 18), Image.Resampling.BOX).convert("L")
    d = list(grid.getdata())
    print("  32x18 block brightness map (>10 = X):")
    for y in range(18):
        print("   " + "".join("X" if d[y * 32 + x] > 10 else "." for x in range(32)))

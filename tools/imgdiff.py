import sys, glob, itertools
from PIL import Image, ImageChops, ImageOps

files = sorted(glob.glob(sys.argv[1]))
imgs = []
for f in files:
    im = Image.open(f).convert("RGB")
    im.load()
    imgs.append(im)
    print(f, im.size)

def profiles(im):
    """Mean luminance per column (width,1) and per row (1,height): a box-filter
    resize averages whole blocks, so these two strips are the scene's coarse
    shape and are compared to tell "same view, different lighting" from
    "different view"."""
    L = im.convert("L")
    w, h = L.size
    col = L.resize((w, 1), Image.Resampling.BOX)
    row = L.resize((1, h), Image.Resampling.BOX)
    return list(col.getdata()), list(row.getdata())

def cmp(a, b):
    w, h = a.size
    n = w * h
    dr, dg, db = ImageChops.difference(a, b).split()
    mask = ImageChops.lighter(ImageChops.lighter(dr, dg), db)  # per-pixel max channel diff
    hist = mask.histogram()
    print("   identical pixels: %d/%d (%5.1f%%)" % (hist[0], n, 100.0 * hist[0] / n))
    for thr in (1, 2, 4, 8, 16, 32, 64, 90, 128):
        big = sum(hist[thr:])
        print("   max-channel-diff > %3d : %7d px  (%5.1f%%)" % (thr, big, 100.0 * big / n))
    ca, ra = profiles(a)
    cb, rb = profiles(b)
    col_big = sum(1 for x, y in zip(ca, cb) if abs(x - y) > 8)
    row_big = sum(1 for x, y in zip(ra, rb) if abs(x - y) > 8)
    print("   columns whose mean luminance moved >8: %d/%d    rows: %d/%d"
          % (col_big, w, row_big, h))

for i, j in itertools.combinations(range(len(imgs)), 2):
    print("pair %d vs %d:" % (i, j))
    cmp(imgs[i], imgs[j])

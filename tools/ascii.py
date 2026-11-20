import sys, glob
from PIL import Image

for fn in sorted(glob.glob(sys.argv[1])):
    im = Image.open(fn).convert("RGB")
    im.load()
    w, h = im.size
    small = im.resize((96, 36), Image.Resampling.BOX)
    L = small.convert("L")
    ramp = " .:-=+*#%@"
    print("=== %s  %dx%d, ascii at 96x36 (row 0 = top) ===" % (fn, w, h))
    rows = []
    for y in range(36):
        line = "".join(ramp[min(9, int(v * 10 / 256))] for v in list(L.crop((0, y, 96, y + 1)).getdata()))
        rows.append(line)
    for i, r in enumerate(rows):
        print("%2d |%s" % (i, r))
    # where the bright pixels are
    data = list(im.getdata())
    ys = {}
    for idx, (r, g, b) in enumerate(data):
        if r + g + b > 150:
            ys.setdefault(idx // w, 0)
            ys[idx // w] += 1
    bright = sorted(ys.items())
    print("bright-pixel rows: %d rows with any; first %s last %s; counts: %d max"
          % (len(bright), bright[0][0] if bright else None, bright[-1][0] if bright else None,
             max((c for _, c in bright), default=0)))
    print("md5:", __import__("hashlib").md5(open(fn, "rb").read()).hexdigest())

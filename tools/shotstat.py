import sys, glob, hashlib, collections
from PIL import Image

for fn in sorted(glob.glob(sys.argv[1])):
    im = Image.open(fn); im.load()
    rgb = im.convert("RGB")
    w, h = rgb.size
    flat = list(rgb.getdata())
    cnt = collections.Counter(flat)
    nonblack = sum(v for k, v in cnt.items() if k != (0, 0, 0))
    K = (0.299, 0.587, 0.114)
    def L(t): return t[0]*K[0] + t[1]*K[1] + t[2]*K[2]
    lums = [L(c) for c in flat]
    rowsL = [sum(lums[y*w:(y+1)*w])/w for y in range(h)]
    mid = lums[(h//2)*w:(h//2+1)*w]
    edges = sum(1 for i in range(1, w) if abs(mid[i]-mid[i-1]) > 12)
    print(fn, rgb.size, rgb.mode)
    print("   distinct colors:", len(cnt), " non-black:", nonblack, "/", w*h)
    print("   top:", cnt.most_common(5))
    print("   row-lum min/max:", round(min(rowsL), 1), round(max(rowsL), 1),
          " distinct-row-lum:", len(set(round(x) for x in rowsL)))
    print("   mid-row brightness jumps:", edges)
    print("   md5:", hashlib.md5(open(fn, "rb").read()).hexdigest())

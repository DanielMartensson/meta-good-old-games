import glob, os, re

names = ["ut99", "chocolate-doom", "heretic1-demo", "hexen2-demo",
         "hexen-demo", "quake1-demo", "quake2-demo", "quake3-demo",
         "ioquake3", "fteqw", "librequake"]
base = ("/home/ubuntu/Dokument/Github/Watermelon-Wine/watermelon-wine-os/"
        "build-openstlinuxweston-stm32mp25-mx/tmp-glibc/deploy/deb")
TERM = 96  # ar trailer backtick, as a byte value


def walk(p):
    with open(p, "rb") as f:
        f.read(8)
        off = 8
        while True:
            h = f.read(60)
            if len(h) < 60 or h[58] != TERM or h[59] != 10:
                return
            nm = h[0:16].decode("ascii", "replace").strip()
            try:
                sz = int(h[48:58])
            except ValueError:
                return
            if nm.startswith("data.tar"):
                f.seek(off + 60)
                hdr = f.read(8)
                print(os.path.basename(p), os.path.getsize(p), nm, sz,
                      "C1=%02x" % hdr[6], "C2=%02x" % hdr[7])
            off = off + 60 + sz
            if off % 2:
                off += 1
            f.seek(off)


for arch in ("cortexa35", "all", "stm32mp25_watermelon_wine_1a"):
    for n in names:
        for p in sorted(glob.glob(os.path.join(base, arch, n + "_*.deb"))):
            if re.search(r"-(dbg|dev|src|doc|locale)", os.path.basename(p)):
                continue
            walk(p)

import re
import sys

path = sys.argv[1]
data = open(path, "rb").read()

for pat in (b"#version 310 es", b"#extension GL_OES_shader_io_blocks"):
    for m in re.finditer(re.escape(pat), data, re.M):
        lo = max(0, m.start() - 300)
        hi = min(len(data), m.end() + 1600)
        chunk = data[lo:hi]
        printable = bytes(c if 32 <= c < 127 else 0x0a if c in (9, 10) else 0x7c for c in chunk)
        print("== offset", m.start(), "pattern", pat.decode(), "of", path)
        sys.stdout.write(printable.decode("latin1"))
        print()
        print("--")

# libsdl3 - the SDL 3.x library the seat carries, and how a recipe asks for it

Detail file for `libsdl3_3.4.16.bb` - this layer's own recipe, and the only
`libsdl3` recipe this build parses (a find over the layers tree answers no other).
Instructions only here; pins, sizes, checksums and measurements are in
[PROOF.md](PROOF.md).

## Load it

```bash
bitbake libsdl3
```

The `arm64` debs go to the seat with `dpkg -i`. `inherit cmake lib_package`
splits the library, so the runtime package is `libsdl3-0`:

```
/usr/lib/libSDL3.so.0.5.0                 the library
/usr/lib/libSDL3.so.0 -> that file        the dev symlink
/usr/share/licenses/SDL3/LICENSE.txt      SDL's own license text
/usr/include/SDL3/*.h                     the dev package
```

The seat already carries `libsdl3-0 3.4.16-r0.0` installed, so a rebuild here
replaces the same version in place.

## Use it

A recipe asks for SDL3 by name, which is what the upstream project's own
CMake does:

```cmake
find_package(SDL3 REQUIRED CONFIG)
```

and this recipe answers it - SDL 3.x installs `SDL3Config.cmake` and
`SDL3Targets.cmake`, and `PROVIDES = "virtual/libsdl3"` is what keeps the
distro's own SDL provider honest about which one is in the sysroot. The
`opennow` and the `opennowlite` recipe both carry `libsdl3` in `DEPENDS`, so
its headers and its CMake package are in their sysroots; the games that use
SDL 2 are not in this recipe's chain at all.

## Measured

Sizes, the library's own md5 against the seat's, the deb split and the
failure modes are in [PROOF.md](PROOF.md).

## Files

```
libsdl3_3.4.16.bb   the recipe
README.md           this file
PROOF.md            pins, sizes, checksums, measured facts, failure modes
```

# libsdl3 - pins, measurements and the design record

Detail file for `recipes-multimedia/libsdl3/libsdl3_3.4.16.bb`.

## Pin

```
SRC_URI  = "git://github.com/libsdl-org/SDL.git;protocol=https;branch=main"
SRCREV   = "67bf94eed520569fa2eb615922547e8f419f49f3"
PV       = "3.4.16"
S        = "${WORKDIR}/git"
LICENSE  = "Zlib & BSD-2-Clause"
LIC_FILES_CHKSUM = file://LICENSE.txt;md5=036a54229112040a743509a86b30c80c
                   file://src/hidapi/LICENSE.txt;md5=7c3949a631240cb6c31c50f3eb696077
                   file://src/hidapi/LICENSE-bsd.txt;md5=b5fa085ce0926bb50d0621620a82361f
HOMEPAGE   = "https://www.libsdl.org/"
BUGTRACKER = "https://github.com/libsdl-org/SDL/issues/"
```

`inherit cmake lib_package pkgconfig` and `PROVIDES = "virtual/libsdl3"` - the
`lib_package` split is what makes the runtime package `libsdl3-0` rather than
`libsdl3`, and it is why the seat's own `dpkg -s libsdl3` names nothing while
`dpkg -s libsdl3-0` answers `install ok installed`.

## The debs this branch builds (built 2026-11-20 from this tree)

| deb | size | md5 |
| --- | --- | --- |
| `libsdl3-0_3.4.16-r0.0_arm64.deb` | 806,780 B | `25762c0796f69f4c7fd74a7793586f5b` |
| `libsdl3-dbg_3.4.16-r0.0_arm64.deb` | 2,937,676 B | `217da66f07dabd695540d4a901174443` |
| `libsdl3-dev_3.4.16-r0.0_arm64.deb` | 443,384 B | `0f3e9798992839edb77c98f5cba8781d` |
| `libsdl3-src_3.4.16-r0.0_arm64.deb` | 1,737,712 B | `40240280652e6e3a7ecfea7730a6724f` |

These four files in `tmp-glibc/deploy/deb/cortexa35` carry the 2026-10-17 date
and a single hard link, so this round's run did not replace them: the recipe's
packaging task hashes match this tree, which is the honest way of saying the
package here is the earlier round's and not one written today.

What the base deb carries, read out of its own tar:

| path | size | md5 |
| --- | --- | --- |
| `./usr/lib/libSDL3.so.0.5.0` | 2,998,360 B | `3ca492e073185b1d6b258cdadc765312` |
| `./usr/share/licenses/SDL3/LICENSE.txt` | 884 B | `036a54229112040a743509a86b30c80c` |
| `./usr/lib/libSDL3.so.0` | symlink | -> `libSDL3.so.0.5.0` |

The license file's md5 is the recipe's own `LIC_FILES_CHKSUM` md5 for
`LICENSE.txt`, read at the payload's installed path inside the deb (the seat's
copy of that file was read only as a size this round), which is the pin doing
its job at the deb end. The
base deb's own control says `Version: 3.4.16-r0.0` and `Depends: libc6 (>=
2.39+git0+58cbbd43fe)` - one runtime dependency, nothing GL- or SDL-shaped.

## The cross-check against the seat, measured today

The seat's own library is this recipe's bytes:

```
seat   /usr/lib/libSDL3.so.0.5.0   2,998,360 B  md5 3ca492e073185b1d6b258cdadc765312
deb    ./usr/lib/libSDL3.so.0.5.0 2,998,360 B  md5 3ca492e073185b1d6b258cdadc765312
seat   /usr/lib/libSDL3.so.0 -> libSDL3.so.0.5.0
seat   /usr/share/licenses/SDL3/LICENSE.txt 884 B
```

with `dpkg -s libsdl3-0` answering `Version: 3.4.16-r0.0`, `Status:
install ok installed`, `PackageArch: cortexa35` and the same
`/usr/lib/libSDL3.so.0.5.0` at the same size, so the layer's pin, the deb and
the seat are one object rather than three that happen to look alike.

## Who consumes SDL 3 here, measured today

`bitbake -e` resolves the two CloudMatch recipes with `libsdl3` in their
`DEPENDS`:

```
opennow     DEPENDS ... libsdl3 vulkan-loader vulkan-headers libva wayland ...
opennowlite DEPENDS ... libsdl3 cmake-native rust-1.89.0-host
```

and this recipe's own `DEPENDS` resolves to

```
pkgconfig-native cmake-native virtual/aarch64-ostl-linux-gcc
virtual/aarch64-ostl-linux-compilerlibs virtual/libc wayland wayland-native
wayland-protocols libxkbcommon libdrm vulkan-loader udev
virtual/libgles2 virtual/egl alsa-lib ninja-native
```

so both conditional appends are live on this machine: `DISTRO_FEATURES` carries
`opengl` and `alsa`, which is what puts `virtual/libgles2 virtual/egl` and
`alsa-lib` in the sysroot and gets SDL's own autodetection the Wayland video
driver, the Vulkan loader, the KMSDRM/EVDev input side and the ALSA audio
backend.

Nothing on the seat **loads** SDL 3 at runtime, and that was measured rather
than assumed: `ldd` answers no `libSDL3` for `/usr/bin/opennow`,
`/usr/bin/opennow-qt`, `/usr/bin/opennow-core`,
`/usr/lib/libopennow_streamer_ffi.so`, `/usr/bin/xash3d`,
`/usr/bin/dosbox-x`, `/usr/bin/OpenJazz`, `/usr/bin/chocolate-doom`,
`/usr/bin/fteqw-gles`, `/usr/bin/ioquake3-gl2` or `/usr/bin/eduke32`. SDL 3 is
a build-time dependency here - OpenNOW's own CMake does
`find_package(SDL3 REQUIRED CONFIG)` and needs the CMake package and the
headers in the sysroot - while the games this layer ships link SDL 2, which is
a different library and not in this recipe's chain.

## Why each switch is here

- `-DSDL_TEST_LIBRARY=OFF -DSDL_TESTS=OFF -DSDL_INSTALL_TESTS=OFF`: SDL's own
  CMake otherwise builds the test suite and installs its test library, and the
  test library wants pieces the target sysroot does not have.
- `FILES:${PN} += "${datadir}/licenses"`: SDL's CMake installs its `LICENSE.txt`
  under `${datadir}/licenses/SDL3` with no option to turn that off, so the
  package has to ship it or the installed-vs-shipped QA check fails - which is
  also why the base deb's license file is the third thing it carries.
- `PROVIDES = "virtual/libsdl3"` beside the name, so a consumer that wants
  "an SDL 3" resolves to this recipe rather than to an SDL 2 provider.

## Failure modes

- The flag that forces this recipe's packaging to re-run is `bitbake -C package`
  (`--clear-stamp`). Measured over this tree: `bitbake -C package rust-1.85.0-host
  rust-1.89.0-host libsdl3` re-ran `do_package`, `do_packagedata`, `do_package_qa`,
  `do_package_write_deb`, `do_create_spdx` and `do_create_runtime_spdx` and
  answered `Tasks Summary: Attempted 3363 tasks of which 3311 didn't need to be
  rerun and all succeeded`. `bitbake -cc package` is not that flag: bitbake's
  `-c/--cmd` takes an attached value, so `-cc package` reads as the cmd `c` plus
  the target `package` and answers `ERROR: Nothing PROVIDES 'package'. Close
  matches: package-index`.
- Re-running `do_package_write_deb` is not the same as writing a new file: when
  the content is what the earlier round's sstate already carries, the deploy
  dir keeps the same single-hard-linked file at the earlier date, which is why
  the four sizes above are stated with their 2026-10-17 dates rather than as
  "written today".
- `rm_work` is inherited, so this recipe's own work dir is gone after a build;
  the debs in the deploy dir are what can still be read, and `dpkg-deb -c` /
  `-f` on them are the round's own witnesses.

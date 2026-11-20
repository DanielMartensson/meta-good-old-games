# rust host toolchains - pins, measurements and the design record

Detail file for `rust-1.85.0-host.bb` and `rust-1.89.0-host.bb`.

## Pin

Both recipes fetch the official dist tarballs from `static.rust-lang.org/dist`
and pin every one by sha256; the four pieces are the compiler, the package
manager, the std for the build host's own triple and the std for the device
triple:

| Recipe | component | sha256 |
| --- | --- | --- |
| 1.89.0 | `rustc-1.89.0-x86_64-unknown-linux-gnu.tar.xz` | `b42c254e1349df86bd40bc28fdf386172a1a46f2eeabe3c7a08a75cf1fb60e27` |
| 1.89.0 | `cargo-1.89.0-x86_64-unknown-linux-gnu.tar.xz` | `99fc10be2aeedf2c23a484f217bfa76458494495a0eee33e280d3616bb08282d` |
| 1.89.0 | `rust-std-1.89.0-x86_64-unknown-linux-gnu.tar.xz` | `2719470dcd78b3f97d78b978c8f85a1a58d84ff11b62558294621c01bca34d49` |
| 1.89.0 | `rust-std-1.89.0-aarch64-unknown-linux-gnu.tar.xz` | `abea0955dded88c68d731524ab9d29b162fae23bf5805b9f1dec063cba37c2aa` |
| 1.85.0 | `rustc-1.85.0-x86_64-unknown-linux-gnu.tar.xz` | `7436f13797475082cd87aa65547449e01659d6a810b4cd5f8aedc48bb9f89dfb` |
| 1.85.0 | `cargo-1.85.0-x86_64-unknown-linux-gnu.tar.xz` | `0aff33b57b0e0b102d762a2b53042846c1ca346cff4b7bd96b5c03c9e8e51d81` |
| 1.85.0 | `rust-std-1.85.0-x86_64-unknown-linux-gnu.tar.xz` | `285e105d25ebdf501341238d4c0594ecdda50ec9078f45095f793a736b1f1ac2` |
| 1.85.0 | `rust-std-1.85.0-aarch64-unknown-linux-gnu.tar.xz` | `8af1d793f7820e9ad0ee23247a9123542c3ea23f8857a018651c7788af9bc5b7` |

Both recipes' `LIC_FILES_CHKSUM` pins the two license files inside the
rustc component - `LICENSE-MIT;sha256=23f18e03dc49df91622fe2a76176497404e46ced8a715d9d2b67a744657a`
and `LICENSE-APACHE;sha256=62c7a1e35f56406896d7aa7ca52d0cc022b5d2796e7d6905db8a3636` -
against `LICENSE = "MIT & Apache-2.0"`.

`RUST_HOST_TRIPLE = "x86_64-unknown-linux-gnu"` and `RUST_TARGET_TRIPLE =
"aarch64-unknown-linux-gnu"` in both, and `HOST_RUST_DIR = "${TMPDIR}/rust-<version>-host"`:
the toolchain's own dir under `${TMPDIR}`, not the native staging tree.

## The toolchain this staged, measured today on the build host

```
${TMPDIR}/rust-1.89.0-host    du -sk 726,588 kB   (lib/rustlib 330,932 kB)
  bin: cargo 39,124,328 B   rustc 654,128 B   rustdoc 13,198,768 B
       rust-gdb 980 B   rust-gdbgui 2,160 B   rust-lldb 1,072 B
  rustc --version   rustc 1.89.0 (29483883e 2025-08-04)
  cargo --version   cargo 1.89.0 (c24e10642 2025-06-23)
  lib/rustlib/x86_64-unknown-linux-gnu/lib    35 objects
  lib/rustlib/aarch64-unknown-linux-gnu/lib   34 objects
      libaddr2line-c31ab9d61a388beb.rlib, libadler2-015edeae3ea0f284.rlib,
      liballoc-162bc36d557e167b.rlib, libcfg_if-5f58d7e62c42cdaf.rlib, ...

${TMPDIR}/rust-1.85.0-host    du -sk 703,796 kB   (lib/rustlib 368,316 kB)
  bin: cargo 35,355,128 B   rustc 643,784 B   rustdoc 12,747,536 B
       rust-gdb 980 B   rust-gdbgui 2,160 B   rust-lldb 1,072 B
  rustc --version   rustc 1.85.0 (4d91de4e4 2025-02-17)
  cargo --version   cargo 1.85.0 (d73d2caf9 2024-12-31)
  lib/rustlib/x86_64-unknown-linux-gnu/lib    34 objects
  lib/rustlib/aarch64-unknown-linux-gnu/lib   33 objects
      libaddr2line-b5c2000e0cd7e2da.rlib, libadler-7522a3b17c9865c1.rlib,
      liballoc-dd37cd35aaa8bbc1.rlib, libcfg_if-d85d8ca815fd8ede.rlib, ...
```

Both dirs also carry `components`, `etc`, `install.log`, `rust-installer-version`,
`uninstall.sh` and one manifest per component (`manifest-cargo`,
`manifest-rustc`, `manifest-rust-std-<triple>`), which is what `install.sh --prefix=`
leaves, and `lib/rustlib/<triple>/lib/` is where cargo finds `std` for that
triple - which is why the `opennowlite` recipe's `cargo build --offline
--locked --lib --release --package opennow-streamer-ffi --target
aarch64-unknown-linux-gnu` needs no network at all.

The two toolchains differ by edition support, and that is the whole reason this
layer has two of them: the OpenNOW workspace is edition 2024 and needs 1.85 or
newer, and this build's default native toolchain is 1.75, which cannot compile
it. The released `v1.0.5` workspace at the pin the CloudMatch recipes carry now
needs 1.89, which is why `opennow` and `opennowlite` both depend on
`rust-1.89.0-host` - measured from `bitbake -e`:

```
opennow     DEPENDS ... libsdl3 vulkan-loader vulkan-headers libva wayland
                      wayland-protocols wayland-native libdrm udev nv-codec-headers
                      rust-1.89.0-host ...
opennowlite DEPENDS ... gstreamer1.0 gstreamer1.0-plugins-base
                      gstreamer1.0-plugins-bad wayland wayland-protocols wayland-native
                      libdrm udev vulkan-loader vulkan-headers libsdl3 cmake-native
                      rust-1.89.0-host
```

and `rust-1.85.0-host` is named by no recipe in this tree: it stays because the
older OpenNOW rounds built against it and its own `${TMPDIR}` dir is what those
rounds' numbers were read from.

## The debs this branch builds (built 2026-11-20 from this tree)

The whole layer was built again from the restructured tree at its own path, and
the dependency packages were built in the same round:

| deb | size | md5 |
| --- | --- | --- |
| `rust-1.89.0-host-dbg_1.0-r0.0_arm64.deb` | 1,036 B | `d4f25295be763ded95068c5edb7fa0b5` |
| `rust-1.89.0-host-dev_1.0-r0.0_arm64.deb` | 1,068 B | `153fe58750e2fe3659f7056257730c0e` |
| `rust-1.85.0-host-dbg_1.0-r0.1_arm64.deb` | 1,036 B | `ac9c99f8c31f43d034e5d31714335598` |
| `rust-1.85.0-host-dev_1.0-r0.1_arm64.deb` | 1,068 B | `a52c3daaf50c9feba105a6205c567d65` |

That is the deb shape, and it is a shape and not an accident: neither recipe
has a base deb at all - `ls` of the deploy dir under `rust-1.8[59].0-host_`
answers nothing - because these recipes install the toolchain into
`${TMPDIR}` and install nothing into `${D}`, so `${PN}` packages empty and only
the `-dbg` and `-dev` pieces are written. The two `-dbg` debs are 1,036 B and
the two `-dev` debs are 1,068 B, and `dpkg-deb -c` of them lists no regular
files, which is the same thing said from the other end.

`rust-1.89.0-host`'s debs are the ones the earlier round wrote: their files in
the deploy dir are dated 2026-10-17 with a single hard link, and its packaging
task chain's hashes match this tree, so the package is unchanged rather than
rebuilt-here. `rust-1.85.0-host` is the one that did get rewritten today, and
its version moved `1.0-r0.0` -> `1.0-r0.1`: forcing its packaging to re-run
makes the PR server hand out the next revision, so its two debs carry `r0.1`
with a 2026-11-20 date, with the same 1,036 B / 1,068 B sizes.

## How this recipe installs, and why each switch is there

- `rm -rf ${HOST_RUST_DIR}` at the top of `do_install`, then four `install.sh
  --prefix=${HOST_RUST_DIR}` runs: a rebuild gets a fresh toolchain rather than
  an overlaid one, so the staged dir is always exactly this recipe's pins.
- `CFG_DISABLE_LDCONFIG=1` in front of every `install.sh`: `install.sh` would
  otherwise run `ldconfig` and write into `/etc/ld.so.conf.d`, and this is a
  build-host side dir, so nothing here may touch the host's dynamic-linker
  config.
- `do_populate_lic[postfuncs] = ""`: the license-population QA postfunc resolves
  `LIC_FILES_CHKSUM`'s `file://` paths against the native scene's remapped
  `${WORKDIR}/${PN}-${PV}`, which does not contain the unpacked component
  dirs, so the check can never find them; the population task itself still runs
  and copies the generic MIT/Apache texts from `COMMON_LICENSE_DIR`.
- The three `[ -x ... ] || bbfatal` lines are the recipe's own sanity: `cargo`,
  `rustc` and the device triple's `lib/rustlib/<triple>` have to exist before
  `opennow`'s `do_compile` runs.
- The `rustc --version` sanity check is deliberately **not** in `do_install`:
  running the compiler under pseudo deadlocks, and the binaries are verified by
  the `-x` tests instead. The version lines above were measured by running
  `rustc --version` and `cargo --version` on the staged dir directly, outside
  pseudo.

## Failure modes

- The flag that forces these recipes' packaging to re-run is `bitbake -C package`
  (`--clear-stamp`). `bitbake -cc package` is not that: bitbake's `-c/--cmd`
  takes an attached value, so `-cc package` reads as cmd `c` plus the target
  `package`, and answers `ERROR: Nothing PROVIDES 'package'. Close matches:
  package-index` - the same shape as `-cc none`, which answers
  `Nothing PROVIDES 'none'`.
- These packages are never in an image (`BBCLASSEXTEND = "native"`, and the
  recipes are native-only), so a seat that says `rust-1.89.0-host` is not
  installed is right: the layer's dependency on them is a build-host
  dependency, not a runtime one. The runtime library a game needs from Rust is
  in the game's own deb, not here.
- `${TMPDIR}/rust-<version>-host` survives `rm_work` (it is not a work dir),
  which is what lets a later round re-measure the staged toolchain without
  rebuilding it - and what makes the `du -sk` numbers above readable at all.

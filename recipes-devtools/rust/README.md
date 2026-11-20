# rust host toolchains - load them and point a cargo build at them

Detail file for `rust-1.89.0-host.bb` and `rust-1.85.0-host.bb`. Instructions
only here; every pin, size, checksum and measurement is in
[PROOF.md](PROOF.md).

## Load them

```bash
bitbake rust-1.89.0-host rust-1.85.0-host
```

Nothing to install on the seat: these are build-host toolchains, `BBCLASSEXTEND
= "native"` keeps them out of every image, and their arm64 packages carry no
payload. `rust-1.89.0-host` is the one this tree uses; `rust-1.85.0-host` is
kept for the older OpenNOW rounds and no recipe in this tree depends on it now.

## Use them

A recipe that builds Rust on the build host points CMake or its own `cargo`
call at this recipe's own host dir:

```bash
${TMPDIR}/rust-1.89.0-host/bin/cargo
${TMPDIR}/rust-1.89.0-host/bin/rustc
```

which is what the `opennow` recipe hands to CMake as `-DCARGO_EXECUTABLE` and
`-DOPENNOW_LICENSE_RUSTC_EXECUTABLE`, and what the `opennowlite` recipe's `do_compile`
runs its `cargo build` with. The dir is `lib/rustlib/<triple>` for both the
build host's own triple `x86_64-unknown-linux-gnu` and the device triple
`aarch64-unknown-linux-gnu`, so `--target aarch64-unknown-linux-gnu` resolves
without cargo having to fetch anything.

The default native toolchain on this build (1.75, from `meta-rust-bin` /
`cargo-native`) stays where it is: these recipes install into their own
`${TMPDIR}` dir rather than into the native staging area, so the recipes that
want 1.75 keep it.

## Measured

Sizes, versions, deb shapes and the failure modes are in [PROOF.md](PROOF.md).

## Files

```
rust-1.85.0-host.bb   the 1.85.0 host toolchain
rust-1.89.0-host.bb   the 1.89.0 host toolchain
README.md             this file
PROOF.md              pins, sizes, checksums, measured facts, failure modes
```

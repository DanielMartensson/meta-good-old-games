SUMMARY = "OpenNOWLite - a separate Wayland streaming window for GeForce NOW"
DESCRIPTION = "OpenNOWLite is the plan-B client for this board: it opens its \
own Wayland window for the stream instead of hosting the picture in \
opennow-qt, draws it with GStreamer (waylandsink plus the SoC's stateless V4L2 \
H.264 decoder, fed with the vendor's H.264 and Opus bytes through appsrc) and \
keeps the seat's keyboard and pointer as a separate input pipeline. The native \
streamer of the released upstream OpenNOW v1.0.5 is used only to carry the \
input and the already-encoded media to Nvidia: it never decodes here, because \
the encoded packets are tapped out of it and handed to this program's own \
pipeline. Only the shipped opennow-core binary is asked for sign-in, the \
catalog and the CloudMatch consent, and only over its JSON-lines RPC."
HOMEPAGE = "https://github.com/OpenCloudGaming/OpenNOW"
BUGTRACKER = "https://github.com/OpenCloudGaming/OpenNOW/issues"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# The tree this recipe builds is fetched, not vendored: SRC_URI is OpenNOW's own
# repository at the same released pin the opennow recipe next door carries - the
# tag v1.0.5 = a9de957a5902c54186e9d4b44b1562f0b6beb951, the tip of refs/heads/main
# - and this recipe's one patch is applied on top of that tree by bitbake's do_patch
# task, with patch -p1 at ${S}. The
# part of that tree the recipe drives is native/opennow-streamer, and the round
# that moved here proved it the same tree the recipe used to vendor: `patch -p1
# --dry-run` of this patch from the pristine repo root checks exactly five files
# with rc=0, and `diff -rU0` of the patched subtree against the vendored copy
# answers 18 lines, every one of them an `Only in` line for the cruft `cargo-vendor`
# prunes (three `Cargo.toml.orig` files and the opus autotools/VS2015 set under
# `vendor/audiopus-sys/opus`). See PROOF.md for that witness. What moved here is
# therefore nothing but the fetch: `files/native` (22 M, 1028 files) went out of
# the tree because a fetch of upstream carries what it carried, and a pinned SRCREV
# is what keeps it reproducible. The v1.0.5 wording is measured, not relabelled:
# diffing the opennow-streamer subtree of the v1.0.4 and v1.0.5 release trees on
# the build host answers with no differing file, so the v1.0.5 bump moves the
# version string and nothing in the source.
# The file is still named opennowlite_git.bb because that name is what gives
# this package its name, opennowlite, which is what the boards' index carries.
SRC_URI = "git://github.com/OpenCloudGaming/OpenNOW.git;protocol=https;branch=main \
           file://0001-hand-encoded-media-to-the-host.patch \
           file://opennow-lite.c \
           file://opennowlite-launch.sh"
SRCREV = "a9de957a5902c54186e9d4b44b1562f0b6beb951"

# The version has to sort ABOVE what the boards already carry: 1.0.4+nightly.1515.1+git0+b3fe71026d-r2.11
# and the 1.0.5+release+git0+a9de957a59-r2.12 the seat carries today. ${SRCPV} is
# computed from the fetched SRCREV again, and the +release+ segment is still what
# buys the ordering: do_packagedata's version-going-backwards QA check compares
# version segments one by one, and a bare "1.0.5+git0+a9de957a59" loses against a
# "1.0.4+nightly..." string because "git0..." sorts below "nightly...". The segment
# also says what this pin is: the released v1.0.5 tag, not a nightly.
PV = "1.0.5+release+git${SRCPV}"

# The git fetch unpacks the repository under its own git/ directory, so the source
# root here is ${WORKDIR}/git and the crate tree is reached as
# ${S}/native/opennow-streamer: the same path, and the same cargo workspace, that
# the opennow recipe next door drives inside its own upstream checkout. The patch
# is applied at ${S}, which is why its hunks carry paths under native/. The two
# file:// entries land in ${WORKDIR} itself, which is where the app's compile line
# reads them from, and the generated protocol headers sit there too.
S = "${WORKDIR}/git"

# The one patch this recipe carries, and it IS applied: it is in SRC_URI after the
# git fetch, so bitbake's patch task runs it with patch -p1 over the fetched tree
# at ${S} before anything compiles - which is why its hunks carry paths under
# native/ and why the tree this recipe builds is upstream's tree plus this delta,
# the same shape the vendored tree used to be by hand. Upstream's streamer decodes
# the media itself and only publishes decoded frames, which is what opennow-qt needs
# and what this client does not want: paying for a decode here and re-encoding for
# GStreamer would spend the board's VPU twice for one picture. The patch adds an
# encoded-media tap to that same streamer, so the H.264 and Opus packets the vendor
# sends are forwarded to this program without being touched, while the Rust side
# keeps owning the NVST connection, the input protocol and the session's lifecycle.
# What it adds, and what the tree therefore contains once the task has run, is the
# EncodedMediaKind/EncodedMediaPacket/EncodedMediaTap pair, MediaSink::with_tap(),
# spawn_passthrough_linux(), a validate_backend() bypass for a device-less engine,
# and the encoded_media_callback plus media_passthrough fields of OpenNowStreamerConfig.
#
# The header the app includes is the one that lands in the fetched tree under
# native/opennow-streamer/crates/opennow-streamer-ffi/include, so the layout the app
# fills and the layout the Rust side validates are one file and cannot drift apart;
# that include directory carries exactly one header, opennow_streamer_ffi.h, which
# is what the patch's first hunk extends.

# PR-server counter resets to r0.0 whenever the recipe changes; pin it so the
# deb always installs over what the boards already carry. The seat carries
# 1.0.5+release+git0+a9de957a59-r2.12, whose client came from the vendored tree
# and this recipe's rev-1 banner source. The counter moves to r2.13 here because
# the client source moved with this re-home: opennow-lite.c went from 187175 B
# (md5 e5bc80c820f84f245a0ebd109753a0c7) to 187717 B (md5 36a9f99a029affa9c1117dabfa0a1528),
# the banner's rev 2, which keeps the queue line apart from the footer because the
# vendor only publishes a position on some polls. So the version has to sort above
# the ...-r2.12 on the seat, and dpkg -i has to answer over (...-r2.12) rather than
# the same-version refusal. What does NOT move the counter is the v1.0.4 to v1.0.5
# pin bump on its own: nothing in the opennow-streamer tree differs between those
# two release tags, measured by diffing the subtree on the build host, and that is
# also what the fetch keeps honest. The counter moves one more time to r2.14 on
# Nov 22 2026 because the source moved a second time rather than because anything
# else did: rev 3 at 206974 B (md5 d9dea2442e6817a7527823bd679ad244) adds the
# --search-game-id name->id read and raises the core reply bound CORE_READ_MAX
# from 262144 to 1048576 bytes, which is what one page of this account's own
# library list needs (331752 B measured on the seat), so the client that refuses
# to launch a name and the client that launches one are different clients and the
# version has to say so. The counter moves one more time to r2.15 on Nov 23 2026
# because rev 4 at 210162 B (md5 62b5d65000f1dda0290710e0fc87a2b1) moves the
# client again: the two launch flags take the names the user types at them
# (--search-game-id becomes --launch-game-name, which does read the id AND
# starts the game, and --launch-app-id becomes --launch-game-id), --help now
# prints the whole usage text while a wrong or unknown flag prints only the
# flags list above it rather than all the explanations, and every launch now
# moves the core's own `unresolved-allocation.json` - the dead session a
# failed allocation left, which otherwise blocks every new session with
# session_allocation_recovery_required - into `backup/<epoch>-unresolved-
# allocation.json` at its start, the same hand-move the audit rounds made
# before each round of them, now made by the client itself. That move did not
# work as rev 4 had it: rev 4 ignored what `mkdir` of the `backup` directory
# answered, so a data directory that has no `backup` yet - and every data
# directory the client is pointed at with `--data-dir` that the seat's own
# rounds did not use - answered `No such file or directory` out of the
# `rename` after it rather than out of the `mkdir` that had just failed, and
# the blocker stayed exactly where it was. So the counter moves one more time
# to r2.16 on Nov 24 2026 for rev 5 at 211555 B (md5 d9cdeb215c57b87bd37736c02c0eb741),
# which keeps the same move but reads what `mkdir` answered (a directory that
# is already there answers `EEXIST` and is kept, since the seat's own data
# directory has had a `backup` since the first hand-move of item 66) and
# names the directory it could not make in the log line when it is a real
# refusal, and which no longer lets two dead allocations that come down in
# the same second move onto one name: the second one goes to
# `backup/<epoch>-unresolved-allocation.json.1` rather than over the first,
# so the move can never eat an older dead allocation. What r2.16 is on the
# seat, though, is a move that still did not move: rev 5 names the variable
# that ALREADY carries `<data-dir>/backup` and then writes the destination as
# `<that>/backup/<epoch>-...`, so the name it points at sits one level below
# a `<data-dir>/backup/backup` that is never created, and the `rename` answers
# `No such file or directory` there. Measured twice: on the seat against a
# scratch data directory the client's own uid could write, where the log
# answered the move-refusal line and the blocker stayed in the data directory
# while the core's own `backup/` sat empty; and against the same code as a
# standalone replica off the layer, where three blockers planted one after
# another refused the same way every time. So the counter moves one more time
# to r2.17 on Nov 24 2026 for rev 6 at 211541 B (md5 aca3e5e4a7f1d09fc45df3b2e0163a5d),
# which is rev 5 with the destination written from that variable as it is
# rather than through a second `backup` level, so the move is
# `<data-dir>/backup/<epoch>-unresolved-allocation.json` - the same name the
# audit's own hand-moves used - and the collision bump stays off the same
# second, where the replica of this rev moved three blockers planted inside
# one second to `<epoch>-...json`, `.1` and `.2` and said nothing at all when
# a data directory carries no blocker.
PKGR = "r2.17"

DEPENDS = " \
    gstreamer1.0 \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-bad \
    wayland \
    wayland-protocols \
    wayland-native \
"

# Build-only, and each one comes from a place that demands it unconditionally,
# exactly as the opennow recipe documents for its CMake-driven cargo build:
#   - libdrm and udev: opennow-streamer-platform-linux's v4l2 module is
#     compiled with no #[cfg] at all on Linux, so the FFI build needs them
#     whatever cargo features are selected.
#   - vulkan-loader and vulkan-headers: the same crate's default feature is
#     vulkan, so its bindings compile with no cargo feature selected.
#   - libsdl3: opennow-streamer-platform depends on sdl2 unconditionally.
#   - cmake-native: sdl2-sys's build script cross-builds vendored SDL2 with
#     cmake, which has to come from the build host.
#   - rust-1.89.0-host: cargo, rustc and the aarch64 rust-std this workspace
#     (edition 2024) is built with.
DEPENDS += " libdrm udev vulkan-loader vulkan-headers libsdl3 cmake-native rust-1.89.0-host"

inherit pkgconfig

# The streamer's C-compiling dependencies (sdl2-sys, opus bindings, the cc
# crates) resolve their compilers and tools the way the opennow recipe had to
# teach it, so this is the same environment that recipe installs:
#   - ONE PATH export, with the rust host toolchain first, the task's own
#     native sysroot (which carries cmake-native and the wrapped cross
#     drivers) in the middle and /usr/bin last, where the host's plain cc,
#     gcc, g++, ld, ar and as live. A second, later "export PATH=${PATH}:/usr/bin"
#     would clobber the rust prefix and cargo would fail with rustc not found.
#   - CARGO_HOME: the shared, already primed crate cache, and --offline
#     --locked so this build never reaches for the LAN.
#   - HOST_CC=gcc: cc-rs runs host-side build scripts (the ones compiling
#     C for the build machine) and CC here is the aarch64 target driver,
#     which rejects x86-only flags, so host compiles are steered at gcc.
#   - CMAKE: sdl2-sys's build script cross-builds vendored SDL2 with cmake
#     and takes the program from here rather than from PATH.
do_compile:prepend() {
    export PATH="${TMPDIR}/rust-1.89.0-host/bin:${PATH}:/usr/bin"
    export CARGO_HOME="${HOME}/.cargo"
    export HOST_CC="gcc"
    export CMAKE="${WORKDIR}/recipe-sysroot-native/usr/bin/cmake"

    # The aarch64 cross drivers staged for this recipe have a dead default
    # sysroot (/not/exist), so any bare aarch64-ostl-linux-gcc invocation
    # cannot find the target libc, crt1.o, crti.o or crbegin.o. Replace the
    # unversioned drivers in place with a wrapper that re-executes the real
    # driver with the machine flags and an absolute --sysroot, the same
    # idiom the opennow recipe uses: in place rather than as a PATH shadow,
    # so a bare name through PATH and a cached full path both reach the
    # wrapper, and idempotent, because a wrapper's first bytes are the sh
    # shebang and a restored ELF driver's are not.
    tcdir="${WORKDIR}/recipe-sysroot-native/usr/bin/aarch64-ostl-linux"
    for n in gcc g++ c++; do
        pw="${tcdir}/aarch64-ostl-linux-${n}"
        [ -f "${pw}" ] || continue
        [ "$(head -c 9 "${pw}" 2>/dev/null)" = "#!/bin/sh" ] && continue
        mv "${pw}" "${pw}.real"
        printf '#!/bin/sh\n# opennowlite-sysroot-wrap\nexec %s/aarch64-ostl-linux-%s.real -mcpu=cortex-a35+crc -mbranch-protection=standard --sysroot="%s/recipe-sysroot" "$@"\n' \
            "${tcdir}" "${n}" "${WORKDIR}" > "${pw}"
        chmod +x "${pw}"
    done

    # cargo's cross build needs the target's own link driver, and it has to be
    # the wrapped one above: the aarch64 drivers have a dead default sysroot, so
    # a bare name would fail with ld: cannot find crt1.o/crti.o/crtbegin.o. The
    # opennow recipe's conditional block exported the plain "gcc" and let PATH
    # resolve it to this toolchain directory; naming the wrapped driver itself
    # here keeps the request independent of PATH ordering.
    export CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER="${WORKDIR}/recipe-sysroot-native/usr/bin/aarch64-ostl-linux/aarch64-ostl-linux-gcc"
}

# The window is this program's: an xdg-shell toplevel created here, with the
# Wayland display handed to waylandsink through GstWlDisplayHandleContextType
# and the surface through the GstVideoOverlay window handle, so no Qt and no
# compositor module is involved and the picture fills the window this program
# owns. xdg-shell is generated from the sysroot's wayland-protocols.
#
# The FFI library is built for this machine out of the fetched tree with this
# recipe's patch already applied to it, with no backend feature selected: the
# media path here is a passthrough, so the opennow-streamer backends (VA-API,
# bundled FFmpeg and its nv-codec-headers) are not needed and are not pulled in.
# The result is installed under this
# recipe's own directory and dlopened by absolute path, so OpenNOW's own
# /usr/lib/libopennow_streamer_ffi.so is never written over, never linked
# against and never loaded by this process' link step.
do_compile() {
    wayland-scanner client-header \
        ${STAGING_DATADIR}/wayland-protocols/stable/xdg-shell/xdg-shell.xml \
        ${WORKDIR}/xdg-shell-client-protocol.h
    wayland-scanner private-code \
        ${STAGING_DATADIR}/wayland-protocols/stable/xdg-shell/xdg-shell.xml \
        ${WORKDIR}/xdg-shell-protocol.c

    # The relative-pointer protocol carries the hand's own unclamped delta,
    # which is what lets the camera turn past the window edge; the pointer-
    # constraints protocol keeps the seat's pointer on this surface so those
    # deltas keep arriving. Both are unstable v1 and both are staged by the
    # wayland-protocols dependency above.
    wayland-scanner client-header \
        ${STAGING_DATADIR}/wayland-protocols/unstable/relative-pointer/relative-pointer-unstable-v1.xml \
        ${WORKDIR}/relative-pointer-unstable-v1-client-protocol.h
    wayland-scanner private-code \
        ${STAGING_DATADIR}/wayland-protocols/unstable/relative-pointer/relative-pointer-unstable-v1.xml \
        ${WORKDIR}/relative-pointer-unstable-v1-protocol.c
    wayland-scanner client-header \
        ${STAGING_DATADIR}/wayland-protocols/unstable/pointer-constraints/pointer-constraints-unstable-v1.xml \
        ${WORKDIR}/pointer-constraints-unstable-v1-client-protocol.h
    wayland-scanner private-code \
        ${STAGING_DATADIR}/wayland-protocols/unstable/pointer-constraints/pointer-constraints-unstable-v1.xml \
        ${WORKDIR}/pointer-constraints-unstable-v1-protocol.c

    # The app first, so a broken app fails this task in seconds instead of
    # after the Rust library below has been built. -ldl: the streamer's
    # symbols come from dlopen, and this recipe must not carry OpenNOW's
    # library at link time. -I${WORKDIR} is where the generated protocol
    # headers are, and the second -I is the vendored tree's own copy of the
    # streamer's C header, so opennow-lite.c is compiled against the very
    # header the library below is built from.
    #
    # gstreamer-app-1.0 is the appsrc library (libgstapp-1.0.so.0): it is a
    # separate shared object from libgstreamer-1.0, so GstAppSrc's symbols are
    # undefined at link time without it, and it has its own pkg-config file
    # rather than being part of gstreamer-1.0's Libs.
    ${CC} ${CFLAGS} ${CPPFLAGS} -I${WORKDIR} \
        -I${S}/native/opennow-streamer/crates/opennow-streamer-ffi/include \
        -o opennow-lite \
        ${WORKDIR}/opennow-lite.c ${WORKDIR}/xdg-shell-protocol.c \
        ${WORKDIR}/relative-pointer-unstable-v1-protocol.c \
        ${WORKDIR}/pointer-constraints-unstable-v1-protocol.c \
        ${LDFLAGS} -ldl $(pkg-config --cflags --libs gstreamer-1.0 gstreamer-app-1.0 gstreamer-video-1.0 gstreamer-wayland-1.0 wayland-client)

    # The streamer library, from the vendored tree, for this machine.
    # No --target would be the opennow idiom, but it is not what this graph
    # needs: the lite build selects no backend feature, so the vendored OpenH264
    # C++ is compiled from source by openh264-sys2 with cc-rs, and cc-rs takes
    # the architecture to compile for from cargo's TARGET build-script variable.
    # Without --target that is the *host* triple, because rust-1.89.0-host's
    # rustc prints no "Target:" line in --version --verbose (confirmed here), so
    # cc-rs believes it is on x86_64 and appends the host bitsize switch to
    # every aarch64 compile:
    #     aarch64-ostl-linux-g++.real: error: unrecognized command-line option
    #     '-m64'
    # which fails the openh264-sys2 build command. The opennow recipe never hit
    # this because linux-ffmpeg-bundled gets the H.264 codec from the bundled
    # FFmpeg instead, so no cc-rs C++ is compiled there. rustc already produced
    # aarch64 code through its built-in default target, so naming the triple
    # only makes cargo state it, which gives cc-rs the right architecture and
    # puts the artifacts one directory deeper.
    cd ${S}/native/opennow-streamer
    cargo build --offline --locked --lib --release \
        --package opennow-streamer-ffi --target aarch64-unknown-linux-gnu \
        --target-dir ${WORKDIR}/rust-target
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 opennow-lite ${D}${bindir}/opennow-lite
    install -m 0755 ${WORKDIR}/opennowlite-launch.sh ${D}${bindir}/opennowlite

    # The streamer library this recipe built, in its own directory: the file
    # name is this package's, so nothing here can be confused with the
    # library OpenNOW ships, which stays byte-identical on the board. With
    # --target the artifact sits under the triple's own directory.
    install -d ${D}/usr/lib/opennowlite
    install -m 0644 ${WORKDIR}/rust-target/aarch64-unknown-linux-gnu/release/libopennow_streamer_ffi.so \
        ${D}/usr/lib/opennowlite/libopennowlite_streamer_ffi.so
}

FILES:${PN} += "${bindir}/opennow-lite ${bindir}/opennowlite /usr/lib/opennowlite/libopennowlite_streamer_ffi.so"

# Runtime pieces: waylandsink is the bad feature package that also carries
# libgstwayland-1.0.so.0, appsrc lives in gstreamer1.0 (libgstapp-1.0.so.0,
# which the link line takes from gstreamer-app-1.0.pc), the SoC's
# stateless H.264 decoder v4l2slh264dec is already in this image, and the
# Opus element the audio path needs is gstreamer1.0-plugins-base's own
# split package for its opus feature, which layers/meta-watermelon-wine's
# bbappend turns that feature on for. libopus is the runtime library that
# element and the streamer library link against.
RDEPENDS:${PN} += " \
    gstreamer1.0 \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-bad-waylandsink \
    gstreamer1.0-plugins-base-opus \
    libopus \
"

# The Rust library is linked by rustc's own driver and arrives pre-stripped,
# without the GNU hash table the ldflags test looks for and with the build
# directory inside its debug info. All three are the same expected facts the
# opennow recipe records for its Rust artifacts. The C app is not pre-stripped,
# so it gets a -dbg package, and that .debug file still names the workdir the
# xdg-shell protocol C file was compiled in, which is the same buildpaths
# finding the opennow recipe records for its own artifacts.
INSANE_SKIP:${PN} += "already-stripped ldflags buildpaths"
INSANE_SKIP:${PN}-dbg += "buildpaths"

# The launcher and the client are runtime data of the base package.
KEYWORDS = "GeForce;cloud;gaming;lite;window;"

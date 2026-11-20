SUMMARY = "Xash3D FWGS - the GoldSrc engine, built as an OpenGL ES client for the seat"
DESCRIPTION = "Xash3D FWGS is the portable reimplementation of the GoldSrc engine \
that Half-Life and its add-ons run on, and the seat cannot run the engine the 1998 \
release shipped: that one is a Win32 binary, and the x86 Linux build FWGS still \
publishes is an i386 library pair this board has no loader for. What this recipe \
builds is the engine as a native aarch64 shared library (libxash.so) plus the launcher \
its own game_launch/game.cpp puts beside it (/usr/bin/xash3d, which dlopens libxash.so \
and calls Host_Main), and exactly one renderer: the gles3compat ref_dll. That renderer \
is the only one this seat can drive. The board carries EGL, GLES1, GLES2 and GLES3 and \
the Vivante GAL/VSC driver and has no desktop GL at all (no libGL.so on any search \
path, eglBindAPI(EGL_OPENGL_API) answers EGL_BAD_PARAMETER and EGL_CLIENT_APIS names \
only OpenGL_ES and OpenVG), so the plain ref_gl renderer is out twice over: with XASH_GLES \
unset, ref/gl/gl_opengl.c:1492 GL_SetupAttributes asks SDL for a COMPATIBILITY-profile \
context, and ref/gl/wscript gives the ref_gl target a -lGL line when --enable-static-gl \
is on. gles3compat is the target that compiles its own GL types (ref/gl/gl_export.h:35 \
turns XASH_GLES3COMPAT into XASH_GLES 1 and declares the entry points itself) and takes \
every one of them at run time through SDL, so no GL header or GL library is needed at \
build time either; at run time GL_SetupAttributes asks for an ES profile, which is what \
the seats EGL driver hands out. --enable-packaging is what places the pieces the way a \
packaged system wants them, and with --prefix=/usr that spells the whole install: \
/usr/bin/xash3d for the launcher, /usr/lib/xash3d for libxash.so, libref_gles3compat.so \
and libmenu.so (the mainui menu), /usr/share/xash3d for the data root, which is where \
/usr/share/xash3d/valve/extras.pk3 from the xash-extras submodule lands. The game tree \
itself is not here, and no GoldSrc game tree is a recipe in this layer: none of the \
GoldSrc games this round plays on the seat carries an author's own grant of free \
download, so Half-Life: Up-link stays a locally installed test on the seat's own \
/gamedata mount and this recipe's README.md carries that verdict with the quotes, \
plus the whole how-to-run-it: the writable directory, delta.lst, the gamedll pair, \
the extras.pk3 and the save games. This recipe installs no launcher of its own."
HOMEPAGE = "https://github.com/FWGS/xash3d-fwgs"
BUGTRACKER = "https://github.com/FWGS/xash3d-fwgs/issues"

LICENSE = "GPL-3.0-or-later"
LIC_FILES_CHKSUM = "file://engine/common/common.c;beginline=1;endline=15;md5=2839f32588989257fbf1c08fff5d71fe"

# The tree carries no top-level LICENSE file to checksum, and the engine headers
# all carry the same banner: engine/common/common.c lines 1 to 15 are the GPL-3
# notice ("either version 3 of the License, or (at your option) any later
# version"), 369 of the C sources say version 3 and none of them says version 2,
# so the license is GPL-3.0-or-later and the checksum is taken on those first
# fifteen lines. The Half-Life game libraries are NOT in this tree: they come
# from Valve's own Half-Life 1 SDK, whose agreement is LicenseRef-Valve-Half-Life-1-SDK
# and is not a free licence, so the gamedll pair this seat runs (valve/dlls/hl_arm64.so
# and valve/cl_dlls/client_arm64.so) is built from that SDK and installed on the seat
# locally rather than shipped as a recipe in this layer.

# gitsm and not git, because the engine is built out of submodules, not beside
# it: mainui (the menu this recipe installs as libmenu.so), xash-extras (the
# extras.pk3 that goes into /usr/share/xash3d/valve), library-suffix (COM_GenerateCommonLibraryName,
# which spells the _arm64 suffix the game libraries get), MultiEmulator,
# mbedtls, libbacktrace, and the codec sources bzip2, mpg123, opus, opusfile,
# libogg and vorbis. Those last six are only built when the system does not
# already have them, and DEPENDS below is what keeps them off the build. The
# submodules whose .gitmodules row says update = none (nanogl, gl-wes-v2,
# gl4es, maintui, glslang, opengnm, Vulkan-Headers) are gitlinks in the
# recorded tree too, so gitsm fetches them and the build then ignores them:
# their Subproject rows in wscript are gated on NANOGL, GLWES, GL4ES, TUI and
# on DEST_OS == ps4, and none of those is on in this configuration.
SRC_URI = "gitsm://github.com/FWGS/xash3d-fwgs.git;protocol=https;branch=master"

# Pin: upstream master tip when this recipe was written, 707596f4
# "public: reformat with xash-clang-format" by Alibek Omarov, 10 Nov 2026.
# Pinned rather than left at HEAD because the configure line below is a
# statement about this tree: --enable-gles3compat exists as a RefDll row
# (wscript:169), the ref/gl Subproject row that consumes it is gated on
# GL or NANOGL or GLWES or GL4ES or GLES3COMPAT (wscript:98), and the
# update = none submodule rows above are read from this exact .gitmodules.
SRCREV = "707596f4905689ec224e993fcaa4d4ad66aa67d6"

# The tree carries no version file to read a release out of (the engine version
# is a build number in public/build.h), so the deb version is the pin.
PV = "0.0+git${SRCPV}"

# PKGR pinned like every other recipe in this layer, so the deb version stays
# comparable across rebuilds.
PKGR = "r0.01"

S = "${WORKDIR}/git"

# What the binaries link against. SDL2 is what the engine is built on
# (engine/wscript adds platform/sdl2/*.c and links SDL%d once XASH_SDL is set,
# and the sdl2 waf tool is loaded at wscript:174), and freetype2 is a hard
# requirement, not a nicety: 3rdparty/mainui/wscript calls check_pkg(freetype2)
# and scripts/waifulib/fwgslib.py:160 check_pkg defaults fatal = True, so
# configure stops with "freetype2 not available" when it is missing. The rest
# of the list is what engine/wscript:315 puts on the client library link line
# (bzip2, mpg123, MultiEmulator, opus, opusfile, vorbis, vorbisfile): with
# --enable-bundled-deps off, waf checks each of them through pkg-config
# (check_system_pkg at wscript:674) and builds its own copy from the matching
# submodule only when the system answer is no. The seat already carries
# libogg.so, libvorbis.so, libvorbisfile.so, libopus.so, libopusfile.so,
# libbz2.so and libz.so, so naming them here keeps the six bundled builds out
# of the recipe and leaves the .so list of what is installed short.
#
# The inherit line further down pulls in pkgconfig-native for a different
# reason: mainui's waf step finds freetype2 only through pkg-config, and the
# waf check runs on the build host PATH, so the target pkgconfig package (which
# does exist in OE-core) is no help there.
DEPENDS = "libsdl2 freetype zlib bzip2 libogg libvorbis libopus opusfile"

# Same names at run time, plus the sound sink: SDL opens the ALSA device
# itself (SDL_AUDIODRIVER=alsa in the run helper this README writes), so libasound.so is
# wanted here but not linked, which is why it sits in RDEPENDS and not in
# DEPENDS. alsa-lib is the same library the fteqw and eduke32 recipes next
# door already pull in, so nothing new has to go into the image for this one.
RDEPENDS:${PN} = "libsdl2 freetype zlib bzip2 libogg libvorbis libopus opusfile alsa-lib"

# Waf only; see the note on freetype2 above DEPENDS for why pkgconfig-native
# has to be here as well.
inherit pkgconfig

# No configure script and no Makefile: waf. These flags are one statement
# about what is built, and each of them is the reason for that statement:
#
#   --enable-packaging (wscript:196) is what makes PREFIX, LIBDIR, BINDIR and
#     SHAREDIR mean anything. Without it wscript:657 sets them all to PREFIX,
#     so the launcher, libxash.so and libmenu.so would sit in the same
#     directory and the data root would have no name of its own. With it, and
#     with --prefix=/usr, wscript:655 spells SHAREDIR = ${PREFIX}/share/xash3d
#     and wscript:656 adds /xash3d to LIBDIR, which is what the run script
#     repeats in its LD_LIBRARY_PATH, -rodir and -ref arguments.
#   --enable-gles3compat (RefDll row at wscript:169) selects the ref_gles3compat
#     target in ref/gl/wscript, which compiles ref/gl/*.c with
#     -DXASH_GLES3COMPAT=1 and links nothing: no -lGL, no GL headers.
#   --disable-gl takes the ref_gl target out. It is the default RefDll row
#     (wscript:170 RefDll(gl, True)), so leaving it on would build a second
#     renderer whose context request (gl_opengl.c:1492, compatibility profile)
#     and whose -lGL line the seat cannot answer.
#   --disable-werror (wscript:208) leaves waf out of its own strict block, the
#     list at wscript:313 of some thirty -Werror=<diagnostic> entries. That
#     list is aimed at the warnings gcc 13 raises on this tree, and the
#     OE CFLAGS already carry their own -Werror policy; the alternative here
#     is a configure that fails before anything is compiled.
#
# What is deliberately NOT switched on: --enable-dedicated (the xash dedicated
# server program is not wanted on a seat that plays single player), --enable-
# bundled-deps (see DEPENDS), --enable-tui (its Subproject row wants the
# maintui submodule, whose .gitmodules row says update = none) and --64bits
# / --32bits, which only mean anything to an x86 target.
do_configure() {
    cd ${S}
    ./waf configure \
        --enable-packaging \
        --prefix=${prefix} \
        --enable-gles3compat \
        --disable-gl \
        --disable-werror
}

do_compile() {
    cd ${S}
    ./waf build -j ${BB_NUMBER_THREADS}
}

# waf install with --destdir=${D} is the packaging route: every install_path in
# the tree is absolute or PREFIX-relative, so the destdir lands them at
# ${D}/usr/bin/xash3d, ${D}/usr/lib/xash3d/libxash.so,
# ${D}/usr/lib/xash3d/libref_gles3compat.so, ${D}/usr/lib/xash3d/libmenu.so and
# ${D}/usr/share/xash3d/valve/extras.pk3. --destdir is wanted over the
# DESTDIR environment variable because this is the same spelling hlsdk-portable
# uses, whose top wscript (hlsdk wscript:310) fatals without it.
do_install() {
    cd ${S}
    ./waf install --destdir=${D}
}

# The launcher waf installs is spelled xash3d and takes an engine-side
# argument set, not a game: it has no idea which tree to open, where the
# writable directory is or which renderer the seat can drive. That set belongs
# to the payload, and no payload recipe is in this layer any more, so the
# launcher that matters is the one this README writes. What stays here is one
# note for anyone hand-running it: because
# DEFAULT_RPATH is $ORIGIN (wscript:571), libxash.so is looked for next to
# /usr/bin/xash3d first and is not there, so LD_LIBRARY_PATH=/usr/lib/xash3d
# has to be in the environment for the four libraries in that directory to
# resolve at all. No payload recipe names this package from the other side any
# more, so the pair is declared once, in this README, and the run helper the
# seat actually uses is written out there in full.

# Four libraries and not three: libxash.so (the engine core), libmenu.so (the
# mainui submodule), libref_gles3compat.so (the renderer picked by
# --enable-gles3compat) and filesystem_stdio.so, which is not a plugin someone
# chose to turn on but the unconditional bld.shlib in filesystem/wscript, the
# WAD/PAK/searchpath layer. It loses the lib prefix because filesystem/wscript:
# 33 rewrites cxxshlib_PATTERN for every target except android.
#
# FILES has to say both roots: autoconf's default list ships ${bindir} and
# lib*.so files that sit directly in ${libdir}, and neither of the two roots
# below is in either shape, which is why the same line the ioquake3 and opennow
# recipes carry is needed here.
FILES:${PN} += "${libdir}/xash3d ${datadir}/xash3d"

SUMMARY = "DOSBox-X - the x86 DOS emulator this seat runs the Descent shareware episodes on"
DESCRIPTION = "Descent and Descent II are not id's games: they are Parallax Software's, published by Interplay, and there is no idgames-style free release of them, no source release and no native port to build. The two shareware trees this round put on the seat's own /gamedata mount carry an INSTALL.EXE and the multi-volume .SOW archives that unpack to a 1995 DOS tree, and the executable that tree finally runs is a DOS/4GW binary this seat cannot load by itself. There is nothing else on the board that speaks x86 DOS, so this is the engine the two games run on, and it is one engine for both. Neither Descent is a recipe in this layer, and the reason is written in this recipe's README.md: Interplay's own release notes do hand out free distribution of the shareware episode and of the three-level demo, but the only copies on hand come off an Internet Archive community item, which is not an authorized download source, so the two games stay a locally installed test of this engine, installed on the seat's own /gamedata mount. \
The two things the seat needs from it are decided by the tree, not by guesswork. First, SDL2 is not a preference: src/gui/sdlmain.cpp:309 is #if !defined(C_SDL2) ... #error, so a build whose SDL probe failed does not get as far as a link line, and C_SDL2 is defined only by the AM_PATH_SDL2 that the tree ships in its own acinclude.m4 (:34-:92), whose finding route at :74 is pkg-config --exists sdl2 with sdl2-config as the other answer. There is no sdl2-config in this sysroot, because libsdl2's BINCONFIG (layers/openembedded-core/meta/recipes-graphics/libsdl2/libsdl2_2023.0.1.bb:32) belongs to the main package and only the -dev half populates the sysroot, but the sysroot does carry usr/lib/pkgconfig/sdl2.pc, which is what makes the pkg-config branch the one that has to answer, and that branch needs the tool itself, which is what the pkgconfig class at layers/openembedded-core/meta/classes-recipe/pkgconfig.bbclass:7 puts in recipe-sysroot-native/usr/bin. Second, the pixels: DOSBox-X draws the DOS framebuffer into an SDL_Surface and hands it over with SDL_UpdateWindowSurface (src/gui/sdlmain.cpp:1509) and SDL_UpdateWindowSurfaceRects (:1508 area, the overscan path at :1523) and fills the border with SDL_FillRect (:1512), the same window-surface shape the chocolate-doom recipe in this layer relies on, so SDL2's own renderer decides what the upload is and no GL context is ever opened here. --disable-opengl is what keeps GL out of this build: the default at configure.ac:609 is enable_opengl=yes, whose body at :622 runs pkg-config gl, at :626 adds pkg-config gl --libs and at :628 falls back to -lGL, none of which the seat can answer, and C_OPENGL is also what pulls the desktop-GL includes at src/gui/sdlmain.cpp:298-:320. --disable-freetype is the matching one for text: the default at configure.ac:980 asks freetype2 through pkg-config at :989 and adds -lfreetype at :993, and its only use is the TTF fonts that install next to the binary; without it the DOS text mode is drawn from the compiled-in VGA font and the FREECG98.BMP bitmap that is still installed. The mouse is SDL2's own: src/gui/sdlmain.cpp:2690 SDL_SetRelativeMouseMode(SDL_TRUE) and :2699 back to SDL_FALSE are the captured-pointer pair the DOS mouse uses, so a game that reads INT 33 gets SDL2's relative deltas, which is what a mouselooking Descent wants. \
The cpu core is the part worth pinning the truth of, because it is what the seat's 1.2 GHz Cortex-A35 lives or dies on: configure.ac:829-:835 is the aarch64) case, which sets AC_DEFINE(C_TARGETCPU,ARMV8LE) and c_targetcpu=arm, and the dynrec gate at :1181-:1188 accepts xarm, so C_DYNREC is defined and the code generator in src/cpu/core_dynrec.cpp:165, the #elif C_TARGETCPU == ARMV8LE branch, is compiled. The x86 dynamic core is the one that does NOT come on: its gate at :1172-:1186 needs x86 or x86_64, so this build gets the recompiling ARM core and not a translator for the wrong host. Whether that core is fast enough to play Descent on this board is what the seat round measures, and the number lands in this README rather than here. \
The config file is what makes the launchers the way they are: the search order at src/gui/sdlmain.cpp:8745-:8776 is -conf files first, then the current directory (:8748 ParseConfigFile(\"dosbox-x.conf\")), then the directory the executable lives in (:8763, GetDOSBoxXPath at :8665 and :8711) and only then Cross::GetPlatformConfigDir() (:8712). Run from /usr/bin with nothing next to it, DOSBox-X would look in a read-only place, so each game's own run cds into a per-user writable directory that carries its own dosbox-x.conf and lands in the first slot that is answered. \
Licence: DOSBox-X itself is GPL-2 (COPYING is the v2 text, and 106 sources carry the \"or (at your option) any later version\" line, none carries a version 3), which says nothing about the DOS games it runs; those are the owner's own copies, installed by hand, and this recipe installs no data at all. This build is for my own use on this seat and is not redistributed."
HOMEPAGE = "https://dosbox-x.com"
BUGTRACKER = "https://github.com/joncampbell123/dosbox-x/issues"

# COPYING is GNU's plain v2 text, md5 13df4b3611f08fada0e7fa65465a92cc, and it
# is the only COPYING in the tree; the -or-later half is what the sources say,
# 106 of them carrying the "or (at your option) any later version" line of the
# v2 boilerplate. Nothing in src/ carries a version 3 text: the grep for "GPL"
# next to "version 3" over the tree is empty, so nothing here has to be
# downgraded to GPL-2.0-only.
LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://COPYING;md5=13df4b3611f08fada0e7fa65465a92cc"

# Pin: the repository's own tip at the moment this round was written, 78c02293
# 1f0ad9e567bcaaefc155fc428d9bdc7e, "Merge pull request #6819 from cimarronm/
# fix-manifest-con-layout" of Thu Nov 12 2026 -0800, which is the same commit
# the repository answers for either name (git ls-remote HEAD answers it for
# joncampbell123/dosbox-x). No tag is used: the tree carries release tags but
# the shape the layer's other git recipes use is the commit, and the tag-name
# form does not parse (see the same note the chocolate-doom recipe carries).
#
# The url is the user's path rather than the org's: https://github.com/dosbox-x/
# dosbox-x answers 401 from this build machine ("git ls-remote" there prints
# "fatal: could not read Username for 'https://github.com'"), which is what the
# fetcher would see as a fatal clone error, while joncampbell123/dosbox-x is
# the repository that answers the dumb protocol and the org path redirects to.
SRC_URI = "git://github.com/joncampbell123/dosbox-x.git;protocol=https;branch=master"
SRCREV = "78c022931f0ad9e567bcaaefc155fc428d9bdc7e"

# The version the tree itself declares: configure.ac:2 is AC_INIT(dosbox-x,
# 2026.10.01, ...), so the deb's version is the upstream release rather than a
# date this side made up. PKGR is pinned like the rest of this layer so the
# version stays comparable across rebuilds.
PV = "2026.10.01"
PKGR = "r0.01"

# configure.ac and Makefile.am only: the repository ships no generated configure
# (ls of the tree answers autogen.sh and configure.ac, and autogen.sh:20-:23 is
# aclocal, autoheader, automake --include-deps --add-missing --copy and autoconf,
# the same four the class runs as autoreconf -Wcross --install --force), so the
# class regenerates the build system and builds outside the source tree.
S = "${WORKDIR}/git"

# autotools for the reason above, pkgconfig because the SDL probe this tree
# ships is answered through pkg-config: acinclude.m4:74 is the "if test
# x$manual_sdl2config = xno && pkg-config --exists sdl2" branch, and without
# the tool on PATH manual_sdl2config is no, the branch cannot run, SDL2_LIBS
# stays empty and configure.ac:536 prints its own error for that case,
# "--enable-sdl2 was specified, but SDL2 was not found" - and even if that
# error were skipped, src/gui/sdlmain.cpp:309's #error says the tree does not
# compile without C_SDL2, which only that same branch defines. The recipe next
# door carries the same pair of reasons in its own words.
inherit autotools pkgconfig

# The three libraries the probes in configure.ac answer for: SDL2 through the
# tree's own AM_PATH_SDL2 (:533), zlib through AC_CHECK_LIB(z, inflateEnd) at
# :883 and libpng through AC_CHECK_LIB(png, png_get_io_ptr) at :887, which is
# what makes the built-in screencap write a PNG rather than a raw dump and is
# how the draw proof of a game run on it gets its pictures. What is deliberately
# NOT a dependency is what the configure flags below take out: freetype
# (configure.ac:989-:993), the ffmpeg avcodec block, libslirp (:895) and
# libfluidsynth, plus the pcap probe at :891, which finds nothing in this sysroot
# and stays out on its own. alsa-lib IS here, and the reason is measured rather
# than assumed: objdump -p of the installed /usr/bin/dosbox-x answers
# libasound.so.2, libSDL2-2.0.so.0, libz.so.1, libpng16.so.16, libstdc++.so.6,
# libm.so.6, libgcc_s.so.1 and libc.so.6, so the AM_PATH_ALSA at :853 did
# succeed in this build (alsa-lib is in the sysroot, reached through libsdl2)
# and the link line does carry -lasound. That is also the two lines this
# dependency buys nothing by accident: no libGL and no libfreetype are in that
# list, which is what --disable-opengl and --disable-freetype below are for.
DEPENDS = "libsdl2 alsa-lib libpng zlib"
RDEPENDS:${PN} = "libsdl2 alsa-lib libpng"

# One line of intent, and every entry is a thing the seat has no answer for:
# --disable-opengl is the desktop-GL block described in the DESCRIPTION above
# (configure.ac:609 default yes, :622-:631 the pkg-config and -lGL answers); the
# seat has no libGL and no X, its only GL is the Vivante's EGL/GLES, and the
# window-surface path needs none of it. --disable-freetype is the TTF text
# rendering at configure.ac:980-:993, which would otherwise drag the four TTF
# files (SarasaGothicFixed.ttf alone is 16677912 B) into an image with very
# little room left, and the DOS text mode keeps its compiled-in VGA font plus
# the FREECG98.BMP bitmap that is still installed. --disable-avcodec is the
# ffmpeg block: ffmpeg is in this sysroot, so the default would link a 30 MB
# dependency into a DOS emulator that records nothing here. --disable-libslirp
# and --disable-sdlnet are the network-play backbones (:895 and the SDL2_net
# option), which the seat plays single player against. --disable-libfluidsynth
# is the MIDI soundfont backend while the games' music goes through DOS's own
# driver. What stays on is what Descent needs: the recompiling cpu core (left
# alone so the aarch64 case at :829-:835 turns the ARM codegen on), the fpu at
# :1209, the screenshots at :1323 and the printer at :1024, all in-tree and
# asking nothing of the board.
EXTRA_OECONF = "\
    --enable-sdl2 \
    --disable-opengl \
    --disable-freetype \
    --disable-avcodec \
    --disable-libslirp \
    --disable-sdlnet \
    --disable-libfluidsynth \
"

# The install rule this tree writes for itself (Makefile.am:142-:172) is not the
# automake one: it copies the binary, then a set of data files, then four things
# that belong to a desktop, and then a line that only runs if the host has setcap.
# do_install below is this rule minus the parts the seat has no use for, and the
# reason each part goes:
#
#   the metainfo, .desktop, hicolor icon and bash-completion entries (Makefile.am
#     :158-:166) are a software centre, a launcher that opens a .desktop file and
#     a shell completion, and weston starts the games from the launchers in
#     /usr/bin instead; the same four are why the recipe next door deletes them.
#   the man page (:164-:165) wants a mandir the seat does not carry.
#   the CJK and TTF fonts (the wqy_1?pt.bdf glob at :152, SarasaGothicFixed.ttf
#     at :154) are the text-mode font fallbacks, 3559172 B, 5456587 B and
#     16677912 B, and --disable-freetype took the TTF path out; the bitmap font
#     at :149 (FREECG98.BMP, 524350 B) stays because it is what the compiled-in
#     VGA rendering uses without any font library.
#   the glshaders (:146, :156) are the OpenGL output mode's shader set, and
#     --disable-opengl means nothing loads them.
#   the .lng translations (:157) are the localised GUI menu strings.
#   the drivez readme (:155) is the Drive API front end's Windows installer text.
#
# What is kept is the binary and the two reference configs plus the bitmap font:
# the reference confs are the small documentation the conf is written against,
# and both are read at run time as examples by the CONFIG and SET commands.
#
# The setcap line (Makefile.am:167, :172) is skipped twice over: it is prefixed
# with - so its failure is ignored, and it is a capability on cap_net_raw for a
# host that has none of this seat's weston session; nothing on the seat reads
# raw devices from this binary.
do_install() {
    install -d ${D}${bindir} ${D}/usr/share/dosbox-x
    install -m 0755 ${B}/src/dosbox-x ${D}${bindir}/dosbox-x
    install -m 0644 ${S}/dosbox-x.reference.conf ${D}/usr/share/dosbox-x/
    install -m 0644 ${S}/dosbox-x.reference.full.conf ${D}/usr/share/dosbox-x/
    install -m 0644 ${S}/contrib/fonts/FREECG98.BMP ${D}/usr/share/dosbox-x/
}

# /usr/share is not in autoconf's default list and the four libraries and the
# data root are not either, the same shape the ioquake3 and opennow recipes
# carry; the .so files here are none of them, this package installs one binary
# and three data files.
FILES:${PN} += "/usr/share/dosbox-x"

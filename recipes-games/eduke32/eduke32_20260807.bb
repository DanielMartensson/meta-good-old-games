SUMMARY = "EDuke32 - the Duke Nukem 3D engine this seat runs, built with the software renderer"
DESCRIPTION = "Duke Nukem 3D's 1996 release is an x86 DOS program (DUKE3D.EXE reading \
DUKE3D.GRP), so nothing of it runs on the seat, and the game comes back through EDuke32, \
the engine that reads the same DUKE3D.GRP and the same .CON scripts. This recipe builds \
the engine only; the episode data is the owner's own copy of 3D Realms' shareware \
release of Episode One, and the README in this directory says where it goes and what \
command opens it. Upstream ships its source as \
one xz tarball off dukeworld, not as a git tree, so this is a pinned tarball fetch rather \
than a git fetch. The make system is EDuke32's own (GNUmakefile + Common.mak), and every \
setting that matters is passed on the make command line rather than edited into the tree. \
What the build asks for is the CLASSIC renderer: USE_OPENGL=0, which Common.mak:807 then \
forces POLYMER=0 and USE_LIBVPX=0 behind, and build.h:1082 videoGetRenderMode() answers \
REND_CLASSIC when USE_OPENGL is undefined. That is the renderer the seat can actually pay \
for: the board carries EGL, GLES 1/2/3 and the Vivante GAL driver and has no desktop GL \
at all, while EDuke32's whole GL path is desktop-only - polymost1Vert.glsl and \
polymost1Frag.glsl are '#version 120', the Polymost2 inline shaders are '#version 110', \
glsurface.cpp:113 and :126 compile their own '#version 110' and '#version 120' sources and \
imgui wants '#version 130'. EDuke32 does have an ES branch, but compat.h:1541 defines \
EDUKE32_GLES only for __ANDROID__ and EDUKE32_IOS and there is no make toggle for it, so \
no make invocation gets an ES renderer out of this tree. RENDERTYPE=SDL stays, which is \
what puts the classic renderer through SDL2's own textures and blitters (build/src/2d.cpp, \
engine.cpp drawrooms()) rather than through the desktop GL one, and that is also what makes \
the seat's picture a plain SDL2 wayland surface like every other engine here. Video output \
and audio both come from SDL2: SDL_OpenAudioDevice with SDL_AUDIODRIVER=alsa reaches the \
seat's ALSA sink, and the .PSM/.XM and .ogg/.ogg-in-grp tracks play through the vendored \
libxmp-lite and minivorbis, which is why no libvorbis is needed here - source/audiolib/src \
/vorbis.cpp:45 includes the vendored minivorbis.h rather than <vorbis.h>. What is NOT \
vendored is FLAC: source/audiolib/src/flac.cpp:44 includes <FLAC/all.h>, and GNUmakefile: \
569 adds -lFLAC -lasound to the link for SUBPLATFORM=LINUX unconditionally, so flac and \
alsa-lib are build dependencies and Common.mak:993's -lrt -latomic are satisfied by the \
glibc and gcc-runtime parts of the sysroot, which carry librt.so, librt.a, libdl.a and \
libatomic.so. This build is for my own use and is not redistributed."
HOMEPAGE = "https://www.duke4.net/"

# source/duke3d/gpl-2.0.txt is the GPL-2 text and every source file under
# source/duke3d and source/audiolib says so at its top: actors.cpp:6-9 reads
# "you can redistribute it and/or modify it under the terms of the GNU General
# Public License version 2 as published by the Free Software Foundation", and
# audiolib/src/flac.cpp:3-10 adds "or (at your option) any later version", so
# the engine as a whole is GPL-2.0-or-later. The rest of the tree is stated
# here rather than claimed by LICENSE: mimalloc (v2 MIT, its own LICENSE file
# checked below) and libxmp-lite (its own README) are vendored inside the
# tarball and link into this binary, imgui and glad (both MIT) are vendored
# too and only matter to the GL path this recipe does not use, and source/
# build/buildlic.txt and source/kenbuild/buildlic.txt are Ken Silverman's
# BUILDLIC.TXT for the Build tool, which the engine does not use.
LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://source/duke3d/gpl-2.0.txt;md5=b234ee4d69f5fce4486a80fdaf4a4263 \
                    file://source/mimalloc/LICENSE;md5=cd47cf280095d74b469beed1f28c8e77 \
                    file://source/libxmp-lite/README;md5=a6b99447a8e54affe90ca781272af78d"

# Pin. dukeworld.com/eduke32/synthesis/latest/ carries one source tarball at the
# time this recipe was written, eduke32_src_20260807-10669-ec5824db8.tar.xz:
# 19,085,516 bytes (curl -I answers that content-length) and the sha256 below,
# and the tree inside it is stamped r10669-ec5824db8. Pinned by hash rather
# than taken from "latest", because the tarball address moves on every
# synthesis build and the make settings and the -j/--game_dir and --nosetup
# option names this recipe passes are read out of that exact tree; a moved
# "latest" would otherwise be a silently different engine.
SRC_URI = "https://dukeworld.com/eduke32/synthesis/latest/eduke32_src_20260807-10669-ec5824db8.tar.xz;sha256sum=f292e2c69f7f3453dd145d19ce1c114bd7136a8a9cf5b9b6641525fd53fa56c5"

# The tarball carries the build date 20260807 and the revision 10669-ec5824db8
# in its own name and stamps the same into EDUKE32_REVISION.mak, so the deb
# version is that date; no git suffix, since this is a tarball fetch.
PV = "20260807"

# PKGR pinned like the rest of this layer, so the deb version stays
# comparable across rebuilds.
PKGR = "r0.01"

# The tarball's single top directory, which is what unpacking writes under
# ${WORKDIR}. Named here instead of left to OE's PACKAGE_NAME-PACKAGE_VERSION
# guess, because that guess does not know EDuke32's own naming.
S = "${WORKDIR}/eduke32_20260807-10669-ec5824db8"

# Three real shared libraries, all of which the seat already carries
# (libSDL2-2.0.so.0, libFLAC.so.12 and libasound.so.2 in /usr/lib), and
# nothing else has to go into the image for this engine: -lrt and -latomic
# at Common.mak:993 come from glibc's librt.so and gcc-runtime's libatomic.
# so, -ldl comes from glibc's libdl.a, and -lm and -pthread come from libc.
DEPENDS = "libsdl2 flac alsa-lib"
RDEPENDS:${PN} = "libsdl2 flac alsa-lib"

# SDLCONFIG is left empty on the make command line, which takes Common.mak:955's
# own fallback: -l$(SDLNAME) with SDLNAME=SDL2 from SDL_TARGET=2 at Common.mak:338
# plus -D_REENTRANT -DSDL_USEFOLDER. There is no sdl2-config in this sysroot (the
# libsdl2 recipes ship the library and its headers, not the vendor's config script)
# and the makefile's own PKG_CONFIG at Common.mak:202 is the host's, usable only
# for the gtk+-2.0 probe this build turns off with HAVE_GTK2=0, so a stub would
# have to be planted here instead - fteqw in this layer does exactly that. It is
# not planted: the SDL_TARGET=2 branch of sdl_inc.h:15 needs SDL_USEFOLDER to be
# defined to take the <SDL2/SDL.h> include at all, and that define lives only in
# the SDLCONFIG-empty branch above, so the fallback is kept and only the one
# missing include directory is added to it.
#
# That directory is what the vendor's own sdl2-config --cflags answer is, and
# EDuke32 needs it because its vendored SDL2 shim spells the header without the
# folder: source/build/include/sdl_inc.h:16 has <SDL2/SDL.h>, but source/imgui/
# src/imgui_impl_sdl2.cpp:117 has plain <SDL.h>, and this sysroot keeps SDL2's
# headers one directory deeper (recipe-sysroot/usr/include/SDL2/SDL.h), so
# imgui is the object that stops the build without it:
#     source/imgui/src/imgui_impl_sdl2.cpp:117:10: fatal error: SDL.h: No such file or directory
# imgui is compiled even with the software renderer: GNUmakefile:160 excludes
# imgui_impl_sdl2.cpp only when RENDERTYPE is not SDL, and RENDERTYPE=SDL is
# what the classic renderer needs, so the vendored copy is built and linked and
# simply never reached by a REND_CLASSIC frame.
do_compile() {
    make -C "${S}" eduke32 \
        ARCH=arm64 \
        PLATFORM=LINUX SUBPLATFORM=LINUX \
        CC="${CC}" CXX="${CXX}" AR="${AR}" RANLIB="${RANLIB}" STRIP="${STRIP}" \
        CFLAGS="${CFLAGS} -I${WORKDIR}/recipe-sysroot/usr/include/SDL2" \
        CXXFLAGS="${CXXFLAGS} -I${WORKDIR}/recipe-sysroot/usr/include/SDL2" \
        SDL_TARGET=2 SDLCONFIG= \
        PACKAGE_REPOSITORY=1 \
        RELEASE=1 \
        USE_OPENGL=0 POLYMER=0 USE_LIBVPX=0 \
        HAVE_GTK2=0 STARTUP_WINDOW=0 \
        NETCODE=1 USE_MIMALLOC=1 \
        VC_REV=10669 VC_HASH=ec5824db8
}

# The makefile links the game into the top of the tree as its bare target name,
# so ${S}/eduke32 is the binary. mapster32, the level editor, is NOT built here:
# `all: duke3d` at GNUmakefile:754 would link it as well, and the seat runs the
# game, not an editor, so the make target is the engine's own name from the
# BUILDRULE at GNUmakefile:783 instead of the umbrella target.
do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/eduke32 ${D}${bindir}/eduke32
}

# The makefile strips its own release binary: RELEASE=1 leaves STRIP set
# (Common.mak:447 clears it only for RELEASE=0) and the BUILDRULE at
# GNUmakefile:803 runs $(STRIP) over the linked executable, which is why
# STRIP="${STRIP}" is handed over above - left at Common.mak:203's default
# it would be the host's `strip`, which cannot read an aarch64 object and
# the unstripped binary would then ship with the engine's own debug info.
# By the time do_install sees the file there is nothing left for OE to strip,
# so this QA hit is the expected outcome of that build, the same as the other
# pre-stripped recipes in this layer (opennow, opennowlite, fteqw).
INSANE_SKIP:${PN} += "already-stripped"

SUMMARY = "OpenJazz - the Jazz Jackrabbit 1 engine this seat runs Epic's shareware data on"
DESCRIPTION = "Jazz Jackrabbit 1 is a 1994 Epic MegaGames platformer whose DOS binary \
(x86 JAZZ.EXE, driven by DPMI.000) cannot run on the seat at all, so the game comes back \
through OpenJazz, a from-scratch reimplementation of the same engine that reads the \
original data files rather than a converted format. This recipe builds the engine only: \
the episode data is the owner's own copy of Epic's Holiday Hare '94 shareware episode, \
installed by hand, and this recipe carries none - the README in this directory says \
where it goes and what command opens it, and carries in full why no recipe here fetches \
it: Epic's own LICENSE.DOC does grant distribution of that episode, but the only copy on \
hand comes off an Internet Archive community item, which is not an authorized download \
source. Nothing here carries any \
content. The engine is one C++ \
program, cmake -B build -S . in the upstream tree, linking SDL2 and the three vendored \
libraries under ext/: argparse (command line), miniz (the archive reader) and stb \
(texture atlases), plus ext/xmp, which is libxmp compiled into the binary so the \
.PSM/.MUS modules the game ships play without a soundfont or an external mixer. Video \
is SDL2's own renderer: src/io/gfx/video.cpp:379 SDL_CreateRenderer(window, nullptr) \
builds the window's renderer and src/io/gfx/video.cpp:485 SDL_RenderSetLogicalSize \
gives it the game's 320x200 canvas, so the upscale to the seat's 1920x1080 is SDL2's \
work and the engine needs no GL context of its own. Audio is SDL2's audio device \
(src/io/sound.cpp:243 SDL_OpenAudioDevice for the SDL2 build) fed by libxmp \
(src/io/sound.cpp:353 xmp_load_module_from_callbacks), and the seat's 48 kHz ALSA pin \
is what SDL2 opens when SDL_AUDIODRIVER says alsa. Configuration and saves are XDG \
files under ~/.config/openjazz and ~/.cache/openjazz (src/platforms/xdg.cpp:32-36), \
which weston can write, and the game data path is compiled in through \
-DDATAPATH (src/main.cpp:218-219 adds it as a SYSTEM|GAME path), so the engine takes \
the owner's own data directory as a plain argument and no launcher is needed at all. This build is for my own use and is not \
redistributed."
HOMEPAGE = "https://github.com/AlisterT/openjazz"
BUGTRACKER = "https://github.com/AlisterT/openjazz/issues"

# COPYING is the GPL-2 text and the sources' own headers say "version 2 of the
# License, or (at your option) any later version" (src/main.cpp:10-12), so the
# licence is v2-or-later. doc/licenses.txt repeats it for the whole tree and
# spells out that the vendored ext/xmp is libxmp - "See MIT license at the end of
# file" (doc/licenses.txt:libxmp row), and ext/xmp/LICENSE is that MIT text
# ("Permission is hereby granted, free of charge ... to deal in the Software
# without restriction"), which is compatible with linking it into this GPL-2
# binary. The SDL row of the same file is the other vendored license: "Version 1.2
# of this library is distributed under the terms of the GNU LGPL license",
# "Version 2 and 3 use the zlib license", and this build is the SDL2 one, so the
# vendored half of this binary is zlib plus ext/scale2x, whose own row in the
# same file repeats the GPL-2 boilerplate and matches the engine's license.
LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://COPYING;md5=b234ee4d69f5fce4486a80fdaf4a4263"

# Pin: the tip of master when this recipe was written, a1626f4e "Merge pull
# request #157 from carstene1ns/feat/episode-select" (1 Sep 2026), the tree that
# CMakeLists.txt:4 stamps as project version 20260301. The upstream repository's
# default branch is master, not main - `git ls-remote --heads` lists master,
# feature/config, feature/screenshots, feature/zipfiles and old-dev, and asking
# for branch=main is what makes do_fetch answer "Unable to find revision ...
# in branch main even from upstream". Pinned rather than left at HEAD like
# every other recipe in this layer: the option names this build passes
# (SDL_VERSION, NETWORK, SCALE, DATAPATH) are read out of that CMakeLists.txt,
# and src/io/file.cpp:700-760 is what decides whether a data path is accepted
# at all, so a tree that moves either has to be re-checked against the file
# rather than configured silently differently.
SRC_URI = "git://github.com/AlisterT/openjazz.git;protocol=https;branch=master"
SRCREV = "a1626f4edd4a7af72c54103021790c56d8ecfced"

# CMakeLists.txt:4 is `project(OpenJazz VERSION 20260301 ...)`, and the git
# suffix is the recipe's own, as in the ioquake3 recipe.
PV = "20260301+git${SRCPV}"

# PKGR pinned like the rest of this layer, so the deb version stays comparable
# across rebuilds.
PKGR = "r0.01"

S = "${WORKDIR}/git"

inherit cmake pkgconfig

# SDL2 only: the tree also speaks SDL3 and SDL1.2 (CMakeLists.txt:163-200), and
# the seat already carries SDL2 for every other game engine in this layer, so
# asking for the third one would add a library the board does not otherwise
# need. NETWORK=OFF is the honest setting for a single-seat machine with no
# SDL_net in the image: the option only gates the sockets code path, and the
# build says "Network: Disabled" rather than carrying a netplay feature that
# has nothing to talk to. SCALE=ON is the tree's default and costs one
# vendored subdirectory (ext/scale2x) for the software upscalers.
DEPENDS = "libsdl2"

# DATAPATH is compiled in (src/main.cpp:218-219) as the last game-data path the
# engine will try, which is what lets a hand-installed tree stay one argument:
# the owner's episode sits under a directory of their own choice and is passed
# as a positional argument, which the engine adds in FRONT of this compiled-in
# path, so the compiled-in default is the fallback rather than the one place a
# recipe has to have written data into.
EXTRA_OECMAKE = " \
    -DSDL_VERSION=2 \
    -DNETWORK=OFF \
    -DSCALE=ON \
    -DDATAPATH=${datadir}/openjazz \
"

# SDL2's runtime library, since OpenJazz links it: the engine recipe installs
# only /usr/bin/OpenJazz and its icon/desktop files, and no payload recipe
# depends on this one any more, and the owner's own data directory is what
# gets the engine running, so libsdl2 has to be in the image beside it.
RDEPENDS:${PN} = "libsdl2"

# CMakeLists.txt:461-464 installs the engine's own icon set as well as the
# desktop entry, and OE's default FILES covers ${datadir}/applications and
# ${datadir}/pixmaps but not ${datadir}/icons, which is what makes do_package
# stop with "Files/directories were installed but not shipped". The icons are
# two small files and they belong to the engine package rather than to a
# separate one.
FILES:${PN} += "${datadir}/icons/hicolor"

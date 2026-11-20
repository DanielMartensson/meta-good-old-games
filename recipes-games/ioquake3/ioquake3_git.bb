SUMMARY = "ioquake3 1.36 - the Quake III engine, built as one opengl2/GL ES client for the seat"
DESCRIPTION = "ioquake3 is the id Tech 3 source that the Quake III Arena engine was rebuilt \
from: one client that speaks the Quake III protocol, loads the game out of pk3 archives and \
draws it with a renderer it picks at runtime. This recipe builds exactly one renderer, the \
opengl2 one, and links it straight into the client instead of shipping it as a dlopen library, \
so the board ends up with one binary and no renderer search path to get wrong. The reason that \
renderer is the one worth having here is the seat's driver: the Vivante GAL on STM32MP257F answers \
EGL with OpenGL_ES only and has no desktop GL at all, and the opengl2 renderer is the only one of \
ioquake3's two that has an OpenGL ES path - upstream's own README.md:201 says the opengl1 renderer \
does not have one at all - and code/sdl/sdl_glimp.c:325 only takes the ES entry points when \
QGLES_VERSION_ATLEAST( 2, 0 ) holds, while code/renderergl2/tr_glsl.c:256 hands the driver a \
shader source whose first line is the `#version 300 es` directive when the driver reports GLES 3. \
The opengl1 renderer would ask this board for a desktop context and \
never get one, so it is switched off and only the GL2 renderer gets compiled. The only thing this recipe \
installs beside the client is this build's own version-6 vm modules, packed in a \
vm-pure.pk3 under /usr/share/ioquake3/baseq3, which is code and not content, so the \
Quake III data this engine reads is the owner's own tree, named by the launcher's \
fs_basepath/fs_game defaults or by a +set on the command line; the README in this \
directory says where that tree goes and what command opens it."
HOMEPAGE = "https://github.com/ioquake/ioq3"
BUGTRACKER = "https://github.com/ioquake/ioq3/issues"

# COPYING.txt is GNU v2, the same text the sources' own headers carry -- code/
# renderergl2/tr_glsl.c:9 says "version 2 of the License, or (at your option) any
# later version", so the licence is v2-or-later and not v2-only. The md5 is this
# file's own and not GNU's: the vendored copy is the old formatted layout, which
# hashes to 87113aa2b484c59a17085b5c3f900ebf rather than the 94d55d512a9ba36caa9b7df079bae19f
# of the current plain-text one.
LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://COPYING.txt;md5=87113aa2b484c59a17085b5c3f900ebf"

# Pin: main's tip when this recipe was written, 52088b9e "General tidy-up to
# Huff_Decompress (fixes #894)" (4 Nov 2026). Pinned rather than left at HEAD,
# like every other recipe in this layer, because the option names this build
# passes are read out of this tree's cmake/ directory and are checked again
# below in do_configure:prepend: BUILD_RENDERER_GL1 and friends are spelled in
# cmake/CMakeLists.txt, and a tree that renames them has to be re-checked
# against that file before it is built, not silently configured differently.
# The branch is main, which is what the repository calls its default branch
# today; branch=master is the name this tree used to answer with and the fetch
# now stops with "Unable to find revision ... in branch master even from
# upstream" rather than fetching the same commit through a name it dropped.
# Both local patches below sit against this exact tree the way the fteqw
# recipe's three do: 0001 inserts the build-machine settings after the BUILD_TYPE_ARG
# block at cmake/utils/qvm_tools.cmake:11-:13 and adds one line to that
# ExternalProject's CMAKE_ARGS list at :20, so a tree that moved either
# anchor has to be re-checked against the file before the patch is applied,
# not applied at an offset that silently stops clearing the settings. 0002 inserts one
# extra condition into the nine-pak demand at code/qcommon/files.c:3697 inside FS_CheckPak0,
# and the demand it guards is why the seat cannot boot id's shareware demo with the
# engine as shipped: the whole demo payload is one demoq3/pak0.pk3 with 1384 files, and
# measured twice the process dies right after mounting it, console ending "1384 files
# in pk3 files" then "Quake 3 data files are missing. Please copy pak0.pk3 through
# pak8.pk3 from the baseq3 directory in your Quake 3 install or CD-ROM to: ..." plus
# "ERROR: couldn't open crashlog.txt", with no live process ten seconds later and the
# weston shots the same desktop every time. Nothing the launcher passes can dodge it:
# com_basegame answers baseq3 in this Q3 build, so with any pak mounted the standalone
# branch at :3652 is not taken and com_standalone gets forced to 0 at :3657 (a
# +set com_standalone 1 on the command line is overwritten there), and the demoq3
# rescue just above only ORs bit 0 into the foundPak mask behind a hard-coded
# DEMO_PAK0_CHECKSUM (its own warning says "may work fine, but it is not guaranteed
# or supported") while the fatal still wants all nine bits. So vanilla ioquake3
# cannot boot the demo at all, which is why FTEQW grew its own q3demo protocol and
# why this engine replaced that FTE route, measured to render pure black on this
# Vivante. The patch leaves a full game untouched (fs_game empty or baseq3 answers
# the same check as before) and skips the demand exactly when fs_game names the
# owner's demoq3 tree, after which the engine boots, mounts that pak and loads
# this build's own version-6 vm modules out of the vm-pure.pk3 installed in baseq3.
SRC_URI = "git://github.com/ioquake/ioq3.git;protocol=https;branch=main \
           file://0001-build-the-qvm-tools-on-the-build-machine-not-on-the-target.patch \
           file://0002-let-the-demoq3-shareware-payload-skip-the-purchased-game-pak-mask-check.patch \
          "
SRCREV = "52088b9eae247d8e321c8dc1fb49a879652854f9"

# ioquake3's own version is cmake/identity.cmake:2 (PROJECT_VERSION 1.36), and
# the deb version is that plus the pinned PKGR below, like every other recipe
# here, so a rebuild does not reset the build counter and move the deb version.
PV = "1.36+git${SRCPV}"

# PKGR: pinned, like every other recipe in this layer, so the deb version
# stays comparable across rebuilds. r0.04 re-homes this build's vm modules: they
# left the payload recipe's demoq3 tree, which is gone from this layer, and came
# into this package's own /usr/share/ioquake3/baseq3, with the launcher's default
# fs_basepath/fs_game moved to that pair beside them; r0.03 packs this build's vm
# modules into
# vm-pure.pk3 instead of leaving them loose, which is what makes the in-game
# ui load at all (see the FS_FindVM and FS_PureServerSetLoadedPaks facts above
# the pk3 block); r0.02 is the round that adds 0002 and, with it,
# a do_install whose launcher the seat can actually run: the r0.01 deb on the seat
# carries a launcher whose comment block arrived at the deb broken across bare word
# lines (its /usr/bin/ioquake3 logs "line 15: r_preferOpenGLES: not found" before any
# engine output at all), while this file's printf list has every line quoted and
# comment-prefixed, so a rebuild from here installs the launcher that was always
# meant to ship; the bump installs over the r0.01 deb the seats carry.
PKGR = "r0.04"

S = "${WORKDIR}/git"

inherit cmake

# The one external library this build needs. Everything else ioquake3 would
# drag in is either switched off or carried by code/thirdparty inside the
# checkout and compiled into the binary by cmake/libraries/*.cmake: USE_INTERNAL
# _LIBS is on, so zlib-1.3.1, jpeg-9f, libogg-1.3.6 and libvorbis-1.3.7 come
# from the vendored copies and the pkg_check_modules(OGG/VORBIS/JPEG) branches
# are never taken, which is what leaves libpng and libjpeg-turbo out of DEPENDS.
# SDL is the exception: cmake/libraries/sdl.cmake:34 only has a vendored SDL for
# WIN32 and APPLE, and on Linux falls through to find_package(SDL2 REQUIRED) at
# :56, which is satisfied by the sysroot's libsdl2-dev. The seat already carries
# libSDL2.so for the FTEQW client, so this engine adds no library to the image.
DEPENDS = "libsdl2"

# What this build is and is not. Every option below is spelled out rather than
# left at upstream's default, and the reasons are the seat's, not upstream's:
#
# BUILD_RENDERER_GL1 OFF is the GLES decision above: the opengl1 renderer is
# desktop-only, so it would not run here at all, and with it off only the GL2
# renderer's sources are compiled (cmake/client.cmake:89 says of the two
# renderer source lists "These are never simultaneously populated").
#
# USE_RENDERER_DLOPEN OFF is what makes that single-binary build work: with
# dlopen on, cmake/renderer_gl2.cmake:78 produces renderer_opengl2 as a shared
# library and the client has to find and dlopen it by name at startup, which
# needs a directory layout to install it into. With it off, cmake/client.cmake
# folds the renderer sources into the client target and the binary is
# self-contained. upstream's own README.md:208's `+set cl_renderer opengl2` is then not needed
# and left out of the launcher: there is no second renderer to choose between.
#
# BUILD_SERVER OFF leaves the ioq3ded dedicated server out: the seat plays
# against the local server the client runs by itself, which is the `+map` path
# in cl_main.c, and a second process on this 1768 MB board is one more thing to
# keep in memory for nothing.
#
# BUILD_GAME_LIBRARIES OFF keeps the shared-library game modules out: the seat
# loads its gamecode as QVM bytecode, not as .so files, and cmake/basegame.cmake
# :130-:156 is the block that would put cgame/qagame/ui .so files in the image
# next to the client, which is three libraries the seat never opens.
#
# BUILD_GAME_QVMS ON is the payload's reason and it is the one thing here that
# is not a cut. The demo's own gamecode is version-3 bytecode (the seat's own
# log said "Architecture doesn't have a bytecode compiler, using interpreter"
# and then "ERROR: User Interface is version 3, expected 6"), which this engine
# refuses: the version constant is in the module, and cmake/identity.cmake and
# code/qcommon/vm.cpp expect the modules built from this tree. So the payload
# needs the engine's own vm/{cgame,qagame,ui}.qvm, which is exactly what
# code/web/client-config.json:29-:35 prescribes for demoq3 ("Copy baseq3/vm/*.qvm
# to demoq3/vm/ as the Quake 3 demo QVMs are not compatible"). cmake/basegame.cmake
# :158-:179 is the add_qvm block that produces them, into ${BASEGAME}/vm.
# The tools they need (q3rcc/q3cpp/q3lcc/q3asm/q3lburg, cmake/tools/CMakeLists.txt
# :67-:93) are build-time programs, and cmake/utils/qvm_tools.cmake:20 is what
# builds them: its ExternalProject CMAKE_ARGS list carries only SOURCE_DIR,
# CMAKE_MODULE_PATH and CMAKE_MINIMUM_REQUIRED_VERSION, so they are configured
# with the machine's own compiler rather than the cross one, which is also why
# :15-:18 names them ${HOST_EXECUTABLE_SUFFIX}. The bytecode they emit is the
# Q3VM ISA and not machine code, so the modules the seat loads are the same
# bytes the build host compiled, and the QVM interpreter in cmake/shared_sources.cmake
# (code/qcommon/vm_interpreted.c) runs them wherever the seat is.
#
# USE_OPENAL OFF picks the SDL audio driver over OpenAL and is the audio
# decision for this engine. code/client/snd_main.c:504 tries S_AL_Init only when
# s_useOpenAL is on, and falls through to the base driver otherwise, and that
# base driver is code/sdl/sdl_snd.c:246's SDL_OpenAudioDevice(NULL, ...), which
# is the path the seat already has working: it is an ALSA device behind the
# pcm.!default stanza meta-watermelon-wine's alsa-state-stm32mp2.bbappend adds,
# and it is the same driver the Chocolate Doom games play their sounds through.
# OpenAL would be one more audio library in the image and one more layer between
# the game and that stanza, and it buys nothing the SDL driver does not already
# do here. USE_OPENAL_DLOPEN is switched off to match, so cmake/libraries/openal.cmake
# never reaches its find_package(Threads REQUIRED).
#
# USE_HTTP, USE_VOIP and USE_MUMBLE OFF are the same kind of cut: the first
# wants curl (USE_HTTP is what cmake/libraries/curl.cmake arranges link lines
# for, and the seat's engine downloads nothing since the payload is a recipe
# fetch rather than a mirror), the other two want opus/opusfile and libmumble
# for voice chat nobody uses on a board that plays one local arena. USE_CODEC_OPUS
# off goes with them: the demo's sounds are wav and its music is ogg, which
# USE_CODEC_VORBIS on keeps, through the vendored libogg/libvorbis.
#
# USE_INTERNAL_SDL OFF is the only internal library switched off, and it has to
# be: cmake/libraries/sdl.cmake:34 pairs it with HAVE_INTERNAL_SDL, which the
# same file only sets on WIN32 and APPLE, so on this target the flag does nothing
# either way -- stating it is what keeps the option list honest about which
# vendored copies are actually used.
BUILD_SERVER = "-DBUILD_SERVER=OFF"
BUILD_RENDERER_GL1 = "-DBUILD_RENDERER_GL1=OFF"
BUILD_RENDERER_GL2 = "-DBUILD_RENDERER_GL2=ON"
BUILD_GAME_LIBRARIES = "-DBUILD_GAME_LIBRARIES=OFF"
BUILD_GAME_QVMS = "-DBUILD_GAME_QVMS=ON"
BUILD_STANDALONE = "-DBUILD_STANDALONE=OFF"
USE_RENDERER_DLOPEN = "-DUSE_RENDERER_DLOPEN=OFF"
USE_OPENAL = "-DUSE_OPENAL=OFF"
USE_OPENAL_DLOPEN = "-DUSE_OPENAL_DLOPEN=OFF"
USE_HTTP = "-DUSE_HTTP=OFF"
USE_CODEC_VORBIS = "-DUSE_CODEC_VORBIS=ON"
USE_CODEC_OPUS = "-DUSE_CODEC_OPUS=OFF"
USE_VOIP = "-DUSE_VOIP=OFF"
USE_MUMBLE = "-DUSE_MUMBLE=OFF"
USE_FREETYPE = "-DUSE_FREETYPE=OFF"
USE_INTERNAL_LIBS = "-DUSE_INTERNAL_LIBS=ON"
USE_INTERNAL_SDL = "-DUSE_INTERNAL_SDL=OFF"

# LTO is ioquake3's own default, stated here rather than left to the tree: the
# root CMakeLists.txt asks check_ipo_supported and turns CMAKE_INTERPROCEDURAL_OPTIMIZATION
# on when the toolchain answers yes, and the cross gcc and binutils here do. It
# is kept on because it is the game's frame time it moves, not the build's: it
# inlines the renderer's hot calls (tr_shade.c and tr_surface.c) across
# translation units on a seat that is already fill-rate bound, so an engine
# compiled without it is spending its budget on call overhead before the GPU
# ever sees a draw call.
INTERPROCEDURAL_OPTIMIZATION = "-DCMAKE_INTERPROCEDURAL_OPTIMIZATION=TRUE"

EXTRA_OECMAKE = "\
    ${BUILD_SERVER} \
    ${BUILD_RENDERER_GL1} \
    ${BUILD_RENDERER_GL2} \
    ${BUILD_GAME_LIBRARIES} \
    ${BUILD_GAME_QVMS} \
    ${BUILD_STANDALONE} \
    ${USE_RENDERER_DLOPEN} \
    ${USE_OPENAL} \
    ${USE_OPENAL_DLOPEN} \
    ${USE_HTTP} \
    ${USE_CODEC_VORBIS} \
    ${USE_CODEC_OPUS} \
    ${USE_VOIP} \
    ${USE_MUMBLE} \
    ${USE_FREETYPE} \
    ${USE_INTERNAL_LIBS} \
    ${USE_INTERNAL_SDL} \
    ${INTERPROCEDURAL_OPTIMIZATION} \
"

do_configure:prepend() {
    # The option names this build passes live in this tree's own cmake file, so
    # a tree that renames them would otherwise be configured with upstream's
    # defaults instead, and the binary that installs would not be the one this
    # recipe says it is: opengl1 would be built and dlopen would be on.
    for opt in BUILD_SERVER BUILD_RENDERER_GL1 BUILD_RENDERER_GL2 BUILD_GAME_LIBRARIES \
        BUILD_GAME_QVMS BUILD_STANDALONE USE_RENDERER_DLOPEN USE_OPENAL USE_OPENAL_DLOPEN \
        USE_HTTP USE_CODEC_VORBIS USE_CODEC_OPUS USE_VOIP USE_MUMBLE USE_FREETYPE \
        USE_INTERNAL_LIBS USE_INTERNAL_SDL; do
        if ! grep -q "option($opt" "${S}/CMakeLists.txt"; then
            bbfatal "ioquake3: ${S}/CMakeLists.txt has no option($opt ...) line; the -D${opt}= line in this recipe does not apply to this pin."
        fi
    done
}

# The engine's own version-6 game modules are code, not content, so they belong
# to this package and go into this package's own tree at /usr/share/ioquake3/baseq3.
# ioquake3 always loads <fs_basepath>/baseq3 before it loads the fs_game directory,
# so one pk3 there is findable whichever Quake III tree the owner names - baseq3,
# demoq3 or a mod like missionpack - which is what code/web/client-config.json:33-:35
# is asking for when it says to copy baseq3/vm/*.qvm into the game's vm directory.
# Nothing has to be claimed here: the base class's default FILES:${PN} already
# covers /usr/share/${PN}, which is exactly where do_install writes this file.

do_install() {
    install -d ${D}${bindir}

    # The client comes out of the cmake build directory under the name cmake/
    # identity.cmake:4 gives it (ioquake3), and it is the only executable this
    # build produces, since the dedicated server and the shared-library game
    # modules are switched off above; the vm/*.qvm handled below are not
    # executables but bytecode this same build compiles. It is not at the top of
    # the build directory: cmake/client.cmake:87 hands the target to cmake/utils/set_output_dirs.cmake,
    # whose :10 line puts every output under ${B}/${CMAKE_BUILD_TYPE}, and
    # CMakeLists.txt:42 is what makes that word Release -- OE's cmake class passes
    # an empty -DCMAKE_BUILD_TYPE=, so the tree's own `if(NOT CMAKE_BUILD_TYPE)`
    # branch sets it. The path is spelled out rather than searched for so a build
    # that stopped producing the client fails here, with this file named, instead
    # of installing nothing and leaving the launcher behind.
    install -m 0755 "${B}/Release/ioquake3" ${D}${bindir}/ioquake3-gl2

    # The version-6 game modules go into this package's own baseq3 tree rather
    # than beside the client: add_qvm's OUTPUT_DIRECTORY at cmake/basegame.cmake:166
    # is ${BASEGAME}/vm, so this build leaves them at Release/baseq3/vm/{cgame,
    # qagame,ui}.qvm, and they install at /usr/share/ioquake3/baseq3. That is the
    # one tree ioquake3 loads before whatever fs_game the owner names - baseq3,
    # demoq3 or a mod like missionpack - so one vm-pure.pk3 there serves every
    # Quake III tree on the seat, which is what code/web/client-config.json:33-:35
    # asks for when it says to copy baseq3/vm/*.qvm into a game's vm directory.
    # What they must NOT be is loose files in a game directory, which is a
    # measured fact about this engine rather than a packaging choice: with the
    # three of them in the payload's demoq3/vm as
    # plain files the console gets the menu through ("File \"vm/ui.qvm\" found
    # at \".../quake3-demo/demoq3\"", "ui loaded in 2820288 bytes on the
    # hunk", "4 arenas parsed", "6 bots parsed") and dies at the in-game one with
    # "VM_Create on UI failed" after the map has already loaded ("loaded maps/
    # q3dm7.aas", "found 67 level items", "AAS initialized"), which is what the
    # proof run measured twice: the process is gone, the weston shots are the
    # 84 KB desktop every time. code/client/cl_ui.c:1119 is where that call
    # happens and :1121 where the fatal is raised off a NULL; vm.c:615 shows the
    # only NULL path left here is retval < 0 out of code/qcommon/files.c:1398's
    # FS_FindVM, and FS_FindVM:1419 searches a loose game directory only while
    # !fs_numServerPaks, which the local server sets at files.c:4011 (fs_num-
    # ServerPaks = c inside FS_PureServerSetLoadedPaks) the moment the demo's
    # pak0 is mounted. So the first CL_InitUI, before the server has spoken,
    # finds the loose file, and the second one, the in-game ui this layer needs,
    # finds nothing at all. A pk3 keeps them findable in both passes, and the
    # pk3 run's console says so at each of the three: "File \"vm/ui.qvm\" found
    # in \"...demoq3/vm-pure.pk3\"" then "ui loaded in 2820288 bytes", "File
    # \"vm/qagame.qvm\" found in ..." then "qagame loaded in 5108352 bytes", and
    # "File \"vm/cgame.qvm\" found in ..." then "cgame loaded in 4183168 bytes"
    # followed by "CL_InitCGame: 5.07 seconds" with the process still running.
    # The three members come from this build's own cmake output, so a build that
    # stopped producing one fails here with the name that is missing rather than
    # packing a pk3 the engine then cannot boot. The writer is python3's zipfile
    # rather than a zip command because do_install runs inside this recipe's own
    # sysroot, not on the dev machine: the first round of this change probed for
    # /usr/bin/zip and bbfatal fired, which said the dev machine's zip is not on
    # this PATH at all, while the same log's extend_recipe_sysroot line already
    # names python3-native as one of the packages this recipe's sysroot carries.
    # ZIP_DEFLATED is what the seat's proof pk3 was written with, so the shipped
    # file is the same kind the console answer above was measured against.
    install -d ${D}/usr/share/ioquake3/baseq3
    for qvm in cgame qagame ui; do
        if [ ! -f "${B}/Release/baseq3/vm/${qvm}.qvm" ]; then
            bbfatal "ioquake3: cmake produced no Release/baseq3/vm/${qvm}.qvm for this pin"
        fi
    done
    python3 -c "import os,sys,zipfile; z = zipfile.ZipFile(sys.argv[1], 'w', zipfile.ZIP_DEFLATED); [ z.write(os.path.join(sys.argv[2], q + '.qvm'), 'vm/' + q + '.qvm') for q in ('cgame', 'qagame', 'ui') ]; z.close()" \
        ${D}/usr/share/ioquake3/baseq3/vm-pure.pk3 "${B}/Release/baseq3/vm"
    chmod 0644 ${D}/usr/share/ioquake3/baseq3/vm-pure.pk3

    # The launcher. ioquake3 never finds its game next to the executable, so
    # the payload this layer installs lives under /usr/share and the engine is
    # told where it is: fs_basepath is the directory holding the game
    # directory, fs_game names that directory, and fs_homepath is where the
    # engine writes its config, its logs and its screenshots, which has to be
    # writable and therefore cannot be the /usr/share tree. The default names
    # this recipe's own /usr/share/ioquake3/baseq3, the tree this package installs
    # its vm-pure.pk3 into, so the engine boots on its own and the owner's Quake
    # III data goes in beside it: their pak0.pk3 at /usr/share/ioquake3/baseq3
    # plays a full game with nothing else stated, and their demo or their Team
    # Arena tree is one +set fs_game away, which the command line can state since
    # these defaults are only defaults.
    #
    # The two SDL_* exports are the seat's own window and sound system: this
    # board has no X server, only weston, so SDL_VIDEODRIVER=wayland is the
    # video driver SDL2 has to be pointed at (left unset SDL2 looks for X11 and
    # fails before a window exists), and SDL_AUDIODRIVER=alsa is the ALSA sink
    # behind the seat's pcm.!default stanza. Both are overridable from the
    # environment so a hand-run test can point them elsewhere.
    #
    # r_preferOpenGLES 1 is upstream's own README.md:208's line and the one driver fact here:
    # code/sdl/sdl_glimp.c:1086 gives r_preferOpenGLES a default of -1, and at
    # :543 that default makes the renderer try the desktop API before the ES one,
    # while this board answers EGL with OpenGL_ES only. Stating 1 asks for the
    # API the seat has instead of leaving the choice to a probe.
    #
    # r_fullscreen 1 with r_mode -1 and r_customwidth 1024 r_customheight 576 is
    # the seat's fill-rate answer, in the shape ioquake3 spells it rather than the
    # vid_* names FTEQW uses: code/renderergl2/tr_init.c:1270 is where r_customwidth
    # and r_customheight are read, and r_mode -1 is what tells GLimp_SetMode to use
    # that pair instead of one of its own mode list. The desktop stays the seat's
    # 1920x1080 and only the 3d scene is drawn into the smaller buffer the
    # compositor scales up, and on this Vivante GCNANO ULTRA31 that buffer area
    # is what decides the framerate, which is why the same shape is what the FTEQW
    # launchers in this layer ask for too.
    printf '%s\n' \
        '#!/bin/sh' \
        '# Launcher for the ioquake3 opengl2 client this layer builds, on the' \
        '# Quake III payload it is paired with. The payload is not beside the' \
        '# binary: fs_basepath is the tree holding the game directory, fs_game' \
        '# names that directory and fs_homepath is where the engine writes its' \
        '# config, its console log and its screenshots. Override with IOQUAKE3_BASEPATH' \
        '# / IOQUAKE3_GAME / IOQUAKE3_HOME / IOQUAKE3_ARGS, or pass your own' \
        '# +set and +commands on the command line, which win.' \
        '# The SDL_* lines are the seat: no X server, only weston, so wayland is' \
        '# the video driver SDL2 needs, and alsa is the ALSA sink the seat opens' \
        '# through its asound.state stanza. The r_preferOpenGLES set is the driver' \
        '# answers EGL with OpenGL ES 3.1 only and has no desktop GL, and that cvar is' \
        '# what tells the renderer to try the ES API first. The video sets draw the 3d' \
        '# scene into a 1024x576 buffer and let the compositor scale it to the 1920x1080' \
        '# of the seat TV; this seat is fill-rate bound, so that buffer is what decides' \
        '# how smooth the game plays.' \
        'basepath=${IOQUAKE3_BASEPATH:-/usr/share/ioquake3}' \
        'game=${IOQUAKE3_GAME:-baseq3}' \
        'home=${IOQUAKE3_HOME:-$HOME/.local/share/ioquake3}' \
        'ioq3args=${IOQUAKE3_ARGS:-+set r_preferOpenGLES 1 +set r_fullscreen 1 +set r_mode -1 +set r_customwidth 1024 +set r_customheight 576}' \
        'export SDL_VIDEODRIVER=${SDL_VIDEODRIVER:-wayland}' \
        'export SDL_AUDIODRIVER=${SDL_AUDIODRIVER:-alsa}' \
        'exec /usr/bin/ioquake3-gl2 $ioq3args +set fs_basepath "$basepath" +set fs_game "$game" +set fs_homepath "$home" "$@"' \
        > ${D}${bindir}/ioquake3
    chmod 0755 ${D}${bindir}/ioquake3
}

# The client links the seat's libSDL2.so, which the FTEQW client already drags
# in, so this engine adds no library to the image; RDEPENDS still states what
# the binary needs so a board without it is told rather than left to segfault.
# Nothing but a library is a dependency of this engine: the vm modules packed
# into vm-pure.pk3 above are the gamecode half, and the level, model and sound
# data they drive is the owner's own pak0.pk3, which no recipe of this layer
# fetches - id's own agreements do hand the demo releases out free of charge, but
# the only carrier of those exact archives is a third-party mirror of id's dead
# ftp corner, and nobody from id answers for it. The engine boots on its own:
# without a pak it prints the missing-data error and stops, which is the answer
# for a board the owner has not put a game on yet.
RDEPENDS:${PN} += "libsdl2"

SUMMARY = "Chocolate Doom - the Doom, Heretic and Hexen engine the DOOM-family games on this seat run on"
DESCRIPTION = "Doom, Heretic and Hexen: Beyond Heretic cannot come through the fteqw client this layer builds: the rows of FTEQW's own game table that would carry them are commented out in engine/common/fs.c:203-205 ({"-doom","doom","FTE-Doom",{"doom.wad"},NULL,{"*ftedoom"},"Doom"} and its doom2 and doom3 siblings), the tree carries no ftedoom plugin, and glmod_doom.c is only a reader for the Doom map format, so there is no gamecode to load and these games need an engine of their own. This is it, and it is one codebase for all three plus Strife: src/Makefile.am:6-10 names the five programs the install target puts in its bin directory, chocolate-doom chocolate-heretic chocolate-hexen chocolate-strife and chocolate-server, from src/doom src/heretic src/hexen and src/strife, and src/d_iwad.c:40 45 58 name the WADs they want, {"doom1.wad", doom, shareware}, {"heretic1.wad", heretic, shareware} and {"hexen.wad", hexen, commercial}, so any WAD this seat runs has to be named exactly as the engine spells it and the game version is read out of the file name, not out of a checksum; this recipe carries no game data, and the README in this directory says where a WAD of the owner's own copy goes and what command runs it. The renderer asks nothing of a GL stack: src/i_video.c:146 draws the game into a 320x200 8-bit paletted SDL_Surface whose pixels are I_VideoBuffer, and src/i_video.c:1299 SDL_CreateRenderer(screen, -1, renderer_flags) hands the upscale to SDL2's own renderer, so this binary opens no GL context and carries no shader source; SDL2's wayland and EGL/GLES stack on the seat decides what that renderer is, and force_software_renderer (src/i_video.c:146, a config variable at src/m_config.c:793, bound at src/i_video.c:1528) is the knob at src/i_video.c:1285 that drops SDL_RENDERER_PRESENTVSYNC and takes SDL2's CPU blitter instead. Audio is the same shape: src/i_sound.c:97 is the table of compiled sound modules, the default snd_sfxdevice at src/i_sound.c:52 is SNDDEVICE_SB, and src/i_sdlsound.c:1120 says SNDDEVICE_SB belongs to the SDL2_mixer module, whose Init opens audio through SDL_Init(SDL_INIT_AUDIO) and Mix_OpenAudioDevice, so the sound effects need libSDL2_mixer on the board and that is what libsdl2-mixer is a dependency for here. Music needs nothing: the default snd_musicdevice is SNDDEVICE_SB too, and that device is in the built-in OPL emulator's list (src/i_oplmusic.c:1782, {SNDDEVICE_ADLIB, SNDDEVICE_SB}) while SDL2_mixer's own music module lists only PAS/GUS/WAVEBLASTER/SOUNDCANVAS/GENMIDI/AWE32 (src/i_sdlmusic.c:435), so the .mus and .mid the WADs carry are played through the engine's own MIDI and OPL code (src/mus2mid.c, src/i_oplmusic.c) with no soundfont and no patch set. Mouselook is SDL2's relative mode: src/i_video.c:268 SDL_SetRelativeMouseMode(!show), src/i_input.c:443 SDL_GetRelativeMouseState, mouse_sensitivity a config variable at src/m_config.c:132, and src/i_video.c:542 SDL_WarpMouseInWindow only when the grab is released, which SDL2 emulates on wayland through its _WAYLAND_EMULATE_MOUSE_WARP. Config and saves need no root: GetDefaultConfigDir (src/m_config.c:2624, SDL_GetPrefPath) puts them at ~/.local/share/chocolate-doom with saves at savegames/<wad>/ (src/m_config.c:2764-2769), which weston can write, and -savedir (src/m_config.c:2733) is there if a caller wants another. --video-driver is the engine's own way of naming the SDL video driver: src/m_config.c:708 binds it, src/i_video.c:1130 puts SDL_VIDEODRIVER into the environment, and src/i_video.c:1439 SetSDLVideoDriver() runs before the SDL_Init(SDL_INIT_VIDEO) at src/i_video.c:1441, so the game is started with `--video-driver wayland` rather than by putting `SDL_VIDEODRIVER` in the environment. This build is for my own use and is not redistributed."
HOMEPAGE = "https://github.com/chocolate-doom/chocolate-doom"
BUGTRACKER = "https://github.com/chocolate-doom/chocolate-doom/issues"

# The whole package is GPL-2: README.md:101 says "Chocolate Doom is distributed
# under the GNU GPL. See the COPYING", and COPYING is COPYING.md, the markdown
# rendering of the GPL-2 text (upstream keeps only that one copy; find over the
# tree gives ./COPYING.md and nothing else). textscreen/, opl/ and pcsound/ are
# in-tree copies of the same project under the same text.
LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://COPYING.md;md5=60d644347832d2dd9534761f6919e2a6"

# Pin: upstream's chocolate-doom-3.1.1 release tag, whose commit is
# 410d96855b5df5410ff591a90efeafa889119224, and which is what configure.ac:1
# AC_INIT(Chocolate Doom, 3.1.1, ...) describes. What SRCREV carries is that
# commit rather than the tag name, because the tag-name forms do not parse:
# srcrev_internal_helper at layers/openembedded-core/bitbake/lib/bb/fetch2/
# __init__.py:1241 refuses a ;tag= url whose SRCREV is any different string,
# and with the tag name as SRCREV the revision is not a sha-1, so urldata_init
# at lib/bb/fetch2/git.py:246 hands it to _latest_revision, which at :841 stops
# at "Recipe uses a floating tag/branch ... without a fixed SRCREV" for a
# recipe that does not call get_srcrev (no SRCPV in PV here). nobranch=1 is the
# other half of the pin: _contains_ref at lib/bb/fetch2/git.py:712 then asks
# only that the commit exists in the repository (git log --pretty=oneline -n 1
# <rev>) rather than that a branch contains it, and download at :447 fetches
# refs/*:refs/*, which is how the tag's commit is reached. Pinned as that commit
# rather than as the repository's tip (a2dfd51fcc00d272dc8a17a97ccd6683333ff4ea
# at the same moment, "Merge pull request #1820 from NicknineTheEagle/nn/cmake")
# because that tip is the CMake merge and this recipe builds the autotools tree.
SRC_URI = "git://github.com/chocolate-doom/chocolate-doom.git;protocol=https;nobranch=1"
SRCREV = "410d96855b5df5410ff591a90efeafa889119224"

# The version the tag carries, and PKGR pinned like the rest of this layer so
# the deb version stays comparable across rebuilds.
PV = "3.1.1"
PKGR = "r0.01"

# That commit unpacks to its own tree; autotools regenerates configure from
# configure.ac and Makefile.am (the class deletes any shipped configure and
# runs autoreconf -Wcross --install --force, layers/openembedded-core/meta/
# classes-recipe/autotools.bbclass:235), builds in ${WORKDIR}/build (B above)
# and installs with make install, which is what puts the five programs and the
# four chocolate-*-setup scripts into /usr/bin.
S = "${WORKDIR}/git"

# The class whose name is in the DEPENDS above, pkgconfig, is the one that puts
# pkg.m4 in the sysroot; the class of the same name here is what puts the tool
# itself there, since layers/openembedded-core/meta/classes-recipe/pkgconfig.bb-
# class:7 is one line, DEPENDS:prepend = "pkgconfig-native ", and that native
# package is what ships recipe-sysroot-native/usr/bin/pkg-config. configure needs
# both halves: the SDL probe at configure.ac:74 expands into a shell test that
# runs pkg-config, and layers/openembedded-core/meta/classes-recipe/autotools.bb-
# class:239 only exports CONFIG_SITE, never PKG_CONFIG, so with only the m4 file
# present the generated configure prints the error autoconf has for a probe whose
# helper it cannot find, "The pkg-config script could not be found or is too old",
# which is where the run stopped after DEPENDS gained pkgconfig. The layer's own
# precedent for the same thing is recipes-apps/opennow/opennow_git.bb:530, which
# inherits pkgconfig next to qt6-cmake, and whose :365-:381 comment records that
# the class exports PKG_CONFIG_LIBDIR, PKG_CONFIG_PATH and PKG_CONFIG_SYSROOT_DIR
# at the task sysroot and that the tool lives in recipe-sysroot-native/usr/bin.
inherit autotools pkgconfig

# libsdl2 is what the renderer, the window and the input go through; libsdl2-mixer
# is the sound-effects module, which the default SNDDEVICE_SB picks (see the
# DESCRIPTION above) and without which the games would have no sound effects at
# all, the only alternative in src/i_sound.c:97 being the pcsound module that
# wants /dev/dsp. libsdl2-mixer drags in its own deps (flac libmikmod libvorbis,
# layers/meta-openembedded/meta-oe/recipes-graphics/libsdl/libsdl2-mixer_2.0.4.bb:4).
# pkgconfig is here because configure.ac:74 80 89 154 172 190 ask for packages
# through PKG_CHECK_MODULES, and in this build that macro is undefined: the
# definition lives in the one file that carries it, the target pkgconfig recipe's
# ${datadir}/aclocal/pkg.m4 (layers/openembedded-core/meta/recipes-devtools/
# pkgconfig/pkgconfig_git.bb:43), and layers/openembedded-core/meta/classes-
# recipe/autotools.bbclass:186 points aclocal at the recipe's own target sysroot
# with --system-acdir=${ACLOCALDIR}/, so the file is only visible with pkgconfig
# among the dependencies. With it missing, m4 leaves every PKG_CHECK_MODULES call
# unexpanded, which is what leaves the macros inside its arguments sitting in the
# generated text at top level, and autoconf's pattern check then reports those
# innocent tokens as undefined -- the four configure.ac:25 41 81 157 lines
# autom4te prints before it gives up (AS_IF, AM_PROG_AR, AC_DEFINE and
# AC_MSG_FAILURE, none of which is anything to do with pkgconfig). The
# alternative OE itself uses for this, layers/openembedded-core/meta/recipes-
# graphics/wayland/wayland_1.22.0.bb:36's sed that deletes the PKG_CHECK_
# MODULES lines, cannot be done here: :74 is the call whose SDL_CFLAGS and
# SDL_LIBS the renderer's own compile and link lines are built from.
DEPENDS = "libsdl2 libsdl2-mixer pkgconfig"
RDEPENDS:${PN} = "libsdl2 libsdl2-mixer"

# What is switched off is what the seat has no use for: SDL2_net is four-player
# network play and src/net_sdl.c:384 else // DISABLE_SDL2NET says the code compiles
# without it; doc, fonts and bash-completion want python and asciidoc at build
# time for the man pages, the textscreen fonts and the completion scripts, and
# the seat has neither; icons and metainfo are desktop files weston does not
# launch. libsamplerate (resampling of the WAD sounds), libpng (PNG screenshots)
# and fluidsynth (a MIDI soundfont backend, and the seat's music is the OPL
# synth) are asked for as "check" by configure.ac:146 164 182, so --without keeps
# them out of both the dependency list and the binary instead of letting the
# probe decide. zpool (the memory pools) stays on, which is what src/i_sound.c's
# games run on by default.
EXTRA_OECONF = "\
    --disable-sdl2net \
    --disable-doc \
    --disable-fonts \
    --disable-icons \
    --disable-bash-completion \
    --without-libsamplerate \
    --without-libpng \
    --without-fluidsynth \
"

# chocolate-setup itself is a noinst_PROGRAMS (src/Makefile.am:12) and the four
# chocolate-*-setup entries in bin are the generated shell copies of it
# (src/Makefile.am:14 SETUP_BINARIES, :19 execgames_SCRIPTS); they want a terminal
# of their own and are not what the seat runs the games with, so they carry no
# dependency of their own here.

# The desktop files the install puts outside /usr/bin are taken out. Two of them
# are made unconditionally regardless of the switches above: src/Makefile.am:240
# and :241 are the metainfo_DATA list, whose four appstream XMLs go to
# ${prefix}/share/metainfo with no ENABLE_ around them, and the 128x128 hicolor
# icons under ${prefix}/share/icons/hicolor come in through the same install
# pass. --disable-icons at configure.ac:127 and :244 only clears the HAVE_ICONS
# and the python that generates the pngs, so the rule still installs what the
# tree ships ready-made. They are then the 14 entries do_package reports as
# installed and not shipped. Neither kind does anything on this seat: the appstream
# files are for a software centre and a metainfo-aware launcher, and the icons are
# for a desktop that opens a .desktop file, while the seat starts the games by
# running the programs in /usr/bin directly, so what is wanted is that they are not
# installed at all, which is also what keeps them out of the image the seat's
# rootfs has so little room in.
do_install:append () {
    rm -rf ${D}/usr/share/metainfo ${D}/usr/share/icons
}

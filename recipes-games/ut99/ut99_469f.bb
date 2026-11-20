SUMMARY = "Unreal Tournament 99 GOTY on the OldUnreal 469f arm64 engine, in /gamedata/ut"
DESCRIPTION = "Unreal Tournament 99 is Epic's 1999 arena shooter, and this recipe is the \
game as the seat plays it: the retail GOTY CD1 data plus OldUnreal's 469f arm64 build of \
the engine that runs it on aarch64. Neither half comes from this layer's sources. The data \
is the GOTY CD1 ISO from OldUnreal's file mirror, 649,633,792 bytes: every map of the retail \
game (Maps/, 54 MB), its textures (Textures/, 225 MB), sounds (Sounds/, 138 MB), music \
(Music/, 48 MB) and the UnrealScript packages and ini defaults under System/, and nothing \
else on the disc is installed - DirectX7/, GameSpy/, NetGamesUSA.com/, Microsoft/, \
System400/, Setup.exe and the disc's html/bitmap help belong to the Windows installer this \
seat does not run. What is laid down is 627 files, 553 MB, and the engine tarball on top. \
The engine is OldUnreal's community 469 patch, release v469f-rc6, Linux arm64: a 75,147,582-\
byte tar.bz2 whose System/ carries ut-bin (a 214,424-byte aarch64 PIE whose RUNPATH is \
$ORIGIN), the Engine/Core/Render game libraries, the renderers OpenGLDrv, XOpenGLDrv, \
VulkanDrv and SoftDrv, ALAudio with its bundled OpenAL Soft, SDL 3.4.10 and the .u packages \
that bring the data tree up to 469. It is installed over the ISO's System/ so the 469 engine \
and packages win over the 436 ones on the disc. The renderer this seat actually draws with is \
SoftDrv: the seat's filesystem carries no libGL.so.1 at all for OpenGLDrv, XOpenGLDrv reaches \
a GLES 3.1 context but Vivante's GLSL ES 3.10 compiler rejects its shaders outright (its own \
error log answers "Unrecognizable interface block qualifier" for the driver's out/in blocks, in \
either the OES or the EXT spelling, and the DrawTile uniform block is over GL ES 3.1's per-stage \
budget), and \
VulkanDrv finds no device that meets its minimum. The shipped UnrealTournament.ini therefore \
names SoftDrv explicitly at 1920x1080, the panel's own size, so nothing is upscaled: 1280x720 \
still draws but weston magnifies it, and changing size after the first frame crashes the \
engine in USDLViewport::Repaint. The tree lives on /gamedata/ut rather than in /usr/share \
because the seat's root filesystem has 224 MB free and the game is 686 MB; /gamedata is the \
8 GB data partition, and the package is installed onto it with dpkg-deb -x rather than dpkg \
-i. Local personal build only, no redistribution: the game is Epic's, the terms are Epic's \
Terms of Service as OldUnreal's LICENSE.md states, and nothing here is a free licence. The \
launcher enforces one instance, which is not cosmetic: two UT99 processes on this seat made \
the player's own weapon and footstep sounds disappear."
HOMEPAGE = "https://github.com/OldUnreal/UnrealTournamentPatches"

# The engine tarball's LICENSE.md is the terms this install is under: it opens
# with "The Epic Games Terms of Service apply to the use and distribution of
# this game", then lists the third-party licences of the binaries OldUnreal
# ships (musl libc, libogg, zlib, GPL-2 sources and their offers). It is not a
# free licence for the game data, and the game data is Epic's; this recipe is
# for a personal build on the seat's owner's own hardware and does not
# redistribute the result. There is no generic licence name for Epic's terms,
# so the SBOM reads them out of the tree this recipe unpacks.
LICENSE = "LicenseRef-Epic-Games-Unreal-Tournament-ToS"
NO_GENERIC_LICENSE[LicenseRef-Epic-Games-Unreal-Tournament-ToS] = "ut/LICENSE.md"
LIC_FILES_CHKSUM = "file://ut/LICENSE.md;md5=53f0740816ff9701ae6592cb7dafb07c"

# Two prebuilt sources, both pinned by sha256 so changed bytes under these
# addresses fail do_fetch instead of being installed:
#
#  - the GOTY CD1 ISO from OldUnreal's file mirror. files.oldunreal.net serves
#    it with Content-Length 649633792 and Content-Type application/x-iso9660-
#    image, and it hashes to the sha256 below. unpack=false because bitbake's
#    unpacker works off the file extension and has no ISO handler: the file
#    comes whole and do_extract_gamedata takes the data dirs out of it with bsdtar.
#
#  - OldUnreal's 469f release, asset OldUnreal-UTPatch469f-Linux-arm64.tar.bz2
#    under the repository's v469f-rc6 tag: 75,147,582 bytes, sha256 below.
#    unpack=false as well, because it is not the release alone: the same
#    do_extract_gamedata lays it over the ISO tree, so the order of the two
#    unpacks is explicit rather than bitbake's SRC_URI order.
SRC_URI = " \
    https://files.oldunreal.net/UT_GOTY_CD1.ISO;unpack=false;sha256sum=e184984ca88f001c5ddd52035d76cd64e266e26c74975161b5ed72366c74704f \
    https://github.com/OldUnreal/UnrealTournamentPatches/releases/download/v469f-rc6/OldUnreal-UTPatch469f-Linux-arm64.tar.bz2;unpack=false;sha256sum=56c09d3f7f1889f01acf21638c71952c5e2dfbd737b075ffaea3b94bfe52ef46 \
    file://UnrealTournament.ini \
    file://ut99 \
"

# The engine's own build of the 469 patch, which is what the release notes of
# v469f-rc6 call it; dpkg accepts it as a version.
PV = "469f"

# PKGR pinned like the rest of this layer, so the deb version stays comparable
# across rebuilds.
PKGR = "r0.01"

# The fetched ISO and tarball stay in the work directory, the extracted tree is
# built there under ut/, and the two local files are already there, so S is the
# work directory itself.
S = "${WORKDIR}"

# bsdtar takes the data off the ISO. It is libarchive's ISO9660 reader, Rock
# Ridge included, which is what keeps the disc's MixedCase names (System/, Maps/,
# ...) and is the only unpacker in oe-core that reads this disc at all: p7zip-
# native's 7z is a symlink to 7za, and 7za answers "Can not open the file as
# archive" on this ISO and exits 2. The build host's own tools are not used.
DEPENDS = "libarchive-native"

# The two halves are put together in a task of their own rather than by
# bitbake's unpacker: the ISO's System/Maps/Music/Sounds/Textures and its
# Help/ReadMe.htm are laid down first (the Windows installer's
# dll/exe/bat/ico/bmp/cab/inf files inside those dirs are skipped), then the
# 469 tarball is unpacked over the result, so its System/ binaries and .u
# packages replace the 436 ones from the disc and its SystemLocalized/, Web/,
# Help/ReleaseNotes.htm and Textures/ are added. Help/ReadMe.htm is the
# release's own readme; it is kept because the game's menu opens it and because
# it is the closest thing the CD has to a notice of what the disc is.
# Help/{Splash*.bmp,*.ico,Logo.bmp} are the Windows installer artwork and are
# not taken.
#
# It is not a do_unpack:append because bsdtar comes from libarchive-native, and a
# recipe's native sysroot is not staged until do_prepare_recipe_sysroot, which
# runs after do_unpack. This task sits between them and calls bsdtar by its
# sysroot path.
python do_extract_gamedata() {
    import os
    import shutil
    import subprocess

    s = d.getVar("S")
    bsdtar = os.path.join(d.getVar("STAGING_BINDIR_NATIVE"), "bsdtar")
    ut = os.path.join(s, "ut")
    if os.path.isdir(ut):
        shutil.rmtree(ut)
    os.makedirs(ut)

    # bsdtar's extract takes whole-directory patterns, so naming the data dirs
    # takes those subtrees and nothing else on the disc (DirectX7/, GameSpy/,
    # Microsoft/, NetGamesUSA.com/, System400/, Logs/, Setup.exe, Autorun.inf),
    # and the excludes drop the Windows installer's dll/exe/bat/ico/bmp/cab/inf
    # files that sit inside the data dirs.
    iso = os.path.join(s, "UT_GOTY_CD1.ISO")
    subprocess.check_call([
        bsdtar, "-xf", iso, "-C", ut,
        "--exclude", "*.dll", "--exclude", "*.exe", "--exclude", "*.bat",
        "--exclude", "*.ico", "--exclude", "*.bmp", "--exclude", "*.cab",
        "--exclude", "*.inf",
        "System", "Maps", "Music", "Sounds", "Textures", "Help/ReadMe.htm",
    ])

    # the 469 engine goes on top, so its System/ binaries and packages win
    # over the 436 ones on the disc. tarfile rather than the tar binary: the
    # task's PATH is the native sysroot and does not have to carry a host tar
    # for this.
    eng = os.path.join(s, "OldUnreal-UTPatch469f-Linux-arm64.tar.bz2")
    import tarfile
    with tarfile.open(eng) as t:
        t.extractall(ut)
}
# do_populate_lic is in the list because LIC_FILES_CHKSUM and NO_GENERIC_LICENSE
# both point at ut/LICENSE.md, the engine tarball's terms file, and that file only
# exists once this task has unpacked the tarball: populate_lic would otherwise run
# straight after do_unpack and answer "LIC_FILES_CHKSUM points to an invalid file".
addtask extract_gamedata after do_prepare_recipe_sysroot before do_compile do_populate_lic
do_extract_gamedata[depends] += "libarchive-native:do_populate_sysroot"

# Where the tree is installed. /gamedata is the seat's data partition: the
# 4.2 GB root filesystem has 224 MB free and this tree is 686 MB, so it cannot
# live under /usr. Override for a different partition layout.
UT99_DIR ?= "/gamedata/ut"

do_install() {
    install -d ${D}${UT99_DIR}
    cp -a ${S}/ut/. ${D}${UT99_DIR}/

    # The shipped configuration is the one measured on the seat: SoftDrv, the
    # renderer that draws, at 1920x1080, the panel's own size, so weston has
    # nothing to magnify. It replaces the ISO's and the engine's defaults; the
    # engine reads System/UnrealTournament.ini when it exists and only writes
    # a new one when it does not.
    install -m 0644 ${WORKDIR}/UnrealTournament.ini ${D}${UT99_DIR}/System/UnrealTournament.ini

    # The engine wants a User.ini next to it and writes its defaults there on
    # first run, but a package-owned tree is root-owned until the seat's user
    # takes it over (see README.md); shipping the engine's DefUser.ini as
    # User.ini means the first run does not depend on that write succeeding.
    install -m 0644 ${D}${UT99_DIR}/System/DefUser.ini ${D}${UT99_DIR}/System/User.ini

    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/ut99 ${D}${bindir}/ut99

    # the tree is data weston, not root, has to read and write (logs, Cache/,
    # save games), and the extracted files keep the uid of the bitbake run,
    # which do_package_qa calls host contamination.
    chown -R root:root ${D}${UT99_DIR}
}

# The package is a prebuilt game tree plus one shell script: its ELF files are
# OldUnreal's and id's, not this build's, so nothing here may strip, split or
# relink them, and the QA checks that assume a recipe built its own binaries do
# not apply.
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"
EXCLUDE_FROM_SHLIBS = "1"
INSANE_SKIP:${PN} += "already-stripped ldflags buildpaths dev-so file-rdeps textrel"

FILES:${PN} += "${UT99_DIR} ${bindir}/ut99"

# The launcher runs OldUnreal's ut-bin with the compressed tree next to it and
# the system's ALSA/OpenAL stack underneath; the engine carries its own SDL 3,
# OpenAL Soft, libsndfile and mpg123 in System/, so the package needs no
# runtime dependency beyond a libc.

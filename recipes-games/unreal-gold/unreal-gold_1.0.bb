SUMMARY = "Unreal / Unreal Gold on the seat: Gold's data off the 2000 CD, run by OldUnreal's \
469f arm64 engine, with SoftDrv"
DESCRIPTION = "Unreal is Epic's 1998 first-person game, and this recipe is the way the seat \
plays it, which is a hybrid rather than a port: the content is Unreal Gold's, the campaign's \
maps, textures, sounds and music as they sit on the Gold CD of August 2000, and the binary \
that runs them is OldUnreal's 469f arm64 build of the Unreal Tournament engine, a 214,424-\
byte aarch64 PIE. It has to be a hybrid because Unreal Gold's own engine cannot draw here \
at all, and that was measured rather than assumed: the 227k arm64 Unreal patch tried first \
ships OpenGLDrv.so and XOpenGLDrv.so and nothing else, its log walks through OpenGLDrv \
failing on the undefined symbol NvOptimusEnablement, SDLDrv.Errors.Failed3D and death by \
the assertion 'RenDev [SDLViewport.cpp] [Line: 1068]', XOpenGLDrv answers 'XOpenGL: No \
Window found!', and the seat's filesystem carries no libGL.so.1 anywhere at all - only \
libEGL.so, libGLESv2.so and Vivante's libGLSLC.so. What does draw is SoftDrv, and SoftDrv \
is an accepted renderer for these UE1 games by the seat's owner, so the engine is the 469 \
line, the only arm64 UE1 binary on this seat that ships that renderer. The 469f tarball has \
no Unreal 1 game in it - ut-bin and ucc-bin only - so the game comes off the Gold CD, and \
the Unreal 1 classes 469 wants come from the UnrealShare.u and UnrealI.u that ship inside \
469 rather than from Gold's own packages, whose 2000-era class layout the 469 linker \
refuses. Two things are patched into the tree to make the campaign playable, and both are \
measured. Gold's travel and restart URLs are Windows paths, so the eight bytes two dots, \
backslash, maps, backslash are rewritten in place to two dots, slash, Maps, slash, one \
occurrence each in System/UMenu.u and System/UnrealShare.u and none in the .unr maps, which \
keep their travel targets as plain names: that is what lets Unreal.unr travel to Vortex2.unr \
on Linux. And the ini names SoftDrv with its internal size, which the shipped copy sets to \
800x600, the internal render size the seat's owner plays at, drawn by SoftDrv and scaled by \
weston onto the 1920x1080 panel; 1920x1080 internal also draws and was measured, 1280x720 \
and 960x540 draw too but looked worse on this panel, and no size may be changed after the \
first frame, which segfaults the engine in USDLViewport::Repaint. The front-end being UT99's \
menu rather than Gold's is a settled consequence rather than a bug, and it was tested: \
swapping the 227k UMenu.u into 469 dies on the class set the 227 packages want, 30 lines of \
load errors including UWindow.UWindowComboBoxControl, Engine.ObjectShadow and \
UnrealShare.UnrealUserManager, so the 469 menu is the only one that loads and the seat shows \
it on ESC. The tree lives on /gamedata rather than under /usr because the seat's 4.2 GB root \
filesystem has 69 MB free and this tree is 882 MB, and it is installed onto that partition \
with dpkg-deb -x rather than dpkg -i. Local personal build only, no redistribution: the game \
is Epic's and the terms are Epic's Terms of Service, which OldUnreal's LICENSE.md states, and \
nothing here is under a free licence."
HOMEPAGE = "https://github.com/OldUnreal/Unreal-testing"

# The terms this install is under are the ones OldUnreal's 469f release carries: its
# LICENSE.md opens "The Epic Games Terms of Service apply to the use and distribution of
# this game" and then lists the third-party licences of the binaries it ships. Epic's
# Terms of Service are not a free licence and no distro carries them under a generic
# name, so the SBOM reads them out of the tree this recipe unpacks; this build is for
# the seat's owner's own use and does not redistribute the result.
LICENSE = "LicenseRef-Epic-Games-Unreal-ToS"
NO_GENERIC_LICENSE[LicenseRef-Epic-Games-Unreal-ToS] = "ugt/LICENSE.md"
LIC_FILES_CHKSUM = "file://ugt/LICENSE.md;md5=53f0740816ff9701ae6592cb7dafb07c"

# Four prebuilt sources, all four pinned by sha256 so that changed bytes under these
# addresses fail do_fetch rather than ship a different game:
#
#  - the Unreal Tournament GOTY CD1 ISO off OldUnreal's file mirror, the same asset the
#    ut99 recipe pins: its System/ is the 436-era tree the 469 packages go over, and its
#    Maps/Textures/Sounds/Music are the Unreal 1 content 469 was built to run, so the
#    hybrid starts from the whole ut99 tree.
#
#  - OldUnreal's 469f release (v469f-rc6 tag): the arm64 engine (ut-bin), the renderers
#    including SoftDrv.so, the game libraries and the .u packages that bring the data
#    tree up to 469. Same asset and same pin as ut99.
#
#  - the Unreal Gold CD itself, UNREAL_GOLD.ISO off OldUnreal's file mirror: 676,734,976
#    bytes, hashes to the pin below, and the same pin the OldUnreal Linux installer for
#    Unreal uses. Its MAPS/MUSIC/SOUNDS/TEXTURES are the 1998 content this recipe lays
#    over the UT99 tree - 99 maps, 52 texture packages, 96 sound packages and 48 tracks.
#    unpack=false, because bitbake's unpacker has no ISO handler; do_extract_gamedata
#    takes the dirs out with bsdtar.
#
#  - OldUnreal's 227k Unreal patch for Linux, v227k_15: 131,414,620 bytes. Only its
#    Maps/Music/Sounds/Textures additions are taken (bonus maps, the window fonts, Old
#   WeaponsTex.utx and OldWeaponsSounds.uax, the content the 227 patch adds to the CD).
#    Its System/ is deliberately NOT taken: that is the 227 engine and its packages, and
#    469 refuses them, which is what the front-end swap above measured.
SRC_URI = " \
    https://files.oldunreal.net/UT_GOTY_CD1.ISO;unpack=false;sha256sum=e184984ca88f001c5ddd52035d76cd64e266e26c74975161b5ed72366c74704f \
    https://github.com/OldUnreal/UnrealTournamentPatches/releases/download/v469f-rc6/OldUnreal-UTPatch469f-Linux-arm64.tar.bz2;unpack=false;sha256sum=56c09d3f7f1889f01acf21638c71952c5e2dfbd737b075ffaea3b94bfe52ef46 \
    https://files.oldunreal.net/UNREAL_GOLD.ISO;unpack=false;sha256sum=7e360d0cc9e5533f38859819fd3cbfea7c475ecd428f9f433b5f8e1d5742cbca \
    https://github.com/OldUnreal/Unreal-testing/releases/download/v227k_15/OldUnreal-UnrealPatch227k-Linux.tar.bz2;unpack=false;sha256sum=b04eba6d5c885c98bd9da8279840b15cd8fa06ed2b750235e7ad7aa26ef3f7a1 \
    file://UnrealTournament.ini \
    file://unreal-gold \
"

# the payload's own version; the engine inside it is OldUnreal's 469f.
PV = "1.0"

# PKGR pinned like the rest of this layer, so the deb version stays comparable
# across rebuilds.
PKGR = "r0.01"

# The four fetched assets stay in the work directory and the extracted tree is built
# there under ugt/, so S is the work directory itself.
S = "${WORKDIR}"

# bsdtar takes the data off the two CDs; it is libarchive's ISO9660 reader, Rock Ridge
# included, which is the only unpacker in oe-core that reads these discs at all (7za
# answers "Can not open the file as archive" on the UT ISO). The build host's own tools
# are not used.
DEPENDS = "libarchive-native"

# The four halves are put together in a task of their own rather than by bitbake's
# unpacker, so the order is explicit: the UT99 ISO's data dirs first, the 469f release
# over it (so 469's binaries and .u packages win over the disc's 436 ones), then the
# Gold CD's Maps/Music/Sounds/Textures over that, then the 227k patch's data additions
# last. On a name that exists on both CDs the Gold CD wins, which is how the tree the
# seat was proven with was built (its Castle1.utx is the CD's 4,533,635-byte package,
# not the 177,545-byte UT99 one), and it is a case-insensitive match because the seat's
# /gamedata partition is vfat and two spellings of one package name would collide there.
# The Gold CD's own dir names come off the disc upper case (MAPS/, MUSIC/, SOUNDS/,
# TEXTURES/) while the engine and the ini use Maps/, Music/, Sounds/ and Textures/, so
# the merge renames them.
python do_extract_gamedata() {
    import os
    import shutil
    import subprocess
    import tarfile

    s = d.getVar("S")
    bsdtar = os.path.join(d.getVar("STAGING_BINDIR_NATIVE"), "bsdtar")
    out = os.path.join(s, "ugt")
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(out)

    # 1. the UT99 disc: the data dirs plus the release's own readme. The Windows
    #    installer's files inside those dirs are skipped, as in the ut99 recipe.
    utiso = os.path.join(s, "UT_GOTY_CD1.ISO")
    subprocess.check_call([
        bsdtar, "-xf", utiso, "-C", out,
        "--exclude", "*.dll", "--exclude", "*.exe", "--exclude", "*.bat",
        "--exclude", "*.ico", "--exclude", "*.bmp", "--exclude", "*.cab",
        "--exclude", "*.inf",
        "System", "Maps", "Music", "Sounds", "Textures", "Help/ReadMe.htm",
    ])

    # 2. the 469 engine over it, so its System/ binaries and packages win over the
    #    436 ones from the disc, and its Textures/, Music/ and Sounds/ additions go in.
    eng = os.path.join(s, "OldUnreal-UTPatch469f-Linux-arm64.tar.bz2")
    with tarfile.open(eng) as t:
        t.extractall(out)

    # 3. the Gold CD: its dirs are taken whole, Windows installer dirs are not, and
    #    the uppercase disc names become the MixedCase names the engine uses.
    stage = os.path.join(s, "goldcd")
    if os.path.isdir(stage):
        shutil.rmtree(stage)
    os.makedirs(stage)
    goldiso = os.path.join(s, "UNREAL_GOLD.ISO")
    names = subprocess.run([bsdtar, "-tf", goldiso],
                           capture_output=True, text=True).stdout.split()
    tops = {}
    for n in names:
        top = n.split("/")[0]
        if top.lower() in ("maps", "music", "sounds", "textures"):
            tops.setdefault(top.lower(), top)
    subprocess.check_call([bsdtar, "-xf", goldiso, "-C", stage]
                          + [tops[k] for k in sorted(tops)])
    merge = {"maps": "Maps", "music": "Music", "sounds": "Sounds", "textures": "Textures"}
    for key, mixed in merge.items():
        srcdir = os.path.join(stage, tops[key])
        dst = os.path.join(out, mixed)
        os.makedirs(dst, exist_ok=True)
        for dirpath, dirnames, fnames in os.walk(srcdir):
            rel = os.path.relpath(dirpath, srcdir)
            for f in fnames:
                src = os.path.join(dirpath, f)
                dstleaf = os.path.join(dst, rel, f) if rel != "." else os.path.join(dst, f)
                # a package both CDs carry: drop the one already in the tree, case-
                # insensitively, so the Gold CD's copy replaces it instead of sitting
                # next to it as a second spelling.
                leaf = os.path.dirname(dstleaf)
                os.makedirs(leaf, exist_ok=True)
                keep = f.lower()
                for twin in os.listdir(leaf):
                    if twin.lower() == keep and twin != f:
                        os.remove(os.path.join(leaf, twin))
                if os.path.exists(dstleaf):
                    os.remove(dstleaf)
                shutil.copy2(src, dstleaf)
    shutil.rmtree(stage)

    # 4. the 227k patch's data additions last, and only its data: its System/ is the
    #    227 engine, whose class layout 469 refuses.
    patch = os.path.join(s, "OldUnreal-UnrealPatch227k-Linux.tar.bz2")
    with tarfile.open(patch) as t:
        for m in t.getmembers():
            parts = m.name.split("/")
            if len(parts) > 1 and parts[0].capitalize() in ("Maps", "Music", "Sounds", "Textures"):
                t.extract(m, out)

    # 5. the travel paths. Gold's URLs are Windows paths: '..\maps\' is eight bytes
    #    and '../Maps/' is the same eight bytes, so the rewrite happens in place and
    #    no offset in any package moves. Measured on the tree the seat runs: one
    #    occurrence each in System/UMenu.u and System/UnrealShare.u, and none in the
    #    .unr maps themselves, which carry their travel targets as plain names.
    old = b".." + b"\x5c" + b"maps" + b"\x5c"
    new = b".." + b"/" + b"Maps" + b"/"
    hits = {}
    for dirpath, dirnames, fnames in os.walk(os.path.join(out, "System")):
        for f in fnames:
            p = os.path.join(dirpath, f)
            with open(p, "rb") as fh:
                blob = fh.read()
            if old in blob:
                with open(p, "wb") as fh:
                    fh.write(blob.replace(old, new))
                hits[os.path.relpath(p, out)] = blob.count(old)
    if not hits:
        raise RuntimeError("no Windows travel path found to patch: "
                           "the tree is not the one this recipe was proven with")
    print("unreal-gold: Windows travel paths rewritten in place, "
          "one per package, in: " + ", ".join(sorted(hits)))
}
# do_populate_lic is in the list because LIC_FILES_CHKSUM and NO_GENERIC_LICENSE both
# point at ugt/LICENSE.md, the engine tarball's terms file, and that file only exists
# once this task has unpacked the tarball.
addtask extract_gamedata after do_prepare_recipe_sysroot before do_compile do_populate_lic
do_extract_gamedata[depends] += "libarchive-native:do_populate_sysroot"

# Where the tree is installed. /gamedata is the seat's data partition: the 4.2 GB
# root filesystem has 69 MB free and this tree is 882 MB, so it cannot live under
# /usr. Override for a different partition layout.
UG_DIR ?= "/gamedata/ugt"

do_install() {
    install -d ${D}${UG_DIR}
    cp -a ${S}/ugt/. ${D}${UG_DIR}/

    # the measured configuration: SoftDrv, the renderer that draws, at 800x600
    # internal, which is the size the seat's owner plays at on this 1920x1080 panel.
    install -m 0644 ${WORKDIR}/UnrealTournament.ini ${D}${UG_DIR}/System/UnrealTournament.ini

    # the engine wants a User.ini next to its ini and writes its defaults there on
    # first run; shipping DefUser.ini as User.ini means that first run does not
    # depend on the write succeeding in a package-owned tree.
    install -m 0644 ${D}${UG_DIR}/System/DefUser.ini ${D}${UG_DIR}/System/User.ini

    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/unreal-gold ${D}${bindir}/unreal-gold

    # the tree is data weston, not root, has to read and write (logs, Cache/, save
    # games), and the extracted files otherwise keep the uid of the bitbake run.
    chown -R root:root ${D}${UG_DIR}
}

# The package is two prebuilt game trees plus one shell script: its ELF files are
# OldUnreal's, not this build's, so nothing here may strip, split or relink them,
# and the QA checks that assume a recipe built its own binaries do not apply.
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"
EXCLUDE_FROM_SHLIBS = "1"
INSANE_SKIP:${PN} += "already-stripped ldflags buildpaths dev-so file-rdeps textrel"

FILES:${PN} += "${UG_DIR} ${bindir}/unreal-gold"

# The launcher runs OldUnreal's ut-bin with the tree next to it and the seat's own
# ALSA/OpenAL stack underneath; the engine carries its own SDL 3, OpenAL Soft,
# libsndfile and mpg123 in System/, so the package needs no runtime dependency
# beyond a libc.

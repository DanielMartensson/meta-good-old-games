# EDuke32 - the Duke Nukem 3D engine, and how to put an episode on top of it

## Load it

```bash
bitbake eduke32
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the engine only:

```
/usr/bin/eduke32    the engine: r10669, classic software renderer, drawn through SDL2
```

## Play it

Duke Nukem 3D's 1996 release is an x86 DOS program (`DUKE3D.EXE` reading `DUKE3D.GRP`),
so nothing of that release runs on the seat. What runs is EDuke32 reading **the same
data** with no conversion step. Put your own copy of an episode's data where you like
(here: `/gamedata/games/duke3d`) and name it to the engine with `-j`:

```bash
export HOME=/home/weston XDG_RUNTIME_DIR=/home/weston WAYLAND_DISPLAY=wayland-1
export SDL_VIDEODRIVER=wayland SDL_AUDIODRIVER=alsa
/usr/bin/eduke32 -j /gamedata/games/duke3d -nologo -nosetup -v1 -l1
```

The names the engine spells are DOS names and they are case-bearing: what a v1.3d
Episode One tree has to hold is `DUKE3D.GRP`, `DEFS.CON`, `GAME.CON`, `USER.CON`,
`DUKE.RTS` (plus `LICENSE.TXT` and `README.DOC` so you know which release it is).

Getting a copy: 3D Realms' own site still carries its shareware release of Episode One,
and the archive is a self-extractor two layers deep, so unpack it yourself:

```bash
curl -LO 'https://dukeworld.duke4.net/classic%20dukeworld/share/3dduke13.zip'   # 5,924,374 B
unzip 3dduke13.zip                                  # LICENSE.TXT, INSTALL.EXE, FILE_ID.DIZ, DN3DSW13.SHR
unzip DN3DSW13.SHR -d duke3d                        # the 17-member v1.3d release, 12,833,604 B
mkdir -p /gamedata/games/duke3d && mv duke3d/* /gamedata/games/duke3d/
```

`DN3DSW13.SHR` is a PKWARE self-extracting archive (an MZ header plus PKLITE-compressed
members with a zip directory record appended), so an ordinary zip reader opens it. Keep
the seven files above and drop the ten the seat has no use for: `DUKE3D.EXE`, `SETUP.EXE`,
`SETMAIN.EXE`, `COMMIT.EXE`, `DN3DHELP.EXE`, `DEMO1.DMO`/`DEMO2.DMO`/`DEMO3.DMO`,
`MODEM.PCK`, `ULTRAMID.INI`.

`/usr/bin/duke3d-demo`, the launcher the payload recipe used to write, is gone from this
layer; the command above is what it execs. Override with `DUKE3D_ARGS`-style arguments
by editing that line - the engine's own defaults are `-nologo -nosetup -v1 -l1`, which
starts Episode One level 1 because this release only has Episode One; `-nologo` alone
sits you at the main menu, and `-r -v1 -l1` records a demo.

## Why the command is this shape

| Piece | Reason |
| --- | --- |
| `-j <dir>` | the data is never beside the executable the way it is for a DOS game: `G_ExtPreInit` at `source/duke3d/src/common.cpp:243-251` puts the current directory on the search list and then chdirs into `~/.config/eduke32` (`common.cpp:281`), so the payload has to be handed over explicitly. `-j [dir]` at `cmdline.cpp` is "Add a directory to Duke3D's search list", and the seat logs the working run as `INFO\| Using directory <dir>/` then `INFO\| Using DUKE3D.GRP as main data file.` |
| **never** `DUKE3DGRP=/abs/path/DUKE3D.GRP` | that override at `common.cpp:328` looks right and does not play: the engine logs the override, reads the grp header, then cannot reopen the file and dies `ERROR\| No palette found.` - the palette is a member *inside* the grp. Set `DUKE3DGRP` only for a grp you know works |
| one dash, never two | the parser matches the option name at `c+1` after **one** leading dash, so `-nologo` `-nosetup` `-v1` `-l1` are the spellings; `--nologo` is reported as `WARN\| Ignored application parameter '--nologo'.` |
| `-nosetup` | wanted even though this build has `STARTUP_WINDOW=0`: there is no startup window, but the engine still opens its SETUP screen for a configuration file it has never seen (`game.cpp:6732` tests `g_noSetup` against `ud.configversion`). With `-nosetup` the seat's run goes straight into the level (`INFO\| E1L1: HOLLYWOOD HOLOCAUST`) |

The renderer is EDuke32's CLASSIC one (`USE_OPENGL=0`, `build.h:1082` answers
`REND_CLASSIC`), which draws through SDL2's own textures and blitters rather than
through desktop GL - this board has EGL, GLES 1/2/3 and the Vivante GAL driver and
**no desktop GL at all**, and EDuke32's whole GL path is desktop-only (`#version 110`/
`#version 120`/`#version 130` shaders, and `compat.h:1541` defines `EDUKE32_GLES` only
for Android and iOS with no make toggle for it). So the seat's picture is a plain SDL2
wayland surface, and SDL2's `SDL_VIDEODRIVER=wayland` is what puts it there - this board
carries no X server at all. Audio goes to the seat's ALSA sink, which is why
`SDL_AUDIODRIVER=alsa`.

## What may be downloaded, and where from

The engine's own tree is GPL-2 and comes from dukeworld as a pinned tarball. Duke's
shareware episode comes from **3D Realms' own site** (`dukeworld.duke4.net`, the
`classic dukeworld/share` page), which is the release's own publisher, and its
`LICENSE.TXT` is 3D Realms' own shareware agreement: *"Individuals can -- and are
encouraged! -- to give away copies of the Game without charge"* - free to download and
free to play, not free to ship. Nothing of this is rebuilt or redistributed by this
layer; the fetch above is a hand install of a copy you own.

## Measured

Pins, sizes, the seat's own log of a working run, cost and failure modes are in
[PROOF.md](PROOF.md).

## Files

```
eduke32_20260807.bb   the engine, built with EDuke32's own GNUmakefile + Common.mak
README.md             this file
PROOF.md              pins, sizes, the measured run, failure modes
```

There are no local patches: every setting that matters is passed on the make command
line rather than edited into the tree.

# meta-good-old-games

The good old games that run on embedded systems e.g STM32MP25x, on the `scarthgap`
branch of this repository: one branch per Yocto release, and this branch carries
the whole layer - every engine, every patch. `main` is only the index page, and
it carries the sheet of all the start frames.

Three rules, and they decide everything below:

- a game is in only if it is free to download **and** free to play, on the
  rights holder's own words, pinned as a `LicenseRef` in the recipe e.g shareware/freeware/demo;
- a game counts as playable only with proof that it draws, plus working audio
  and working control;
- payloads install one at a time, because the seat's root filesystem is nearly
  full.

And one rule this round adds, about where the game data comes from:

- a recipe carries an engine, a patch or a payload whose download source the
  rights holder authorizes. where the only carrier on hand is a third party's
  mirror of a corner the owner stopped answering - id's `idstuff` tree is served
  today by `gwdg.de`, and that standing is not trusted - the engine stays the
  recipe, the data does not, and the engine's own README says where to put your
  own copy on the seat and what command runs it.

## The engines, and the games each one's README teaches

| Engine (its own recipe) | Its README's instructions add | Games the seat plays this way |
| --- | --- | --- |
| Chocolate Doom | your own Doom-family WAD, under the name the engine spells | id's DOOM shareware 1.9, Freedoom Phase 2 (the free DOOM II game), id's Heretic episode, the Hexen demo |
| FTEQW | your own Quake-family tree, named by `-basedir` and `-game` | id's Quake 1 shareware, QuakeWorld 2.30, the Quake II demo, the Quake III demo, Hexen II shareware |
| ioquake3 | your own Quake III tree, `baseq3`, `demoq3`, `missionpack`, `ta` | the Quake III Arena demo 1.11, Quake III: Team Arena |
| EDuke32 | your own Duke Nukem 3D data tree, `-j <dir>` over `DUKE3D.GRP` | Duke Nukem 3D Episode One shareware |
| OpenJazz | your own Jazz Jackrabbit data directory | Jazz Jackrabbit 1 Holiday Hare '94, Jazz Jackrabbit 2's shareware episode |
| DOSBox-X | your own DOS tree, mounted in the emulator's own `[autoexec]` | Descent 1 shareware v1.4, Descent 2 Interactive Demo v1.0 |
| Xash3D FWGS | your own GoldSrc tree plus its two aarch64 game libraries | Half-Life: Up-link, Sven Co-op 4.8 |

## The games that ship as payloads

| Game | Recipe | Licence |
| --- | --- | --- |
| LibreQuake v0.09-beta, the free Quake data | `librequake` | free: BSD-3 for the data, GPL-2 for the gamecode |
| OpenArena 0.8.8, the free Quake III-style game | `openarena` | free: GPL-2 for program and data |
| Unreal Tournament 99 GOTY, on OldUnreal's 469f arm64 engine at `/gamedata/ut` | `ut99` | free to download and play only: Epic's Terms of Service, a personal build of the owner's own copy, not redistributed |
| Unreal / Unreal Gold at `/gamedata/ugt`, the same 469f binary | `unreal-gold` | free to download and play only: Epic's Terms of Service, personal use only |

Both Unreal recipes fetch the engine from OldUnreal's own releases on
`github.com/OldUnreal`, which is where the patches keep getting updated, and the
engine's own `LICENSE.md` is the licence pin.

## The games that are streamed and never downloaded

| Game | Why |
| --- | --- |
| Dark and Darker | free to **play** only: OpenNOWLite streams it off the host's CloudMatch servers, so no byte of either game is fetched here |
| Destiny 2 | the same stream, the same reason |

Where the seat keeps the data the engines run on is one rule too: the payload data
that is on the seat lives at `/gamedata/games/<game>/`, on its 8 GB data mount, and
never in the 4.2 GB root filesystem - the shareware and demo trees, Freedoom's
WAD, the DOS releases and the GoldSrc trees all sit there, and an engine's README
names the same path its proof round used.

## The whole shelf, in tables

[games-table.md](games-table.md) carries the layer's three tables: the games the
seat plays, with each one's release, whose licence words and how it gets on the
seat; the games that are not a recipe and the reason - a grant that does not pass,
a carrier nobody answers for, or an engine that cannot draw on this seat's EGL/GLES
GPU; and the games that are free both ways but have no payload on hand yet.

## How a game gets on the seat and runs

Every recipe has two documents beside it: a `README.md` that carries only the two
things that matter - how the package is put on the seat, `dpkg -i` for a package
that lives under `/usr` and `dpkg-deb -x` for a tree that has to go on the `/gamedata`
mount, and the actual command that runs the game, with the switches that change
map, skill, renderer or window size - and a `PROOF.md` that carries the pins, the
sizes, the checksums, the measured rounds and the failure modes. An engine whose
data is not a recipe carries the instructions for putting your own data where that
engine reads it instead, and the same command line. The one running log of every
round that changed this layer is numbered and kept whole in
`recipes-games/opennow/PROOF.md`, and the recipes quote its items by number.

The full notes of this round - what measured, what stopped shipping and why - are
in [NOTES.md](NOTES.md).

## Building

```bash
bitbake chocolate-doom fteqw ioquake3 eduke32 openjazz dosbox-x xash3d-fwgs
bitbake librequake openarena ut99 unreal-gold opennow opennowlite
bitbake st-image-weston
```

A package from this layer is built as an `arm64` `.deb` in
`tmp-glibc/deploy/deb/cortexa35/` and installed on the seat; the deb is relayed
host → local → seat, because this board's build host cannot ssh to the seat.

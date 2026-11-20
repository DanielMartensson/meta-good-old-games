# The games table

The whole game shelf of this layer in one table each: what the seat plays,
what is not a recipe and why, and what is free both ways but has no payload
on hand. Every row was measured against a real round or a real dead end, and
the engine READMEs carry the command that was run. [NOTES.md](NOTES.md) is
the layer's long notes; this file is only the tables.

## The games the seat plays

| Game | Release | Licence | How it gets on the seat |
| --- | --- | --- | --- |
| id's DOOM shareware 1.9 | 1993 | id's shareware agreement, and the release note's own words | `doom1.wad` on Chocolate Doom, installed by hand from your own copy |
| Freedoom Phase 2, the free DOOM II game | 2024 release 0.13.0 | free: Freedoom's own agreement, source on GitHub | the same route, and this one has a source the layer does trust |
| id's Heretic shareware episode | 1996 | id's shareware agreement | `heretic1.wad` on Chocolate Doom, by hand |
| id/Raven's Hexen demo | 1996 | id's demo release note | `hexen.wad` on Chocolate Doom, by hand |
| id's Quake 1 shareware | 1996 | id's shareware agreement | an `id1` tree on FTEQW, by hand |
| id's QuakeWorld (2.30) | 1997 | id's shareware agreement for the data, GPL-2 for the gamecode | the `qw` pair beside that same `id1`, by hand |
| id's Quake II demo (3.14) | 1997 | id's demo release note | a `baseq2` tree on FTEQW, by hand |
| id's Quake III Arena demo (1.11) | 1999 | id's demo release note | a `demoq3` tree, on FTEQW or on ioquake3, by hand |
| id/Raven's Hexen II shareware | 1996 | Activision's limited-use sublicense, which is what the demo's own text carries | a `data1` tree on FTEQW, by hand |
| Quake III: Team Arena | 2001 | the owner's, and the user's own copy | a `ta` tree on ioquake3, by hand |
| Duke Nukem 3D Episode One (v1.3d) | 1996 | 3D Realms' shareware agreement | `DUKE3D.GRP` and its six side files under `-j` on EDuke32, by hand |
| Jazz Jackrabbit 1, Holiday Hare '94 | 1994 | Epic's own `LICENSE.DOC`, which grants the unmodified shareware package | a data directory on OpenJazz, by hand |
| Jazz Jackrabbit 2 shareware | 1998 | Apogee's shareware agreement | the same engine with its `-DENABLE_JJ2=ON`, by hand |
| Descent 1 shareware (v1.4), Descent 2 Interactive Demo (v1.0) | 1995, 1996 | Interplay's own release notes grant free distribution of both | a DOS tree mounted in DOSBox-X's `[autoexec]`, by hand |
| Half-Life: Up-link | 1999 | its own files carry a copyright line and nothing else | a GoldSrc tree plus the aarch64 gamedll pair on Xash3D FWGS, by hand |
| Sven Co-op 4.8 | 2024 | the mod's own free release, and no retail game needed | the same route, see the xash3d README |
| LibreQuake v0.09-beta | 2021 | BSD-3 + GPL-2 | a recipe, installed with `dpkg -i` |
| OpenArena 0.8.8 | 2012 | GPL-2, program and data | a recipe |
| Unreal Tournament 99 GOTY | 1999 | Epic's Terms of Service, personal use only, not redistributed | a recipe, unpacked onto `/gamedata/ut` with `dpkg-deb -x` |
| Unreal / Unreal Gold | 1998 | Epic's Terms of Service, personal use only | a recipe, on `/gamedata/ugt` |
| Dark and Darker, Destiny 2 | 2017, 2017 | free to play only, never free to download | nothing is installed: OpenNOWLite streams them |

## Not a recipe, and why in one line

| Game | Why |
| --- | --- |
| id's DOOM shareware, Heretic's episode, the Hexen demo, Quake 1, QuakeWorld, the Quake II demo, the Quake III demo, Hexen II | the grants pass - id's own agreements do hand out the shareware episodes and the demo releases, and Hexen II's own text carries Activision's limited-use sublicense - but the only carrier on hand is id's `idstuff` corner as mirrored by `gwdg.de`, which no owner answers for, so no recipe fetches it: the data is a hand install, and each engine's README carries the exact command the proof round ran |
| id's Wolfenstein 3D | id's own repository is the DOS source, the SDL port on top of it is SDL 1.2 while the seat has SDL 2, and the mirror holds no DOS shareware release either - `wolf3d/` carries only the iPhone `wolf3d_iphone_v1.x` trees, which are Objective-C/GLES code with no WAD, and `source/wolfsrc.zip` unpacks to `.C` sources only - so there is no carrier to fetch even by hand |
| Quake III: Team Arena's demo | the grant is the owner's, but there is no unpackable carrier anywhere: `teamarena/TeamArenaDemo.exe` is a 127 MB PE32 GUI installer, `teamarena/mac/tademobeta.bin` is a 126 MB MacBinary with no pk3 member in it, there is no `teamarena/linux` tree, and `map_paks/` and `misc/` hold retail add-ons - the game plays on ioquake3 only off the owner's own CD |
| Return to Castle Wolfenstein and Wolfenstein: Enemy Territory demos | both carriers are on the mirror, but the GPL sources id ships beside them carry desktop-GL-only renderers (`glBegin`/`glVertex` in `tr_main.c`, ATI's `glPatchParameteriATI` in `tr_init.c`, `GLX_EXTENSIONS` and a desktop `glext.h`) and FTEQW's supported-games list names neither game, so nothing on this seat can draw them |
| Doom 3 demo, Quake 4 demo, ETQW demo | all three carriers are on the mirror, but they are id Tech 4 and id's own sources beside them load the renderer through GLX only: the Doom 3 SDK's `renderer/qgl.h` declares `qglXChooseVisual`, `qglXCreateContext` and `qglXGetProcAddressARB` over a desktop `Display *`, its `glext.h` carries `GL_ARB_vertex_program` and `GL_ARB_fragment_program`, and there is no EGL or GLES path anywhere in the tree - the same desktop-GL wall as the row above |
| id's Doom 3, Paranoia 2: Savior | the owner's own answer: too heavy to play on this seat |
| Descent 3 | the free source release is the retail engine's, and no free payload for the game was found |
| the retail Half-Life, the full Descent, the full Unreal data | these are played with the free shareware and demo episodes, because the retail data is not free to download |
| the Half-Life 1 game libraries | built from Valve's own Half-Life 1 SDK, whose agreement is `LicenseRef-Valve-Half-Life-1-SDK` and is not a free licence, so the aarch64 `hl_arm64.so`/`client_arm64.so` pair the seat runs is built locally rather than shipped as a recipe |
| Diffusion, the Xash3D mod | the author's own answer: `Yes, it's 64-bit and OpenGL only. It needs OpenGL version 3.0`, and this seat is EGL/GLES-only with no `libGL.so.1`, so its `glsl/enhance_fp.glsl` cannot draw here; its payload also carries no grant and is unfetchable without a logged-in session (ModDB's mirror answers 403 `cf-mitigated: challenge`, itch.io's locker needs a session `file_token`), so it stays an instruction in the xash3d README and is installed nowhere |
| UT2004 | OldUnreal's 3374 arm64 patch carries the same two GL renderers and no software one, so it stops where Unreal Gold stopped, and the renderer this seat needs is the software one |
| Unreal II | OldUnreal has no Linux/arm64 branch for it at all |
| Dark and Darker, Destiny 2 | free to **play** only through an OpenNOWLite CloudMatch stream the host's servers run; never free to download, so no payload and no recipe, which is the OpenNOW/OpenNOWLite pair's job |
| UT99's and Unreal Gold's trees | kept as recipes on a different footing: Epic's Terms of Service apply, they are a personal build of the owner's own copies and are not redistributed, and the licence pin says so (`LicenseRef-Epic-Games-Unreal-Tournament-ToS`, `LicenseRef-Epic-Games-Unreal-ToS`), which is the only reason those two rows are above and this one is here |

## Not in this build yet

| Game | Release | Licence | Why it is not in yet |
| --- | --- | --- | --- |
| Jazz Jackrabbit 2's shareware episode | 1998 | Apogee's shareware agreement | the engine is one cmake switch away (`-DENABLE_JJ2=ON`), and the game is free both ways, but the 1998 shareware release is a Win32 self-extractor whose archive every unpacker tried refused, so there is no payload on hand to install, and the seat has no wine/wine64/box64/box86 to unpack one on |


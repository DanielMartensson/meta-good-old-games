# FTEQW - pins, sizes, measured rounds

All pins below are what the build that is in the image used. Numbers are kept here,
out of the README, so the README stays one screen of instructions.

## The engine

```
git://github.com/fte-team/fteqw.git  protocol=https  branch=master
SRCREV  f09ac7cccd2d713ddf881d3f1324703082c2cfe4   ("Merge pull request #458 from Xylemon/master", 27 Sep 2026)
LICENSE GPL-2.0-or-later, engine/LICENSE md5 79c2cc4426b9e6f3b9098f02eb47aac1
PV 0.0+git<SRCPV>, PKGR r0.01
```

The pin is stated rather than left at HEAD because both local patches sit against
this exact tree (`0001` touches the `#if defined(ANDROID)` at `engine/gl/glquake.h:38`,
`0003` the version gate at `plugins/quake3/clq3_ui.c:1844`), and a moved HEAD would
have to be re-checked against both. `do_compile:prepend` greps the FTE_GLES branch
back into `glquake.h` before compiling, so a hunk that silently missed stops the
build with the name of the patch instead of shipping a binary that is quietly not
the one described here.

Three patches in `files/`, all applied:

| Patch | Bytes, md5 | What it is |
| --- | --- | --- |
| `0001-use-the-gles2-headers-instead-of-the-desktop-gl-ones.patch` | 1,243 B, md5 `e498f0bc7c78991f38268c0ead90def1` | the only place in the tree that chooses desktop `GL/gl*.h` vs `GLES2/gl*.h` is that `#if defined(ANDROID)`; the patch extends it to `#if defined(ANDROID) || defined(FTE_GLES)`, and the compile then carries `-DGLESONLY -DGLSLONLY -DFTE_GLES` and takes every GL entry point at runtime through `SDL_GL_GetProcAddress`. The binary never links a desktop GL |
| `0002-quake3-real-time-accept-null.patch` | 1,791 B, md5 `6701ec38f1fe9186fdd9169304c0ef95` | OpenArena's `qagame.qvm` calls the `G_REAL_TIME` trap with a null `qtime` (`OP_CONST 0; OP_ARG 8; OP_CONST -42; OP_CALL`), which is legal - ioquake3's `VM_ArgPtr(0)` answers NULL and its traps do not validate at all, and `Com_RealTime(NULL)` returns `time(NULL)` without touching the struct. FTE's `Q3VM_GetRealtime` already has the `if (qtime)` guard, but the `VALIDATEPOINTER` macro at `plugins/quake3/svq3_game.c:837` rejects the null before the trap body runs, so the game died with `Call to game trap 41 passes invalid pointer` right after the UI VM started. The patch lets the null through and keeps the range check for everything else |
| `0003-quake3-demo-ui-api-3.patch` | 1,473 B | the Quake III Arena 1.11 demo ships `vm/ui.qvm` built against UI API 3. id only ever added UI traps at the end of that enum after the release, so API 3 runs fine against FTE's API 4/6 trap table - it is the same table ioquake3 exposes to old mods. FTE's `UI_Start` rejected anything but 4 or 6, which killed the demo menu and left the client with a spawned-but-empty listen server. Accept 3 as well. (`vm/cgame.qvm` is API 3 too but `CG_Start` has no version check, so only the UI needed relaxing.) |

Build shape: no configure script. `FTE_CONFIG=fteqw` (the makefile's own default at
`engine/Makefile:128`) picks the config header; a leaner one cannot be used -
`config_minimal.h` leaves `RTLIGHTS` off, so `gl_shadow.c`'s body compiles to nothing
while `gl/gl_rlight.c:756` still references the cvars it defines outside any guard,
and the link stops with `undefined reference to 'r_shadow_realtime_world'` (and its
siblings); `config_fteqw_noweb.h` leaves `PACKAGEMANAGER` on without `WEBCLIENT`, and
`engine/client/m_download.c` then declares a struct without the members it uses
(`'package_t' has no member named 'curdownload'`). `FTE_TARGET=SDL`, not `SDL2`:
line 727 matches `SDL2` exactly and swaps the dynamic link lines for
`libs-$(ARCH)/*.a` static archives only `make makelibs` builds. `gl-rel`, not
`glcl-rel`: `glcl-tmp` adds `-DCLIENTONLY`, which makes `quakedef.h:412` take the
`#define isDedicated false` branch, so `sys_sdl.c:1468` reads `false = true;` and
gcc stops it (`lvalue required as left operand of assignment`); `gl-rel` runs
`_clsv-rel` with empty `JOINT_CFLAGS` at `engine/Makefile:667`, so both `HAVE_CLIENT`
and `HAVE_SERVER` come out and `isDedicated` is the real variable `sys_sdl.c:29`
defines - which is also what the seat can use: the same binary plays and can run
`-dedicated`. `PKGCONFIG` stays at the makefile's own cross-compiling answer
(`/bin/false`), so the SDL probe is answered by the generated wrapper
`${WORKDIR}/fteqw-sdl2-config` (its `--cflags` answer is not empty: FTEQW includes
`<SDL.h>` while SDL2's headers live in `recipe-sysroot/usr/include/SDL2/`), and the
library sets are handed over by hand: `IMAGELDFLAGS="-lpng -ljpeg -lz"`,
`OGGVORBISLDFLAGS="-lvorbisfile -lvorbis -logg"`, `FREETYPE_LDFLAGS="-lfreetype"`.
`STRIP` is OE's cross strip, which is what makes the makefile take its `_out-rel`
branch at `engine/Makefile:2077` (GNU `strip` does not read an aarch64 object), and
is why `INSANE_SKIP:${PN} += "already-stripped"`.

Installed binary: `/usr/bin/fteqw-gles` 5,493,000 B, md5 `3328dc12cf97264b24f86da0342916d4`
unchanged since the 0002 patch landed. This round's deb
`fteqw_0.0+git0+f09ac7cccd-r0.01_arm64.deb`, 1,920,812 B, md5 `29a198a9f8bf8653d4f2540c8d49cd52`,
and `dpkg -i` answered `Unpacking fteqw (...) over (...)`: the launcher moved, the
binary kept its md5. `ldd -d` on the installed binary answers no `libGL` and no
`libGLEShim` in `NEEDED` - the client speaks only the seat's
Vivante `GCNANOULTRA31_VIP2` V6.4.21 driver, and the seat has no desktop GL at all
(`EGL_CLIENT_APIS` is `OpenGL_ES` and OpenVG only, `eglBindAPI(EGL_OPENGL_API)`
answers `EGL_BAD_PARAMETER`).

## Why this engine, and what was rejected

- **QuakeSpasm** died on the board itself: its renderer is desktop fixed-function GL,
  and this seat has no desktop GL at all. FTEQW is the engine that can be compiled
  GLES-only, so it is what plays.
- **A Quake II game** was considered and skipped for free-of-charge reasons: FTEQW
  carries the q2 gamecode hook, but a Quake II game needs id's paks and no fully free
  Q2 asset set exists (the Yamagi downloads are client source plus CTF/Reckoning/Ground
  Zero addon source packages whose data still needs the proprietary paks), so there
  was nothing free-to-use AND free-to-download to pair it with. OpenArena is free both
  ways and stands in for that game.
- **`jdolan/quetoo-data`** was rejected the same way: its licence (CC-BY-SA-4.0) would
  have been fine, but the payload is Quetoo's, not FTEQW's. The `v1.0.73` data is a
  1.15 GB zip (1.09 GB in a deb) against the seat's free rootfs, and the content does
  not open even with room: `maps/<name>.mf` plus `maps/<name>.bsp`, whose BSP header
  is `IBSP` version 84 - not one of the magics this engine registers (`47` RTCW, `46`
  Quake III, `38` Quake II, `69` Quake2World in `engine/gl/gl_model.c`), so the seat
  answers `Unrecognised model format IBSP` then `Mod_NumForName: maps/rage.bsp not
  found`; the pk3 is not a zip FTEQW reads either (`appears to be missing its central
  directory`, `engine/common/fs_zip.c`), and the assets are `.mat`/`.mf` with no
  Quake III `.shader` and no `qagame.qvm`, so there is no gamecode to run even after
  a conversion. Nothing from it is installed, and this layer does not build the Quetoo
  engine.

## The fps ladder (LibreQuake `lqdm3`, measured 2026-11-11, over the steady 20 s)

fps = `wl_surface@19.commit()` lines of the game's own surface in a `WAYLAND_DEBUG=1`
log; the `damage()` column is the surface the seat really allocated. A 16:9 size the
seat does not answer is padded up to the next 4:3 one.

| Ask | Surface | steady fps |
| --- | --- | --- |
| `vid_fullscreen 2` (1920x1080) | 1920x1080 | 14.0 |
| 1600x900 | 1600x900 | 21.6 |
| 1280x720 | 1280x720 | 32.5, 32.6, 32.8 |
| 1152x648 | 1152x864 | 29.6 - the pad costs more than the round size above it |
| **1024x576 (shipped)** | 1024x768 | 37.0, 37.0, 37.0 |
| 960x540 | 1024x768 | 36.9, 37.2 - same surface, so the wider ask is better |
| 896x504 | 1024x768 | 36.9 - same surface again, a free size to ask but no free fps |
| 768x432 | 800x600 | 56.9, 57.1 |
| 640x360 | 640x480 | 63.0, 63.7, 64.3 |
| `vid_fullscreen 0` at 640x480 | 640x480 | 64.1 - costs the same, leaves a centred window |

What does **not** help: `r_vertexlight 1`, `r_dynamic 0`, `gl_picmip 2`,
`GL_LINEAR_MIPMAP_NEAREST`, `r_fastsky 1`, `gl_texture_anisotropy 0`, `r_warp 1` each
moved `lqdm3` by under 2 fps at 1080p on their own (14.0 -> 15.3-16.4); the only two
that buy anything at all do it by not drawing the lightmap (`r_drawflat 1` at 24.6,
`r_lightmap 1` at 28.2). The seat's own repaint cadence puts a ceiling of about 64 fps
on any surface, which is why nothing below 640x360 buys anything.

## Measured, per game tree on this engine

Rounds at the seat's 1920x1080; screenshots through `XDG_PICTURES_DIR=<dir> weston-screenshooter`;
colour/`non-black`/pair numbers from `tools/shotstat.py` and `tools/imgdiff.py` on the build
host (the seat has no PIL). Threshold in diff columns: max-channel difference of 8.

### LibreQuake (the packaged tree, launcher defaults only)

`/usr/bin/fteqw +wait 3 +map lqdm3`. Console: `current map : lqdm3 (Hyperborea)`,
`entities : 60/60/131072 (mem: 3.2%) models 50/16384 sounds 79/4096`, `Setting mode
1024*576 32bpp OpenGLES2+`, `wl_surface@19.damage(0, 0, 1024, 768)`,
`xdg_toplevel@26.set_fullscreen(wl_output@13)`, `"LQ Player joined the server"`,
`lock_pointer x2`, `relative_pointer_v1 relative_motion x4`, `Initing default SDL audio device.`
**37.0 fps steady** over the measured 20 s; 85 % of one core, rss 146,984 kB at the
`/proc` samples, `ps` beside it `VSZ 1041076 KB RSS 147104 KB %CPU 94.0`;
`free -m` while it ran: 1768 total, 464 used, 1304 available.

### id's Quake 1 shareware episode (`id1` tree, hand tree at 19 MB)

Round of 2026-11-15, `/gamedata/installs/drawproof-m1.sh`, console
`/tmp/quake1-demo-console`, shots `/gamedata/installs/shots/m1/quake1-demo`.
`+wait 3 +map e1m1`. Console 851 lines: `Setting mode 1024*576 32bpp OpenGLES2+`,
`GL_VENDOR: Vivante Corporation` / `GL_RENDERER: Vivante GCNANOULTRA31_VIP2` /
`GL_VERSION: OpenGL ES 3.1 V6.4.21.1.1058597`, `------- Quake Initialized -------`,
`client Player connected`, the map's own title line `the Slipgate Complex`, `Player
entered the game`; the only refusals are `couldn't exec config.cfg`, `couldn't exec
fte.cfg`, `couldn't exec autoexec.cfg` - the payload ships no config tree. Cost at
the 30 s mark: RSS 123,756 kB / 69.5 % of one core.

| Shot | colours | non-black of 2,073,600 | row lum | mid-row jumps | md5 |
| --- | --- | --- | --- | --- | --- |
| 1 idle | 25,830 | 2,071,757 | 12.2-37.2 | 12 | `dd23e524910527df08bd9d77026ab9a7` |
| 2 idle +10 s | 25,830 | 2,071,757 | 12.2-37.2 | 12 | same |
| 3 after sweep | 23,160 | 2,073,598 | 15.3-27.4 | 7 | `9cadc93f6dd33665f60f78d6997d96c1` |
| 4 after sweep | 23,160 | 2,073,598 | 15.3-27.4 | 7 | same |

| Pair | identical | >8 | columns / rows moved |
| --- | --- | --- | --- |
| idle 1 vs 2 | 2,073,600 (100.0 %) | 0 | 0 / 1,920 and 0 / 1,080 |
| sweep 2 vs 3 | 32,079 (1.5 %) | 1,708,803 (82.4 %) | 726 / 1,920 and 286 / 1,080 |
| 3 vs 4 | 2,073,600 (100.0 %) | 0 | 0 and 0 |

Both shots are a rendered `e1m1`: shot 1 looks down the riveted metal corridor with the
laser gun raised and the status bar reading `0 [face] 100 [] 25`; shot 3 is the same
room pitched down onto the floor grate with two full-bright red light panels at the
top edge, which is why that shot carries 38,718 pixels of exactly `(255, 0, 0)`. The
pitch snaps because the sweep is a ydotool absolute jump, whose whole distance arrives
as one relative delta in SDL2's relative mouse mode - the turn is the proof, its size
is the input method's. Audio: `Initing default SDL audio device.` printed, and
`/proc/<pid>/maps` mapped `/usr/lib/libSDL2-2.0.so.0.3000.1` **and**
`/usr/lib/libasound.so.2.0.0` - no `libSDL2_mixer`, because FTE's Quake 1 sound path
is SDL2's own, unlike Chocolate Doom's.

This payload's own fps ladder over `e1m1`: **61.1 fps at 1024x576** (rss 131,364 kB /
vsz 950,300 kB, 83 % of one core), 32.8 at 1280x720, 57.1 at 768x432, 63.7 at 640x360,
14.0 over the whole 1920x1080.

### id's Quake II demo (`baseq2` tree + loose `players/` + `game.so`)

What the packaged payload held: `baseq2/pak0.pak` 49,951,322 B; `baseq2/players/` 59
files, 2,193,468 B loose (34 in `players/male/`, 25 in `players/female/`, the two
`tris.md2` bodies at 278,592 and 275,632 B, one `weapon.md2` at 38,912 B each with
`weapon.pcx` at 9,089); `baseq2/game.so` 814,088 B, id's GPLv2 game module compiled
for arm64 from the 48 `.c` files of `game/` in id's source release at commit
`372afde4`, plus `files/0001-game-static-declarations.patch`: `game/g_local.h:461`
declares the three armor indexes `extern` while `g_items.c:43-45` define the same
names `static`; VC++ 6 lets the earlier extern slide, gcc 13 calls the definition a
constraint violation and refuses the file, and none of the other 47 files names those
indexes, so dropping the two no-header-mentioned ones is a no-op on both compilers.
The loose `players/` tree is not a quirk of the packer: id's own demo build of the
engine (`Quake-2-master`'s `qcommon/files.c`, the `#else // this is just for demos
to prevent add on hacking` branch) opens `config.cfg` and everything under `players/`
straight from the gamedir and never from inside a pak, and FTEQW's Quake II client
(`clq2_ents.c:2370`, `cl_parse.c:5673`) looks the same way, so without those files the
players never appear at all. Map names come out of the pak's own index (56-byte
name/offset/length entries read on the seat): `maps/demo1.bsp`, `maps/demo2.bsp`,
`maps/demo3.bsp` and nothing else, so asking for the registered release's start map
is refused with `Can't find "maps/quake1.bsp"`.

Round of 2026-11-15, `/gamedata/installs/drawproof-q2.sh`, console `/tmp/q2-console`,
shots `/gamedata/installs/shots/m1/quake2-demo`, `+map demo1`. Console 893 lines:
`Setting mode 1024*576 16bpp OpenGLES2+`, `GL_RENDERER: Vivante GCNANOULTRA31_VIP2`,
`------- Quake II Initialized -------`, `==== InitGame ====`, `41 entities inhibited`,
`1 teams with 2 entities`, `client Player connected`, title line `demo1`. Cost at the
30 s mark: RSS 164,380 kB / 89.6 % of one core.

| Shot | colours | non-black | row lum | mid-row jumps | md5 |
| --- | --- | --- | --- | --- | --- |
| 1 idle | 17,416 | 2,067,312 | 10.0-29.0 | 17 | `b7d042c479b85f25d203ed6bce78d4bb` |
| 2 idle +9 s | 18,348 | 2,070,391 | 9.4-29.7 | 17 | `7081d505af3300555f9be744f360f897` |
| 3 after sweep | 12,609 | 2,071,075 | 18.6-34.2 | 9 | `d27e02482f6c814515cc1ccc43a77e8a` |
| 4 after sweep | 12,770 | 2,069,576 | 19.8-34.2 | 9 | `66a3236e842d1266bc6530100b32d337` |

| Pair | identical | >8 | columns / rows moved |
| --- | --- | --- | --- |
| idle 1 vs 2 | 1,844,021 (88.9 %) | 180,395 (8.7 %) | **0 and 0** |
| sweep 2 vs 3 | 21,566 (1.0 %) | 1,598,397 (77.1 %) | 1,242 / 1,920 and 597 / 1,080 |
| 3 vs 4 | 1,869,421 (90.2 %) | 165,575 (8.0 %) | 0 and 0 |

The pattern that game wants: ~9 % of pixels changed with **not one column or row of
the coarse shape moved** is an animated light in a still view; the sweep pair turning
1,242 of 1,920 columns is the camera. `game.so` is in this proof the way the payload
needs it: the round reached `==== InitGame ====` and `41 entities inhibited`, which
only the Quake II gamecode can answer - a round that had not loaded it stops before
those lines. The red console line `Unable to load players/male/w_blaster.md2` belongs
to the full release: that name is in neither the loose tree nor `pak0.pak`, and the
first-person blaster the shots show drew anyway. 61.0 fps steady at 1024x576 in the
launcher-defaults ladder (32.8 at 1280x720, 14.0 at 1920x1080).

### QuakeWorld (`qw` tree: `qwprogs.dat` 197,684 B + `files.dat` 6,076 B + `id1` episode)

Round of 2026-11-18, `/gamedata/installs/drawproof-qw.sh e1m1`, console
`/gamedata/logs/quakeworld-console`, shots `/gamedata/installs/shots/qw/`. 21 console
lines: `Engine Version: FTE git-6733-f09ac7ccc-dirty`, `Setting mode 1024*576 32bpp
OpenGLES2+`, the three GL lines above, `OpenGLES2+ renderer initialized`, `-------
Quake Initialized -------`, `client Player connected`, `the Slipgate Complex`, `Player
entered the game`; the three expected `couldn't exec` refusals only. Cost: **rss
127,912 kB at 70.4 %** on the 19 MB tree, **rss 131,208 kB at 70.3 %** on the 212K
symlinked tree (the `id1` -> episode link is walked, not trusted: the 212K tree whose
`id1` is a symlink answered the same 21 console lines and the same four shot hashes
as the same tree with the pak copied into it at 19 MB, which is why one payload can
link at the other instead of holding the 18,689,235-byte pak twice). The packaged
tree then proved the same numbers again through the launcher the old recipe shipped
(`-basedir /usr/share/quakeworld`, 212K plus the symlink): same 21 lines, same maps,
rss 129,244 kB at 70.2 %, four shots **byte-identical** - md5s `94d1dd87af9d8a1046bf763c2cfa42e6`
for the idle pair and `9cadc93f6dd33665f60f78d6997d96c1` for the sweep pair, so every
number in the table below is the packaged tree's own.

| Shot | colours | non-black | row lum | mid-row jumps | md5 |
| --- | --- | --- | --- | --- | --- |
| 1 idle | 26,008 | 2,071,757 | 12.2-37.2 | 12 | `94d1dd87af9d8a1046bf763c2cfa42e6` |
| 2 idle +10 s | 26,008 | 2,071,757 | 12.2-37.2 | 12 | same |
| 3 after sweep | 23,160 | 2,073,598 | 15.3-27.4 | 7 | `9cadc93f6dd33665f60f78d6997d96c1` |
| 4 after sweep | 23,160 | 2,073,598 | 15.3-27.4 | 7 | same |

| Pair | identical | >8 | columns / rows moved |
| --- | --- | --- | --- |
| idle 1 vs 2 | 2,073,600 (100.0 %) | 0 | 0 and 0 |
| sweep 2 vs 3 | 32,079 (1.5 %) | 1,708,579 (82.4 %; 426,171 (20.6 %) by >32) | 739 / 1,920 and 287 / 1,080 |
| 3 vs 4 | 2,073,600 (100.0 %) | 0 | 0 and 0 |

No fps number here and that is the engine's answer, not a gap: `+stat_start`/`+stat_stop`
say `Unknown command "stat_start"` in `-game qw`, and `-set cl_stats 3 +status +cl_stats`
answers only `"cl_stats" is "3" (default)` - its readout is drawn on the HUD, not printed
to the console - so this round carries cost instead of a framerate ladder, and the fps
tier is the one measured for the same client and the same `e1m1` in the Quake 1 round
above.

Why `e1m1` is the proof map and not a deathmatch map: in `-game qw` this payload answers
`+map e1m1` and `+map start` with `client Player connected` and `Player entered the game`;
`+map dm2`, `+map dm6` and `+map q1q3dm6` print nothing at all and the screen asks
`DOWNLOAD MAP DM3 FROM HTTPS://MAPS.QUAKEWORLD.NU/ALL/?`, because FTE's QuakeWorld attract
loop wants `dm3`, which is not in the shareware episode's pak; the mirror's `quakeworld/maps/`
only carries two third-party map zips (`deathpack1u.zip`, `q1edge.zip`) whose licence this
audit has no grant for. Two more things are left out for reasons that are not size: **no
`id1/progs.dat`** - FTEQW picks the program file by depth (`pr_cmds.c:1945-1963`), whose
own FIXME says `id1/progs.dat vs qw/qwprogs.dat - these should be considered to have the
same priority`, so a Quake 1 progs at the same depth would take over QW's, and this payload
ships QW's only; **no `idctf1.bsp`** - the 791,123 B `unsup/idctf1.zip` on the mirror carries
Tim Willits' CTF conversion at 1,773,560 B and QW's own `qwprogs.dat` answers
`Couldn't find spawn function for item_flag_team1` for its flag entities, since CTF item
spawns belong to a CTF mod's progs.

The QW program file is a GPL half: `progs.src` (180 B, first line `./qwprogs.dat`) is id's
own build list of fourteen `.qc` sources, and every one carries id's GPL-2 header
(`defs.qc` at 25,010 B opens `Copyright (C) 1996-1997 Id Software, Inc. ... This program is
free software; you can redistribute it and/or modify it under the terms of the GNU General
Public License ... version 2`), with `gnu.txt` 14,969 B md5 `8b2da277227a33a539463fe8bcfa0b11`
as the text that covers it. id's own note `q1source.txt` (2,960 B) says `The code is all
licensed under the terms of the GPL (gnu public license)` - and its other sentence is why
no pak of QW's own exists: `All of the Quake data files remain copyrighted and licensed
under the original terms, so you cannot redistribute data from the original game, but if
you do a true total conversion, you can create a standalone game based on this code.` The
two `files.dat` on hand are 6,076 B (from `q1source.zip`, whose zip's own `SLICNSE.TXT` is
byte-identical to the 10,036 B copy decoded out of id's shareware install parts) and 5,764
B (from `qw-qc.tar.gz` 124,623 B); the `qwprogs.dat` is byte-identical either way, and
swapping the smaller `files.dat` into the proven tree gave byte-identical shot hashes and
an identical console, so the size makes no difference.

### id/Raven's Hexen II demo (`data1` tree, one pak)

`data1/pak0.pak` 23,537,707 B, 701 entries: `maps/demo1.bsp` 2,440,396 B (printed by the
engine as **Blackmarsh**), `maps/demo2.bsp` 2,165,208 B, the one deathmatch arena
`maps/ravdm1.bsp` 1,024,960 B, plus the textures, models, sprites, sounds, console scripts
and the `progs.dat` the demo runs its game logic out of. No code of its own needed - FTEQW
speaks the Hexen II protocol natively (unlike the Quake II side, whose gamecode this layer
compiles). The archive on the mirror is a Windows self-extractor, 12,244,480 B, whose zip
starts 25,649 B in and holds 132 entries / 27,482,419 unpacked bytes; the rest is the 1998
InstallShield setup and Windows help files. Its `HEXEN II SUBLICENSE.doc` is id/Raven's own
"ACTIVISION SOFTWARE SUBLICENSE AGREEMENT / LIMITED USE SUBLICENSE" (`install and use one
copy ... solely and exclusively for your own personal entertainment`), pinned upstream as
`LicenseRef-Activision-LIMITED-USE-SUBLICENSE`.

Round of 2026-11-15, `/gamedata/installs/drawproof-m1.sh`, console `/tmp/hexen2-demo-console`,
shots `/gamedata/installs/shots/m1/hexen2-demo`, `+map demo1`. 696 console lines: `-------
Hexen II Initialized -------`, `client Player connected`, title line `Blackmarsh`, `Player
joined the game`; cost at the 30 s mark RSS 196,368 kB / 69.7 % of one core.

| Shot | colours | non-black | row lum | mid-row jumps | md5 |
| --- | --- | --- | --- | --- | --- |
| 1 idle | 61,992 | 2,043,309 | 16.2-47.0 | 5 | `dfcdaa04f36bb64e20c2ba62d3a1735b` |
| 2 idle +11 s | 61,773 | 2,043,219 | 16.2-47.0 | 5 | `52cbbc212277336f5edb521305cd386a` |
| 3 after sweep | 29,955 | 2,068,061 | 15.5-46.7 | 8 | `87a3e392aa5adb52aff8a0d48dba3104` |
| 4 after sweep | 29,955 | 2,068,061 | 15.5-46.7 | 8 | bit-identical to 3 |

| Pair | identical | >8 | columns / rows moved |
| --- | --- | --- | --- |
| idle 1 vs 2 | 2,044,923 (98.6 %) | 24,409 (1.2 %) | **0 and 0** |
| sweep 2 vs 3 | 109,972 (5.3 %) | 1,714,144 (82.7 %) | 934 / 1,920 and 305 / 1,080 |
| 3 vs 4 | 2,073,600 (100.0 %) | 0 | 0 and 0 |

98.6 % the same picture with not one column or row of the coarse shape moved is animated
texture (Hexen II animates skies and liquids) in a still view; 61,992 distinct colours is
the richest shot set of this layer's Quake-protocol payloads; the sweep pair's 934 columns
are the view turning and its 29,955 colours are the same Blackmarsh from another angle.
This console also carries the one ALSA line this layer's rounds have ever printed:
`ALSA lib ... pcm.c:8740:(snd_pcm_recover) underrun occurred` - alsa-lib **opened** the
sink and started playing, the seat's CPU was starved while the game drew, and alsa-lib
recovered instead of refusing. That is the opposite of the `Device or resource busy`
refusal, which never opens the device at all. 57.0 fps steady at 1024x576 (14.0 at
1920x1080).

### id's Quake III Arena demo through this engine (the FTE route)

`demoq3/pak0.pk3` 46,853,694 B, 1,387 files: the demo's arenas `q3dm1`, `q3dm7`,
`q3dm17`, `q3tourney2` and their models, textures, sounds and console scripts. Carrier:
`idstuff/quake3/linux/linuxq3ademo-1.11-6.x86.gz.sh`, a gzip inside a shell script whose
payload is a zip of 1,387 entries / 79,243,450 unpacked bytes (`unpack=false` because
bitbake's unpacker works off the extension and knows no `.sh`). Its `Help/Q3A_EULA.txt`
is id's "LIMITED USE SOFTWARE DEMO LICENSE AGREEMENT".

| Run | Measurement |
| --- | --- |
| FTE route, defaults, `q3dm7` (round of 2026-11-13) | 48.2 fps sustained at 1024x576 (rss 136,604 kB), 14.0 fps at 1920x1080, **every shot black** |
| FTE route again 2026-11-18, no argument at all | **Draws**: pair under `gallery/quake3-demo` measured 189,557 and 189,772 distinct colours at 1920x1080 with 2,072,742 and 2,073,401 of 2,073,600 non-black, 99 and 90 distinct row lums, and only 83.0 % of the pair identical, the arena's own bots and item respawns being the only things that move. Console: `Connecting to 127.0.0.1:27960...`, `Connected to 127.0.0.1:27960 (plain-text).`, `Player entered the game`, mission `q3dm7`. A map name passed as an argument lands as a second `+connect` argument, because the launcher appends `"$@"` after `+connect "127.0.0.1:<port>"`, so the client prints `usage: connect <server>` instead of connecting - which is why that round passed nothing |
| ioquake3 on the same payload | 136,766 / 136,265 / 86,041 colours, 2,070,123-2,073,582 non-black, sweep 92.7 %, 1,691/1,920 columns (round of 2026-11-15; full record in `../ioquake3/PROOF.md`) |
| live playtest 2026-11-15, `/usr/bin/ioquake3 +map q3dm7` started AT the seat | Alive, owner at the seat's keyboard and mouse: audio, graphics and control confirmed playing 100%. One shot measured on the way: **320,386 distinct colours** at 1920x1080, 2,071,397/2,073,600 non-black, 120 distinct row lums - the richest draw of the layer, a lit arena plus HUD. Only refusals are the expected ones: `couldn't exec autoexec.cfg` (this demo ships no cfg) and four `Failed to load sound` lines for `assist.wav`, `defense.wav`, `hgrenb1a`/`hgrenb2a` - team-feedback and grenade-pickup sounds the full release ships and this demo does not, verified against the payload: the 1,387-file pak holds 249 wavs and zero matches for `assist`, `defen` or `vo_` |

Which route plays this game is stated once: the game is played through
`/usr/bin/ioquake3`. The FTE route is kept because FTEQW ships a row of its own game
table for exactly this payload, and the round of 2026-11-18 found it drawing at its
own defaults, so no cvar was needed to make it draw; the cause of the black shots of
2026-11-13 is not reproducible from that round, so it stays unnamed, and only the fact
that the route as shipped now draws is claimed.

## The Quake 1 shareware archive, if a hand install goes through id's own release

The hand tree above is the owner's copy; if one goes through id's own shareware release
instead, note what it takes to open: `idstuff/quake/quake106.zip` (9,094,045 B, sha256
`ec6c9d34b1ae0252ac0066045b6611a7919c2a0d78a3a66d9387a8f597553239`) is an install part,
not a game - its members are `resource.1` at 9,086,574 B (md5 `994d127ea51ba4bae1030cb7242806aa`),
`install.bat` 311, `deice.exe` 36,064 and `resource.dat` 196, and `resource.dat` is DEICE's
own index `SIZE=9086574 EXPSIZE=19300000 PATH=\QUAKE_SW`. The game is two containers deeper:
the 1,945 bytes at the front of `resource.1` are a DOS stub, and from 1945 the bytes are a
plain lzh archive whose 16 members are all `-lh5-` and expand to 19,653,186 bytes, with id's
level-0 headers (22 bytes before the name plus the name and a trailing checksum byte, the
header's own 16-bit checksum outside that count), so a member starts at `header + hsize + 2`
and the walk starts at 1945 (`CWSDPMI.EXE`, header 33, 16,761 packed, 25,920 unpacked) and
ends at 9,054,698 (`SLICNSE.TXT`, header 33, 4,040 packed, 10,036 unpacked). The game is
`ID1\PAK0.PAK` at header 29,005, 8,612,632 packed to **18,689,235** unpacked, CRC `00004EAC`,
md5 `5906e5998fc3d896ddaf5e6a62e03abb`. Neither `unzip` nor tar opens that, and the build
sandbox has no 7z, no `lha`, no `bsdtar` and no `py7zr`, so the payload recipe that used to
do this carried a decoder: a port of libarchive's `archive_read_support_format_lha.c` in the
three parts needed (`lzh_read_pt_bitlen` for the bit-length table in lha's fixed scheme,
`lzh_make_huffman_table` for canonical codes, `lzh_decode` for literals and back-references
over the 8 KiB history pre-filled with `0x20`s). It was checked member for member against
7-Zip 23.01's own extraction before it went into a recipe:

| Member | Bytes | md5 |
| --- | --- | --- |
| `HELP.TXT` | 5,404 | `01cda96876b1abc2e0d2d369a8e8e547` |
| `LICINFO.TXT` | 9,311 | `2b63fd0623b86b22ef915c7c0fca8ff2` |
| `ORDER.TXT` | 3,869 | `f9bb104c9d34b0f9feb9716ea67e665d` |
| `README.TXT` | 19,087 | `e64696a79d7de813c15942cc11e88d86` |
| `READV106.TXT` | 8,774 | `a819ce048485c688b62a68f2abfa2e87` |
| `SLICNSE.TXT` | 10,036 | `5450cdeca31e270a64fc64e1b52a25a6` |
| `ID1\PAK0.PAK` | 18,689,235 | `5906e5998fc3d896ddaf5e6a62e03abb` |

That md5 is id's own - the same bytes are mirrored raw as `PAK0.PAK` at the Internet
Archive's sibling `quake-shareware-pak` item, which is the cross-check on the md5 and
not the source. Episode 1 only: the registered and ported episodes are not in this
archive and are not free to use.

## Failure modes

| Symptom | Cause |
| --- | --- |
| nothing draws at all | left alone the Vivante hands back an "OpenGL ES-CM 1.1" context and the renderer refuses it; `vid_gl_context_es 1` is what asks for the ES 3.1 one |
| `Quake Error: Out of memory` before a map appears, right after `==== InitGame ====` | `pr_ssqc_memsize`/`pr_csqc_memsize` left at `-1`: that value reaches `PR_Configure -> PRAddressableFlush` (`qclib/initlib.c:532`) as `0xffffffff`, is clamped to `0x80000000` = 2 GB (`initlib.c:604`) and the `malloc` fails on this 1,768 MB seat. The two are different arenas; the chained-`+wait` OOM below is the client-side one |
| an argument list that chains 900-1200 `+wait`s dies at `Out of memory` in `qclib/initlib.c:532` | `wait` delays ONE frame and ignores its argument (`cmd.c:206 Cmd_Wait_f` reads the number but waits a single `rframe`), so chained waits do not hold the game open; the arena is re-malloc'd whole every time the queued-command list grows, so the fix is `pr_csqc_memsize 64m` in the launcher rather than a longer queue. Short queues load the map 8/8 |
| a second `-basedir`/`-game` is ignored, and the game opens LibreQuake's tree | FTE answers the **first** match (`COM_CheckParm` -> `COM_CheckNextParm(parm, 0)` at `common.c:5451`), so every game of this family needs its pair **first** on the engine's command line, which is why `/usr/bin/fteqw` hard-codes the LibreQuake pair |
| a `baseq2` tree opened as Quake 1 | same first-match rule picks the protocol; a `-quake` earlier on the line hijacks the tree |
| FTE refuses a gamedir name | it refuses names starting `lib`/`bin`/`doc`/`help`/`downloads`, and `-updatesrc ""` has to stay on the command line |
| a double `+map` (a launcher default map plus the user's own) or `-set sv_listen_q3 1` in the Quake III gamecode segfaults the client, `rc=139` | so no default map in a Q3 launcher; the map comes in as arguments - the rest of the Q3 gamecode's failure modes are in `../openarena/PROOF.md` |
| `-dedicated` client will not host | `CL_Connect_f` always calls `CL_Disconnect_f` first, which tears an in-process listen server down; a Quake III client cannot host, so the arena is a separate `-dedicated` process the player connects to |
| a server left over from a previous run moves the new one to the next port and the client cannot find it | FTE only stops on `-9`; kill the old pid first (the old launcher had a `trap` for it) |
| a motionless player drops back into its menu, and the menu reads 160 fps instead of the arena's 48 | `timeout`/`cl_timeout` are inactivity timers with 65/60 s defaults; raise them |
| `-basedir /usr/share -game lq1` with a `/usr/share/lq1` symlink | an earlier round's hack; the launcher mounts the payload directly (`-basedir /usr/share/librequake -game id1`) and no symlink is anywhere |
| `ALSA: Couldn't open audio device: Device or resource busy` | not this engine's doing: the seat's ALSA sink serves one process at a time, so proof rounds kill every other game first (an UT99 SoftDrv client of a previous round was still alive in the ioquake3 round of 2026-11-15). An `snd_pcm_recover underrun occurred` line is different and harmless: the sink opened, played, underflowed and alsa-lib recovered |
| `weston-screenshooter` answers "unauthorized" | the weston compositor on this seat never grants the screenshooter authority (`weston_compositor_add_screenshot_authority` is never called). It answered here only after the seat's own configuration change; the picture proofs this layer relies on are FTE's own `+screenshot` files (`~/.local/share/<game>/<gamedir>/screenshots/fte-<stamp>-0.png`) plus one shot of a running game, which is as far as the board gets |
| `-set con_logfile <name>` creates a 0-byte file | this build's console text does not go there, so console answers were read from a pty; `screenshot` typed into that pty answers `Unknown command "screenshot"` even though `cl_screen.c:3671` registers it - it belongs to the GUI console, and the C-side stdin console does not carry it. `+screenshot <name> +quit` on the command line works but only ever catches the menu: `+wait` chains that would advance real frames instead of the queue end in a timeout with no file (400 chained `+wait`s answered `rc=124`) |
| `ssh -tt` needed to keep FTE alive | FTE exits when its console queue finishes because it was not started from a tty; typed console commands take NO `+` prefix, only command-line queued ones do |
| an fps count of 12,284 fps | an earlier round counted the timestamps of the frame-commit log as if they were seconds; they are milliseconds |
| busybox `top` shows the process as `fteqw-g+` and a `ps` filter on the game never matches | busybox truncates the name; use `ps -o ... -p <pid>` off `/proc` |
| OpenArena dies with `Call to game trap 41 passes invalid pointer` | that is the 0002 patch's null `G_REAL_TIME` call; the patch is in this recipe, so the image on the seat carries the fix |
| the Quake III demo's menu will not come up, `User-Interface VM uses incompatible API version (3)` | the demo's `vm/ui.qvm` is API 3; that is the 0003 patch's relaxation |

## Files

`files/` carries the three patches above; `fteqw_git.bb` builds the client and writes
`/usr/bin/fteqw`. Payload recipes of this family are gone from the layer - see the
hand-install commands in README.md.

## The debs this branch builds (built 2026-11-20 from this tree)

The whole layer was built again from this restructured tree at its own path: `bitbake chocolate-doom fteqw ioquake3 eduke32 dosbox-x openjazz xash3d-fwgs librequake openarena opennowlite` ran green on the build host's `stm32mp25-watermelon-wine-1a` machine, and this recipe's own task chain re-ran inside that run, so the numbers below are this tree's and not the ones an earlier round happened to record. `fteqw` leaves the build as `0.0+git0+f09ac7cccd-r0.01`:

| deb | size | md5 |
| --- | --- | --- |
| `fteqw_0.0+git0+f09ac7cccd-r0.01_arm64.deb` | 1,921,108 B | `5addfa36dc61f90e504c2225b3637127` |
| `fteqw-dbg_0.0+git0+f09ac7cccd-r0.01_arm64.deb` | 1,484 B | `80e76a163a4a61bdc4ecf9c6aa91d007` |
| `fteqw-dev_0.0+git0+f09ac7cccd-r0.01_arm64.deb` | 1,536 B | `cfd7480bd18de20690ab25cfaa2c8582` |

The base deb installs 2 files: `/usr/bin/fteqw-gles`, the engine, at 5,493,000 B md5 `884516b44ae03a6cc436b1aa0a881c43`, and `/usr/bin/fteqw`, the launcher, at 5,493,000 B md5 `884516b44ae03a6cc436b1aa0a881c43`, which is byte-for-byte the launcher the earlier round recorded on the seat (same md5 `1286cd7dcdda11ed182781bbd1ffc814`), so only the engine's own build moved here. The deb grew from 1,920,812 B to 1,921,108 B and the binary's md5 moved while its 5,493,000 B stayed, which is the build path inside the object and not a source change: the source is the same pin `f09ac7cccd` and the same one patch. The seat's pin is the same version, so the install replaces it in place.

# xash3d-fwgs - pins, sizes, measured rounds

Numbers are kept here, out of the README, so the README stays one screen of
instructions. Everything below is what the build in the image and the seat rounds
actually did.

## The engine

```
gitsm://github.com/FWGS/xash3d-fwgs.git  protocol=https  branch=master
SRCREV 707596f4905689ec224e993fcaa4d4ad66aa67d6  ("public: reformat with xash-clang-format", 10 Nov 2026)
LICENSE GPL-3.0-or-later, LIC_FILES_CHKSUM engine/common/common.c lines 1-15, md5 2839f32588989257fbf1c08fff5d71fe
PV 0.0+git${SRCPV}   PKGR r0.01   S ${WORKDIR}/git
DEPENDS  = libsdl2 freetype zlib bzip2 libogg libvorbis libopus opusfile
RDEPENDS = libsdl2 freetype zlib bzip2 libogg libvorbis libopus opusfile alsa-lib
FILES   += ${libdir}/xash3d ${datadir}/xash3d
```

`gitsm://` because the tree has submodules and the renderer and the extras come out of
them. The tree carries **no** top-level LICENSE file to checksum, and the engine headers all
carry the same banner: `engine/common/common.c` lines 1 to 15 are the GPL-3 notice ("either
version 3 of the License, or (at your option) any later version"), 369 of the C sources say
version 3 and none of them says version 2, so the license is GPL-3.0-or-later and the checksum
is taken on those first fifteen lines. The Half-Life **game** libraries are NOT in this tree:
they come from Valve's own Half-Life 1 SDK, whose agreement is `LicenseRef-Valve-Half-Life-1-SDK`
and is not a free licence, so the gamedll pair this seat runs is built from that SDK and
installed locally rather than shipped as a recipe.

## Build shape - no patches, every decision is a waf switch

```
./waf configure --enable-packaging --prefix=${prefix} --enable-gles3compat --disable-gl --disable-werror
./waf build  -j ${BB_NUMBER_THREADS}
./waf install --destdir=${D}
```

- `--enable-packaging` (`wscript:196`) is what makes PREFIX, LIBDIR, BINDIR and SHAREDIR mean
  anything. Without it `wscript:657` sets them all to PREFIX, so the launcher, `libxash.so` and
  `libmenu.so` would sit in the same directory and the data root would have no name of its own.
  With it, and with `--prefix=/usr`, `wscript:655` spells `SHAREDIR = ${PREFIX}/share/xash3d` and
  `wscript:656` adds `/xash3d` to LIBDIR, which is what the run script repeats in its
  `LD_LIBRARY_PATH`, `-rodir` and `-ref` arguments.
- `--enable-gles3compat` (the RefDll row at `wscript:169`) selects the `ref_gles3compat` target
  in `ref/gl/wscript`, which compiles `ref/gl/*.c` with `-DXASH_GLES3COMPAT=1` and links
  nothing: no `-lGL`, no GL headers.
- `--disable-gl` takes the `ref_gl` target out. It is the default RefDll row
  (`wscript:170 RefDll(gl, True)`), so leaving it on would build a second renderer whose
  context request (`gl_opengl.c:1492`, compatibility profile) and whose `-lGL` line the seat
  cannot answer.
- `--disable-werror` (`wscript:208`) leaves waf out of its own strict block, the list at
  `wscript:313` of some thirty `-Werror=<diagnostic>` entries. That list is aimed at the
  warnings gcc 13 raises on this tree, and the OE CFLAGS already carry their own `-Werror`
  policy; the alternative here is a configure that fails before anything is compiled.
- Deliberately **not** switched on: `--enable-dedicated` (the xash dedicated server program is
  not wanted on a seat that plays single player), `--enable-bundled-deps` (see `DEPENDS`),
  `--enable-tui` (its Subproject row wants the maintui submodule, whose `.gitmodules` row says
  `update = none`) and `--64bits`/`--32bits`, which only mean anything to an x86 target.

`waf install --destdir=${D}` is the packaging route: every `install_path` in the tree is
absolute or PREFIX-relative, so the destdir lands them at `${D}/usr/bin/xash3d`,
`${D}/usr/lib/xash3d/libxash.so`, `libref_gles3compat.so`, `libmenu.so` and
`${D}/usr/share/xash3d/valve/extras.pk3`. `--destdir` is wanted over the `DESTDIR` environment
variable because this is the same spelling `hlsdk-portable` uses, whose top `wscript`
(`hlsdk wscript:310`) fatals without it.

## Why this engine and this renderer

The engine the 1998 releases shipped is a Win32 binary, and the x86 Linux build FWGS still
publishes is an i386 library pair this board has no loader for, so the engine has to be built
for aarch64 here. Exactly one renderer comes on, because the seat is EGL/GLES-only: it carries
EGL, GLES1, GLES2 and GLES3 and the Vivante GAL/VSC driver, and has no desktop GL at all (no
`libGL.so` on any search path, `eglBindAPI(EGL_OPENGL_API)` answers `EGL_BAD_PARAMETER`,
`EGL_CLIENT_APIS` names only `OpenGL_ES` and `OpenVG`). The plain `ref_gl` renderer is out
twice over for that reason: with `XASH_GLES` unset, `ref/gl/gl_opengl.c:1492` `GL_SetupAttributes`
asks SDL for a COMPATIBILITY-profile context, and `ref/gl/wscript` gives the `ref_gl` target a
`-lGL` line when `--enable-static-gl` is on. `gles3compat` compiles its own GL types
(`ref/gl/gl_export.h:35` turns `XASH_GLES3COMPAT` into `XASH_GLES 1` and declares the entry
points itself) and takes every one of them at run time through SDL, so no GL header or GL
library is wanted at build time either, and at run time `GL_SetupAttributes` asks for an ES
profile, which is what the seat's EGL driver hands out. The seat round's own lines are

```
GL_CheckExtension: GL_OES_texture_cube_map - enabled
GL2_ShimInit: config: ASYNC MAP CYCLE=1 VER=310
```

and the extension dump it prints is the seat's, down to `GL_VIV_direct_texture`.

## What the package installs

```
/usr/bin/xash3d                          67,704 B    the launcher waf installs; dlopens libxash.so, calls Host_Main
/usr/lib/xash3d/libxash.so                2,725,976 B  the engine core, RUNPATH $ORIGIN
/usr/lib/xash3d/libref_gles3compat.so                   the one renderer this seat can drive
/usr/lib/xash3d/libmenu.so                              the mainui menu
/usr/lib/xash3d/filesystem_stdio.so                     the WAD/PAK/searchpath layer
/usr/share/xash3d/valve/extras.pk3      1,698,896 B md5 0def12ec5581d655a70b7b157597f6f3
```

Four libraries and not three: `libxash.so` (the engine core), `libmenu.so` (the mainui menu),
`libref_gles3compat.so` (the renderer) and `filesystem_stdio.so` (the WAD/PAK/searchpath layer).
`extras.pk3` is the menu/HUD/font pack from the `xash-extras` submodule.

The launcher waf installs is spelled `xash3d` and takes an engine-side argument set, not a
game: it has no idea which tree to open, where the writable directory is or which renderer the
seat can drive. That set belongs to the game, so the launcher that matters is the run script
this README writes. For anyone hand-running waf's own: because `DEFAULT_RPATH` is `$ORIGIN`
(`wscript:571`), `libxash.so` is looked for next to `/usr/bin/xash3d` first and is not there,
so `LD_LIBRARY_PATH=/usr/lib/xash3d` has to be in the environment for the four libraries in
that directory to resolve at all. The seat's own copy also names
`/gamedata/opt/opusfile/usr/lib`, because the engine's `libopusfile.so.0` is the only library
this image did not already have - the others (`libogg`, `libvorbis`, `libvorbisfile`, `libopus`,
`libbz2`, `libz`) are in `DEPENDS` for exactly that reason.

## Half-Life: Up-link - the tree the seat actually runs, and why it is not a recipe

The tree is installed on the seat at `/gamedata/games/half-life-uplink` (23 files,
88,481,841 B) as an engine test. It fails on the **licence** half rather than the download
half, and everything the release ships that could carry a grant was read:

| Carrier | Size / md5 | What it says |
| --- | --- | --- |
| `readme.txt` | 20,364 B md5 `63af51a2e69c0ba7a09056440e2aba71` | ends `Copyright (1998 Sierra On-Line, Inc.)`, and nothing else about distribution |
| `valve/Titles.txt` | 10,400 B | `? Copyright Valve Corp. 1999` |
| `media/order/default.html` | - | `Sierra Studios and Valve, L.L.C. ? 1999` |
| `hl.dll` | 849,408 B md5 `0e7c3b4f786aadb88a1c0f1657cd5275` | no match |
| `hldemo.exe` | 737,280 B md5 `b8fc5f249bc3231b16568b03490a16fd` | no match |
| `sw.dll`, `hw.dll`, `hl_res.dll`, `UNWISE.EXE` | - | no match |

Across all of them there are zero matches for `distribut`, `freeware`, `shareware`,
`license` or `non-commercial`, so the string `LicenseRef-Sierra-On-Line-Up-link-readme.txt`
is not a licence grant - it is a copyright line. No recipe.

## What the release is and is not, which matters for anyone re-running it

`pak0.PAK` is 79,150,544 B (header `PACK\xd0\xd5`, 1952 entries, directory entries 64-byte-strided
at offset 79040400) and carries exactly ten maps: `hldemo1`, `hldemo2`, `hldemo3`, `t0a0`,
`t0a0a`, `t0a0a b`, `t0a0b1`, `t0a0b2`, `t0a0c`, `t0a0d`, plus `maps/graphs/*.nod`. There is no
`c0a0` or `c1a0`, so at the end of the last mission map the engine's own `Spawn Server: c0a0`
answers `Host_Error: Could not load model maps/c0a0.bsp from disk` - that is the release ending,
not a fault. The missing Half-Life assets log the same way: `Could not load model models/w_357.mdl`
and the `v_gauss`, `p_rpg`, `crossbow`, `egon`, `satchel`, `squeak`, `hgun` models,
`sound/squeek/...`, `sound/plats/vehicle_ignition.wav`, `overviews/<map>.txt`, `Prospero01.mp3`,
because Uplink shipped a subset. Harmless on every engine run of this layer:
`couldn't exec valve/vfs.cfg`, `couldn't exec video.cfg`/`opengl.cfg`,
`SV_LoadProgs: couldn't get physics API`, `Cannot load xashcomm.lst`, `FS_LoadImage: couldn't
load "game"`, `Missing bitmap font: gfx\vgui\fonts\1600_Default Scheme.tga`.

`delta.lst`, the one file the engine fatals without and that is in none of the places a payload
would be expected to carry it (not in the Uplink release zip, not in its `pak0.PAK`, not in
`FWGS/xash3d-fwgs`, not in `FWGS/xash-extras`, not in `FWGS/hlsdk-portable`), is Valve's own:

```
https://raw.githubusercontent.com/ValveSoftware/halflife/master/network/delta.lst
12,317 B  md5 68ba2c129b6a29e70d1cc89d1c4330f5
sha256 3937bf78850dd024938ef94ceaadd30e9bd3967a3872aa85fc6b17ae83c46001
rows: clientdata_t none
      entity_state_t gamedll Entity_Encode
      entity_state_player_t gamedll Player_Encode
      custom_entity_state_t gamedll Custom_Encode
      usercmd_t none
      weapon_data_t none
      event_t none
```

Without it the log is one line and nothing else happens:
`Delta_InitFields: couldn't load file delta.lst`.

## The gamedll pair, and the suffix that makes the engine find it

`valve/dlls/hl_arm64.so` 2,016,864 B and `valve/cl_dlls/client_arm64.so` 1,120,096 B, in the
**writable** tree. GoldSrc is not a complete game by itself: the gameplay comes from the game
DLL, and the seat needs an aarch64 one, which is what this layer's `hlsdk-portable` build
produced before that recipe was taken out of the layer. `_arm64` is the suffix the `library-suffix`
submodule's `COM_GenerateCommonLibraryName` spells, so the engine finds them without a config
line. They are built from Valve's Half-Life 1 SDK, whose agreement is
`LicenseRef-Valve-Half-Life-1-SDK` and is not a free licence, so they are installed on the seat
locally and are shipped by no recipe.

## The control proof, which is the payload's own config rather than a patch

The release's `valve/default.cfg` (1,905 B md5 `05c06bb2f46283b8a89722d958f5caef`, still dated
Feb 1 1999 in the read-only rodir, and the writable tree carries no copy of it) ends with
`sensitivity 3` and `+mlook`, so Xash3D is in mouselook from the first load, and the release's
binds are `,` and `a` for `+moveleft`, `.` and `d` for `+moveright`, `PGUP` `+lookup`, `PGDN`
`+lookdown`, `INS` `+klook`, `v` and `'` `+moveup`, `/` `+movedown`, `UPARROW` `+forward`,
`SHIFT` `+speed`, `CTRL` `+duck`, `r` `+reload`, `END` `centerview`, `MWHEELUP`/`MWHEELDOWN` the
inventory wheel, `F11` the release's own `zoom_in`/`zoom_out` alias pair, `F5` `snapshot`.

## The saves, and the case trap that makes them unloadable

The release's zip carries twelve saves - `autosave01.sav` 475,035 B, `autosave.sav` 339,075 B,
`quick01.sav` 460,208 B, `quick.sav` 329,951 B, `HLSave01.sav` 166,714 B, `HLSave.sav` 165,673 B,
and the `hldemo1.HL1`/`.HL2`/`.HL3` triple at 154,061/1,260/12 B and the `hldemo2` triple at
175,740/1,836/4 B - and all twelve are in `valve/save/` now: the six whose names do not clash went
in as they are spelled, and the six that would have overwritten the seat's own autosaves and its
own per-map quicksaves went in under an `uplink-` prefix (`uplink-autosave01.sav`,
`uplink-hldemo2.HL1`), so the saves the seat wrote during the proof round are still there beside
them - `autosave01.sav` 365,959 B and `autosave.sav` 360,869 B with their `autosave*.bmp`
snapshots at 284,854 B, `hldemo1.HL1`/`.HL2`/`.HL3` at 161,695/6,439/12 B and the `hldemo2` triple
at 175,231/11,326/4 B. A save name is the stem of its triple, so renaming the stem is what keeps
both sets loadable. The engine created `valve/save/` in lower case, not `valve/SAVE/` as the
release's own zip spells it, and anything copied into a `SAVE` directory with capital letters is
read only if the filesystem is case-insensitive, which this one is not.

## Measured on the seat - the release's own two maps, in-game, with the user at the seat

```
+map t0a0     # the trainer: doors, the holo, the sentence speaker
+map hldemo1  # the first mission map: intro, CD audio, the panel and ceiling scripts
```

The engine's own lines, from `/gamedata/logs/uplink6.log`, `uplink7.log` and the cleaned
`/gamedata/logs/u5clean.log` (890,333 B):

```
[21:16:13] Found: func_door, firing (startdoor)
[21:16:15] Playing sentence !HOLO_INTRO (3.0)
[21:16:29] Playing sentence !HOLO_BUTTON (4.0)
[21:13:21] Firing: (start_cd_audio)
[21:13:21] Found: target_cdaudio, firing (start_cd_audio)
[21:13:22] Firing: (DEMOTXT1) ... DEMOTXT2 ...
[21:13:27] Firing: (intro_ambient_1) ... intro_ambient_2 ...
[21:13:50] Found: func_door_rotating, firing (start_popping_panel_mm)
[21:13:17] lighting: colored
```

`lighting: colored` appears three times across the runs, at 21:13:17, 21:18:25 and 21:33:07 -
that line is the renderer taking the lightmapped path rather than the flat one, which is the
engine saying it is drawing the world with per-vertex light. `Playing sentence !HOLO_INTRO` is
the game's own voice line through the sound channel; `Found: target_cdaudio, firing (start_cd_audio)`
is the mission's CD track started, and the user at the seat confirmed the graphics, the frame rate
and the audio over the whole session. One window shot is at
`/gamedata/installs/shots/uplink1/wayland-screenshot-2026-11-17_20-27-31.png`, 691,797 B, the
release's own menu.

The seat's run script `/gamedata/run/xash-uplink.sh` is the shape in README.md verbatim, 555 B -
including the `YDOTOOL_SOCKET=/tmp/.ydotool_socket` line, which is where the seat's synthetic-keystroke
daemon would have attached and nothing does, and anything after it is engine command-line
(`+map t0a0` is the release's trainer level, `+map hldemo1` its first mission map).

## The gallery pair of 2026-11-21 - the trainer map, and the byte-identical pair that is not a parked game

The owner played Uplink at the seat's own keyboard and mouse and answered that it works 100 %, so
this is the last round this game needs and the only thing owed was the gallery pair. The round ran
as one run of the game, `/gamedata/run/uplink-sweep.sh t0a0 half-life-uplink-xash`, whose engine
call is the seat's own `/gamedata/run/xash-uplink.sh +map t0a0` verbatim: the engine prints its own
args as `-game valve -rodir /gamedata/games/half-life-uplink -ref gles3compat -width 1024 -height
576 -dev 2 -noch +map t0a0` with `/gamedata/games/uplink-writable is working directory now`, and
`Adding PAK: /gamedata/games/half-life-uplink/valve/pak0.PAK (1952 files)` with the wads and
`Adding ZIP: valve/extras.pk3 (151 files)`. The console is `/gamedata/logs/gallery-half-life-uplink-xash.txt`
70,804 B, and its first lines are SDL's own probe spam - `Failed loading wl_egl_window_create:
/usr/lib/libwayland-client.so.0: undefined symbol: _wl_egl_window_create` and the `xkb_*` lines
beside it, SDL 2 trying its wayland bridge through `dlopen` - which is not what the engine draws
through and which the round's own `GL2_ShimInit` line answers after. The driver waits for that
renderer line and for `lighting: colored` before it presses the shutter, so the pair cannot be the
menu the game opens on. What the game then printed is its own in-game state: `Spawn Server: t0a0`,
`loading maps/t0a0.bsp`, `lighting: colored`, and the trainer's scripted events firing in order -
`Found: func_door, firing (startdoor)`, `Found: scripted_sentence, firing (sent_intro)` with
`Playing sentence !HOLO_INTRO (3.0)` and `Playing sentence !HOLO_BUTTON (4.0)`, `Found: func_door,
firing (td1)`. The seat's scene graph gives the window as `top-level window 'Half-Life' of xash3d`
at `role xdg_toplevel`, surface ID 31.

| Shot | colours | non-black | row lum | mid-row jumps | md5 |
| --- | --- | --- | --- | --- | --- |
| `start.png` 1,016,434 B | 16,000 | 2,073,513 | 12.5-26.5 | 3 | `f5c25d8714f9a62a899d28aea81ea840` |
| `idle.png` 1,016,434 B, +15.2 s, nothing pressed | 16,000 | 2,073,513 | 12.5-26.5 | 3 | `f5c25d8714f9a62a899d28aea81ea840` |
| `after1.png` 1,036,632 B, one mouse jump earlier | 14,740 | 2,073,599 | 12.4-29.0 | 1 | `1bf73beb47d6098626ed077ebbf52e3b` |
| `after2.png` 1,036,632 B, +6.2 s, nothing pressed | 14,740 | 2,073,599 | 12.4-29.0 | 1 | `1bf73beb47d6098626ed077ebbf52e3b` |

| Pair | identical | >8 | columns / rows moved |
| --- | --- | --- | --- |
| `start` vs `idle` | 2,073,600 (100.0 %) | 0 | 0 and 0 |
| `after1` vs `after2` | 2,073,600 (100.0 %) | 0 | 0 and 0 |
| either of the first pair vs either of the second | 7,646 (0.4 %) | 1,006,935 (48.6 %) | **270** / 1,920 and **21** / 1,080 |

What that says, and what it does not: both idle pairs are byte-identical, so the two shots of each
pair are the same frame, and that is read as the game's own **still view of a live game**, not as a
parked menu - the console named the map, loaded it and had the trainer's scripted speaker and its
doors firing before the shutter was pressed, and nothing was pressed between the shots of a pair.
The pair that does change is the one whose input is named: the driver sent exactly one
`ydotool mousemove 1500 300`, which SDL's relative mouse mode takes as one whole delta, and the view
under it moved by 270 of 1,920 columns and 21 of 1,080 rows of the coarse shape with 48.6 % of the
pixels past a max-channel difference of 8. Only that pair may be read as the camera, and the layer's
rule says so in the driver's own comment. The gallery pair this game ships is the `start`/`idle` one;
the two `after` shots stay beside it under their own probe names.

## Failure modes

| Symptom | Cause |
| --- | --- |
| `Delta_InitFields: couldn't load file delta.lst` and then nothing else | the engine fatals without that file, and it is in none of the places a payload would be expected to carry it, so it has to be put in the **writable** tree by hand. Its bytes and rows are above |
| the run does not start at all | `LD_LIBRARY_PATH` is not set: the four libraries live in `/usr/lib/xash3d`, which is not on any default search path, and `DEFAULT_RPATH` is `$ORIGIN` (`wscript:571`) so `libxash.so` is looked for next to `/usr/bin/xash3d` first and is not there. The separator is a colon - a space in that variable breaks it |
| the engine writes into the game tree | it cannot: the rodir is read-only in this build, so the writable tree (`XASH3D_BASEDIR`, no `-writedir` flag exists) has to be a separate one, and the two must not be the same directory |
| the release's saves do not load | the engine created `valve/save/` in lower case, not `valve/SAVE/` as the release's own zip spells it; a case-sensitive filesystem will not bridge that. A save name is the stem of its `.HL1`/`.HL2`/`.HL3` triple, so renaming the stem keeps both the release's saves and the seat's own loadable |
| `Host_Error: Could not load model maps/c0a0.bsp from disk` after the last mission map | that is the release's own ending: `pak0.PAK` carries ten maps and no `c0a0` or `c1a0` |
| `Could not load model models/w_357.mdl` and the `v_gauss`, `p_rpg`, `crossbow`, `egon`, `satchel`, `squeak`, `hgun` models, `sound/squeek/...`, `sound/plats/vehicle_ignition.wav`, `overviews/<map>.txt`, `Prospero01.mp3` | expected: Uplink shipped a subset of Half-Life's assets |
| `couldn't exec valve/vfs.cfg`, `couldn't exec video.cfg`/`opengl.cfg`, `SV_LoadProgs: couldn't get physics API`, `Cannot load xashcomm.lst`, `FS_LoadImage: couldn't load "game"`, `Missing bitmap font: gfx\vgui\fonts\1600_Default Scheme.tga` | expected on every engine run of this layer |
| `ref_gl` renderer asked for, and no context | the seat has no desktop GL: `gl_opengl.c:1492` asks SDL for a COMPATIBILITY-profile context and the `ref_gl` target carries a `-lGL` line with `--enable-static-gl`. `--enable-gles3compat --disable-gl` is the answer, and the seat's own lines are `GL_CheckExtension: GL_OES_texture_cube_map - enabled` and `GL2_ShimInit: config: ASYNC MAP CYCLE=1 VER=310` |
| a Sven Co-op tree opened with nothing beside it | `FWGS/xash3d-fwgs` carries no `SVEN CO-OP` game definition anywhere in its tree, so Sven Co-op data has nothing on this seat to run it against; the co-op project builds its own gamedll |
| a GoldSrc mod that wants desktop GL3 | Diffusion, by Aynekko, says `Yes, it's 64-bit and OpenGL only. It needs OpenGL version 3.0`, and `glsl/enhance_fp.glsl` is a desktop-GL3 shader this EGL/GLES-only seat cannot open; its payload is also unfetchable without a logged-in session (ModDB mirror answers 403 `cf-mitigated: challenge`, itch.io's locker needs a session `file_token`) and its page carries no grant. Documented, installed nowhere |
| `--destdir` vs `DESTDIR` | `waf install --destdir=${D}` is the spelling this tree wants; `hlsdk-portable`'s top `wscript` (`hlsdk wscript:310`) fatals without the flag form |
| `wscript:196` / `--enable-packaging` left off | `wscript:657` sets PREFIX, LIBDIR, BINDIR and SHAREDIR all to PREFIX, so the launcher, `libxash.so` and `libmenu.so` would sit in one directory and the data root would have no name of its own |

## Files

`files/` is empty on purpose: the renderer choice, the packaging layout and the dependency
switches are all options the tree's own `wscript` has. `xash3d-fwgs_git.bb` builds the engine
and installs the four libraries, the launcher and `extras.pk3`.

## Open

- [x] the GL-free renderer: `gles3compat` compiles its own GL types and takes them through SDL,
      and the seat answers an ES profile at run time
- [x] the writable/read-only split, which is the only shape this engine will run in on a read-only
      rootfs
- [ ] a GoldSrc payload with a real grant. None was found on this round's audit: the Uplink release
      carries a copyright line only, the retail Half-Life is not free to download, and the free
      add-ons that do carry a grant of their own are the ones worth a recipe when a source
      authorized by their authors is found for them

## The debs this branch builds (built 2026-11-20 from this tree)

The whole layer was built again from this restructured tree at its own path: `bitbake chocolate-doom fteqw ioquake3 eduke32 dosbox-x openjazz xash3d-fwgs librequake openarena opennowlite` ran green on the build host's `stm32mp25-watermelon-wine-1a` machine, and this recipe's own task chain re-ran inside that run, so the numbers below are this tree's and not the ones an earlier round happened to record. `xash3d-fwgs` leaves the build as `0.0+git0+707596f490-r0.01`:

| deb | size | md5 |
| --- | --- | --- |
| `xash3d-fwgs_0.0+git0+707596f490-r0.01_arm64.deb` | 3,077,968 B | `54e7305e8fe737a0f12a2b05315ab8b6` |
| `xash3d-fwgs-dbg_0.0+git0+707596f490-r0.01_arm64.deb` | 5,491,460 B | `3e9888916ac7f4408b1e9f4f8606ac2d` |
| `xash3d-fwgs-dev_0.0+git0+707596f490-r0.01_arm64.deb` | 2,172 B | `f04c3a7368e57574fa802f40ccfe60ac` |
| `xash3d-fwgs-src_0.0+git0+707596f490-r0.01_arm64.deb` | 2,202,360 B | `4072a7a660c48a3a3afaa42e8385c992` |

The base deb installs 6 files: the loader `/usr/bin/xash3d` and the libraries the engine dlopays out of `/usr/lib/xash3d` - `libxash.so` 2,725,976 B md5 `9083f883bd698970b518f0951e2e12f9`, `libmenu.so`, `libref_gles3compat.so` 397,680 B md5 `a74aa518f74ff8149f2db5e49bf983e8` and `filesystem_stdio.so` - plus the `valve` tree's `extras.pk3`. The seat's pin is the same version, so the install replaces it in place.

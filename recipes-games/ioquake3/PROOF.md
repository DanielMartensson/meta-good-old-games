# ioquake3 - pins, sizes, measured rounds

Numbers are kept here, out of the README, so the README stays one screen of
instructions. Everything below is what the build in the image and the rounds on the
seat actually did.

## Pin

```
git://github.com/ioquake/ioq3.git  protocol=https  branch=main
SRCREV 52088b9eae247d8e321c8dc1fb49a879652854f9   ("General tidy-up to Huff_Decompress (fixes #894)", 4 Nov 2026)
LICENSE GPL-2.0-or-later, COPYING.txt md5 87113aa2b484c59a17085b5c3f900ebf
PV 1.36+git<SRCPV> (1.36 is cmake/identity.cmake:2 PROJECT_VERSION), PKGR r0.04
```

`branch=main` because that is what the repository calls its default branch today:
`branch=master` is a name this tree dropped, and the fetch now stops with
`Unable to find revision ... in branch master even from upstream` rather than fetching
the same commit through a name it no longer answers. The md5 above is this file's own
and not GNU's - the vendored `COPYING.txt` is the old formatted layout, which hashes to
`87113aa2...` rather than the `94d55d512a9ba36caa9b7df079bae19f` of the current plain-text
GNU text; the sources' own headers (`code/renderergl2/tr_glsl.c:9`) say "version 2 of the
License, or (at your option) any later version", so the licence is v2-or-later.

PKGR history, since the deb version has to stay comparable across rebuilds:
r0.01 shipped a launcher whose comment block arrived at the deb broken across bare word
lines - that `/usr/bin/ioquake3` on a seat logged `line 15: r_preferOpenGLES: not found`
before any engine output at all, because the continuation lines of the comment were
executed - while this file's `printf` list has every line quoted and comment-prefixed.
r0.02 added patch 0002 and the `do_install` that goes with it. r0.03 packed this build's
vm modules into `vm-pure.pk3` instead of leaving them loose, which is what makes the in-game
UI load at all (the FS_FindVM fact below). r0.04 moved those modules out of the payload
recipe's `demoq3` tree, which is gone from this layer, into this package's own
`/usr/share/ioquake3/baseq3`, with the launcher's default `fs_basepath`/`fs_game` pair
moved to match.

## Patches

| Patch | What it changes |
| --- | --- |
| `0001-build-the-qvm-tools-on-the-build-machine-not-on-the-target.patch` | inserts the build-machine settings after the `BUILD_TYPE_ARG` block at `cmake/utils/qvm_tools.cmake:11-:13` and adds one line to that ExternalProject's `CMAKE_ARGS` list at `:20`, so the qvm tools (`q3rcc`/`q3cpp`/`q3lcc`/`q3asm`/`q3lburg`, `cmake/tools/CMakeLists.txt:67-:93`) are configured with the build machine's compiler instead of the cross one - which is also why `:15-:18` names them `${HOST_EXECUTABLE_SUFFIX}`. Without it the tools are built for aarch64 and cannot run on the x86 build host, so no bytecode gets emitted at all. The bytecode itself is Q3VM ISA and not machine code, so the modules the seat loads are the same bytes the build host compiled, and the interpreter in `code/qcommon/vm_interpreted.c` runs them wherever the seat is |
| `0002-let-the-demoq3-shareware-payload-skip-the-purchased-game-pak-mask-check.patch` | inserts one extra condition into the nine-pak demand at `code/qcommon/files.c:3697` inside `FS_CheckPak0`, so it is skipped exactly when `fs_game` names the `demoq3` tree. A full game's check (`fs_game` empty or `baseq3`) answers exactly as before. Why the demand is fatal here, measured twice: the whole demo payload is one `demoq3/pak0.pk3`, and the process dies right after mounting it - console ends `1384 files in pk3 files`, then `Quake 3 data files are missing. Please copy pak0.pk3 through pak8.pk3 from the baseq3 directory in your Quake 3 install or CD-ROM to: ...` plus `ERROR: couldn't open crashlog.txt`, with no live process ten seconds later and the weston shots the same desktop every time. Nothing the launcher passes can dodge it: `com_basegame` answers `baseq3` in this Q3 build, so with any pak mounted the standalone branch at `:3652` is not taken and `com_standalone` gets forced to 0 at `:3657` (a `+set com_standalone 1` on the command line is overwritten there), and the `demoq3` rescue just above only ORs bit 0 into the found-pak mask behind a hard-coded `DEMO_PAK0_CHECKSUM` - its own warning says "may work fine, but it is not guaranteed or supported" - while the fatal still wants all nine bits. So vanilla ioquake3 cannot boot the demo at all, which is why FTEQW grew its own `q3demo` protocol row and why this engine replaced that FTE route |

`do_configure:prepend` greps every `-D<opt>` name this recipe passes back into
`CMakeLists.txt`'s own `option(...)` lines, so a tree that renamed one stops the build
with that name instead of silently configuring with upstream's defaults (which would
build opengl1 and turn dlopen on).

## What the package installs

```
/usr/bin/ioquake3-gl2                    the client, opengl2 renderer linked in
/usr/bin/ioquake3                        the launcher
/usr/share/ioquake3/baseq3/vm-pure.pk3   332,078 B, this build's own version-6 vm modules
```

The three members inside that pk3, with the sizes the seat's own console printed back:
`vm/ui.qvm` 2,820,288 / `vm/qagame.qvm` 5,108,352 / `vm/cgame.qvm` 4,183,168 bytes
(`ui loaded in 2820288 bytes`, `qagame loaded in 5108352 bytes`, `cgame loaded in 4183168 bytes`
followed by `CL_InitCGame: 5.07 seconds` with the process still running). `ZIP_DEFLATED`,
written by python3's `zipfile` rather than a `zip` command, because `do_install` runs inside
this recipe's own sysroot: the first round of that change probed for `/usr/bin/zip` and the
`bbfatal` fired, which said the dev machine's zip is not on that PATH at all, while the same
log's `extend_recipe_sysroot` line already names python3-native as a package this sysroot
carries. `do_install` fails with the name of any missing `${B}/Release/baseq3/vm/<qvm>.qvm`,
so a build that stopped producing one cannot pack a pk3 the engine then cannot boot.

## Measured on the seat

Rounds at the seat's 1920x1080, screenshots through `XDG_PICTURES_DIR=<dir> weston-screenshooter`,
colour and pair numbers from `tools/shotstat.py` and `tools/imgdiff.py` on the build host
(the seat has no PIL). Threshold in diff columns: max-channel difference of 8.

### Round of 2026-11-15 - four configurations of the same client on the same payload

The black screen Quake III threads get blamed on, settled by measurement. Four configs,
three shots each at 1920x1080 (`/gamedata/installs/shots/q3b`, consoles `/tmp/ioq3-black-*`):

| Configuration | Distinct colours | Non-black of 2,073,600 | Sweep pair >8 | row-lum min | |
| --- | --- | --- | --- | --- | --- |
| launcher defaults | 136,766 / 136,265 / 86,041 | 2,070,123-2,073,582 | 92.7 %, 1,691/1,920 cols, 981/1,080 rows | 45.7 | draws, mouselook alive |
| `+set r_msaaSamples 4 +set r_multisamplesamples 4 +set r_ext_multisample 4` | 137,093 / 135,530 / 80,379 | 2,071,002-2,073,584 | 92.2 %, 1,714/1,920 cols, 885/1,080 rows | 45.9 | draws |
| `+set r_mode -1 +set r_customwidth 1920 +set r_customheight 1080` | 170,312 / 135,461 / 78,322 | 2,073,197-2,073,429 | 94.2 %, 1,745/1,920 cols, 940/1,080 rows | 47.6 | draws, at the obvious framerate cost |
| `+set r_mode 0` | 35,581 / 35,040 / 53,354 | 2,073,600 (all) | 91.0 %, 1,702/1,920 cols, 624/1,080 rows | **9.4** | **dark - the failure is this one** |

What that says:

- The two causes the Quake III threads blame - a multi-sampling request on a driver whose
  MSAA framebuffers come back incomplete, and a framebuffer the size of the panel - **do
  not reproduce** on this board: the msaa ask draws with the same colour count as the default
  (137,093 vs 136,766), a near-nothing change at the ogles2 renderer, and the panel-sized ask
  draws *better* (170,312 colours).
- `r_mode 0` is the broken ask. The console answers `MODE: 0, 320 x 240 fullscreen hz:N/A`:
  the mode list's first entry, a 320x240 4:3 surface the compositor then stretches over the
  16:9 panel. The shot is not black-black (all 2,073,600 pixels carry something) but its
  brightness collapses (`row-lum min/max: 9.4 62.1` against the 45.7-94.2 of a working run)
  and its mid row has **0** brightness jumps where the working shots have 12, so the picture
  has no edges to read. Never ship `r_mode 0`.
- The engine's own archived config, read back after each run, is what settles the multisample
  spelling: it stores `seta r_ext_multisample "4"` and `seta r_ext_framebuffer_multisample "0"`,
  and `strings /usr/bin/ioquake3-gl2` names those two and **not** `r_msaaSamples` or
  `r_multisamplesamples`. So ioquake3 1.36's real cvars are the `r_ext_multisample` /
  `r_ext_framebuffer_multisample` pair, and the `r_msaaSamples` name the newer ioquake3 threads
  quote is an unknown name here. The trap that makes that knowable at all: the console cannot
  tell registered from unknown - a `toggle` probe printed no `Unknown command` line even for a
  made-up name - only `strings` on the binary could.

### The audio trap in the same round

All four runs printed

```
SDL audio driver is "alsa".
SDL_OpenAudioDevice() failed: ALSA: Couldn't open audio device: Device or resource busy
Sound initialization failed.
```

and the reason is not ioquake3: a UT99 SoftDrv client from the previous round (`/proc`
named it `./System/ut-bin Bluff.unr?Game=DeathMatchUnrealI`) was running through the whole
round, and the seat's ALSA sink serves **one** process at a time. The earlier round of
2026-11-14 printed `SDL audio driver is "alsa"` and played, with no busy in the log. So a
proof round kills every other game before launching the next one, and a launcher is not
broken when its audio init says `Device or resource busy`.

### Live playtest of 2026-11-15, started AT the seat

The owner started `/usr/bin/ioquake3 +map q3dm7` at the seat's own keyboard and mouse and
played it: audio, graphics and control confirmed 100 % by hand, not by the ydotool trick.
One shot taken on the way measured **320,386 distinct colours** at 1920x1080 - the richest
frame of the layer so far, a lit arena plus HUD - with 2,071,397 of 2,073,600 non-black and
120 distinct row lums. The console's only refusals were the expected ones: `couldn't exec
autoexec.cfg` (the demo ships no config) and four `Failed to load sound` lines for `assist.wav`,
`defense.wav` and `hgrenb1a`/`hgrenb2a` - team-feedback and grenade-pickup sounds the full
release ships and this demo does not, verified against the payload rather than assumed: the
1,387-file `pak0.pk3` carries 249 wavs and zero matches for `assist`, `defen` or `vo_`.

### The payload this round ran, named by the engine itself

Round of 2026-11-14, `/usr/bin/ioquake3 +map q3dm7`, five shots at 1920x1080: 29,990-52,268
distinct colours, 2,073,373-2,073,433 non-black, idle pair 94.3 % identical with 2.0 % of the
frame alive (the `com_showfps 1` HUD), sweep pairs 73.9-88.8 % moved, up to 1,504/1,920 columns
and 539/1,080 rows, alive 2:04 at 133,980 kB. The console named the payload it found: `demoq3/vm-pure.pk3
(3 files)` with the three qvm sizes above and `pak0.pk3 (1387 files)`, which is the 46,853,694-byte
member that payload carried (md5 `0613b3d4ef05e613a2b470571498690f`), and it printed `MODE: -1,
1024 x 576 fullscreen hz:N/A` - the launcher's own custom pair, taken.

### The FTE route on the same payload, kept for the comparison

48.2 fps sustained at 1024x576 (rss 136,604 kB), 14.0 fps over the whole 1920x1080, and the
round of 2026-11-13 rendered **pure black** on this Vivante, while ioquake3 on the same payload
drew 136,766 distinct colours with 2,072,380 of 2,073,600 non-black and 92.7 % of the frame
moved over a mouse sweep. The engine swap is what made the same payload draw. Full record in
`../fteqw/PROOF.md`.

## What the payload would be, if a hand install goes through id's own release

Recorded so a hand install of the same bytes can be checked against them - this layer fetches
none of it (the carrier is a third-party mirror of id's dead ftp corner and nobody from id
answers for it):

```
carrier  idstuff/quake3/linux/linuxq3ademo-1.11-6.x86.gz.sh
         a gzip inside a shell script; `unpack=false` because bitbake's unpacker
         works off the file extension and knows no `.sh`
         1,387 entries / 79,243,450 unpacked bytes inside
         sha256 64dee3f69b6e792d1da4fe0ac98fedc7eb1e37ea1027fb609a9fadd06150a4ec
inside    demoq3/pak0.pk3  46,853,694 B, 1,387 files, md5 0613b3d4ef05e613a2b470571498690f
         Help/Q3A_EULA.txt - id's "LIMITED USE SOFTWARE DEMO LICENSE AGREEMENT",
                            the grant an earlier recipe pinned as LicenseRef-id-LIMITED-USE-DEMO
arenas    q3dm1, q3dm7, q3dm17, q3tourney2
gamecode  the demo's own vm modules are version 3: this engine says
          "Architecture doesn't have a bytecode compiler, using interpreter" then
          "ERROR: User Interface is version 3, expected 6", so the payload needs this
          build's own version-6 modules, which is what code/web/client-config.json:29-:35
          prescribes for demoq3 ("Copy baseq3/vm/*.qvm to demoq3/vm/ as the Quake 3 demo
          QVMs are not compatible")
```

## Failure modes

| Symptom | Cause |
| --- | --- |
| `VM_Create on UI failed` after the map has already loaded (`loaded maps/q3dm7.aas`, `found 67 level items`, `AAS initialized`), process gone, shots the 84 KB desktop every time | the vm modules are **loose files** in the game directory rather than inside a pk3 - a measured fact about this engine, not a packaging choice. The menu pass comes through (`File "vm/ui.qvm" found at "<tree>"`, `ui loaded in 2820288 bytes`, `4 arenas parsed`, `6 bots parsed`) and the in-game pass dies: `code/client/cl_ui.c:1119` is where `CL_InitUI`'s call happens and `:1121` where the fatal is raised off a NULL; `vm.c:615` shows the only NULL path left here is `retval < 0` out of `code/qcommon/files.c:1398`'s `FS_FindVM`, and `FS_FindVM:1419` searches a loose game directory only while `!fs_numServerPaks`, which the local server sets at `files.c:4011` (`fs_numServerPaks = c` inside `FS_PureServerSetLoadedPaks`) the moment the demo's pak0 is mounted. So the first `CL_InitUI`, before the server has spoken, finds the loose file, and the second one finds nothing at all. A pk3 keeps them findable in both passes - the pk3 run's console says so at each of the three modules |
| `Quake 3 data files are missing. Please copy pak0.pk3 through pak8.pk3 ...` right after `1384 files in pk3 files`, process gone | the nine-pak demand in `FS_CheckPak0`; that is what patch 0002 relaxes for a `fs_game demoq3` tree. On an unpatched engine, nothing the launcher passes can dodge it |
| nothing draws, and SDL dies before a window exists | this board has no X server: `SDL_VIDEODRIVER=wayland` is what SDL2 has to be pointed at, and `r_preferOpenGLES 1` is what tells the renderer to try the ES API before the desktop one (`sdl_glimp.c:1086` defaults it to -1, and at `:543` that default tries desktop first while the driver answers EGL with `OpenGL_ES` only) |
| a dark picture, all pixels carrying something, `row-lum` minimum near 9 instead of near 45, mid row with 0 brightness jumps, console `MODE: 0, 320 x 240 fullscreen hz:N/A` | `r_mode 0` was shipped: the mode list's first entry is a 320x240 4:3 surface stretched over the 16:9 panel. The seat's answer is `r_mode -1` with the custom pair |
| `SDL_OpenAudioDevice() failed: ALSA: Couldn't open audio device: Device or resource busy` + `Sound initialization failed.` | not this engine: the seat's ALSA sink serves one process at a time, and an older round's game was still alive. Kill the others first. An `snd_pcm_recover underrun occurred` line is the opposite case - the sink opened, played, underflowed and alsa-lib recovered - and is harmless |
| `Failed to load sound` for `assist.wav`, `defense.wav`, `hgrenb1a`/`hgrenb2a` | expected on the demo: team-feedback and grenade-pickup sounds the full release ships and this demo does not, verified against the payload's 249 wavs |
| `couldn't exec autoexec.cfg`, `couldn't exec config.cfg` | expected: the demo ships no config tree |
| a run whose first minute is not the game | the opengl2 renderer opens two `GLSL_InitGPUShaders` passes, each printing `loaded 115 GLSL shaders (48 gen 42 light 25 etc) in 21.85 seconds` (21.37 s for the second), so a proof round waits 85 s after launch |
| a `toggle` probe prints no `Unknown command` for a made-up cvar | the console cannot tell registered from unknown; only `strings` on the binary can |
| a `+set com_standalone 1` that does nothing | overwritten at `files.c:3657` once any pak is mounted, because `com_basegame` answers `baseq3` in this Q3 build |

## Files

`files/` carries the two patches above; `ioquake3_git.bb` builds the client, packs the
vm modules and writes `/usr/bin/ioquake3`. No payload recipe of this family is in this
layer any more - the data commands are in README.md.

## The debs this branch builds (built 2026-11-20 from this tree)

The whole layer was built again from this restructured tree at its own path: `bitbake chocolate-doom fteqw ioquake3 eduke32 dosbox-x openjazz xash3d-fwgs librequake openarena opennowlite` ran green on the build host's `stm32mp25-watermelon-wine-1a` machine, and this recipe's own task chain re-ran inside that run, so the numbers below are this tree's and not the ones an earlier round happened to record. `ioquake3` leaves the build as `1.36+git0+52088b9eae-r0.04`:

| deb | size | md5 |
| --- | --- | --- |
| `ioquake3_1.36+git0+52088b9eae-r0.04_arm64.deb` | 983,552 B | `3ab8269ad488b10e9f1203c52a8149ce` |
| `ioquake3-dbg_1.36+git0+52088b9eae-r0.04_arm64.deb` | 1,711,448 B | `943902daf73211d153cdbacee61494ee` |
| `ioquake3-dev_1.36+git0+52088b9eae-r0.04_arm64.deb` | 1,760 B | `d04365e237133adf5d256a9f29ea7626` |
| `ioquake3-src_1.36+git0+52088b9eae-r0.04_arm64.deb` | 1,208,244 B | `61300545c7c968ee12081125760f582c` |

The base deb installs 3 files: `/usr/bin/ioquake3-gl2` at 1,521,360 B md5 `13678e14a9a2c78fe3c072347d1d4fb6`, the launcher /usr/bin/ioquake3 at 1,521,360 B md5 `13678e14a9a2c78fe3c072347d1d4fb6` and `usr/share/ioquake3/baseq3/vm-pure.pk3`, the engine's own default-pak witness, at 332,078 B md5 `4a3e8eafd700c6e3f9737b07379da89c`. This branch's PKGR is `r0.04` against the seat's `r0.03`, so `dpkg -i` answers over the installed package rather than refusing a same-version install.

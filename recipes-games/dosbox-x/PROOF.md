# dosbox-x - pins, sizes, measured rounds

Numbers are kept here, out of the README, so the README stays one screen of
instructions. Everything below is what the build in the image and the seat rounds
actually did.

## The emulator

```
git://github.com/joncampbell123/dosbox-x.git  protocol=https  branch=master
SRCREV 78c022931f0ad9e567bcaaefc155fc428d9bdc7e  ("Merge pull request #6819 from cimarronm/
        fix-manifest-con-layout", Thu Nov 12 2026 -0800)
LICENSE GPL-2.0-or-later, COPYING md5 13df4b3611f08fada0e7fa65465a92cc
PV 2026.10.01 (configure.ac:2 AC_INIT(dosbox-x, 2026.10.01, ...))   PKGR r0.01
S  ${WORKDIR}/git
```

The pin is the repository's own tip at the moment this recipe was written, and it is
the same commit the repository answers for either name (`git ls-remote HEAD` answers it
for `joncampbell123/dosbox-x`). No tag is used: the tree carries release tags but the
shape the layer's other git recipes use is the commit, and the tag-name form does not
parse (same note the chocolate-doom recipe carries).

The url is the user's path rather than the org's:
`https://github.com/dosbox-x/dosbox-x` answers 401 from this build machine
(`git ls-remote` there prints `fatal: could not read Username for 'https://github'`),
which the fetcher sees as a fatal clone error, while `joncampbell123/dosbox-x` is the
repository that answers the dumb protocol and the org path redirects to.

`COPYING` is GNU's plain v2 text, md5 `13df4b3611f08fada0e7fa65465a92cc`, and it is the
only `COPYING` in the tree; the `-or-later` half is what the sources say, 106 of them
carrying the `or (at your option) any later version` line of the v2 boilerplate, and
nothing in `src/` carries a version 3 text - the grep for `GPL` next to `version 3` over
the tree is empty - so nothing here has to be downgraded to `GPL-2.0-only`. That licence
licenses the emulator and says nothing about the DOS programs it runs.

## Build shape - no patches, every decision is a switch

`inherit autotools pkgconfig`.

```
--enable-sdl2 --disable-opengl --disable-freetype --disable-avcodec
--disable-libslirp --disable-sdlnet --disable-libfluidsynth
DEPENDS   = libsdl2 alsa-lib libpng zlib
RDEPENDS  = libsdl2 alsa-lib libpng
```

- SDL2 is not a preference: `src/gui/sdlmain.cpp:309` is `#if !defined(C_SDL2) ... #error`,
  so a build whose SDL probe failed never gets as far as a link line. `C_SDL2` is defined
  only by the `AM_PATH_SDL2` the tree ships in its own `acinclude.m4` (`:34-:92`), whose
  finding route at `:74` is `pkg-config --exists sdl2` with `sdl2-config` as the other
  answer. There is no `sdl2-config` in this sysroot, because libsdl2's `BINCONFIG`
  (`layers/openembedded-core/meta/recipes-graphics/libsdl2/libsdl2_2023.0.1.bb:32`)
  belongs to the main package and only the `-dev` half populates the sysroot - but the
  sysroot does carry `usr/lib/pkgconfig/sdl2.pc`, which makes the pkg-config branch the
  one that has to answer, and that branch needs the tool itself, which is what the
  `pkgconfig` class at `layers/openembedded-core/meta/classes-recipe/pkgconfig.bbclass:7`
  puts in `recipe-sysroot-native/usr/bin`. `--enable-sdl2` with no probe behind it dies
  with `--enable-sdl2 was specified, but SDL2 was not found`.
- The pixels: DOSBox-X draws the DOS framebuffer into an `SDL_Surface` and hands it over
  with `SDL_UpdateWindowSurface` (`src/gui/sdlmain.cpp:1509`) and
  `SDL_UpdateWindowSurfaceRects` (`:1508` area, the overscan path at `:1523`) and fills
  the border with `SDL_FillRect` (`:1512`) - the same window-surface shape the chocolate-doom
  recipe in this layer relies on, so SDL2's own renderer decides what the upload is and
  **no GL context is ever opened here**. `--disable-opengl` is what keeps GL out of this
  build: the default at `configure.ac:609` is `enable_opengl=yes`, whose body at `:622`
  runs `pkg-config gl`, at `:626` adds `pkg-config gl --libs` and at `:628` falls back to
  `-lGL`, none of which the seat can answer, and `C_OPENGL` is also what pulls the
  desktop-GL includes at `src/gui/sdlmain.cpp:298-:320`.
- `--disable-freetype` is the matching one for text: the default at `configure.ac:980`
  asks `freetype2` through pkg-config at `:989` and adds `-lfreetype` at `:993`, and its
  only use is the TTF fonts that install next to the binary; without it DOS text mode is
  drawn from the compiled-in VGA font and the `FREECG98.BMP` bitmap that is still installed.
- `--disable-avcodec` takes libavcodec out (its only use is recording, and this seat records
  nothing); `--disable-libslirp` and `--disable-sdlnet` take the two network-play backbones
  (`configure.ac:895` and the SDL2_net option) out, which the seat plays single player against;
  `--disable-libfluidsynth` is the matching one for the synth sound font.
- The mouse is SDL2's own: `src/gui/sdlmain.cpp:2690` `SDL_SetRelativeMouseMode(SDL_TRUE)` and
  `:2699` back to `SDL_FALSE` are the captured-pointer pair the DOS mouse uses, so a game
  reading INT 33 gets SDL2's relative deltas, which is what a mouselooking Descent wants.

## The cpu core, which is what the seat's 1.2 GHz Cortex-A35 lives or dies on

`configure.ac:829-:835` is the `aarch64)` case, which sets `AC_DEFINE(C_TARGETCPU,ARMV8LE)`
and `c_targetcpu=arm`, and the dynrec gate at `:1181-:1188` accepts `xarm`, so `C_DYNREC`
is defined and the code generator in `src/cpu/core_dynrec.cpp:165`, the
`#elif C_TARGETCPU == ARMV8LE` branch, is compiled. The **x86** dynamic core is the one that
does *not* come on: its gate at `:1172-:1186` needs x86 or x86_64, so this build gets the
recompiling ARM core and not a translator for the wrong host. Whether that core is fast
enough to play Descent on this board is what the seat round measures, and the answer is in
the rounds below rather than in the build log.

`do_install` installs one binary and three data files (`dosbox-x.reference.conf`,
`dosbox-x.reference.full.conf`, `contrib/fonts/FREECG98.BMP`), which is why
`FILES:${PN} += "/usr/share/dosbox-x"` - `/usr/share` is not in autoconf's default list,
the same shape the ioquake3 and opennow recipes carry. Nothing in this package is a `.so`.

## What the package installs

```
/usr/bin/dosbox-x    15,717,824 B    answers DOSBox-X version 2026.10.01 Commit f1c4414
                                     (Linux SDL2 64-bit)
```

There is no launcher in this package: every game gets its own `-conf`, written by whoever
owns the game, and the two Descent confs on the seat live at `/gamedata/run/`.

## The two Descent tests, and why neither is a recipe here

The licence half passes, on the publishers' own words, verbatim from inside the payloads.
A recipe carrying them would pin these as `LicenseRef`s (e.g.
`LicenseRef-Parallax-Descent-shareware-README.TXT`), the same shape used elsewhere in this
layer for shareware/demo grants.

Descent v1.2's `README.TXT` (51,952 B, md5 `1ae50d747b030750fb37afc093d0bb50`, the member
inside `DESCENT2.SOW`) opens with

```
                   THIS GAME MAY BE FREELY DISTRIBUTED
                        See section 10 for details
```

and its section 10, byte-identical in the v1.0, v1.1, v1.4 and v1.5 READMEs, is

```
Shareware Descent is freely distributable to anyone and everyone so
long as it is distributed in UNMODIFIED FORM and no fees are charged.
```

over the closing banner `DESCENT IS COPYRIGHT (C) 1994 PARALLAX SOFTWARE CORPORATION
/ ALL RIGHTS RESERVED / PUBLISHED BY INTERPLAY PRODUCTIONS, INC.`; `DESCENT.TXT`
(795 B md5 `af1b16e7d9cee7da34afd565e8a9622e`) in the v1.0 set ends with the author's
own instruction `>>>>>>>>>>>> DISTRIBUTE THIS GAME EVERYWHERE! <<<<<<<<<<<<<<`.

The Descent II Interactive Demo v1.0 `README.TXT` (88,065 B md5
`ab80e41750af684992cb2e066bb67856`, CRC `7340BE9C`) opens with

```
                 THIS DEMO IS FREELY DISTRIBUTABLE
                    FOR NON-COMMERCIAL PURPOSES
```

and its operative block is headed `SOFTWARE USE LIMITATIONS AND LIMITED LICENSE` and
grants `You may make copies of the Software for your personal noncommercial home
entertainment use and to give to friends and acquaintances on a no cost
noncommercial basis.` - while also warning `Any permissions granted herein are
provided on a temporary basis and can be withdrawn by Interplay Productions at any
time.`

The download half fails, and that is the whole reason. Both games are Parallax Software's,
published by Interplay - not id's, so there is no idgames-side release of them to fetch
(`ftp.idsoftware.com` is a dead domain, and its Wayback CDX holds 2,053 captures, 0 of them
containing the string "descent"), there is no source release and no native port to build,
so DOSBox-X is the only thing on the seat that can run them. The copies on hand come off an
Internet Archive community item (`Descent_896/desc14sw.zip`, `dcnt12-1.zip`/`dcnt12-2.zip`,
`d2demo10.zip`), which is not an authorized download source, and the layer's rule is that a
recipe needs both halves. So the two games stay an engine test on the seat, installed by hand
on its `/gamedata` mount, and this recipe is what that test is made of.

The `UNMODIFIED FORM` clause also says why a repack would not have fixed the source problem:
`Descent_896/desc14sw.ZIP` (3,003,254 B) ships a `DESCENT.PIG` of 2,509,799 B CRC
`774EE4E1`, which is not the official v1.4 media (5,092,871 B, CRC `DFA31C9A`), so that
package is a modified payload. The seat's own tree carries the official media, reassembled
by hand from the two ARJ volumes.

## Measured on the seat

### Descent 1, shareware v1.4, official ARJ media reassembled on the seat

- the game's own device pick is recorded by the game itself: `DESCENT.CFG` rewritten in the
  mounted DOS directory at 17:40 answers `DigiDeviceID=0xe016` / `DigiPort=0x220` /
  `DigiIrq=7` / `DigiDma=1` at `DigiVolume=4` and `MidiDeviceID=0xa009` at `MidiVolume=4`, so
  the digit stream went to the Sound Blaster at 0x220 and the music to the emulated OPL, both
  at volume 4; `PILOT.PLR` 472 B written in the same run starts `RLPD` and carries
  `LastPlayer=PILOT` in the CFG, which is the pilot-name screen having completed
- the window draws: shots at `/gamedata/installs/shots/d1fast`, and the user at the seat
  confirmed the audio over the speaker while the run was up
- `machine = vgaonly` + `windowresolution = 640,400` is the 320x200 canvas at 2x

### Descent 2, the Interactive Demo v1.0, bound on the seat

- the title screen draws and is measured: 1920x1080 shots at
  `/gamedata/installs/shots/d2fast3`, `d2fast4` and `d2bot`, 182 distinct sampled colors of
  42,625 samples, mean luma 91.9 and stdev 93.0, centre crop 250-253 distinct colors, top
  colors `#03234b`, `#ffd200`, `#ffffff`, `#000000` - the dark blue and the gold are Descent's
  own title palette, and the picture is the game's own `ENTER YOUR PILOT NAME:` /
  `LOADING DATA ... V1.0` under the DOS/4GW window bar
- the same three shots differ by 75-165 pixels, **all of them inside a 17x11 box at the
  weston clock corner**, which is what proved the DOS content was a static title rather than
  an unrendered window: the game sat waiting for a keypress
- measuring had to happen on the build host: the seat has no PIL, so
  `tools/shotstat.py` there answers `ModuleNotFoundError: No module named 'PIL'`

### The DOS/4GW bind, which is what makes the trees runnable at all

The releases ship bindable images (`DESCENT.UBN`, `D2DEMO.UBN`, `SETUP.UBN`) rather than
executables, and `INSTALL.EXE` is a custom installer (`$Id: install.c 1.52 1994/12/29 16:51:17
allender`) that ARJ-unarchives the `.SOW` volumes and then binds, so the runnable EXE exists
only after the bind. The bind pair is byte-identical across every D1 and D2 payload
(`4GWBIND.EXE` 45,551 B md5 `b0afd738915cd8651929a329d74b3afb`, `4GWPRO.EXE` 242,724 B md5
`6bb6be039f854052b2bf8d580382fdf1`), and the seat ran it in the emulator:

```
4GWBIND.EXE 4GWPRO.EXE D2DEMO.UBN D2DEMO.EXE > BIND.OUT
  -> BIND.OUT 39 B, "4GWPRO.EXE + D2DEMO.UBN => D2DEMO.EXE"
  -> D2DEMO.EXE 1,297,237 B md5 91a2d936276aa23f1789aec692da2e5d
```

the same bind on `DESCENT.UBN` 835,841 B left `DCNTSHR.EXE` 1,053,605 B md5
`0571c536290e5f078b4aee77789d5635` and `SETUP.EXE` 441,525 B, and the release's own
`DESCENT.BAT` (163 B, `rem Batch file to run shareware version of Descent.`) is what runs
that binary - `descent.bat -lowmem` is the line in `d1fast.conf`.

`D2.BAT` is `@echo off` / `rem Batcatch file to run Descent 2 Interactive Demo` /
`if exist *.swp del *.swp` / `eregcard` / `d2demo %1..%9`, and the release ships `D2DEMO.UBN`
only: there is no `D2DEMO.EXE` in the official package until the bind above makes one, which
is why the seat tree looks the way it does.

## The fill route, which is what the two Descent gallery pairs are taken at

Two levers make the DOS picture the seat's whole panel, and both were measured rather than
assumed:

- `[sdl] maximize = true` makes the window the panel: the emulator reads that key at
  `src/gui/sdlmain.cpp:3799` and hands it to SDL as `SDL_WINDOW_MAXIMIZED` at
  `src/gui/sdlmain.cpp:1881`, so SDL2's wayland backend resizes the window to the desktop, and
  the run logs `surface consider=1920x1048 final=1920x1048` (`src/output/output_surface.cpp:171`
  and `:473`, the `LOG_MSG("surface consider=%ux%u final=%ux%u", ...)` pair). The seat's own
  scene graph agrees - the same `weston-debug scene-graph 400ms` that reads the title readout
  back gives the window's geometry as `(0, 0) -> (1920, 1048)` - because the panel is 1920x1080
  and its bottom 32 rows, `1048..1079`, are the wallpaper's own gold `(255,210,0)` panel that
  the compositor keeps outside the window
- `[render] aspect = true` plus `scaler = xbrz_bilinear forced` with `xbrz fixed scale factor = 2`
  paints into the 4:3 box `aspectCorrectFitClip` computes inside that window - `output_surface.cpp:147`,
  `:366` and `:449` are the three calls, each of them guarded by `render.aspect` - which is 1397 px
  of the 1920, since 1048 x 4/3 is 1397, and the measured captures agree exactly: both gallery
  shots' non-black extent is columns `261..1657` and rows `0..1047`, and the other 523 columns
  above the panel are pure black bars at the sides
- the scaler is the part that fills, and the emulator's own source says why: the `scaler = none`
  presentation is `SDL_BlitSurface(sdl.blit.surface, nullptr, sdl.surface, &sdl.clip)` at
  `output_surface.cpp:727`, an unscaled blit, so the DOS raster keeps its own size inside the clip,
  while the scaler's path scales first - `xBRZ_Render` at `:671`, then `xBRZ_PostScale` into the
  clip at `:679` and the aspect path's at `:708`. The bake-off that measured it, three confs that
  differ only in how `aspect` is spelled while all three carry `scaler = none`:

  | conf | `[render] aspect` | what the DOS side was showing | full-window shot | non-black box |
  | --- | --- | --- | --- | --- |
  | `d2k` | `bilinear` | Descent 2's IPX card | 62,623 B md5 `cc47777d38d2b5d1bc5a18e43e266748` | 640x400 at columns `640..1279`, rows `324..723` |
  | `d2l` | `nearest` | the same card | 62,682 B md5 `0be9d7104c5bfd528dff954b37352df2` | the same 640x400 box |
  | `d2m` | `true` | the briefing blueprints | 36,283 B md5 `6454364be8dcec9375db0592d30105c0` | 618x434 at columns `656..1273`, rows `303..736` |

  all three print the same `surface consider=1920x1048 final=1920x1048`, so the window was the panel
  in all three runs, and all three still drew about 640 px of picture in the middle of 1920

What is left once the window fills is what a presented frame costs, and that is the only lever
the two games respond to:

- `[cpu] cycles = max` asks the emulator to raise the count until a whole game loop fits inside
  the frame, and at the fill route one never does, so `cycleup = 10` / `cycledown = 20` percent
  walk it down to its 200/ms floor and both games stay inside their own boot films. The frameskip
  ladder that measured it, same fill conf otherwise, three confs at `cycles = max`:

  | conf | `[render] frameskip` | readout over the run |
  | --- | --- | --- |
  | `d2b` | 0 | `200 cycles/ms`, FPS 1-5, 14-20%/RT |
  | `d2h` | 1 | `200 cycles/ms`, FPS 3-5, 21-49%/RT |
  | `d2i` | 2 | `200 cycles/ms`, FPS 3-6, 24-58%/RT |

  every shot of that round stayed on a boot card or a loading screen, and the confs that got past
  them are the ones that stop self-tuning: the fill confs carry an explicit `cycles = 10000` /
  `20000`, which reads `10000 cycles/ms` / `20000 cycles/ms` and puts the DOS side at 100%/RT
- the gallery pair is then read off the emulator's own readout rather than off the picture:
  `[sdl] showdetails = true` writes `cycles/ms`, FPS and %RT into the SDL window's title, which
  the seat's scene graph carries, so a DOS side parked at a menu reads `FPS 0, 100%/RT` with
  byte-identical shots - the emulator's `changedLines` early-out at `output_surface.cpp:733`
  and `:750` returns before any `SDL_UpdateWindowSurface`, so the compositor is handed the same
  bytes - while in-mine playback reads FPS > 0 and a different md5 per probe

The pairs the fill route produced:

- Descent 2's pair is at `/gamedata/installs/shots/gallery/descent2/{start,idle}.png`, taken at
  `/gamedata/run/d2p.conf` (the fill conf above, at `cycles = 10000` and `frameskip = 0`). `start.png`
  772,473 B md5 `d8ba8e2b57427bf8681fb69428910320` reads FPS 3 at 47%/RT and carries the Interactive
  Demo's own overlay `DEMO PLAYBACK (2% DONE)`; `idle.png` 868,803 B md5 `7642f10482aa11f8bf0ffdfd01dae687`
  reads FPS 4 at 39%/RT and carries `DEMO PLAYBACK (30% DONE)`. The percentage is the game's own
  drawing, not something the round drew, so the two shots being 2% and 30% through is the proof
  that they are different in-game frames, with nothing pressed between the two
- a second Descent 2 pair at `frameskip = 1` and `cycles = 20000` (`/gamedata/run/d2r.conf`) reads
  FPS 4/5 at 20-21%/RT at 830,117/827,567 B and sits in the mine the same way; it is kept under its
  probe tag rather than in the gallery name
- Descent 1's DOS side is told apart from the seat by its own screens, which each cost the
  compositor a different number of bytes and only the animated ones read FPS > 0: the loading
  screen's bar at FPS 1-2 and ~617 KB, the `SELECT PILOT / <CREATE NEW> PILOT` menu at FPS 0 and
  ~558 KB, and the main menu (`DESCENT / NEW GAME / LOAD GAME... / ... / VIEW DEMO... / ... / QUIT`)
  completely still at FPS 0, 100%/RT and ~903 KB. The walk that reaches the mine is one Enter per
  state: Enter until the main menu, one Enter to `DIFFICULTY LEVEL / TRAININEE..INSANE` with ROOKIE
  gold at ~831 KB and FPS 0, one Enter to the mission's briefing film - the space station with green
  captions, ~660-700 KB and FPS 3-6 - and the Escape that jumps that film into the mine, since the
  release's own `REFCARD.TXT` says ESCAPE is `Main menu/Abort game`. Descent 1's tree holds no demo
  file at all (`DESCENT.HOG` 2,339,773 B and `DESCENT.PIG` 5,092,871 B are the maps and the graphics,
  and the only `.DEM` on the seat is Descent 2's `D2DEMO.DEM` 355,173 B), so `VIEW DEMO` is not a
  route here and `NEW GAME` is
- inside the mine the ship is flown with held keys, which is what makes the DOS side animate at all:
  ydotool's press/sleep/release is a held key, and the release's own `REFCARD.TXT` says ARROW KEYS
  are the ship's rotations, A is forward, Z reverse, Q/E bank, CTRL fires primary and SPACEBAR
  secondary, so the pair is taken around a held Right arrow

### What makes a presented frame cost, measured against the window

Four confs on one binary, all four driven to Descent 1's own mine and flown with the
same held Right arrow, all four read off the emulator's own title and off the seat's
own `/proc` record:

| conf | the window weston's scene graph gives | `[render]` | FPS in the mine | %RT | the emulator's own CPU |
| --- | --- | --- | --- | --- | --- |
| `d1p.conf` | `(0, 0) -> (1920, 1048)` | `scaler = xbrz_bilinear forced` at `xbrz fixed scale factor = 2`, `frameskip = 0` | 3-5 | 21-55% | 90-103% of one core |
| `d1f.conf` | the same `(0, 0) -> (1920, 1048)` | the same xbrz, `frameskip = 2` | 3-5 | 24-54% | - |
| `d1win.conf` | `(586, 12) -> (906, 212)`, a 320x200 window | the same xbrz | 7-19 | 87-100% | 92-98% of one core |
| `d1s.conf` | the same `(0, 0) -> (1920, 1048)` | `scaler = none` | 10-20 | 99-101% | 92-94% of one core |
| `d1win.conf` of the 15:00 round | a 320x200 window whose own rect that round never captured - `weston-debug scene-graph` printed the `View` lines and the title, no geometry, and the run's log answers `LOG: Configured windowposition: -` | the same xbrz, at `maximize = false` + `windowresolution = 640x400` | 5-10 steady, 33 and 38 at the two shots | 69-96% | - |

The maximised window is not slow because the panel is big. `d1s.conf` keeps the window
the whole panel and takes the scaler out, and it runs 10-20 fps with the DOS side at
99-101% of real time, so `SDL_UpdateWindowSurface` of a 1920x1048 window and weston's
own repaint of it are not what limits this route. What limits it is the scaler's
destination, and the tree says where that destination comes from: with `sdl_xbrz.enable`
`OUTPUT_SURFACE_SetSize` sets `sdl.clip.w`/`sdl.clip.h` to the window itself at
`output_surface.cpp:146-:149`, narrows it to the 4:3 box with `aspectCorrectFitClip` at
`:147`, and `xBRZ_PostScale` at `:679` is handed exactly that `clipWidth, clipHeight` as
its destination. The captures agree on both sides of the box: the maximised runs bound
columns `261..1657` and rows `0..1047`, and inside the 320x200 window the picture bounds
columns `27..292` and rows `0..199`, which is 266 px - 200 x 4/3 is 266.67 - so the
scaler paints 1,464,056 px per presented frame at the panel against 53,200 px in the
window, 27.5 times as many.

The windowed conf's own log answers the window's size rather than an ask: piped through
`tools/caplog.py`, `/gamedata/logs/surface-win.log` carries `LOG: surface consider=720x540
final=720x540` for the DOS text mode and `LOG: surface consider=320x200 final=320x200`
once Descent switches the DOS side to Mode 13, and it also carries the emulator's own
complaint about that window's format, `LOG: SDL: Current window pixel format:
SDL_PIXELFORMAT_RGB888` / `LOG: SDL: You are running in 24 bpp mode, this will slow down
things!`. The same log prints the emulator's own hint at a lever that has not been
measured here: `The 'scanline render on demand' option is available and may provide a
modest boost in video render performance if set to true`.

What the area ratio does not explain, the same table does: 27.5 times the pixels, 3-4
times the frames. The rest is fixed cost that both routes pay, and it is paid in the same
place - the xBRZ source is the DOS raster, `xBRZ_Render` at `:671` scales the 320x200
Mode 13 canvas up to the 640x400 buffer at `scale_factor = 2` in both confs, and
Descent's own world render happens inside the emulator at 320x200 before any of this.
That fixed half is nearly the whole core on its own: with the scaler out, the DOS side
keeps 99-101% of real time and still costs 92-94% of one core, so the 24 bpp window blit
plus the 1.46M px `xBRZ_PostScale` have only the few percent left over to them, and that
is what hands the DOS side 21-55% of real time rather than 100%.

`[render] frameskip` is not a lever on this route: `sdlmain.cpp:1139` takes the key and
answers `(void)frameskip;//UNUSED`, and the measured `frameskip = 2` conf reads 24-54% of
real time against the 21-55% the same window reads at `frameskip = 0`. The one lever the
fill route has is whether the scaler paints, and `scaler = none` is the only spelling of
it that keeps the DOS side at real time while `maximize` still fills the panel - it leaves
the DOS raster at its own size in the middle of the panel, which is why the gallery pairs
above are taken with the scaler on.

The pairs these routes left, all four shots in the mine with the emulator's title
animating: `/gamedata/installs/shots/ab/full/{start,idle}.png` at 519,292/519,884 B md5
`845a77a6c8e42541dd7ee2d9ece85bd0`/`0780203e3d5c6fcdc9f1a3243a723983` reading FPS 5 at 33%/RT
and FPS 4 at 27%/RT, and `/gamedata/installs/shots/ab/win/{start,idle}.png` at 175,536/175,331 B
md5 `3e02e9f718887fb1a5b446a0dbbdca7a`/`02ac55864581eea741545de47c442616` reading FPS 17 at 98%/RT
and FPS 23 at 98%/RT. The emulator's own record reads rss 207,704/203,592 kB on the two xbrz
confs and 137,900 kB on the `scaler = none` one, so the xbrz scaler's buffer set costs about 66 MB
of the seat's resident pool, and the window's own size barely moves that figure: the buffer set is
the scaler's source-side one, not a destination-sized one.

The route that used to make these two dumps is superseded by the one above, and its measurement
is kept here: a windowed capture (`/gamedata/run/d1w.conf`, `d2w.conf`, a 640x417 `surface` window)
from which `tools/doswindow.py` cut the emulator's own window out of the desktop capture using the
wallpaper's navy `(3,35,75)` and gold `(255,210,0)` fields and below DOSBox-X's own menu-strip greys
`(63,63,63)`, `(31,31,31)` and `(191,191,191)` 17 px inside the window top - both games bounded the
same 640x400 DOS picture, at `1142,295..1781,694` for Descent 1 and `969,439..1608,838` for Descent
2 - and stretched that crop to 1920x1080, which is the emulator's own `aspect = false` look. What
replaced it is the `maximize = true` measurement above, which needs no after-the-fact crop at all.

### The window's own rate, read again on the afternoon of Nov 21 2026

The windowed route was measured a second time, by its own driver `/gamedata/run/round-nov21-d1window.sh`,
against the fill conf with only two keys moved: `maximize = false` and `windowresolution = 640x400`
(the DOS raster is 320x200 and the xbrz fixed scale factor is 2, so 640x400 is that raster scaled at
1:1). `output = surface`, `scaler = xbrz_bilinear forced`, `machine = vgaonly`, `core = dynamic` and
`cycles = 10000` are byte-identical to the maximised conf, so the only variable is the presentation
surface. The run's own log says the ask is ignored exactly as the earlier windowed row says -
`LOG: surface consider=720x540 final=720x540` for the DOS text mode, then `LOG: surface consider=320x200
final=320x200` once Descent switches the DOS side to Mode 13 - beside `LOG: Configured windowposition: -`
and the same `SDL_PIXELFORMAT_RGB888` / `24 bpp` complaint. What this round adds is the **rate** at that
window, read off the emulator's own title while the ship sat in its mine: the parked menu read
`FPS 0, 100%/RT`, the Enter ladder and the two Escapes that jump the briefing film put the ship in the
mine and the readout went to `FPS 38, 94%/RT`, then `FPS 33, 69%/RT` at the moment of `start.png` and
`FPS 8, 93%/RT` at the moment of `idle.png`, with the six steady probes that followed reading `FPS 7/7/9/5/10/9`
at 96/93/88/95/92/91 % of real time. At this window the DOS side is not starved by the scaler - 88-96 %
of real time against the 21-55 % the maximised window gets at the same scaler - which is what the 53,200
px against 1,464,056 px of that destination box, measured above, predicts.

| Shot | colours | non-black | row lum | mid-row jumps | md5 |
| --- | --- | --- | --- | --- | --- |
| `start.png` 187,924 B | 7,309 | 2,052,182 | 30.0-224.0 | 199 | `f22561cd80444dc0d8a6523903948a03` |
| `idle.png` 193,251 B, +17.0 s, nothing pressed | 5,559 | 2,059,504 | 30.0-224.0 | 131 | `e7abcd84d47256c0c785a08b90b70848` |

| Pair | identical | >8 | columns / rows moved |
| --- | --- | --- | --- |
| `start` vs `idle` | 2,021,806 (97.5 %) | 50,494 (2.4 %) | 26 / 1,920 and **0** / 1,080 |

Two honest notes on that pair. It is a capture of the whole panel rather than a crop of the window, and
the shot's own colour counts say so: the wallpaper's navy `(3,35,75)` covers 1,281,530 px of the 2,073,600
and its gold `(255,210,0)` covers 413,227 px, so the 320x200 window sits somewhere inside those fields.
This round never captured the window's own rect - `weston-debug scene-graph 400ms` printed the `View`
lines with the window's title and no geometry for it - which is why the row above states the two
`consider=` sizes rather than a rectangle. And **nothing at all** was pressed between the two shots, so
the mine's own animation is what moved 2.5 % of the panel and 26 of 1,920 columns of the coarse shape
with **not one row** of it moved, while the readout went from FPS 33 to FPS 8: unlike the earlier rounds,
which flew the ship with a held Right arrow to make a pair animate at all, this pair is the game's own
motion. It stays under its probe tag `/gamedata/installs/shots/window/d1win/` rather than a gallery name,
because a 320x200 window in the middle of the panel is not the whole-panel fill the gallery rule asks for.

## Failure modes

| Symptom | Cause |
| --- | --- |
| `E_Exit: Can't init SDL x11 not available`, and no window at all | `SDL_VIDEODRIVER=wayland` was left unset: this board carries no X server. The run that does start logs `LOG: SDL: version 2.30.1, Video wayland, Audio alsa` |
| a log on `/gamedata` that grew to 5.5 GB | at the end of a non-interactive autoexec the emulator asks `You are currently running a program or game.` / `Are you sure to quit anyway now?` / `y/n:` and nothing answers, so it printed 4,500 pairs before the run was stopped - about 4.7 MB in the first 400 KB and 5.5 GB in the file that filled the whole mount while still open on fds 1 and 2. Use `>/dev/null 2>&1`, or `/gamedata/tools/caplog.py <cap>` |
| `LOG: "openglnb" is not a valid value for variable: output.` | `output = surface` is the only video system this build accepts - `--disable-opengl` took the GL one out, and the seat has no desktop GL to answer it with anyway |
| a window that answers the DOS raster's own size whatever `windowresolution` or `windowmaximize` is asked of it, and scalers that print `surface consider=` against that same size | those keys need an output that does hardware scaling, and `output = openglnb` is refused on this build (`--disable-opengl`), so a `maximize = false` + `windowresolution = 640x400` conf answers 320x200 in every run - the seat's scene graph gave `(586, 12) -> (906, 212)`, `(249, 417) -> (569, 617)` and `(362, 475) -> (682, 675)`, and the run's own log answers `LOG: surface consider=320x200 final=320x200` - which leaves only two presentation sizes on this route: the 320x200 window and the maximised 1920x1048 one. What does make the window the whole panel on a `surface` build is `[sdl] maximize = true`, and the runs that set it print `surface consider=1920x1048 final=1920x1048` instead - see the fill route above. `fullscreen = true` still segfaults this weston, so every conf here ships `fullscreen = false` |
| a maximised window that runs 3-5 fps in the mine and reads 21-55%/RT, while the same maximised window at `scaler = none` runs 10-20 fps at 99-101%/RT | the fill is done by the xBRZ scaler, and its destination is the window's own 4:3 clip box - `xBRZ_PostScale` at `output_surface.cpp:679` is handed `sdl.clip.w`/`sdl.clip.h`, which `:146-:149` and `aspectCorrectFitClip` at `:147` set to 1,464,056 px at the panel against 53,200 px in the 320x200 window - so that software blit eats the little of one core the DOS side does not already cost, and the DOS side is handed 21-55% of real time. `[render] frameskip` is not the lever: `sdlmain.cpp:1139` answers `(void)frameskip;//UNUSED` and the measured `frameskip = 2` conf reads 24-54% against the 21-55% at `frameskip = 0`. The fill confs therefore carry the scaler and accept 3-5 fps, or take the scaler out and leave the DOS raster in the middle of the panel |
| a Descent gallery pair that is byte-identical and reads `FPS 0, 100%/RT` | the DOS side is parked, not the window: both releases wait at their own `SELECT PILOT / <CTRL-D> DELETES / <CREATE NEW> MIKE` menu, and DOSBox-X's `changedLines` early-out in `EndUpdate` stops redrawing completely when the DOS screen has not changed, so the compositor keeps being handed the same bytes and the readout reads FPS 0 at full real time. The earlier version of this row - no synthetic keystroke reaching the window at all - is what a later round proved wrong: the seat runs `ydotoold --socket-path /tmp/.ydotool_socket --socket-ownership 0666` (pid 23672) next to `seatd` (pid 831), and `ydotool key 28:1 28:0` (Enter), `57:1 57:0` (Space), `1:1 1:0` (Escape) and `click 1` do reach the window - they are what cleared the IPX card, created the pilot and jumped the briefing film into the mine. What the readout is for is telling these two states apart, since a loading bar animates at FPS > 0 too and a mine animates at FPS > 0 too, and the only difference visible from the seat is the game's own overlay |
| the window fills but the DOS side never leaves its own boot films, and the readout says `200 cycles/ms` | `[cpu] cycles = max` asks DOSBox-X to raise the count until a full game loop fits inside the frame, and at the fill route a loop never fits, so `cycleup = 10` / `cycledown = 20` percent walk it down to its 200/ms floor and the DOS side gets 14-58% of real time. The fill confs therefore set an explicit count (`cycles = 10000` / `20000`), which is what put the DOS side at 100%/RT and got both games to their mines |
| `SETUP.EXE` will not draw on a `vgaonly` machine | the Epic-style setup wants a screen wider than 320x200 to draw its own; `d1setup.conf` is the one run that needs `svga_s3` |
| `--enable-sdl2 was specified, but SDL2 was not found` | the `AM_PATH_SDL2` fallback wants `sdl2-config`, which libsdl2's main-package-only `BINCONFIG` leaves out of this sysroot; the pkg-config branch is the one that answers, and it needs the `pkgconfig` class's tool |
| measuring says `ModuleNotFoundError: No module named 'PIL'` | the seat has no PIL; shot measurement runs on the build host with `tools/shotstat.py` |
| `ydotool: Unknown command: mouseclick` and no key reaching the window at all | this seat's ydotool has no `mouseclick` subcommand - the one that lands is `ydotool click 1` - and a reboot wipes `/tmp`, which is where its daemon's socket lives, so `ydotoold --socket-path=/tmp/.ydotool_socket --socket-perm=0666` has to be restarted beside `seatd` and `YDOTOOL_SOCKET=/tmp/.ydotool_socket` exported by whoever drives the window |
| `already-stripped`-style surprises in `do_install` | none here: this build is not pre-stripped, and `do_install` installs one binary and three data files |

## Files

`files/` is empty on purpose: the SDL2 probe, the GL-free pixel path and the ARM cpu core are
all switches the tree's own `configure.ac` already has. `dosbox-x_git.bb` builds the emulator.

## What this recipe's own round settled, and what is still open

- [x] one engine for both games, one bind pair, both trees mounted from `/gamedata`
- [x] the Wayland/GL-free pixel path: no GL context is opened by this emulator at all,
      `--disable-opengl` and the SDL window-surface blit are what the seat pays for
- [x] the two Descent gallery dumps are taken at the seat's whole panel by the engine itself:
      `[sdl] maximize = true` makes the window 1920x1048 and logs `surface consider=1920x1048
      final=1920x1048`, `[render] aspect = true` plus `scaler = xbrz_bilinear forced` at
      `xbrz fixed scale factor = 2` paints into the 1397x1048 box `aspectCorrectFitClip` computes
      inside it, and `[cpu] cycles` is an explicit count so the DOS side keeps its 100%/RT; `aspect
      = false`, `windowresolution`, `windowmaximize`, `output = openglnb`/`openglpp`, `videodriver
      = x11` and `fullscreen = true` are still the dead routes measured here, and so is `[render]
      frameskip`, which `sdlmain.cpp:1139` answers `(void)frameskip;//UNUSED` to - the 640x417 crop
      route that used to make these dumps keeps its measurement above
- [x] the keystroke route exists on the seat with nobody at a keyboard: `seatd` (pid 831) and
      `ydotoold --socket-path /tmp/.ydotool_socket --socket-ownership 0666` (pid 23672) are both on
      it, and `ydotool key 28:1 28:0` (Enter), `57:1 57:0` (Space), `1:1 1:0` (Escape) and `click
      1` do reach the emulator's window - they are what cleared the IPX card, created the pilot and
      jumped both briefing films into their mines
- [ ] mouselook proof for the two Descents: the emulator's side is already in place (`SDL_SetRelativeMouseMode`
      plus `[sdl] mouse_emulation = locked`), and what is measured so far is the ship being flown with
      held arrow keys rather than with the mouse
- [x] a playback route to in-game frames, for Descent 2: its Interactive Demo plays its own recording
      and draws its own `DEMO PLAYBACK (n% DONE)` overlay, so the gallery pair above is a playback and
      not a flight, and the release's own `D2DEMO.DEM` 355,173 B is that recording. The same route is
      not available for Descent 1, whose tree holds no demo file at all, so its mine comes from `NEW GAME`
- [ ] a `PRINT SCREEN` route, which would leave the proof inside the mounted DOS tree rather than in
      a compositor capture: the release's section 6 `DEMOS & SCREENSHOTS` says demo files play back and
      that the key writes `SCREENxx.PCX` into the DOS directory the game runs from, and Descent 1's own
      `REFCARD.TXT` lists `print scrn` as `save screen shot` - unmeasured on the seat so far
- [ ] a fill that plays at a usable rate: at the scaler the maximised window is 3-5 fps in the mine at
      21-55% of real time, and this route answers only two sizes - the DOS raster's own 320x200 window
      and the maximised 1920x1048 one, since the windowed run's own log says `consider=320x200` against
      a `windowresolution = 640x400` ask - so a middle size needs an output that does hardware scaling,
      and `--disable-opengl` took that output out of this build. What the same afternoon read back is
      which of the two sizes does play: at the same xbrz scaler the 320x200 window keeps the DOS side at
      69-96 % of real time and reads FPS 5-10 steady with 33 and 38 at the two moving shots against the
      3-5 fps and the 21-55 % the maximised window gets at that scaler, so the rate is the window's and
      the fill is the panel's, and the pair that window left is a whole-panel capture at
      `/gamedata/installs/shots/window/d1win/` with the window's own rect never read. Two levers the tree
      still offers have not been measured on the seat: the emulator's own `scanline render on demand` hint
      and the window's 24 bpp `SDL_PIXELFORMAT_RGB888` surface, which the run itself calls out as
      `You are running in 24 bpp mode, this will slow down things!`

## The debs this branch builds (built 2026-11-20 from this tree)

The whole layer was built again from this restructured tree at its own path: `bitbake chocolate-doom fteqw ioquake3 eduke32 dosbox-x openjazz xash3d-fwgs librequake openarena opennowlite` ran green on the build host's `stm32mp25-watermelon-wine-1a` machine, and this recipe's own task chain re-ran inside that run, so the numbers below are this tree's and not the ones an earlier round happened to record. `dosbox-x` leaves the build as `2026.10.01-r0.01`:

| deb | size | md5 |
| --- | --- | --- |
| `dosbox-x_2026.10.01-r0.01_arm64.deb` | 6,021,732 B | `e1b8c5aaa972b38cdbfaf2b96c10032f` |
| `dosbox-x-dbg_2026.10.01-r0.01_arm64.deb` | 20,667,600 B | `e02da356db4d192e67f29e76728fc555` |
| `dosbox-x-dev_2026.10.01-r0.01_arm64.deb` | 3,288 B | `712c28a92ef01c578a88eed31ba59493` |
| `dosbox-x-src_2026.10.01-r0.01_arm64.deb` | 7,498,784 B | `d5259e9f9c15a2700d6407c182ea057f` |

The base deb installs 4 files: `/usr/bin/dosbox-x` at 15,717,824 B md5 `2531964ba92baa2a0eb460328bb28651` plus the share's reference confs and its `FREECG98.BMP`. The seat's pin is the same version, so the install replaces it in place.

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

## The gallery dump of the two Descents, and why it is a crop

- the seat's gallery pairs live at `/gamedata/installs/shots/gallery/descent1-demo` and
  `descent2-demo`, taken from the confs `/gamedata/run/d1w.conf` and `d2w.conf` with the
  briefing films skipped by a walk of ten/twelve Enters plus one Escape - the Escape is what
  jumps the flight-officer film into the mine cockpit the game opens play on
- the engine cannot fill the seat's screen, so the layer's `shots/descent1-demo.png` and
  `shots/descent2-demo.png` are those same captures cropped to the emulator's own picture
  and stretched to 1920x1080: the window answers **640x417 whatever is asked of it**, since
  `output = surface` is the only video system this build accepts (`output = openglnb` answers
  `LOG: "openglnb" is not a valid value for variable: output.`), `windowresolution` needs an
  output that does hardware scaling, `windowmaximize` is not a key of this build at all,
  `normal2x = normal3x forced` and `normal2x = xbrz3 forced` with `autofit = true` still print
  `surface consider=640x417 final=640x417`, and `videodriver = x11` has no X server on the seat
  to use
- `tools/doswindow.py` recovers the picture from the capture instead of asking the compositor
  to change size: the window's left and right edges are the widest run of pixels that are
  neither the wallpaper's navy `(3,35,75)` nor its gold `(255,210,0)`, its height is the
  gap-tolerant run down both side columns, and the DOS picture starts 17 px below the window
  top, at the first row that is not DOSBox-X's own menu-strip greys `(63,63,63)`, `(31,31,31)`
  and `(191,191,191)` - both games bound the same 640x400, at `1142,295..1781,694` for Descent
  1 and `969,439..1608,838` for Descent 2
- the crop is stretched to fill 1920x1080, which is the emulator's own `[render] aspect = false`
  at fullscreen: fill the frame, no black bars, and it is what those two confs set

## Failure modes

| Symptom | Cause |
| --- | --- |
| `E_Exit: Can't init SDL x11 not available`, and no window at all | `SDL_VIDEODRIVER=wayland` was left unset: this board carries no X server. The run that does start logs `LOG: SDL: version 2.30.1, Video wayland, Audio alsa` |
| a log on `/gamedata` that grew to 5.5 GB | at the end of a non-interactive autoexec the emulator asks `You are currently running a program or game.` / `Are you sure to quit anyway now?` / `y/n:` and nothing answers, so it printed 4,500 pairs before the run was stopped - about 4.7 MB in the first 400 KB and 5.5 GB in the file that filled the whole mount while still open on fds 1 and 2. Use `>/dev/null 2>&1`, or `/gamedata/tools/caplog.py <cap>` |
| `LOG: "openglnb" is not a valid value for variable: output.` | `output = surface` is the only video system this build accepts - `--disable-opengl` took the GL one out, and the seat has no desktop GL to answer it with anyway |
| `windowresolution` / `windowmaximize` do nothing to the window size | they need an output that does hardware scaling; on a `surface` build the window answers 640x417 whatever is asked, and the scalers still print `surface consider=640x417 final=640x417`. `fullscreen = true` segfaults this weston - these confs ship `fullscreen = false` |
| no in-game frames of either Descent, and no mouselook proof | needs a human at the seat's attached USB keyboard (`Dell USB Keyboard` = `/dev/input/event1`, major 13 minor 65; `USB Optical Mouse` = `/dev/input/event0`). No synthetic keystroke reaches this window at all: the `ydotool` OE package (cortexa35) lists `/usr/bin/ydtoold` in `dpkg -L` but the daemon is not on the filesystem, `/proc/kallsyms` (5,090,979 B) has 0 matches for `uinput` so the vendor kernel has nothing for a daemon to attach to, and the socket `/home/weston/.ydotool_socket` is there but `ydotool key 28:1 28:0` answers `failed to connect socket ... Connection refused` with rc=2. The 86 B real-mode `.COM` built for this round to poke the emulated BIOS key ring instead (md5 `ddbfea7dfe5d3ced5096f2d18920d6e0`, verified by `objdump -D -b binary -m i8086`, since deleted from the seat) reached nothing, because DOSBox-X's emulated DOS takes buffered input from its own queue rather than from the BIOS ring |
| `SETUP.EXE` will not draw on a `vgaonly` machine | the Epic-style setup wants a screen wider than 320x200 to draw its own; `d1setup.conf` is the one run that needs `svga_s3` |
| `--enable-sdl2 was specified, but SDL2 was not found` | the `AM_PATH_SDL2` fallback wants `sdl2-config`, which libsdl2's main-package-only `BINCONFIG` leaves out of this sysroot; the pkg-config branch is the one that answers, and it needs the `pkgconfig` class's tool |
| measuring says `ModuleNotFoundError: No module named 'PIL'` | the seat has no PIL; shot measurement runs on the build host with `tools/shotstat.py` |
| `already-stripped`-style surprises in `do_install` | none here: this build is not pre-stripped, and `do_install` installs one binary and three data files |

## Files

`files/` is empty on purpose: the SDL2 probe, the GL-free pixel path and the ARM cpu core are
all switches the tree's own `configure.ac` already has. `dosbox-x_git.bb` builds the emulator.

## What this recipe's own round settled, and what is still open

- [x] one engine for both games, one bind pair, both trees mounted from `/gamedata`
- [x] the Wayland/GL-free pixel path: no GL context is opened by this emulator at all,
      `--disable-opengl` and the SDL window-surface blit are what the seat pays for
- [x] the two Descent gallery dumps fill the seat's frame: `tools/doswindow.py` crops the
      verified windowed captures to the emulator's 640x400 DOS picture and the crop is
      stretched to 1920x1080, since every route that would have made the engine fill the
      frame itself was measured dead here (`output = openglnb` refused, `windowresolution`
      and `windowmaximize` inert on a `surface` build, scalers leaving the 640x417 surface,
      no X server for `videodriver = x11`, `fullscreen` segfaulting weston)
- [ ] mouselook proof for the two Descents, once a human is at the seat's keyboard: the
      emulator's side is already in place (`SDL_SetRelativeMouseMode`), what is missing is a
      keypress to start a level
- [ ] a playback route to in-game frames with no keystroke injection at all: the release ships
      its own recording, `D2DEMO.DEM` 355,173 B, and the game's section 6 `DEMOS &
      SCREENSHOTS` says demo files play back and that the `PRINT SCREEN` key writes
      `SCREENxx.PCX` into the DOS directory the game runs from

## The debs this branch builds (built 2026-11-20 from this tree)

The whole layer was built again from this restructured tree at its own path: `bitbake chocolate-doom fteqw ioquake3 eduke32 dosbox-x openjazz xash3d-fwgs librequake openarena opennowlite` ran green on the build host's `stm32mp25-watermelon-wine-1a` machine, and this recipe's own task chain re-ran inside that run, so the numbers below are this tree's and not the ones an earlier round happened to record. `dosbox-x` leaves the build as `2026.10.01-r0.01`:

| deb | size | md5 |
| --- | --- | --- |
| `dosbox-x_2026.10.01-r0.01_arm64.deb` | 6,021,732 B | `e1b8c5aaa972b38cdbfaf2b96c10032f` |
| `dosbox-x-dbg_2026.10.01-r0.01_arm64.deb` | 20,667,600 B | `e02da356db4d192e67f29e76728fc555` |
| `dosbox-x-dev_2026.10.01-r0.01_arm64.deb` | 3,288 B | `712c28a92ef01c578a88eed31ba59493` |
| `dosbox-x-src_2026.10.01-r0.01_arm64.deb` | 7,498,784 B | `d5259e9f9c15a2700d6407c182ea057f` |

The base deb installs 4 files: `/usr/bin/dosbox-x` at 15,717,824 B md5 `2531964ba92baa2a0eb460328bb28651` plus the share's reference confs and its `FREECG98.BMP`. The seat's pin is the same version, so the install replaces it in place.

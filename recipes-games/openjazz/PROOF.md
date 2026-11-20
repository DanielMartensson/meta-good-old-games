# OpenJazz - pins, sizes, measured rounds

Numbers are kept here, out of the README, so the README stays one screen of
instructions. Everything below is what the build in the image and the seat rounds
actually did.

## The engine

```
git://github.com/AlisterT/openjazz.git  protocol=https  branch=master
SRCREV a1626f4edd4a7af72c54103021790c56d8ecfced  ("Merge pull request #157 from
        carstene1ns/feat/episode-select", 1 Sep 2026)
LICENSE GPL-2.0-or-later, COPYING md5 b234ee4d69f5fce4486a80fdaf4a4263
PV 20260301+git${SRCPV}      PKGR r0.01      S ${WORKDIR}/git
```

`CMakeLists.txt:4` is `project(OpenJazz VERSION 20260301 ...)`, and the git suffix is the
recipe's own, as in the ioquake3 recipe.

The pin is the tip of master when this recipe was written. The upstream repository's
default branch is `master`, **not** `main` - `git ls-remote --heads` lists `master`,
`feature/config`, `feature/screenshots`, `feature/zipfiles` and `old-dev`, and asking for
`branch=main` is what makes `do_fetch` answer `Unable to find revision ... in branch main
even from upstream`. It is pinned rather than left at HEAD, like every other recipe of this
layer, because the option names this build passes (`SDL_VERSION`, `NETWORK`, `SCALE`,
`DATAPATH`) are read out of that `CMakeLists.txt`, and `src/io/file.cpp:700-760` is what
decides whether a data path is accepted at all, so a tree that moves either has to be
re-checked against the file rather than configured silently differently.

`COPYING` is the GPL-2 text, md5 `b234ee4d69f5fce4486a80fdaf4a4263`, and the sources' own
headers say "version 2 of the License, or (at your option) any later version" (`src/main.cpp:10-12`),
so the licence is v2-or-later. `doc/licenses.txt` of the same tree repeats it for the whole
tree and spells out that the vendored `ext/xmp` is libxmp - "See MIT license at the end of
file" - and `ext/xmp/LICENSE` carries that MIT text ("Permission is hereby granted, free of
charge ... to deal in the Software without restriction"), which is compatible with linking
it into this GPL-2 binary. The SDL row of the same file is the other vendored license:
"Version 1.2 of this library is distributed under the terms of the GNU LGPL license",
"Version 2 and 3 use the zlib license", and this build is the SDL2 one, so the vendored half
of this binary is zlib plus `ext/scale2x`, whose own row in the same file repeats the GPL-2
boilerplate and matches the engine's license.

## Build shape - no patches, every decision is an upstream option

`inherit cmake pkgconfig`. `DEPENDS = "libsdl2"`.

```
-DSDL_VERSION=2  -DNETWORK=OFF  -DSCALE=ON  -DDATAPATH=${datadir}/openjazz
```

| Option | Value | Why |
| --- | --- | --- |
| `SDL_VERSION` | `2` | the seat already carries SDL2 for every other engine in this layer; the tree also speaks SDL3 and SDL1.2 (`CMakeLists.txt:163-200`), and asking for the third one would add a library the board does not otherwise need |
| `NETWORK` | `OFF` | the honest setting for a single-seat machine with no SDL_net in the image: the option only gates the sockets code path, and the build says "Network: Disabled" rather than carrying a netplay feature that has nothing to talk to |
| `SCALE` | `ON` | the tree's default; costs one vendored subdirectory (`ext/scale2x`) for the software upscalers |
| `DATAPATH` | `${datadir}/openjazz` | compiled in (`src/main.cpp:218-219`) as the **last** game-data path the engine will try, so a hand-installed tree stays one argument and the compiled-in default is a fallback rather than the one place a recipe had to have written data into |

Video is SDL2's own renderer: `src/io/gfx/video.cpp:379` `SDL_CreateRenderer(window, nullptr)`
builds the window's renderer and `src/io/gfx/video.cpp:485` `SDL_RenderSetLogicalSize` gives
it the game's 320x200 canvas, so the upscale to the seat's 1920x1080 is SDL2's work and the
engine needs no GL context of its own. Audio is SDL2's audio device (`src/io/sound.cpp:243`
`SDL_OpenAudioDevice` for the SDL2 build) fed by libxmp (`src/io/sound.cpp:353`
`xmp_load_module_from_callbacks`), so the `.PSM`/`.MUS` modules the game ships play with no
soundfont and no external mixer. Configuration and saves are XDG files under
`~/.config/openjazz` and `~/.cache/openjazz` (`src/platforms/xdg.cpp:32-36`), which weston
can write.

`FILES:${PN} += "${datadir}/icons/hicolor"`: `CMakeLists.txt:461-464` installs the engine's
own icon set as well as the desktop entry, and OE's default `FILES` covers
`${datadir}/applications` and `${datadir}/pixmaps` but not `${datadir}/icons`, which is what
makes `do_package` stop with "Files/directories were installed but not shipped". The icons
are two small files and they belong to the engine package.

## What the package installs

```
/usr/bin/OpenJazz    397,160 B
```

That is the whole engine package - no game data, no launcher. The `jazz1-demo` package this
family used to ship (60 files, 2,806,385 B at `/usr/share/jazz1-demo`, plus the `/usr/bin/jazz1-demo`
launcher it wrote) is gone from this layer, and with it the seat's `/usr/share/jazz1-demo`.

## The Holiday Hare '94 episode, if a hand install is checked against these bytes

This layer fetches none of it, and the copy on hand is a locally installed one - see the
source verdict in README.md and the licence verdict below.

```
60 files, 2,806,385 B, under their original DOS upper-case names
release archive on hand: archive.org community item msdos_jazzxmas_shareware
  1,446,421 B, sha256 09bc8991f24a26059982d3106eada91999a884be9a821da178128c9b0fe046fc
  66 members; the seat's tree keeps the 60 the engine reads and drops the 6 DOS-only ones:
  JAZZ.EXE, SETUP.EXE, SETUP.INT, HELPME.EXE, DPMI.000, JAZZ.PIF
```

The licence is `LICENSE.DOC` inside the release archive, Epic MegaGames' `SOFTWARE LICENSE
AGREEMENT`, md5 `397b52fef7ca5f513eed7094809fe10f`. Its operative lines:

```
Epic authorizes distribution and/or copying of the Program only in
accordance with the following terms and conditions (the "General Terms and
Conditions") and with other terms and conditions set forth below that may
be applicable:
```

under term 4:

```
You may copy and/or distribute the Program only in its original,
unaltered form, with all files included unmodified, and without
making any additions, modifications or deletions except as provided
in this paragraph.  You may not modify the Program or any of its
files, and the Program must be distributed as a complete package.
You may not change, delete, merge or rename any files or elements
of the Program in any manner, and you may not add any files or new
elements (except for installation routines which do not interfere with
the proper operation or installation of the Program).
```

and term 6's `you shall not charge any fee or other compensation for the Program,
although you may charge a distribution fee for costs associated with
distributing the Program`. The agreement is explicit that the Program `is NOT public
domain or free software or "freeware"`, so the episode is free to download and free to
play under a shareware agreement rather than under a free licence - the grant a recipe
would pin as a `LicenseRef`, e.g. `LicenseRef-Epic-SOFTWARE-LICENSE-AGREEMENT`. Nothing
here is redistributed; the engine beside it is GPL-2.

## Measured on the seat - Holiday Hare, round of 2026-11-15

Two rounds that day, both under the seat's normal weston session with every other game
killed first so the ALSA sink was free. The first round was the launcher bug:
``unknown option `-fullscreen` `` and five shots of 124,281 bytes of empty desktop. The
round that drew is at `/gamedata/installs/shots/jazz1e` on the seat.

| Step | What is on screen |
| --- | --- |
| title card | "Jazz Jackrabbit" in green on purple, "(C) 1994 EPIC MEGAGAMES", the animated intro drawn by `STARTUP.0SC` |
| main menu | NEW GAME / LOAD GAME / INSTRUCTIONS / SETUP OPTIONS / ORDER INFO / EXIT GAME, the OpenJazz logo bottom left, `MENU.000` |
| episode list | the 12 rows, `1 TURTLE TERROR` highlighted, `X HOLIDAY HARE`, `Z BONUS LEVELS`, `SPECIFIC LEVEL` |
| level 0 of world 50 | snow and ice, three snow-covered pines, a candy-cane pole, Jazz with his blaster, a blue dog, and the HUD: score `00000000`, clock `7:58:6`, lives `03`, `91` blaster ammo |

`shotstat.py` on the level shot (`wayland-screenshot-2026-11-15_15-54-40.png`):

```
1920x1080 RGB
distinct colors: 140    non-black: 1819175 / 2073600 (87.7%)
row-lum min/max: 0.0 158.4    distinct-row-lum: 111
mid-row brightness jumps: 127
md5: f92586e9a1b98665f4369ebb88ffbd17
```

`imgdiff.py` on the same scene left alone and then walked (threshold: max-channel
difference of 8):

```
level vs level 15 s later:   1,981,465/2,073,600 identical (95.6 %),
                             92,135 px changed ( 4.4 %)  -- the HUD clock and sprite animation only
level vs three taps of Right: 1,482,346 px changed (71.5 %),
                             columns whose mean luminance moved >8: 881/1920, rows: 716/1080
                             -- the camera scrolled and the timer ran from 7:58:6 to 7:46:0
```

So the draw is live: the title animation, the menus, a real level, and the same level with
the camera moved by the keyboard. There is no mouselook to prove - Jazz Jackrabbit is a
side-scroller and the mouse is only a menu convenience - so the control proof is the walk:
three taps of Right moved Jazz, scrolled the world and drained the health bar when the dog
touched him.

**Audio.** The engine's log on the same run says

```
DEBUG Opened 44100Hz Audio at 16 bit, 2 channels with 2048 samples (sound.cpp:269)
DEBUG Opened file: /usr/share/jazz1-demo/SOUNDS.000 (file.cpp:125)
DEBUG Opened file: /usr/share/jazz1-demo/XM2.PSM (file.cpp:125)
```

`XM2.PSM` is the level's music module, played by the libxmp that is compiled into
`/usr/bin/OpenJazz` (no separate libxmp in the process maps). The process also maps
`libSDL2-2.0.so.0.3000.1`, `libasound.so.2.0.0` and `alsa-lib/libasound_module_rate_speexrate.so`,
which is the 44.1 kHz stream going through the seat's ALSA sink. (The paths in that log are
the old payload's tree; a hand tree logs its own directory instead.)

**Cost.** While the level was running: RSS 28,948 KiB, 33.9 % CPU, elapsed 1:02 (the 33.9 %
is one core's share of the frame loop plus libxmp). The installed payload was 2.9 M and the
engine binary is 397,160 bytes.

## Failure modes

| Symptom | Cause |
| --- | --- |
| ``unknown option `-fullscreen` `` before any data is opened, and shots of an empty desktop | the long option spellings need **two** dashes: the bundled `ext/argparse` reads `-fullscreen` as a short-option cluster. That is what the first seat round of the old launcher did, and the fixed one execs `--fullscreen` |
| `-m` and `--mute` | `-m/--mute` is the switch; it is how a round checks the video path alone |
| a key press doing nothing near the start | the title animation runs about 20 s and eats any key pressed during it; wait for the log's `Opened file: .../MENU.000` before pressing |
| the episode list jumping several rows | a **held** arrow key repeats: a 1 s hold of Down jumped from row 1 to row 6. Taps of 120 ms are what navigate it |
| a renamed data file is read as a different game | the engine reads Epic's original DOS names (`LEVEL0.001`, `STARTUP.0SC`, `MENU.000`, `SOUNDS.000`, `XM2.PSM`) |
| a data directory is ignored | positional arguments are added as `PATH_TYPE_GAME` paths at `src/main.cpp:186-219`, and the compiled-in `DATAPATH` is the **last** path tried, so an argument wins over it; `src/io/file.cpp:700-760` is what decides whether a path is accepted at all |
| `Files/directories were installed but not shipped` for the icons | `CMakeLists.txt:461-464` installs the icon set next to the desktop entry, and OE's default `FILES` does not cover `${datadir}/icons` - hence the `FILES` line above |
| a "complete package" complaint about the 60-file tree | term 4 of Epic's agreement forbids dropping, renaming or merging members: the release zip holds 66 files and a data-only tree keeps 60. A re-shipment has to ship all 66 |
| `Network: Disabled` in the build log | expected: `NETWORK=OFF` is the honest single-seat setting |
| Jazz Jackrabbit 2 will not be installed at all | its shareware is a Win32 self-extractor archive every unpacker here refuses outright, so this round could not open it. The engine's own experimental JJ2 support sits behind `-DENABLE_JJ2=ON` (`CMakeLists.txt:241`) and is off in this build on purpose |

## Files

`files/` is empty on purpose: the SDL2 choice, the network switch, the scaling switch and
the compiled-in data path are all options upstream's `CMakeLists.txt` already has.
`openjazz_git.bb` builds the engine and installs `/usr/bin/OpenJazz`.

## Open

- [x] Audio through the seat's 48 kHz ALSA sink, measured: the engine logs
      `Opened 44100Hz Audio at 16 bit, 2 channels with 2048 samples` (`src/io/sound.cpp:269`),
      plays `XM2.PSM` through the statically linked libxmp, and the running process maps
      `libSDL2`, `libasound` and `libasound_module_rate_speexrate`
- [ ] a payload recipe for this episode, once a source authorized by Epic exists for it: the
      agreement above needs all 66 members unmodified, so the recipe would install the whole
      package rather than the 60-file data subset the seat runs now
- [ ] Jazz Jackrabbit 2 as its own hand install, once its archive can be opened at all

## The debs this branch builds (built 2026-11-20 from this tree)

The whole layer was built again from this restructured tree at its own path: `bitbake chocolate-doom fteqw ioquake3 eduke32 dosbox-x openjazz xash3d-fwgs librequake openarena opennowlite` ran green on the build host's `stm32mp25-watermelon-wine-1a` machine, and this recipe's own task chain re-ran inside that run, so the numbers below are this tree's and not the ones an earlier round happened to record. `openjazz` leaves the build as `20260301+git0+a1626f4edd-r0.01`:

| deb | size | md5 |
| --- | --- | --- |
| `openjazz_20260301+git0+a1626f4edd-r0.01_arm64.deb` | 157,408 B | `73a3048424b77d2650e71b2c3004daf8` |
| `openjazz-dbg_20260301+git0+a1626f4edd-r0.01_arm64.deb` | 862,496 B | `06b891d974da6d02757ae4b28493d37c` |
| `openjazz-dev_20260301+git0+a1626f4edd-r0.01_arm64.deb` | 2,052 B | `03238c2a90d3e3a312e7ecd801cb5143` |
| `openjazz-src_20260301+git0+a1626f4edd-r0.01_arm64.deb` | 276,000 B | `9897b6a76eecec60c05f70af222b626e` |

The base deb installs 5 files, the engine `/usr/bin/OpenJazz` at 397,160 B md5 `da770ca8080dd577432db855dec8ade8` plus the icons and the `.desktop` entry. The seat's pin is the same version, so the install replaces it in place.

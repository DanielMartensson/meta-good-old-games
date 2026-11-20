# EDuke32 - pins, sizes, measured rounds

Numbers are kept here, out of the README, so the README stays one screen of
instructions. Everything below is what the build in the image and the rounds on the
seat actually did.

## The engine

```
https://dukeworld.com/eduke32/synthesis/latest/eduke32_src_20260807-10669-ec5824db8.tar.xz
  19,085,516 B, sha256 f292e2c69f7f3453dd145d19ce1c114bd7136a8a9cf5b9b6641525fd53fa56c5
LICENSE GPL-2.0-or-later, source/duke3d/gpl-2.0.txt md5 b234ee4d69f5fce4486a80fdaf4a4263
S    ${WORKDIR}/eduke32_20260807-10669-ec5824db8      PV 20260807      PKGR r0.01
```

Upstream ships its source as one xz tarball off dukeworld, not as a git tree, so this is
a pinned tarball fetch rather than a git pin. The tree inside it is stamped
`eduke32_20260807-10669-ec5824db8` and stamps the same into its own `EDUKE32_REVISION.mak`,
so the deb version is the tarball's date and the engine's banner says which revision it is:

```
EDuke32 r10669-ec5824db8
Built Aug  8 2026 02:54:34, GCC 13.4.0, 64-bit
```

It is pinned by hash rather than taken from `latest`, because that address moves on every
synthesis build and the make settings and the option spellings were read out of this exact
tree; a moved `latest` would be a silently different engine. The engine is GPL-2.0-or-later:
`source/duke3d/gpl-2.0.txt` is the license text and every file under `source/duke3d` says
so at its top (`actors.cpp:6-9` "the GNU General Public License version 2",
`source/audiolib/src/flac.cpp:3-10` adds "or (at your option) any later version"). Vendored
inside the tarball and linked into this binary are mimalloc (v2 MIT), libxmp-lite, imgui and
glad; `source/build/buildlic.txt` and `source/kenbuild/buildlic.txt` are Ken Silverman's
BUILDLIC for the Build tool, which the engine does not use. This build is for my own use and
is not redistributed.

## Build shape - no patches, every setting on the make line

```
make -C "$S" eduke32 ARCH=arm64 PLATFORM=LINUX SUBPLATFORM=LINUX \
    CC=... CXX=... AR=... RANLIB=... STRIP=... \
    CFLAGS+=" -I${WORKDIR}/recipe-sysroot/usr/include/SDL2" \
    CXXFLAGS+=" -I${WORKDIR}/recipe-sysroot/usr/include/SDL2" \
    SDL_TARGET=2 SDLCONFIG= PACKAGE_REPOSITORY=1 RELEASE=1 \
    USE_OPENGL=0 POLYMER=0 USE_LIBVPX=0 HAVE_GTK2=0 STARTUP_WINDOW=0 \
    NETCODE=1 USE_MIMALLOC=1 VC_REV=10669 VC_HASH=ec5824db8
```

- The target is `eduke32`, not `all`: `all: duke3d` at `GNUmakefile:754` would also link
  `mapster32`, the level editor, and the seat runs the game.
- `SDLCONFIG=` is left empty, which takes `Common.mak:955`'s own fallback: `-lSDL2` plus
  `-D_REENTRANT -DSDL_USEFOLDER`. There is no `sdl2-config` in this sysroot, and unlike fteqw
  no stub is planted here, because `source/build/include/sdl_inc.h:15` takes the `<SDL2/SDL.h>`
  include only when `SDL_USEFOLDER` is defined and that define lives only in this empty-`SDLCONFIG`
  branch - the fallback is kept.
- The one directory the fallback cannot supply is `${WORKDIR}/recipe-sysroot/usr/include/SDL2`,
  which is what the vendor's own `sdl2-config --cflags` answer is, and EDuke32 needs it because
  its vendored SDL2 shim spells the header without the folder: `sdl_inc.h:16` has `<SDL2/SDL.h>`
  but `source/imgui/src/imgui_impl_sdl2.cpp:117` has plain `<SDL.h>`, and this sysroot keeps
  SDL2's headers one directory deeper. Without that directory the object that stops the build is
  imgui's:

      source/imgui/src/imgui_impl_sdl2.cpp:117:10: fatal error: SDL.h: No such file or directory

  imgui is compiled even with the software renderer - `GNUmakefile:160` excludes
  `imgui_impl_sdl2.cpp` only when `RENDERTYPE` is not SDL, and `RENDERTYPE=SDL` is what the
  classic renderer needs - so the vendored copy is built and linked and simply never reached by
  a `REND_CLASSIC` frame.
- `STRIP="${STRIP}"` is handed over because `RELEASE=1` leaves `STRIP` set (`Common.mak:447`
  clears it only for `RELEASE=0`) and the `BUILDRULE` at `GNUmakefile:803` runs it over the
  linked executable. Left at `Common.mak:203`'s default it would be the host's `strip`, which
  cannot read an aarch64 object, and the unstripped binary would ship with the engine's debug
  info. By the time `do_install` sees the file there is nothing left for OE to strip, which is
  why the `already-stripped` QA hit is expected here, as in the other pre-stripped recipes of
  this layer (opennow, opennowlite, fteqw).

## The renderer, and why the seat cannot pay for the other one

The build asks `USE_OPENGL=0`, EDuke32's CLASSIC renderer: `build.h:1082`
`videoGetRenderMode()` answers `REND_CLASSIC` when `USE_OPENGL` is undefined, and
`Common.mak:807` forces `POLYMER=0` and `USE_LIBVPX=0` behind it. `RENDERTYPE=SDL` stays,
so the classic renderer draws through SDL2's own textures and blitters (`source/build/src/2d.cpp`,
`engine.cpp drawrooms()`) and the seat's picture is a plain SDL2 wayland surface like every
other engine here.

The GL path is not what this board has. The seat carries EGL, GLES 1/2/3 and the Vivante GAL
driver and has no desktop GL at all - `/usr/lib` has `libEGL.so`, `libGLESv2.so` and
`libGLESv1_CM.so`, no `libGL.so` in any arch directory - while EDuke32's whole GL path is
desktop-only: `polymost1Vert.glsl` and `polymost1Frag.glsl` are `#version 120`, the Polymost2
inline shaders are `#version 110`, `glsurface.cpp:113` and `:126` compile their own
`#version 110`/`#version 120` sources and imgui wants `#version 130`. EDuke32 does have an ES
branch, but `compat.h:1541` defines `EDUKE32_GLES` only for `__ANDROID__` and `EDUKE32_IOS`
and there is no make toggle for it, so no make invocation gets an ES renderer out of this
tree. That reason is read out of the source and out of the seat's `/usr/lib`: a `USE_OPENGL=1`
build was **not** shipped and not run on the seat, so the desktop-GL version numbers and the
missing `EDUKE32_GLES` toggle are the argument, not a measured refusal.

## What is linked, and what is not

`DEPENDS = "libsdl2 flac alsa-lib"`, and those are the only three real shared libraries:
`readelf -d` on the installed binary says

```
NEEDED: libSDL2-2.0.so.0, libFLAC.so.12, libasound.so.2,
        libstdc++.so.6, libm.so.6, libc.so.6
```

and the seat already carries all of them in `/usr/lib` (`libSDL2-2.0.so.0`, `libFLAC.so.12`,
`libasound.so.2`). What is **not** a dependency is vorbis: `source/audiolib/src/vorbis.cpp:45`
includes the vendored `minivorbis.h` rather than `<vorbis.h>`, so `.ogg` tracks inside the grp
play with nothing linked from outside. What *is* vendored-and-must-be: FLAC -
`source/audiolib/src/flac.cpp:44` includes `<FLAC/all.h>` and `GNUmakefile:569` adds
`-lFLAC -lasound` to the link for `SUBPLATFORM=LINUX` unconditionally. `-lrt` and `-latomic`
at `Common.mak:993` come from glibc's `librt.so`/`librt.a` and gcc-runtime's `libatomic.so`,
`-ldl` from glibc's `libdl.a`, `-lm` and `-pthread` from libc.

## What the package installs

```
/usr/bin/eduke32    2,451,168 B     deb eduke32_20260807-r0.01_arm64.deb  1,028,632 B
```

That is the whole package: no data, no launcher. The payload recipe this family used to have
is gone from the layer, and with it `/usr/bin/duke3d-demo` and the 11,402,093-byte tree it
wrote at `/usr/share/duke3d-demo`.

## The episode data, if a hand install goes through 3D Realms' own release

Recorded so a hand install of the same bytes can be checked against them. This is 3D Realms'
own site (`dukeworld.duke4.net`, the classic dukeworld share page), i.e. the release's own
publisher, and unlike id's dead ftp corner there is a first-party carrier for it.

```
carrier  https://dukeworld.duke4.net/classic%20dukeworld/share/3dduke13.zip
         5,924,374 B, sha256 c67efd179022bc6d9bde54f404c707cbcbdc15423c20be72e277bc2bdddf3d0e
         the http form answers a 301 to the same https address, and a fresh download
         re-fetches byte-identical
         4 members: LICENSE.TXT, INSTALL.EXE, FILE_ID.DIZ, DN3DSW13.SHR
         FILE_ID.DIZ names the release "DUKE NUKEM 3D v1.3d - Shareware - by
         3D Realms Entertainment"
inside   DN3DSW13.SHR  5,848,108 B, sha256 a6c010e735a8f8d910807d41985f16379583cd9d81f14a1e4634f30d04b881e4
         a PKWARE self-extracting archive: an MZ header plus PKLITE-compressed members
         with a zip directory record appended, so an ordinary zip reader opens the
         embedded zip, which holds the 17 members of the v1.3d release, 12,833,604 B
keep     the 7 the engine and the player read, and drop the 10 it does not:
         DUKE3D.EXE 1,178,963 (the x86 game itself), SETUP.EXE, SETMAIN.EXE, COMMIT.EXE
         (the DOS sound-card/video editor and its two helpers), DN3DHELP.EXE (the help
         viewer), DEMO1.DMO/DEMO2.DMO/DEMO3.DMO (the recorded v1.3d demos), MODEM.PCK
         (the modem-record bank the DOS game's multiplayer used), ULTRAMID.INI
         (the DOS Ultramid sound driver's answer file). That is the same cut the Arch
         AUR's eduke32-shareware-episode PKGBUILD makes; README.DOC is added so the
         seat can read which release this is.
```

| File | Bytes | md5 | What it is |
| --- | --- | --- | --- |
| `DUKE3D.GRP` | 11,035,779 | `c03558e3a78d1c5356dc69b6134c5b55` | the whole game: E1M1-E1M9 of Luminous Genesis, its sprites, sounds, the Duke's own voices, the episode's music by Bobby Prince, and the palette the engine dies without when the grp is handed to it the wrong way |
| `GAME.CON` | 99,639 | `aac2c561d79ed37ffc04cfeff907ba42` | the behaviour script the engine compiles; `common.cpp:36` defaults the main data file to `DUKE3D.GRP` and the same file spells the `.con` and `.rts` names |
| `DEFS.CON` | 28,893 | `4d7789f1542d69e473a89f0b21ec44e4` | the definitions module `GAME.CON` includes |
| `USER.CON` | 36,960 | `159a0931fed4f14cf2e41478f7240bbe` | the user's key, weapon and setup defaults, which the engine reads and rewrites |
| `DUKE.RTS` | 188,954 | `9d29f9673bbdb56068acf7645c13749c` | the Remote Ridicule sound bank the `-rts` option names |
| `LICENSE.TXT` | 9,108 | `583bf2a6cb3d404a21c2205041c45481` | 3D Realms' own agreement for this release |
| `README.DOC` | 2,760 | `b9da7097bcd359de8debdff0466018b6` | the release notes: "What's new in Duke Nukem 3D v1.3D?", "V1.2 is NOT official and ... get v1.3D" |

Total 11,402,093 bytes, 7 files, each kept under its original upper-case DOS name because
that is the name EDuke32 spells in its own source. A decoder of these bytes asserts the kept
count and total, so a member that quietly went away fails rather than installing something
else. The payload's own deb was `duke3d-demo_1.3-r0.01_arm64.deb`, 4,081,756 B.

The licence is `LICENSE.TXT` from inside the archive, 3D Realms' Software License Agreement
for distribution as shareware. Its first lines are "Episode One of Duke Nukem 3D (tm),
including all versions, patches and upgrades (the \"Game\") Copyright 1996 3D Realms
Entertainment a division of Apogee Software, Ltd.", and it opens "BY COPYING, USING OR
DISTRIBUTING THE GAME, YOU INDICATE YOUR AGREEMENT TO THIS LICENSE". Under "LIMITED
DISTRIBUTION RIGHTS" it says individuals "can -- and are encouraged! -- to give away copies
of the Game without charge" and that free online services and BBSs of 250 nodes or fewer may
carry it, while "*ALL* OTHER DISTRIBUTION ... REQUIRES: WRITTEN PERMISSION ... AND
PRE-PAYMENT OF A LICENSE FEE". A shareware distribution agreement, not a free licence: free
to download and free to play, not free to ship - the grant a recipe pins as a `LicenseRef`,
e.g. `LicenseRef-3D-Realms-Duke-Nukem-3D-shareware-LICENSE.TXT`. Nothing here is redistributed;
the engine beside it is the GPL-2 EDuke32.

## Measured on the seat - Episode One, launcher defaults, round of 2026-11-15

Six shots at 1920x1080 under the seat's normal weston session, with every other game killed
first so the ALSA sink was free. Shots on the seat at `/gamedata/installs/shots/duke1e`.

| Step | Distinct colours | Non-black of 2,073,600 | Row lum min/max | Distinct row lums | Mid-row jumps | md5 |
| --- | --- | --- | --- | --- | --- | --- |
| idle, 20:37:27 | 175 | 1,673,718 (80.7 %) | 3.9-82.4 | 48 | 436 | `6880bdeca547631789709239a3653a6d` |
| idle, 20:37:32 | 138 | 1,662,027 (80.2 %) | 3.9-62.7 | 48 | 436 | `2498bbd1a109b19b32f7de424570f4a9` |
| walked forward, 20:37:39 | 135 | 1,644,622 (79.3 %) | 3.9-62.7 | 49 | 440 | `ff085d3206109ff5ce3308b28469f3dc` |
| yaw right, 20:37:43 | 141 | 1,603,359 (77.3 %) | 7.7-52.8 | 45 | 343 | `a99b3ff4d8cd8644db647e147486ed5d` |
| yaw back, 20:37:46 | 135 | 1,644,622 (79.3 %) | 3.9-62.7 | 49 | 440 | `ff085d3206109ff5ce3308b28469f3dc` |
| pitch down, 20:37:50 | 134 | 1,820,376 (87.8 %) | 10.1-52.3 | 40 | 87 | `f66a91454e9c5a27a1f38340bcc1c679` |

`imgdiff.py` over those six (threshold: max-channel difference of 8):

```
idle pair (27 s vs 32 s):   2,046,393/2,073,600 identical (98.7 %), 27,207 px changed ( 1.3 %)
                            columns whose mean luminance moved >8: 43/1920, rows: 27/1080
idle vs yaw right:          1,691,269 px changed (81.6 %), columns: 708/1920, rows: 165/1080
idle2 vs walked forward:      376,405 px changed (18.2 %), columns:   0/1920, rows:  89/1080
walked vs yaw back:         2,073,600/2,073,600 identical (100.0 %)
idle vs pitch down:         1,734,609 px changed (83.7 %), columns: 137/1920, rows: 210/1080
```

What that says, piece by piece:

- The idle pair is a still view with only animation alive in it: 98.7 % of the pixels are the
  same and only 43 of 1,920 columns changed their shape, so Duke's standing frame is not
  drifting and the level is not being re-lit at random.
- Walking forward kept **every** column and moved 89 of 1,080 rows: forward motion along the
  view axis moves the horizon and the floor/ceiling bands, not the walls' left-right shape.
  The 18.2 % of pixels changed is the same geometry getting bigger.
- Yaw right moved 708 of 1,920 columns and 81.6 % of the pixels.
- **The yaw round trip came back byte-identical.** `mousemove 300 0` then `mousemove -- -300 0`
  returned the exact frame the walk had produced, the same md5 `ff085d3206109ff5ce3308b28469f3dc`
  and 0 pixels of difference, so the mouse drives the camera and the camera keeps the angle it
  is given.
- Pitch down is the frame with the most non-black pixels of the six, 1,820,376: looking at the
  floor fills the whole panel with lit texture, and its mid row has 87 brightness jumps against
  the 440 of a level view.

Colour counts are low here (135-175) because `REND_CLASSIC` draws an 8-bpp picture - the log
says `GFX| Setting video mode 1920x1080 (8-bpp fullscreen)` - so a palette is what the grp's
palette member is for, and the engine dies without it.

**Controls.** The keyboard walk is `ydotool key 26:1` held 3 s and released by `ydotool key 26:0`
(the up arrow). The mouse yaw is `ydotool mousemove 300 0` and `ydotool mousemove -- -300 0` -
the `--` is needed before a negative relative move, since a bare negative argument is read as an
option and the tool answers `mousemove: invalid option -- '5'`. Pitch is `ydotool mousemove 0 220`.

**Audio.** The engine's log on the same run says

```
3.7223s  ASS| Using SDL ALSA driver on STM32MP25-DK, 400b0000.audio-controller-i2s-hifi i2s-hifi-0
3.7226s  ASS| Initialized sound at 48.0 KHz stereo with 64 voices
3.7227s  ASS| Initializing MIDI driver: AdLib OPL3 emulation
```

which is the Apogee Sound System opening the seat's own I2S audio controller at the seat's 48 kHz,
64 voices wide, with the episode's `.mid` music behind an emulated OPL3. The same run also printed
four of

```
ALSA lib .../alsa-lib/1.2.11/src/pcm/pcm.c:8740:(snd_pcm_recover) underrun occurred
```

which is an opened sink recovering from a starved stream, not a refusal. The owner confirmed on the
live seat that the game's graphics and audio work.

**Cost.** While E1L1 was running: RSS 71,592 KiB at 112 % CPU. EDuke32 has no fps readout - the
only frame-rate control in the tree is `maxfps`/`FRAMELIMIT` at `menus.cpp:616` and `:619`, a cvar
and not a counter - so no fps number is quoted for this engine, and its proof is shots, the log
lines below and the cost.

## The working run, as the engine's own log names it

```
0.0540s INFO| EDuke32 r10669-ec5824db8
0.0670s INFO| Initializing SDL 2.30.1 (SDL-release-2.30.1-0-g5adbf3765)
0.1677s INFO| Using 'wayland' video driver.
0.1984s INFO| Using DUKE3D.GRP as main data file.
0.2245s  CON| Including: DEFS.CON (28893 bytes)
0.2482s WARN| Found 4 warning(s), 0 error(s).
0.2765s  CON| Compiled 107960 bytes in 65ms, v1.3D compatibility mode
0.8206s INFO| Initialized 96.0M cache
1.5389s INFO| Loaded DUKE.RTS
3.3583s INPT| No game controllers found.
3.3599s  GFX| Setting video mode 1920x1080 (8-bpp fullscreen).
3.6078s  GFX| Refresh rate: 60.00Hz.
3.6520s  ASS| Using SDL ALSA driver on STM32MP25-DK, 400b0000.audio-controller-i2s-hifi i2s-hifi-0
3.6523s  ASS| Initialized sound at 48.0 KHz stereo with 64 voices
3.6523s  ASS| Initializing MIDI driver: AdLib OPL3 emulation
7.4342s INFO| E1L1: HOLLYWOOD HOLOCAUST
```

The engine reached the level in about 8 s of init. `INPT| No game controllers found.` is SDL2
finding no gamepad, which is honest on this seat: the keyboard and the mouse are what play it,
and the proof round drove both. `v1.3D compatibility mode` is the engine reading the v1.3d
episode's `GAME.CON` with its 1996 semantics, and the four warnings it counts (`USER.CON:753`
overwriting a sound definition, `GAME.CON:157`, `GAME.CON:1923` and `GAME.CON:2817`) are
upstream's own `.CON` script quirks, present in the release data rather than introduced here,
with 0 errors.

## Installing on the seat

The debs are built on the host and relayed host to local to seat, because this board's build
host cannot ssh to the seat:

```
eduke32_20260807-r0.01_arm64.deb   1,028,632 B
```

On the seat they install with `dpkg -i --force-depends`: the seat's dpkg answers
`Package flac is not installed` and leaves the package "unconfigured", but the files install and
the game runs, because `/usr/lib` on the seat already carries `libFLAC.so.12` and
`libasound.so.2.0.0` even though those two are not installed as *packages* there.

## Failure modes

| Symptom | Cause |
| --- | --- |
| `WARN\| Ignored application parameter '--nologo'.` | EDuke32's parser matches the option name at `c+1` after **one** leading dash, so a double-dash spelling is an unknown option and the engine reports it rather than failing. `-nologo`, `-nosetup`, `-v1` and `-l1` are the spellings |
| `ERROR\| No palette found.` after `WARN\| Unable to open <abs>/DUKE3D.GRP.` | `DUKE3DGRP` handed the **absolute** grp path. The engine logs the override (`INFO\| Using <abs>/DUKE3D.GRP as main data file (DUKE3DGRP env var override).`), reads the grp header (`INFO\| DUKE3D.GRP has checksum 0x81e89fde`), then cannot reopen the file the way it was handed it, and the palette - a member *inside* the grp - never arrives: `ERROR\| There was a problem initializing the engine: No palette found.` Use `-j <dir>`, which the seat logs as `INFO\| Using directory <dir>/` then `INFO\| Searching for game data...` then `INFO\| Using DUKE3D.GRP as main data file.` `DUKE3DGRP` is still honoured when a caller sets it, so a hand-run grp swap stays possible |
| a `USE_OPENGL=1` build that will not run | the tree's GL path wants desktop GL and the seat has none; this recipe does not build it, see the renderer section above (source argument, not a measured refusal) |
| `Device or resource busy` in the audio init | not this engine: the seat's ALSA sink serves one process at a time, so a proof round has to kill every other game first |
| `ALSA lib .../pcm.c:8740:(snd_pcm_recover) underrun occurred` | the sink opened and alsa-lib recovered from a starved stream. The round that started the level printed four of these, and the same run drew and played |
| the engine opens a SETUP screen instead of the level | it still does for a configuration file it has never seen (`game.cpp:6732` tests `g_noSetup` against `ud.configversion`) even with `STARTUP_WINDOW=0`; `-nosetup` is what skips it |
| `already-stripped` QA hit | expected: `RELEASE=1` leaves `STRIP` set and the `BUILDRULE` at `GNUmakefile:803` runs OE's cross strip over the linked executable, so `do_install` has nothing left to strip |
| a low distinct-colour count in a shot (135-175) | not a failure: `REND_CLASSIC` draws an 8-bpp picture, so the shot carries a palette, not a photographic colour range |

## Files

`files/` is empty on purpose: every setting that matters is passed on the make command line
rather than edited into the tree. `eduke32_20260807.bb` builds the engine and installs only
`/usr/bin/eduke32`.

## TODO

- [ ] `USE_OPENGL=1` measured on the seat as a documented refusal, so the row above carries a
      log line rather than a source argument.
- [ ] The v1.3d recorded demos (`DEMO1.DMO`-`DEMO3.DMO`) are dropped from a hand install here.
      They are playable on EDuke32 with `-d`, so a `DUKE3D_ARGS`-style `-d 1` line would show the
      release's own gameplay without a proof round of ours.

## The debs this branch builds (built 2026-11-20 from this tree)

The whole layer was built again from this restructured tree at its own path: `bitbake chocolate-doom fteqw ioquake3 eduke32 dosbox-x openjazz xash3d-fwgs librequake openarena opennowlite` ran green on the build host's `stm32mp25-watermelon-wine-1a` machine, and this recipe's own task chain re-ran inside that run, so the numbers below are this tree's and not the ones an earlier round happened to record. `eduke32` leaves the build as `20260807-r0.01`:

| deb | size | md5 |
| --- | --- | --- |
| `eduke32_20260807-r0.01_arm64.deb` | 1,028,656 B | `3c3afa7328d12f737c87382777f86d99` |
| `eduke32-dbg_20260807-r0.01_arm64.deb` | 2,264 B | `6d33fa67023f2f4c8d196c304d61df16` |
| `eduke32-dev_20260807-r0.01_arm64.deb` | 2,288 B | `6f2ada2d7dc3919ee7ea886e7fdcf5da` |

The base deb installs one file, `/usr/bin/eduke32`, at 2,451,168 B md5 `d2c7be1ae31ce7528a5957faf5bb1f54`: the earlier round's same 2,451,168 B, so the binary's size never moved and only its md5 did, with the build path. The deb itself is 1,028,656 B against that round's 1,028,632 B. The seat's pin is the same version, so the install replaces it in place.

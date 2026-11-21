# ut99 - pins, the measured round, and the open items

Detail file for `ut99_469f.bb`, beside the minimal `README.md`: every number in
here was measured on the seat with the package this recipe builds.

Unreal Tournament 99, as the seat plays it: Epic's GOTY CD1 data tree with
OldUnreal's 469f arm64 engine on top, installed at `/gamedata/ut` and started
with `ut99`. It is the only game on this seat whose data is a full retail disc
rather than a demo, and the only UE1 game that draws: Unreal Gold and UT2004
were tried and stopped at the renderer (see `TODO.md`).

**This build is for my own use, no redistribution.** The game is Epic Games';
the terms are the Epic Games Terms of Service that OldUnreal's `LICENSE.md`
opens with (`The Epic Games Terms of Service apply to the use and distribution
of this game`), and nothing in this recipe or on this seat is a free licence.
The recipe builds a local package for the seat's owner's own hardware and the
result is not published, sold or handed on.

## What the recipe installs

| Half | Source | Size |
| --- | --- | --- |
| Game data | `https://files.oldunreal.net/UT_GOTY_CD1.ISO`, sha256 `e184984c…74704f` | 649,633,792 B ISO; 627 files / 553 MB extracted |
| Engine | OldUnreal `v469f-rc6`, asset `OldUnreal-UTPatch469f-Linux-arm64.tar.bz2`, sha256 `56c09d3f…52ef46` | 75,147,582 B |
| Configuration | `files/UnrealTournament.ini` | SoftDrv, 1920x1080 |
| Launcher | `files/ut99` | one instance, wayland + alsa, 1920x1080 |

The engine the seat runs is one binary rather than two: `/gamedata/ut/System/ut-bin` at 214,424 B,
an aarch64 PIE, and the same file the unreal-gold recipe installs at `/gamedata/ugt/System/ut-bin`.
`do_extract_gamedata` puts the two halves together, in that order, with `bsdtar`
from `libarchive-native` taking the ISO (libarchive's ISO9660 reader, Rock Ridge
names kept, which is what keeps the disc's MixedCase `System/`, `Maps/`, ...):
only `System/`, `Maps/`, `Music/`, `Sounds/`, `Textures/` and `Help/ReadMe.htm`
are taken, the Windows installer's `*.dll`, `*.exe`, `*.bat`, `*.ico`, `*.bmp`,
`*.cab` and `*.inf` inside those directories are skipped, and `DirectX7/`,
`GameSpy/`, `NetGamesUSA.com/`, `Microsoft/`, `System400/`, `Logs/` and `Setup.exe`
on the disc's top level are outside the named dirs and never come near the tree.
627 files, 553 MB. The 469 tarball is then unpacked over the result with
`tarfile`, so its `System/` binaries and `.u` packages replace the 436 ones from
the CD and its `SystemLocalized/`, `Web/`, `Textures/` and `Help/ReleaseNotes.htm`
are added. What `dpkg-deb -c` then lists is 2,260 entries, and the installed
tree is 690 MB on the seat.

## Installing on the seat

The tree lives on `/gamedata`, the 8 GB ext4 data partition, not under
`/usr/share`: the 4.2 GB root filesystem has 224 MB free and the package does
not fit. From the build host, copy the deb to the seat and extract it onto
`/`, where the package's own paths already start with `/gamedata/ut`. On this
deb that means pulling `data.tar.xz` out of the `ar` container by hand, because
`dpkg-deb -x` cannot read it on the seat - the measured failure is below.

`dpkg -i` is deliberately not used: it would copy the 380 MB deb into
`/var/cache/apt` on the root filesystem first. The package has no dependencies
the seat does not already have - the engine carries its own SDL 3.4.10, OpenAL
Soft, libsndfile and mpg123 in `System/`, and the rest is the seat's libc,
libstdc++ and ALSA.

`dpkg-deb -x` does not work on this deb on the seat, and the failure is
measured, not guessed: `dpkg-deb -I` and `-f` answer, while `-c` and `-x` stop
after 119 entries with

```
dpkg-deb (subprocess): decompressing archive 'ut99.deb' (size=380371124)
  member 'data.tar': lzma error: compressed data is corrupt
tar: Unexpected EOF in archive
```

the seat's dpkg is `1.22.0-dirty` (arm64) and its `xz -t` of the same bytes,
taken out of the deb by hand, answers fine. The smaller app debs this layer
builds - up to `librequake` at 97 MB - do extract with `dpkg-deb -x`. The deb
itself is not at fault: the build host's dpkg `1.22.6` lists all 2260 entries of
the same file. Until that seat-side dpkg is understood, the install command is
the ar member pulled out by hand: its 60-byte header starts 1,940 bytes in
(`od -j 1940 -N 60 -c ut99.deb` prints `data.tar.xz ... 380369124 \``, and the
member's bytes start at header + 60 = 2,000):

```sh
tail -c +2001 ut99.deb | head -c 380369124 | xz -dc -T1 | tar xf - -C /
# the engine writes UnrealTournament.ini, User.ini, Cache/ and save games in
# place, and the extracted files keep the uid of the bitbake run
chown -R weston:weston /gamedata/ut
```

`/etc/asound.conf` must already force the 48 kHz family on `pcm.!default`.
The seat's I2S-to-HDMI sink is clocked for 48 kHz only; a 44.1 kHz source is
mis-clocked and plays roughly 2.6 times too fast. The same pin is what gave
Doom, Heretic and Hexen their sound back on the seat after this round, so it
is load-bearing for the Chocolate Doom payloads too, not only for UT99. The shipped ini keeps UT's
own `OutputRate=44100Hz` because `ALAudio` notices the sink's rate and falls
back correctly (`Can't set OutputRate of 44100, using system (OS) defaults of
48000`), so the game plays at the sink's rate instead of fighting it. The
48 kHz pin is an OS-layer change and is not in this recipe; it belongs in
`meta-watermelon-wine` (see `TODO.md`).

## Running it

```sh
ut99                                  # DM-Deck16][ bot deathmatch
ut99 DM-Curse][.unr                   # another arena
UT99_MAP=DM-Morpheus.unr ut99         # same thing through the environment
```

The launcher `cd`s to `/gamedata/ut` first - every path in
`UnrealTournament.ini` is relative (`../System/*.u`, `../Maps/*.unr`) - then
runs `./System/ut-bin <map> -log`. Options:

| Variable | Default | Effect |
| --- | --- | --- |
| `UT99_DIR` | `/gamedata/ut` | data and engine tree |
| `UT99_MAP` | `DM-Deck16][.unr?game=BotPack.DeathMatchPlus?difficulty=3` | map URL |
| `UT99_ARGS` | empty | extra engine arguments, word-split on purpose |
| `UT99_SYSTEM_SDL3` | unset | `1` puts `/usr/lib` in front of the engine's bundled SDL 3.4.10 |

Ctrl+Shift+F makes the weston window fullscreen. The engine itself never asks
for fullscreen (`RequestFullscreen 0`); it opens a plain window at 1920x1080
and weston is what scans it out.

## Renderer: SoftDrv, and why

| Renderer | Result on this seat |
| --- | --- |
| `SoftDrv` | draws; shipped |
| `OpenGLDrv` | cannot load: needs `libGL.so.1`, the seat has Vivante EGL/GLES and no Mesa |
| `XOpenGLDrv` | gets a GLES 3.1 context with the system SDL 3.5.0, then dies in `BuildShaderProgram` because it requires `GL_OES_shader_io_blocks`, which the Vivante GLSL ES 3.10 compiler does not have |
| `VulkanDrv` | `No Vulkan device found supports the minimum requirements` |

The ini names `SoftDrv.SoftwareRenderDevice` for `GameRenderDevice`,
`RenderDevice` and `WindowedRenderDevice` so the engine does not spend its
startup on a fallback chain. The bundled SDL 3.4.10 cannot bring EGL up at all;
`UT99_SYSTEM_SDL3=1` is the switch for the GPU attempts, because the system SDL
3.5.0 does create the GLES context that XOpenGLDrv then rejects on its shader.

1920x1080 is the internal and the window size because the panel is 1920x1080.
At 1280x720 the engine still draws, but weston magnifies the frame to the
panel, which is what made the game look zoomed; native size avoids that at the
cost of fill rate. The seat is fill-rate bound on its Vivante GPU, and SoftDrv
is a CPU rasteriser, so 1080p is expensive; see `TODO.md` for the measured
tiers and the open fps question.

## What was measured

The proof run is the shipped launcher `/usr/bin/ut99` on the installed tree, at
the ini's 1920x1080, `DM-Deck16][.unr?game=BotPack.DeathMatchPlus?difficulty=3`,
weston running with `--debug` so `weston-screenshooter` can capture, and the
view turned with `ydotool` against a `uinput` device:

| Measure | Value |
| --- | --- |
| Engine RSS | 149,632 kB at 01:13, 152,344 kB at 01:34 |
| CPU | 101-102 % of one core |
| Startup | `Startup time: 44.701549 seconds`, then `Entering main loop` |
| Window | `SDLDrv: ResizeViewport: 1920 x 1080 Pixels - RequestFullscreen 1`, then `SDL_SetWindowFullscreenMode(borderless) succeeded - video driver: wayland` |
| Audio | `ALAudio: We are using OpenAL device: OpenAL Soft (ALSA Default)`, then `Can't set OutputRate of 44100, using system (OS) defaults of 48000` |
| Draw | five 1920x1080 RGB PNG captures, 19,933-32,258 distinct colours and 851,778-1,854,254 non-black of 2,073,600 px |
| Mouselook | after `ydotool mousemove --absolute` sweeps, 869/1920 columns and 854/1080 rows of a capture pair moved by more than 8 px and 88.9 % of pixels differ |
| Modules | `/proc/<pid>/maps` shows `SoftDrv.so`, `SDLDrv.so`, `ALAudio.so`, `libopenal.so.1`, `Engine/Core/Render/Fire/IpDrv/UWeb.so` and the bundled `libSDL3-3.0.so.0`: no `OpenGLDrv`, `XOpenGLDrv` or `VulkanDrv` anywhere in the process |
| One instance | the second `/usr/bin/ut99` refuses: `ut99: already running (lock /tmp/ut99.lock)`, rc=1 |

A re-verify round on 2026-11-15 ran the same launcher on the same map with four
shots at 1920x1080: **31,397-38,446 distinct colours**, 2,059,009-2,059,573 of
2,073,600 non-black, row lums over 4.2-97.0 in 84-85 distinct levels. The idle
pair kept 83.5 % of pixels identical with 16.5 % alive - bots and the HUD keep
this arena alive, so no idle pair on this class is expected to be bit-identical,
and the sweep pair answered 86.9 % of pixels differing by more than 1 and 40.6 %
by more than 8, which is the view turning. The console named the map
(`LoadMap: DM-Deck16][.unr?...?game=BotPack.DeathMatchPlus?difficulty=3`) and
repeated the audio line as this round saw it: `ALAudio: We are using OpenAL
device: OpenAL Soft (ALSA Default)` and `ALAudio: Version 1.1 ALSOFT 1.24.3
found`.

An earlier run of the same game at 1280x720 SoftDrv measured 138,512 kB RSS at
about 101 % of one core; the shipped configuration is the native 1080p one above.

Two instances must not run at once: with two `ut-bin` processes the player's
own weapon and footstep sounds disappeared, and a clean single-instance
restart brought them back. That is why the launcher holds a `flock` and
refuses when a `ut-bin` is already alive.

## Files

| File | What it is |
| --- | --- |
| `ut99_469f.bb` | the recipe: two pinned downloads, the bsdtar/tar assembly in `do_extract_gamedata`, the tree and the launcher |
| `files/ut99` | the launcher, installed as `/usr/bin/ut99` |
| `files/UnrealTournament.ini` | the shipped engine configuration: SoftDrv, 1920x1080, ALAudio, 44100/48000 fallback |
| `TODO.md` | what is still broken or unmeasured: the GLES shader, Vulkan, the resize crash, fps, the asound.conf landing, the gamedata partition image |
| `README.md` | the load/use page: how the deb is unpacked onto the seat and how `ut99` is started |
| `PROOF.md` | this file |

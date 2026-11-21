# OpenJazz - the Jazz Jackrabbit 1 engine, and how to put an episode on it

## Load it

```bash
bitbake openjazz
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the engine only:

```
/usr/bin/OpenJazz    the engine: one C++ SDL2 program, GPL-2
```

Jazz Jackrabbit 1 is a 1994 Epic MegaGames platformer whose DOS binary (x86 `JAZZ.EXE`,
driven by `DPMI.000`) cannot run on the seat at all, so the game comes back through
OpenJazz, a from-scratch reimplementation of the same engine that reads Epic's **original
data files** - no conversion step, no extracted format.

## Play it

Put your own copy of an episode's data files where you like (here: on the seat's
`/gamedata` mount) and hand the directory to the engine:

```bash
export HOME=/home/weston XDG_RUNTIME_DIR=/home/weston WAYLAND_DISPLAY=wayland-1
export SDL_VIDEODRIVER=wayland SDL_AUDIODRIVER=alsa
/usr/bin/OpenJazz --fullscreen /gamedata/games/jazz1
```

The seat has no X server, so `SDL_VIDEODRIVER=wayland` is what SDL2 has to be pointed at,
and its sound is the 48 kHz ALSA sink, so `SDL_AUDIODRIVER=alsa`. The engine reads the
data under their **original DOS upper-case names** - `LEVEL0.001`, `STARTUP.0SC`,
`MENU.000`, `SOUNDS.000`, the `XM2.PSM` music modules - so a tree that renamed them is
read as a different game.

Any positional argument is a further game directory: `src/main.cpp:186-190` adds each
one as a `PATH_TYPE_GAME` path, and the directory compiled in through `-DDATAPATH`
(`CMakeLists.txt:153-155`) is the **last** path the engine tries - which is what lets a
hand-installed tree stay one argument and lets the compiled-in default be a fallback
rather than a place a recipe had to write data into.

Engine switches (`src/main.cpp:102-146`): `-f/--fullscreen`, `--window`,
`-s/--scale N`, `-m/--mute`, `-w/--world N`, `-l/--level N`, `-q/--quiet`.
The long forms need **two** dashes - `-fullscreen` is read by the bundled
`ext/argparse` as a short-option cluster and the engine exits
``unknown option `-fullscreen` `` before it opens any data, which is exactly what the
first seat round of this launcher did. `-m` mutes, which is how a round checks the
video path alone.

Settings and saves are XDG files, so the seat's `weston` user gets
`~/.config/openjazz` and `~/.cache/openjazz` (`src/platforms/xdg.cpp:32-36`) and no
root write is needed at run time.

## Two behaviours a proof script has to know

- The title animation runs about 20 s and eats any key pressed during it, so the first
  key press has to wait for the main menu - the log's `Opened file: .../MENU.000` is the
  marker.
- The episode list moves one row per press, but a **held** arrow key repeats: a 1 s hold
  of Down jumped from row 1 to row 6. Taps of 120 ms are what navigate it. Episode X
  (Holiday Hare) is row 9, because `episodeToWorld(9) = 50` is the world the
  `LEVEL0.050` belongs to.

## What may be downloaded, and where from

**No recipe here fetches Jazz data**, and the seat's Holiday Hare '94 episode was
installed by hand, because a recipe in this layer needs both halves of a grant and only
one of them passes. The licence half passes on Epic's own words (`LICENSE.DOC` is Epic
MegaGames' `SOFTWARE LICENSE AGREEMENT`, quotes in PROOF.md - the agreement is explicit
that the Program `is NOT public domain or free software or "freeware"`, so the episode is
free to download and free to play under a shareware agreement rather than under a free
licence, and a recipe carrying it would pin that as a `LicenseRef`, e.g.
`LicenseRef-Epic-SOFTWARE-LICENSE-AGREEMENT`). The download half fails: the only copy on
hand comes off an Internet Archive community item, which is not an authorized download
source, and Epic's own site does not carry the 1994 shareware episode.

The agreement's `complete package` clause is a second problem worth keeping: the release
zip holds 66 files, and a data-only tree keeps 60 because the six DOS-only members
(`JAZZ.EXE`, `SETUP.EXE`, `SETUP.INT`, `HELPME.EXE`, `DPMI.000`, `JAZZ.PIF`) do not run on
the seat - but term 4 forbids exactly that (`You may not change, delete, merge or rename
any files or elements of the Program in any manner, and the Program must be distributed as
a complete package`). Anyone who ships this episode again has to ship all 66 members and
let the engine read the data files it wants.

## Jazz Jackrabbit 2

JJ2 has its own experimental support in this same tree behind `-DENABLE_JJ2=ON`
(`CMakeLists.txt:241`), and it is **off** in this build on purpose: the JJ2 payload is a
different release, of a different game, and it stays a hand install of its own. JJ2's own
shareware is a Win32 self-extractor archive that every unpacker here refuses outright, so
this round could not open it at all - that is why only Jazz 1's Holiday Hare '94 is
proven on the seat.

## Measured

Pins, sizes, the drawn round, the launcher-bug round, audio and cost are in
[PROOF.md](PROOF.md).

## Files

```
openjazz_git.bb   the engine, built with the tree's own cmake
README.md         this file
PROOF.md          pins, sizes, measured rounds, failure modes
```

There are no local patches: the SDL2 choice, the network switch, the scaling switch and
the compiled-in data path are all options upstream's `CMakeLists.txt` already has.

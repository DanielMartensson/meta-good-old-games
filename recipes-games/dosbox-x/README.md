# dosbox-x - the x86 DOS emulator, and how to put a DOS game on it

## Load it

```bash
bitbake dosbox-x
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the emulator and its own
reference config - no game data:

```
/usr/bin/dosbox-x                     the emulator: one binary, GPL-2, SDL2 window-surface path
/usr/share/dosbox-x/                  dosbox-x.reference.conf, dosbox-x.reference.full.conf,
                                      FREECG98.BMP (the compiled-in VGA text font's bitmap)
```

## Play it

This emulator is how the seat runs x86 DOS software at all - there is nothing else on the
board that speaks x86 DOS. Put your own DOS tree on the seat's writable `/gamedata` mount
and give the run a `-conf` file of its own:

```bash
export HOME=/home/weston XDG_RUNTIME_DIR=/home/weston WAYLAND_DISPLAY=wayland-1
export SDL_VIDEODRIVER=wayland SDL_AUDIODRIVER=alsa
/usr/bin/dosbox-x -conf /gamedata/run/mygame.conf
```

`SDL_VIDEODRIVER=wayland` is not optional: without it the emulator exits
`E_Exit: Can't init SDL x11 not available`, because this board carries no X server. A run
that does start logs the line naming both backends, `LOG: SDL: version 2.30.1, Video wayland,
Audio alsa`.

The conf, and what each line buys (`/gamedata/run/d1fast.conf` and `d2fast.conf`, the two
Descent tests on this seat):

```ini
[dosbox]
machine = vgaonly          # the game's own 320x200 Mode 13 canvas; svga_s3 is only for
memsize = 16               # a DOS SETUP.EXE that wants a screen wider than 320x200
[render]
aspect = false             # fill the frame, no black bars
frameskip = 0
[sdl]
output = surface           # the only video system this build accepts
windowresolution = 640,400 # 2x integer scale of that canvas, so a screenshot lands on
fullscreen = false         # whole DOS pixels; true segfaults this weston
autolock = false
mouse_emulation = locked   # SDL2's own captured pointer, which is what a game that reads
                           # INT 33 gets its deltas from
[cpu]
core = dynamic             # the recompiling ARM core; cputype = auto
cycles = max
[mixer]
nosound = false
rate = 44100               # Descent's own digit stream is 44.1 kHz
[autoexec]
mount c /gamedata/games/descent1
c:
descent.bat -lowmem
```

A DOS release's own `.BAT` is what starts its own game - Descent v1.4's `DESCENT.BAT`
(163 B, `rem Batch file to run shareware version of Descent.`) runs the binary its installer
made. Which binary that is matters: the releases ship **bindable** images (`DESCENT.UBN`,
`D2DEMO.UBN`, `SETUP.UBN`) rather than executables, and `INSTALL.EXE` (a custom installer,
`$Id: install.c 1.52 1994/12/29 16:51:17 allender`) ARJ-unarchives the `.SOW` volumes and
then binds, so the runnable EXE exists only after the bind. The bind pair is byte-identical
across every D1 and D2 payload, and the seat ran it inside the emulator:

```
4GWBIND.EXE 4GWPRO.EXE D2DEMO.UBN D2DEMO.EXE > BIND.OUT   # 4GWPRO.EXE + D2DEMO.UBN => D2DEMO.EXE
```

## Why a `-conf` path and not a config file beside the binary

The search order at `src/gui/sdlmain.cpp:8745-:8776` is `-conf` files first, then the current
directory (`:8748` `ParseConfigFile("dosbox-x.conf")`), then the directory the executable lives
in, then `Cross::GetPlatformConfigDir()`. Run from `/usr/bin` with nothing next to it, DOSBox-X
would look in a read-only place, so the `-conf` slot is the only one that can be answered from
outside a writable tree - which is also why this recipe installs no launcher: every game gets
its own conf, written by whoever owns the game.

Never point DOSBox-X's stdout and stderr at an unbounded log on `/gamedata`: at the end of a
non-interactive autoexec the emulator asks `You are currently running a program or game.` /
`Are you sure to quit anyway now?` / `y/n:`, nothing answers, and it printed 4,500 pairs before
the run was stopped - about 4.7 MB in the first 400 KB of a log and 5.5 GB in the file that
filled the whole mount while still open on fds 1 and 2. Use `>/dev/null 2>&1`, or
`/gamedata/tools/caplog.py <cap>` which keeps the first N bytes and drains the rest.

## What may be downloaded, and where from

The emulator's own tree is GPL-2 from upstream GitHub. **No DOS game is fetched by any
recipe here**, and the two Descent tests on this seat were installed by hand, because a
recipe in this layer needs both halves of a grant and only one of them passes:

- The licence half passes, on the publishers' own words, verbatim from inside the payloads
  (quotes and pins are in PROOF.md).
- The download half fails: Descent and Descent II are Parallax Software's, published by
  Interplay - not id's, so there is no idgames-side release of them to fetch (`ftp.idsoftware.com`
  is a dead domain and its Wayback CDX holds 2,053 captures, 0 of them containing the string
  "descent"), there is no source release and no native port to build (so DOSBox-X is the only
  thing on the seat that can run them at all), and the copies on hand come off an Internet
  Archive community item, which is not an authorized download source.

The `UNMODIFIED FORM` clause also says why a repack would not have fixed that:
`Descent_896/desc14sw.ZIP` (3,003,254 B) ships a `DESCENT.PIG` of 2,509,799 B CRC
`774EE4E1`, which is not the official v1.4 media (5,092,871 B, CRC `DFA31C9A`), so that
package is a modified payload. The seat's own tree carries the official media, reassembled
by hand from the two ARJ volumes.

## Measured

Pins, sizes, the two Descent rounds, the gallery-crop route and the dead ends are in
[PROOF.md](PROOF.md).

## Files

```
dosbox-x_git.bb   the emulator, with its own autotools and the switches above
README.md         this file
PROOF.md          pins, sizes, measured rounds, failure modes
```

There are no local patches: the SDL2 probe, the GL-free pixel path and the ARM cpu core are
all switches the tree's own `configure.ac` already has.

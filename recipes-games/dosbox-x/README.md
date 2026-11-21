# dosbox-x - the x86 DOS emulator, and how to put a DOS game on it

## Load it

```bash
bitbake dosbox-x
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the emulator and its
own reference config - no game data:

```
/usr/bin/dosbox-x                the emulator: one binary, GPL-2, SDL2 window-surface path
/usr/share/dosbox-x/             dosbox-x.reference.conf, dosbox-x.reference.full.conf,
                                 FREECG98.BMP (the compiled-in VGA text font's bitmap)
```

## Play it

This emulator is how the seat runs x86 DOS software at all - there is nothing else
on the board that speaks x86 DOS. Put your own DOS tree on the seat's writable
`/gamedata` mount and give the run a `-conf` file of its own:

```bash
export HOME=/home/weston XDG_RUNTIME_DIR=/home/weston WAYLAND_DISPLAY=wayland-1
export SDL_VIDEODRIVER=wayland SDL_AUDIODRIVER=alsa
/usr/bin/dosbox-x -conf /gamedata/run/mygame.conf
```

`SDL_VIDEODRIVER=wayland` is not optional: without it the emulator exits
`E_Exit: Can't init SDL x11 not available`, because this board carries no X server.
A run that does start logs the line naming both backends, `LOG: SDL: version 2.30.1,
Video wayland, Audio alsa`.

The conf, and what each line buys - this is the conf the Descent 2 gallery pair was taken
at, `/gamedata/run/d2p.conf`, whose only difference from the Descent 1 one is the mounted
tree and the line that starts the game:

```ini
[dosbox]
machine = vgaonly           # the game's own 320x200 Mode 13 canvas; svga_s3 is only for
memsize = 16                # a DOS SETUP.EXE that wants a screen wider than 320x200
[render]
aspect = true               # the 4:3 box the picture is letterboxed into
frameskip = 0               # dead on this route: sdlmain.cpp:1139 answers (void)frameskip
scaler = xbrz_bilinear forced  # the only scaler measured to let the picture fill that box
xbrz fixed scale factor = 2 # what the seat's own scaling maths answers at this window
xbrz max scale factor = 0   # 0 = no ceiling: the fixed factor above is the one that runs
[sdl]
output = surface            # the only video system this build accepts
maximize = true             # the window takes the whole panel: this is what fills the screen
fullscreen = false          # true segfaults this weston
windowposition = -          # left to the compositor, since maximize won it
showmenu = false            # no menu strip across the top of the window
showdetails = true          # cycles/ms, FPS and %RT into the window's title: this is what
                            # tells a running DOS side from a parked one from the seat
autolock = false
mouse_emulation = locked    # SDL2's own captured pointer, which is what a game that reads
                            # INT 33 gets its deltas from
[cpu]
core = dynamic              # the recompiling ARM core; cputype = auto
cycles = 10000              # not max: at this route max self-tunes the DOS side away
[mixer]
nosound = false
rate = 44100                # Descent's own digit stream is 44.1 kHz
[autoexec]
mount c /gamedata/games/descent2
c:
D2DEMO.EXE
```

Descent 1's own conf is the same with `mount c /gamedata/games/descent1` and `descent.bat
-lowmem`, since that release's own `.BAT` is what starts its own game.

Two window sizes are what this build answers, and which one a run gets is most of what
decides its rate. A conf that asks `maximize = false` and a `windowresolution` of its own
still gets a window of the DOS raster's own size, because those keys want an output that
scales in hardware and `output = openglnb` is refused here; `maximize = true` is the other
answer. What sits between them is the scaler rather than the window: with a scaler the
picture fills the panel and the emulator's own per-frame blit is handed the panel's box,
which costs enough to leave the DOS side a fraction of real time, and `scaler = none` at
that same maximized window keeps the DOS side at real time while leaving the picture at its
own size in the middle. The measured rates and the source line that sets the blit's
destination are in [PROOF.md](PROOF.md).

What the fills half is made of, in the source rather than in a guess:
`src/output/output_surface.cpp:147` runs `aspectCorrectFitClip` whenever `render.aspect`
is truthy, so `maximize = true` plus `aspect = true` makes the clip the 4:3 box of the
maximized window, and the scaler is what paints into that box - `scaler = none` leaves
the DOS raster at its own size, because that path's `SDL_BlitSurface` does not scale. The
cost half, and every measured number, is in [PROOF.md](PROOF.md).

A DOS side that is parked never animates, and the emulator says why: its `EndUpdate`
returns before touching the window when the DOS screen has not changed a line, so a game
sitting at its own menu reads FPS 0 and keeps being handed the same bytes. The synthetic
keystrokes the seat sends do reach the window - the board runs `seatd` and `ydotoold`
beside the compositor, and Enter, Space, Escape, a left click and a held arrow all land -
which is what gets a DOS release off its own boot screens at all. What each release needs
and what each Escape does inside the mine is in [PROOF.md](PROOF.md).

A DOS release's own `.BAT` is what starts its own game. The two Descent releases ship
**bindable** images (`DESCENT.UBN`, `D2DEMO.UBN`, `SETUP.UBN`) rather than executables,
so the runnable EXE exists only after the bind - and the seat ran that bind inside the
emulator itself:

```
4GWBIND.EXE 4GWPRO.EXE D2DEMO.UBN D2DEMO.EXE > BIND.OUT   # 4GWPRO.EXE + D2DEMO.UBN => D2DEMO.EXE
```

## Why a `-conf` path and not a config file beside the binary

The search order at `src/gui/sdlmain.cpp:8745-:8776` is `-conf` files first, then the
current directory (`:8748` `ParseConfigFile("dosbox-x.conf")`), then the directory the
executable lives in, then `Cross::GetPlatformConfigDir()`. Run from `/usr/bin` with
nothing next to it, DOSBox-X would look in a read-only place, so the `-conf` slot is
the only one that can be answered from outside a writable tree - which is also why this
recipe installs no launcher: every game gets its own conf, written by whoever owns the
game.

Never point DOSBox-X's stdout and stderr at an unbounded log on `/gamedata`: at the end
of a non-interactive autoexec the emulator asks `You are currently running a program or
game.` / `Are you sure to quit anyway now?` / `y/n:`, nothing answers, and the log grows
without bound while still open. Use `>/dev/null 2>&1`, or
`/gamedata/tools/caplog.py <log> <bytes>` which keeps the first N bytes and drains the
rest.

## What may be downloaded, and where from

The emulator's own tree is GPL-2 from upstream GitHub. **No DOS game is fetched by any
recipe here**, and the two Descent tests on this seat were installed by hand, because a
recipe in this layer needs both halves of a grant and only one of them passes:

- The licence half passes, on the publishers' own words, verbatim from inside the
  payloads (quotes and pins are in PROOF.md).
- The download half fails: Descent and Descent II are Parallax Software's, published by
  Interplay - not id's, so there is no idgames-side release of them to fetch (`ftp.idsoftware.com`
  is a dead domain and none of its Wayback captures contains the string "descent"; `ftp.gwdg.de/pub/misc/ftp.idsoftware.com/idstuff/` is a mirror of id's old tree
  and is never fetched from), there is no source release and no native port to build (so DOSBox-X
  is the only thing on the seat that can run them at all), and the copies on hand come off an
  Internet Archive community item, which is not an authorized download source.

## Measured

Pins, sizes, the Descent rounds, the fill route's frame rate and the dead ends are in
[PROOF.md](PROOF.md).

## Files

```
dosbox-x_git.bb   the emulator, with its own autotools and the switches above
README.md         this file: load and use
PROOF.md          pins, sizes, measured rounds, failure modes
```

There are no local patches: the SDL2 probe, the GL-free pixel path and the ARM cpu core
are all switches the tree's own `configure.ac` already has.

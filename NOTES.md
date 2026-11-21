# meta-good-old-games

Yocto `scarthgap` layer: the good old games that run on the seat. One branch
per Yocto release, and `scarthgap` is the branch that carries the layer - `main`
is only the index page. The games that are not applications stay here, the
applications that are not games live in `meta-embedded-apps`, and everything
that is OS-level lives in `layers/meta-watermelon-wine`.

The layer's rules: a game counts as playable only with proof that it draws
plus working audio and working mouse/keyboard control; a game is in only if it
is free to download **and** free to play, on the rights holder's own words,
pinned as a `LicenseRef` in the recipe e.g shareware/freeware/demo; every
recipe has its own `README.md` that states the actual command that runs the
game; payloads install one at a time so the seat's root filesystem can hold
them. A payload also has to come from a download source the rights holder
authorizes - an unsure source is not good enough. For the id family that
corner used to be `idstuff` on `ftp.idsoftware.com`, and today that tree is
served by a third party at `ftp.gwdg.de/pub/misc/ftp.idsoftware.com/`: nobody
from id answers for it, and whether id still wants that archive to exist is
unknown, so it is not an authorized source, no recipe in this layer fetches
from it, and every id-family game's data is a hand install against the
engine's own README instead. Where the engine first chosen for a payload did
not draw, the engine was swapped and the data kept: the rule is that the game
has to work, not that a particular engine has to.

## The engines, and what each one is for

| Recipe | What it is, and what its README teaches |
| --- | --- |
| `recipes-games/chocolate-doom/chocolate-doom_git.bb` | the one engine the DOOM, Heretic and Hexen games run on: five programs out of one codebase, and the WAD's own file name is what decides the game |
| `recipes-games/fteqw/fteqw_git.bb` | the GLES-only Quake client: Quake 1, QuakeWorld, Quake II, Quake III and Hexen II, each one a `-basedir`/`-game` pair and a protocol switch |
| `recipes-games/ioquake3/ioquake3_git.bb` | ioquake3 1.36 as one opengl2/GL ES client, which is the engine that made the Quake III route draw, and what Quake III: Team Arena is played on |
| `recipes-games/eduke32/eduke32_20260807.bb` | EDuke32 built with its CLASSIC software renderer, which is the renderer this Vivante seat can pay for |
| `recipes-games/openjazz/openjazz_git.bb` | OpenJazz, the SDL2 reimplementation that reads Epic's original Jazz data in place; JJ2 is one cmake switch away |
| `recipes-games/dosbox-x/dosbox-x_git.bb` | the one thing on the seat that speaks x86 DOS, and what the two Descent shareware tests run on |
| `recipes-games/xash3d-fwgs/xash3d-fwgs_git.bb` | the GoldSrc engine built as an OpenGL ES client, which is the only renderer this EGL/GLES-only seat can drive |
| `recipes-games/opennow/opennow_git.bb` | signs the seat into the CloudMatch account; the stream itself is the host's |
| `recipes-games/opennowlite/opennowlite_git.bb` | the client that plays the host's stream on this seat, in its own Wayland window |
| `recipes-games/librequake/librequake.bb` | the free Quake data, installed as an `id1` tree FTEQW reads |
| `recipes-games/openarena/openarena.bb` | OpenArena 0.8.8's free arena data, the same shape |
| `recipes-games/ut99/ut99_469f.bb` | UT99 GOTY on OldUnreal's 469f arm64 engine, at `/gamedata/ut`, drawn by SoftDrv |
| `recipes-games/unreal-gold/unreal-gold_1.0.bb` | Epic's 1998 campaign inside that same 469f engine, at `/gamedata/ugt` |
| `recipes-devtools/rust/rust-1.85.0-host.bb`, `rust-1.89.0-host.bb` | the host cargo the two CloudMatch clients' FFI builds against: `rust-1.89.0-host` is the one `opennow` and `opennowlite` resolve in their `DEPENDS`, and `rust-1.85.0-host` is kept for the older OpenNOW rounds and is named by no recipe in this tree now |
| `recipes-multimedia/libsdl3/libsdl3_3.4.16.bb` | SDL 3.x as the CMake package OpenNOW's build asks for (`find_package(SDL3 REQUIRED CONFIG)`): the seat carries `libsdl3-0 3.4.16-r0.0`, but no binary on it loads `libSDL3` at runtime, since the games this layer ships link SDL 2 |

Patches this layer applies to upstream live in each recipe's `files/` and are
named for what they fix; the recipe's own README says why each one is needed.
Nothing else is vendored here, and a Quake-style game cross-compiles against
oe-core's own SDL 2. The OpenNOWLite client now fetches the same upstream tree
the opennow recipe fetches, at the same pinned tag, and applies its own one
patch to it: the two clients build their streamer FFI out of a fetch of one
upstream pin rather than one of them carrying a second copy of it.

Every recipe has two documents beside it: a `README.md` that only says how the
package gets on the seat and how the game is started, and a `PROOF.md` that
carries the pins, the sizes, the checksums, the measured rounds and the
failure-mode table. The one running log of every round that changed this layer
is numbered and kept whole in `recipes-games/opennow/PROOF.md`, whose items the
other recipes quote by number; the audit items do not move into the per-recipe
files, because the items quote each other by number.


## The game tables

The three tables of the shelf - the games the seat plays, the games that are
not a recipe and why, and the games that are free both ways but have no payload
on hand - are in [games-table.md](games-table.md), so they stay with the shelf
they describe and one screen long. What a game needs to count as playable, and
what was drawn for each one, continues below.

## Draw proof

A game counts as playable only with a screenshot of a rendered world measured
by `tools/shotstat.py` and `tools/imgdiff.py`, plus working audio and working
control - mouselook for the shooters, the keyboard walk for the platformers,
since a platformer has no mouselook to prove. Each engine's README carries its
own last proof round: the shots, what they measured, and the console lines that
name the seat's audio sink. The seat's own screenshots live on its `/gamedata`
mount at `/gamedata/installs/shots/gallery/<game>/`, two per game: `start.png`
is the frame the game opens its play on, `idle.png` the same frame seconds later
with nothing at all pressed between the two, so the gallery is the opening frame
rather than a frame the player had already run or jumped into. Getting there is
what each engine's own launcher arguments are for - `+wait 3` plus a named map
for the Quake-family clients, `-warp 1 1 -skill 3` for the Chocolate Doom pair,
`UG_MAP=Vortex2.unr` for Unreal Gold's first playable map rather than its intro
flyby, and a walk of ten/twelve Enters plus one Escape for the two Descent
briefings, the Escape being what skips the flight-officer film into the mine.
Quake III's dump is the ioquake3 route's pair, because that recipe's README says
ioquake3 is how the payload is played, and that round also proved the FTEQW route
draws once a dedicated server owns the arena and the client connects to it, at
the launcher's own defaults and with no map argument at all.

The two Descent pairs are DOSBox-X's own picture at the seat's whole panel, and the
fill route that makes it so was measured rather than assumed: `[sdl] maximize = true`
makes the window the panel - the run then logs `surface consider=1920x1048
final=1920x1048` and weston's scene graph gives the window's geometry as `(0, 0) ->
(1920, 1048)` - and `[render] aspect = true` plus `scaler = xbrz_bilinear forced` with
`xbrz fixed scale factor = 2` is what paints into the 4:3 box that
`aspectCorrectFitClip` computes inside it, 1397 px of the 1920, with black bars at the
sides. Every `scaler = none` spelling of `aspect` prints the same `consider=1920x1048
final=1920x1048` and still draws about 640-700 px in the middle, because that path's
`SDL_BlitSurface` does not scale, so the scaler is what fills and the only levers left
are how much a presented frame costs: `[cpu] cycles = max` self-tunes to the emulator's
200/ms floor at this route and both games stay inside their own boot films, so the
confs carry an explicit count, which reads `10000 cycles/ms` rather than that floor.
What is still inert is what the old route relied on being inert: `windowresolution` and
`windowmaximize` need an output that does hardware scaling - a windowed run's own log
answers `consider=320x200` against a `windowresolution = 640x400` ask, which leaves this
route exactly two sizes, the DOS raster's own window and the maximized panel - `output
= openglnb`/`openglpp` are still refused, there is no X server for `videodriver = x11`,
`fullscreen = true` still segfaults this weston, and `[render] frameskip` is taken and
answered `(void)frameskip;//UNUSED`. The frame cost itself is now measured against the
window rather than assumed: the maximized window at the scaler plays the mine at a few
frames and hands the DOS side only 21-55% of real time, the same maximized window at
`scaler = none` runs at 10-20 fps with the DOS side at 99-101% of real time, and the
320x200 window at the scaler runs at 7-19 fps with it at 87-100%. The panel is not what
costs - the scaler's destination is, since the emulator hands `xBRZ_PostScale` the window's
own 4:3 box, 27 times as many pixels at the panel as in that window, on top of a DOS side
that already costs nearly a whole core of this Cortex-A35. The superseded route - a 640x417
window, with `tools/doswindow.py` cutting
the emulator's own window out of the desktop capture from the wallpaper's navy and gold
fields and cutting below the 17 px menu strip, then stretching that 640x400 crop to the
frame - keeps its measurement in the engine's `PROOF.md`.

The streaming pair's dumps are the seat's own `weston-screenshooter` captures
while OpenNOWLite holds the window: Dark and Darker drew its menu and its 3-D
ranger, and Destiny 2's dump is the deepest capture of its host launcher's
window, which is all the stream has shown beyond its own media start so far -
measured down to about 439 s of media. OpenNOWLite's own window now also draws
the queue wait inside itself: an elapsed clock plus an attempt counter, and a
persistent `QUEUE n  ETA mm:ss` line while the host reports a queue position, so
the wait is readable on the seat's own screen rather than only in a log.

## This branch is one commit old

The branch was rebuilt from scratch on 2026-11-20: the previous history, whose
tip was `17a99f9` on the layer repository and `aa1ded00` in the parent
repository, carried the ten id and 3D Realms payload recipes (`doom1-demo`,
`doom2-demo`, `heretic1-demo`, `hexen-demo`, `hexen2-demo`, `quake1-demo`,
`quake2-demo`, `quake3-demo`, `quakeworld`, `duke3d-demo`) and their
`gwdg.de`-served `idstuff` fetches. Those fetches were dropped for the reason
above, the games became hand installs against the engine READMEs, and rather
than keeping a history whose commits install what this round stops shipping,
the branch was replaced by one fresh commit that carries the whole layer and
this record. The old commits are gone from the remote; nothing on the seat
depends on them.

## Building

```bash
bitbake chocolate-doom fteqw ioquake3 eduke32 openjazz dosbox-x xash3d-fwgs
bitbake librequake openarena ut99 unreal-gold opennow opennowlite
bitbake st-image-weston
```

A package from this layer is built as an `arm64` `.deb` in
`tmp-glibc/deploy/deb/cortexa35/` and installed on the seat with `dpkg -i`; a
tree that has to go on the seat's `/gamedata` mount is unpacked there with
`dpkg-deb -x` instead, because the root filesystem is nearly full and `/gamedata`
is the 8 GB data partition. The deb is relayed host to local to seat, because
this board's build host cannot ssh to the seat.

## Companion layers

`openembedded-core`, `meta-oe`, `meta-python`, `multimedia-layer` are required.
`meta-qt6`, `meta-rust-bin` and `meta-clang` are re-added by the Watermelon-Wine
BSP manifest and must be on `BBLAYERS` as well. Network access is needed during
the build, for the source tarballs and the two CloudMatch clients' cargo.

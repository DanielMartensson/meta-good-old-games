# OpenNOWLite - the plan-B client: its own window, its own pipeline

Detail file for `opennowlite_git.bb`. This file is instructions only. The
switch-by-switch table is `files/README.md`, the OpenNOWLite project's own
README kept in this recipe's `files/` beside the fetched upstream tree; every
number, pin and measurement lives in [PROOF.md](PROOF.md).

## Load it

```bash
bitbake opennowlite
```

The `arm64` deb installs three files, and nothing else:

```
/usr/bin/opennow-lite                                 the client (this recipe's own C source)
/usr/bin/opennowlite                                  the launcher
/usr/lib/opennowlite/libopennowlite_streamer_ffi.so   OpenNOW's streamer, used as a carrier only
```

It shares no path with `opennow`, and OpenNOW's own library at
`/usr/lib/libopennow_streamer_ffi.so` is never loaded, shadowed or replaced:
the client links `-ldl` only and opens its own library by absolute path.

## Run it

```bash
opennowlite --launch-app-id=ID
opennowlite --search-game-id="dark and darker"
```

A launch needs one of the two, never both. `--launch-app-id` is a CloudMatch
**app id** and is used as it stands; `--search-game-id` is a **title's name**,
which this client resolves itself by asking the shipped core for the same
library the OpenNOW window lists, a page at a time - the name is matched
case-insensitively against every title, and the one title carrying it supplies
its own `launchAppId`. Exactly one title carrying the name is what starts a launch:
none is refused with the count the list answered, more than one is refused with
every title and id printed, so a launch never starts against a guess and a
human picks one of them with `--launch-app-id` instead. Both spellings work,
`--search-game-id=<name>` and `--search-game-id <name>`. For the ids on this
board: the D&D series is `102303211` and Destiny 2 is `100362311`.

While the vendor still holds a launch, the window's banner says the wait as the
queue the account is paying rather than as a clock alone: `<n> QUEUES LEFT` with
the vendor's own eta beside it, and the same number is logged once every time it
changes. The wait's own summary line carries it too, as the position the last
poll answered and the lowest one seen.

The client opens its own xdg-shell window (`app_id=opennowlite`, 1920x1080 by
default; `--width`/`--height`, or `--fullscreen`) and draws the stream into it
with waylandsink, so there is no KMS hand-over to arrange. The seat's keyboard
and pointer reach the stream through this window's own `wl_keyboard` and
`wl_pointer` listeners, so the seat's keyboard keeps focus on it.

To run a round nobody is in front of and still read what the screen showed:
`--record=FILE` writes the first `--record-seconds=S` of picture as a raw H.264
witness, and `--auto-pilot` answers the host's own notices (`--pilot-at=`).
Their switches are in `files/README.md`.

## Two rules that decide a round

**One client at a time.** `opennow-core` holds an exclusive lock over its data
directory, so this client and `opennow` cannot stream at the same moment. A
launch that answers `The OpenNOW data directory is already in use` met a client
that is still alive: check before launching, and kill the orphan rather than
launching over it - a core whose client was killed still holds the lock, and its
`session_conflict` answers for 80-120 s before the core releases.

**Quit.** Hold **Ctrl+Alt+Shift+O** for about 0.4 s: the client exits through
its own shutdown path and closes the stream. If that is unavailable,
`kill -TERM` the recorded client and the `opennow-core` it spawned - never kill
`weston`, `weston-keyboard`, `weston-desktop-shell` or `seatd`.

## Why it is here, in one line

The shipped OpenNOW client is the one that works on this seat's GPU, and it
costs the picture: its window is one Qt compositor that has to render the
interface, decode the stream and present it in the same loop, and that is where
this seat's frame rate is lost. OpenNOWLite keeps OpenNOW's carrier and OpenNOW's
core for the session, the sign-in and the encoded bytes, and replaces the
decoding and the presenting with this program's own window and GStreamer chain,
which is where the streamer's own counters say the vendor sent 59.4 frames a
second for a whole hour. It is the plan-B client beside `opennow`, not a build
of it.

## Pinned, measured, audit

Pins, patch, the measured rounds, the failure-mode table and the open items for
this client are in [PROOF.md](PROOF.md); the numbered rounds that changed this
client are recorded in the layer's shared running log, in
`../opennow/PROOF.md`, whose items 66-69 are quoted by number here.

## Files

```
opennowlite_git.bb   the recipe
README.md            this file
PROOF.md             pins, measured rounds, open items
files/               opennow-lite.c, the launcher, this project's own README,
                     the id helper, LICENSE, the one patch
```

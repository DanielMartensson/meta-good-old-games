# OpenNOW - the sign-in client, and the one window that streams

Detail file for `opennow_git.bb`. This file is instructions only: how the
package loads, how to start it, the knobs worth knowing. Every pin, patch,
measurement and every round that changed this layer lives in
[PROOF.md](PROOF.md), whose running log of items 1..69 is quoted by number from
the other CloudMatch client's page.

## Load it

```bash
bitbake opennow
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the shipped client
and its core, and the Rust streamer library OpenNOW loads at run time:

```
/usr/bin/opennow                    the launcher this recipe writes
/usr/bin/opennow-qt                 the client: signs in, shows the catalog, hosts the stream window
/usr/bin/opennow-core               the core: catalog, CloudMatch consent, session, and the streamer it starts
/usr/bin/nanobrowser                the QtWebEngine browser this recipe builds beside it
/usr/lib/libopennow_streamer_ffi.so the streamer library, built from the same fetched tree
```

Nothing here is asked for a credential from outside: the core owns sign-in and
keeps the account in `~/.config/OpenNOW/fallback-sessions/`.

## Run it

```bash
XDG_RUNTIME_DIR=/home/weston WAYLAND_DISPLAY=wayland-1 opennow
```

This image's weston listens on `wayland-1` under `/home/weston`, so those two
names have to be preserved, not replaced. In the app: pick the backend your
machine offers and the codec **H.264** - this build is the bundled-FFmpeg +
V4L2-request one that makes the STM32MP25 VPU usable.

To keep every counter greppable in one session, which is what the numbers in
the record were measured from:

```bash
export OPENNOW_RENDERER=opengl
export OPENNOW_LOG_PRESENT=1 OPENNOW_LOG_FRAME_RATES=1
opennow 2>&1 | tee -a /home/weston/.runs/g1.log
```

## Which knobs are worth knowing

| Knob | What it does |
| --- | --- |
| `OPENNOW_RENDERER=opengl` | The interface draws with OpenGL ES instead of Vulkan. **Lowercase matters**: the test is `qgetenv("OPENNOW_RENDERER").toLower() == "opengl"`, so `vulkan` and unset are the same path. |
| `OPENNOW_SWAP_INTERVAL` | The interface window's present throttle: how many vsyncs must pass before the next presentation. Unset or `<= 0` becomes 2; `1` is upstream's every-vsync behaviour, which is the setting that reproduces the vendor assert. |
| `OPENNOW_CARGO_FEATURES` | The decoder the streamer is built with. For this SoC the only usable value is `linux-ffmpeg-bundled`; a distribution FFmpeg has no V4L2-request hwaccel and cannot replace it. |
| `NANOBROWSER_*` | The browser's own page/panel timings - the table lives in PROOF.md. |

## Running on one seat at a time

`opennow-core` holds an exclusive lock over its data directory, so OpenNOW and
the `opennowlite` client cannot stream at the same moment. Before a new session,
check that no client is already alive - an orphaned core keeps the lock and the
next launch dies with `The OpenNOW data directory is already in use`; the fix is
in `../opennowlite/README.md`.

For the whole browser: run `nanobrowser`, and as root disable its Chromium
sandbox with `QTWEBENGINE_DISABLE_SANDBOX=1`.

## Measured, troubleshooting, audit

Pins, configuration notes, the measured rounds, the layer-wide troubleshooting
table and the shared running log of every round that changed this layer (items
1..69, quoted by number from the other CloudMatch client's README) live in
[PROOF.md](PROOF.md).

## Files

```
opennow_git.bb  the recipe
README.md       this file
PROOF.md        pins, configuration record, measured rounds, audit log
files/          the one patch
```

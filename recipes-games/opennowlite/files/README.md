OpenNOWLite
===========

**A player.** It takes the stream that the shipped CloudMatch core allocates
and draws it on its own picture and audio pipeline instead of letting the
core's own front end present it: its own Wayland window, GStreamer (appsrc fed
with the vendor's H.264 and Opus bytes, waylandsink putting the decoded picture
into that window), the streamer's NVST input path and the kernel's ALSA sink.

It exists because the Qt front end that comes with the core has a present path
that loses the frame rate on this seat. OpenNOWLite asks the same core for the
same allocation and plays it itself. The core still does the account work:
sign-in, catalog, consent. The client draws and plays.

Two sentences about who builds it, because this is the question that keeps
coming back: **this directory is source only.** The binary you run on the seat
is produced by **Yocto / BitBake** from the recipe one level up,
`../opennowlite_git.bb`, which packages the result as an **arm64 `.deb`** the
seat installs with `dpkg -i`. Nothing on the seat is compiled or patched by hand.

This directory mirrors what that recipe ships: the same upstream pin, the same
one patch, the same `opennow-lite.c` and launcher. The upstream tree itself is
fetched by the recipe at a pinned `SRCREV`, so there is no `native/` copy of it
here any more; the patch is what the recipe applies to the fetched tree, and
[../PROOF.md](../PROOF.md) records the witness that the fetched-and-patched tree
is the tree the seat's older package was built from.


What the package is and what was proven
--------------------------------------

The recipe builds `1.0.5+release+git0+a9de957a59-r2.16`:

```
upstream   OpenCloudGaming/OpenNOW  tag v1.0.5  =  a9de957a5902c54186e9d4b44b1562f0b6beb951
branch     refs/heads/main          (the tag is the tip of main)
FFI ABI    18  (OPENNOW_STREAMER_FFI_ABI_VERSION at this pin; unchanged by this recipe's patch)
core RPC   6   (unchanged; the client still speaks it)
```

It installs three files and nothing else:

```
/usr/bin/opennow-lite                                 the client (this opennow-lite.c)
/usr/bin/opennowlite                                  the launcher (this opennowlite-launch.sh)
/usr/lib/opennowlite/libopennowlite_streamer_ffi.so   OpenNOW's streamer, used as a carrier only
```

Proof rounds ran end to end on the seat with that client, on Destiny 2
(`--launch-game-id=100362311`): picture, audio, keyboard, mouse, at the frame
rate the streamer's own counters report. Every number from those rounds - the
deb sizes and md5s, the delivered input counts, the audio packet counts, the
resident memory and the CPU share - is in [../PROOF.md](../PROOF.md); this file
stays about using the client.


Requirements
------------

One `aarch64` Linux host with:

* a Linux kernel with **FFSMAF** (the audio sink is ALSA: no PulseAudio, no
  PipeWire, and the client refuses a kernel without the format)
* a **V4L2**-capable stateless H.264 decoder for the SoC's GPU, plus the
  GStreamer base/bad plugins and `waylandsink`
* the **shipped OpenNOW CloudMatch core** next to it (`/usr/bin/opennow-core`),
  already signed in

`aarch64` only - the streamer library this recipe builds from upstream's tree is
`aarch64` code.


Install
-------

The install has exactly two parts: build the deb on the build host, and `dpkg -i`
it on the seat. There is no configure/make on the seat and no `apt`.

1. Source the OS build environment on the build host, in the machine's own
   build directory (`stm32mp25` machine, `cortexa35` deb directory).

2. Build OpenNOWLite. This layer is the only layer involved:

   ```bash
   bitbake opennowlite
   ```

   If the package was already deployed into the image, BitBake's database says it
   is up to date and nothing is rebuilt. Evict it first:

   ```bash
   bitbake -cc none opennowlite
   bitbake opennowlite
   ```

   Never leave `opennowlite` or `opennow` in the machine's "not tested by the CI
   build" list: that override wins before BitBake even parses the layer, and the
   build looks "successful" when nothing was rebuilt.

3. The deb lands in the deploy directory, with the package version in its name:

   ```bash
   find "$BUILDDIR"/tmp-*/deploy/deb/cortexa35 -name 'opennowlite*_arm64.deb'
   ```

   The `-dbg` and `-dev` packages come along; the client's own deb is the one to
   install.

4. Copy it to the seat and install it. The build host cannot reach the seat
   directly, so the deb is relayed through the local machine:

   ```bash
   scp opennowlite_..._arm64.deb root@192.168.1.33:/tmp/
   ssh root@192.168.1.33 'dpkg -i /tmp/opennowlite.deb'
   ```

   `dpkg -i` is the entire install procedure: OpenEmbedded built an arch-specific
   deb for the target machine and the seat *is* that machine, so nothing is
   rebuilt there. Do not rebuild on the seat and do not run `apt` there. Then
   delete the copy in `/tmp`.

5. Check that the new package is the one that got installed:

   ```console
   # dpkg -s opennowlite
   Package: opennowlite
   Version: 1.0.5+release+git0+a9de957a59-r2.16
   ```

   Because `PKGR` moved, `dpkg -i` answers `over (...-r2.15)` and replaces the
   client the seat carries rather than refusing a same-version install.

No reboot. The seat's desktop is a plain Wayland compositor, so a new package
needs nothing reloaded; if a client was running, stop it with the quit shortcut
(below) and start it again. Do not kill the desktop's own processes
(`weston`, `weston-keyboard`, `weston-desktop-shell`, `seatd`).


Quick start
-----------

```bash
opennowlite --launch-game-id=100362311
```

The launcher keeps whatever Wayland session the desktop already gives you and
only falls back the way the OpenNOW launcher does: this image's weston listens
on `wayland-1` under `/home/weston`, so `XDG_RUNTIME_DIR` and `WAYLAND_DISPLAY`
have to be preserved, not replaced. To run a round nobody is in front of and
still read what the screen showed, launch it detached:

```bash
XDG_RUNTIME_DIR=/home/weston WAYLAND_DISPLAY=wayland-1 \
  setsid nohup opennowlite --launch-game-id=ID \
  >/gamedata/run/lite.log 2>&1 </dev/null &
```

The client opens its own xdg-shell window (`app_id=opennowlite`, 1920x1080 by
default) and draws the stream into it, so there is no scan-out device to arrange
and no KMS hand-over. `--test-pattern` opens that same window and plays a test
pattern with no session at all, which is how the window and the input path are
proven without spending a session.

`--media-dead=S` ends the run after S seconds; without it the run lives until
you quit it. The client is detached with `setsid`, so the way to read it is the
redirect the launch writes: its logging is its stdout and stderr, one line per
event behind an `opennowlite:` or `opennowlite-input:` prefix, which is what the
markers in the record are grepped from. The one line that says the round is alive
is the queue clock: `opennowlite: queue wait=<n>s attempts=<n> queues left at the
last poll=<n> lowest seen=<n>`, where the queues left is the place in the
vendor's queue the same wait published.

**One client at a time.** OpenNOW and OpenNOWLite cannot stream at the same
moment: the core holds an exclusive lock over its data directory, and an
orphaned core keeps holding it. Check that no client is already alive before a
launch, and kill the orphan rather than launching over it.

**The quit shortcut is Ctrl+Alt+Shift+O** (the `KEY_O` keycode 24, registered
through the compositor). Hold it about 0.4 s and the client exits through its
own shutdown path: it closes the stream and releases the window. If that is
unavailable, `kill -TERM` the recorded PID pair (the client plus the `opennow-core`
it spawned). Never kill the desktop's own processes.


Picking a game by name
----------------------

The client takes a CloudMatch **app id**, and it can be handed a game's **name**
instead with `--launch-game-name` - the name does both steps, the id read and
the game started, in one call - which asks the shipped core for the same
library the OpenNOW window lists and takes the id off the title that carries
the name:

```console
$ opennowlite --launch-game-name="dark and darker"
opennowlite: library page 1 reply=<n> bytes items=100 more=1 matched=1
opennowlite: library page 2 reply=<n> bytes items=71 more=0 matched=1
opennowlite: library read pages=2 items=171 complete=1: the name "dark and darker" matched 1 title(s)
opennowlite: the name "dark and darker" is the title "Dark and Darker" at launchAppId 102303211
```

The name is matched case-insensitively against every item's title on every page,
and the id is that item's own `launchAppId`, so nothing is cached and nothing is
scraped: the list is the pages `catalog.library.list` answers, read one after
another with the cursor, catalog context and revision the previous page carried.
This account's library is 171 games in two pages (100 then 71), and a title two
pages both carry is kept once. A catalog that moves mid-read answers
`catalog_changed`, so a read that stops early is made once more from the first
page, and a launch is only started after a complete read. **Exactly one** title
carrying the name is what starts a launch, and the log names the title it landed
on. No title carrying it is refused with the number of items the pages did
answer; more than one is refused with every title and every id printed, so a
launch is never started against a guess and a human picks one with
`--launch-game-id=<id>` instead. Both spellings work, `--launch-game-name=<name>`
and `--launch-game-name <name>`; giving a name and an id together is refused too,
since that is one launch asked for two ways.


Credentials
-----------

**OpenNOWLite never reads, stores or asks for a credential.** No cookies, no
username, no password, no token. It asks the shipped core over JSON-lines RPC
for three things - `catalog.game.get`, `catalog.launch.inspect`,
`session.create` / `session.poll` - and the core owns sign-in, the catalog and
the CloudMatch consent, using the seat's existing signed-in account:

```
~/.config/OpenNOW/fallback-sessions/<hash>.json     <- read by the core, not by this client
```

* **Sign in once with OpenNOW** (the Qt client) before using OpenNOWLite. The
  account is then shared.
* If that record is missing or unreadable, the launch dies at
  `catalog.game.get authentication_required` after about 11 s. Put the record
  back or sign in again with OpenNOW, and never move that file to a backup:
  it is what makes a game launch at all. The other OpenNOW file the seat sees,
  `unresolved-allocation.json`, is a *failed allocation*: while it sits in the
  data directory it answers every new `session.create` with
  `session_allocation_recovery_required` and no game starts at all. The client
  now moves it into `backup/<epoch>-unresolved-allocation.json` at the start of
  every launch by itself, so a new game needs no hand to clear it first, and
  nothing is ever deleted.
* Do not hand-edit the record; the hash in its filename is not something to
  invent.
* `--data-dir DIR` makes the core use another data directory, which is how a
  sign-in can be done without disturbing the seat's account.


Command line
------------

A switch may be written `--flag=value` or `--flag value`. `--help` prints the
client's own full help text: the flags list and every explanation under it.
A wrong or unknown flag answers the flags list alone rather than all the
explanations. The examples below show what each placeholder takes.

| Switch | Meaning |
| --- | --- |
| `--launch-game-id=N` **(one of the two)** | CloudMatch app id (a positive integer). Not a game name or slug — a name goes to `--launch-game-name`. |
| `--launch-game-name=NAME` `--launch-game-name NAME` | A title's name instead of an id: the shipped core's own `catalog.library.list` is asked and the item whose title contains NAME supplies its `launchAppId`, and the game then starts. Case-insensitive, deduped by id. Refused when no title or more than one title carries the name, the second time with every title and id printed. |
| `--help` | Print the whole help text: the flags list and every explanation. |
| `--fullscreen` | Make the window the whole screen instead of a windowed surface. |
| `--width=N` `--height=N` | Window size, 1920x1080 by default. |
| `--test-pattern` | Open the window and play a test pattern through the default GStreamer description, with no session at all. |
| `--pipeline=DESC` | Same as `--test-pattern` but with your own GStreamer description (it needs an element named `sink`); this is how the window, the decoder and the sink are tested apart from a session. |
| `--mouse-absolute` | Put the pointer's place on the wire instead of a distance. |
| `--mouse-look=P` | How much of a turn one swept pixel is worth, 25..400, 100 by default. |
| `--no-pointer-lock` | Keep the distance path without locking the seat's pointer to the window. |
| `--show-cursor` | Keep the seat's desktop arrow over the window (default: the game's own pointer only). |
| `--auto-pilot` | Let the client answer the cloud game's own notices, so a round can run with nobody at the seat. |
| `--pilot-at=S` | The notice clock: a comma list of seconds after the stream starts, each optionally naming the key (`63:r,212:r` by default; `r` Return, `e` Escape, `s` Space, `c` a left click, `m` a MouseMove written as `<dx>x<dy>`, `:k` plus a number presses a physical key by evdev scancode, `:m` arms a MouseMove). |
| `--pilot-times=N` `--pilot-gap=S` | Repeat the notice clock N times, S seconds apart. |
| `--pilot-click` `--pilot-point=WxH` | Click, and where the click is aimed (a stated point is walked to by a distance). |
| `--record=FILE` | Write the picture out as a raw H.264 witness file. |
| `--record-seconds=S` `--record-from=S` | How much of the picture, and from when. |
| `--poll-attempts=N` | How many times, 2 s apart, a preparing session is polled (300 by default, so a free-tier queue wait does not end the launch). |
| `--debug-input` | Stamp every input submit with its offset inside the session and the cost of the streamer's own call. |
| `--debug-audio=FILE` `--debug-audio-seconds=S` | Also write the first S seconds (60 by default) of the stream's Opus frames to FILE as length-prefixed packets. |
| `--media-dead=S` | Stop after S seconds. |
| `--data-dir=DIR` | Run the shipped core against another copy of its data directory. |

The placeholders, with worked examples - `FILE` is any path the seat can write
to, and `/gamedata/run/` is where the witness files of the audit rounds went:

```bash
# launch by name: the id is read AND the game starts, one call
opennowlite --launch-game-name="dark and darker"

# launch by id, with a window that is not the default 1920x1080
opennowlite --launch-game-id=102303211 --width=1280 --height=720

# keep a 180 s picture witness that starts 170 s into the session, and give up
# when the picture has not advanced for 120 s
opennowlite --launch-game-id=100362311 \
  --record=/gamedata/run/witness.h264 --record-seconds=180 --record-from=170 \
  --media-dead=120

# the notice clock by hand: press Return at +63 s and +212 s, then jiggle the
# pointer 40 times 90 s apart, and click once at the middle at the end
opennowlite --launch-game-id=100362311 \
  --auto-pilot --pilot-at=63:r,212:r --pilot-times=40 --pilot-gap=90 \
  --pilot-click --pilot-point=960x540

# also keep the first 30 s of sound as Opus packets, and stamp what every key
# and click cost
opennowlite --launch-game-id=100362311 \
  --debug-input --debug-audio=/gamedata/run/sound.packets --debug-audio-seconds=30

# poll the allocation for 5 minutes rather than the default ten
opennowlite --launch-game-id=102303211 --poll-attempts=150

# turn: one swept pixel is worth 150 % of the default half-turn scale
opennowlite --launch-game-id=102303211 --mouse-look=150
```

So `FILE` above is like `/gamedata/run/sound.packets`, `S` is seconds (180 s of
picture, 30 s of sound, a 90 s gap), `N` is a count (`--pilot-times=40`,
`--poll-attempts=150`), `P` is a percent 25..400 and `WxH` is pixels
(`960x540` is the middle of the default window). `--pilot-at` carries its own
mini spelling: seconds after the stream starts, optionally `:` plus a key -
`r` Return, `e` Escape, `s` Space, `c` a click at `--pilot-point`, `:k57` the
physical Space by evdev scancode.


`OPENNOW_MOUSE_ACCELERATION` (1.0..150.0) from the process environment through a
once-evaluated lock, so a client that was execed bare cannot set them - `--mouse-look`
is the same scaling where it can.

The client's logging is its stdout and stderr: one line per event, with a
level-free `opennowlite:` / `opennowlite-input:` prefix, which is why a detached
launch carries a `>log 2>&1` redirect at all. The marker that says the round is
alive is the queue clock, `opennowlite: queue wait=<n>s attempts=<n> queues left
at the last poll=<n> lowest seen=<n>` — the wait's own length, its poll count and
the place in the vendor's queue it published — and the window shows the same
clock and the same place as a banner, the last one drawn as `<n> QUEUES LEFT`. There is no config file: the switches
are the whole interface.


Building from source
--------------------

The build you want is the one the recipe runs, because that is where the pin and
the compile flags are right: `bitbake opennowlite`. There is **no build system
in this directory** - no CMake, Meson or Makefile - and there does not need to
be one:

* `opennow-lite.c` is compiled by the recipe with `${CC}` (the cross-gcc) plus
  the generated `-protocol.c` files made by `wayland-scanner` from the
  `wayland-protocols` copies in the sysroot;
* the streamer library is built from the fetched-and-patched tree with
  `cargo build --offline --locked --lib --release --package opennow-streamer-ffi`,
  for this machine, and installed as `libopennowlite_streamer_ffi.so` under this
  recipe's own directory.

The client never compiles upstream's C++ - it links to the shipped core - so
moving to a different upstream means changing the pin lines in the recipe:

```bitbake
SRC_URI = "git://github.com/OpenCloudGaming/OpenNOW.git;protocol=https;branch=main ..."
SRCREV  = "a9de957a5902c54186e9d4b44b1562f0b6beb951"
```

The one thing that can break is the ABI contract: if a new tree's
`OPENNOW_STREAMER_FFI_ABI_VERSION` is not the value the fetched
`opennow_streamer_ffi.h` carries, this client and the library have to move with
the pin. The log prints what the client was built against.


Troubleshooting
---------------

What each failure means, and the fix that was measured against it, is the table
in [../PROOF.md](../PROOF.md). The short version: a launch that answers
`The OpenNOW data directory is already in use` met a client that is still alive;
`session_conflict` means the core still holds a session and releases its lock
after 80-120 s; `media.no_allocation` means the app id is wrong or the game is
not available to this account; and `catalog.game.get authentication_required`
means the core has no signed-in account to use. A launch that met a dead
allocation from a previous failure moves it into `backup/` itself first and
says so on the log, so `session_allocation_recovery_required` should no longer
need a hand at all.


Layout
------

| Path | What it is |
| --- | --- |
| `README.md` | this document |
| `opennow-lite.c` | the whole client: C, one translation unit, plus the generated `-protocol.c` files |
| `opennowlite-launch.sh` | the launcher: keeps the desktop's Wayland session and execs the client |
| `0001-hand-encoded-media-to-the-host.patch` | the one patch the recipe applies on top of the fetched upstream tree, in the form the recipe's patch task consumes it |
| `LICENSE` | what this project's own tree carries as documentation; the recipe's own license statement is in `../opennowlite_git.bb` |

The upstream Rust tree is not here: the recipe fetches it at a pinned `SRCREV`,
which is what keeps the tree the client links against reproducible.


License
-------

This is a front end for a subscription service with a proprietary backend. The
project carries no license of its own. **Use it only against an account you are
entitled to use.**

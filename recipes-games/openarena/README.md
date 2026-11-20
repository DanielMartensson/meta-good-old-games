# OpenArena 0.8.8 - the whole free game, and how the fteqw launcher plays it

## Load it

```bash
bitbake openarena
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the data and one
launcher:

```
/usr/share/openarena/baseoa        nine pk3s: the maps, players, weapons, shaders, sounds
/usr/share/openarena/missionpack   one pk3: the maps the 0.8.8 release adds
/usr/bin/openarena                 the launcher this recipe writes
```

`missionpack` is kept as a sibling directory rather than merged, so a map inside
it stays addressable by its own name.

## Play it

The launcher's defaults are already the seat's - `-basedir /usr/share/openarena
-game baseoa -quake3` plus the video sets - so a complete game command is:

```bash
export HOME=/home/weston XDG_RUNTIME_DIR=/home/weston WAYLAND_DISPLAY=wayland-1
/usr/bin/openarena +wait 3 +map oa_dm6
```

**`+wait` before `+map` is not decoration**: a bare `+map` queued before the
renderer is up leaves the game silent with a black screenshot; `+wait 3 +map`
loads every time. Maps in `missionpack` open the same way once their tree is the game - either
`OPENARENA_GAME=missionpack` in front of the line, which replaces the launcher's
own `-game`, or `+set fs_game missionpack` at the end of it, which the engine
answers after the launcher's defaults.

Override with `OPENARENA_BASEDIR` / `OPENARENA_GAME` / `OPENARENA_ARGS`, or pass
your own `-basedir`, `-game` and `-set` arguments at the end, which win.

## Why the launcher sets what it sets

| Set | Reason |
| --- | --- |
| `-quake3` stated by hand | FTEQW's table of named games has no OpenArena entry, so `-game baseoa` only names the tree and the protocol is stated explicitly - which protocol and which data are both spelled on the seat, nothing guessed |
| `-updatesrc ""` | has to stay on both the Quake and the OpenArena runs |
| `vid_gl_context_es 1` | the Vivante driver hands back an ES-CM 1.1 context when left alone and refuses the game then; this cvar asks for the GL ES 3.1 one |
| `vid_fullscreen 1` + `vid_width 1024 vid_height 576` | the seat's fill-rate answer in the shape FTEQW spells: the desktop stays 1920x1080 and the 3d scene is drawn into the smaller buffer the compositor scales up, and that buffer's area decides how smooth the game plays on this Vivante |

The Quake III gamecode is **inside** the client the fteqw recipe builds, not
loaded: its makefile links the quake3 plugin and botlib straight in, and running
the client prints `------- Quake III Arena Initialized -------`. So this payload
carries no code at all.

## What may be downloaded, and where from

OpenArena is free **both ways**, and that is why it is one of the two data
recipes this layer has: the project built its maps, models, textures, sounds and
music from scratch, and its own `readme_088.txt` inside the release zip carries a
LICENSE section - "The program and data are licensed under the GNU GPL v2. See
the file 'COPYING' for details." - with the GNU v2 text itself at the top of the
same zip. One free download gets the whole game, with no proprietary pak
anywhere in the payload. The pins and the licence notes are in PROOF.md.

## Measured

Pins, sizes, the fps ladder, the console lines and the Q3 gamecode's failure
modes are in [PROOF.md](PROOF.md).

## Files

```
openarena.bb    the payload: the release's own zip, installed as baseoa + missionpack
README.md       this file
PROOF.md        pins, sizes, measured rounds, failure modes
```

# FTEQW - the Quake-family engine, and how to put a game tree on top of it

## Load it

```bash
bitbake fteqw
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the engine only:

```
/usr/bin/fteqw-gles    the client: one binary, GL renderer built against this seat's GLES 3.1
/usr/bin/fteqw         the launcher this recipe writes; default = the LibreQuake tree
```

## Play it

FTEQW reads **one** game tree, the one `-basedir` + `-game` name, and the protocol
flag says which game that tree speaks. Put your own copy of a game's files where
you like (here: under `/gamedata/games/<name>`), keep the directory names the
engine asks for, and start the client yourself - the payload launchers this layer
used to ship are gone, and these commands are what they ran.

The seat needs its Wayland/SDL environment first:

```bash
export HOME=/home/weston XDG_RUNTIME_DIR=/home/weston WAYLAND_DISPLAY=wayland-1
export SDL_VIDEODRIVER=wayland SDL_AUDIODRIVER=alsa
```

**Quake 1** (an `id1` tree: `pak0.pak` inside it):

```bash
/usr/bin/fteqw-gles -basedir /gamedata/games/quake1 -game id1 -quake -updatesrc "" \
    -set vid_gl_context_es 1 -set vid_fullscreen 1 -set vid_width 1024 -set vid_height 576 \
    -set pr_ssqc_memsize 134217728 -set pr_csqc_memsize 64m +wait 3 +map e1m1
```

**QuakeWorld** (a `qw` tree holding `qwprogs.dat` + `files.dat`, plus the `id1`
episode tree beside it): same command with `-game qw` and **no protocol flag** -
FTEQW picks the protocol with the same first-match rule it uses for `-game`, and
a `-quake` earlier on the line would take the Quake 1 row and its progs instead.

**Quake II** (a `baseq2` tree). FTEQW carries no Quake II gamecode: `SVQ2_GetGameAPI`
dlopens a **native** `baseq2/game.so`, so this route needs that module as well as the
data, and the demo's players only appear when `baseq2/players/` is loose on disk and
not inside the pak.

```bash
/usr/bin/fteqw-gles -basedir /gamedata/games/quake2 -game baseq2 -quake2 -updatesrc "" \
    -set vid_gl_context_es 1 -set vid_fullscreen 1 -set vid_width 1024 -set vid_height 576 \
    -set pr_ssqc_memsize 16m -set pr_csqc_memsize 16m +wait 3 +map demo1
```

**Hexen II** (a `data1` tree, one pak; the game logic comes out of the `progs.dat`
inside it, so the engine needs no code of its own here):

```bash
/usr/bin/fteqw-gles -basedir /gamedata/games/hexen2 -game data1 -hexen2 -updatesrc "" \
    -set vid_gl_context_es 1 -set vid_fullscreen 1 -set vid_width 1024 -set vid_height 576 \
    -set pr_ssqc_memsize 16m -set pr_csqc_memsize 16m +wait 3 +map demo1
```

**Quake III Arena** (a `demoq3` tree). A Quake III client cannot host: `CL_Connect_f`
always calls `CL_Disconnect_f` first, which tears an in-process listen server down,
so the arena is owned by a second process the player connects to. Both processes are
`/usr/bin/fteqw-gles` - not the launcher, which would only re-exec itself:

```bash
/usr/bin/fteqw-gles -dedicated -basedir /gamedata/games/quake3 -game demoq3 -quake3demo \
    -set sv_listen_q3 1 -set sv_public 0 -set net_port 27960 \
    -set vid_gl_context_es 1 -set vid_fullscreen 1 -set vid_width 1024 -set vid_height 576 \
    -set pr_ssqc_memsize 32m -set pr_csqc_memsize 32m -set timeout 3000 -set cl_timeout 3000 \
    +wait 20 +map q3dm7 > /gamedata/logs/q3server.log 2>&1 &
# wait for "Server spawned" in that log, then the player:
/usr/bin/fteqw-gles -basedir /gamedata/games/quake3 -game demoq3 -quake3demo \
    -set vid_gl_context_es 1 -set vid_fullscreen 1 -set vid_width 1024 -set vid_height 576 \
    -set pr_ssqc_memsize 32m -set pr_csqc_memsize 32m -set timeout 3000 -set cl_timeout 3000 \
    -set net_port 27960 +connect 127.0.0.1:27960
```

Quake III also has its own engine on this seat, `/usr/bin/ioquake3`, and that is the
route that plays `q3dm7` and the Quake III demo's arenas most comfortably - see
`../ioquake3/README.md`. The FTEQW route above is kept because FTEQW ships a row of
its own game table for exactly this payload (`demoq3`, `demoq3/pak0.pk3`) and this
recipe patches its UI gate to take the demo's API-3 `vm/ui.qvm`.

The two games that already have their own launcher and data recipe on the seat need
nothing of this file: `/usr/bin/fteqw` opens LibreQuake (`-basedir /usr/share/librequake
-game id1 -quake`), and OpenArena has its own protocol row and its own
`/usr/bin/openarena`.

## Why the switches are these

- `-quake` / `-quake2` / `-hexen2` / `-quake3demo` state the protocol. A directory
  named `id1` does not say which game it is (LibreQuake lives in an `id1` tree too),
  and the first match wins, so the flag has to be there and no other protocol flag
  may come earlier on the line.
- `-updatesrc ""` keeps FTEQW from pulling other games' sources in.
- FTEQW answers the **first** `-basedir` and `-game` on its command line
  (`COM_CheckParm` -> `COM_CheckNextParm(parm, 0)`), which is why every game of this
  family starts with its own pair instead of adding one to `/usr/bin/fteqw`.
- `vid_gl_context_es 1` asks the Vivante for a GL ES 3.1 context; left alone it hands
  back an "OpenGL ES-CM 1.1" one and the renderer refuses it, so nothing draws.
- `vid_fullscreen 1 vid_width 1024 vid_height 576` draws the 3d scene into a smaller
  surface the compositor scales to the seat's 1920x1080. This is the setting that
  decides how smooth the game plays - the board is fill-rate bound (the ladder is in
  PROOF.md).
- `pr_ssqc_memsize` / `pr_csqc_memsize` size the two Quake-C VM arenas. Their `-1`
  default asks `malloc` for 2 GB, which fails on this 1,768 MB seat **before a map is
  drawn**, so they must be stated per game: Quake 1 and QuakeWorld need the big pair,
  Quake II and Hexen II run happily at 16m.
- `timeout 3000 cl_timeout 3000` on the Quake III route: both are inactivity timers
  whose 65/60 s defaults drop a motionless player back into its menu, and the menu
  is where the fake 160 fps number comes from.

## What may be downloaded, and where from

FTEQW's own tree is GPL-2 and comes from upstream GitHub. The id data trees above
are a different matter: id's own agreements do hand out the shareware episodes and
demo releases free of charge (`SLICNSE.TXT`'s section 6 "Permitted Distribution",
id's "LIMITED USE SOFTWARE DEMO LICENSE AGREEMENT"), but today the only carrier of
those exact archives is a third-party mirror of id's dead `ftp.idsoftware.com`
corner, and nobody from id answers for it. So no recipe here fetches them, and the
route is a hand install of a copy you already own. The one Quake-family game this
layer does fetch is LibreQuake - its own project's free content, downloaded from its
own repository - and OpenArena likewise.

## Measured

The pins, the payload sizes, the per-game rounds and the failure modes are in
[PROOF.md](PROOF.md).

## Files

```
fteqw_git.bb    the engine, built from upstream's tree with the three patches in files/
files/          the three local patches (GLES headers, two Quake III VM relaxations)
PROOF.md        pins, sizes, measured rounds, failure modes
README.md       this file
```

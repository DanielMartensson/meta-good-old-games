# LibreQuake - the free Quake data, and how the fteqw launcher mounts it

## Load it

```bash
bitbake librequake
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the data only - the
whole release's own tree, under the protocol's own game-directory name:

```
/usr/share/librequake/id1    pak0.pak + pak1.pak, the maps, the music, progs.dat, docs
```

## Play it

The fteqw launcher's defaults are already this tree (`-basedir /usr/share/librequake
-game id1 -quake`), so a complete game command is:

```bash
export HOME=/home/weston XDG_RUNTIME_DIR=/home/weston WAYLAND_DISPLAY=wayland-1
/usr/bin/fteqw +wait 3 +map lqdm3
```

This release is **deathmatch only**: the map list is `lqdm1` .. `lqdm13` and
`+listmaps` on the seat prints exactly those thirteen, with no campaign and no
demos - the single-player side of the data is simply absent, so a Quake round on
this payload means picking a DM map.

Override with `FTEQW_BASEDIR` / `FTEQW_GAME` / `FTEQW_ARGS`, or pass your own
`-basedir`, `-game` and `-set` arguments at the end of the line, which win:

```bash
# any other free Quake tree you own, under its own name
export FTEQW_BASEDIR=/gamedata/games/myquake FTEQW_GAME=mymod
/usr/bin/fteqw +wait 3 +map e1m1
```

`id1` is the Quake protocol's own name for the game directory, and `-quake` is
what states which protocol the tree speaks; FTEQW's table of named games has a
Quake entry, so the pair is matched, not guessed.

## What the payload holds

Inside `id1`: the two rebuilt paks, the thirteen DM maps as loose `.bsp`/`.lit`
pairs, the music as `track02..11.ogg`, the `progs.dat` the Quake-C multiplayer
code runs (which is why the launcher carries `pr_ssqc_memsize`), and the docs/
config set - `docs/` carries the two licence texts the split below spells out.
The tree arrives as the release's own `full.zip` and is installed unchanged, so
a tree that renamed anything is a different game to this engine.

## What may be downloaded, and where from

LibreQuake is the reason this layer can carry a **whole free game**: the project
rebuilt the Quake assets from scratch under their own licences, and one free
download gets the whole game with no proprietary pak anywhere in the payload -
which is exactly what the id Quake data does not have. The licence is the repo's
own two-text split (BSD-3 on the assets, GPL-2 on the quakec progs); the quotes
and the pins are in PROOF.md.

## Measured

Pins, sizes, the licence texts, the seat round and its fps ladder are in
[PROOF.md](PROOF.md).

## Files

```
librequake.bb   the payload: the release's own full.zip, installed as id1
README.md       this file
PROOF.md        pins, sizes, measured rounds, failure modes
```

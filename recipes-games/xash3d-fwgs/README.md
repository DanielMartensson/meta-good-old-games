# xash3d-fwgs - the GoldSrc engine, and how to put a GoldSrc game on it

## Load it

```bash
bitbake xash3d-fwgs
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the engine core, one
renderer and the engine's own menu/filesystem modules - no game content:

```
/usr/bin/xash3d                          the launcher waf installs: dlopens libxash.so, calls Host_Main
/usr/lib/xash3d/libxash.so               the engine core
/usr/lib/xash3d/libref_gles3compat.so    the one renderer this seat can drive
/usr/lib/xash3d/libmenu.so               the mainui menu
/usr/lib/xash3d/filesystem_stdio.so      the WAD/PAK/searchpath layer
/usr/share/xash3d/valve/extras.pk3       the menu/HUD/font pack this build ships
```

## Play it

The engine the 1998 releases shipped is a Win32 binary, and the x86 Linux build FWGS
still publishes is an i386 library pair this board has no loader for, so the engine is
built here for aarch64 - and a GoldSrc game needs five things beside it. None of them is
in this package, and each is a different kind of thing:

| What | Where | Why it has to be there |
| --- | --- | --- |
| the game tree, read-only | `XASH3D_RODIR` / `-rodir` | the `.bsp`s, the `.pak`s, the `liblist.gam`. The engine never writes into it |
| a writable tree | `XASH3D_BASEDIR` - there is **no** `-writedir` flag | the engine writes `valve/save/`, its own `.cfg`s and its `.dem` recordings here; the rodir is read-only in this build, so the two must be separate |
| `valve/delta.lst` | in the **writable** tree | the engine fatals without it, and it is in none of the places a payload would be expected to carry it, so it goes there by hand (where its bytes come from is in PROOF.md) |
| the gamedll pair | `valve/dlls/hl_arm64.so`, `valve/cl_dlls/client_arm64.so` | GoldSrc is not a complete game by itself: the gameplay comes from the game DLL, and the seat needs an aarch64 one. `_arm64` is the suffix the `library-suffix` submodule's `COM_GenerateCommonLibraryName` spells, so the engine finds them with no config line. They come from Valve's Half-Life 1 SDK, whose agreement is not a free licence, so they are installed on the seat locally and shipped by no recipe |
| `extras.pk3` | `XASH3D_BASEDIR/valve/` | the menu/HUD/font pack; this recipe installs it at `/usr/share/xash3d/valve/` and the seat's copy is the one in its writable tree |

The four libraries sit in `/usr/lib/xash3d`, which is not on any default search path, and
`DEFAULT_RPATH` is `$ORIGIN` (`wscript:571`), so `libxash.so` is looked for next to
`/usr/bin/xash3d` first and is not there: `LD_LIBRARY_PATH=/usr/lib/xash3d` has to be in
the environment for the run to start at all. The separator is a colon - a space in that
variable breaks it.

The run script the seat actually uses, written out in full (this recipe installs no
launcher of its own, and the pair of trees is the owner's):

```sh
#!/bin/sh
export HOME=/home/weston
export XDG_RUNTIME_DIR=/home/weston
export WAYLAND_DISPLAY=wayland-1
export SDL_VIDEODRIVER=wayland
export SDL_AUDIODRIVER=alsa
export LD_LIBRARY_PATH=/usr/lib/xash3d:/usr/lib
export XASH3D_BASEDIR=/gamedata/games/<game>-writable
export XASH3D_RODIR=/gamedata/games/<game>
exec /usr/bin/xash3d -game valve -rodir /gamedata/games/<game> \
    -ref gles3compat -width 1024 -height 576 -dev 2 -noch "$@"
```

`-game valve` is the game directory inside both trees, and it is the right one whatever
the release calls itself: a payload's own `valve/liblist.gam` declares `game "Half-Life"`
and points at `d:\quiver\valve\dlls\hl.dll`, so the engine's game is `valve` and the
gamedll is the `hl` one. `-ref gles3compat` is the only renderer this seat can drive.
`-width 1024 -height 576` is the seat's fill-rate answer in the shape the engine spells.
`-dev 2 -noch` is the developer level and no checkpoint save, which is what lets a whole
map be walked without dying. Anything after it is engine command-line: `+map t0a0` is the
release's trainer level and `+map hldemo1` its first mission map.

## Controls, saves and what a new game needs

Control is proven by the payload's own config, not by a patch: the release's
`valve/default.cfg` ends with `sensitivity 3` and `+mlook`, so Xash3D is in mouselook from
the first load, and the release's binds are `,`/`a` `+moveleft`, `.`/`d` `+moveright`,
`PGUP` `+lookup`, `PGDN` `+lookdown`, `INS` `+klook`, `v`/`'` `+moveup`, `/` `+movedown`,
`UPARROW` `+forward`, `SHIFT` `+speed`, `CTRL` `+duck`, `r` `+reload`, `END` `centerview`,
`MWHEELUP`/`MWHEELDOWN` the inventory wheel, `F11` the release's own `zoom_in`/`zoom_out`
alias pair, `F5` `snapshot`.

Saves go where the engine puts them, and the engine spells the directory in **lower case**:
it created `valve/save/` inside the writable tree, not `valve/SAVE/` as the release's own
zip spells it, and that is where the release's own saves have to be copied to. A save name
is the stem of its `.HL1`/`.HL2`/`.HL3` triple, so renaming the stem is what keeps both the
release's saves and the seat's own loadable. Anything copied into a `SAVE` directory with
capital letters is read only if the filesystem is case-insensitive, and this one is not.

To put another GoldSrc game on the seat, the tree has to be GoldSrc-shaped - a `valve/`
(or `<gamemod>/`) directory with a `liblist.gam` that names the gamedll, `maps/*.bsp`,
`*.pak`/`*.pk3` - plus the `delta.lst` and the gamedll pair above.

**Half-Life-based games including Sven Co-op**: the engine speaks Half-Life-style GoldSrc
content, and Sven Co-op data has nothing on this seat to run it against as it stands -
`FWGS/xash3d-fwgs` carries no `SVEN CO-OP` game definition anywhere in its tree, so a Sven
Co-op tree needs its own gamedll, which is what the co-op project builds. The Half-Life
family is otherwise one engine: Up-link, a full Half-Life tree and a mod all open the same
way above, one `-game` away.

## What may be downloaded, and where from

The engine's own tree is GPL-3.0-or-later from upstream GitHub. **No GoldSrc game tree is
a recipe in this layer**, and the Up-link test on the seat was installed by hand. Unlike the
two Descents, this one fails on the **licence** half rather than the download half: everything
the release ships that could carry a grant was read, and nothing does (the full table of
carriers and what each says is in PROOF.md) - across all of them there are zero matches for
`distribut`, `freeware`, `shareware`, `license` or `non-commercial`, so a `LicenseRef`
built from that readme would be a copyright line, not a grant. No recipe.

One free-licence GoldSrc mod was checked and is a hard no on this seat, on its author's own
words: Diffusion, by Aynekko, says `Yes, it's 64-bit and OpenGL only. It needs OpenGL version
3.0`, and `glsl/enhance_fp.glsl` is a desktop-GL3 shader this EGL/GLES-only seat cannot open;
its payload is also unfetchable without a logged-in session (the ModDB mirror answers 403
`cf-mitigated: challenge`, and itch.io's locker needs a session `file_token`), and its page
carries no grant. It is documented here and installed nowhere.

## Measured

Pins, sizes, the maps a release carries, the console round, the save inventory and the
expected refusals are in [PROOF.md](PROOF.md).

## Files

```
xash3d-fwgs_git.bb   the engine, built with the tree's own waf
README.md            this file
PROOF.md             pins, sizes, measured rounds, failure modes
```

There are no local patches: the renderer choice, the packaging layout and the dependency
switches are all options the tree's own `wscript` has.

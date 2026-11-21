# ioquake3 - the Quake III engine, and how to put a Quake III tree on top of it

## Load it

```bash
bitbake ioquake3
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the engine and its own
gamecode - no content:

```
/usr/bin/ioquake3-gl2                  the client: one binary, the opengl2 renderer linked in
/usr/bin/ioquake3                      the launcher this recipe writes
/usr/share/ioquake3/baseq3/vm-pure.pk3 this build's version-6 vm/{cgame,qagame,ui}.qvm
```

## Play it

The launcher's defaults are already the seat's: `fs_basepath /usr/share/ioquake3`,
`fs_game baseq3`, `fs_homepath ~/.local/share/ioquake3`, the SDL wayland/alsa pair,
and the seat's video shape. So put your own Quake III data where the launcher names
and run the game:

```bash
# a full game: the owner's pak0.pk3 goes in the default tree
mv ~/pak0.pk3 /usr/share/ioquake3/baseq3/pak0.pk3
/usr/bin/ioquake3 +map q3dm7
```

```bash
# id's 1.11 demo: keep its own tree name, and the launcher's default is one +set away
mkdir -p /usr/share/ioquake3/demoq3
mv linuxq3ademo-1.11-6.x86.gz.sh /usr/share/ioquake3/demoq3   # unpack it yourself:
                                                              # a gzip inside a shell script; the payload's
                                                              # counts and hashes live in PROOF.md
/usr/bin/ioquake3 +map q3dm7 +set fs_game demoq3
```

The `+set fs_game demoq3` comes last on the engine's command line, so it wins over
the launcher's own default. `q3dm1`, `q3dm7`, `q3dm17` and `q3tourney2` are the arenas
that demo ships.

```bash
# Team Arena: same baseq3 tree, the owner's missionpack pak, and the tree's own name
mkdir -p /usr/share/ioquake3/missionpack
mv pak2.pk3 /usr/share/ioquake3/missionpack/pak0.pk3    # TA's pak0 is its own, not a rename
/usr/bin/ioquake3 +map q3tourney2 +set fs_game missionpack
```

Baseq3 is always loaded before `fs_game`, so the `vm-pure.pk3` this recipe installs
there is what the demo, a full game and Team Arena all load their gamecode from -
which is what upstream's own `code/web/client-config.json:33-:35` is asking for when
it says to copy `baseq3/vm/*.qvm` into a game's `vm/` directory.

**Wait 85 s before the first screenshot.** The opengl2 renderer opens two
`GLSL_InitGPUShaders` passes, and each prints `loaded 115 GLSL shaders (48 gen 42 light
25 etc) in 21.85 seconds` (21.37 s for the second), so the first minute of a run is the
shader compile and not the game.

Override with `IOQUAKE3_BASEPATH` / `IOQUAKE3_GAME` / `IOQUAKE3_HOME` / `IOQUAKE3_ARGS`,
or pass your own `+set` and `+commands` at the end, which win.

## Why the launcher sets what it sets

| Set | Reason |
| --- | --- |
| `fs_basepath` + `fs_game` + `fs_homepath` | ioquake3 never finds its game next to the executable: the data is under a read-only `/usr/share`, and the config, console log and screenshots have to go somewhere weston can write |
| `SDL_VIDEODRIVER=wayland` | this board carries no X server at all; SDL2 looks for X11 and dies before a window exists unless told otherwise |
| `SDL_AUDIODRIVER=alsa` | the ALSA sink behind the `pcm.!default` stanza the OS layer adds |
| `r_preferOpenGLES 1` | `code/sdl/sdl_glimp.c:1086` defaults it to -1, and at `:543` that default tries the desktop GL API first while this driver answers EGL with `OpenGL_ES` only |
| `r_fullscreen 1` + `r_mode -1` + `r_customwidth 1024` + `r_customheight 576` | the seat's fill-rate answer in the shape ioquake3 spells (`code/renderergl2/tr_init.c:1270` reads the custom pair, `r_mode -1` makes `GLimp_SetMode` use it): the desktop stays 1920x1080 and only the 3d scene is drawn into the smaller buffer the compositor scales up, and that buffer's area is what decides the framerate on this Vivante. Never ship `r_mode 0` - see PROOF.md |

The engine reads its game out of pk3 archives and picks its renderer at runtime; this
recipe compiles exactly one renderer (opengl2, linked in rather than dlopened), because
the seat's driver has no desktop GL and the opengl1 renderer has no ES path.

## What may be downloaded, and where from

The engine's own tree is GPL-2 and comes from upstream GitHub, and the `vm` modules
are that tree's own build product. The Quake III **data** is not fetched by any recipe
here. id's own agreements do hand the demo releases out free of charge - the 1.11 demo's
`Help/Q3A_EULA.txt` is id's "LIMITED USE SOFTWARE DEMO LICENSE AGREEMENT" - but today
the only carrier of those exact archives is a third-party mirror of id's dead
`ftp.idsoftware.com` corner, and nobody from id answers for that mirror, so the route
is a hand install of a copy you already own.

One engine-side fact does belong to a hand demo install, and it is already in this
build: vanilla ioquake3 cannot boot id's demo at all. `code/qcommon/files.c:3697`
inside `FS_CheckPak0` demands all nine paks, and with any pak mounted the standalone
branch at `:3652` is not taken (`com_basegame` answers `baseq3`, so a `+set com_standalone
1` on the command line is overwritten at `:3657`) while the `demoq3` rescue above only
ORs bit 0 into the found-pak mask behind a hard-coded `DEMO_PAK0_CHECKSUM` and the fatal
still wants all nine bits. The measured answer is that the process dies right after
mounting the pak: `1384 files in pk3 files`, then `Quake 3 data files are missing...`.
Patch `0002-let-the-demoq3-shareware-payload-skip-the-purchased-game-pak-mask-check.patch`
adds one condition to that demand so it is skipped exactly when `fs_game` names the
`demoq3` tree, and leaves a full game's check untouched.

## Measured

Pins, sizes, the black-screen and audio measurements, and the live playtest are in
[PROOF.md](PROOF.md).

## Files

```
ioquake3_git.bb   the engine, built with its own cmake and the two patches in files/
files/            the two local patches (build the qvm tools on the build machine;
                  let the demoq3 payload skip the nine-pak demand)
PROOF.md          pins, sizes, measured rounds, failure modes
README.md         this file
```

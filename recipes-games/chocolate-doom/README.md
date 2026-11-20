# chocolate-doom - the engine the DOOM, Heretic and Hexen games run on

## Load it

```bash
bitbake chocolate-doom
```

The `arm64` deb goes to the seat with `dpkg -i`. It installs the engine only:

```
/usr/bin/chocolate-doom   /usr/bin/chocolate-heretic  /usr/bin/chocolate-hexen
/usr/bin/chocolate-strife /usr/bin/chocolate-server
/usr/bin/chocolate-*-setup                            (terminal front ends)
```

## Play it

The engine needs a WAD, and the WAD's **file name** is what decides the game
(`src/d_iwad.c`'s table: `{"doom1.wad", doom, shareware}`, `{"heretic1.wad", heretic,
shareware}`, `{"hexen.wad", hexen, commercial}`, `{"freedoom2.wad", doom2, commercial,
"Freedoom: Phase 2"}`). So put your own copy where you like, under the name the engine
spells, and name it with `-iwad`:

```bash
export HOME=/home/weston XDG_RUNTIME_DIR=/home/weston WAYLAND_DISPLAY=wayland-1
export SDL_AUDIODRIVER=alsa
/usr/bin/chocolate-doom  -iwad /gamedata/games/doom/doom1.wad    --fullscreen \
    --video-driver wayland -warp 1 1 -skill 3
/usr/bin/chocolate-heretic -iwad /gamedata/games/heretic/heretic1.wad --fullscreen \
    --video-driver wayland -warp 1 1 -skill 3
/usr/bin/chocolate-hexen -iwad /gamedata/games/hexen/hexen.wad --fullscreen \
    --video-driver wayland -warp 1 1 -skill 3 -v10override
/usr/bin/chocolate-doom  -iwad /gamedata/games/freedoom/freedoom2.wad --fullscreen \
    --video-driver wayland -file 1 -skill 3
```

`-warp <episode> <level>` opens the map directly instead of the title card, `-skill 3`
is "Hurt+", `-file 1` is Freedoom's second phase. Hexen needs `-v10override`: its demo
`.CON` scripts are v1.0-era and the engine otherwise refuses them.

## Freedoom, the one DOOM-family game whose own source this layer trusts

Freedoom is the free DOOM II-compatible game, released by its own project under its own
agreement, so it can be downloaded and played without a rights question. Its own release
page carries the zip; the WAD inside it is what the engine asks for by name:

```bash
cd /gamedata/games/freedoom
wget https://github.com/freedoom/freedoom/releases/download/v0.13.0/freedoom-0.13.0.zip
unzip -j freedoom-0.13.0.zip 'freedoom2.wad'
/usr/bin/chocolate-doom -iwad /gamedata/games/freedoom/freedoom2.wad --fullscreen \
    --video-driver wayland -file 1 -skill 3
```

The id shareware and demo WADs (`doom1.wad`, `heretic1.wad`, `hexen.wad`) carry the
same kind of grant - id's own agreements and release notes do hand out the shareware
episodes and the demo releases - but no recipe in this layer fetches them: the only
carrier on hand is id's `idstuff` corner as mirrored by a third party (`gwdg.de`), and
nobody from id answers for that mirror, so it is not a source this layer takes. Bring
your own copy (an original CD, a store copy you own) and use the command above.

## Why the switches are these

- `--video-driver wayland` is the engine's own way of naming SDL's video driver
  (`src/m_config.c:708` binds it, `src/i_video.c:1130` exports `SDL_VIDEODRIVER`
  before the `SDL_Init(SDL_INIT_VIDEO)`). The seat carries no X11 at all.
- `SDL_AUDIODRIVER=alsa` has to stay in the environment: the engine has no audio-driver
  setting, and `src/i_sdlsound.c` opens the sink through SDL2's mixer - which is why
  `libsdl2-mixer` is a dependency. Music needs nothing extra: the OPL emulator in
  `src/i_oplmusic.c` renders the `.mus`/`.mid` the WADs carry.
- `-fullscreen` costs little: the game draws a 320x200 8-bit paletted `SDL_Surface`
  and SDL2's renderer does the upscale.
- Mouselook is SDL2's relative mode, and config plus saves go to
  `~/.local/share/chocolate-doom`, which weston can write.

## Measured

The proof rounds and their numbers are in [PROOF.md](PROOF.md).

## Files

```
chocolate-doom_git.bb   the engine, built from upstream with no patches
PROOF.md                the pins and the measured rounds
README.md               this file
```

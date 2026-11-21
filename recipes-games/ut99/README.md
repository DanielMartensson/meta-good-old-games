# ut99 - Unreal Tournament 99 GOTY: how the seat loads it and opens it

## What this package installs

    /gamedata/ut            the GOTY CD1 data (Maps/, Textures/, Sounds/, Music/)
                            under OldUnreal's 469f arm64 engine in System/
    /gamedata/ut/System/ut-bin   the engine: an aarch64 PIE, the same binary the
                                 ut99 and unreal-gold recipes both ship
    /usr/bin/ut99           the launcher

The tree is 686 MB, so it does not go under `/usr`: the seat's 4.2 GB root
filesystem cannot hold it, and `/gamedata` is its 8 GB data partition.

## How to load it on the seat

The deb is built on the build host and relayed host → local → seat. Because the
game lives on the `/gamedata` mount rather than in `/usr`, it is unpacked there
rather than installed with `dpkg -i`:

    dpkg-deb -x ut99_469f-r0.01.deb /
    chown -R weston:weston /gamedata/ut

The `chown` is what makes the game playable: the package puts the tree up as
`root:root`, and the engine writes logs, `Cache/` and its save games into it
from `~/.utpg` side, so weston rather than root has to own it.

## How to run it

    ut99                        # DM-Deck16][ deathmatch against bots
    ut99 'DM-Deck16][.unr?game=BotPack.DeathMatchPlus?difficulty=5'
    ut99 '<a map URL from Maps/>'   # any of the tree's own maps

    UT99_DIR=/gamedata/ut       # the tree
    UT99_MAP=<map URL>          # default DM-Deck16][.unr?game=BotPack.DeathMatchPlus?difficulty=3
    UT99_ARGS=...               # extra engine arguments
    UT99_SYSTEM_SDL3=1          # put the seat's SDL 3.5.0 over the bundled 3.4.10

The engine is started from inside its own tree, so the map URL may also be the
first argument. `-log` is always passed: the run's `UnrealTournament.log` and
its crash dumps land in `~/.utpg/System` next to the save games.

The renderer is `SoftDrv`, named in the shipped `System/UnrealTournament.ini` at
1920x1080, the panel's own size, so weston has nothing to magnify. The window is
made fullscreen with weston's own `Ctrl+Shift+F`, because the engine ignores
`-fullscreen` and asks the compositor for a plain window, and because a viewport
size change after the first frame crashes it in `USDLViewport::Repaint`.

One instance at a time: the launcher holds `flock /tmp/ut99.lock` and refuses a
second one, and a second `ut-bin` started without the script is caught by the
`pgrep` guard. That is not cosmetic - with two UE1 processes on this seat the
player's own weapon and footstep sounds went silent.

## Licence

Epic's Terms of Service, pinned as
`LicenseRef-Epic-Games-Unreal-Tournament-ToS` out of the engine tarball's own
`LICENSE.md` (`The Epic Games Terms of Service apply to the use and
distribution of this game`). The game is not free to download and this build is
the seat's owner's own copy, not redistributed.

## Measured

The pins, the sizes and checksums of both halves, the measured round and the
failure modes are in [PROOF.md](PROOF.md); what is still broken or unmeasured is
in [TODO.md](TODO.md).

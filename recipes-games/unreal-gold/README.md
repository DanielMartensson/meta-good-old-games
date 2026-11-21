# unreal-gold - Unreal / Unreal Gold: how the seat loads it and opens it

## What this package installs

    /gamedata/ugt             Gold's 1998 campaign (Maps/, Textures/, Sounds/,
                              Music/) under OldUnreal's 469f arm64 engine in
                              System/ - 882 MB
    /gamedata/ugt/System/ut-bin   the engine: the same aarch64 PIE the ut99 recipe ships
    /usr/bin/unreal-gold      the launcher

It is a hybrid on purpose: Gold's own engine cannot draw here at all, so the
content is Gold's and the binary is OldUnreal's 469f Unreal Tournament build,
the only UE1 binary on this seat that ships a renderer which draws. That
renderer is `SoftDrv`, and the front-end being UT99's menu rather than Gold's
own is a settled consequence of the same fact, not a bug.

## How to load it on the seat

Same relay as the other packages (build host → local → seat), and unpacked onto
the `/gamedata` mount rather than installed with `dpkg -i`, because the tree is
882 MB and the seat's 4.2 GB root filesystem has 69 MB free:

    dpkg-deb -x unreal-gold_1.0-r0.01.deb /
    chown -R weston:weston /gamedata/ugt

The `chown` is what makes it playable: the engine writes logs, `Cache/` and its
save games into the tree, so weston rather than root has to own it.

## How to run it

    unreal-gold                        # the game's own intro map, Unreal.unr
    UG_MAP=Vortex2.unr unreal-gold     # the first playable map, not the flyby
    unreal-gold Vortex2.unr            # the same, as the first argument

    UG_DIR=/gamedata/ugt      # the hybrid tree
    UG_MAP=<map URL>          # default Unreal.unr, the intro flyby
    UG_RES=800x600            # internal render size, the measured default
    UG_ARGS=...               # extra engine arguments

The retake round opened on `UG_MAP=Vortex2.unr`, because the gallery wants the
frame the game opens its play on and `Unreal.unr` is an intro flyby the player
cannot move in.

`UG_RES` is what the software renderer draws at, and weston scales that window
onto the 1920x1080 panel: 800x600 was measured sharper on this panel than
1280x720 and 960x540. The launcher rewrites the size in `System/UnrealTournament.ini`
before the engine starts, never at runtime - a viewport change after the first
frame crashes it in `USDLViewport::Repaint`. As with `ut99`, weston's own
`Ctrl+Shift+F` is what puts the window on the whole panel.

One instance at a time: `flock /tmp/unreal-gold.lock` plus a `pgrep -f
System/ut-bin` guard. With two UE1 processes on this seat the player's own
weapon and footstep sounds went silent.

## Licence

Epic's Terms of Service, pinned as
`LicenseRef-Epic-Games-Unreal-ToS` out of the 469f tarball's `LICENSE.md`.
Gold's data is Epic's, the CD is the seat owner's own, and nothing here is
under a free licence or redistributed.

## Measured

The pins, the sizes and checksums of the four fetched halves, the measured rounds
and the renderer failures are in [PROOF.md](PROOF.md), whose `TODO` section at
the bottom is what is still open here.

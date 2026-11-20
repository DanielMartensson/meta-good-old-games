# chocolate-doom - pins and measurements

## Pin

```
git://github.com/chocolate-doom/chocolate-doom.git  nobranch=1
SRCREV 410d96855b5df5410ff591a90efeafa889119224   (the 3.1.1 release tag)
LICENSE GPL-2.0-or-later, COPYING.md md5 60d644347832d2dd9534761f6919e2a6
PV 3.1.1, PKGR r0.01
```

The pin is the tag's commit, not the repository tip: `nobranch=1` asks only that the
commit exists, and the tip at the same moment was a CMake merge while this recipe
builds the autotools tree.

Installed binaries: five programs plus four `chocolate-*-setup` front ends. The
`share/metainfo` appstreams and the hicolor icons are removed in `do_install`:
`src/Makefile.am:240-:241` installs the four appstream XMLs unconditionally and
they are the entries `do_package` then reports as installed-but-not-shipped.

## The engine's contract

`src/d_iwad.c` names the WADs, and the file name decides the game and its edition:

```
:40  {"doom1.wad",    doom,   shareware,  "Doom Shareware"}
:44  {"freedoom2.wad",doom2,  commercial, "Freedoom: Phase 2"}
:48  {"heretic1.wad", heretic,shareware,  "Heretic Shareware"}
:49  {"hexen.wad",    hexen,  commercial, "Hexen"}
```

`-iwad` only states where the file is (`:902 M_CheckParmWithArgs("-iwad", 1)`), and
there is no `-basedir` concept here, so the first-match problem the FTEQW launchers
document does not exist for this engine.

## Freedoom, the payload this layer could fetch

The freedoom project's own release page hands out its own game, so this is the one
DOOM-family payload whose source is trusted. `freedoom-0.13.0.zip`, 24,143,781 B,
sha256 `3f9b264f3e3ce503b4fb7f6bdcb1f419d93c7b546f4df3e874dd878db9688f59`; inside it,
`freedoom2.wad` 28,787,748 B, 3,610 lumps, md5 `cd666466759b5e5f63af93c5f0ffd0a1`.
An earlier round packaged it as `doom2-demo_0.13.0-r0.01_arm64.deb`, 7,849,068 B
sha256 `cf3797a4aeda8f34a9928e6fff7b19c79b7294ddcf633dd0681cbeebdf6585af`, installing
`/usr/share/doom2-demo/freedoom2.wad` plus a 1,400 B launcher. It is now a hand
install like every other payload of this family, which is why the name `doom2-demo`
is gone from the layer: the game is Freedoom, and id never released a Doom II
shareware licence.

## Measured on the seat

All rounds are at the seat's 1920x1080, screenshots through
`XDG_PICTURES_DIR=<dir> weston-screenshooter`, numbers from `tools/shotstat.py`
and `tools/imgdiff.py` on the build host (the seat has no PIL). The threshold in
the diff columns is a max-channel difference of 8.

### Freedoom Phase 2, round of 2026-11-15

`-warp 1 1 -skill 3`, four shots. The drawn pixels of all four span `x 240..1679`
at full height, so the game runs a 4:3 1,440x1080 surface and the panel carries
240 px of black bar on each side, 518,400 of the black pixels.

| Shot | Distinct colours | Non-black of 2,073,600 | Row luminance | Mid-row jumps | md5 |
| --- | --- | --- | --- | --- | --- |
| 1 idle | 3,459 | 1,506,198 | 9.4-84.8 | 38 | `8b155ee82d0d6a2a0630c6743420566f` |
| 2 idle, 6 s later | 3,443 | 1,506,166 | 9.4-84.8 | 38 | `6ba3c54263846b01cc13996f4d05f036` |
| 3 after a wide mouse sweep | 1,670 | 1,555,030 | 17.9-118.6 | 30 | `f29fb78bd937e739bcdf19e638fc8a7f` |
| 4 after an Enter press | 1,674 | 1,555,030 | 17.9-118.6 | 30 | `d918cd1b1a4dece788aa45b8133b0f9f` |

| Pair | Identical | >8 | columns/rows whose shape moved |
| --- | --- | --- | --- |
| idle 1 vs 2 | 2,071,295 (99.9 %) | 2,251 px (0.1 %) | 0 / 1,920 and 0 / 1,080 |
| sweep 2 vs 3 | 822,314 (39.7 %) | 1,228,450 (59.2 %) | 1,415 / 1,920 and 907 / 1,080 |
| after Enter 3 vs 4 | 2,071,892 (99.9 %) | 1,663 (0.1 %) | 0 / 1,920 and 0 / 1,080 |

Process at `RSS 67,352 kB / 60.4 % of one core` for 50 s. The engine printed `adding
/usr/share/doom2-demo/freedoom2.wad` and the banner `Freedoom: Phase 2`. Console
lines: `OPL_Init: Using driver 'SDL'.`, `S_Init: Setting up sound.`,
`I_SDL_PrecacheSounds: Precaching all sound effects`, and grepping the 25 lines for
`error|couldn|failed|busy` matched nothing. `/proc/<pid>/maps` of the running game
mapped `libSDL2_mixer-2.0.so.0.2.2`, `libSDL2-2.0.so.0.3000.1`,
`libasound.so.2.0.0` and `alsa-lib/libasound_module_rate_speexrate.so`, so both the
effects and the OPL-rendered music reach the seat's 48 kHz ALSA sink.

### id's DOOM shareware 1.9, round of 2026-11-15

`-warp 1 1 -skill 3`, four shots. The engine answered `adding .../doom1.wad` and the
banner `DOOM Shareware`, at `RSS 36,708 kB / 60.0 % of one core`.

| Shot | Distinct colours | Non-black | Row luminance | Mid-row jumps | md5 |
| --- | --- | --- | --- | --- | --- |
| 1 idle | 4,811 | 1,548,071 | 14.7-90.0 | 48 | `7b443c3fd3895ba44e1eba950a1b9f49` |
| 2 idle, 7 s later | 4,809 | 1,548,095 | 14.7-90.0 | 48 | `86fee277001ea9d2269d6344d57aba97` |
| 3 after the sweep | 1,898 | 1,550,400 | 34.0-117.8 | 40 | `29def8ecba6fcbc24669961db2981363` |
| 4 after the sweep | 1,905 | 1,550,279 | 34.0-117.8 | 40 | `168a1765954e284c8aeffea6277b3d62` |

| Pair | Identical | >8 | columns/rows moved |
| --- | --- | --- | --- |
| idle 1 vs 2 | 2,072,533 (99.9 %) | 1,028 px (0.0 %) | 0 and 0 |
| sweep 2 vs 3 | 804,493 (38.8 %) | 1,262,052 (60.9 %) | 1,418 / 1,920 and 907 / 1,080 |
| 3 vs 4 | 2,071,558 (99.9 %) | 1,832 (0.1 %) | 0 and 0 |

The older round of 2026-11-13, through the pre-rename `doom-shareware` launcher,
measured 4,057-4,482 colours, 1,544,527-1,550,400 non-black, idle pair 100.0 %
identical, sweep pairs 50.0-56.0 % moved, 1,228/1,920 columns, 36,808 kB RSS at
58.4 % CPU - the same cost under the new launcher name, which says the rename
changed the contract and not the engine.

### id's Heretic shareware episode, rounds of 2026-11-13 and 15:30 on 11-15

2026-11-13: 2,817-3,558 colours, 1,371,547-1,555,200 non-black, idle pair 99.2 %
identical with 0.7 % alive (the title's animated flame), sweep pairs 56.5-75.0 %
moved, 950-1,414 columns, 46,260 kB RSS at 63.5 % CPU.

2026-11-15, same command: 3,385-3,702 colours, 1,205,296-1,501,343 non-black
(the first pair is the darker title view, the second the lit level view), idle pair
99.8 % identical with only 3,716 px past the threshold and **0 columns and 0 rows**
of shape moving, sweep pair 48.9 % identical, 1,059,496 px (51.1 %) moved, 421
columns.

### id/Raven's Hexen demo, round of 2026-11-13

`-warp 1 1 -skill 3`, 4 shots: 3,377-4,447 colours, 1,552,132-1,555,200 of
2,073,600 non-black, idle pair **already 75 % different** - the demo's animated sky
moves while the game waits, so a still-frame pair is not a valid control proof here -
sweep pairs 53.0-56.0 % moved with up to 1,141 of 1,920 columns, 87,368 kB RSS at
75.6 % CPU. The colour counts are low because the game is a paletted 320x200 buffer
upscaled: the palette is 256 entries, and the compositor's scaling is what makes
1.5 M non-black pixels exist at all.

## Failure modes

| Symptom | Cause |
| --- | --- |
| `ALSA: Couldn't open audio device: Device or resource busy` | not this engine: the seat's ALSA sink serves one process at a time, so a proof round kills every other game first (an UT99 SoftDrv client of the round before was still alive in the ioquake3 round of 2026-11-15) |
| a Hexen run that refuses its `.CON` scripts | the demo's scripts are v1.0-era; `-v10override` is the switch that accepts them |
| `unknown option '-fullscreen'` | not this engine: that is OpenJazz's bundled argparse, which wants two dashes |

## The debs this branch builds (built 2026-11-20 from this tree)

The whole layer was built again from this restructured tree at its own path: `bitbake chocolate-doom fteqw ioquake3 eduke32 dosbox-x openjazz xash3d-fwgs librequake openarena opennowlite` ran green on the build host's `stm32mp25-watermelon-wine-1a` machine, and this recipe's own task chain re-ran inside that run, so the numbers below are this tree's and not the ones an earlier round happened to record. `chocolate-doom` leaves the build as `3.1.1-r0.01`:

| deb | size | md5 |
| --- | --- | --- |
| `chocolate-doom_3.1.1-r0.01_arm64.deb` | 671,792 B | `dc3a9ed3375c7b280c84fa4c5e1fe8b1` |
| `chocolate-doom-dbg_3.1.1-r0.01_arm64.deb` | 1,967,476 B | `346d01a4eb6d7908bd64db60ece43d0c` |
| `chocolate-doom-dev_3.1.1-r0.01_arm64.deb` | 2,716 B | `ed0ba0aa0434364a98e5d0f6dce110d3` |
| `chocolate-doom-doc_3.1.1-r0.01_arm64.deb` | 42,532 B | `47cca1588b126f745bba05b69f242b07` |
| `chocolate-doom-src_3.1.1-r0.01_arm64.deb` | 938,732 B | `6620c224fc00dcf2cd4babac76399656` |

The base deb installs 15 files, the five binaries the seat runs plus their `-setup` front ends and the `.desktop`/`.png` entries. `/usr/bin/chocolate-doom` is 810,744 B md5 `ff06f26ccc831ad40ba070a1f23dad0a`, `chocolate-heretic` 820,880 B md5 `d14e6a2b509a151e6657bed1c62b31be` and `chocolate-hexen` 1,047,432 B md5 `51800ad325b4909ca1c47b80cd048f5a`. The seat's dpkg pin is the same version, so installing this deb replaces what the seat carries in place (`dpkg -i` answers over it), and the binaries are the same size as the seat's while their md5s move because the compile lines carry this build's own paths.

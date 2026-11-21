# unreal-gold - pins, the measured round, and the open items

Detail file for `unreal-gold_1.0.bb`, beside the minimal `README.md`. Every
number here was measured on the seat with the package this recipe builds; the
load/use half lives in `README.md`, the still-open half is the `TODO` section at
the bottom.

Unreal / Unreal Gold (Epic's 1998 first-person game, the Gold CD of August
2000) as the seat actually plays it, which is a hybrid: the content is Gold's
and the engine is OldUnreal's 469f arm64 Unreal Tournament binary. The tree
sits at `/gamedata/ugt` and `unreal-gold` starts it.

**This build is for my own use, no redistribution.** The game is Epic Games';
the terms are the Epic Games Terms of Service that OldUnreal's `LICENSE.md`
opens with (`The Epic Games Terms of Service apply to the use and distribution
of this game`), and nothing in this recipe or on this seat is a free licence.
The recipe builds a local package for the seat's owner's own hardware, and the
result is not published, sold or handed on.

## Why it is a hybrid

Unreal Gold's own engine does not draw on this seat, and that was measured
before any of this was written down:

- The 227k arm64 Unreal patch ships `OpenGLDrv.so` and `XOpenGLDrv.so` and
  nothing else. Started stock, its log walks through
  `TryRenderDevice ini:Engine.Engine.GameRenderDevice`, OpenGLDrv dying on the
  undefined symbol `NvOptimusEnablement`, SDL answering
  `SDLDrv.Errors.Failed3D` and the engine stopping at
  `Assertion failed: RenDev [SDLViewport.cpp] [Line: 1068]` with a SIGSEGV;
  XOpenGLDrv answers `XOpenGL: No Window found!`. The stock install reached
  `LoadMap: Entry` and possessed `UPakFemaleOne`, so the data and the scripts
  were fine - only the renderer was dead.
- The 227k Linux engine reads `System/UnrealLinux.ini`, not the `System/Unreal.ini`
  the Windows one uses; the renderer matrix was re-run against the right file
  before any conclusion was drawn.
- The seat's filesystem carries no `libGL.so.1` at all, in any arch directory:
  only `libEGL.so`, `libGLESv2.so`, `libGLESv1_CM.so` and Vivante's `libGLSLC.so`.
  So `OpenGLDrv` has nothing to dlopen, and `XOpenGLDrv` reaches a GLES 3.1
  context (`Vivante GCNANOULTRA31_VIP2`, GLSL ES 3.10) whose compiler rejects its
  shaders: `Unrecognizable interface block qualifier` for its `out`/`in` blocks
  (in the OES and in the EXT spelling, which was tested by patching the driver's
  own GLSL string) and a 2.8 kB-per-stage `DrawTile` uniform block over GL ES 3.1's
  budget.
- 469f's `SoftDrv.so` cannot be carried over to the 227k engine: it needs `Render.so`,
  `Engine.so`, `Core.so` and `libSDL3.so.0` by soname, and the symbols it asks those
  for (`appPowdd`, `appSqrtd`, `FMemStack::GetThreadMem()`, `GMemThreaded`,
  `UObject::QueryInterface`, `UObject::AddRef`, `UObject::Release`, 469's
  `appFailAssert(char const *, int)` overload) are not in 227k's, and `GMemThreaded`
  plus the `UnGuardBlockTLS` guard are TLS/data relocations that must resolve at
  `dlopen`. 39 missing symbols in total, so the module dies before it can draw.
- SoftDrv being the renderer is an accepted choice, not an open compromise: the
  owner said on 14 Nov that a game which only works through SoftDrv is fine.

So the engine is the 469 line, the only arm64 UE1 binary on this seat that ships
a renderer which draws. Its `System/` is what the seat runs - `ut-bin`, `SoftDrv.so`,
`ALAudio.so`, SDL 3, OpenAL Soft, and the `.u` packages - and the game is Gold's
`Maps/`, `Textures/`, `Sounds/` and `Music/`. The Unreal 1 classes 469 needs come
from the `UnrealShare.u` and `UnrealI.u` that ship inside 469: 469's linker
SIGSEGVs on Gold's own 2000-era packages, so those two are UT99's and Gold's `UPak.u`
is not used at all (UT99 has no such package).

## What the recipe installs

| Half | Source | Size |
| --- | --- | --- |
| UT99 tree | `https://files.oldunreal.net/UT_GOTY_CD1.ISO`, sha256 `e184984c…74704f` | 649,633,792 B ISO; `System/` and the UT content under it |
| Engine | OldUnreal `v469f-rc6`, asset `OldUnreal-UTPatch469f-Linux-arm64.tar.bz2`, sha256 `56c09d3f…52ef46` | 75,147,582 B |
| Gold content | `https://files.oldunreal.net/UNREAL_GOLD.ISO`, sha256 `7e360d0c…42cbca` | 676,734,976 B ISO; its `MAPS/`, `MUSIC/`, `SOUNDS/`, `TEXTURES/` |
| Gold additions | OldUnreal `v227k_15`, asset `OldUnreal-UnrealPatch227k-Linux.tar.bz2`, sha256 `b04eba6d…3f7a1` | 131,414,620 B; only its `Maps/`, `Music/`, `Sounds/`, `Textures/` |
| Configuration | `files/UnrealTournament.ini` | SoftDrv, 800x600 internal |
| Launcher | `files/unreal-gold` | one instance, wayland + alsa, 800x600 |

The engine is the one binary both UE1 recipes ship: `/gamedata/ugt/System/ut-bin` at 214,424 B, an
aarch64 PIE, identical to the ut99 recipe's `/gamedata/ut/System/ut-bin`.

`do_extract_gamedata` puts the four together in a written order rather than in
bitbake's: the UT99 disc's `System/`, `Maps/`, `Music/`, `Sounds/`, `Textures/`
and `Help/ReadMe.htm` first (`bsdtar` from `libarchive-native`, whose ISO9660
reader keeps Rock Ridge names, and the Windows installer's `*.dll`, `*.exe`,
`*.bat`, `*.ico`, `*.bmp`, `*.cab` and `*.inf` inside those dirs are skipped),
then the 469 release over that with `tarfile` so its binaries and packages win
over the disc's 436-era ones, then the Gold CD's four data dirs over that, then
the 227k patch's data additions last - its `System/` is deliberately not taken,
since that is the 227 engine and the class layout 469 refuses.

Two details of the merge are load-bearing. The Gold CD's own top-level dirs are
uppercase on the disc (`MAPS/`, `MUSIC/`, `SOUNDS/`, `TEXTURES/`) while the ini
and the engine use `Maps/`, `Music/`, `Sounds/` and `Textures/`, so the merge
renames them. And where both CDs carry one package name the Gold CD's copy
replaces the UT99 one rather than sitting next to it as a second spelling -
matched case-insensitively, because the tree is later installed onto a partition
that may not be case-sensitive. This is how the tree the seat was proven with was
built: its `Textures/Castle1.utx` is the CD's 4,533,635-byte package, not the
177,545-byte UT99 one.

The last step of the task is the byte patch the campaign needs. Gold's travel and
restart URLs are Windows paths, so the eight bytes two-dots/backslash/`maps`/backslash
are rewritten in place to two-dots/slash/`Maps`/slash, which is the same length, so
no offset inside any package moves. Measured on the tree: one occurrence each in
`System/UMenu.u` and `System/UnrealShare.u`, and none in the `.unr` maps, which keep
their travel targets as plain relative names. With the patch, `Unreal.unr` travels to
`Vortex2.unr` on Linux; without it the campaign stops at the first travel.

## Installing on the seat

The tree is 882 MB, so it lives on `/gamedata` and not under `/usr`: the seat's
4.2 GB root filesystem has 69 MB free. Install it the way `ut99` is installed,
by extracting the deb onto `/` with `dpkg-deb -x` - the package's own paths already
start with `/gamedata/ugt` - and not with `dpkg -i`, which would first copy a
~900 MB deb onto the root filesystem. Only one big payload at a time fits: with
`ut99`'s tree and this one installed, `/gamedata` holds 2 of its 7.8 GB, and
`/gamedata/ug`, the stock 227k tree the seat tried Unreal Gold with first, is
815 MB more that nothing needs any more.

## How it runs

`unreal-gold` starts the game at Gold's own intro map (`Unreal.unr`); the first
screen is the flyby with `PRESS ESC TO BEGIN`.

```
unreal-gold                     # Gold's intro, 800x600 internal
UG_MAP=Vortex2.unr unreal-gold  # a named Gold map
unreal-gold Vortex2.unr         # same, as an argument
UG_RES=1920x1080 unreal-gold    # internal size, drawn by SoftDrv
```

The launcher holds one instance (flock on `/tmp/unreal-gold.lock` plus a
`pgrep -x ut-bin` guard): two UE1 processes on this seat played music and other
players' sounds while the player's own weapon and footsteps went silent. It also
writes the internal size into `[SDLDrv.SDLClient]` before the engine starts,
because a viewport change after the first frame crashes the engine in
`USDLViewport::Repaint`.

To open a Gold map from inside a running game, use the console: press `~` and
type `open Vortex2.unr`. That is the reliable route - see the note below about
what UT99's menu can and cannot start.

## What was measured on the seat

The hybrid draws, with audio and mouselook, at SoftDrv, at 800x600 internal,
inside a 1920x1080 window, and the log says so in the same run:

```
Bound to SoftDrv.so
SDLDrv: Set physical window size to 1920 x 1080 px - Internal resolution is 800 x 600 px
LoadMap: Unreal.unr?Name=Player?Class=Botpack.TMale2?team=255?...
Browse: Vortex2.unr?...
LoadMap: Vortex2.unr?Name=Player?Class=Botpack.TMale2?team=255?...
```

Screenshots and their measurements (`tools/shotstat.py` over the pulled shots):

| Shot | What it shows | Measurement |
| --- | --- | --- |
| 10:07 `vortex2-live.png` | `Vortex2.unr` from the console, HUD `+12`, `Translator` message, the level's own geometry | 39,105 distinct colours, 1,466,285 of 2,073,600 pixels non-black, 14 brightness jumps across the middle row, md5 `8e47ba10…d5f3d9` |
| 10:03 `live1.png` | Gold's intro flyby with `PRESS ESC TO BEGIN`, the Epic / Digital Extremes / GT logos | 46,473 distinct colours, 1,728,818 of 2,073,600 non-black |
| 09:56:30 / 09:56:41 | `Unreal.unr` travelling into `Vortex2.unr` | 25,680 and 20,889 distinct colours, 2,028,277 and 2,050,529 of 2,073,600 non-black |
| 12:19:14-12:19:41, 2026-11-15 re-verify | the launcher default: `Bluff.unr?game=DeathMatchUnrealI`, botmatch, four shots | 2,345-21,453 distinct colours, 2,041,258-2,073,600 non-black |

The pairs of that 2026-11-15 round: the idle pair answers 99.2 % of pixels
differing by more than 1 - expected, because a botmatch on Bluff has the bots
fighting and their flashes lighting the arena; one shot catches a white muzzle
flash of 11,557 pixels at `(254,254,255)` on top. Pair 1 vs 3 returns 60.6 %
identical with only 4/1920 columns of mean luminance moved, the same view with
the fight still alive in it, and the sweep pairs answer 98.8-99.2 % of pixels
differing. The console named the class (`LoadMap: Bluff.unr?...?game=DeathMatchUnrealI`)
and repeated the audio line as this round saw it: `ALAudio: We are using OpenAL
device: OpenAL Soft (ALSA Default)`, `ALAudio: Version 1.1 ALSOFT 1.24.3 found`.

The proof run also walked the console (`~`), typed `open Vortex2.unr`, moved the
mouse (`ydotool`) and left the audio at the seat's 48 kHz ALSA pin; the owner
confirmed music and controls by eye on the seat.

Resolutions, measured rather than chosen by rule: 800x600 internal is what the
owner prefers and is what the shipped ini sets; 1920x1080 internal draws with no
weston scaling, 1280x720 and 960x540 draw as well but looked worse on this panel,
and no size may be asked for after the first frame.

## The front-end, which is why ESC shows UT99

Pressing ESC shows UT99's Game menu with UT99's title plate, and that is
expected rather than a bug: the engine is 469, and the menu is a package of the
engine's. Gold's own `UMenu.u` was tried in the hybrid tree rather than assumed
dead: with the 227k package in place of UT99's, the 469 engine does not get a
menu at all, it dies on the class set the 227 packages want, 30 lines of load
errors that name `UWindow.UWindowComboBoxControl`, `UWindow.UWindowSmallRestart
Button`, `Engine.ObjectShadow`, `UnrealShare.UnrealUserManager` and
`Engine.GameInfo.SavedGameInfo` among others. Every one of those classes exists
in the 227 class layout and not in 469's, so the front-end would need the whole
227 package set, which is the linker failure already recorded above. The seat
therefore runs UT99's menu - whose own `UMenu.u` carries one Windows travel
path, which is what the byte patch fixes.

What the UT99 menu can and cannot start, as measured:

- Its `New` / botmatch dialog does start Gold maps: the run that clicked through
  it logged `Browse: DKNightOp.unr?Game=UnrealI.DarkMatch`, `Game class is
  'DarkMatch'`, `Login: Player`, and the map loaded. The preview pane in that
  dialog is black because `DKNightOp` has no `LevelSummary` and no
  `Screenshot` texture on the CD (`Failed to load 'Texture DKNightOp.Screenshot'`
  and the `LevelSummary` line next to it), which is what makes a working start
  look like a failure.
- The dialog's map list is UT99's list of game types, so a Gold single-player
  map started through it runs as a deathmatch with the UT classes rather than
  as the campaign. The campaign route is the console (`open <map>`) or
  `UG_MAP`, which take the map's own game information.
- `Load Game` lists nine empty slots plus a `Restart <map>` line: the hybrid
  tree carries Gold's save games in `Save/`, so what is offered is Gold's.
- The benign line `Load error: Class UnrealI.ASMDAmmo was replaced with
  UnrealShare.ASMDAmmo` is UT's package set answering for Gold's, and the level
  loads with it.

## TODO

- [ ] **No fps number yet.** `stat fps` did not appear in the captured shots, so
      the SoftDrv cost is unquantified. Put `stat fps` on the console before the
      shot, or start with `-log` and read the run's `stat` output, and record
      800x600, 1280x720 and 1920x1080 against RSS and CPU, so the shipped
      default can be argued from numbers as well as from how the panel looked.
- [ ] **Single-player from the menu is UT99's shape, not Gold's.** The menu is
      469's, so `New` gives a botmatch, not the campaign with a difficulty
      choice. A `+igame UnrealI.UnrealGameInfo`-style start or a small ini/User
      Settings preset that makes the campaign route reachable from the dialog
      without the console would take the console out of the owner's way. Not
      attempted yet, and the console works today.
- [ ] **Save games and the preference directory.** The hybrid shares `~/.utpg`
      with `ut99`: logs, crash files, `Cache/` and save games of the two games
      sit in the same directory, and `~/.utpg/System/UnrealTournament.ini`
      shadows the tree's own when the engine writes it. A per-game `HOME` or a
      copied preference tree in the launcher would separate them, and until then
      a second `ut-bin` process is refused by the launcher on purpose.
- [ ] **`/gamedata` is not yet part of the image.** There is no fstab entry or
      prepared image in `layers/meta-watermelon-wine` (OS layer) for the
      partition, so the mount today is whatever the seat happens to have. Until
      that regime exists, nothing here is reproducible across a reflash, and the
      partition's size needs a note next to `SDCARD_SIZE`.
- [ ] **`/gamedata/ug`, the stock 227k tree, is dead weight.** It is 815 MB, it
      was only ever the renderer test, and removing it is what makes a third
      payload possible on the seat at all. Kept for now because it is the tree
      the failure notes were written from.
- [ ] **Registered Unreal content stays out.** Nothing here beyond the Gold CD
      (the 1998 campaign plus the 227 patch's additions) is in the payload; the
      community map packs OldUnreal offers are free content and could be a
      separate payload on top of this tree.
- [ ] **UT2004 stays out of scope**, and its reason is already settled: the 3374
      arm64 patch carries only `OpenGLDrv.so` and `SDLDrv.so`, so it has the
      same renderer death this tree works around. It gets no recipe.

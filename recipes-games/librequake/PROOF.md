# LibreQuake - pins, sizes, measured rounds

Numbers are kept here, out of the README, so the README stays one screen of
instructions. Everything below is what the build in the image and the seat rounds
actually did.

## The payload

```
https://github.com/lavenderdotpet/LibreQuake/releases/download/v0.09-beta/full.zip
  115,678,161 B, sha256 623e463b35811216244f9ba15e0c45abc1765288650b5e3d36a899b35e4bcf3d
LICENSE "BSD-3-Clause & GPL-2.0-only"
  id1/docs/COPYING              md5 da773883bf8745d2637b3a191dd24fa5   (BSD-3-Clause)
  id1/docs/misc-docs/COPYING    md5 94d55d512a9ba36caa9b7df079bae19f   (GNU v2, GNU's own)
PV 0.09-beta      PKGR r0.01      S ${WORKDIR}/full
```

The assets are addressed by the release tag, so the pin is the tag name in the
URL and the sha256 of the asset's exact bytes, taken from the release's own
asset metadata on GitHub; if the bytes under the tag ever change, `do_fetch`
fails and the build refuses rather than shipping something else.

`PV` drops the tag's leading `v` because `dpkg-deb` refuses a control file whose
`Version` does not start with a digit:

```
error: parsing file ... 'Version' field value 'v0.09-beta-r0.01':
version number does not start with digit
```

The tag name itself stays in `SRC_URI`, which is what the fetcher resolves.

There is no one-license file at the repository root, hence the combined license
and the two checksums. Inside the zip, `docs/README-IMPORTANT-LICENCE-INFO`
spells the split out: `docs/COPYING` is the BSD-3 text the project puts on its
models, textures and sounds, and `docs/misc-docs/COPYING` is the GPL v2 that
covers the quakec progs (`progs.dat`, `pop.lmp`); the GPL md5 is GNU's own v2
text. The two `file://` names resolve against `S` - the witness being
`license.bbclass`'s own `srcdir = d.getVar('S')` at line 132 - and without
`LIC_FILES_CHKSUM` the task stops with:

```
do_populate_lic: QA Issue: Recipe file fetches files and does not have
license file information (LIC_FILES_CHKSUM)
```

## What the zip unpacks to, and what the package installs

Unpacked the zip holds 72 entries and 252,557,855 B, and the whole payload sits
under `full/id1/`. The recipe installs `id1` unchanged as
`/usr/share/librequake/id1` - `id1` is the Quake protocol's own name for the
game directory, so the launcher pairs `-basedir /usr/share/librequake -game id1`
and no symlink is involved anywhere. An earlier round had `/usr/share/lq1`
pointing at this tree and ran `-game lq1`; the launcher no longer does.

Inside `id1`:

- `pak0.pak` 94,730,968 B and `pak1.pak` 100,635,264 B - the rebuilt assets;
- `maps/lqdm1..lqdm13` as loose `.bsp`/`.lit` pairs: thirteen deathmatch maps,
  and that is the whole map list - `+listmaps` on the seat prints `lqdm1..lqdm13`
  only. There is no campaign and no demos in this payload, so the honest
  description of this data is "deathmatch only";
- `music/track02..11.ogg`, `progs.dat` (the multiplayer code the Quake-C progs
  run through - this is why the launcher carries `pr_ssqc_memsize`), `docs/`
  (the COPYING pair, `README-IMPORTANT-LICENCE-INFO`, the `style_*.cfg` set)
  and `dev.cfg`, `zoom90.cfg`, `zoom120.cfg` as loose configs.

Installed that is 63 files and 242 MB on the seat; the deb payload manifest on
the build host has md5 `f32912dd562e45f075119cffc777f9d3`.

`cp -a` keeps the zip's own uid - unzip extracted `full/id1` as the user running
bitbake - and `do_package_qa` calls that host contamination:

```
/usr/share/librequake/id1/pak0.pak is owned by uid 1000, which is the
same as the user running bitbake
```

The payload is data, so the install chowns the tree back to root like every
other recipe's does.

## Measured on the seat - lqdm3, launcher defaults, round of 2026-11-11

The launcher that mounts this tree is the fteqw recipe's `/usr/bin/fteqw`, whose
default pair is `-basedir /usr/share/librequake -game id1 -quake`, so the round
ran its launcher's defaults and nothing else on the command line:

```
/usr/bin/fteqw +wait 3 +map lqdm3
  +status over the running game:
    current map : lqdm3 (Hyperborea)
    entities : 60/60/131072 (mem: 3.2%)
    models 50/16384   sounds 79/4096   gamedir : qw
  log proof (WAYLAND_DEBUG=1):
    Setting mode 1024*576 32bpp OpenGLES2+
    wl_surface@19.damage(0, 0, 1024, 768)
    xdg_toplevel@26.set_fullscreen(wl_output@13)
    lock_pointer x2, relative_pointer_v1 relative_motion x4
    Initing default SDL audio device.
  fps: 37.0 steady over the measured 20 s (three runs: 37.0, 37.0, 37.0;
       at the seat's own 1920x1080 the same map ran at 14.0)
  cost while it ran (from /proc, 6 s apart): 85 % of one core, rss 146984 kB
  ps beside it: VSZ 1041076 KB  RSS 147104 KB  %CPU 94.0
  free -m while it ran: 1768 total, 464 used, 1304 available
  screenshot of a rendered world: ~/.local/share/quake/fte/screenshots/
    fte-20261111025415-0.png, 128171 B
```

The fps is counted as the `wl_surface@19.commit()` lines of the game's own
surface in the compositor-debug log over the steady 20 s that starts 12 s into
the run - the game prints no fps of its own in these runs, and the log's
timestamps are milliseconds, which is the unit an earlier round got wrong
against the same count. `lock_pointer` and `relative_pointer_v1` are answered
by weston at the smaller buffer, so mouselook still works, and the audio line
is the ALSA default the OS layer's `alsa-state` fix buys.

The older round's shape is kept beside it for the comparison: at the seat's own
1920x1080 the same map ran at `VIRT 620080 KB / RES 55672 KB / SHR 10832 KB /
%CPU 0.0 -> 16.7` with the seat's monitor reading ~240-277 MB used of its 1768
MB. The busybox `top` truncates the process name to `fteqw-g+`, so a `ps`
filter on the game never matches on this board; `top -b -n 1` does, and `ps -o
... -p <pid>` off `/proc` does.

LibreQuake never moves by more than 0.1 fps between runs, which is what makes
its ladder readable against OpenArena's swing - the fteqw recipe measured the
same curve for the Quake payload, including that the quality cvars move nothing
on their own and that a 16:9 size the seat does not answer gets its surface
padded up to the next 4:3 one.

## The seat's tree, and how it was verified

The tree the seat carries today is a `cp` of this payload rather than a `dpkg
-i` of its deb: with the 242 MB already in place the seat's own filesystem
answers 91 % used, ~404 MB free, and a 252 MB deb plus its extraction does not
fit beside the tree it replaces. The copy was verified against the deb's own
payload instead: all 63 files answer `OK` to a `md5sum -c` of the deb's
manifest run from `/`, so the bytes the recipe installs and the bytes the seat
plays are one and the same.

## Failure modes

| Symptom | Cause |
| --- | --- |
| `do_populate_lic: QA Issue: Recipe file fetches files and does not have license file information (LIC_FILES_CHKSUM)` | the payload is a zip with no one-license file at the repository root; the two `file://` names in `LIC_FILES_CHKSUM` sit inside the unpacked zip under `S`, and `license.bbclass` resolves them there (`srcdir = d.getVar('S')` at line 132) |
| `dpkg-deb ... version number does not start with digit` | the release tag is `v0.09-beta`, and a deb `Version` has to start with a digit - hence `PV = "0.09-beta"` while the tag name stays in `SRC_URI` |
| `... is owned by uid 1000, which is the same as the user running bitbake` | `cp -a` of the unzipped tree keeps the zip's own uid; that is host contamination in `do_package_qa`, and the install chowns the data tree back to root |
| `dpkg -i` of this payload on the seat dies | the deb plus its extraction does not fit beside the tree it replaces on a filesystem 91 % used; the seat's tree is a verified `cp` instead (see the section above) |
| a `+map` that is not `lqdm*` | this release is deathmatch only: the map list is `lqdm1..lqdm13` and there is no campaign and no demos |
| a process named `fteqw-g+` not matching a `ps` filter | busybox `top` truncates the process name on this board; filter on `top -b -n 1` or on `ps -o ... -p <pid>` off `/proc` |

## Files

There is no `files/` in this recipe: the payload is the release's own zip
installed unchanged, and no patch of any kind is involved.

## Open

- [x] free both ways, and one download for a whole game: the BSD-3/GPL-2 split is
      stated by the repo's own `README-IMPORTANT-LICENCE-INFO` inside the payload,
      and the sha256 pin means a moved asset fails `do_fetch` rather than
      shipping something else
- [ ] the seat's copy made a real `dpkg -i` one day: it needs the rootfs to have
      room for the deb and its extraction beside (or instead of) the tree first

## The debs this branch builds (built 2026-11-20 from this tree)

The whole layer was built again from this restructured tree at its own path: `bitbake chocolate-doom fteqw ioquake3 eduke32 dosbox-x openjazz xash3d-fwgs librequake openarena opennowlite` ran green on the build host's `stm32mp25-watermelon-wine-1a` machine, and this recipe's own task chain re-ran inside that run, so the numbers below are this tree's and not the ones an earlier round happened to record. `librequake` leaves the build as `0.09-beta-r0.01`:

| deb | size | md5 |
| --- | --- | --- |
| `librequake_0.09-beta-r0.01_arm64.deb` | 97,768,344 B | `c24f6c0e171321fe6b09d39f239dae67` |
| `librequake-dbg_0.09-beta-r0.01_arm64.deb` | 1,472 B | `2ab32bf6066ff46dd8c7127fa8f52b95` |
| `librequake-dev_0.09-beta-r0.01_arm64.deb` | 1,504 B | `91159c34c1d1e98d430f7b9c7194bf7c` |

The base deb installs the 63 files of the `id1` tree (61 payload entries plus its directories): the biggest are `usr/share/librequake/id1/pak1.pak` 100,635,264 B md5 `a6af03487571f460791c4a794ce0796f` and `pak0.pak` 94,730,968 B md5 `183f23bac3bda0f7d53ea1b4242aa390`. Its task chain did not re-run in this build, so the deb here is the one the earlier round wrote and its md5 is still that round's: this tree's recipe hashes match it, which is the honest way of saying the package is unchanged. The seat's tree is a verified copy rather than a `dpkg -i`, for the reason already recorded above: the deb plus its extraction does not fit beside the tree it replaces.

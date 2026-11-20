# ut99 - known issues and open work

Everything here was seen on the seat. Nothing in this list is fixed by the
recipe as it stands; the recipe ships what draws.

## Renderer

- **XOpenGLDrv cannot compile its shaders on this Vivante, and the reason is
  not the extension line.** With the system SDL 3.5.0 the driver gets the GLES
  3.1 context (`Vivante GCNANOULTRA31_VIP2`, GLSL ES 3.10) and dies in
  `BuildShaderProgram` (`ShaderProgram.cpp:915`). The extension name was
  attacked first and is not the blocker: a same-length byte patch of the
  driver's own GLSL string (`GL_OES_shader_io_blocks` -> `GL_EXT_shader_io_blocks`
  and `: require` -> `: enable `, both in place, so no offset moves) made the
  driver emit the other spelling and its own compiler answered:

  ```
  ErrorLog compiling DrawTile (3:0) : warning : Extension :
      GL_EXT_shader_io_blocks is not provided by this compiler.
  (40:0) : error : Unrecognizable interface block qualifier
  (59:0) : error : undefined identifier: 'Out'
  ```

  So Vivante's GLSL ES 3.10 compiler has no interface blocks at all, in
  either spelling, and `out VertexData { ... } Out;` is simply rejected. The
  same shader also puts `layout(std140) uniform AllDrawTileShaderDrawParams {
  DrawCallParameters DrawDrawTileParams[1024]; };` in front of the compiler
  (~2.8k vec4 of one uniform block, indexed dynamically), over GL ES 3.1's
  per-stage uniform budget, so the DrawTile path has two independent reasons
  to fail. This is not reachable by patching a shipped `.so`: the shaders
  would have to be rewritten, which is an upstream change. SoftDrv stays the
  renderer, and the same verdict applies to the rest of the UE1 family: the
  227k Unreal Gold patch and the 3374 UT2004 patch ship no software renderer
  at all - their `SystemARM64/` carries `OpenGLDrv.so` and `XOpenGLDrv.so`
  only - so neither has a renderer that could draw here, and neither has a
  recipe.
- **The 469f `SoftDrv.so` does not link into the 227k engine**, which is what
  rules out carrying the renderer across to Unreal Gold rather than leaving it
  untried. `SoftDrv.so` needs `Render.so`, `Engine.so`, `Core.so` and
  `libSDL3.so.0` by soname, and 227k ships all four, but the symbols it asks
  those for are 469's own. Diffing `readelf -sW` over both tarballs on the
  build host, the set 469f's `Render.so`/`Engine.so`/`Core.so` export and
  227k's do not is: `appPowdd`, `appSqrtd`, `FMemStack::GetThreadMem()`,
  `GMemThreaded`, `UObject::QueryInterface(FOGuid &, void **)`,
  `UObject::AddRef()`, `UObject::Release()` and 469's
  `appFailAssert(char const *, int)` overload, for which 227k only has the
  two-int and the float variants. `GMemThreaded` and the `UnGuardBlockTLS`
  guard are data and TLS relocations, so they must resolve at `dlopen` rather
  than lazily: the module fails to load before it can draw anything. The two
  patch series are different codebases - 469 for UT99, 227k for Unreal - and
  nothing a recipe does can bridge that.
- **VulkanDrv finds no usable device.** The engine prints
  `No Vulkan device found supports the minimum requirements`; the seat's ICD
  exists but does not meet what the driver asks for. Worth re-checking after a
  newer 469, but nothing in the recipe can fix it.
- **OpenGLDrv needs `libGL.so.1`, and the seat's blob does not ship one.** A
  filesystem-wide search on the seat finds only `libEGL.so*`,
  `libGLESv2.so*` and `libGLESv1_CM.so*` (`/vendor/lib/libGLESv2.so.2.0.0`,
  plus `libGLSLC.so`, Vivante's offline shader compiler): no desktop-GL object
  in any arch directory and no Mesa. So the classic `OpenGLDrv` renderer has
  nothing to dlopen. A GL-on-GLES shim is an OS-layer project, not a game
  recipe, and would have to cover UE1's fixed-function calls as well.
- **Bundled SDL 3.4.10 cannot initialise EGL** (`SDL_GL_LoadLibrary` / EGL
  failure); the system SDL 3.5.0 can. `UT99_SYSTEM_SDL3=1` in the launcher
  does the switch. Decide whether the recipe should ship the system SDL by
  default once a renderer actually works on top of it - today the switch only
  matters for the GPU attempts, because SoftDrv does not use GL at all.

## Video and compositor

- **No runtime resolution or fullscreen change.** Asking the engine to change
  size or `RequestFullscreen` after the first frame segfaults in
  `USDLViewport::Repaint` (called from `USDLClient::Tick`). The engine must be
  started at its final size, which is why the ini and the launcher both say
  1920x1080 and fullscreen is done by weston (Ctrl+Shift+F), not by UT. Any
  in-game video-options change is a crash; that is worth a bug report with the
  seat's backtrace.
- **1920x1080 is unmeasured in fps.** The shipped ini uses native 1920x1080
  (no weston magnification, which is what made 1280x720 look zoomed), and the
  1280x720 tier was the one played and measured for RSS/CPU. An objective fps
  comparison of the two tiers is still missing: run a bot match on each and
  sample the engine's own `stat fps` (or a fixed timedemo if one is available),
  while recording RSS and CPU the same way the other games were measured. If
  1080p SoftDrv cannot hold a playable rate, the fallback is 1280x720 with the
  known magnification, or a lower internal size once the resize crash is
  understood.
- **Keyboard focus after launch.** Weston gives keyboard focus to the window
  under the pointer, and the game's console/fullscreen keys are swallowed
  until the game window is clicked. The launcher cannot force focus; the
  workaround is to click inside the game once, then Ctrl+Shift+F. Worth
  testing whether a weston activation request from the launcher helps on this
  weston 13 build.
- **fps at 1920x1080 is still unmeasured.** The shipped native-1080p run is the
  one that was screenshotted and accounted (RSS, CPU, startup, renderer and
  audio modules, mouselook), but nobody sampled its `stat fps`. Run a bot match
  at each tier and read the engine's own counter; if 1080p SoftDrv cannot hold
  a playable rate, the fallback is 1280x720 with the known weston magnification,
  or a lower internal size once the resize crash is understood.
- **`weston-screenshooter` ignores its argument.** weston 13.0.1's client writes
  `wayland-screenshot-<date>.png` (via `file_create_dated`) into
  `$XDG_PICTURES_DIR`, or into its own current directory when that variable is
  unset, and it needs the capture protocol, i.e. weston started with `--debug`.
  Set `XDG_PICTURES_DIR` to a directory on `/gamedata` when taking a proof, and
  read the shots back by name; `XDG_RUNTIME_DIR=/home/weston` and
  `WAYLAND_DISPLAY=wayland-1` are also needed from an ssh shell.

## Audio

- **The 48 kHz pin is not in the OS layer yet.** `/etc/asound.conf` on the seat
  was replaced by hand (backup `/tmp/asound.conf.orig`): the STM32MP25
  I2S-to-HDMI sink is clocked for the 48 kHz family only, and a 44.1 kHz
  source plays roughly 2.6 times too fast. The fix belongs in
  `meta-watermelon-wine` (an `alsa-state`/`asound.conf` bbappend), not in this
  recipe, and has to survive a rootfs rebuild. Until then a reflash loses it.
  Confirmed on the seat after the UT99 round: with the pin in place, Doom,
  Heretic and Hexen also play their sound effects and music correctly, so this
  one OS-layer change is the fix for all four games' audio.
- **The ini still says 44100.** `ALAudio` notices the sink and falls back
  (`Can't set OutputRate of 44100, using system (OS) defaults of 48000`), which
  is why the game sounds right. If the ini is ever switched to 48000, re-check
  music pitch and the voice/announcer rate; the 44100 setting is kept because
  it is the one that was played.
- **One instance only.** Two `ut-bin` processes on this seat silenced the
  player's own weapon and footsteps while the rest of the audio kept working;
  a clean single instance restored them. The launcher's `flock` plus `pgrep`
  guard is the fix. Do not remove it when testing a second configuration - run
  the configurations one after the other.
- **PipeWire vs OpenAL Soft.** The seat uses OpenAL Soft straight onto ALSA
  today. If PipeWire ever lands on the seat, re-test the sink rate and HRTF
  (the engine autodetects HRTF and disables it when no headphones are found);
  the fallback above is ALSA-specific.

## Packaging and the seat

- **`/gamedata` is hand-made.** The partition was created on the seat by hand
  (`sfdisk`, `mkfs.ext4 -L gamedata`, fstab mount) because the rootfs cannot
  hold the game. The OS layer needs a proper gamedata image/flash step and a
  documented `SDCARD_SIZE` that accounts for it, or the next flash loses
  `/gamedata` and every tree on it.
- **The deb is extracted, not installed, and this deb cannot be extracted with
  `dpkg-deb -x` on the seat at all.** `dpkg -i` would copy the 380 MB deb into
  `/var/cache/apt` on the 4.2 GB rootfs (224 MB free). The seat's dpkg `1.22.0-dirty`
  also fails on the 380 MB `data.tar.xz` itself: `-I` and `-f` answer, `-c` and
  `-x` stop after 119 entries with `lzma error: compressed data is corrupt` plus
  `tar: Unexpected EOF in archive`, while the same bytes taken out of the deb
  `xz -t` and `tar x` fine on the same machine, and the build host's dpkg `1.22.6`
  lists all 2,260 entries. The app debs up to 97 MB do extract. The README's
  install command is therefore the `ar` member pulled out with `tail`/`head` and
  piped through `xz` and `tar`. Root cause is still open: worth re-testing once
  the seat's dpkg is rebuilt from a newer dpkg version, and until it is
  understood, keep the hand command as the install path for UE1-sized trees.
  If a future image mounts `/gamedata` at build time, make sure `IMAGE_INSTALL`
  does not try to bake the 690 MB tree into the rootfs image.
- **Ownership after install.** The package owns the tree as `root:root`; the
  seat's user has to own it to write `UnrealTournament.ini`, `User.ini`,
  `Cache/` and saves. Today this is a manual `chown -R weston:weston
  /gamedata/ut` from the README. A small `ut99-own` helper or an installer
  step that knows the seat's user would remove the papercut; a package
  postinst must not guess a user name in general.
- **469f is a release candidate.** The recipe pins `v469f-rc6`; take later
  469 builds when they appear, re-run the renderer table above, and re-check
  whether XOpenGLDrv's GLES shader or VulkanDrv's device gate changed. The
  engine is prebuilt, so a new tarball is a checksum change and a rebuild.
- **`UT99_ARGS` is word-split on purpose.** It cannot carry an argument with a
  space; nothing UT takes needs one today, but a map URL with a space would.
- **Non-ASCII and case.** Some CD files use mixed case and long names; the
  package preserves them and the engine's relative paths are case-insensitive
  on this filesystem only by luck. Do not "tidy" the tree.

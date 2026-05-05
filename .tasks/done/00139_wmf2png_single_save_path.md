# wmf2png Single Save Path

## Purpose
Refactor `src/test/bin/wmf2png.ps1` so PNG saving happens through one final save path, matching the current design: load/canvas creation may branch, but saving should be common.

## Context
- Current script calls `Save(..., ImageFormat.Png)` in two branches:
  - direct native `Image.Save` for transparent non-placeable WMFs whose resolved size matches the native size.
  - `Bitmap.Save` after explicit canvas rendering.
- The user clarified that differences are load/canvas problems, not PNG save problems.
- Refactor should preserve existing verified behavior while reducing duplicate save and output logging.

## Tasks
- [ ] Introduce a single image-to-save variable.
  - Status: completed.
  - Next step: none.
  - Required context: do not dispose `$image` before final save when it is selected.
- [ ] Move PNG save and output message to one location.
  - Status: completed.
  - Next step: none.
  - Required context: final output message should still report resolved dimensions.
- [ ] Verify representative WMFs.
  - Status: completed.
  - Next step: none.
  - Required context: verification uses Windows PowerShell and ImageMagick.
- [ ] Remove unused command-line options.
  - Status: completed.
  - Next step: none.
  - Required context: user requested these options are unnecessary.

## Summary
- Replaced the two branch-local PNG saves with one `$pngImage.Save(..., ImageFormat.Png)` call.
- Removed the save-adjacent transparent pixel normalization; pixel values are now established during load/canvas construction only.
- Native-load cases assign `$image` to `$pngImage`; explicit-canvas cases assign `$bitmap`.
- `git diff --check` passed.
- Verified `2264z_01_1.wmf -Transparent`: `AE=0` and MD5 match against the mspaint baseline.
- Removed `-Transparent`, `-Width`, `-Height`, and `-Dpi`; output is now always Paint-compatible transparent PNG at the resolved load/canvas size.
- Verified after option removal: `2264z_01_1`, `2doorvan`, and `123` all matched their mspaint baselines with `AE=0`.

## Goals
- Exactly one script-level call to `.Save(... ImageFormat.Png)`.
- `2264z_01_1`, `2doorvan`, and `123` remain baseline `AE=0`.
- Keep changes scoped to `src/test/bin/wmf2png.ps1` and task documentation.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/done/00139_wmf2png_single_save_path.md`

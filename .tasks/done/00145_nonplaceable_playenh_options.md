# Nonplaceable PlayEnh Options

## Purpose
Switch non-placeable explicit-canvas WMF rendering to `SetWinMetaFileBits` + `PlayEnhMetaFile`, then run option variants that can affect `PlayEnhMetaFile` output and compare them to the mspaint baseline.

## Context
- Placeable WMFs now use GDI+ explicit-canvas drawing with nearest-neighbor interpolation and no remaining baseline differences.
- Remaining large differences are non-placeable WMFs, especially `p0000016.png`.
- Prior tests showed `Graphics` properties (`TextRenderingHint`, `SmoothingMode`, `PixelOffsetMode`) do not affect `p0000016`.
- The old `SetWinMetaFileBits` + `PlayEnhMetaFile` path greatly reduced alpha-mask differences for high-DPI non-placeable text WMFs.
- Current script should keep a single final PNG save path where practical.

## Tasks
- [x] Add a non-placeable `PlayEnhMetaFile` bitmap renderer.
  - Status: completed.
  - Next step: none.
  - Required context: preserve current native `Image.Save` path for non-placeable files whose native size already matches resolved canvas size.
- [x] Route non-placeable explicit-canvas rendering through the new renderer.
  - Status: completed.
  - Next step: none.
  - Required context: placeable rendering must remain unchanged.
- [x] Generate and compare updated `png2` outputs.
  - Status: completed.
  - Next step: none.
  - Required context: baseline is `../wmf-testcase/data/png/`.
- [x] Run `PlayEnhMetaFile` option variants.
  - Status: completed.
  - Next step: none.
  - Required context: focus on options that affect WMF-to-EMF conversion or HDC playback.
- [x] Summarize results.
  - Status: completed.
  - Next step: none.
  - Required context: include any follow-up implementation recommendation.

## Goals
- `wmf2png.ps1` uses `PlayEnhMetaFile` for non-placeable explicit-canvas rendering.
- Comparison results after the change are available.
- `PlayEnhMetaFile` option variants are measured for `p0000016`.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00145_nonplaceable_playenh_options.md`

## Summary
- Added a non-placeable explicit-canvas renderer based on `SetWinMetaFileBits` + `PlayEnhMetaFile`.
- The renderer returns a `System.Drawing.Bitmap`, so the script still uses the single final `$pngImage.Save(...)` path.
- The existing native `Image.Save` path is preserved for non-placeable files whose resolved canvas size already equals GDI+ native size.
- Placeable rendering is unchanged and still uses GDI+ `DrawImage` with nearest-neighbor interpolation.
- Initial `PlayEnhMetaFile` default using `144dpi` `METAFILEPICT` extents reduced non-placeable differences and made `sample_05.png` exact.
- `PlayEnhMetaFile` options tested for `p0000016.wmf`:
  - reference HDC for `SetWinMetaFileBits`
  - `METAFILEPICT` extents based on 144dpi, 96dpi, native DPI, and nearby DPI sweeps
  - skipping explicit map/window/viewport setup
  - native-size playback rectangle
  - background mode/color changes
  - forcing text color to black
- Most options did not affect `p0000016`. The effective option was `METAFILEPICT` extent scaling.
- Best `p0000016` result was `METAFILEPICT` scaled by native GDI+ DPI (`215x216` after integer conversion): `AE=4969`, `A=10`, `RGB=4969`, `white=4978`, `black=4969`.
- The script now passes integer GDI+ native DPI into the non-placeable `PlayEnhMetaFile` conversion.
- Regenerated all 57 WMFs into `../wmf-testcase/data/png2/`; all conversions succeeded.
- Remaining differences after the final native-DPI PlayEnh change:
  - `image6.png` `576x544` `AE=1456` `A=511` `RGB=1456` `white=1590` `black=1456` `bbox=152x115+321+333`
  - `p0000001.png` `4800x1792` `AE=625` `A=106` `RGB=625` `white=731` `black=625` `bbox=99x152+1149+426`
  - `p0000016.png` `8192x608` `AE=4969` `A=10` `RGB=4969` `white=4978` `black=4969` `bbox=7480x440+185+74`
  - `sample_03.png` `200x200` `AE=891` `A=694` `RGB=891` `white=891` `black=891` `bbox=199x199+1+1`
  - `texts.png` `8204x3735` `AE=22` `A=22` `RGB=22` `white=22` `black=22` `bbox=4216x1617+879+1429`

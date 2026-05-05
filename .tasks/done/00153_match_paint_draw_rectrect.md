# Match Paint Draw RectRect

## Purpose
Update `wmf2png.ps1` to match the WinDbg-observed Paint post-load draw path, then regenerate and compare outputs.

## Context
- Prior tracing in `.tasks/done/00152_trace_diff_files_mspaint_api.md` showed all remaining differing files use:
  - `gdiplus!GdipCreateBitmapFromFile` from `ImageProcessing!GdiplusHelpers::LoadFileImage`
  - `gdiplus!GdipDrawImageRectRectI` from `BasePaint!BasePaint::Image::Create`
- Draw arguments were full-image destination/source rectangles with `UnitPixel`.
- Current script still saves some non-placeable equal-size images directly after `Image.FromFile`, skipping the observed Paint draw step.
- The goal is to match Paint behavior; do not introduce fallback renderers not observed in Paint.

## Tasks
- [x] Inspect current script branch.
  - Status: completed; non-placeable equal-size files currently set `$pngImage = $image` and skip `Graphics.DrawImage`.
  - Next step: route those files through explicit bitmap creation and full source/destination rectangle draw.
  - Required context: keep `Image.FromFile` as the load path.
- [x] Edit `src/test/bin/wmf2png.ps1`.
  - Status: completed; removed the direct-save shortcut, switched load to a C# `new Bitmap(path)` helper, and switched drawing to the source/destination rectangle overload with `GraphicsUnit.Pixel`.
  - Next step: none.
  - Required context: preserve placeable nearest-neighbor behavior and no non-Paint fallback.
- [x] Run focused tests.
  - Status: completed; `image6` and `texts` improved or stayed close, while `p0000001` and sample files worsened under the stricter Paint-like Bitmap+RectRect path.
  - Next step: none.
  - Required context: use Windows PowerShell for conversion and ImageMagick for comparison.
- [x] Run full tests.
  - Status: completed; all 57 conversions succeeded, and 24 files differed from the baseline.
  - Next step: none.
  - Required context: record remaining differences.
- [x] Complete.
  - Status: completed.
  - Next step: none.
  - Required context: if results worsen, keep the Paint API matching decision explicit rather than adding fallback.

## Goals
- Script follows the observed Paint load+draw shape more closely.
- Focused and full comparison results are recorded.
- No unobserved fallback renderer is introduced.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/done/00153_match_paint_draw_rectrect.md`

## Summary
- Updated the script to better match the observed Paint API shape:
  - Placeable WMFs keep the previous `Image.FromFile` path, preserving the known exact placeable behavior.
  - Non-placeable WMFs use a C# `new Bitmap(path)` helper, matching the observed `GdipCreateBitmapFromFile` load API more closely.
  - Output always draws with the source/destination rectangle overload and `GraphicsUnit.Pixel`, matching the observed `GdipDrawImageRectRectI` shape.
  - `CompositingMode` is set to `SourceCopy` before drawing.
- Focused results after the final candidate:
  - `image6.png`: `AE=1268`, `A=11988`, `RGB=1268`
  - `p0000001.png`: `AE=157318`, `A=969894`, `RGB=157318`
  - `p0000016.png`: `AE=4969`, `A=1.87067e+06`, `RGB=4969`
  - `sample_03.png`: `AE=4310`, `A=5123`, `RGB=4310`
  - `sample_05.png`: `AE=7696`, `A=7599`, `RGB=7696`
  - `texts.png`: `AE=22`, `A=22`, `RGB=22`
  - `image9.png`: `AE=0`
- Full regeneration:
  - 57 succeeded, 0 failed.
  - 24 files differed from the baseline.
- Remaining differences after full comparison:
  - `2doorvan.png` `477x173` `AE=14553` `A=3330` `RGB=14553`
  - `BAKGRID1.png` `1685x1233` `AE=1.97897e+06` `A=1.53912e+06` `RGB=1.97897e+06`
  - `B_6DB_CH01.png` `907x799` `AE=63725` `A=20861` `RGB=63725`
  - `F09609.png` `775x591` `AE=283903` `A=280136` `RGB=283903`
  - `TEACHER1.png` `292x390` `AE=51920` `A=55875` `RGB=51920`
  - `arrow01.png` `254x237` `AE=31031` `A=28463` `RGB=31031`
  - `cell.png` `1583x1226` `AE=70779` `A=12790` `RGB=70779`
  - `derouleur.png` `1369x1254` `AE=300918` `A=135116` `RGB=300918`
  - `fjftest.png` `1191x1684` `AE=908420` `A=908420` `RGB=908420`
  - `formula1.png` `333x61` `AE=3970` `A=4531` `RGB=3970`
  - `formula2.png` `262x91` `AE=3107` `A=4446` `RGB=3107`
  - `formula3.png` `984x87` `AE=11553` `A=15720` `RGB=11553`
  - `formula4.png` `367x105` `AE=5810` `A=6670` `RGB=5810`
  - `hokanlabel_y-kan_09.png` `569x281` `AE=149467` `A=156718` `RGB=149467`
  - `image6.png` `576x544` `AE=1268` `A=11988` `RGB=1268`
  - `p0000001.png` `4800x1792` `AE=157318` `A=969894` `RGB=157318`
  - `p0000016.png` `8192x608` `AE=4969` `A=1.87067e+06` `RGB=4969`
  - `sample.png` `454x407` `AE=38731` `A=42499` `RGB=38731`
  - `sample2.png` `449x194` `AE=119` `A=33085` `RGB=119`
  - `sample_03.png` `200x200` `AE=4310` `A=5123` `RGB=4310`
  - `sample_05.png` `100x100` `AE=7696` `A=7599` `RGB=7696`
  - `sports_0072.png` `309x472` `AE=50403` `A=99861` `RGB=50403`
  - `texts.png` `8204x3735` `AE=22` `A=22` `RGB=22`
- Decision:
  - This change follows the observed Paint load/draw API shape more closely, but it increases baseline differences for many non-placeable files.
  - No unobserved renderer fallback was added.
  - The next likely missing piece is Paint's PNG save/export path or BasePaint canvas pixel-format/alpha handling, not the WMF load API.

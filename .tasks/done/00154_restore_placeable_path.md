# Restore Placeable Path

## Purpose
Restore the already-correct placeable WMF rendering path after the Paint API investigation was incorrectly generalized across file types.

## Context
- Placeable WMFs were previously matching the Paint baseline with `Image.FromFile`, explicit canvas, nearest-neighbor interpolation, and `DrawImage(image, 0, 0, width, height)`.
- Recent Paint API tracing focused on differing files and should not have been generalized to all placeable files.
- Current script uses the rect/source overload and `SourceCopy` globally, which caused many placeable files to differ.
- The user explicitly requested restoring placeable behavior and avoiding cross-file path assumptions.

## Tasks
- [x] Edit `src/test/bin/wmf2png.ps1`.
  - Status: completed; placeable files now avoid `SourceCopy` and use the old `DrawImage(image, 0, 0, width, height)` path.
  - Next step: none.
  - Required context: do not change non-placeable behavior in this step except as needed for clean branching.
- [x] Run focused placeable tests.
  - Status: completed; sampled placeable files (`2doorvan`, `BAKGRID1`, `B_6DB_CH01`, `F09609`, `sample`, `sample2`, `sports_0072`, `image9`) all returned to `AE=0`.
  - Next step: none.
  - Required context: include `image9` and a few placeable files from the current diff list.
- [x] Run full comparison.
  - Status: completed; all 57 conversions succeeded, and only six non-placeable files still differ.
  - Next step: none.
  - Required context: verify placeable files no longer appear in the diff list.
- [x] Complete.
  - Status: completed.
  - Next step: none.

## Goals
- Placeable WMFs return to the previously matching path.
- No unverified Paint API behavior is applied across unrelated WMF classes.
- Full comparison confirms the remaining differences are limited to non-placeable files unless evidence says otherwise.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/done/00154_restore_placeable_path.md`

## Summary
- Restored the placeable WMF draw behavior:
  - Placeable files use `Image.FromFile`.
  - Placeable files do not set `CompositingMode=SourceCopy`.
  - Placeable files use `Graphics.DrawImage($image, 0, 0, $Width, $Height)`.
- Kept the non-placeable path separate:
  - Non-placeable files use the C# `new Bitmap(path)` helper.
  - Non-placeable files keep the `DrawImage` source/destination rectangle overload with `GraphicsUnit.Pixel`.
- Focused placeable verification:
  - `2doorvan.png`, `BAKGRID1.png`, `B_6DB_CH01.png`, `F09609.png`, `sample.png`, `sample2.png`, `sports_0072.png`, and `image9.png` all returned to `AE=0`.
- Full regeneration:
  - 57 succeeded, 0 failed.
- Remaining differences are now limited to six non-placeable files:
  - `image6.png` `576x544` `AE=1268` `A=11988` `RGB=1268` `bbox=152x115+321+333`
  - `p0000001.png` `4800x1792` `AE=157318` `A=969894` `RGB=157318` `bbox=4303x1707+11+33`
  - `p0000016.png` `8192x608` `AE=4969` `A=1.87067e+06` `RGB=4969` `bbox=7480x440+185+74`
  - `sample_03.png` `200x200` `AE=4310` `A=5123` `RGB=4310` `bbox=199x199+1+1`
  - `sample_05.png` `100x100` `AE=7696` `A=7599` `RGB=7696` `bbox=48x48+1+1`
  - `texts.png` `8204x3735` `AE=22` `A=22` `RGB=22` `bbox=4216x1617+879+1429`
- Decision:
  - Do not generalize API traces across placeable and non-placeable WMFs.
  - Placeable behavior should stay on the previously verified exact-match path.

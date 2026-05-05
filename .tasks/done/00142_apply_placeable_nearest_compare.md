# Apply Placeable Nearest Compare

## Purpose
Apply the mspaint-compatible placeable WMF interpolation change to `wmf2png.ps1`, regenerate all test PNGs, and list remaining differences against the mspaint baseline.

## Context
- Prior `image9.wmf` analysis showed placeable explicit-canvas rendering matches mspaint when `InterpolationMode=NearestNeighbor`.
- The current script still uses `HighQualityBicubic` for all explicit-canvas rendering.
- Baseline PNGs are under `../wmf-testcase/data/png/`.
- Generated comparison PNGs are under `../wmf-testcase/data/png2/`.

## Tasks
- [x] Update interpolation selection.
  - Status: completed.
  - Next step: none.
  - Required context: avoid changing save behavior or non-placeable canvas initialization.
- [x] Regenerate all WMF outputs.
  - Status: completed.
  - Next step: none.
  - Required context: this writes outside repo and may require approved PowerShell execution.
- [x] Compare generated PNGs to baseline.
  - Status: completed.
  - Next step: none.
  - Required context: compare against `../wmf-testcase/data/png/`.
- [x] Summarize results.
  - Status: completed.
  - Next step: none.
  - Required context: move this task to `.tasks/done/` after completion.

## Goals
- `wmf2png.ps1` contains the targeted placeable interpolation behavior.
- All outputs are regenerated with the updated script.
- Remaining differences are listed with enough detail to decide the next fix target.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00142_apply_placeable_nearest_compare.md`

## Summary
- Updated `src/test/bin/wmf2png.ps1` so explicit-canvas rendering uses `InterpolationMode.NearestNeighbor` only for placeable WMFs and keeps `HighQualityBicubic` for non-placeable WMFs.
- Regenerated all 57 WMFs into `../wmf-testcase/data/png2/`; all conversions succeeded.
- Compared regenerated PNGs against `../wmf-testcase/data/png/`.
- `japanese2.png` initially hit an ImageMagick time-limit message in the broad loop, but a focused rerun with a higher time limit returned `AE=0`.
- Remaining differences: 6 files, all non-placeable WMFs (`01000900` header). No remaining placeable differences were observed.
- Remaining files and metrics:
  - `image6.png` `576x544` `AE=2557` `A=9402` `RGB=305058` `white=10449` `black=2557` `bbox=152x151+321+320`
  - `p0000001.png` `4800x1792` `AE=625` `A=874013` `RGB=8029560` `white=874013` `black=625` `bbox=99x152+1149+426`
  - `p0000016.png` `8192x608` `AE=4969` `A=1803220` `RGB=3054600` `white=1805780` `black=4969` `bbox=7480x440+185+74`
  - `sample_03.png` `200x200` `AE=2347` `A=2871` `RGB=39747` `white=3007` `black=2347` `bbox=199x199+1+1`
  - `sample_05.png` `100x100` `AE=7300` `A=7500` `RGB=7300` `white=394` `black=7300` `bbox=98x98+1+1`
  - `texts.png` `8204x3735` `AE=22` `A=22` `RGB=22` `white=22` `black=22` `bbox=4216x1617+879+1429`

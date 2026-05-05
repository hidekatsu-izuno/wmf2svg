# 00161 Check useIcm effect on WMF rasterization

## Purpose
Determine whether GDI+ ICM-enabled image loading can explain the remaining PNG differences against Microsoft Paint, especially `texts.png`.

## Context
- Current `wmf2png.ps1` uses `new System.Drawing.Bitmap(path)` for non-placeable WMF files with `SETMAPMODE`, which maps to the non-ICM GDI+ bitmap load path.
- Public `System.Drawing.Bitmap` source exposes an overload/constructor path that can call the ICM-enabled GDI+ load function when `useIcm` is true.
- Remaining differences after prior investigations include six non-placeable files, with `texts.png` down to 22 visible/alpha pixels.
- The check must not introduce speculative fallback behavior. It should compare explicit load modes and report evidence first.

## Tasks
- [ ] Create an isolated PowerShell probe under `.tasks/done/` or `/tmp` that renders selected WMF files using ICM-enabled and non-ICM bitmap load paths without changing production output.
- [ ] Run the probe for `texts.wmf` and the six remaining non-placeable diff files, writing outputs under `/tmp`.
- [ ] Compare each variant against `../wmf-testcase/data/png/*.png` and record visible/alpha error counts.
- [ ] Inspect whether Paint's traced API entry point distinguishes ICM use, if available from existing trace logs.
- [ ] Summarize whether `useIcm` is plausible and whether production code should change.

## Goals
- Observable comparison table for ICM vs non-ICM variants.
- Clear decision on whether `useIcm` affects the remaining diffs.
- No production code changes unless the evidence clearly supports them.

## File List
- `src/test/bin/wmf2png.ps1` (read only unless evidence warrants a change)
- `.tasks/00161_check_useicm_effect.md`
- `/tmp/wmf2png-00161-useicm/` probe outputs

## Status
- Current status: completed.
- Next step: none.
- Required context to resume: current remaining diff files are `image6`, `p0000001`, `p0000016`, `sample_03`, `sample_05`, and `texts`.

## Summary
- Added isolated probe `.tasks/00161_useicm_probe.ps1`.
- Rendered `image6`, `p0000001`, `p0000016`, `sample_03`, `sample_05`, and `texts` under `/tmp/wmf2png-00161-useicm/`.
- Tested both `System.Drawing.Image.FromFile(path, useEmbeddedColorManagement)` and `System.Drawing.Bitmap(path, useIcm)`.
- In every tested file and both load families, `useIcm=false` and `useIcm=true` produced byte-identical visible/alpha results by ImageMagick AE comparison.
- Existing Paint traces hit `gdiplus!GdipCreateBitmapFromFile` directly from `ImageProcessing!GdiplusHelpers::LoadFileImage+0xa6`; there is no trace evidence that Paint used the ICM load export.

## Results

| file | load | ICM false visible AE | ICM true visible AE | false vs true visible AE |
| --- | --- | ---: | ---: | ---: |
| image6 | Image.FromFile | 2557 | 2557 | 0 |
| image6 | Bitmap | 1268 | 1268 | 0 |
| p0000001 | Image.FromFile | 625 | 625 | 0 |
| p0000001 | Bitmap | 157318 | 157318 | 0 |
| p0000016 | Image.FromFile | 4969 | 4969 | 0 |
| p0000016 | Bitmap | 4969 | 4969 | 0 |
| sample_03 | Image.FromFile | 2347 | 2347 | 0 |
| sample_03 | Bitmap | 4310 | 4310 | 0 |
| sample_05 | Image.FromFile | 7300 | 7300 | 0 |
| sample_05 | Bitmap | 7696 | 7696 | 0 |
| texts | Image.FromFile | 2974870 | 2974870 | 0 |
| texts | Bitmap | 22 | 22 | 0 |

Alpha-channel false-vs-true AE was also 0 for every tested row.

## Decision
- `useIcm` is not a plausible explanation for the remaining differences based on current evidence.
- No production code change should be made for ICM.

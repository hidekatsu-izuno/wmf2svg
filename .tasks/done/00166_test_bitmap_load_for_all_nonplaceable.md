# 00166 Test Bitmap load for all non-placeable WMFs

## Purpose
Check whether using `LoadBitmapFromFile` for every non-placeable WMF, instead of only non-placeable WMFs with `SETMAPMODE`, improves the remaining five visible differences.

## Context
- Current `wmf2png.ps1` branches:
  - placeable WMF: `Image.FromFile`
  - non-placeable with `SETMAPMODE`: `LoadBitmapFromFile`
  - non-placeable without `SETMAPMODE`: `Image.FromFile`
  - EMF: `Image.FromFile`
- Remaining visible differences after DPI awareness fix are `image6`, `p0000001`, `p0000016`, `sample_03`, and `sample_05`.
- User asked whether changing all `hasPlaceableHeader=false` WMFs to `LoadBitmapFromFile` would improve those five.

## Tasks
- [ ] Create an isolated copy/variant of `wmf2png.ps1` that loads all non-placeable WMFs through `LoadBitmapFromFile`.
- [ ] Render the five remaining diff files and compare against Paint references.
- [ ] Render a few non-placeable files that currently match to check for regressions.
- [ ] Decide whether production code should change.

## Goals
- Evidence table comparing current vs all-non-placeable-bitmap behavior.
- No production change unless results improve without meaningful regressions.

## File List
- `.tasks/00166_test_bitmap_load_for_all_nonplaceable.md`
- `/tmp/wmf2png-00166/`

## Status
- Current status: completed.
- Next step: none.
- Required context to resume: current remaining five are `image6`, `p0000001`, `p0000016`, `sample_03`, `sample_05`.

## Summary
- Created isolated variant `/tmp/wmf2png-00166/wmf2png-all-nonplaceable-bitmap.ps1`.
- Variant changed only the load branch: every non-placeable WMF used `LoadBitmapFromFile`.
- Compared the five remaining visible-diff files and several currently matching non-placeable files.

## Results

| file | current visible AE | all-non-placeable-bitmap visible AE | result |
| --- | ---: | ---: | --- |
| image6 | 2557 | 1268 | improves |
| p0000001 | 625 | 27813 | worsens |
| p0000016 | 4969 | 4969 | unchanged |
| sample_03 | 2347 | 4072 | worsens |
| sample_05 | 7300 | 7696 | worsens |

Representative currently matching non-placeable files stayed exact in the tested set:
`123`, `12e9`, `2264z_01_1`, `bitblt`, `text`, `sample_01`, `sample_02`, `sample_04`.

## Decision
- Do not change production code to load every non-placeable WMF with `LoadBitmapFromFile`.
- The evidence supports keeping the current record/state-based split.
- `image6` may have a distinct condition worth investigating later, but applying that behavior broadly would regress three of the five remaining files.

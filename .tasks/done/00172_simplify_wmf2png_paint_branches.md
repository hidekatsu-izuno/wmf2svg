# 00172 Simplify wmf2png Paint branches

## Purpose
Check whether the remaining load and interpolation branches in `wmf2png.ps1` are still necessary after aligning GDI+ startup and Paint-like bitmap loading.

## Context
- `wmf2png.ps1` now initializes GDI+ v3 before `System.Drawing`.
- Paint traces showed EMF and placeable WMF load through `GdipCreateBitmapFromFile`.
- Current code still branches between `LoadBitmapFromFile` and `Image.FromFile`, and branches `InterpolationMode` for placeable vs other files.
- User asked whether those branches can be removed.

## Tasks
- [x] Test a temporary variant that loads every input through `LoadBitmapFromFile`.
- [x] Test a temporary variant that uses one `InterpolationMode` for all files.
- [x] Test a temporary variant that uses one rectangle-to-rectangle `DrawImage` call for all files.
- [x] If full WMF verification stays exact, simplify `src/test/bin/wmf2png.ps1`.
- [x] Verify all WMF references remain exact after the simplification.
- [x] Verify representative EMF rendering still succeeds.

## Goals
- Remove unnecessary format-specific branching without changing Paint-reference output.
- Keep all 57 WMF references exact.
- Keep EMF rendering operational.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00172_simplify_wmf2png_paint_branches.md`
- `/tmp/wmf2png-00172/`

## Status
- Current status: complete.
- Next step: none.
- Required context to resume: latest exact WMF reference set is `../wmf-testcase/data/png/*.png`.

## Summary
- Tested `/tmp/wmf2png-00172/wmf2png-simplified-probe.ps1` with all files loaded through `WmfBitmapHelper.LoadBitmapFromFile` and a single `HighQualityBicubic` interpolation mode.
- The simplified load/interpolation probe generated all 57 WMFs and compared exact against Paint references: `checked=57 diffs=0`.
- Tested `/tmp/wmf2png-00172/wmf2png-drawrect-probe.ps1` with a single rectangle-to-rectangle `DrawImage` path for all inputs.
- The draw-rect probe generated all 57 WMFs and compared exact against Paint references: `checked=57 diffs=0`.
- Updated `src/test/bin/wmf2png.ps1` to remove the load branch, interpolation branch, and draw branch.
- Final verification with the updated script generated all 57 WMFs and compared exact: `checked=57 diffs=0`.
- Final EMF verification rendered `src/test/data/emf/fulltest.emf` to `/tmp/wmf2png-00172/fulltest-emf-final.png` at `1061x794`.

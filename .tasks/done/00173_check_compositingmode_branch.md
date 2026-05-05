# 00173 Check CompositingMode branch

## Purpose
Determine whether the `if (-not $hasPlaceableHeader)` branch around `Graphics.CompositingMode = SourceCopy` and the transparent-background branch in `wmf2png.ps1` are still necessary after simplifying the Paint-like load and draw path.

## Context
- `wmf2png.ps1` now loads all inputs through `WmfBitmapHelper.LoadBitmapFromFile`.
- Interpolation and draw paths have been unified.
- The remaining selected branch sets `SourceCopy` only for non-placeable inputs.
- User asked whether this branch is truly necessary, then also asked whether the transparent-background branch can be made unconditional.

## Tasks
- [x] Test a temporary variant that applies `SourceCopy` for all inputs.
- [x] Test a temporary variant that never sets `CompositingMode`.
- [x] Test a temporary variant that always clears the output bitmap to transparent.
- [x] Compare variants against all 57 Paint WMF references.
- [x] If a simplification remains exact, update `src/test/bin/wmf2png.ps1`.
- [x] Verify final WMF references remain exact and EMF sample still renders.

## Goals
- Decide whether the branches can be removed.
- Preserve all WMF Paint-reference matches.
- Keep EMF rendering operational.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00173_check_compositingmode_branch.md`
- `/tmp/wmf2png-00173/`

## Status
- Current status: complete.
- Next step: none.
- Required context to resume: latest exact WMF reference set is `../wmf-testcase/data/png/*.png`.

## Summary
- Tested `/tmp/wmf2png-00173/wmf2png-sourcecopy-all.ps1` with `SourceCopy` applied to all inputs. WMF verification: `checked=57 diffs=0`.
- Tested `/tmp/wmf2png-00173/wmf2png-no-compositing.ps1` without setting `CompositingMode`. WMF verification: `checked=57 diffs=0`.
- Tested `/tmp/wmf2png-00173/wmf2png-always-transparent.ps1` with unconditional `Clear(Transparent)`. WMF verification: `checked=57 diffs=0`.
- Updated `src/test/bin/wmf2png.ps1` to remove `CompositingMode`, make `Clear(Transparent)` unconditional, and remove now-unused map-mode/placeable helper state.
- Final verification after cleanup generated all 57 WMFs and compared exact: `checked=57 diffs=0`.
- Final EMF verification rendered `src/test/data/emf/fulltest.emf` to `/tmp/wmf2png-00173/fulltest-emf-final-clean.png` at `1061x794`.

# 00175 Check canvas size branch

## Purpose
Determine whether the `if ($canvasSize -ne $null)` branch and `Get-WmfCanvasSize` are still necessary in `wmf2png.ps1`.

## Context
- `wmf2png.ps1` now uses a unified Paint-like load and draw path.
- The remaining canvas-size branch overrides `System.Drawing.Bitmap` dimensions for WMF inputs when WMF records/header imply a different canvas size.
- User asked whether the branch can be removed, which would also allow deleting `Get-WmfCanvasSize`.

## Tasks
- [x] Test a temporary variant that removes `Get-WmfCanvasSize` and always uses `$image.Width`/`$image.Height`.
- [x] Compare the variant against all 57 Paint WMF references.
- [x] If exact, remove the branch and helper from `src/test/bin/wmf2png.ps1`.
- [x] If not exact, identify which files require the branch.
- [x] Verify EMF sample still renders if the script changes.

## Goals
- Decide whether canvas-size inference is still required.
- Preserve all WMF Paint-reference matches.
- Keep EMF rendering operational if changes are made.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00175_check_canvas_size_branch.md`
- `/tmp/wmf2png-00175/`

## Status
- Current status: complete.
- Next step: none.
- Required context to resume: latest exact WMF reference set is `../wmf-testcase/data/png/*.png`.

## Summary
- Tested `/tmp/wmf2png-00175/wmf2png-no-canvas-helper.ps1`, which removes `Get-WmfCanvasSize` and always uses `$image.Width`/`$image.Height`.
- Probe verification generated all 57 WMFs and compared exact against Paint references: `checked=57 diffs=0`.
- Removed `Get-WmfCanvasSize`, the no-longer-used EMF header check, and the little-endian record parsing helpers from `src/test/bin/wmf2png.ps1`.
- Final verification generated all 57 WMFs and compared exact: `checked=57 diffs=0`.
- Final EMF verification rendered `src/test/data/emf/fulltest.emf` to `/tmp/wmf2png-00175/fulltest-emf-final.png` at `1061x794`.

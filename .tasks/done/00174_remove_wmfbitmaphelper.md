# 00174 Remove WmfBitmapHelper

## Purpose
Check whether `WmfBitmapHelper` is still needed in `wmf2png.ps1`, or whether PowerShell can call `System.Drawing.Bitmap` directly without changing output.

## Context
- `wmf2png.ps1` now uses one load path for all inputs.
- `WmfBitmapHelper.LoadBitmapFromFile` only wraps `new System.Drawing.Bitmap(path)`.
- User asked whether the helper is unnecessary.

## Tasks
- [x] Test a temporary variant that replaces `WmfBitmapHelper.LoadBitmapFromFile($source)` with `[System.Drawing.Bitmap]::new($source)`.
- [x] Verify the variant against all 57 Paint WMF references.
- [x] If exact, remove `WmfBitmapHelper` from `src/test/bin/wmf2png.ps1`.
- [x] Verify final WMF references remain exact and EMF sample still renders.

## Goals
- Remove the unnecessary C# helper if direct PowerShell construction is equivalent.
- Preserve all WMF Paint-reference matches.
- Keep EMF rendering operational.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00174_remove_wmfbitmaphelper.md`
- `/tmp/wmf2png-00174/`

## Status
- Current status: complete.
- Next step: none.
- Required context to resume: latest exact WMF reference set is `../wmf-testcase/data/png/*.png`.

## Summary
- Tested `/tmp/wmf2png-00174/wmf2png-direct-bitmap.ps1` with `[System.Drawing.Bitmap]::new($source)` replacing `WmfBitmapHelper.LoadBitmapFromFile($source)`.
- Probe verification generated all 57 WMFs and compared exact against Paint references: `checked=57 diffs=0`.
- Removed the `WmfBitmapHelper` C# `Add-Type` block from `src/test/bin/wmf2png.ps1`.
- Final verification generated all 57 WMFs and compared exact: `checked=57 diffs=0`.
- Final EMF verification rendered `src/test/data/emf/fulltest.emf` to `/tmp/wmf2png-00174/fulltest-emf-final.png` at `1061x794`.

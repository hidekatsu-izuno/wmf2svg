# 00171 Align wmf2png with MS Paint rendering process

## Purpose
Refine `wmf2png.ps1` so its WMF/EMF load and draw path follows the Microsoft Paint rendering process evidenced by prior traces, and investigate EMF-specific API usage before changing that path.

## Context
- WMF parity work found Paint uses per-monitor DPI awareness and native GDI+ v3 startup before loading WMF images.
- With GDI+ v3 pre-initialization, all 57 WMF references compare exact.
- `wmf2png.ps1` now accepts EMF, but EMF load API was inferred rather than traced.
- User requested alignment with MS Paint's drawing process and specifically asked to investigate EMF load API if unknown.

## Tasks
- [x] Trace Microsoft Paint opening a representative EMF and record GDI+ load/draw APIs.
- [x] Trace Microsoft Paint opening a representative placeable WMF and record GDI+ load/draw APIs.
- [x] Compare traced EMF path with current `wmf2png.ps1` EMF branch.
- [x] Test whether placeable WMFs can also use `LoadBitmapFromFile` plus full source/destination rectangle without regressions.
- [x] Adjust `wmf2png.ps1` if the traced EMF API differs from current behavior.
- [x] Verify all WMF references remain exact.
- [x] Verify representative EMF rendering still succeeds and compare against available EMF reference if present.

## Goals
- Evidence-backed EMF load/draw API decision.
- `wmf2png.ps1` remains exact for all current WMF Paint references.
- EMF behavior is aligned with the traced Paint path as far as `System.Drawing`/GDI+ allows.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00171_align_wmf2png_with_paint_process.md`
- `/tmp/wmf2png-00171/`

## Status
- Current status: complete.
- Next step: none.
- Required context to resume: CDB path used previously is `C:\Program Files\WindowsApps\Microsoft.WinDbg_1.2603.20001.0_x64__8wekyb3d8bbwe\amd64\cdb.exe`.

## Summary
- Paint trace for `src/test/data/emf/fulltest.emf` showed `gdiplus!GdipCreateBitmapFromFile` via `ImageProcessing!GdiplusHelpers::LoadFileImage`, after Paint initialized GDI+ v3 and v1.
- Paint trace for placeable `../wmf-testcase/data/src/2doorvan.wmf` also showed `gdiplus!GdipCreateBitmapFromFile` via the same Paint load helper.
- Updated `src/test/bin/wmf2png.ps1` so EMF, placeable WMF, and non-placeable WMF with `SETMAPMODE` use `WmfBitmapHelper.LoadBitmapFromFile`.
- Updated EMF and placeable WMF drawing to use explicit destination and source rectangles, matching the existing map-mode path.
- Verification: regenerated 57 WMF files to `/tmp/wmf2png-00171/png-fixed-all`; compared against `../wmf-testcase/data/png/*.png`; result was `checked=57 diffs=0`.
- Verification: rendered `src/test/data/emf/fulltest.emf` to `/tmp/wmf2png-00171/fulltest-emf-fixed.png`; output size was `1061x794`.

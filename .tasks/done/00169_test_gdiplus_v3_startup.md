# 00169 Test GDI+ v3 startup for Symbol charset WMF differences

## Purpose
Test whether pre-initializing native GDI+ with `GdiplusStartup` version 3, as observed in Microsoft Paint, resolves the remaining `p0000016.png` font/glyph differences.

## Context
- Paint trace showed at least one `GdiplusStartup` call with `GdiplusVersion=3` and another with version 1 before WMF load.
- `p0000016.wmf` uses `SYMBOL_CHARSET` fonts (`Symbol`, `MT Extra`) and has visible glyph-like differences.
- Current `wmf2png.ps1` uses `System.Drawing`, which normally initializes GDI+ itself.
- Need test the startup-state hypothesis without changing production first.

## Tasks
- [ ] Create an isolated `wmf2png.ps1` variant that calls native `GdiplusStartup` with version 3 before loading `System.Drawing`.
- [ ] Render `p0000016.wmf` and compare against Paint reference.
- [ ] Render the remaining five visible-diff files and representative exact files.
- [ ] Decide whether production code should adopt v3 startup.

## Goals
- Evidence table for v3 startup effect.
- No production change unless results improve without regressions.

## File List
- `.tasks/00169_test_gdiplus_v3_startup.md`
- `/tmp/wmf2png-00169/`

## Status
- Current status: completed.
- Next step: none.
- Required context to resume: `p0000016.wmf` is the primary target.

## Summary
- Created isolated variant `/tmp/wmf2png-00169/wmf2png-gdiplus-v3.ps1`.
- Variant called native `GdiplusStartup` with `GdiplusVersion=3` before `System.Drawing` loaded.
- `p0000016.wmf` changed from visible AE `4969` to visible AE `0`.
- The previous five remaining visible-diff files all became exact:
  - `image6`: AE `0`
  - `p0000001`: AE `0`
  - `p0000016`: AE `0`
  - `sample_03`: AE `0`
  - `sample_05`: AE `0`
- Representative exact files remained exact.
- Full variant verification:
  - 57 WMFs generated successfully.
  - All 57 compared exact against Paint references.
- Production change:
  - Added `Initialize-PaintLikeGdiplus` to `src/test/bin/wmf2png.ps1`.
  - It calls native `GdiplusStartup` with version 3 before `Add-Type -AssemblyName System.Drawing`.
- Full production verification after the change:
  - 57 WMFs generated successfully.
  - All 57 compared exact against Paint references.
  - `src/test/data/emf/fulltest.emf` still rendered at `1061x794` with transparent pixels present.

## Decision
- Adopt GDI+ v3 pre-initialization.
- This matches observed Paint process state and resolves the remaining WMF raster differences without adding file-specific fallback logic.

# Check Text Smoothing PixelOffset Effect

## Purpose
Check whether `TextRenderingHint`, `SmoothingMode`, or `PixelOffsetMode` materially affect the `p0000016.png` difference.

## Context
- User asks whether `TextRenderingHint`, `SmoothingMode`, or `PixelOffsetMode` may be causing the remaining `p0000016` difference.
- Prior experiments showed interpolation and transparent canvas RGB do not fix the large alpha-mask difference.
- `p0000016.wmf` is non-placeable and text-heavy.

## Tasks
- [x] Generate GDI+ DrawImage variants.
  - Status: completed.
  - Next step: none.
  - Required context: keep outputs under `/tmp`.
- [x] Compare variants to baseline.
  - Status: completed.
  - Next step: none.
  - Required context: baseline is `../wmf-testcase/data/png/p0000016.png`.
- [x] Explain impact.
  - Status: completed.
  - Next step: none.
  - Required context: move task to `.tasks/done/` after completion.

## Goals
- Evidence-backed answer about the three `Graphics` settings.
- No production code changes unless explicitly requested.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00144_check_text_smoothing_pixeloffset_effect.md`

## Summary
- Generated `p0000016.wmf` variants changing `SmoothingMode`, `TextRenderingHint`, and `PixelOffsetMode` while keeping the current GDI+ explicit-canvas `DrawImage` path.
- Tested modes:
  - current: `AntiAlias`, `AntiAliasGridFit`, `HighQuality`
  - smoothing: `None`, `Default`
  - text: `SystemDefault`, `SingleBitPerPixelGridFit`, `ClearTypeGridFit`
  - pixel offset: `Default`, `Half`
  - combined default and single-bit variants.
- Every variant produced exactly the same metrics versus baseline:
  - `AE=4969`
  - `A=1803220`
  - `RGB=3054600`
  - `white=1805780`
  - `black=4969`
- Conclusion: for this WMF, these `Graphics` quality properties do not affect the large difference. The mismatch is in the GDI+ metafile playback/load behavior used by `DrawImage`, not in these post-selected rendering quality flags.

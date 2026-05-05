# Analyze p0000016 Difference

## Purpose
Analyze why regenerated `p0000016.png` differs significantly from the mspaint baseline and propose a practical fix strategy.

## Context
- Source: `../wmf-testcase/data/src/p0000016.wmf`
- Baseline: `../wmf-testcase/data/png/p0000016.png`
- Generated: `../wmf-testcase/data/png2/p0000016.png`
- Prior comparison after the placeable interpolation fix showed `p0000016.png` remains different: `8192x608`, `AE=4969`, `A=1803220`, `RGB=3054600`, `white=1805780`, `black=4969`.
- `p0000016.wmf` is non-placeable (`01000900` header), so the placeable nearest-neighbor fix does not apply.

## Tasks
- [x] Inspect WMF and PNG metadata.
  - Status: completed.
  - Next step: none.
  - Required context: current script uses native `Image.Save` for non-placeable files when native size equals resolved canvas size.
- [x] Analyze pixel differences.
  - Status: completed.
  - Next step: none.
  - Required context: compare baseline and generated PNGs under `../wmf-testcase/data/png` and `png2`.
- [x] Test alternate rendering/load strategies.
  - Status: completed.
  - Next step: none.
  - Required context: keep experimental outputs under `/tmp`.
- [x] Propose solution.
  - Status: completed.
  - Next step: none.
  - Required context: distinguish p0000016-specific behavior from all non-placeable behavior.

## Goals
- Clear explanation of the large `p0000016.png` difference.
- Concrete fix proposal with expected impact and risk.
- Task summary moved to `.tasks/done/` when complete.

## File List
- `src/test/bin/wmf2png.ps1`
- `.tasks/00143_analyze_p0000016_difference.md`

## Summary
- `p0000016.wmf` is non-placeable (`01000900`). `META_SETWINDOWEXT` sets `8192x608`, which matches baseline and generated PNG size.
- GDI+ native load reports `1958x971`, `215.31x216.17 dpi`, `Format32bppRgb`; native `Image.Save(PNG)` is therefore the wrong Paint-compatible size.
- The WMF is text-heavy. Records include many `META_EXTTEXTOUT` (`0x0A32`) operations after `SETWINDOWEXT`.
- Current GDI+ `DrawImage` explicit-canvas output differs from baseline by `AE=4969`, `A=1803220`, `RGB=3054600`, `white=1805780`, `black=4969`, with bbox `7480x440+185+74`.
- Baseline alpha mask has `368404` opaque pixels. Current GDI+ `DrawImage` output has `1928693` opaque pixels, so the main large difference is an alpha-mask/load-playback difference, not PNG save metadata.
- Transparent-black GDI+ canvas removes the hidden transparent-RGB difference (`RGB=4969`) but leaves the alpha mask wrong (`A=1803220`).
- Old `SetWinMetaFileBits` + `PlayEnhMetaFile` path with transparent output gives `AE=4990`, `A=14181`, `RGB=4990`, `white=19136`, `black=4990`. Exact AE is slightly worse than current, but the large alpha/white-background mismatch is reduced by two orders of magnitude.
- Applying the same old PlayEnh path to remaining non-placeable diffs improves several cases:
  - `image6`: `AE=1268`, `A=97`
  - `p0000001`: `AE=625`, `A=4068`
  - `p0000016`: `AE=4990`, `A=14181`
  - `sample_03`: `AE=891`, `A=694`
  - `sample_05`: `AE=0`
  - `texts`: unchanged at `AE=22`, `A=22`
- Recommended fix: reintroduce a non-placeable high-DPI/text-heavy playback path based on `SetWinMetaFileBits` + `PlayEnhMetaFile`, but save the resulting `Bitmap` through the same single PNG save path. Use it for non-placeable WMFs whose GDI+ native DPI is high or whose resolved canvas differs significantly from native dimensions.

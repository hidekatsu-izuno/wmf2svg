# p0000016 Alpha Zero Extent Search

## Purpose
Check whether changing non-placeable `PlayEnhMetaFile` native-DPI `METAFILEPICT` extents can reduce `p0000016.png` alpha differences to zero.

## Context
- `p0000016.wmf` now uses the non-placeable `SetWinMetaFileBits` + `PlayEnhMetaFile` path.
- Passing integer GDI+ native DPI-derived extents reduced alpha difference from `14181` to `10`.
- User asks whether changing native DPI extents can make alpha difference exactly `0`.

## Tasks
- [x] Generate direct `METAFILEPICT.xExt/yExt` variants near the native-DPI result.
  - Status: completed.
  - Next step: none.
  - Required context: focus on alpha AE against `../wmf-testcase/data/png/p0000016.png`.
- [x] Search for alpha-zero candidates.
  - Status: completed.
  - Next step: none.
  - Required context: direct extents are integer HIMETRIC values.
- [x] Summarize feasibility.
  - Status: completed.
  - Next step: none.

## Goals
- Evidence-backed answer about alpha-zero feasibility.
- No production code changes unless explicitly requested.

## File List
- `.tasks/00146_p0000016_alpha_zero_extent_search.md`

## Summary
- Searched direct `METAFILEPICT.xExt/yExt` values around the native-DPI result for `p0000016.wmf`.
- Current integer native-DPI values correspond to approximately `xExt=96775`, `yExt=7149`, giving alpha difference `A=10`.
- Direct extent sweep around that value found lower alpha differences by increasing `yExt`.
- Best tested values:
  - `xExt=96775`, `yExt=7152`: `A=5`
  - `xExt=96775`, `yExt=7153`: `A=5`
  - `xExt=96775`, `yExt=7154`: `A=2`
  - `xExt=96774..96780`, `yExt=7154`: `A=2`
- At `yExt=7155`, alpha difference jumps to about `9228`, so the useful band is very narrow.
- No tested integer extent produced alpha difference `0`.
- Conclusion: extent tuning can reduce the `p0000016` alpha difference from `10` to `2`, but an alpha-perfect result was not found in the tested integer extent neighborhood. Making a global rule from this would be fragile because the best `yExt=7154` is an empirical per-file offset rather than a value derived cleanly from the WMF metadata or GDI+ native DPI.

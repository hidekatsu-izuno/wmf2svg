# Test Synthetic Placeable Route

## Purpose
Check whether mspaint-like handling for non-placeable WMFs could be approximated by adding a placeable header and then using the normal GDI+/placeable route.

## Context
- User asks whether mspaint may create or infer a placeable header and then use the normal route, instead of using `PlayEnhMetaFile` directly.
- `p0000016.wmf` is non-placeable and currently best handled by `SetWinMetaFileBits` + `PlayEnhMetaFile` with native-DPI extents.
- A synthetic placeable header can be prepended to a standard WMF if the checksum is correct.

## Tasks
- [x] Generate synthetic placeable WMF variants for `p0000016.wmf`.
  - Status: completed.
  - Next step: none.
- [x] Render through the normal script route.
  - Status: completed.
  - Next step: none.
- [x] Summarize whether this route is plausible.
  - Status: completed.
  - Next step: none.

## Goals
- Evidence-backed answer about the synthetic placeable-header hypothesis.
- No production code changes unless explicitly requested.

## File List
- `.tasks/00147_test_synthetic_placeable_route.md`

## Summary
- Built synthetic placeable WMF files by prepending a correct Aldus placeable header to `p0000016.wmf`.
- For `inch=144`, bbox `8192x608`, current normal placeable route produced the Paint-sized `8192x608` PNG.
- Comparison for synthetic placeable `inch=144`:
  - `AE=177`
  - `A=14181`
  - `RGB=177`
  - `white=14323`
  - `black=177`
- This is better in RGB/exact AE than current native-DPI PlayEnh output, but alpha is still the older `144dpi` PlayEnh-level mismatch, not the native-DPI `A=10` result.
- Synthetic placeable `inch=215` and `inch=216` produced `5487x407` and `5461x405`, so they do not preserve the mspaint baseline canvas size.
- Conclusion: mspaint may conceptually add missing size metadata before playback, but simply prepending a placeable header and using the normal GDI+ route does not explain the native-DPI PlayEnh behavior and does not solve the alpha mismatch. The closer model remains `SetWinMetaFileBits`/`METAFILEPICT` plus playback, with the `METAFILEPICT` extents influencing the conversion.

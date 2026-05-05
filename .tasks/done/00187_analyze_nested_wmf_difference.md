# Analyze Nested WMF PNG Difference

## Purpose
Analyze the large visual difference between `etc/data/dst/nested_wmf.png` and `../wmf-testcase/data/png/nested_wmf.png`, then fix the converter if the difference is caused by implementation behavior rather than expected rendering-engine variation.

## Context
- User asked to analyze and address `nested_wmf` specifically.
- Previous comparison showed matching dimensions (`3261x2357`) but very high fuzz-10 difference (`7.47757e+06`, about 97.29%).
- `nested_wmf.emf` is an EMF+ Dual file containing nested/enhanced metafile data.
- Recent fixes changed standalone EMF header sizing and EMF+ canvas origin handling.

## Tasks
1. Status: completed
   Next step: Done.
   Required context: The converter output had an opaque black-looking background at first; after nested WMF pattern-color handling the embedded chart rendered with white background, but remained too small in the upper-left.

2. Status: completed
   Next step: Done.
   Required context: `nested_wmf.emf` contains an EMF+ image object whose payload is a placeable WMF with two pad bytes before the WMF header, drawn via DrawImagePoints with source unit `UnitPixel`.

3. Status: completed
   Next step: Done.
   Required context: AWT/SVG now read DrawImagePoints `srcUnit`; `UnitPixel` uses the destination points directly instead of normalizing them by source dimensions.

4. Status: completed
   Next step: Done.
   Required context: Added AWT/SVG regression tests for pixel-unit DrawImagePoints scaling and a WMF parser test for truncated `EXTTEXTOUT` dx data.

5. Status: completed
   Next step: Done.
   Required context: Regenerated `etc/data/dst/nested_wmf.png` and `etc/data/dst/nested_wmf.svg`; dimensions match the reference at `3261x2357`.

6. Status: completed
   Next step: Done.
   Required context: `mvn -q test -Dtest=AwtGdiTest,SvgGdiTest,WmfGdiTest` passes. ImageMagick compare for `nested_wmf.png` is reduced from the original fuzz-10 AE about `7.47757e+06` to `455521`; remaining difference is dominated by raster/text antialiasing and low-resolution nested WMF rasterization.

7. Status: completed
   Next step: Move this file to `.tasks/done/00187_analyze_nested_wmf_difference.md`.
   Required context: Completion notes are appended below.

## Goals
- Explain why `nested_wmf` differs so much.
- Fix the converter if the issue is implementation-related.
- Verify the fix does not regress nearby samples.

## File List
- `.tasks/00187_analyze_nested_wmf_difference.md`
- `../wmf-testcase/data/src/nested_wmf.emf`
- `../wmf-testcase/data/png/nested_wmf.png`
- `etc/data/dst/nested_wmf.png`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfPlusParser.java`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Notes
- Cause 1: EMF+ DrawImagePoints contains `srcUnit = UnitPixel`; the converter always normalized destination points by source dimensions, shrinking the nested WMF into the upper-left corner.
- Cause 2: The embedded placeable WMF has two padding bytes before the standard WMF header and contains enhanced-metafile comments plus WMF fallback records. For fallback rendering, enhanced comments must be skipped rather than replayed over the fallback.
- Cause 3: The last WMF `EXTTEXTOUT` record is truncated in its dx array; Windows still draws the text. The parser now draws the text without dx when EOF occurs while reading dx.
- Fixes: Added placeable WMF handling for EMF+ metafile images in AWT/SVG, added `WmfParser(false)` for fallback-only parsing, honored `UnitPixel` DrawImagePoints placement, applied monochrome pattern brush colors, and tolerated truncated `EXTTEXTOUT` dx data.
- Verification: Targeted tests pass; regenerated `nested_wmf.png`/`.svg`; output size matches `3261x2357`; fuzz-10 AE improved from about `7.47757e+06` to `455521`.
- Remaining risk: PNG output still differs from the Windows reference in antialiasing and text/raster quality because the nested WMF is rasterized before final scaling in the AWT path. SVG keeps the nested content vectorized via an embedded SVG data URI.

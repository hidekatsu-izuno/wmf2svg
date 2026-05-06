# Fix 60677 PNG Rendering

## Purpose
Investigate why `../wmf-testcase/data/dst/60677.png` renders incorrectly while `../wmf-testcase/data/dst/60677.svg` appears correct, then fix the PNG rendering path.

## Context
- Test outputs are now written to `../wmf-testcase/data/dst`.
- `src/test/java/net/arnx/wmf2svg/MainTest.java` writes SVG through `SvgGdi` and PNG/JPEG through `AwtGdi`.
- The input file is `../wmf-testcase/data/src/60677.wmf`.
- Existing reference output exists at `../wmf-testcase/data/png/60677.png`.
- The working tree already contains unrelated user changes; preserve them.

## Tasks
- [x] Status: completed. Inspect `60677.wmf`, generated SVG/PNG, reference PNG, and debug GDI records to identify which operation differs in PNG output. Result: dimensions match, but generated PNG loses Japanese/full-width text; the file is a placeable WMF containing embedded EMF/EMF+ comments.
- [x] Status: completed. Locate the responsible implementation in the PNG/AWT path and compare it with the SVG path behavior. Result: PNG uses `AwtGdi`; SVG uses logical text advances and font sizes, while AWT PNG scales the output surface and must preserve GDI text metrics across that transform. Deferring EMF+ text was tested and rejected because it did not fix 60677 and regressed EMF+ text tests.
- [x] Status: completed. Apply a scoped fix and add or adjust focused tests that cover the failing behavior. Result: AWT `extTextOut` now normalizes DBCS `dx` arrays with `GdiUtils.fixTextDx`. Dual EMF+ comments are buffered until `GetDC`; if an unsupported or unsafe EMF+ record is found, the buffered EMF+ drawing is discarded and the GDI fallback remains active.
- [x] Status: completed. Verify with targeted Maven tests and regenerate/check `60677.png` in `../wmf-testcase/data/dst`. Result: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` and `mvn -q test` passed. Regenerated `../wmf-testcase/data/dst/60677.png`; it matches `/tmp/60677-buffered-final2.png` exactly by ImageMagick AE comparison.

## Goals
- `60677.png` should visually and structurally match the behavior represented by the correct SVG path.
- The fix should be covered by a focused regression test where practical.
- Verification results and any remaining limitations should be recorded here before moving this file to `.tasks/done/`.

## File List
- `.tasks/00202_fix_60677_png_rendering.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java` (likely)
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java` (possible)
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java` (likely)
- `../wmf-testcase/data/src/60677.wmf` (input, read-only)
- `../wmf-testcase/data/dst/60677.png` and `.svg` (generated outputs)
- `../wmf-testcase/data/png/60677.png` (reference output)

## Completion Summary
- Cause 1: the AWT PNG path passed raw byte-oriented `ExtTextOut` advances into decoded Java strings. For Shift-JIS text in `60677.wmf`, that made full-width labels and Japanese text use the wrong character positions.
- Cause 2: the file contains EMF+ Dual comments. SVG can remove fallback nodes after supported EMF+ draws, but AWT cannot remove pixels already painted by an imperfect EMF+ replay. The fix buffers Dual EMF+ comments until `GetDC`, replays them only when the buffered block is safe, and otherwise leaves the normal GDI fallback records to render.
- `60677.wmf` contains EMF+ `DrawPath` in the Dual stream; that path is not treated as safe for AWT fallback suppression, so the PNG follows the fallback output instead.
- Changed `AwtGdi` and added focused tests for Shift-JIS advance normalization, supported Dual replay, and unsupported Dual fallback behavior.
- Verification passed with targeted AWT tests and the full Maven test suite.

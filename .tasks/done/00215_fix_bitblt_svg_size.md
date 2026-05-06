# Fix bitblt SVG Size

## Purpose
Fix `bitblt.svg` rendering size so the SVG output matches the correct `bitblt.png` output.

## Context
- User reports `bitblt.svg` has an incorrect display size while `bitblt.png` is correct.
- The source and generated/reference files are in `../wmf-testcase/data`.
- The likely affected path is SVG bitmap placement for `BitBlt`/DIB bitmap handling; AWT output should remain unchanged.
- There are existing uncommitted changes in AWT/SVG from prior EMF+ fallback and custom cap fixes; preserve them.

## Tasks
- [x] Status: completed. Compare generated `bitblt.svg`, generated `bitblt.png`, and reference `bitblt.png` dimensions and embedded image geometry.
- [x] Status: completed. Trace `bitblt.wmf` bitmap record parameters into `SvgGdi` bitmap placement.
- [x] Status: completed. Update SVG bitmap sizing/placement consistently without changing correct AWT behavior.
- [x] Status: completed. Add or update focused regression tests for the `BitBlt` SVG size case.
- [x] Status: completed. Verify targeted tests and generated `bitblt.svg`.
- [x] Status: completed. Record decisions and move this task to done.

## Goals
- `bitblt.svg` displays bitmap content at the same destination size as `bitblt.png`.
- Existing SVG bitmap tests continue to pass.
- No regression to recent EMF+ fallback/custom cap work.

## File List
- `.tasks/00215_fix_bitblt_svg_size.md`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Summary
- The generated `bitblt.svg` had a default 330x460 root canvas even though the drawn content included a 700x600 background and bitmap grid.
- `getCanvasBounds` intentionally avoids expanding default canvases for small positive overflows, because text bounds can be estimated and too wide.
- Added a constrained positive-content canvas rule: when non-placeable content starts at the origin and exceeds the default canvas in both width and height, use the actual content bounds.
- Added a regression test that keeps the existing small text-overflow behavior and verifies a large bitmap drawing expands to 700x600.

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest#testNoPlaceableHeaderDoesNotExpandDefaultCanvasForPositiveOverflow+testNoPlaceableHeaderExpandsDefaultCanvasForLargePositiveBitmapContent test`
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
- `mvn -q test`
- Regenerated `../wmf-testcase/data/dst/bitblt.svg`; root is now `width="700" height="600" viewBox="0 0 700 600"`.

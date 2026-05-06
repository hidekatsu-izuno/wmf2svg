# Fix 60677 Arrow Tip Caps

## Purpose
Fix the 60677 arrow rendering so both PNG/AWT and SVG output show triangular arrow tips instead of line segments extending past the arrow and ending in square caps.

## Context
- User reports both `60677.png` and `60677.svg` have arrow tips with extra line extension and square ends.
- Reference data in `../wmf-testcase/data/png` shows triangular tips.
- Recent work changed EMF+ Dual replay and custom line cap handling in both AWT and SVG areas; preserve those fixes while correcting the arrow cap geometry.
- There are existing uncommitted SVG changes for Dual EMF+ fallback and custom line caps.

## Tasks
- [x] Status: completed. Inspect 60677 generated/reference outputs and identify the record/cap path causing the square tip.
- [x] Status: completed. Update AWT and SVG cap rendering consistently.
- [x] Status: completed. Add or update regression tests for the 60677-style arrow tip case.
- [x] Status: completed. Verify targeted tests and full tests.
- [x] Status: completed. Record decisions and move this task to done.

## Goals
- 60677 arrow tips render as triangles in both AWT PNG and SVG.
- No regression to existing EMF+ custom cap tests, Dual EMF+ fallback behavior, or previous 60677 fixes.

## File List
- `.tasks/00214_fix_60677_arrow_tip_caps.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Spec Notes
- Microsoft MS-EMFPLUS `EmfPlusCustomLineCapData` defines `BaseInset` as the distance between the beginning of the line cap and the end of the line.
- The current default custom cap parser reads the fill path but ignores `BaseInset`, so the stroked line reaches the cap endpoint/tip and remains visible through triangular caps.
- The fix should use `BaseInset` to shorten the stroked centerline before drawing custom caps, rather than guessing from the cap path geometry.

## Summary
- Parsed `BaseInset` and `WidthScale` from default EMF+ custom line cap data in both AWT and SVG renderers.
- Trimmed the stroked line/polyline by the scaled custom cap inset while keeping cap placement anchored to the original line endpoints.
- Updated default custom arrow cap regression tests so the line body stops at the cap base and does not draw through the triangular tip.
- Regenerated 60677 via the full test run; the SVG output now stops the line before the triangular arrow paths instead of extending to the tip.

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest#testEmfPlusDrawPathUsesDefaultCustomArrowEndCap test`
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest#testEmfPlusDrawPathUsesDefaultCustomArrowEndCap test`
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest,net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
- `mvn -q test`

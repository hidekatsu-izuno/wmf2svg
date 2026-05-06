# Fix 60677 AWT Extra Center Shapes

## Purpose
Fix the AWT-rendered `60677.png` so the center shapes that are absent in the reference PNG and SVG output are not drawn.

## Context
- User reports only the AWT `60677.png` shows extra center shapes.
- Reference PNG is `../wmf-testcase/data/png/60677.png`.
- Generated output is `../wmf-testcase/data/dst/60677.png` and `.svg`.
- Recent changes made AWT buffer Dual EMF+ and suppress GDI fallback only when renderable EMF+ records are present in the `GetDC` comment.
- Existing unrelated working tree changes must be preserved.

## Tasks
- [x] Status: completed. Compare AWT PNG, reference PNG, and SVG output to locate the extra center shapes.
- [x] Status: completed. Trace whether the extra shapes are GDI fallback after a buffered Dual EMF+ segment and identify the suppression boundary.
- [x] Status: completed. Apply a scoped AWT EMF+ coordinate scaling fix and update focused tests.
- [x] Status: completed. Verify with targeted tests, full tests, and regenerated 60677 outputs.

## Goals
- AWT `60677.png` no longer shows center shapes missing from reference/SVG.
- Dual EMF+ buffering remains EMF+ first and does not regress text/arrow rendering.
- Verification is recorded before moving this task file to `.tasks/done/`.

## File List
- `.tasks/00205_fix_60677_awt_extra_center_shapes.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`

## Completion Summary
- Cause: AWT EMF+ records were rendered in WMF/SVG logical coordinates while the PNG canvas used placeable/frame pixel dimensions, so the lower EMF+ shapes appeared halfway up the image as extra center shapes.
- Fix: AWT now applies EMF+ canvas scaling from placeable headers and EMF frame headers to points, sizes, driver-string transforms, and pen widths.
- Dual EMF+ handling: renderable Dual EMF+ records are buffered and replayed; plain fallback GDI is suppressed, while `GetDC` records are treated as explicit GDI interop and allowed after the buffered EMF+ replay.
- Verification: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` and `mvn -q test` passed. Regenerated `../wmf-testcase/data/dst/60677.png` and confirmed the extra center shapes are gone.

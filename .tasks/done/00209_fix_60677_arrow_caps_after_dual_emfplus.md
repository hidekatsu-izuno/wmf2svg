# Fix 60677 Arrow Caps After Dual EMF+ Change

## Purpose
Restore the start and end arrow symbols in `60677.png` while keeping Dual EMF+ fallback suppression consistent with the EMF+ first policy.

## Context
- User reports the arrow start/end symbols disappeared from `60677.png`.
- Recent AWT change replays pending Dual EMF+ comments before GDI fallback and suppresses the fallback outside `GetDC`.
- `60677.svg` had previously been corrected; this task focuses on AWT PNG behavior without reintroducing GDI fallback as the primary path.

## Tasks
- [x] Status: completed. Compared current `60677.png` with reference output and identified that arrow endpoint caps were absent while the line bodies remained.
- [x] Status: completed. Traced the EMF+ records around `60677` arrow drawing; the arrows use `DrawPath` with default custom line cap path data, followed by `GetDC`.
- [x] Status: completed. Adjusted AWT EMF+ line cap handling so default custom cap paths render through EMF+ itself, and cap coordinates are transformed consistently with the stroked line.
- [x] Status: completed. Added focused regression tests for dashed custom caps and default custom cap path data on `DrawPath`.
- [x] Status: completed. Verified with targeted tests, full tests, and regenerated `60677.png`.

## Goals
- `../wmf-testcase/data/dst/60677.png` includes the arrow start/end symbols.
- `image9.png` remains rendered via EMF+ rather than Dual GDI fallback.
- Existing `GetDC` interop behavior remains intact.

## File List
- `.tasks/00209_fix_60677_arrow_caps_after_dual_emfplus.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`

## Summary
- Fixed AWT EMF+ custom line cap rendering without restoring GDI fallback as the primary path.
- Added support for default custom line cap fill paths, in addition to the existing adjustable arrow support.
- Routed simple open `DrawPath` records with custom caps through the endpoint-cap drawing path.
- Converted cap endpoints to the same EMF+ logical coordinate space used by the stroked line; this was the immediate reason the 60677 caps appeared at the wrong half-scale position and were absent from the expected arrow endpoints.
- Regenerated `../wmf-testcase/data/dst/60677.png`; the start circle and end arrowheads are visible again.

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`

# Fix image9 PNG Rendering

## Purpose
Fix `image9.png` output while preserving the correct `image9.svg` behavior.

## Context
- User reports `image9.png` is rendered incorrectly, while `image9.svg` is correct.
- Reference PNG is expected at `../wmf-testcase/data/png/image9.png`.
- Current generated output is `../wmf-testcase/data/dst/image9.png` and `.svg`.
- Recent uncommitted changes touched AWT/SVG EMF+ fallback-state organization and must be preserved.

## Tasks
- [x] Status: completed. Compare reference PNG, generated PNG, and SVG output to identify the visible failure.
- [x] Status: completed. Trace the failing operation in the AWT renderer.
- [x] Status: completed. Apply a scoped AWT fix without changing correct SVG behavior.
- [x] Status: completed. Add or update focused tests.
- [x] Status: completed. Verify with targeted tests, full tests, and regenerated `image9.png`.

## Goals
- `../wmf-testcase/data/dst/image9.png` visually matches the reference PNG for the reported failure.
- `image9.svg` remains correct.
- Existing EMF+/fallback behavior for 60677 stays stable.

## File List
- `.tasks/00208_fix_image9_png_rendering.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java` or related AWT files as identified
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java` if needed

## Summary
- `image9.png` was blank because AWT buffered Dual EMF+ comments until footer while suppressing the following GDI fallback early; by footer, the EMF+ image replay no longer occurred at the right point in the nested EMF replay.
- Changed AWT Dual EMF+ handling to replay pending EMF+ comments immediately before GDI drawing fallbacks, then suppress the Dual GDI fallback outside `GetDC`.
- Added an AWT regression test covering `DrawImagePoints` bitmap replay before a `stretchDIBits` fallback.
- Verified with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` and `mvn -q test`.
- Regenerated `../wmf-testcase/data/dst/image9.png`.

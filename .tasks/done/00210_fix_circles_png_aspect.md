# Fix Circles PNG Aspect

## Purpose
Restore circular shapes in `Circles.png` so they match the reference output instead of appearing flattened.

## Context
- User reports the circles in `Circles.png` are crushed.
- Recent AWT EMF+ changes touched coordinate transforms for custom line caps, so verify whether this is a new regression or an older mapping/aspect issue.
- Focus on PNG/AWT output first, while preserving EMF+ direct rendering and existing fallback behavior.

## Tasks
- [x] Status: completed. Compared current `Circles.png` output with the reference image and identified that the blue circle group was vertically stretched.
- [x] Status: completed. Traced the source records and renderer path; the WMF wrapper defers an embedded EMF that sets `MM_ISOTROPIC`, `WindowExt=102x102`, `ViewportExt=102x81`, then draws arcs.
- [x] Status: completed. Fixed AWT pending-EMF footer replay so the outer WMF origin is kept while the embedded EMF viewport scale is still honored.
- [x] Status: completed. Added focused regression coverage for footer-mapped embedded EMF replay retaining the EMF viewport scale.
- [x] Status: completed. Verified with targeted tests, full tests, and regenerated `Circles.png`.

## Goals
- `../wmf-testcase/data/dst/Circles.png` shows round circles matching the reference aspect.
- Existing EMF+, Dual fallback, and recent `60677.png` arrow-cap behavior remain intact.

## File List
- `.tasks/00210_fix_circles_png_aspect.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`

## Summary
- Cause: `AwtGdi.flushPendingEmf()` used the WMF footer DC for embedded EMF replay and ignored the embedded EMF header mapping records. For `Circles.wmf`, that dropped the EMF's `ViewportExt(102,81)` scale, stretching the circle group vertically to about `418x505` instead of the reference `414x401`.
- Fix: Added an AWT DC mode that keeps the outer WMF origin as a post-scale offset during footer-mapped pending EMF replay. This lets the embedded EMF's own viewport/window scale affect the geometry while preserving the wrapper positioning.
- Regenerated `../wmf-testcase/data/dst/Circles.png`. The measured blue circle group is now `418x401`, matching the reference height (`414x401`) with only minor antialias/stroke-width differences.

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`

# Fix Symbols PNG Scale

## Purpose
Fix `Symbols.png` rendering so the output scale matches the reference image instead of appearing too small.

## Context
- User reports `Symbols.png` is displayed too small.
- Recent AWT changes adjusted pending embedded EMF footer replay mapping for `Circles.png`.
- The likely risk area is AWT coordinate mapping between outer WMF footer state and embedded EMF/WMF records.

## Tasks
- [x] Status: completed. Compared current `Symbols.png` with the reference image and measured rendered bounds.
- [x] Status: completed. Traced the source records and determined the small rendering came from placeable WMF footer replay of an embedded EMF using a 96dpi viewport.
- [x] Status: completed. Fixed AWT pending EMF replay so placeable footer-mapped EMF viewports are scaled to the target PNG DPI.
- [x] Status: completed. Added focused regression coverage.
- [x] Status: completed. Verified with targeted tests, full tests, and regenerated `Symbols.png`.

## Goals
- `../wmf-testcase/data/dst/Symbols.png` visually matches the reference scale.
- Existing pending EMF replay fixes remain intact.

## File List
- `.tasks/00211_fix_symbols_png_scale.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`

## Summary
- Cause: `Symbols.wmf` is a placeable WMF wrapper that replays an embedded EMF at footer time. The embedded EMF sets `ViewportExt(96,96)` and `WindowExt(2540,2540)`, representing a 96dpi EMF coordinate system. Because pending EMF replay has `replayingPendingEmf=true`, the placeable scaling path did not run, so the 96dpi viewport was drawn directly onto a 144dpi PNG canvas. The result was a 2/3-size symbol table.
- Fix: During footer-mapped pending EMF replay under a placeable WMF, scale EMF viewport extents from 96dpi to the target PNG DPI. This preserves the `Circles.png` embedded EMF scale fix while restoring placeable EMF output size.
- Regenerated `../wmf-testcase/data/dst/Symbols.png`.

## Verification
- Bounds before fix: reference `868x932`, generated `578x621`.
- Bounds after fix: reference `868x932`, generated `868x932`.
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`

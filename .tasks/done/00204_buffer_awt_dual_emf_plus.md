# Buffer AWT Dual EMF+

## Purpose
Unify Dual EMF+ handling so AWT buffers Dual EMF+ comments and uses EMF+ output when the buffered records can be handled, instead of always keeping GDI fallback.

## Context
- User requested not to keep GDI fallback for AWT solely because pixels cannot be removed afterward.
- Existing AWT logic buffers Dual EMF+ comments until `GetDC`, but a temporary change disabled fallback suppression.
- SVG can remove fallback nodes after EMF+ rendering; AWT must decide before allowing fallback pixels to draw.
- Existing unrelated working tree changes must be preserved.

## Tasks
- [x] Status: completed. Removed AWT's Dual EMF+ support-check/fallback preference and restored suppression after buffered EMF+ replay.
- [x] Status: completed. Updated tests so supported Dual EMF+ suppresses GDI fallback after buffered replay; non-rendering Dual comments do not suppress unrelated GDI.
- [x] Status: completed. Ran targeted AWT tests and full test suite.

## Goals
- Supported buffered Dual EMF+ records render as EMF+ in AWT and suppress subsequent GDI fallback.
- AWT no longer uses an "unsupported EMF+ record" decision to choose GDI fallback for Dual EMF+.
- Verification output is recorded here before completion.

## File List
- `.tasks/00204_buffer_awt_dual_emf_plus.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`

## Completion Summary
- Dual EMF+ comments in AWT are buffered through `GetDC`, replayed as EMF+, and renderable Dual EMF+ blocks suppress the following GDI fallback.
- Removed the `preferEmfPlusDualFallback` / unsupported-record path from AWT Dual EMF+ handling.
- Verification: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` passed; `mvn -q test` passed.

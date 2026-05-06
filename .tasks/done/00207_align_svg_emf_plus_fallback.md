# Align SVG EMF+ Fallback Semantics

## Purpose
Reflect the AWT EMF+ fallback semantics in `SvgGdi` while preserving SVG's DOM-based fallback removal model.

## Context
- User agreed to align SVG with the AWT fallback specification by semantics, not by copying AWT's pixel-oriented suppression implementation.
- AWT now routes fallback state through helper methods and treats `GetDC` as explicit GDI interop rather than ordinary fallback.
- SVG already supports removing fallback DOM nodes after EMF+ drawing succeeds, but its state transitions are named and structured differently.
- Existing uncommitted work includes AWT fallback helper unification and `.tasks/done/00206_unify_awt_emf_plus_fallback.md`.

## Tasks
- [x] Status: completed. Add SVG helper methods that mirror the AWT fallback state API names and responsibilities.
- [x] Status: completed. Refactor SVG EMF+ comment parsing, `GetDC` transitions, footer finalization, and fallback marking to use the unified helpers.
- [x] Status: completed. Confirm existing SVG/AWT tests cover the behavior or add focused tests if a gap appears.
- [x] Status: completed. Run targeted tests and full test suite.

## Goals
- SVG and AWT use the same conceptual fallback state transitions.
- SVG keeps DOM fallback removal rather than AWT-style pre-draw pixel suppression.
- `GetDC` remains explicit GDI interop and is not removed as ordinary fallback.
- Existing 60677 SVG and PNG behavior remains stable.

## File List
- `.tasks/00207_align_svg_emf_plus_fallback.md`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java` if needed

## Completion Summary
- Refactored `SvgGdi` EMF+ comment parsing and `GetDC` transitions through helper methods that mirror the AWT fallback-state API.
- Preserved SVG's DOM fallback removal model: supported fallback is recorded by keep-node and removed later, instead of pre-suppressing drawing like AWT.
- Kept existing detached metafile-image fallback behavior by allowing explicit detached fallback coverage marking.
- No new tests were needed; existing SVG fallback and `GetDC` tests covered the behavior.
- Verification: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`, `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest,net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`, and `mvn -q test` passed.

# Apply AWT Fallback Policy To SVG

## Purpose
Apply the AWT EMF+ fallback policy to `SvgGdi` so both renderers handle Dual EMF+ replay and GDI fallback consistently.

## Context
- AWT now buffers Dual EMF+ comments, replays them before GDI fallback drawing, suppresses fallback outside `GetDC`, and keeps `GetDC` interop.
- User asks to apply the AWT fallback specification to `SvgGdi`.
- Preserve recent AWT behavior for `image9.png`, `60677.png`, `Circles.png`, and `Symbols.png`; implement equivalent SVG behavior without unrelated refactors.

## Tasks
- [x] Status: completed. Compare AWT and SVG EMF+/Dual fallback state machines and identify mismatches.
- [x] Status: completed. Update `SvgGdi` to use the same replay/suppression rules where applicable.
- [x] Status: completed. Add or update SVG-focused regression coverage.
- [x] Status: completed. Verify targeted tests and full tests.
- [x] Status: completed. Record decisions and move this task to done.

## Goals
- `SvgGdi` matches AWT fallback semantics for Dual EMF+:
  - buffer Dual comments;
  - replay before GDI fallback drawing;
  - suppress fallback after successful EMF+ replay outside `GetDC`;
  - allow GDI interop inside `GetDC`.
- Existing AWT behavior remains unchanged.

## File List
- `.tasks/00212_apply_awt_fallback_policy_to_svg.md`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Summary
- Added Dual EMF+ buffering, replay, and fallback suppression state to `SvgGdi`.
- `SvgGdi` now replays pending Dual EMF+ comments before GDI fallback drawing and suppresses the fallback outside `GetDC`.
- `GetDC` interop remains allowed, so subsequent GDI records are preserved inside the interop section.
- Added SVG regression tests for supported Dual comments, bitmap replay before bitmap fallback, and `GetDC` interop.

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
- `mvn -q test`

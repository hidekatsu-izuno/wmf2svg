# Improve SvgGdi From AwtGdi

## Purpose
Use the AwtGdi implementation experience to identify and apply a low-risk SvgGdi improvement while preserving SVG-specific constraints.

## Context
- SvgGdi cannot perform exact destination-dependent XOR composition in general SVG output.
- SvgGdi should not depend on installed font files for correctness.
- Existing SvgGdi tests already cover approximate SRCINVERT image masking, text background estimation, EMF+ state, and placeable header sizing.
- AwtGdi has more complete raster/state behavior and can guide SvgGdi only where the behavior is representable in SVG or can be pre-rendered into embedded PNG data.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi behavior to select a bounded improvement that respects SVG constraints.
   Next step: inspect relevant raster, text, and state handling.
   Required context: avoid exact XOR composition unless pre-rendering source-only/pattern-only pixels; avoid font-file dependency.
2. Status: completed. Implement the selected SvgGdi improvement in the smallest relevant file set.
   Next step: edit SvgGdi/SvgDc/Svg tests only if needed.
   Required context: preserve existing SVG output conventions and compatibility mode.
3. Status: completed. Add or adjust tests that make the improvement observable.
   Next step: add focused SvgGdiTest coverage.
   Required context: prefer SVG string or embedded PNG assertions over environment-dependent rendering.
4. Status: completed. Run focused Maven tests.
   Next step: run SvgGdiTest and related tests if touched behavior is shared.
   Required context: record any unavailable verification.
5. Status: completed. Summarize decisions and verification, then move this file to `.tasks/done/00064_improve_svg_gdi_from_awt.md`.
   Next step: append completion notes after tests pass.
   Required context: include any limitations left intentionally unchanged.

## Goals
- Produce one concrete SvgGdi improvement inspired by AwtGdi.
- Keep SVG output independent of local font files.
- Do not claim exact SVG XOR where SVG cannot provide it.
- Verify with focused tests.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java` if raster/filter support is selected
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Selected a bounded raster ROP improvement from AwtGdi: when a BitBlt/DIB BitBlt has no source image, `MERGECOPY` collapses to black and `PATPAINT` collapses to white because the source term is zero.
- Implemented this by routing source-less `MERGECOPY` and `PATPAINT` through existing `BLACKNESS` and `WHITENESS` SVG filter paths.
- Did not add exact SVG XOR composition; existing approximate/filter behavior remains unchanged.
- Did not introduce any font-file dependency.
- Added SvgGdi tests for source-less `MERGECOPY` and `PATPAINT`.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`

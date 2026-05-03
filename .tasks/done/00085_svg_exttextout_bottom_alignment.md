# SvgGdi ExtTextOut Bottom Alignment

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make horizontal `extTextOut` `TA_BOTTOM` placement independent of optional clipping/opaque rectangles and avoid a null-rectangle failure in compatible mode.

## Context
- Existing local changes from tasks 00064 through 00084 are present and must be preserved.
- AwtGdi treats `x` / `y` as the text reference point and adjusts `TA_BOTTOM` from font metrics, not from the optional `rect`.
- SvgGdi currently uses `rect[3] - rect[1]` in the horizontal `TA_BOTTOM` compatible branch, which can throw when `rect == null` and makes text placement depend on an optional rectangle.
- SvgGdi has no font-file dependency by design, so the fix should continue using the existing font-size estimates.
- This does not attempt exact SVG XOR behavior or introduce font-file based metrics.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi horizontal `extTextOut` vertical alignment.
   Next step: update SvgGdi y placement.
   Required context: keep existing estimated font-size approach and text-anchor behavior.
2. Status: completed. Update non-vertical SvgGdi `extTextOut` `TA_BOTTOM` handling.
   Next step: add focused tests.
   Required context: compatible mode uses alphabetic baseline, non-compatible mode uses text-before-edge for top-like alignment.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: asserts compatible `TA_BOTTOM` with null rect emits estimated baseline y and non-compatible `TA_BOTTOM` uses the y reference point without requiring a rect.
4. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00085_svg_exttextout_bottom_alignment.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Avoid `extTextOut` `TA_BOTTOM` failure when `compatible == true` and `rect == null`.
- Make `TA_BOTTOM` placement follow the text reference point rather than the optional rectangle.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated horizontal SvgGdi `extTextOut` `TA_BOTTOM` placement so it no longer depends on the optional `rect`.
- Fixed the compatible-mode `TA_BOTTOM` null-rectangle failure by using the existing font-size ascent/descent estimates instead of `rect[3] - rect[1]`.
- Updated non-compatible `TA_BOTTOM` placement to use the text reference point with the existing estimated text height.
- Added SvgGdi tests for compatible and non-compatible `TA_BOTTOM` with `rect == null`.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.

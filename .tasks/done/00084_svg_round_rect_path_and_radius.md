# SvgGdi RoundRect Path And Radius

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make round rectangles use Awt/GDI-style arc dimensions and preserve round corners in current paths.

## Context
- Existing local changes from tasks 00064 through 00083 are present and must be preserved.
- AwtGdi uses `RoundRectangle2D` for `roundRect`, including when a current path is active.
- Awt/GDI `rw` / `rh` are the rounded-corner ellipse width and height, while SVG `<rect rx>` / `<rect ry>` are radii.
- SvgGdi currently writes `rx=rw` / `ry=rh` directly and turns current-path `roundRect` into a plain rectangle.
- This is geometry correction only; it does not alter SVG XOR/filter limitations or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `roundRect` handling.
   Next step: implement Awt-style rounded rectangle geometry in SvgGdi.
   Required context: preserve normal rectangle coordinate normalization.
2. Status: completed. Update direct SvgGdi `roundRect`.
   Next step: emit SVG `rx` / `ry` as half of transformed arc width/height, capped by half the rect size.
   Required context: keep class/fill/pattern behavior unchanged.
3. Status: completed. Update current-path SvgGdi `roundRect`.
   Next step: add rounded-rectangle commands to `SvgPath` instead of a plain rectangle.
   Required context: approximate quarter arcs with cubic Beziers, matching existing ellipse style.
4. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert direct radii use half arc dimensions and path roundRect emits Bezier commands.
   Required context: SVG string assertions are sufficient.
5. Status: completed. Run focused and full Maven tests.
   Next step: verification passed.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
6. Status: completed. Append completion summary and move to `.tasks/done/00084_svg_round_rect_path_and_radius.md`.
   Next step: move task file to done.
   Required context: SVG XOR/filter limitations and font-file independence were unchanged.

## Goals
- Match AwtGdi's rounded-corner geometry more closely.
- Preserve round corners when `roundRect` is recorded into a path.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated direct SvgGdi `roundRect` output so SVG `rx` / `ry` use half of the transformed GDI arc width/height, capped by half the normalized rectangle size.
- Updated current-path `roundRect` to append rounded-corner cubic Bezier commands instead of degrading to a plain rectangle.
- Added SvgGdi tests covering direct half-radius output and path round-rectangle Bezier output.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.

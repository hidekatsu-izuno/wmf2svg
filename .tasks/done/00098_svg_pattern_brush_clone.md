# SvgGdi Pattern Brush Clone

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make SVG pattern brushes own a defensive copy of their bitmap data.

## Context
- Existing local changes from tasks 00064 through 00097 are present and must be preserved.
- AwtPatternBrush clones bitmap data in its constructor and returns clones from `getPattern()`.
- AwtGdi also stores a clone for the last DIB pattern brush image used by monochrome WMF pattern brush fallback.
- SvgPatternBrush currently keeps the caller-provided byte array directly and returns it directly.
- This can make SVG output depend on later external mutation of the source byte array.
- This task should not affect SVG XOR behavior, font-file based measurement, or bitmap rendering semantics beyond defensive ownership.

## Tasks
1. Status: completed. Compare AwtGdi/AwtPatternBrush bitmap ownership with SvgGdi/SvgPatternBrush.
   Next step: update SvgPatternBrush and SvgGdi DIB pattern brush storage to clone image bytes.
   Required context: null image input should remain safe.
2. Status: completed. Implement defensive copying for SvgPatternBrush construction, `getPattern()`, and cached DIB pattern brush image.
   Next step: add focused tests.
   Required context: keep existing monochrome WMF pattern brush fallback behavior.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: mutate original bitmap arrays after brush creation and verify the brush copy is stable.
4. Status: completed. Run focused and full Maven tests.
   Next step: append completion summary.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00098_svg_pattern_brush_clone.md`.
   Next step: finish.
   Required context: mention preserved XOR and font-file constraints.

## Goals
- Prevent caller mutation from altering SvgPatternBrush bitmap contents.
- Match AwtPatternBrush defensive-copy behavior.
- Preserve existing pattern brush rendering behavior.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgPatternBrush.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated `SvgPatternBrush` to clone bitmap data on construction and return clones from `getPattern()`.
- Updated SvgGdi `dibCreatePatternBrush()` to store a defensive copy for monochrome WMF pattern brush fallback.
- Added focused SvgGdi tests covering direct pattern brush ownership and cached DIB pattern brush ownership.
- Preserved existing pattern brush rendering behavior.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.

# SvgGdi Color Space Clone

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: make SVG color space objects defensively own their LOGCOLORSPACE bytes.

## Context
- Existing local changes from tasks 00064 through 00099 are present and must be preserved.
- AwtColorSpace clones LOGCOLORSPACE bytes in its constructor and returns clones from `getLogColorSpace()`.
- SvgColorSpace currently keeps the caller-provided byte array directly and returns it directly.
- This can make color space bookkeeping depend on later external mutation.
- This task should not affect SVG XOR behavior, font-file based measurement, or rendered output paths.

## Tasks
1. Status: completed. Compare AwtColorSpace defensive-copy behavior with SvgColorSpace.
   Next step: update SvgColorSpace.
   Required context: null input should behave as an empty byte array.
2. Status: completed. Implement defensive copying in SvgColorSpace constructor and getter.
   Next step: add focused tests.
   Required context: keep existing `deleteColorSpace` and `setColorSpace` behavior.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: run focused and full Maven tests.
   Required context: mutate original and returned byte arrays and verify stored bytes are stable.
4. Status: completed. Run focused and full Maven tests.
   Next step: append completion summary.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test` both passed.
5. Status: completed. Append completion summary and move to `.tasks/done/00100_svg_color_space_clone.md`.
   Next step: finish.
   Required context: mention preserved XOR and font-file constraints.

## Goals
- Prevent caller mutation from altering SvgColorSpace LOGCOLORSPACE bytes.
- Match AwtColorSpace defensive-copy behavior.
- Keep color space object selection/deletion semantics unchanged.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgColorSpace.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Updated `SvgColorSpace` to clone LOGCOLORSPACE bytes on construction and return clones from `getLogColorSpace()`.
- Added focused SvgGdi tests for original-array mutation, returned-array mutation, and null LOGCOLORSPACE input.
- Kept color space selection/deletion semantics unchanged.
- Preserved existing SVG constraints: no exact XOR emulation was introduced and no font-file dependency was added.
- Verification passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.

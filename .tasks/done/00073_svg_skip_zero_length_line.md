# SvgGdi Skip Zero Length Line

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: skip zero-length `LineTo` output.

## Context
- Existing local changes from tasks 00064 through 00072 are present and must be preserved.
- AwtGdi returns immediately when `lineTo` is called with the current point.
- SvgGdi currently emits a zero-length `<line>` element.
- This is SVG geometry cleanup only; it does not alter XOR/filter limitations or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi `lineTo` behavior.
   Next step: add the same early return to SvgGdi.
   Required context: keep normal line and path behavior unchanged.
2. Status: completed. Update SvgGdi `lineTo`.
   Next step: return when endpoint equals current point.
   Required context: match AwtGdi by checking before currentPath handling.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert zero-length line does not emit a `<line>` element.
   Required context: use SVG string assertions.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00073_svg_skip_zero_length_line.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Avoid redundant zero-length SVG line elements.
- Match AwtGdi `lineTo` behavior.
- Keep the change small and verified.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's zero-length `lineTo` behavior in SvgGdi.
- Added an early return when the line endpoint equals the current point.
- Added a SvgGdi test verifying no `<line>` element is emitted for zero-length lines.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`

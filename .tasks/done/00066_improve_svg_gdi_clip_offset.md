# Improve SvgGdi Clip Offset From AwtGdi

## Purpose
Apply another AwtGdi-informed SvgGdi improvement for clip-region offset state.

## Context
- Existing local changes from tasks 00064 and 00065 are present and must be preserved.
- AwtGdi offsets the current clip by translating the existing Graphics2D clip, so repeated `OffsetClipRgn` calls accumulate.
- SvgGdi represents clip regions as SVG masks and currently stores clip offset state in SvgDc.
- This change must not expand exact XOR composition claims and must not introduce font-file dependencies.

## Tasks
1. Status: completed. Identify a bounded AwtGdi/SvgGdi behavior gap.
   Next step: use clip offset accumulation.
   Required context: only adjust SVG mask offset state.
2. Status: completed. Implement cumulative `OffsetClipRgn` state in SvgDc.
   Next step: update `SvgDc.offsetClipRgn`.
   Required context: preserve existing mask cloning and group creation in SvgGdi.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert repeated clip offsets produce cumulative mask transform.
   Required context: use SVG string assertions, not renderer-dependent checks.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00066_improve_svg_gdi_clip_offset.md`.
   Next step: finalize after verification.
   Required context: mention SVG constraints intentionally unchanged.

## Goals
- Make repeated `OffsetClipRgn` calls cumulative in SvgGdi, matching AwtGdi behavior.
- Keep the change small and covered by tests.
- Preserve SVG XOR and font-file constraints.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched SvgDc clip offset state to AwtGdi/Awt Graphics2D behavior: repeated `OffsetClipRgn` calls now accumulate.
- Added a SvgGdi test that applies two clip offsets and verifies the SVG mask transform uses the cumulative translation.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`

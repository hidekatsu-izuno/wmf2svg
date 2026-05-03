# SvgGdi Normalize Arc Radii

## Purpose
Apply another AwtGdi-informed SvgGdi improvement: choose and emit arc radii from transformed dimensions.

## Context
- Existing local changes from tasks 00064 through 00082 are present and must be preserved.
- AwtGdi creates arcs from transformed rectangle frames, so anisotropic mapping turns a logical circle into a rendered ellipse.
- SvgGdi currently chooses `<circle>` when logical `rx == ry`, and writes SVG arc radii directly from signed relative transforms.
- This can produce the wrong shape under anisotropic mapping and invalid negative arc radii under flipped mapping.
- This is geometry normalization only; it does not alter SVG XOR/filter limitations or font handling.

## Tasks
1. Status: completed. Compare AwtGdi and SvgGdi arc radius handling.
   Next step: update SvgGdi arc/chord/pie radius output.
   Required context: preserve arc direction and path endpoints.
2. Status: completed. Update SvgGdi `arc`, `chord`, and `pie`.
   Next step: use absolute transformed radii and choose circle only when transformed radii are equal.
   Required context: keep logical angle calculations unchanged.
3. Status: completed. Add focused SvgGdiTest coverage.
   Next step: assert anisotropic full-circle arc outputs an ellipse with transformed radii, not a circle.
   Required context: SVG string assertions are sufficient.
4. Status: completed. Run focused and full Maven tests.
   Next step: run SvgGdiTest, then `mvn -q test`.
   Required context: record results.
5. Status: completed. Append completion summary and move to `.tasks/done/00083_svg_normalize_arc_radii.md`.
   Next step: finalize after verification.
   Required context: mention unchanged SVG constraints.

## Goals
- Match AwtGdi's transformed-frame behavior for full-circle arc/chord/pie output.
- Avoid invalid negative SVG arc radii under flipped mapping.
- Preserve XOR constraints and font-file independence.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
- Matched AwtGdi's transformed-frame behavior for `arc`, `chord`, and `pie` radii.
- Full-circle arc/chord/pie output now chooses `<circle>` only when transformed radii are equal.
- SVG arc radii are now emitted as positive values under flipped mapping.
- Added SvgGdi coverage for anisotropic full-circle arc output.
- Did not change SVG XOR/filter limitations or introduce font-file dependencies.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`
  - `mvn -q test`

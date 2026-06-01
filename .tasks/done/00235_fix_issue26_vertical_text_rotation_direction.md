# 00235 Fix Issue 26 Vertical Text Rotation Direction

## Purpose
Fix the rotation direction of the vertical text restored for `image4.emf`.

## Context
The previous fix restored rotation for `image4.emf` text by mapping EMF world-transform rotation into GDI font escapement. The user reports the text is rotated 180 degrees opposite from the expected vertical direction. `SvgGdi` emits SVG text rotation as `rotate(-escapement / 10)`, so the parser-side escapement sign must match that convention.

Current worktree also includes the previous issue #26 pen-width fix and vertical-text fix; preserve those changes.

## Tasks
1. Status: completed
   Next step: Corrected the parser-side world-transform-to-escapement sign.
   Required context: For `image4.emf`, the affected text has a -90 degree world transform; generated SVG should rotate -90 degrees, not +90 degrees.

2. Status: completed
   Next step: Updated the regression test to assert the expected negative rotation direction.
   Required context: `EmfParserTest.testExtTextOutWUsesWorldTransformRotationForTextEscapement`.

3. Status: completed
   Next step: Ran targeted tests and reconverted `image4.emf`.
   Required context: `mvn -q -Dtest=EmfParserTest#testExtTextOutWUsesWorldTransformRotationForTextEscapement test` passed. `mvn -q -Dtest=EmfParserTest#testExtTextOutWUsesWorldTransformRotationForTextEscapement+testExtTextOutWKeepsUnicodeDxPerCharacter test` passed. `mvn -q -DskipTests package` passed. Reconverted `image4.emf`; 7 labels have `rotate(-90.0, ...)`, no `stroke-width="9525"` remains, and max stroke width is about 3.12.

4. Status: completed
   Next step: Move this file to `.tasks/done/00235_fix_issue26_vertical_text_rotation_direction.md`.

## Goals
- `image4.emf` vertical labels rotate in the correct direction.
- Regression test checks the sign explicitly.

## File List
- `.tasks/00235_fix_issue26_vertical_text_rotation_direction.md`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java`
- `src/test/java/net/arnx/wmf2svg/gdi/emf/EmfParserTest.java`

## Completion Summary
`SvgGdi` applies font escapement as `rotate(-escapement / 10)`. The previous parser bridge used the raw world-transform angle as escapement, which made `image4.emf` labels rotate `+90` instead of `-90`. Reversed the parser-side sign and updated the regression test to assert `rotate(-90.0, ...)`.

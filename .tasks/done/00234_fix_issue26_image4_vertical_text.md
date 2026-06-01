# 00234 Fix Issue 26 Image4 Vertical Text

## Purpose
Fix `image4.emf` conversion so EMF+ vertical text is reflected in the generated SVG.

## Context
After fixing issue #26's black-rectangle symptom, the user reported that a vertical-writing text section in `image4.emf` is not reflected. The downloaded issue files are available in `/tmp/issue26_emfs/` and were copied to `../wmf-testcase/data/src/`.

Current worktree already contains the previous issue #26 EMF+ pen-width fix in `SvgGdi`, `AwtGdi`, `SvgGdiTest`, and `.tasks/done/00233...`; keep those changes intact.

## Tasks
1. Status: completed
   Next step: Inspected generated SVG and EMF records from `image4.emf` to identify how the vertical text is encoded.
   Required context: The affected labels are regular EMF `EXTTEXTOUTW` records, not EMF+ DriverString records. Their selected font is Calibri with escapement 0, while the current world transform is a -90 degree rotation.

2. Status: completed
   Next step: Implemented a parser fix that maps world-transform text rotation to existing GDI text escapement.
   Required context: `EmfParser` now tracks selected fonts across select/save/restore/delete, creates a temporary transformed font for rotated text output, calls `extTextOut`, and restores the original selected font.

3. Status: completed
   Next step: Added a focused synthetic regression test.
   Required context: `EmfParserTest` now builds a small EMF with `SETWORLDTRANSFORM` rotation and `EXTTEXTOUTW`, then verifies the SVG has `transform="rotate(90.0, ...)`.

4. Status: completed
   Next step: Ran targeted tests, full Maven tests, and reconverted `image4.emf`.
   Required context: `mvn -q -Dtest=EmfParserTest#testExtTextOutWUsesWorldTransformRotationForTextEscapement+testExtTextOutWKeepsUnicodeDxPerCharacter test` passed. `mvn -q test` passed. Reconverted `image4.emf`; 7 labels have `transform="rotate(90.0, ...)`, and no `stroke-width="9525"` remains. Max stroke width is about 3.12.

5. Status: completed
   Next step: Move this file to `.tasks/done/00234_fix_issue26_image4_vertical_text.md`.
   Required context: Verification is complete.

## Goals
- `image4.emf` generated SVG represents the vertical-writing section.
- Regression coverage prevents losing the EMF+ vertical text signal.
- Existing issue #26 pen-width fix remains valid.

## File List
- `.tasks/00234_fix_issue26_image4_vertical_text.md`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java`
- `src/test/java/net/arnx/wmf2svg/gdi/emf/EmfParserTest.java`

## Completion Summary
`image4.emf` encodes the vertical-looking labels as normal EMF `EXTTEXTOUTW` text under a -90 degree world transform. The font itself has no `@` vertical face name and escapement is 0, so the previous SVG text path emitted the already-transformed baseline point but lost the orientation.

The parser now tracks the selected font and, when text is emitted under a rotated world transform, creates a temporary font with escapement/orientation adjusted by the transform angle. This reuses existing SVG/AWT text rotation handling and avoids file-specific logic.

Verification confirmed the affected labels now receive SVG `rotate(90.0, ...)` transforms, while the previous issue #26 pen-width fix remains intact.

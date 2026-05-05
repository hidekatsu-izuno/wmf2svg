# Investigate Circles Output

## Purpose
Investigate why both `Circles.png` and `Circles.svg` render incorrectly, identify whether the problem is in WMF parsing, shared GDI state, or output-specific rendering, and apply a focused fix.

## Context
- Input file: `../wmf-testcase/data/src/Circles.wmf`
- Existing outputs:
  - `etc/data/dst/Circles.png`
  - `etc/data/dst/Circles.svg`
  - reference PNG: `../wmf-testcase/data/png/Circles.png`
- Both PNG and SVG are reported wrong, so the issue may be shared parsing/state rather than SVG-only styling.
- Existing uncommitted changes for previous `2264z` and `ant` work must be preserved.

## Tasks
1. Status: completed
   Next step: Inspected current `Circles.svg`, output dimensions, and debug GDI command sequence.
   Required context: `Circles.wmf` is a non-placeable WMF wrapper with one embedded EMF comment; the SVG contains frame, text, and arc geometry with viewBox `-3263 -2841 6529 5685`.

2. Status: completed
   Next step: Compared generated PNG against reference at a coarse level.
   Required context: The PNG dimensions match, but the generated PNG bounds only include the right-side blue ellipse area; reference also includes left frame/text. Pixel difference is 7228.

3. Status: completed
   Next step: Traced the suspicious behavior to parser/shared GDI/AWT/SVG implementation.
   Required context: AWT replay uses the footer mapping but also clips to the embedded EMF header bounds. `Circles.wmf` has an EMF header bounds rectangle narrower than actual drawing, so frame/text are clipped from PNG. SVG does not apply this clip and retains the content.

4. Status: completed
   Next step: Restricted AWT pending EMF header clipping so footer-mapped wrappers are not clipped to narrow EMF header bounds.
   Required context: `AwtGdi.flushPendingEmf()` now applies the embedded EMF header clip only when replaying with a captured explicit DC.

5. Status: completed
   Next step: Added regression coverage.
   Required context: `AwtGdiTest.testPendingEmfFooterMappingDoesNotClipToEmfHeaderBounds` verifies footer-mapped pending EMF content can draw outside stale EMF header bounds.

6. Status: completed
   Next step: Verified with targeted tests, regenerated `Circles.png`/`Circles.svg`, and broader Maven tests.
   Required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`, `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest,net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`, and `mvn -q test` passed.

## Goals
- Determine the concrete cause of wrong `Circles` rendering.
- Fix both PNG and SVG if the root cause is shared.
- Verify with tests and regenerated outputs.
- Append completion notes and move this file to `.tasks/done/00179_investigate_circles_outputs.md`.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/wmf/WmfParser.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/wmf/WmfGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`
- `etc/data/dst/Circles.png`
- `etc/data/dst/Circles.svg`
- `../wmf-testcase/data/src/Circles.wmf`

## Completion Summary
- Cause: AWT pending EMF replay used the WMF footer mapping but also clipped to the embedded EMF header bounds. `Circles.wmf` has an EMF header bounds rectangle that does not cover all actual drawing, so PNG output lost surrounding content.
- Fix: `AwtGdi.flushPendingEmf()` now only applies the EMF header clip for captured explicit-DC replay, not for footer-mapped wrapper replay.
- Regression: Added a focused AWT test using an EMF payload whose header bounds are intentionally narrower than its line content.
- Verification: Regenerated `etc/data/dst/Circles.svg` and `etc/data/dst/Circles.png`; all targeted and full Maven tests passed.

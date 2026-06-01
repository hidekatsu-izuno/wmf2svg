# 00233 Fix Issue 26 EMF Black Rectangle

## Purpose
Fix GitHub issue #26 where converting attached EMF files produces SVG output that displays as a black rectangle.

## Context
Issue #26 reports that `image4.emf` and `image27.emf`, attached in `emfs_having_the_problem.zip`, convert successfully with wmf2svg 0.10.4 but the resulting SVG is fully black in Inkscape, Chrome, and Firefox. The reporter also included black SVG outputs and correct PNG renditions.

User requested the same safety-first handling as before: analyze the included EMF files first and confirm they do not have virus-like behavior before using them as regression inputs.

## Tasks
1. Status: completed
   Next step: Downloaded the issue attachment to `/tmp/issue26_emfs.zip` and inspected archive metadata without extracting into the repository.
   Required context: Attachment URL is `https://github.com/user-attachments/files/28419460/emfs_having_the_problem.zip`.

2. Status: completed
   Next step: Statically analyzed the included EMF files as binary metafiles before rendering or converting them.
   Required context: ZIP SHA-256 `267acf8bb066d4698dff42fcbc14ae823a88f446dfbd5e7afe9eff02fa81116d`; entries are `image4.emf`, `image4.png`, `image4.svg`, `image27.emf`, `image27.png`, `image27.svg`. `image4.emf` SHA-256 `0697a22003d3c0bdc84bb761cc9471c283ffc9a59ea5f89446d11b201f5b086f`, size 26952, EMF signature `0x464d4520`, declared bytes 26952, records 616. `image27.emf` SHA-256 `6463530f5c947b816a8a87fd6fce9d2b7ce662e80ff005688ff8d4f30d32f530`, size 54688, EMF signature `0x464d4520`, declared bytes 54688, records 905. Both parsed to EOF without bad record sizes. No `MZ`, `PE`, URL, script, shell, PowerShell, command, executable path, or macro-like tokens were found in ASCII string scans.

3. Status: completed
   Next step: Reproduced the black-rectangle conversion with the current code and identified the responsible EMF+ pen width handling.
   Required context: Current generated SVGs matched the reporter-provided black SVGs. They contained EMF+ outline paths with `stroke-width="9525"`, making the SVG look like a black rectangle.

4. Status: completed
   Next step: Implemented the smallest design-consistent SVG/AWT EMF+ fix.
   Required context: EMF+ pen widths in World units must be scaled by the current EMF+ world/page transform at draw time. Existing pen-object transform scaling is preserved. Updated `SvgGdi` and `AwtGdi`.

5. Status: completed
   Next step: Added focused regression tests without adding binary fixtures.
   Required context: Added a synthetic EMF+ SVG regression test for a large World-unit pen width under a small world transform. Existing pen transform test remains covered.

6. Status: completed
   Next step: Ran Maven tests and targeted issue conversions.
   Required context: `mvn -q -Dtest=SvgGdiTest#testEmfPlusPenTransformScalesStrokeAndDashPattern+testEmfPlusWorldTransformScalesWorldUnitPenWidth test` passed. `mvn -q -Dtest=AwtGdiTest#testEmfPlusPenTransformScalesStrokeWidth test` passed. `mvn -q test` passed. `mvn -q -DskipTests package` passed. Reconverted `image4.emf` and `image27.emf`; no `stroke-width="9525"` remains, max stroke widths are about 3.12 and 3.08 respectively.

7. Status: completed
   Next step: Move this file to `.tasks/done/00233_fix_issue26_emf_black_rectangle.md`.
   Required context: Implementation and verification are complete.

## Goals
- Establish that the attached EMF files are ordinary EMF data and show no virus-like behavior before processing.
- Convert the issue EMFs to SVG without the full black rectangle regression.
- Add regression coverage for the failing record path.
- Leave the repository in a verifiable state with documented commands and results.

## File List
- `.tasks/00233_fix_issue26_emf_black_rectangle.md`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Summary
Static analysis found the issue attachment contains only two EMF files, two PNG references, and two SVG outputs. Both EMFs have valid EMF headers, declared sizes matching actual sizes, clean record traversal to EOF, and no executable signatures, scripts, URLs, shell commands, PowerShell strings, or macro-like tokens in ASCII scans.

The bug was in EMF+ pen width scaling. The attached EMFs store outline pens with width `9525` in World units and draw them under a world transform near `0.000327`, so the actual stroke should be about 3 pixels. The SVG/AWT renderers were scaling EMF+ pen widths only from the pen object's transform, not the current world/page transform, so SVG emitted `stroke-width="9525"` and covered the image in black.

Updated SVG and AWT EMF+ pen width scaling to include the current world transform and page transform, while preserving existing pen-object transform scaling. Added a synthetic SVG regression test for World-unit pen widths under a small world transform.

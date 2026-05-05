# Fix PNG Dimension Mismatches

## Purpose
Investigate and fix PNG dimension mismatches between `etc/data/dst` and `../wmf-testcase/data/png`, starting with the five known mismatches.

## Context
- User asked to handle size mismatches first.
- Known mismatches from task 00185:
  - `github_apache_poi_vector_image.png`: generated `706x578`, reference `708x581`.
  - `github_docx4j_gradient.png`: generated `1096x783`, reference `1095x783`.
  - `github_python_pptx_pic.png`: generated `1632x1056`, reference `1636x1060`.
  - `github_xceed_docx_signature_line.png`: generated `259x131`, reference `257x129`.
  - `nested_wmf.png`: generated `3345x2444`, reference `3261x2357`.
- Must determine causes and confirm fixes do not regress other images.

## Tasks
1. Status: completed
   Next step: Done.
   Required context: Inspected the five mismatches and queried GDI+ `MetafileHeader` bounds, type, DPI, frame/device/mm values. The mismatches came from standalone EMF header sizing, not content growth.

2. Status: completed
   Next step: Done.
   Required context: Root cause was use of EMF `bounds`/raw `frame` extent rather than GDI+ frame-to-pixel sizing. EMF+ files also require zero canvas origin from the frame-derived canvas.

3. Status: completed
   Next step: Done.
   Required context: Added 12-argument `emfHeader` with device/mm values, frame-to-pixel calculation, AWT/SVG canvas sizing, and EMF+ header canvas switch.

4. Status: completed
   Next step: Done.
   Required context: Updated existing nonzero-bounds/header tests and added EMF+ frame-pixel/zero-origin tests.

5. Status: completed
   Next step: Done.
   Required context: Regenerated the five known mismatches; all dimensions now match references.

6. Status: completed
   Next step: Done.
   Required context: Regenerated all outputs through `MainTest#testMain`; full dimension comparison reports `dimension_mismatches=0 missing=0`. Targeted tests pass.

7. Status: completed
   Next step: Move this file to `.tasks/done/00186_fix_png_dimension_mismatches.md`.
   Required context: Include causes, fixes, tests, and remaining risks.

## Goals
- Resolve dimension mismatches where implementation is wrong.
- Explain any intentional/unfixable mismatch with evidence.
- Verify no broader image-size regressions.

## File List
- `.tasks/00186_fix_png_dimension_mismatches.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/emf/EmfParserTest.java`
- `etc/data/dst/*.png`
- `../wmf-testcase/data/png/*.png`

## Completion Notes
- Cause: GDI+ derives EMF image dimensions from the EMF header frame rectangle converted through `szlDevice/szlMillimeters`, with an inclusive `+1` pixel behavior. The converter only used the header bounds or raw frame/bounds relationship, which caused small off-by-one/scale mismatches and a large mismatch for high-DPI EMF+ data.
- EMF+ handling: for EMF+ comments, GDI+ reports a zero-origin frame-derived canvas even when EMF bounds are nonzero or larger in a different coordinate space. AWT and SVG now switch to the frame-derived zero-origin canvas before parsing EMF+ comments.
- Non-EMF+ handling: AWT and SVG preserve nonzero EMF bounds origins, but use the larger of bounds size and frame-derived pixel size. This keeps `github_closedxml_sample_image` behavior while fixing size drift.
- Validation:
  - `mvn -q test -Dtest=AwtGdiTest,SvgGdiTest,EmfParserTest`
  - `mvn -q test -Dtest=MainTest#testMain`
  - Regenerated five known mismatches: all matched reference dimensions.
  - Compared all `etc/data/dst/*.png` against `../wmf-testcase/data/png/*.png`: `dimension_mismatches=0 missing=0`.
- Remaining risk: visual antialias/color differences are still outside this task; this only resolves image dimensions and associated EMF header origin behavior.

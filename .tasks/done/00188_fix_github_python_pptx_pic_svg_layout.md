# Fix github_python_pptx_pic SVG Layout

## Purpose
Analyze why `etc/data/dst/github_python_pptx_pic.svg` has incorrect drawing position or size, then fix the converter if the issue is implementation-related.

## Context
- User reported that `github_python_pptx_pic.svg` appears to have wrong drawing position and size.
- Source file is `../wmf-testcase/data/src/github_python_pptx_pic.emf`.
- Existing rendered outputs are `etc/data/dst/github_python_pptx_pic.svg` and `etc/data/dst/github_python_pptx_pic.png`; Windows/reference PNG is `../wmf-testcase/data/png/github_python_pptx_pic.png`.
- Recent changes touched EMF/EMF+ canvas sizing, nested metafile handling, and DrawImagePoints unit handling. This task must not revert unrelated dirty work.

## Tasks
1. Status: completed
   Next step: Done. Inspected the SVG/PNG dimensions, viewBox, image transforms, and visual output.
   Required context: Determine whether the SVG is shifted, clipped, scaled, or using wrong source/destination units.

2. Status: completed
   Next step: Done. Inspected EMF/EMF+ records in `github_python_pptx_pic.emf`.
   Required context: Identify whether the file uses EMF+ bitmap/metafile image drawing, header bounds/frame, page transform, or world transform.

3. Status: completed
   Next step: Done. Implemented the converter fix.
   Required context: Keep AWT and SVG behavior consistent where the same EMF+ semantics apply.

4. Status: completed
   Next step: Done. Added a focused regression test.
   Required context: Prefer deterministic small EMF+ image placement tests.

5. Status: completed
   Next step: Done. Regenerated `github_python_pptx_pic.svg` and `.png`, then compared dimensions and visual behavior.
   Required context: Confirm the fix improves this sample without regressing nearby EMF+ image samples.

6. Status: completed
   Next step: Done. Ran targeted tests and updated completion notes.
   Required context: Include cause, fix, verification, and remaining risks.

7. Status: completed
   Next step: Move this file to `.tasks/done/00188_fix_github_python_pptx_pic_svg_layout.md`.
   Required context: Only after verification is complete.

## Goals
- Explain why the SVG layout is wrong.
- Produce corrected SVG placement/size behavior.
- Verify the fix with tests and regenerated outputs.

## File List
- `.tasks/00188_fix_github_python_pptx_pic_svg_layout.md`
- `../wmf-testcase/data/src/github_python_pptx_pic.emf`
- `../wmf-testcase/data/png/github_python_pptx_pic.png`
- `etc/data/dst/github_python_pptx_pic.svg`
- `etc/data/dst/github_python_pptx_pic.png`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Completion Notes
- Cause: EMF+ metafile images stored the embedded EMF object bounds as pending parent EMF bounds, so the child EMF's negative-origin canvas expanded the outer SVG. The embedded SVG data URI also kept the child EMF header viewBox instead of the DrawImagePoints source rectangle, which made the displayed image shift and shrink inside the parent image element.
- Fix: `SvgGdi` no longer adds EMF+ image object bounds to the parent pending bounds, and nested metafile SVG data URIs are forced to the EMF+ image source rectangle canvas when drawn through EMF+ image records.
- Regression: Added `testEmfPlusMetafileImageUsesSourceRectCanvas`.
- Verification: `mvn -q test -Dtest=SvgGdiTest,AwtGdiTest,WmfGdiTest` passed.
- Output check: regenerated `etc/data/dst/github_python_pptx_pic.svg` and `.png`; SVG is now `width="1636" height="1060" viewBox="0 0 1636 1060"`, and the embedded SVG image is `width="577.283447265625" height="432.99993896484375" viewBox="0 0 577.283447265625 432.99993896484375"`.
- PNG check: regenerated PNG remains `1636x1060`; comparison with `../wmf-testcase/data/png/github_python_pptx_pic.png` at `-fuzz 10%` reported `39952`, the same known antialias/rendering-engine-level difference observed before this fix.

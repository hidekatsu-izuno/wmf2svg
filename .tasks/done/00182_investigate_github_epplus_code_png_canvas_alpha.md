# Investigate github_epplus_code PNG Canvas And Alpha

## Purpose
Investigate why `github_epplus_code.png` has an incorrect drawing area and appears not to have a transparent background, then apply a focused PNG/AWT fix if needed.

## Context
- Input file is expected to be `../wmf-testcase/data/src/github_epplus_code.emf`.
- Existing output is expected under `etc/data/dst/github_epplus_code.png`.
- The reported problem is PNG-specific: drawing area and background alpha.
- Preserve existing uncommitted changes from previous tasks.

## Tasks
1. Status: completed
   Next step: Inspected input/output files, PNG geometry, alpha histogram, and reference PNG.
   Required context: Determine whether the image is standalone EMF or wrapped WMF, and whether background is fully opaque, partially opaque, or transparent.

2. Status: completed
   Next step: Traced the EMF command sequence for header bounds/frame, map mode, window/viewport records, fill/background records, and image/canvas growth.
   Required context: Identify which records determine the expected drawing area and whether any clear/fill operation makes the background opaque.

3. Status: completed
   Next step: Compared AWT canvas bounds/transparent background handling with SVG or Windows-rendered reference.
   Required context: Isolate whether the issue is canvas sizing, canvas min origin, opaque background, or EMF background fill handling.

4. Status: completed
   Next step: Implemented a focused fix.
   Required context: Avoid sample-specific logic and preserve previous PNG fixes for `Circles` and EMF/WMF rendering.

5. Status: completed
   Next step: Added regression coverage for standalone EMF header canvas and transparent Main EMF PNG output.
   Required context: Prefer deterministic AWT tests for the isolated bounds/alpha behavior.

6. Status: completed
   Next step: Verified with targeted tests, regenerated `github_epplus_code.png`, and broader Maven tests.
   Required context: Confirm drawing bounds and transparency improve without regressions.

## Goals
- Determine the concrete cause of wrong PNG drawing area/background alpha.
- Fix PNG output while preserving existing SVG/PNG behavior.
- Verify with tests and regenerated output.
- Append completion notes and move this file to `.tasks/done/00182_investigate_github_epplus_code_png_canvas_alpha.md`.

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `etc/data/dst/github_epplus_code.png`
- `etc/data/dst/github_epplus_code.svg`
- `../wmf-testcase/data/src/github_epplus_code.emf`

## Completion Notes
- Cause: standalone EMF header bounds were treated like an initial mapping only, so later records such as `setWindowExtEx(2540,2540)` could replace the PNG canvas size even though they were intended for coordinate mapping.
- Cause: `Main` forced opaque white backgrounds for all `.emf` raster output, which conflicted with transparent PNG expectations.
- Fix: added a `Gdi.emfHeader(...)` hook, made `EmfParser` report standalone EMF bounds/frame, and made `AwtGdi` lock the PNG canvas to the EMF header bounds while still applying subsequent mapping records to the DC.
- Fix: removed the `.emf`-specific opaque background default in `Main`.
- Regression tests: added AWT tests for EMF header canvas preservation and transparent EMF PNG output through `Main`.
- Verification: targeted `AwtGdiTest` tests passed; full `mvn -q test` passed.
- Regenerated `etc/data/dst/github_epplus_code.png`; resulting geometry is `795x1124`, alpha channel is present, and content bbox is `377x64+208+530`.

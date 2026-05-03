purpose
- Verify whether Windows playback of generated WMF/EMF effectively rounds mapped subpixel coordinates before deciding whether SvgGdi should preserve or round them.

context
- Recent SvgGdi changes preserved fractional mapped coordinates for polyline and gradient rectangle output.
- WMF/EMF records store many coordinates as integer logical units, but GDI mapping transforms can produce fractional device coordinates.
- The compatibility question is whether actual Windows WMF/EMF playback rounds those transformed coordinates in cases relevant to SvgGdi.
- Verification should use WmfGdi/EmfGdi-generated files and PowerShell/System.Drawing PNG rendering.
- User requested direct Win32 GDI playback instead of System.Drawing rendering.
- This task must not change exact XOR behavior and must not introduce font-file or font-metrics dependencies.

tasks
- [x] Generate minimal EMF/WMF probes for mapped polyline behavior.
- [x] Generate a minimal EMF probe for mapped GradientFill rectangle behavior.
- [x] Render/analyze EMF probes through direct Win32 GDI PlayEnhMetaFile.
- [x] Compare direct Win32 GDI pixel output against integer-rounded and fractional-reference probes.
- [x] Decide whether recent SvgGdi fractional coordinate changes should be kept, adjusted, or reverted.
- [x] If code changes are needed, update SvgGdi/tests and run focused/full Maven tests.

goals
- Produce observable Windows playback evidence for mapped subpixel polyline coordinates.
- Produce observable Windows playback evidence for mapped subpixel gradient rectangle coordinates.
- Document the decision in this task file.
- Avoid speculative conclusions based only on AwtGdi internals.

file list
- .tasks/00130_verify_gdi_rounding_with_metafiles.md
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete.
- next step: none.

notes
- Generated probes in /tmp/wmf2svg-rounding-probe:
  - emf-polyline-half.emf / emf-polyline-rounded.emf
  - wmf-polyline-half.wmf / wmf-polyline-rounded.wmf
  - emf-gradient-half.emf / emf-gradient-rounded.emf
  - emf-world-polyline-half.emf / emf-world-polyline-rounded.emf
  - emf-world-gradient-half.emf / emf-world-gradient-rounded.emf
- Initial mapping-record PNG results were inconclusive:
  - emf-polyline-half rendered no non-white pixels while the rounded control rendered a line.
  - wmf-polyline-half rendered at logical 1-unit placement rather than obvious half-device placement.
  - emf-gradient-half rendered no non-white pixels while the rounded control rendered a full gradient.
- Because the initial probe appears affected by metafile playback/mapping behavior rather than pure rounding, it should not be used as proof that SvgGdi should preserve fractional coordinates.
- Direct Win32 GDI live drawing probe results:
  - mapped half polyline: nonwhite=3 bbox=1,2-3,2
  - integer control polyline: nonwhite=3 bbox=0,1-2,1
  - mapped half gradient: nonwhite=4 bbox=2,2-3,3
  - integer control gradient: nonwhite=4 bbox=1,1-2,2
- These results show GDI rasterizes transformed coordinates onto the pixel grid, and they do not justify preserving fractional SVG coordinates as a compatibility improvement.
- Reverted the uncommitted fractional SvgGdi polyline and gradient-rectangle coordinate changes from tasks 00128 and 00129.
- Kept SvgGdi gradientFill null guards because they match AwtGdi and are unrelated to coordinate rounding.

summary
- Added temporary Win32 GDI PowerShell probes under /tmp/wmf2svg-rounding-probe to render EMF through PlayEnhMetaFile and to draw live GDI Polyline/GradientFill with anisotropic mapping.
- Direct live GDI showed transformed half-coordinate drawing lands on device pixels after GDI rasterization, so preserving fractional SVG coordinates is not a safe compatibility improvement.
- Reverted the uncommitted SvgGdi fractional polyline and gradient rectangle changes.
- Retained SvgGdi gradientFill null guards and the empty selectClipPath fix.
- Verified with focused SvgGdi tests and the full Maven test suite.

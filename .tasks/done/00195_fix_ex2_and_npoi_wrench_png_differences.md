purpose
- Investigate and fix non-font PNG differences in `ex2.png` and `github_npoi_wrench.png`.

context
- Reference PNGs are in `../wmf-testcase/data/png`.
- Current generated PNGs are in `etc/data/dst`.
- `ex2.png` has broad line/geometry differences.
- `github_npoi_wrench.png` has shape/edge/fill differences around a wrench image.
- Existing uncommitted changes from task 00193 modify `AwtGdi.patBlt`; keep them intact.

tasks
- [x] Reproduce and quantify current differences for `ex2.png` and `github_npoi_wrench.png`.
- [x] Inspect source WMF/EMF records and AWT replay paths related to the observed differences.
- [x] Implement focused fixes in `AwtGdi` or parser code, following existing design.
- [x] Add or adjust regression tests for the root causes.
- [x] Regenerate/check the two target PNGs and compare against references.
- [x] Run focused and full Maven tests.

goals
- Reduce significant non-font differences for both target PNGs without regressing prior fixes.
- Preserve transparent-background behavior where expected.
- Document cause, change, and verification in this task file before moving it to `.tasks/done`.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java` if regression tests are added
- Other parser files only if investigation shows the bug is not in AWT rendering

status
- Current status: completed with partial fix.
- Next step: none.
- Required context to continue: previous `patBlt` origin fix remains uncommitted and should not be reverted.

summary
- `github_npoi_wrench.emf`: root cause was placement of the one extra frame-derived canvas pixel when the EMF frame is in negative Y coordinates. The renderer placed the extra row below the bounds; the reference places it above. Adjusted `emfHeader` to move `canvasMinY` upward by the frame/bounds height delta when `frameTop < 0`. Fuzz20 AE improved from 1111 to 344.
- Added `testStandaloneEmfHeaderCanvasPlacesExtraFramePixelAboveNegativeFrame`.
- `ex2.wmf`: source is a WMF containing enhanced-metafile comment chunks. The generated image has only five solid colors, while the reference contains many antialiased intermediate colors. Enabling antialiasing only for pending EMF replay increased the fuzz20 AE from 938597 to 991853, so that change was not kept. No safe implementation change was made for `ex2` in this task.
- Verification: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` passed and `mvn -q test` passed.

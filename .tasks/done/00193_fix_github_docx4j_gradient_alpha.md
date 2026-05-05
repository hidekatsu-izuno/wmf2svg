purpose
- Investigate why `etc/data/dst/github_docx4j_gradient.png` differs mostly in alpha from `../wmf-testcase/data/png/github_docx4j_gradient.png`, then fix the AWT renderer.

context
- The reference PNG is fully opaque.
- The generated PNG has large alpha differences while looking similar on a white background.
- The user asked to investigate the cause and modify `AwtGdi`.
- Worktree was clean at the start of this task.

tasks
- [x] Inspect AWT image creation, background clearing, EMF+ gradient/compositing, and bitmap decoding paths related to alpha.
- [x] Identify the specific drawing operation that leaves transparent pixels in `github_docx4j_gradient.png`.
- [x] Modify `AwtGdi` using the existing rendering design, with focused tests if practical.
- [x] Regenerate or test the relevant output and compare alpha against the reference.
- [x] Run the focused Maven test suite.

goals
- `github_docx4j_gradient.png` should no longer have unintended transparent pixels.
- Existing transparent-background behavior for EMF/PNG output should remain intact.
- Verification should include alpha-specific checks and relevant unit tests.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java` if a focused regression test is added.

status
- Current status: completed.
- Next step: none.
- Required context to continue: `patBlt` wrote directly to `BufferedImage` using logical coordinates as device pixels; with EMF bounds top=277, the white PATCOPY background started at device y=277 instead of y=0.

summary
- Root cause: `AwtGdi.patBlt` bypassed `Graphics2D` and wrote directly to the backing `BufferedImage`, but did not subtract `canvasMinX/canvasMinY` for standalone EMF header bounds. `github_docx4j_gradient.emf` uses bounds `(0,277)-(1094,1059)`, so its white PATCOPY background was written beginning at device y=277 and left the first 277 rows transparent.
- Change: added device/logical coordinate helpers and made `patBlt` convert logical bounds to backing-image device bounds, including clip checks in logical coordinates.
- Regression test: added `testPatBltHonorsStandaloneEmfHeaderCanvasOrigin`.
- Verification: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` passed, `mvn -q test` passed, and `github_docx4j_gradient.png` alpha diff against the reference changed from 303315 pixels to 0.

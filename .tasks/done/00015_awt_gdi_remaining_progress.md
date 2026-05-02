purpose
- Continue reviewing `AwtGdi` for one more concrete implementation gap and implement it if it is safe and verifiable.

context
- User asked again whether more `AwtGdi` implementation can be advanced.
- Existing uncommitted changes from earlier tasks must be preserved.
- Scope is limited to behavior that can be expressed in the Java2D renderer without speculative platform emulation.

tasks
- status: completed
  next step: Inspect remaining stored state and active drawing paths.
  required context: Preserve current production/test diffs.
  action: Start this task.
- status: completed
  next step: Implement and test current-path fill mode behavior.
  required context: Focus on state that is stored but not reflected in output.
  action: Identify a safe target or document none found.
- status: completed
  next step: Run focused and full Maven tests.
  required context: Keep edits scoped to AWT implementation/tests.
  action: Patch code and regression tests.
- status: completed
  next step: Append summary and move task to done.
  required context: Use existing Maven commands.
  action: Verify and close task.

goals
- Advance one more concrete `AwtGdi` behavior if available.
- Preserve all existing uncommitted work.
- Record the outcome and verification.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00015_awt_gdi_remaining_progress.md`

summary
- Found one further concrete AwtGdi gap: `beginPath` fixed the `Path2D` winding rule at path creation time, so later `setPolyFillMode` changes were not reflected by `fillPath`, `strokeAndFillPath`, or `selectClipPath`.
- Added `currentPathWithPolyFillMode()` so path fill/clip operations rebuild the accumulated path using the current DC `PolyFillMode` at the time the path is consumed.
- Added `testFillPathUsesCurrentPolyFillMode` to verify that changing from `ALTERNATE` to `WINDING` after path construction affects the fill result.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`

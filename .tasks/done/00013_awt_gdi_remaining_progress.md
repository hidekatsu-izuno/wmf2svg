purpose
- Continue the `AwtGdi` implementation review and implement any remaining safe renderer improvements.

context
- User asked again whether more `AwtGdi` implementation can be advanced.
- Existing uncommitted changes from tasks `00008` through `00012` must be preserved.
- Focus only on concrete Java2D-renderable behavior, not speculative Windows-only color management.

tasks
- status: completed
  next step: Move task file to `.tasks/`.
  required context: Preserve current uncommitted production/test diffs.
  action: Start this task.
- status: completed
  next step: Inspect remaining stored state and drawing paths.
  required context: Check brush/pen/text/ROP/path/image state propagation.
  action: Identify a safe implementation target or document none found.
- status: completed
  next step: Run focused and full Maven tests.
  required context: Keep changes local and verifiable.
  action: Patch implementation and tests.
- status: completed
  next step: Append summary and move task to done.
  required context: Use existing Maven test commands.
  action: Verify and close task.

goals
- Advance another concrete `AwtGdi` gap if one remains.
- Otherwise document why remaining candidates are not appropriate.
- Keep the task resumable and complete.

file list
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `.tasks/00013_awt_gdi_remaining_progress.md`

summary
- Found one further concrete AwtGdi gap: `setROP2` was applied to stroked shapes, but brush-filled shapes used direct `Graphics2D.fill` and ignored the current ROP2 mode.
- Added a fill ROP2 path in `AwtGdi` that masks the filled shape, renders the selected brush into a source image, and combines source/destination pixels through the existing `applyRop2` logic.
- Added `testFillShapeRop2RespectsClipRegion` to verify filled-shape ROP2 behavior and clipping.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`

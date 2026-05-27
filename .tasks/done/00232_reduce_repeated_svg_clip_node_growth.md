purpose
- Reduce remaining issue-25-like SVG DOM growth for repeated clip/mask state changes where feasible.

context
- User asked to fix other practical cases found in the audit.
- Prior fix handled repeated `excludeClipRect` before drawing.
- Audit found similar candidates in `createClipRgnMask`, `offsetClipRgn`, and EMF+ clip mask operations.
- Work should avoid broad rewrites and preserve behavior once drawing has already occurred under an old mask.

tasks
- [completed] Define a reusable pending-mask check and group update helper.
  - status: completed
  - next step: none
  - required context: only reuse in-place masks when no drawing has occurred under that mask group.
- [completed] Apply helper to WMF/EMF clip operations.
  - status: completed
  - next step: none
  - required context: clone when the current group has children so already-emitted drawing keeps its old clip.
- [completed] Apply helper to feasible EMF+ clip operations.
  - status: completed
  - next step: none
  - required context: keep EMF+ intersect nesting behavior intact.
- [completed] Add focused regression tests.
  - status: completed
  - next step: none
  - required context: avoid depending on untrusted sample files.
- [completed] Verify.
  - status: completed
  - next step: none
  - required context: note any remaining cases not fixed.
- [completed] Finish task record.
  - status: completed
  - next step: move this file to `.tasks/done/`.
  - required context: complete only after verification.

goals
- Repeated clip updates before drawing grow linearly in mask contents without duplicating whole masks/groups.
- Existing drawing remains protected by the clip state active when it was emitted.
- Tests and issue sample verification pass.

file list
- `.tasks/00232_reduce_repeated_svg_clip_node_growth.md`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

summary
- Added `isPendingMask` and `setCurrentMask` helpers in `SvgGdi`.
- Reused an existing current mask only when the active masked group is empty and references that same mask.
- Applied the helper to:
  - `excludeClipRect`
  - `intersectClipRect`
  - `offsetClipRgn`
  - `extSelectClipRgn`
  - `selectClipPath`
  - feasible EMF+ union/exclude clip cases
- Left EMF+ `XOR` and `OffsetClip` clone behavior mostly unchanged because their composition semantics depend on the previous mask and are riskier to mutate in place.
- Added tests for repeated `RGN_DIFF` and repeated `offsetClipRgn` before drawing.
- Verification:
  - `mvn -q -Dtest=SvgGdiTest#testConsecutiveExcludeClipRectsBeforeDrawingReusePendingMask+testConsecutiveDiffClipRegionsBeforeDrawingReusePendingMask+testConsecutiveOffsetClipRgnBeforeDrawingReusesPendingMask test`
  - `mvn -q test`
  - `mvn -q verify`
  - Issue #25 samples remained at `Dessin5.svg` 857,358 bytes / 44 masks and `Dessin3.svg` 1,911,214 bytes / 44 masks.

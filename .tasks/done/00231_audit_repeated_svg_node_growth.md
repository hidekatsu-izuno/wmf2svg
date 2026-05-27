purpose
- Audit SVG backend code for issue-25-like behavior where repeated state updates can create excessive DOM nodes.

context
- User asked whether there are other places with the same kind of node growth problem caused by repetition.
- Issue #25 root cause was repeated `excludeClipRect` cloning and appending whole mask DOMs before any drawing occurred.

tasks
- [completed] Inspect SVG state-update methods that create or clone DOM nodes.
  - status: completed
  - next step: none
  - required context: focus on repeated state changes, not ordinary repeated drawing records.
- [completed] Classify any candidates.
  - status: completed
  - next step: none
  - required context: identify whether old nodes remain referenced by already-emitted drawing.
- [completed] Report findings.
  - status: completed
  - next step: answer user with likely candidates and confidence.
  - required context: no code edits unless an obvious bug fix is requested.

goals
- Give a concise answer with file/line references and risk level.
- Avoid changing code unless the user asks for implementation.

file list
- `.tasks/00231_audit_repeated_svg_node_growth.md`
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`

summary
- Main remaining candidates are in SVG clip/mask state handling.
- `createClipRgnMask` clones the current mask for `RGN_OR`, `RGN_XOR`, and `RGN_DIFF`; repeated calls before drawing can grow masks similarly to issue #25.
- EMF+ clip handling clones `emfPlusClipMask` for union/xor/exclude and copies mask children for offset clip; repeated state-only operations can grow defs unnecessarily.
- `offsetClipRgn` clones the current mask on every call. By itself it is linear when the mask is small, but it can amplify a large pending mask.
- Repeated ordinary drawing nodes, gradients, patterns, and bitmap masks are expected linear output growth and were not classified as the same problem.

# AwtGdi AngleArc Path Handling

## purpose
Fix AwtGdi AngleArc behavior while a path is open so it contributes to the current path instead of drawing immediately.

## context
`angleArc` currently calls `lineTo(start)` and then `strokeShape(createArc(...))` unconditionally. During `beginPath()`, `lineTo` appends to the path, but the arc itself is stroked immediately and is not part of the path consumed by `strokePath`, `fillPath`, or `selectClipPath`. This is inconsistent with other path-aware shape methods.

## tasks
- [x] status: completed
  task: Inspect current AngleArc and path interactions.
  next step: Add currentPath branch before immediate stroke.
  required context: Preserve non-path AngleArc rendering and current-position update.
- [x] status: completed
  task: Implement path-aware AngleArc and focused tests.
  next step: Run focused and full tests.
  required context: Do not change unrelated arc/arcTo behavior.
- [x] status: completed
  task: Run focused and full tests.
  next step: Append summary and move task to done.
  required context: Use existing Maven commands.

## goals
- AngleArc arc segment is deferred into currentPath while a path is open.
- Non-path AngleArc still renders immediately and updates current position.
- Regression test verifies path clipping or stroking uses the arc only after path consumption.

## file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

## summary
- Made `angleArc` path-aware: when a path is open, the connecting line and arc segment are appended to `currentPath`; otherwise the arc is still stroked immediately.
- Preserved current-position update to the arc endpoint.
- Added `testAngleArcInsidePathDoesNotDrawUntilStrokePath` to verify `angleArc` does not render during path construction and renders after `strokePath`.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`

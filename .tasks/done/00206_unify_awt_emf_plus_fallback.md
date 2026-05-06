# Unify AWT EMF+ Fallback Handling

## Purpose
Ensure `AwtGdi` uses one consistent mechanism for EMF+ fallback suppression, Dual EMF+ buffering, and `GetDC` GDI interop.

## Context
- User asks whether AWT fallback handling is unified, and wants it unified if multiple mechanisms differ.
- Current `AwtGdi` has direct flag writes to `suppressEmfPlusFallback`, Dual EMF+ buffering state, and `GetDC` state spread across comment parsing, EMF+ record handling, drawing helpers, and footer flushing.
- AWT can render EMF+ directly, so fallback suppression should be based on supported/rendered EMF+ draw records, while `GetDC` remains explicit GDI interop rather than fallback.
- Working tree starts clean after commit `5845072`.

## Tasks
- [x] Status: completed. Audit current fallback-related state transitions in `AwtGdi`.
- [x] Status: completed. Define a small unified internal API for entering/leaving EMF+ comments, recording rendered EMF+ output, handling Dual buffering, and deciding whether GDI fallback is suppressed.
- [x] Status: completed. Refactor `AwtGdi` to use the unified API instead of direct scattered flag mutation.
- [x] Status: completed. Confirm focused tests cover Dual EMF+, `GetDC` interop, and footer-flushed buffered EMF+.
- [x] Status: completed. Run targeted and full tests.

## Goals
- Fallback state changes in `AwtGdi` are routed through a small set of helper methods.
- Dual EMF+ buffering and non-Dual EMF+ comments share the same replay/parse path where practical.
- Existing 60677 behavior stays fixed: AWT does not draw extra center shapes and keeps required `GetDC` GDI interop.
- Verification commands are recorded before moving this task file to `.tasks/done/`.

## File List
- `.tasks/00206_unify_awt_emf_plus_fallback.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`

## Completion Summary
- Current state before the change: behavior was mostly unified, but the same fallback state was mutated directly from comment handling, `GetDC` handling, and each EMF+ draw helper.
- Refactor: introduced a unified internal path for Dual EMF+ comments, normal EMF+ comment replay, `GetDC` mode transitions, and fallback coverage marking/clearing.
- Removed the redundant `emfPlusDualRenderable` field; renderable Dual comments now mark fallback coverage through the same helper used by actual EMF+ drawing.
- Verification: existing focused tests cover Dual EMF+, `GetDC` interop, and footer-flushed buffered EMF+. `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` and `mvn -q test` passed.

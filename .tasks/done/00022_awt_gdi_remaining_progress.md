# AwtGdi Remaining Progress

## purpose
Check whether AwtGdi still has implementable GDI semantics and implement any remaining focused behavior that can be completed safely in the current renderer.

## context
Previous AwtGdi work covered object lifecycle, clip return values, color-management bookkeeping, EMF+ image effects, bitmap ROP clipping, stretch interpolation, ROP2 for fills/text, path fill timing, canvas growth, and invert region XOR rendering. Remaining candidates should be limited to behavior that can be represented in AwtGdi without broad parser or API redesign.

## tasks
- [x] status: completed
  task: Inspect AwtGdi text, bitmap, path, palette, and DC-state methods for remaining focused implementation opportunities.
  next step: Implement ETO_PDY paired X/Y text advances and parser handoff.
  required context: Avoid broad color-space correction, full bidi/layout redesign, and parser-level changes unless the local API already carries the necessary data.
- [x] status: completed
  task: Implement the highest-confidence remaining behavior with focused tests, if any.
  next step: Verify focused and full test suites.
  required context: ETO_PDY is local enough to implement; EMF dx parsing needs count * 2 values for paired x/y advances.
- [x] status: completed
  task: Verify focused and full test suites.
  next step: Append summary and move task to done.
  required context: Use existing Maven commands.

## goals
- Identify whether AwtGdi still has implementable gaps.
- Add complete, tested implementation for any focused remaining gap.
- Leave unsupported or too-broad items documented rather than half-implemented.

## file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java
- src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

## summary
- Implemented ETO_PDY support in AwtGdi by carrying paired horizontal and vertical text advances through text layout, canvas growth, background fill, normal drawing, and ROP2 text masks.
- Updated EMF ExtTextOut parsing to read two 32-bit advance values per character when ETO_PDY is set, including UTF-16 text converted through the target charset.
- Added `testExtTextOutPdyUsesVerticalAdvances` to verify vertical per-character advances are rendered.
- Verification passed:
  - `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
  - `mvn -q test`

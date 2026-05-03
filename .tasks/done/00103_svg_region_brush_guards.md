purpose
- Make SvgGdi region filling/framing follow AwtGdi brush handling more closely.

context
- AwtGdi fillRgn ignores null and hollow/null brushes, and uses the GdiBrush interface rather than requiring its own concrete brush class.
- SvgGdi fillRgn currently casts the supplied brush to SvgBrush, which can throw for foreign GdiBrush implementations and can append no-op output for null/hollow brushes.
- This is a region rendering robustness improvement only; it must not change XOR behavior or introduce font-file dependencies.

tasks
- [x] Add a helper in SvgGdi that converts renderable GdiBrush values to SvgBrush while filtering null/hollow brushes.
- [x] Update fillRgn to skip non-renderable brushes and support foreign solid/hatched GdiBrush implementations.
- [x] Update frameRgn to use the same brush handling where possible.
- [x] Add SvgGdi tests for null/hollow fillRgn suppression and foreign solid brush region fill.
- [x] Run focused SvgGdi tests. First runs failed because the null/hollow test looked at SVG-wide definitions/style; adjusted it to assert no rendered `<use>` is emitted. Rerun passed with `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test`.
- [x] Run the full Maven test suite. Passed with `mvn -q test`.

goals
- fillRgn no longer throws on null or foreign GdiBrush values.
- fillRgn does not append output for null/hollow brushes.
- fillRgn can render a region with a foreign solid GdiBrush.
- Existing SvgBrush pattern handling remains intact.

file list
- src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java

status
- current: complete
- next step: none

summary
- Added shared SvgGdi brush rendering guards for region fill/frame operations.
- fillRgn now skips null and hollow/null brushes, matching AwtGdi's non-rendering behavior.
- fillRgn and frameRgn can use foreign GdiBrush implementations by converting renderable brush data into a temporary SvgBrush.
- Existing SvgBrush CSS class and pattern handling is preserved.
- Kept SVG constraints unchanged: no exact XOR emulation changes and no font-file dependency.
- Verification passed: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest test` and `mvn -q test`.

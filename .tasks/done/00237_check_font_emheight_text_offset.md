# 00237 Check Font Emheight Text Offset

## Purpose
Clarify whether `font-emheight` should already compensate the text position difference between System.Drawing output and program PNG output for `image4.emf`.

## Context
The user pointed out that `font-emheight` exists for this purpose. Need inspect its usage before answering.

## Tasks
1. Status: completed
   Next step: Searched code for `font-emheight` and related font-height fitting paths.
   Required context: Determine whether it affects AWT PNG rendering, SVG output, or both.

2. Status: completed
   Next step: Compared how font em height is applied to text size versus baseline/origin placement.
   Required context: Focus on `AwtGdi` text drawing and rotated `EXTTEXTOUTW` path.

3. Status: completed
   Next step: Answered whether the remaining offset is expected despite `font-emheight`, or indicates a bug in applying it.
   Required context: No implementation unless user asks.

## Goals
- Give a corrected, precise explanation grounded in code.
- Identify if `font-emheight` is not being applied to PNG, not being applied to rotated text, or only solves font size but not baseline/origin.

## File List
- `.tasks/00237_check_font_emheight_text_offset.md`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/Main.java`

## Completion Summary
Completed on 2026-06-02.

- Confirmed `font-emheight.*` is read by `FontUtil.fontEmHeight`.
- Confirmed SVG fonts use it in `SvgFont` via `heightMultiply`, affecting `getFontSize`.
- Confirmed AWT/PNG rendering does not read `font-emheight.*`; it uses `AwtGdi.fitFontHeight` with Java `FontMetrics`.
- Conclusion: the user's point is correct in principle. The remaining PNG offset likely indicates the AWT path does not apply the same Windows em-height correction used by SVG, and additionally uses Java baseline/ascent/descent for placement.

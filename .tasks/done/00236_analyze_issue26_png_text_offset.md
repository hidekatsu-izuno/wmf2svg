# 00236 Analyze Issue 26 PNG Text Offset

## Purpose
Explain why `../wmf-testcase/data/png/image4.png` and `../wmf-testcase/data/dst/image4.png` show different text positions.

## Context
`../wmf-testcase/data/png/image4.png` was generated with `src/test/bin/wmf2png.ps1`, which uses Windows/System.Drawing metafile rendering. `../wmf-testcase/data/dst/image4.png` is the program-generated PNG. The user reports text-position differences.

## Tasks
1. Status: completed
   Next step: Inspected both PNG dimensions, hashes, and visual/text-region differences.
   Required context: Compare generated reference PNG and program PNG without modifying them.

2. Status: completed
   Next step: Inspected current EMF text parsing/rendering paths for coordinate and transform handling differences.
   Required context: Focus on rotated text labels in `image4.emf` and general text baseline/advance behavior.

3. Status: completed
   Next step: Summarized likely root cause and affected code paths.
   Required context: Do not implement unless the user asks for a fix.

## Goals
- Provide a concise cause explanation grounded in observed image/code evidence.
- Identify whether the offset is from parser transform handling, AWT text metrics, System.Drawing reference differences, or recent vertical text changes.

## File List
- `.tasks/00236_analyze_issue26_png_text_offset.md`
- `../wmf-testcase/data/png/image4.png`
- `../wmf-testcase/data/dst/image4.png`
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java`

## Completion Summary
Completed on 2026-06-02 06:43 JST.

- Confirmed both PNGs are `2995x2246` and the current headless Java output matches `../wmf-testcase/data/dst/image4.png`.
- Confirmed the difference is not a stale output file or a canvas-size mismatch.
- Confirmed Calibri and Arial are available to the Java/Linux environment, so simple font fallback is not the primary cause.
- Root cause: the reference PNG is rendered by Windows/System.Drawing directly from EMF, while the program PNG is rendered through the Java/AWT backend. AWT uses Java2D/FreeType font metrics, `FontMetrics`, and `drawString`/manual advances, which do not exactly match Windows GDI/GDI+ text placement. The rotated `EXTTEXTOUTW` labels in `image4.emf` are especially sensitive because the parser converts the EMF world transform into font escapement, then AWT rotates around the reference point and positions the baseline using Java metrics.

# Add Missing Font Emheight Entries

## Purpose

Add `font-emheight.*` entries to `SvgGdi.properties` for fonts currently installed under `/mnt/c/Windows/Fonts` when `src/test/bin/fontmetrics.ps1` reports a non-default ratio and the property is not already registered.

## Context

- `fontmetrics.ps1` uses `System.Drawing.Text.InstalledFontCollection` and emits entries only when `emHeight != cellAscent + cellDescent`.
- `SvgGdi.properties` already contains many historical Windows and other font entries.
- The worktree already has modified `FontUtil.java` and `SvgGdi.properties`; preserve unrelated existing edits.
- Do not add entries for fonts whose ratio is exactly `1`, because the script intentionally omits those.

## Tasks

1. Status: completed. Generate the current Windows font-emheight entries with `src/test/bin/fontmetrics.ps1`.
   Next step: saved escaped output to `/tmp/font-emheight-current-generated-escaped.properties`.
   Required context: use PowerShell against the Windows-installed font collection.

2. Status: completed. Compute missing `font-emheight.*` keys.
   Next step: found 211 generated keys missing from the existing properties file.
   Required context: keys with spaces use `\u0020`.

3. Status: completed. Add missing entries to `SvgGdi.properties`.
   Next step: inserted the 211 generated entries before `# Other Fonts`.
   Required context: preserve user edits already present in the file.

4. Status: completed. Verify that no emitted key from the current Windows fonts is still missing.
   Next step: comparison reported 0 missing generated keys.
   Required context: verification is comparison-based; no production code changes expected.

5. Status: completed. Record summary and move this task to `.tasks/done/`.
   Next step: move this task to `.tasks/done/`.
   Required context: final response should mention existing unrelated modified files.

## Goals

- All currently emitted `fontmetrics.ps1` entries are present in `SvgGdi.properties`.
- Only missing `font-emheight.*` properties are added.
- The task file records commands, counts, and verification outcome.

## File List

- `src/test/bin/fontmetrics.ps1`
- `src/main/resources/net/arnx/wmf2svg/gdi/svg/SvgGdi.properties`
- `.tasks/00224_add_missing_font_emheight.md`
- Temporary comparison files under `/tmp/font-emheight-current-*`

## Summary

Generated current installed Windows font ratios with:

`powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false); & '$(wslpath -w src/test/bin/fontmetrics.ps1)' | Set-Content -Encoding UTF8 '$(wslpath -w /tmp/font-emheight-current-generated-utf8.properties)'"`

Escaped non-ASCII font names to Java properties `\uXXXX` form, compared keys
against `SvgGdi.properties`, and found 211 missing `font-emheight.*` entries.
Inserted those entries before `# Other Fonts`.

Verification:

- Recomputed generated-vs-existing key difference: `0` missing keys.
- Loaded `SvgGdi.properties` with `java.util.Properties` via JShell and checked representative decoded keys:
  - `font-emheight.Arial Narrow = 0.883139284174213`
  - `font-emheight.游明朝 = 0.776934749620637`
  - `font-emheight.UD デジタル 教科書体 N = 0.864499788940481`
- Attempted `mvn -q -Dtest=FontUtilTest test`, but compilation failed because the current worktree's `FontUtil.java` lacks `fontCharset(String)` required by `SvgFont.java`. That file was already modified before this task, so it was left untouched.

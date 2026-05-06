purpose
- Add an `MT Extra` worksheet to `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx`.

context
- The workbook already contains symbol-font mapping sheets.
- The new worksheet should follow the existing workbook structure.
- For now, the `MT Extra` sheet must prepare code values from `0x21` through `0xFF`.
- `openpyxl` is not available in the local Python environment, so workbook updates will use direct OpenXML package editing.

tasks
- status: completed
  next step: Existing workbook package has four sheets, shared strings, styles, comments, and VML parts. Worksheets use four columns with shared strings and per-font style IDs.
  required context: Target file is `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx`.
- status: completed
  next step: Shared strings contain headers/common code values; styles contain per-font cell formats for existing symbol fonts. The new sheet will add `MT Extra` font styles and keep unmapped Unicode columns blank.
  required context: Use the discovered workbook conventions instead of inventing a new schema.
- status: completed
  next step: Wrote `sheet5.xml`, added workbook metadata/relationship/content-type/app-property entries, and added MT Extra font styles/shared strings. Removed a duplicate ZIP entry created during the first package write.
  required context: Preserve all existing sheets and workbook settings.
- status: completed
  next step: Validation passed: ZIP package has no compressed-data errors, workbook lists `MT Extra` as sheet 5, `sheet5.xml` parses, and A-column codes exactly match `0x21` through `0xFF`.
  required context: Verification must show the `MT Extra` sheet exists and contains code values from `0x21` to `0xFF`.
- status: completed
  next step: Completion summary appended and task file moved to `.tasks/done/`.
  required context: Include changed file, validation result, and any notable implementation decision.

goals
- `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx` contains a new `MT Extra` sheet.
- The sheet includes one row per code value from `0x21` through `0xFF`.
- Existing workbook sheets remain intact.
- The workbook package remains structurally valid.

file list
- `.tasks/00197_add_mt_extra_sheet.md`
- `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx`

completion summary
- Added `MT Extra` as sheet 5 in `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx`.
- Added workbook relationship/content-type/app-property metadata for the new worksheet.
- Added `MT Extra` font styles to `xl/styles.xml` inside the workbook package.
- Added rows `A2:A224` containing `0x21` through `0xFF`; B-D data cells are present but blank because Unicode mappings are not filled yet.
- Verified the ZIP package with `unzip -t`.
- Verified workbook metadata, sheet count, `sheet5.xml` parseability, and exact code coverage by reading OpenXML parts with Python.

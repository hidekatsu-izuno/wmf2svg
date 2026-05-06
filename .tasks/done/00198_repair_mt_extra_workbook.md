purpose
- Repair `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx` after Excel reports that the workbook is corrupted.

context
- The previous change added an `MT Extra` worksheet with code values `0x21` through `0xFF`.
- Basic ZIP validation and simple XML parsing passed, but Excel still reports corruption.
- The repair must preserve the requested `MT Extra` sheet and all existing sheets.

tasks
- status: completed
  next step: Inspected workbook parts. The likely Excel-level corruption is namespace-prefix drift from XML reserialization: existing markup-compatibility attributes still reference original prefixes, while declarations were rewritten to generated prefixes such as `ns4`.
  required context: Target file is `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx`; Excel reports it is corrupted.
- status: completed
  next step: Rebuilt the workbook from the original HEAD blob, preserving existing XML text exactly and applying only minimal textual/package additions for `MT Extra`.
  required context: Prefer direct OpenXML repair without changing unrelated workbook content.
- status: completed
  next step: Validation passed: ZIP test is OK, every XML part parses, all worksheet relationship targets exist, `MT Extra` is sheet 5, style/shared-string counts are consistent, and codes exactly match `0x21` through `0xFF`.
  required context: Verification must go beyond `unzip -t`.
- status: completed
  next step: Completion summary appended and task file moved to `.tasks/done/`.
  required context: Include the suspected cause, changed file, and verification results.

goals
- Excel should no longer report the workbook as corrupted.
- `MT Extra` remains present as a worksheet.
- `MT Extra` contains code values from `0x21` through `0xFF`.
- Existing four sheets remain intact.

file list
- `.tasks/00198_repair_mt_extra_workbook.md`
- `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx`

completion summary
- Rebuilt `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx` from the original HEAD workbook package.
- Preserved original workbook/style/content-type/relationship XML text and namespace declarations, then added only the required `MT Extra` worksheet references and style/shared-string entries.
- Suspected corruption cause: the previous direct XML rewrite changed namespace prefixes in existing parts while markup-compatibility attributes still referenced the original prefixes.
- Added `xl/worksheets/sheet5.xml` with `MT Extra` and code values `0x21` through `0xFF`.
- Verified `unzip -t`, XML parseability for every `.xml` part, worksheet relationship targets, shared-string/style counts, and exact code coverage.

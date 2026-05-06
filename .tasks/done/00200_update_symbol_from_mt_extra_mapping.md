purpose
- Update the symbol-font replacement mapping code using the newly added `MT Extra` mappings from `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx`.

context
- The workbook now contains an `MT Extra` worksheet with code values `0x21` through `0xFF`.
- The `MT Extra` worksheet has source glyph cells for all codes and Unicode mappings for known MT Extra codes.
- The initial interpretation was to update the workbook's `Symbol` worksheet, but inspection showed that direct same-code copying would overwrite valid Symbol-font mappings with different MT Extra glyph mappings.
- The applicable target is `SymbolFontMappings.java`, which owns replacement mappings for Symbol/Wingdings-family fonts.
- Previous workbook repairs showed that whole XML reserialization can corrupt Excel namespace compatibility metadata, so edits must preserve existing XML text where possible.

tasks
- status: completed
  next step: Inspected the `Symbol` and `MT Extra` worksheets. Same-code MT Extra mappings conflict with valid Symbol-font mappings, so the workbook `Symbol` sheet should not be overwritten.
  required context: Only update Symbol mappings where the MT Extra mapping clearly applies to the same code value.
- status: completed
  next step: Determined that the correct target is the `SymbolFontMappings` utility, not the workbook `Symbol` worksheet.
  required context: Existing Symbol mappings should be preserved unless the MT Extra-derived mapping fills a blank or clearly corrects a missing value.
- status: completed
  next step: Added an `MT Extra` mapping array to `SymbolFontMappings.java`, registered the font name, and mapped code points from the workbook's MT Extra sheet.
  required context: Preserve existing Symbol/Wingdings behavior.
- status: completed
  next step: Added focused tests for MT Extra replacement and preservation of unknown source codes.
  required context: Use representative mappings from the workbook such as `0x23`, `0x51`, and `0x63`.
- status: completed
  next step: Relevant and full Maven tests passed.
  required context: Verification must show existing Symbol/Wingdings tests still pass and MT Extra behavior works.

completion summary
- Did not overwrite the workbook `Symbol` sheet because the overlapping MT Extra code values map to different glyphs than valid Symbol-font mappings.
- Added an `MT Extra` mapping table to `src/main/java/net/arnx/wmf2svg/util/SymbolFontMappings.java` using the workbook's MT Extra Unicode mappings.
- Registered `MT Extra` in `SymbolFontMappings.getMapping`.
- Added `testMtExtraMapping` to verify mapped and unmapped MT Extra replacement behavior.
- Verified with `mvn -q -Dtest=SymbolFontMappingsTest test` and `mvn -q test`.
- status: pending
  next step: Append completion summary and move this task file to `.tasks/done/`.
  required context: Include updated codes, mapping source, and validation results.

goals
- Symbol-font replacement mappings include `MT Extra`.
- Existing reliable Symbol mappings are not unintentionally overwritten.
- Workbook remains unchanged by this task unless verification discovers a needed correction.

file list
- `.tasks/00200_update_symbol_from_mt_extra_mapping.md`
- `etc/symbol_wingdings_4sheets_unicode_mapped.xlsx`
- `src/main/java/net/arnx/wmf2svg/util/SymbolFontMappings.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SymbolFontMappingsTest.java`

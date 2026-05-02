# SvgFont fallback font list

## Purpose
SvgFontで古いWindows論理フォント名が指定された場合に、現代的な代替フォントとCSS generic familyをfont-familyへ追加する。

## Context
- `SvgFont.toString()` は `faceName` をfont-familyの先頭に置き、`SvgGdi.properties` の `alternative-font.<font>` があれば1要素として追加する。
- 既存の代替フォント設定は単一フォント名を前提に見えるため、`Consolas, monospace` のような複数フォント指定を正しく複数要素として出力する必要がある。
- CSS出力ではスペースを含むフォント名だけ引用し、generic familyは引用しない既存挙動を保つ。

## Tasks
- [x] `.tasks/todo/00053_svg_font_fallbacks.md` を `.tasks/00053_svg_font_fallbacks.md` へ移動して作業開始状態にする。
- [x] `SvgFont` の代替フォント処理を確認し、カンマ区切りの代替フォントを個別のfont-family要素として追加する。
- [x] `SvgGdi.properties` に指定された9件の論理フォント名と代替フォントリストを追加する。
- [x] `SvgGdiTest` に代表ケースを追加し、複数代替フォントが正しい引用と順序で出力されることを検証する。
- [x] 関連テストを実行し、結果を確認する。
- [x] 完了時に変更内容と判断を追記し、`.tasks/done/00053_svg_font_fallbacks.md` へ移動する。

## Goals
- `Fixedsys` は `Fixedsys, Consolas, monospace` を出力する。
- `MS Serif` は `MS Serif, Times New Roman, serif` を出力する。
- スペースを含むフォント名は引用され、generic familyは引用されない。
- 既存の単一代替フォント設定は壊れない。
- `mvn -q test` または対象テストが成功する。

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgFont.java`
- `src/main/resources/net/arnx/wmf2svg/gdi/svg/SvgGdi.properties`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Resume State
- Current status: completed; task file moved to `.tasks/done/00053_svg_font_fallbacks.md`.
- Next step: none.
- Required context: user requested these mappings:
  - Fixedsys -> Consolas, monospace
  - Modern -> Courier New, monospace
  - MS Sans Serif -> Microsoft Sans Serif, monospace
  - MS Serif -> Times New Roman, serif
  - Roman -> Times New Roman, serif
  - Script -> Segoe Script, cursive
  - Small Fonts -> Segoe UI, sans-serif
  - System -> Segoe UI, sans-serif
  - Terminal -> Cascadia Mono, monospace

## Completion Summary
- `SvgFont` now splits `alternative-font.*` values on commas and adds each non-empty trimmed entry as an individual `font-family` fallback.
- `SvgFont` uses the first configured alternative font as the existing `font-emheight` fallback target, preserving metric fallback behavior for multi-entry mappings.
- `SvgGdi.properties` includes the 9 requested Windows logical font fallback mappings.
- `SvgGdiTest.testLogicalFontFallbackFamilies` verifies all requested mappings, including quoting for font names with spaces and unquoted CSS generic families.
- Verification: `mvn -q test` passed after implementation and again after formatting/line-ending cleanup; `git diff --check` passed.

# AwtGdi logical font fallbacks

## Purpose
AwtGdiで古いWindows論理フォント名が指定され、Java2Dでその実フォントが利用できない場合に、指定された代替フォント候補から利用可能な実フォントを選ぶ。

## Context
- `AwtGdi.toFont(AwtFont)` は現在 `new Font(faceName, style, size)` を直接作成している。
- Java2Dでは存在しないフォント名を指定しても論理フォントにフォールバックされるため、明示的にインストール済みフォント一覧を確認する必要がある。
- `serif`, `monospace`, `sans-serif`, `cursive` などはCSS generic familyであり、Java2Dで探す実フォント名としては扱わない。具体フォント候補が見つからない場合は既存のJava2D既定フォントへ落とす。
- SvgFont側には同種の論理フォント名マッピングが追加済み。

## Tasks
- [x] `.tasks/todo/00054_awt_font_fallbacks.md` を `.tasks/00054_awt_font_fallbacks.md` へ移動して作業開始状態にする。
- [x] `AwtGdi` のフォント生成経路を確認し、フォント名解決の追加位置を決める。
- [x] AwtGdiに9件の論理フォント代替候補を追加し、インストール済み実フォントだけを選択する処理を実装する。
- [x] CSS generic family候補は実フォント探索から除外し、候補がない場合はJava2D既定フォントを使う。
- [x] `AwtGdiTest` に解決ロジックのテストを追加する。
- [x] 関連テストを実行し、結果を確認する。
- [x] 完了時に変更内容と判断を追記し、`.tasks/done/00054_awt_font_fallbacks.md` へ移動する。

## Goals
- 指定フォントが利用可能なら元のフォント名を使う。
- 指定フォントが利用不可で、代替候補の具体フォントが利用可能なら先頭の利用可能候補を使う。
- `serif` などのgeneric familyは実フォント候補として選ばない。
- 具体候補が見つからない場合は `Font.SANS_SERIF` の既定フォールバックを使う。
- 既存のフォントサイズ調整と太字/斜体処理を壊さない。

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`

## Resume State
- Current status: completed; task file moved to `.tasks/done/00054_awt_font_fallbacks.md`.
- Next step: none.
- Required context: use the same mappings as the SvgFont request:
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
- Added AwtGdi logical font fallback mappings for the same 9 legacy Windows font names used by SvgFont.
- `AwtGdi.toFont(AwtFont)` now resolves the requested family against installed Java2D font families before constructing `Font`.
- If the requested family is unavailable, AwtGdi tries configured concrete fallback font names in order.
- CSS/Java logical family names such as `serif`, `sans-serif`, `monospace`, and `cursive` are not treated as installed real font candidates; unresolved cases use `Font.SANS_SERIF`, matching the existing AwtGdi default.
- `@`-prefixed vertical font names are normalized by removing `@` before lookup.
- Added unit tests for keeping an installed requested font, using installed fallbacks, handling `@Terminal`, and skipping generic families.
- Verification: `mvn -q test -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest`, `mvn -q spotless:apply`, `mvn -q test`, and `git diff --check` all passed.

## Follow-up
- Task `00055_awt_load_alternative_font_properties` replaced the initial hardcoded AwtGdi fallback map with `SvgGdi.properties` `alternative-font.*` lookup.

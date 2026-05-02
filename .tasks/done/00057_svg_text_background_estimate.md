# SVG text background estimate

## Purpose
SvgGdiでフォント実測に依存せず、文字数やem相当の推定値からテキスト背景矩形を塗りつぶす。

## Context
- SvgGdiはフォントに依存しない方針のため、実フォント計測は行わない。
- `extTextOut` は `rect` がある場合や一部推定で背景を塗っているが、`textOut` は背景矩形を生成していない。
- 既存の `extTextOut` 推定は `byte[] text.length / 2` に依存しており、変換後文字列長やemベースの推定としては弱い。
- 背景を塗る条件は GDI の `bkMode == OPAQUE` または `ETO_OPAQUE`。

## Tasks
- [x] `.tasks/todo/00057_svg_text_background_estimate.md` を `.tasks/00057_svg_text_background_estimate.md` へ移動して作業開始状態にする。
- [x] `textOut` / `extTextOut` のテキスト座標、アラインメント、縦書き、dx処理を確認する。
- [x] フォントサイズ、変換後文字列長、dx配列から論理座標の背景矩形を推定する共通ヘルパを追加する。
- [x] `textOut` で `bkMode == OPAQUE` のとき、推定背景矩形をテキスト直前に追加する。
- [x] `extTextOut` で `rect == null` の背景推定を共通ヘルパへ置き換え、既存の明示 `rect` 背景は維持する。
- [x] `SvgGdiTest` に `textOut` と `extTextOut` の背景推定テストを追加する。
- [x] 関連テストと全体テストを実行する。
- [x] 完了時に変更内容と判断を追記し、`.tasks/done/00057_svg_text_background_estimate.md` へ移動する。

## Goals
- `textOut` でも不透明背景モードで `<rect fill=...>` が出力される。
- `extTextOut` は明示 `rect` がない場合でも変換後文字列長とem推定で背景を塗る。
- 文字幅推定は横書きで概ね `0.5em * 文字数`、縦書きでは幅 `1em`・高さ `0.5em * 文字数` を基本にする。
- `dx` がある場合は可能な限り `dx` の合計を優先する。
- 実フォント計測には依存しない。

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/svg/SvgGdi.java`
- `src/test/java/net/arnx/wmf2svg/gdi/svg/SvgGdiTest.java`

## Resume State
- Current status: completed; task file moved to `.tasks/done/00057_svg_text_background_estimate.md`.
- Next step: none.
- Required context: keep SvgGdi font-independent; use estimated em metrics instead of Java/AWT font measurement.

## Completion Summary
- Added font-independent text background estimation helpers to SvgGdi.
- `textOut` now emits an estimated background `<rect>` before the `<text>` when `bkMode` is `OPAQUE`.
- `extTextOut` now uses the same estimation helper when it needs a background and no explicit `rect` is provided; explicit `rect` behavior remains unchanged.
- Horizontal text estimates width as `0.5em * convertedString.length()` when no `dx` is supplied; vertical text uses `1em` width and estimated advance as height.
- `dx` arrays continue to drive estimated advance where supplied, and `textOut` includes `textCharacterExtra` in estimated advance.
- Rotated text backgrounds are grouped with their text so the same transform applies to both.
- Added SvgGdi tests for `textOut` and `extTextOut` estimated opaque backgrounds.
- Verification: `mvn -q test -Dtest=net.arnx.wmf2svg.gdi.svg.SvgGdiTest`, `mvn -q spotless:apply`, `mvn -q test`, and `git diff --check` passed.

## Follow-up
- Task `00058_svg_text_fullwidth_background` refined the em estimate so CJK/fullwidth characters count as `1em` instead of `0.5em`.

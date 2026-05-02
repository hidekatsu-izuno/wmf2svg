# AwtGdi er-diagram icon rendering fix

## Purpose
er-diagram.wmf から生成される PNG でアイコンが消える回帰を修正する。

## Context
- 入力: ../wmf-testcase/data/src/er-diagram.wmf
- 正解 PNG: ../wmf-testcase/data/png/er-diagram.png
- 問題 PNG: etc/data/dst/er-diagram.png または target/awt-png/er-diagram.png
- SVG は etc/data/dst/er-diagram.svg に存在するため、AwtGdi のラスター描画経路を中心に調査する。
- 既存ワークツリーには他の Awt/WMF/EMF 変更があるため、無関係な変更は戻さない。

## Tasks
- [x] 正解 PNG・問題 PNG・SVG の差分から消えているアイコン領域と描画要素を特定する。
- [x] WMF 記録を確認し、AwtGdi で落ちている処理を特定する。
- [x] AwtGdi の該当描画処理を修正し、必要なら回帰テストを追加する。
- [x] er-diagram を再生成して正解 PNG と比較確認する。
- [x] focused test, full test, diff whitespace check を実行する。
- [x] 変更内容と判断を追記し、完了へ移動する。

## Goals
- er-diagram.png の消えているアイコンが AwtGdi 生成 PNG に戻る。
- 既存の AwtGdi/WMF/EMF テストが成功する。

## File List
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java (必要な場合)

## Summary
- er-diagram.wmf のアイコンは埋め込み EMF の `EMR_MASKBLT` で描かれていた。
- source DIB は 24bpp、mask DIB は 1bpp だが、mask DIB にはカラーテーブルが含まれていなかった。
- AwtGdi の DIB decode はカラーテーブル欠落時に WMF bitmap と同じ白黒向きへ倒していたため、MaskBlt のマスク解釈が逆になり、アイコン部分が destination copy になって消えていた。
- DIB_RGB_COLORS の 1bpp DIB でカラーテーブルが省略されている場合だけ、既定色を black/white として扱うようにした。DIB_PAL_COLORS の既存挙動は維持した。
- `0xCCAA0029` の MaskBlt で、カラーテーブル無し 1bpp DIB mask によって padding color を destination に残し、アイコン画素を source copy する回帰テストを追加した。

## Verification
- `java -Djava.awt.headless=true -cp target/classes net.arnx.wmf2svg.Main -replace-symbol-font ../wmf-testcase/data/src/er-diagram.wmf target/er-diagram-fixed.png`
- `identify target/er-diagram-fixed.png ../wmf-testcase/data/png/er-diagram.png` -> both `309x194`
- Visual check: `target/er-diagram-fixed.png` shows the three left-side icons.
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`

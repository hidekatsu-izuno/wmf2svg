# AwtGdi 12e9 PNG rendering fix

## Purpose
12e9.wmf から生成される PNG の表示崩れを修正する。SVG は正しく表示されているため、AwtGdi のラスター描画経路を中心に原因を特定する。

## Context
- 入力: ../wmf-testcase/data/src/12e9.wmf
- 正解 PNG: ../wmf-testcase/data/png/12e9.png
- 問題 PNG: etc/data/dst/12e9.png
- 既存ワークツリーには他の Awt/WMF/EMF 変更があるため、無関係な変更は戻さない。

## Tasks
- [x] 12e9 の PNG/SVG/WMF 記録を確認し、PNG だけが崩れる箇所を特定する。
- [x] AwtGdi の該当描画処理を修正する。
- [x] 12e9 を再生成または targeted test で確認する。
- [x] focused test, full test, diff whitespace check を実行する。
- [x] 変更内容と判断を追記し、完了へ移動する。

## Goals
- 12e9.png のラスター表示が SVG と同等に改善する。
- 既存の AwtGdi/WMF/EMF テストが成功する。

## File List
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java (必要な場合)

## Summary
- 12e9.wmf は WMF 側で `setWindowOrgEx(0, 0)` と `setWindowExtEx(1504, 512)` を明示しているが、AwtGdi がその後の `extTextOut` の current-position/dx によって canvas を 2080px 幅まで拡張していた。
- `setWindowExtEx` で明示的な window extent が与えられた場合は canvas サイズを固定し、後続の text bounds による自動拡張を無効化した。
- window extent 固定後に `TA_UPDATECP` と大きな dx を伴う文字出力を行っても canvas が拡張されないことを AwtGdiTest に追加した。

## Verification
- `java -Djava.awt.headless=true -cp target/classes net.arnx.wmf2svg.Main -replace-symbol-font ../wmf-testcase/data/src/12e9.wmf target/12e9-fixed.png`
- `identify target/12e9-fixed.png` -> `1504x512`
- `identify ../wmf-testcase/data/png/12e9.png` -> `1504x512`
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`

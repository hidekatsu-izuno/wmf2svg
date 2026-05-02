# Compare PNGs with fontdir

## Purpose
Windows フォントを `-fontdir /mnt/c/Windows/Fonts/` で登録した状態で正解 PNG と生成 PNG を比較し、残る不具合を特定・修正する。

## Context
- 正解 PNG: `../wmf-testcase/data/png/*.png`
- 生成 PNG: `etc/data/dst/*.png`
- 今回は再生成時に `-fontdir /mnt/c/Windows/Fonts/` を指定する。
- フォントが指定されているため `-replace-symbol-font` は使わず、Symbol/Wingdings も実フォントで描画する。
- フォントが存在しているにも関わらず差が出る場合、text 差分もバグとして扱い、実装修正対象にする。
- `/mnt/c/Windows/Fonts/` にはフォントファイルが存在することを確認済み。

## Tasks
- [x] `-fontdir /mnt/c/Windows/Fonts/` 付き、`-replace-symbol-font` なしで対象 WMF/EMF から `etc/data/dst/*.png` を再生成する。
- [x] 正解 PNG と生成 PNG の欠落・余剰・寸法差・AE/RMSE を計測する。
- [x] 差分が大きい画像を triplet で目視確認し、text 差分を含めて不具合候補を分類する。
- [x] 実装可能な不具合を修正する。
- [x] 修正後に対象 PNG を再生成し、改善を確認する。
- [x] focused test / full test / diff check を実行する。
- [x] 変更内容と残課題を追記し、完了へ移動する。

## Goals
- Windows フォントありで残る実害のある不具合候補が明確になる。
- 実装可能な差分は修正される。
- 残る差分は理由と次の調査対象が記録される。

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java` (必要な場合)
- `src/main/java/net/arnx/wmf2svg/util/FontUtil.java` (必要な場合)
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java` (必要な場合)
- `etc/data/dst/`, `target/png-compare-fontdir/` (生成物)

## Summary
- `/mnt/c/Windows/Fonts/` を登録し、`-replace-symbol-font` なし相当で `etc/data/dst/*.png` を再生成した。
- すべての比較対象で寸法差はなくなった。最終上位差分は `target/png-compare-fontdir/top-ae-ratio.csv` に記録した。
- `text.png` は Windows GDI の `Metafile.GetBounds` が `0,-120,329.9854,459.9787` を返すことを PowerShell で確認した。AwtGdi は負のデバイス境界をキャンバス原点として扱えず、通常テキストが上に詰まり、回転テキストで右方向にキャンバスを広げていた。
- AwtGdi に負のキャンバス原点 (`canvasMinX`, `canvasMinY`) と既存画像シフト付きリサイズを追加した。テキストは GDI 参照点を回転中心にし、右/下方向のテキストは現在の描画面でクリップして、デフォルト WMF キャンバスを不要に拡張しないようにした。
- `text.png` は `330x460` に戻り、AE は `50688` から `32013` に改善した。
- 残差分の多くはフォント差・メトリクス差・アンチエイリアス差が中心。`Pixie` などは `/mnt/c/Windows/Fonts/` に実体がなく、今回の「存在するフォント差分」対象から外れる。

## Verification
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`
- `mvn -q test`
- `git diff --check`

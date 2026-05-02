# Fix PNG comparison candidates

## Purpose
PNG 比較で見つかった高優先度候補を修正し、正解 PNG に近づける。

## Context
- 正解 PNG: `../wmf-testcase/data/png/*.png`
- 最新コード再生成 PNG: `target/png-current/*.png`
- 比較結果: `.tasks/done/00044_compare_generated_pngs.md`
- 対象:
  - `bitblt.png`: 正解 700x600、生成 701x601。下部 ROP セルに大きな色差。
  - `er-diagram.png`: アイコンは復活済みだが、埋め込み EMF 全体が左上に寄る。
  - `text.png`, `texts.png`: rotated/aligned text の配置・clipping 差。
  - `Eg.png`: Pixie 文字の太さ/大きさと背景 gradient/raster 差。
  - `ex2.png`: 密な線画で広範囲に線/パス差分。
  - `ex6.png`: 棒の下の繰り返し text label 差/欠け。
- 追加方針: `text.png` などの text 差がフォント未検出/代替フォント起因と判断できる場合、今回は修正対象から外してよい。
- 既存ワークツリーには前回の `.tasks/done/00044_compare_generated_pngs.md` が未コミットで存在する可能性がある。無関係な変更は戻さない。

## Tasks
- [x] `bitblt.png` の寸法差と ROP セル差の原因を特定し、AwtGdi/関連 parser を修正する。
- [x] `er-diagram.png` の埋め込み EMF mapping/origin 差を特定し、修正する。
- [x] `text.png` と `texts.png` の rotated/aligned text 差を同じ観点で調査し、フォント起因なら今回はスキップする。
- [x] `Eg.png` の Pixie text と背景差を分解し、フォント起因なら text 差はスキップし、実装可能な不具合を修正する。
- [x] `ex2.png` と `ex6.png` の線/パス/text label 差を調査し、実装可能な不具合を修正する。
- [x] 対象 PNG を最新コードで再生成し、AE/RMSE と目視で改善を確認する。
- [x] focused test, full test, diff whitespace check を実行する。
- [x] 変更内容と判断を追記し、完了へ移動する。

## Goals
- 対象 PNG の明確な寸法差・描画欠落・大きな配置差が改善する。
- 対応できない差が残る場合は、理由と次の調査対象を記録する。
- 既存テストが成功する。

## File List
- `src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java`
- `src/main/java/net/arnx/wmf2svg/gdi/emf/EmfParser.java` (必要な場合)
- `src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java`
- `target/png-current/`, `target/png-compare/` (生成物)

## Summary
- `bitblt.png`: AWT の hairline stroke でキャンバスを 1px 余分に広げていたため、1px 以下の stroke は shape bounds で確保するようにした。DIBBITBLT の source なし ROP は source-independent な ROP だけを `patBlt` に流し、source-dependent ROP は destination を維持するようにした。
- `er-diagram.png`: placeable WMF 内の埋め込み EMF で、コメント時点の placeable mapping を「明示 mapping」と扱っていたため、後続の外側 WMF mapping が使われなかった。placeable 時は footer 時点の外側 DC を使って再生するようにした。
- `ex2.png`, `ex6.png`, `texts.png`: EMF コメント後に外側 WMF mapping が現れる場合も footer 時点の DC で埋め込み EMF を再生し、EMF 側の window/viewport mapping record を抑止するようにした。非 placeable の埋め込み EMF は EMF header bounds で clip するようにした。`ex6` は大幅改善、`ex2` も左上にはみ出していた描画が減った。
- `text.png`, `texts.png`, `Eg.png`: Times New Roman, Arial, Verdana, Comic Sans MS, Tahoma, Wingdings などがローカル環境で代替フォントに解決されるため、残る text 差分は今回の方針どおりフォント起因としてスキップした。`Eg` の背景 gradient は目視上ほぼ一致し、主差分は Pixie 文字のフォント置換。
- 再比較結果: `bitblt` AE 1534 / RMSE 3152.18、`er-diagram` AE 5033 / RMSE 12527.1、`ex2` AE 1.02386e+06 / RMSE 12084.4、`ex6` AE 54829 / RMSE 7697.97、`texts` AE 1.94197e+06 / RMSE 4933.67。
- Verification: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`, `mvn -q test`, `git diff --check`。

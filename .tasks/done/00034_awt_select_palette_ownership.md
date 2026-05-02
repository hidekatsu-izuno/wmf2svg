purpose
- AwtGdi.selectPalette が AwtGdi 生成の palette だけを選択状態にするようにし、foreign GdiPalette が DIB_PAL_COLORS 描画へ影響する状態を防ぐ。

context
- AwtGdi.resizePalette/setPaletteEntries は AwtPalette のみを更新対象にしている。
- deleteColorSpace も AwtColorSpace のみを成功扱いにしており、AwtGdi の object state は Awt 実装型に閉じる方針。
- 現在の selectPalette は任意の GdiPalette を selectedPalette に入れ、applyPaletteToDib がその entries を描画に使う。

tasks
- status: completed
  next step: selectedPalette の型を AwtPalette にし、selectPalette で AwtPalette 以外は選択解除として扱う。
  required context: null を渡した場合は palette 選択解除にする。
- status: completed
  next step: AwtGdiTest に foreign GdiPalette が DIB_PAL_COLORS 描画に影響しないテストを追加する。
  required context: createPaletted1BppDib helper で selected palette の効果を観測できる。
- status: completed
  next step: focused test と全体 test を実行する。
  required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` で確認する。

goals
- AwtGdi.selectPalette が AwtPalette を選択できる。
- null または foreign GdiPalette では selected palette が解除される。
- foreign GdiPalette の entries が AwtGdi の DIB_PAL_COLORS 描画に使われない。

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

summary
- AwtGdi.selectedPalette の型を AwtPalette にし、selectPalette が AwtPalette 以外を選択解除として扱うようにした。
- SaveDC/RestoreDC の selected palette 保存型も AwtPalette に揃えた。
- AwtGdiTest に foreign GdiPalette が DIB_PAL_COLORS 描画へ影響しないことを確認するテストを追加した。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`、`mvn -q test`、`git diff --check` は成功した。

purpose
- AwtGdi.deleteColorSpace が AwtGdi 生成の color space だけを成功扱いにするよう、GDI オブジェクト lifecycle の状態実装を補う。

context
- AwtGdi.createColorSpace/createColorSpaceW は AwtColorSpace を生成する。
- SvgGdi.deleteColorSpace は選択中 color space を解除しつつ、SvgColorSpace 以外は false を返している。
- 現在の AwtGdi.deleteColorSpace は任意の GdiColorSpace に true を返すため、AwtGdi が所有しない color space の削除成功を観測できてしまう。

tasks
- status: completed
  next step: AwtGdi.deleteColorSpace の戻り値を AwtColorSpace 型に基づく判定へ変更する。
  required context: 選択中 color space の解除挙動は維持する。
- status: completed
  next step: AwtGdiTest に AwtColorSpace は true、外部 GdiColorSpace は false、選択解除は維持されることを確認するテストを追加する。
  required context: setColorSpace の戻り値で選択状態を観測できる。
- status: completed
  next step: focused test と全体 test を実行する。
  required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` で確認する。

goals
- AwtGdi.deleteColorSpace が AwtGdi 生成の color space 削除成功だけを true で返す。
- 選択中 color space を削除した場合は選択状態が解除される。
- AwtGdi 以外の GdiColorSpace 実装は false で拒否される。

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

summary
- AwtGdi.deleteColorSpace の戻り値を AwtColorSpace のみ true に変更した。
- 選択中 color space の削除時に選択解除する既存挙動は維持した。
- AwtGdiTest に AwtColorSpace/外部 GdiColorSpace/null の戻り値と選択状態を確認するテストを追加した。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`、`mvn -q test`、`git diff --check` は成功した。

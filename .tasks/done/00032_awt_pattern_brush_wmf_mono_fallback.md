purpose
- AwtGdi の WMF monochrome CreatePatternBrush を直前の DibCreatePatternBrush と対応付け、SvgGdi と同等の pattern brush 補正を描画側にも追加する。

context
- SvgGdi.createPatternBrush は monochrome WMF bitmap を受け取った場合、直前の DibCreatePatternBrush の画像を再利用する。
- AwtGdi.dibCreatePatternBrush は pattern brush を作るだけで、直前 DIB pattern brush の状態を保持していない。
- AwtGdi.createPatternBrush は monochrome WMF bitmap をそのまま pattern として扱うため、WMF 由来の pattern brush が SvgGdi と異なる描画になり得る。

tasks
- status: completed
  next step: AwtGdi に直前 DIB pattern brush の image/usage 状態を追加する。
  required context: SvgGdi の lastDibPatternBrushImage/Usage と同じ責務にする。
- status: completed
  next step: dibCreatePatternBrush で状態を更新し、createPatternBrush で monochrome WMF bitmap の場合に直前 DIB pattern brush を再利用する。
  required context: 直前 DIB がない場合は従来通り RGB pattern brush を作る。
- status: completed
  next step: AwtGdiTest に monochrome WMF CreatePatternBrush が直前 DIB pattern brush を使う描画テストを追加する。
  required context: createWmfMonoBitmap と createRgbDib helper を利用できる。
- status: completed
  next step: focused test と全体 test を実行する。
  required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` で確認する。

goals
- AwtGdi.createPatternBrush が monochrome WMF bitmap かつ直前 DIB pattern brush ありの場合、その DIB image/usage を持つ AwtPatternBrush を返す。
- 直前 DIB pattern brush がない場合の既存挙動は維持する。
- pattern brush による実描画で補正を観測できる。

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

summary
- AwtGdi に直前 DibCreatePatternBrush の image/usage を保持する状態を追加した。
- createPatternBrush が monochrome WMF bitmap を受け取り、直前 DIB pattern brush がある場合はその DIB image/usage を持つ AwtPatternBrush を返すようにした。
- monochrome WMF bitmap 判定 helper を追加した。
- AwtGdiTest に pattern brush の実描画で直前 DIB pattern brush 再利用を確認するテストを追加した。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`、`mvn -q test`、`git diff --check` は成功した。

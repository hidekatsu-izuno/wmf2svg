purpose
- AwtPalette の entries を外部配列変更から独立させ、getEntries() 経由で内部状態を書き換えられないようにする。

context
- AwtPalette constructor は entries を clone している。
- しかし getEntries() が内部配列をそのまま返すため、戻り値を書き換えると selected palette を使う DIB_PAL_COLORS 描画結果が変わる。
- AwtPatternBrush と AwtColorSpace は getter で clone を返す方針にしている。

tasks
- status: completed
  next step: AwtPalette.getEntries() が clone を返すようにする。
  required context: setEntries/resize は内部配列を直接更新する既存挙動を維持する。
- status: completed
  next step: AwtGdiTest に createPalette 入力配列と getEntries() 戻り値の変更が描画に影響しないテストを追加する。
  required context: selectPalette と createPaletted1BppDib で palette entries の効果を観測できる。
- status: completed
  next step: focused test と全体 test を実行する。
  required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` で確認する。

goals
- AwtPalette が作成時 entries を保持し続ける。
- getEntries() の戻り値を変更しても AwtPalette 内部状態が変わらない。
- setPaletteEntries/resizePalette の既存更新経路は維持される。

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtPalette.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

summary
- AwtPalette.getEntries() が内部 entries 配列ではなく clone を返すようにした。
- AwtGdiTest に createPalette 入力配列と getEntries() 戻り値の変更が DIB_PAL_COLORS 描画へ影響しないことを確認するテストを追加した。
- setPaletteEntries/resizePalette の既存更新経路は維持した。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`、`mvn -q test`、`git diff --check` は成功した。

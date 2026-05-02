purpose
- AwtGdi.createPalette の version 引数が AwtPalette に保持されない不十分な状態実装を修正する。

context
- GdiPalette は getVersion() を公開している。
- AwtGdi.createPalette(int version, int[] palEntry) は version を受け取るが、AwtPalette は常に 0x300 を返している。
- EMF/WMF/SVG 側の Palette 実装は version を保持している。

tasks
- status: completed
  next step: AwtPalette に version フィールドを追加し、constructor で受け取る。
  required context: entries の resize/setEntries 挙動は維持する。
- status: completed
  next step: AwtGdi.createPalette から version を渡す。
  required context: createPalette は AwtPalette の唯一の生成入口。
- status: completed
  next step: AwtGdiTest に version 保持のテストを追加する。
  required context: GdiPalette.getVersion() で直接観測できる。
- status: completed
  next step: focused test と全体 test を実行する。
  required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` で確認する。

goals
- AwtPalette.getVersion() が createPalette の version 引数を返す。
- palette entries の既存挙動を壊さない。

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtPalette.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

summary
- AwtPalette に version フィールドを追加し、getVersion() が createPalette の version 引数を返すようにした。
- AwtGdi.createPalette から AwtPalette constructor へ version を渡すようにした。
- AwtGdiTest に createPalette の version 保持テストを追加した。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` は成功した。

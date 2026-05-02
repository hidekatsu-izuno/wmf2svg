purpose
- AwtPatternBrush と AwtGdi の直前 DIB pattern brush 状態を入力 byte 配列の後続変更から独立させる。

context
- AwtPatternBrush は image をそのまま保持し、getPattern() でも内部配列を返している。
- AwtGdi.dibCreatePatternBrush は直前 DIB pattern brush の image もそのまま保持している。
- AwtColorSpace や AwtPalette は constructor 入力を clone しており、Awt の GDI object state は作成時 snapshot として扱うのが自然。

tasks
- status: completed
  next step: AwtPatternBrush が constructor 入力を clone し、getPattern() でも clone を返すようにする。
  required context: AwtGdi の描画処理は package-private の image フィールドを読むため、内部 field は維持する。
- status: completed
  next step: AwtGdi.dibCreatePatternBrush が直前 DIB pattern brush image を clone して保持するようにする。
  required context: createPatternBrush の monochrome WMF fallback はこの保持状態を使う。
- status: completed
  next step: AwtGdiTest に入力配列と getPattern() 経由配列の変更が描画に影響しないテストを追加する。
  required context: createRgbDib helper の pixel bytes を変更して確認できる。
- status: completed
  next step: focused test と全体 test を実行する。
  required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` で確認する。

goals
- AwtPatternBrush が作成時の pattern bytes を保持し続ける。
- getPattern() の戻り値を変更しても AwtPatternBrush 内部状態が変わらない。
- 直前 DIB pattern brush fallback も入力配列の後続変更に影響されない。

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtPatternBrush.java
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

summary
- AwtPatternBrush が constructor 入力 image を clone し、getPattern() でも clone を返すようにした。
- AwtGdi.dibCreatePatternBrush の直前 DIB pattern brush image 状態も clone して保持するようにした。
- AwtGdiTest に入力 DIB 配列と getPattern() 戻り値の変更が描画へ影響しないこと、monochrome WMF fallback も DIB snapshot を使うことを確認するテストを追加した。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test`、`mvn -q test`、`git diff --check` は成功した。

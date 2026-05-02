purpose
- AwtGdi の ExtCreateRegion で、EMF から渡される XFORM が反映されていない未実装部分を実装する。

context
- EmfParser は ExtCreateRegion の変換行列を float[6] として Gdi.extCreateRegion に渡している。
- SvgGdi はリージョン側へ xform を保持しているが、AwtGdi は createRegionArea(rgnData, count) のみで xform を無視している。
- AwtGdi のリージョンは Java2D Area として保持され、描画・クリップ時にその Area が使われる。

tasks
- status: completed
  next step: AwtGdi の createRegionArea を xform 対応に拡張する。
  required context: xform は {m11, m12, m21, m22, dx, dy} で、Java2D AffineTransform のコンストラクタ順と対応する。
- status: completed
  next step: extCreateRegion の xform が実際に描画位置へ反映されるテストを追加する。
  required context: RGNDATA の矩形リストは 32 バイトヘッダ後に RECT が続く。
- status: completed
  next step: AwtGdiTest と全体テストを実行する。
  required context: 既存変更が多いため、AwtGdiTest の focused test と mvn test の両方で確認する。

goals
- ExtCreateRegion の xform による移動、回転、拡大縮小、せん断が AWT リージョン Area に反映される。
- xform が null の既存挙動を維持する。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` が成功する。

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

summary
- AwtGdi.extCreateRegion が xform を受け取り、RGNDATA の矩形を変換済み Path2D として Area に追加するようにした。
- xform が null または identity の場合は従来どおり toRectangle を使い、既存挙動を維持した。
- `testExtCreateRegionAppliesXform` を追加し、変換で移動したリージョンだけが塗られることを確認した。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` は成功した。

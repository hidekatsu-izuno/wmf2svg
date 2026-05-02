purpose
- AwtGdi/AwtDc の DC 状態管理で、origin offset と scale extent の old 出力が不十分な箇所を実装する。

context
- AwtDc は window/viewport origin を base と offset に分けて保持している。
- OffsetWindowOrgEx/OffsetViewportOrgEx 後に SetWindowOrgEx/SetViewportOrgEx すると、offset が残ったままになり、以降の座標変換に古い offset が混ざる。
- ScaleWindowExtEx/ScaleViewportExtEx は old 出力パラメータを受け取るが、現在値を返していない。

tasks
- status: completed
  next step: AwtDc の effective origin/extent を整理し、Set*OrgEx と Offset*OrgEx の old 出力と状態更新を修正する。
  required context: toAbsoluteX/Y は window origin を wx+wox、viewport origin を vx+vox として扱っている。
- status: completed
  next step: ScaleWindowExtEx/ScaleViewportExtEx が現在の effective extent を old に返すよう実装する。
  required context: effective extent は base extent と scale factor の積として扱う。
- status: completed
  next step: AwtGdiTest に origin reset と scale old 出力のテストを追加する。
  required context: createMappedGdi は window/viewport ext を同値に設定して 1:1 マッピングを作る。
- status: completed
  next step: focused test と全体 test を実行する。
  required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` で確認する。

goals
- Offset*OrgEx 後の Set*OrgEx が古い offset を引きずらない。
- Set*OrgEx/Offset*OrgEx の old が現在の effective origin を返す。
- Scale*ExtEx の old が現在の effective extent を返す。
- 既存描画とテストを壊さない。

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

summary
- AwtDc の SetWindowOrgEx/SetViewportOrgEx が、既存 offset を含む effective origin を old に返したうえで offset をリセットするようにした。
- OffsetWindowOrgEx/OffsetViewportOrgEx の old も、offset 単体ではなく現在の effective origin を返すようにした。
- ScaleWindowExtEx/ScaleViewportExtEx が現在の effective extent を old に返すようにした。
- AwtGdiTest に origin reset と scale old 出力の回帰テストを追加した。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` は成功した。

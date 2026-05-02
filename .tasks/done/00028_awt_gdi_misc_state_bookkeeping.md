purpose
- AwtGdi の残りの DC 状態 API のうち、描画へ直接反映しづらい layout/mapperFlags/relAbs を状態保持として検証可能にする。

context
- AwtDc は setLayout/setMapperFlags/setRelAbs の値を保持しているが、getter がなく AwtGdiTest から状態保持や save/restore を検証できない。
- MS-WMF では META_SETRELABS は undefined and MUST be ignored とされているため、描画ロジックへ相対座標として反映する対象ではない。
- layout と mapperFlags は Java2D の描画へ完全に対応させるには追加設計が必要だが、API 状態として保存・復元されることは進められる。

tasks
- status: completed
  next step: AwtDc に layout/mapperFlags/relAbsMode の getter を追加する。
  required context: AwtDc は Cloneable で save/restore 時に DC 全体が clone される。
- status: completed
  next step: AwtGdi に package-private getter を追加し、テストから状態を検証できるようにする。
  required context: 既存の ICM/colorAdjustment も package-private getter で検証している。
- status: completed
  next step: AwtGdiTest に保存・復元を含む状態保持テストを追加する。
  required context: setRelAbs は描画へ反映せず、状態保持のみ確認する。
- status: completed
  next step: focused test と全体 test を実行する。
  required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` で確認する。

goals
- layout/mapperFlags/relAbs の状態保持と save/restore がテストで保証される。
- META_SETRELABS を描画相対座標として誤実装しない。
- 既存描画に影響を出さない。

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtDc.java
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

summary
- AwtDc に relAbs/layout/mapperFlags の getter を追加した。
- AwtGdi に package-private getter を追加し、既存の状態 bookkeeping と同じ形でテスト可能にした。
- layout/mapperFlags/relAbs が save/restore に追従する回帰テストを追加した。
- SETRELABS は MS-WMF で undefined and MUST be ignored とされるため、描画相対座標としては扱わない方針を維持した。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` は成功した。

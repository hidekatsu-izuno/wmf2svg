purpose
- AwtGdi の SaveDC/RestoreDC で、選択中 palette と color space が復元されない漏れを実装する。

context
- AwtGdi は selectedPalette と selectedColorSpace を AwtDc の外側に保持している。
- SaveDC は AwtDc と clip だけを保存しているため、RestoreDC 後も selectedPalette/selectedColorSpace が新しい値のまま残る。
- selectedPalette は DIB_PAL_COLORS の DIB デコード時に実描画へ影響する。

tasks
- status: completed
  next step: AwtSavedDc に selectedPalette と selectedColorSpace を含める。
  required context: AwtSavedDc は AwtGdi の private static class で、保存・復元箇所は seveDC/restoreLastDC。
- status: completed
  next step: RestoreDC 時に selectedPalette と selectedColorSpace を復元する。
  required context: palette/color space オブジェクト自体は GdiObject として扱われ、ここでは選択参照を保存する。
- status: completed
  next step: DIB_PAL_COLORS の描画で palette restore が効く回帰テストを追加する。
  required context: applyPaletteToDib は selectedPalette の entries を使って DIB color table の WORD index を RGBQUAD に変換する。
- status: completed
  next step: focused test と全体 test を実行する。
  required context: `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` で確認する。

goals
- SaveDC/RestoreDC で selectedPalette/selectedColorSpace が復元される。
- DIB_PAL_COLORS の色解決が RestoreDC 後の palette に従う。
- 既存描画に回帰を出さない。

file list
- src/main/java/net/arnx/wmf2svg/gdi/awt/AwtGdi.java
- src/test/java/net/arnx/wmf2svg/gdi/awt/AwtGdiTest.java

summary
- AwtSavedDc に selectedPalette と selectedColorSpace を追加し、SaveDC/RestoreDC で選択参照が復元されるようにした。
- RestoreDC 後の selectedPalette が DIB_PAL_COLORS の色解決へ反映される回帰テストを追加した。
- selectedColorSpace も setColorSpace の戻り値で復元を確認した。
- `mvn -q -Dtest=net.arnx.wmf2svg.gdi.awt.AwtGdiTest test` と `mvn -q test` は成功した。

package net.arnx.wmf2svg.gdi.awt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.emf.EmfGdi;

public class AwtGdiTest {
	@Test
	public void testEscapeReplaysStandaloneEmfPayload() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.escape(createEscapeRecord(0x1234, createLineEmf()));
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image) > 0);
		assertEquals(0, alphaAtBottomRight(image));
	}

	@Test
	public void testEscapeUsesEmfFrameForStandaloneEmfPayload() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.escape(createEscapeRecord(0x1234, createBackgroundEmf()));
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image) > 100 * 100);
		assertEquals(0, alphaAtBottomRight(image));
	}

	@Test
	public void testPendingEmfUsesOuterMappingWhenMappingFollowsComment() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.escape(createEscapeRecord(0x1234, createBackgroundEmf()));
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(100, 100, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(200, 200, null);
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(200, image.getWidth());
		assertEquals(200, image.getHeight());
		assertTrue(countPaintedPixels(image) > 30_000);
	}

	@Test
	public void testPlaceableViewportExtRemainsScaledToCanvasPixels() {
		AwtGdi gdi = new AwtGdi();
		gdi.placeableHeader(0, 0, 1000, 1000, 1000);
		gdi.header();
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(1000, 1000, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(1000, 1000, null);
		gdi.rectangle(900, 900, 1000, 1000);
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(144, image.getWidth());
		assertEquals(144, image.getHeight());
		assertTrue(((image.getRGB(140, 140) >>> 24) & 0xFF) != 0);
	}

	@Test
	public void testPolyPolygonRendersCompoundShapeAsSinglePath() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(100, 100, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(100, 100, null);
		gdi.setPolyFillMode(Gdi.WINDING);
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0, 0));
		gdi.selectObject(gdi.createPenIndirect(GdiPen.PS_NULL, 1, 0));
		gdi.polyPolygon(new Point[][]{{new Point(10, 10), new Point(90, 10), new Point(90, 90), new Point(10, 90)},
				{new Point(30, 30), new Point(30, 70), new Point(70, 70), new Point(70, 30)}});
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0, (image.getRGB(50, 50) >>> 24) & 0xFF);
		assertTrue(((image.getRGB(20, 20) >>> 24) & 0xFF) != 0);
	}

	@Test
	public void testZeroLengthLineToDoesNotPaint() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(100, 100, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(100, 100, null);
		gdi.selectObject(gdi.createPenIndirect(GdiPen.PS_SOLID, 10, 0));
		gdi.moveToEx(50, 50, null);
		gdi.lineTo(50, 50);
		gdi.footer();

		assertEquals(0, countPaintedPixels(gdi.getImage()));
	}

	@Test
	public void testClockwiseArcUsesGdiSweepDirection() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(100, 100, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(100, 100, null);
		gdi.setArcDirection(Gdi.AD_CLOCKWISE);
		gdi.selectObject(gdi.createPenIndirect(GdiPen.PS_SOLID, 2, 0));
		gdi.arc(0, 0, 100, 100, 0, 50, 50, 0);
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image, 0, 0, 50, 50) > 0);
		assertEquals(0, countPaintedPixels(image, 0, 60, 50, 40));
	}

	@Test
	public void testBitmapRop3UsesTruthTable() {
		AwtGdi gdi = createOnePixelGdi();
		gdi.setPixel(0, 0, 0xFF0000);
		gdi.dibBitBlt(createRgbDib(0xFF0000), 0, 0, 1, 1, 0, 0, Gdi.SRCINVERT);

		assertEquals(0xFF00FF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testBitmapRop3UsesPatternOperand() {
		AwtGdi gdi = createOnePixelGdi();
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00FF00, 0));
		gdi.dibBitBlt(createRgbDib(0xFFFFFF), 0, 0, 1, 1, 0, 0, Gdi.MERGECOPY);

		assertEquals(0x00FF00, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testBitmapRop4StyleValueUsesBackgroundRop() {
		AwtGdi gdi = createOnePixelGdi();
		gdi.dibBitBlt(createRgbDib(0xFF00FF), 0, 0, 1, 1, 0, 0, 0xCCAA0029L);

		assertEquals(0xFF00FF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testMaskBltUsesForegroundAndBackgroundRops() {
		AwtGdi gdi = createMappedGdi(2, 1);
		gdi.setPixel(0, 0, 0xFF0000);
		gdi.setPixel(1, 0, 0xFF0000);
		gdi.maskBlt(createRgbDib(0xFF0000, 0xFF0000), 0, 0, 2, 1, 0, 0, createRgbDib(0xFFFFFF, 0x000000), 0, 0,
				0xCC000000L | Gdi.NOTSRCCOPY);

		assertEquals(0x00FFFF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0xFF0000, gdi.getImage().getRGB(1, 0) & 0x00FFFFFF);
	}

	@Test
	public void testMaskBltKeepsDestinationForSourceBackgroundColor() {
		AwtGdi gdi = createOnePixelGdi();
		gdi.setPixel(0, 0, 0x00FF00);
		gdi.maskBlt(createRgbDib(0xFF00FF), 0, 0, 1, 1, 0, 0, createRgbDib(0x000000), 0, 0, 0xCCAA0029L,
				Integer.valueOf(0x00FF00FF));

		assertEquals(0x00FF00, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testMaskBltKeepsDestinationForDetectedSourcePaddingColor() {
		AwtGdi gdi = createMappedGdi(2, 1);
		gdi.setPixel(0, 0, 0x00FF00);
		gdi.setPixel(1, 0, 0x00FF00);
		gdi.maskBlt(createRgbDib(0xFF00FF, 0x0000FF), 0, 0, 2, 1, 0, 0, createRgbDib(0x000000, 0x000000), 0, 0,
				0xCCAA0029L);

		assertEquals(0x00FF00, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0x0000FF, gdi.getImage().getRGB(1, 0) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusSolidFillRecordsRenderBasicShapes() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusSolidFillComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x336699, image.getRGB(5, 5) & 0x00FFFFFF);
		assertTrue(((image.getRGB(26, 16) >>> 24) & 0xFF) > 0);
		assertTrue(((image.getRGB(7, 32) >>> 24) & 0xFF) > 0);
	}

	@Test
	public void testEmfPlusObjectsRenderPathAndLines() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusObjectDrawingComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x336699, image.getRGB(5, 5) & 0x00FFFFFF);
		assertEquals(0xFF0000, image.getRGB(25, 5) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusDrawStringRendersText() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusDrawStringComment());
		gdi.footer();

		assertTrue(countPaintedPixels(gdi.getImage(), 5, 8, 80, 20) > 0);
	}

	@Test
	public void testEmfPlusBitmapDrawImageRendersPixels() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusBitmapDrawImageComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0xFF0000, image.getRGB(12, 15) & 0x00FFFFFF);
		assertEquals(0x00FF00, image.getRGB(25, 15) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusDrawImageSourceRectDoesNotStretchClippedBitmap() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusOversizedSourceDrawImageComment(false));
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x00FF00, image.getRGB(12, 15) & 0x00FFFFFF);
		assertEquals(0, (image.getRGB(25, 15) >>> 24) & 0xFF);
	}

	@Test
	public void testEmfPlusDrawImageUsesImageAttributesClampColor() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusOversizedSourceDrawImageComment(true));
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x00FF00, image.getRGB(12, 15) & 0x00FFFFFF);
		assertEquals(0x0000FF, image.getRGB(25, 15) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusDrawImageUsesImageAttributesTileWrapMode() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusTiledSourceDrawImageComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x00FF00, image.getRGB(12, 15) & 0x00FFFFFF);
		assertEquals(0xFF0000, image.getRGB(25, 15) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlus48BppRawBitmapDrawImageRendersPixels() {
		ByteArrayOutputStream pixels = new ByteArrayOutputStream();
		writeShort(pixels, 0);
		writeShort(pixels, 0);
		writeShort(pixels, 0xFFFF);
		writeShort(pixels, 0);
		writeShort(pixels, 0xFFFF);
		writeShort(pixels, 0);

		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusRawBitmapDrawImageComment(0x0010300C, 12, pixels.toByteArray()));
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0xFF0000, image.getRGB(12, 15) & 0x00FFFFFF);
		assertEquals(0x00FF00, image.getRGB(25, 15) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlus64BppPArgbRawBitmapDrawImageUnpremultipliesAlpha() {
		ByteArrayOutputStream pixels = new ByteArrayOutputStream();
		writeShort(pixels, 0);
		writeShort(pixels, 0);
		writeShort(pixels, 0x8000);
		writeShort(pixels, 0x8000);
		writeShort(pixels, 0);
		writeShort(pixels, 0x4000);
		writeShort(pixels, 0);
		writeShort(pixels, 0x8000);

		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusRawBitmapDrawImageComment(0x001A400E, 16, pixels.toByteArray()));
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(((image.getRGB(12, 15) >>> 16) & 0xFF) > 240);
		assertTrue(((image.getRGB(25, 15) >>> 8) & 0xFF) > 120);
	}

	@Test
	public void testEmfPlusTiffBitmapDrawImageRendersPixels() throws Exception {
		assumeTrue(ImageIO.getImageWritersByFormatName("tiff").hasNext());
		assumeTrue(ImageIO.getImageReadersByFormatName("tiff").hasNext());
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusTiffBitmapDrawImageComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0xFF0000, image.getRGB(12, 15) & 0x00FFFFFF);
		assertEquals(0x00FF00, image.getRGB(25, 15) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusTextureBrushFillsWithBitmapPattern() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusTextureBrushComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0xFF0000, image.getRGB(2, 2) & 0x00FFFFFF);
		assertEquals(0x00FF00, image.getRGB(3, 2) & 0x00FFFFFF);
		assertEquals(0xFF0000, image.getRGB(4, 2) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusLinearGradientBrushUsesPresetColors() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusLinearGradientPresetComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		int left = image.getRGB(2, 5);
		int middle = image.getRGB(15, 5);
		int right = image.getRGB(28, 5);
		assertTrue(((left >>> 16) & 0xFF) > ((left >>> 8) & 0xFF));
		assertTrue(((middle >>> 8) & 0xFF) > ((middle >>> 16) & 0xFF));
		assertTrue((right & 0xFF) > ((right >>> 8) & 0xFF));
	}

	@Test
	public void testEmfPlusLinearGradientBrushUsesTileWrapMode() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusLinearGradientTileWrapComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		int repeated = image.getRGB(12, 5);
		assertTrue(((repeated >>> 16) & 0xFF) > ((repeated >>> 8) & 0xFF));
		assertTrue((repeated & 0xFF) < 80);
	}

	@Test
	public void testEmfPlusHatchBrushFillsWithPattern() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusHatchBrushComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x0000FF, image.getRGB(2, 2) & 0x00FFFFFF);
		assertEquals(0xFF0000, image.getRGB(2, 4) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusRenderingOriginOffsetsHatchPattern() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusRenderingOriginHatchBrushComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x0000FF, image.getRGB(2, 4) & 0x00FFFFFF);
		assertEquals(0xFF0000, image.getRGB(2, 5) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusTSGraphicsRenderingOriginOffsetsHatchPattern() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusTSGraphicsRenderingOriginHatchBrushComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x0000FF, image.getRGB(2, 4) & 0x00FFFFFF);
		assertEquals(0xFF0000, image.getRGB(2, 5) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusPathGradientBrushFillsRadially() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusPathGradientComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		int center = image.getRGB(15, 15);
		int edge = image.getRGB(2, 15);
		assertTrue(((center >>> 16) & 0xFF) > ((edge >>> 16) & 0xFF));
		assertTrue(((center >>> 8) & 0xFF) > ((edge >>> 8) & 0xFF));
		assertTrue((center & 0xFF) > (edge & 0xFF));
	}

	@Test
	public void testEmfPlusPathGradientBrushUsesEllipseBounds() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusEllipticalPathGradientComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		int center = image.getRGB(15, 5) & 0xFF;
		int bottom = image.getRGB(15, 9) & 0xFF;
		assertTrue(center > 200);
		assertTrue(bottom < 120);
	}

	@Test
	public void testEmfPlusClipRectClipsAndResets() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusClipRectComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0xFF0000, image.getRGB(6, 6) & 0x00FFFFFF);
		assertEquals(0, (image.getRGB(2, 2) >>> 24) & 0xFF);
		assertEquals(0x0000FF, image.getRGB(22, 22) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusCurveRecordsRenderShapes() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusCurveRecordsComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image, 4, 4, 24, 24) > 0);
		assertTrue(countPaintedPixels(image, 30, 4, 30, 24) > 0);
		assertTrue(countPaintedPixels(image, 4, 32, 42, 18) > 0);
	}

	@Test
	public void testEmfPlusStrokeFillPathRendersFillAndStroke() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusStrokeFillPathComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x336699, image.getRGB(8, 8) & 0x00FFFFFF);
		assertTrue(countPaintedPixels(image, 1, 1, 14, 14) > 20);
	}

	@Test
	public void testEmfPlusFillRegionRendersRegionObject() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusFillRegionComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x336699, image.getRGB(5, 6) & 0x00FFFFFF);
		assertEquals(0, (image.getRGB(1, 1) >>> 24) & 0xFF);
	}

	@Test
	public void testEmfPlusClipPathAndClipRegionClipDrawing() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusClipPathAndRegionComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0xFF0000, image.getRGB(4, 4) & 0x00FFFFFF);
		assertEquals(0, (image.getRGB(16, 16) >>> 24) & 0xFF);
		assertEquals(0x0000FF, image.getRGB(27, 7) & 0x00FFFFFF);
		assertEquals(0, (image.getRGB(21, 7) >>> 24) & 0xFF);
	}

	@Test
	public void testEmfPlusTSClipClipsDrawing() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusTSClipComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0xFF0000, image.getRGB(6, 6) & 0x00FFFFFF);
		assertEquals(0xFF0000, image.getRGB(26, 6) & 0x00FFFFFF);
		assertEquals(0, (image.getRGB(16, 6) >>> 24) & 0xFF);
	}

	@Test
	public void testEmfPlusCompressedTSClipClipsDrawing() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusCompressedTSClipComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0xFF0000, image.getRGB(6, 6) & 0x00FFFFFF);
		assertEquals(0xFF0000, image.getRGB(26, 6) & 0x00FFFFFF);
		assertEquals(0, (image.getRGB(16, 6) >>> 24) & 0xFF);
	}

	@Test
	public void testEmfPlusBeginContainerTransformsDrawing() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusBeginContainerComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x336699, image.getRGB(12, 12) & 0x00FFFFFF);
		assertEquals(0, (image.getRGB(2, 2) >>> 24) & 0xFF);
	}

	@Test
	public void testEmfPlusSourceCopyCompositingCanClearPixels() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusSourceCopyCompositingComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0xFF0000, image.getRGB(2, 2) & 0x00FFFFFF);
		assertEquals(0, (image.getRGB(6, 6) >>> 24) & 0xFF);
	}

	@Test
	public void testEmfPlusPenDashedLineCapRoundsDashes() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusRoundDashCapPenComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue((image.getRGB(6, 10) >>> 24) != 0);
	}

	@Test
	public void testEmfPlusPenTransformScalesStrokeWidth() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusTransformedPenComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue((image.getRGB(10, 23) >>> 24) != 0);
	}

	@Test
	public void testEmfPlusSetTextContrastUpdatesRenderingHint() {
		BufferedImage canvas = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = canvas.createGraphics();
		try {
			AwtGdi gdi = new AwtGdi(graphics, canvas.getWidth(), canvas.getHeight());
			gdi.header();
			gdi.comment(createEmfPlusTextContrastComment(1200));

			assertEquals(Integer.valueOf(120), graphics.getRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST));
			gdi.footer();
		} finally {
			graphics.dispose();
		}
	}

	@Test
	public void testEmfPlusTSGraphicsTextContrastUpdatesRenderingHint() {
		BufferedImage canvas = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = canvas.createGraphics();
		try {
			AwtGdi gdi = new AwtGdi(graphics, canvas.getWidth(), canvas.getHeight());
			gdi.header();
			gdi.comment(createEmfPlusTSGraphicsTextContrastComment(8));

			assertEquals(Integer.valueOf(180), graphics.getRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST));
			gdi.footer();
		} finally {
			graphics.dispose();
		}
	}

	@Test
	public void testEmfPlusDrawDriverStringRendersText() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusDrawDriverStringComment());
		gdi.footer();

		assertTrue(countPaintedPixels(gdi.getImage(), 4, 6, 50, 20) > 0);
	}

	@Test
	public void testEmfPlusVerticalDrawDriverStringStacksText() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusVerticalDrawDriverStringComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image, 4, 6, 18, 20) > 0);
		assertTrue(countPaintedPixels(image, 4, 24, 18, 24) > 0);
		assertEquals(0, countPaintedPixels(image, 28, 6, 24, 18));
	}

	@Test
	public void testEmfPlusDrawStringUsesStringFormatAlignment() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusAlignedDrawStringComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0, countPaintedPixels(image, 0, 0, 20, 25));
		assertTrue(countPaintedPixels(image, 25, 0, 40, 25) > 0);
	}

	@Test
	public void testEmfPlusDrawStringHidesHotkeyPrefix() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusHotkeyHiddenDrawStringComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0, countPaintedPixels(image, 20, 0, 18, 25));
		assertTrue(countPaintedPixels(image, 48, 0, 30, 25) > 0);
	}

	@Test
	public void testEmfPlusDrawStringClipsToLayoutRect() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusClippedDrawStringComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image, 0, 0, 8, 25) > 0);
		assertEquals(0, countPaintedPixels(image, 14, 0, 40, 25));
	}

	private byte[] createLineEmf() throws IOException {
		EmfGdi gdi = new EmfGdi();
		gdi.moveToEx(2, 2, null);
		gdi.lineTo(60, 60);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		return out.toByteArray();
	}

	private byte[] createBackgroundEmf() throws IOException {
		EmfGdi gdi = new EmfGdi();
		gdi.rectangle(0, 0, 100, 100);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		return out.toByteArray();
	}

	private byte[] createEscapeRecord(int escapeFunction, byte[] payload) {
		byte[] record = new byte[4 + payload.length];
		record[0] = (byte) escapeFunction;
		record[1] = (byte) (escapeFunction >>> 8);
		record[2] = (byte) payload.length;
		record[3] = (byte) (payload.length >>> 8);
		System.arraycopy(payload, 0, record, 4, payload.length);
		return record;
	}

	private byte[] createEmfPlusSolidFillComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 12);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFCC6600);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeFloat(payload, 12);
		writeFloat(payload, 12);
		writeEmfPlusRecord(comment, 0x400E, 0x8000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF009966);
		writeInt(payload, 3);
		writeFloat(payload, 2);
		writeFloat(payload, 28);
		writeFloat(payload, 12);
		writeFloat(payload, 28);
		writeFloat(payload, 2);
		writeFloat(payload, 38);
		writeEmfPlusRecord(comment, 0x400C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusObjectDrawingComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4008, 0x0100, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 12);
		writeFloat(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 12);
		writeFloat(payload, 1);
		writeFloat(payload, 1);
		payload.write(0);
		payload.write(1);
		payload.write(1);
		payload.write(0x81);
		writeEmfPlusRecord(comment, 0x4008, 0x0301, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeEmfPlusRecord(comment, 0x4014, 0x0001, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0202, payload.toByteArray());

		payload.reset();
		writeInt(payload, 2);
		writeFloat(payload, 20);
		writeFloat(payload, 5);
		writeFloat(payload, 30);
		writeFloat(payload, 5);
		writeEmfPlusRecord(comment, 0x400D, 0x0002, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDrawStringComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 0);
		writeInt(payload, "EMF+".length());
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 80);
		writeFloat(payload, 20);
		writeUtf16Le(payload, "EMF+");
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusBitmapDrawImageComment() throws IOException {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		byte[] png = createTwoPixelPng();
		writeInt(payload, 0);
		writeInt(payload, 1);
		payload.write(png, 0, png.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x401A, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusOversizedSourceDrawImageComment(boolean clamp) throws IOException {
		return createEmfPlusOversizedSourceDrawImageComment(clamp ? 4 : -1, 0xFF0000FF);
	}

	private byte[] createEmfPlusTiledSourceDrawImageComment() throws IOException {
		return createEmfPlusOversizedSourceDrawImageComment(0, 0);
	}

	private byte[] createEmfPlusOversizedSourceDrawImageComment(int wrapMode, int clampColor) throws IOException {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		byte[] png = createTwoPixelPng();
		writeInt(payload, 0);
		writeInt(payload, 1);
		payload.write(png, 0, png.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500, payload.toByteArray());

		if (wrapMode >= 0) {
			payload.reset();
			writeInt(payload, 0);
			writeInt(payload, 0);
			writeInt(payload, wrapMode);
			writeInt(payload, clampColor);
			writeInt(payload, 1);
			writeInt(payload, 0);
			writeEmfPlusRecord(comment, 0x4008, 0x0801, payload.toByteArray());
		}

		payload.reset();
		writeInt(payload, wrapMode >= 0 ? 1 : 0);
		writeInt(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x401A, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTiffBitmapDrawImageComment() throws IOException {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		byte[] tiff = createTwoPixelImage("tiff");
		writeInt(payload, 0);
		writeInt(payload, 1);
		payload.write(tiff, 0, tiff.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x401A, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusRawBitmapDrawImageComment(int pixelFormat, int stride, byte[] pixels) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 2);
		writeInt(payload, 1);
		writeInt(payload, stride);
		writeInt(payload, pixelFormat);
		writeInt(payload, 0);
		payload.write(pixels, 0, pixels.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x401A, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTextureBrushComment() throws IOException {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		byte[] png = createTwoPixelPng();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		payload.write(png, 0, png.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0100, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 2);
		writeFloat(payload, 6);
		writeFloat(payload, 4);
		writeEmfPlusRecord(comment, 0x400A, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusLinearGradientPresetComment() {
		return createEmfPlusLinearGradientComment(0, 30, 10, 30, 10, 0x00000004);
	}

	private byte[] createEmfPlusLinearGradientTileWrapComment() {
		return createEmfPlusLinearGradientComment(0, 10, 10, 30, 10, 0x00000004);
	}

	private byte[] createEmfPlusLinearGradientComment(int wrapMode, float gradientWidth, float gradientHeight,
			float fillWidth, float fillHeight, int brushDataFlags) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, brushDataFlags);
		writeInt(payload, wrapMode);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, gradientWidth);
		writeFloat(payload, gradientHeight);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0.5f);
		writeFloat(payload, 1);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF00FF00);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0100, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, fillWidth);
		writeFloat(payload, fillHeight);
		writeEmfPlusRecord(comment, 0x400A, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusHatchBrushComment() {
		return createEmfPlusHatchBrushComment(0, 0);
	}

	private byte[] createEmfPlusRenderingOriginHatchBrushComment() {
		return createEmfPlusHatchBrushComment(0, 1);
	}

	private byte[] createEmfPlusTSGraphicsRenderingOriginHatchBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		payload.write(0);
		payload.write(0);
		payload.write(0);
		payload.write(0);
		writeShort(payload, 0);
		writeShort(payload, 1);
		writeShort(payload, 0);
		payload.write(0);
		payload.write(0);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeEmfPlusRecord(comment, 0x4039, 0, payload.toByteArray());
		appendEmfPlusHatchBrushDraw(comment);
		return comment.toByteArray();
	}

	private byte[] createEmfPlusHatchBrushComment(int renderingOriginX, int renderingOriginY) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		if (renderingOriginX != 0 || renderingOriginY != 0) {
			writeInt(payload, renderingOriginX);
			writeInt(payload, renderingOriginY);
			writeEmfPlusRecord(comment, 0x401D, 0, payload.toByteArray());
			payload.reset();
		}
		appendEmfPlusHatchBrushDraw(comment);
		return comment.toByteArray();
	}

	private void appendEmfPlusHatchBrushDraw(ByteArrayOutputStream comment) {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0100, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 8);
		writeFloat(payload, 8);
		writeEmfPlusRecord(comment, 0x400A, 0x0000, payload.toByteArray());
	}

	private byte[] createEmfPlusPathGradientComment() {
		return createEmfPlusPathGradientComment(30, 30);
	}

	private byte[] createEmfPlusEllipticalPathGradientComment() {
		return createEmfPlusPathGradientComment(30, 10);
	}

	private byte[] createEmfPlusPathGradientComment(float width, float height) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFFFFFF);
		writeFloat(payload, width / 2);
		writeFloat(payload, height / 2);
		writeInt(payload, 1);
		writeInt(payload, 0xFF000000);
		writeInt(payload, 4);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, width);
		writeFloat(payload, 0);
		writeFloat(payload, width);
		writeFloat(payload, height);
		writeFloat(payload, 0);
		writeFloat(payload, height);
		writeEmfPlusRecord(comment, 0x4008, 0x0100, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, width);
		writeFloat(payload, height);
		writeEmfPlusRecord(comment, 0x400A, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createTwoPixelPng() throws IOException {
		return createTwoPixelImage("png");
	}

	private byte[] createTwoPixelImage(String format) throws IOException {
		BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, 0xFFFF0000);
		image.setRGB(1, 0, 0xFF00FF00);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, format, out);
		return out.toByteArray();
	}

	private byte[] createEmfPlusClipRectComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeFloat(payload, 5);
		writeFloat(payload, 5);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x4032, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4031, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, 1);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeFloat(payload, 5);
		writeFloat(payload, 5);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusCurveRecordsComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		writeEmfPlusSolidPenObject(comment, 0, 0xFFFF0000, 3);
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0xFF336699);
		writeFloat(payload, 0);
		writeFloat(payload, 360);
		writeFloat(payload, 5);
		writeFloat(payload, 5);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x4010, 0x8000, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 0);
		writeFloat(payload, 270);
		writeFloat(payload, 32);
		writeFloat(payload, 5);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x4012, 0x0000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 4);
		writeFloat(payload, 5);
		writeFloat(payload, 45);
		writeFloat(payload, 15);
		writeFloat(payload, 30);
		writeFloat(payload, 30);
		writeFloat(payload, 55);
		writeFloat(payload, 45);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x4019, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusStrokeFillPathComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		writeEmfPlusSolidBrushObject(comment, 0, 0xFF336699);
		writeEmfPlusSolidPenObject(comment, 1, 0xFFFF0000, 2);
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 2);
		writeFloat(payload, 14);
		writeFloat(payload, 2);
		writeFloat(payload, 14);
		writeFloat(payload, 14);
		writeFloat(payload, 2);
		writeFloat(payload, 14);
		payload.write(0);
		payload.write(1);
		payload.write(1);
		payload.write(0x81);
		writeEmfPlusRecord(comment, 0x4008, 0x0302, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 0);
		writeEmfPlusRecord(comment, 0x4037, 0x0002, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusFillRegionComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		writeEmfPlusRectRegionObject(comment, 0, 3, 4, 10, 12);

		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4013, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusClipPathAndRegionComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 2);
		writeFloat(payload, 14);
		writeFloat(payload, 2);
		writeFloat(payload, 2);
		writeFloat(payload, 14);
		writeFloat(payload, 2);
		writeFloat(payload, 2);
		payload.write(0);
		payload.write(1);
		payload.write(1);
		payload.write(0x81);
		writeEmfPlusRecord(comment, 0x4008, 0x0300, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4033, 0x0000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4031, 0, payload.toByteArray());

		writeEmfPlusRectRegionObject(comment, 1, 25, 5, 8, 8);

		payload.reset();
		writeEmfPlusRecord(comment, 0x4034, 0x0001, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, 1);
		writeFloat(payload, 20);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTSClipComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeShort(payload, 5);
		writeShort(payload, 5);
		writeShort(payload, 10);
		writeShort(payload, 10);
		writeShort(payload, 25);
		writeShort(payload, 5);
		writeShort(payload, 30);
		writeShort(payload, 10);
		writeEmfPlusRecord(comment, 0x403A, 2, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 35);
		writeFloat(payload, 15);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusCompressedTSClipComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeCompressedTSClipValue(payload, 5);
		writeCompressedTSClipValue(payload, 5);
		writeCompressedTSClipValue(payload, 10);
		writeCompressedTSClipValue(payload, 5);
		writeCompressedTSClipValue(payload, 20);
		writeCompressedTSClipValue(payload, 0);
		writeCompressedTSClipValue(payload, 20);
		writeCompressedTSClipValue(payload, 5);
		writeEmfPlusRecord(comment, 0x403A, 0x8002, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 35);
		writeFloat(payload, 15);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusBeginContainerComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeInt(payload, 0);
		writeEmfPlusRecord(comment, 0x4027, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 5);
		writeFloat(payload, 5);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4029, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusSourceCopyCompositingComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4023, 1, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0x00000000);
		writeInt(payload, 1);
		writeFloat(payload, 5);
		writeFloat(payload, 5);
		writeFloat(payload, 4);
		writeFloat(payload, 4);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTextContrastComment(int contrast) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		writeEmfPlusRecord(comment, 0x4020, contrast, new byte[0]);
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTSGraphicsTextContrastComment(int contrast) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		payload.write(0);
		payload.write(0);
		payload.write(0);
		payload.write(0);
		writeShort(payload, 0);
		writeShort(payload, 0);
		writeShort(payload, contrast);
		payload.write(0);
		payload.write(0);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeEmfPlusRecord(comment, 0x4039, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusRoundDashCapPenComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x00000060);
		writeInt(payload, 2);
		writeFloat(payload, 4);
		writeInt(payload, 2);
		writeInt(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 30);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400D, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTransformedPenComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0x00000000);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 40);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x401E, 3, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x00000001);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeFloat(payload, 4);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 4);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 2);
		writeFloat(payload, 5);
		writeFloat(payload, 20);
		writeFloat(payload, 35);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400D, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDrawDriverStringComment() {
		return createEmfPlusDrawDriverStringComment(1, new double[][]{{5, 6}, {18, 6}, {31, 6}});
	}

	private byte[] createEmfPlusVerticalDrawDriverStringComment() {
		return createEmfPlusDrawDriverStringComment(7, new double[][]{{5, 6}});
	}

	private byte[] createEmfPlusDrawDriverStringComment(int options, double[][] positions) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, options);
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeUtf16Le(payload, "ABC");
		writeShort(payload, 0);
		for (int i = 0; i < positions.length; i++) {
			writeFloat(payload, (float) positions[i][0]);
			writeFloat(payload, (float) positions[i][1]);
		}
		writeEmfPlusRecord(comment, 0x4036, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusAlignedDrawStringComment() {
		return createEmfPlusFormattedDrawStringComment("A", 0, 0, 60);
	}

	private byte[] createEmfPlusHotkeyHiddenDrawStringComment() {
		return createEmfPlusFormattedDrawStringComment("A&A&A&A", 2, 0, 80);
	}

	private byte[] createEmfPlusClippedDrawStringComment() {
		return createEmfPlusFormattedDrawStringComment("MMMM", 0, 0, 8, 60);
	}

	private byte[] createEmfPlusFormattedDrawStringComment(String text, int hotkeyPrefix, float tracking,
			float rectWidth) {
		return createEmfPlusFormattedDrawStringComment(text, hotkeyPrefix, tracking, rectWidth, rectWidth);
	}

	private byte[] createEmfPlusFormattedDrawStringComment(String text, int hotkeyPrefix, float tracking,
			float rectWidth, float canvasWidth) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		if (canvasWidth > rectWidth) {
			writeInt(payload, 0x00000000);
			writeInt(payload, 1);
			writeFloat(payload, 0);
			writeFloat(payload, 0);
			writeFloat(payload, canvasWidth);
			writeFloat(payload, 20);
			writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
			payload.reset();
		}
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, hotkeyPrefix);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, tracking);
		writeFloat(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeEmfPlusRecord(comment, 0x4008, 0x0701, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeInt(payload, text.length());
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, rectWidth);
		writeFloat(payload, 20);
		writeUtf16Le(payload, text);
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private void writeEmfPlusSolidBrushObject(ByteArrayOutputStream comment, int objectId, int argb) {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, argb);
		writeEmfPlusRecord(comment, 0x4008, 0x0100 | objectId, payload.toByteArray());
	}

	private void writeEmfPlusSolidPenObject(ByteArrayOutputStream comment, int objectId, int argb, float width) {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, width);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, argb);
		writeEmfPlusRecord(comment, 0x4008, 0x0200 | objectId, payload.toByteArray());
	}

	private void writeEmfPlusRectRegionObject(ByteArrayOutputStream comment, int objectId, float x, float y,
			float width, float height) {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 0x10000000);
		writeFloat(payload, x);
		writeFloat(payload, y);
		writeFloat(payload, width);
		writeFloat(payload, height);
		writeEmfPlusRecord(comment, 0x4008, 0x0400 | objectId, payload.toByteArray());
	}

	private ByteArrayOutputStream createEmfPlusComment() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write('E');
		out.write('M');
		out.write('F');
		out.write('+');
		return out;
	}

	private void writeEmfPlusRecord(ByteArrayOutputStream out, int type, int flags, byte[] data) {
		writeShort(out, type);
		writeShort(out, flags);
		writeInt(out, data.length + 12);
		writeInt(out, data.length);
		out.write(data, 0, data.length);
	}

	private void writeShort(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		out.write((value >>> 8) & 0xFF);
	}

	private void writeInt(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		out.write((value >>> 8) & 0xFF);
		out.write((value >>> 16) & 0xFF);
		out.write((value >>> 24) & 0xFF);
	}

	private void writeFloat(ByteArrayOutputStream out, float value) {
		writeInt(out, Float.floatToIntBits(value));
	}

	private void writeUtf16Le(ByteArrayOutputStream out, String value) {
		for (int i = 0; i < value.length(); i++) {
			writeShort(out, value.charAt(i));
		}
	}

	private void writeCompressedTSClipValue(ByteArrayOutputStream out, int value) {
		out.write(0x80 | (value & 0x7F));
	}

	private AwtGdi createOnePixelGdi() {
		return createMappedGdi(1, 1);
	}

	private AwtGdi createMappedGdi(int width, int height) {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(width, height, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(width, height, null);
		return gdi;
	}

	private byte[] createRgbDib(int... rgbs) {
		int width = rgbs.length;
		int stride = ((width * 24 + 31) / 32) * 4;
		byte[] dib = new byte[40 + stride];
		writeInt32(dib, 0, 40);
		writeInt32(dib, 4, width);
		writeInt32(dib, 8, 1);
		writeUInt16(dib, 12, 1);
		writeUInt16(dib, 14, 24);
		writeInt32(dib, 20, stride);
		for (int x = 0; x < width; x++) {
			int rgb = rgbs[x];
			int offset = 40 + x * 3;
			dib[offset] = (byte) (rgb & 0xFF);
			dib[offset + 1] = (byte) ((rgb >>> 8) & 0xFF);
			dib[offset + 2] = (byte) ((rgb >>> 16) & 0xFF);
		}
		return dib;
	}

	private void writeInt32(byte[] data, int offset, int value) {
		data[offset] = (byte) value;
		data[offset + 1] = (byte) (value >>> 8);
		data[offset + 2] = (byte) (value >>> 16);
		data[offset + 3] = (byte) (value >>> 24);
	}

	private void writeUInt16(byte[] data, int offset, int value) {
		data[offset] = (byte) value;
		data[offset + 1] = (byte) (value >>> 8);
	}

	private int countPaintedPixels(BufferedImage image) {
		int count = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if ((image.getRGB(x, y) >>> 24) != 0) {
					count++;
				}
			}
		}
		return count;
	}

	private int countPaintedPixels(BufferedImage image, int x, int y, int width, int height) {
		int count = 0;
		for (int py = y; py < y + height; py++) {
			for (int px = x; px < x + width; px++) {
				if ((image.getRGB(px, py) >>> 24) != 0) {
					count++;
				}
			}
		}
		return count;
	}

	private int alphaAtBottomRight(BufferedImage image) {
		return (image.getRGB(image.getWidth() - 1, image.getHeight() - 1) >>> 24) & 0xFF;
	}
}

package net.arnx.wmf2svg.gdi.awt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

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

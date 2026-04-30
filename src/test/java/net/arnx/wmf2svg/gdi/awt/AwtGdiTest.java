package net.arnx.wmf2svg.gdi.awt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

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

	private int alphaAtBottomRight(BufferedImage image) {
		return (image.getRGB(image.getWidth() - 1, image.getHeight() - 1) >>> 24) & 0xFF;
	}
}

package net.arnx.wmf2svg.gdi.emf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiColorSpace;
import net.arnx.wmf2svg.gdi.GdiPatternBrush;

public class EmfGdiTest {
	@Test
	public void testDibCreatePatternBrushWritesCreatedibpatternbrushpt() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] dib = createTopDown24BitDib();

		GdiPatternBrush brush = gdi.dibCreatePatternBrush(dib, Gdi.DIB_RGB_COLORS);
		byte[] emf = write(gdi);

		assertNotNull(brush);
		assertEquals(94, readInt32(emf, 88));
		assertEquals(Gdi.DIB_RGB_COLORS, readInt32(emf, 100));
		assertEquals(32, readInt32(emf, 104));
		assertEquals(40, readInt32(emf, 108));
		assertEquals(72, readInt32(emf, 112));
		assertEquals(4, readInt32(emf, 116));
		assertArrayEquals(dib, copyRange(emf, 120, dib.length));
	}

	@Test
	public void testCreateColorSpaceWritesCreatecolorspace() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] logColorSpace = new byte[] { 1, 2, 3, 4, 5 };

		GdiColorSpace colorSpace = gdi.createColorSpace(logColorSpace);
		byte[] emf = write(gdi);

		assertNotNull(colorSpace);
		assertEquals(99, readInt32(emf, 88));
		assertArrayEquals(logColorSpace, copyRange(emf, 100, logColorSpace.length));
	}

	@Test
	public void testCreateColorSpaceWWritesCreatecolorspacew() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] logColorSpace = new byte[] { 6, 7, 8, 9 };

		GdiColorSpace colorSpace = gdi.createColorSpaceW(logColorSpace);
		byte[] emf = write(gdi);

		assertNotNull(colorSpace);
		assertEquals(122, readInt32(emf, 88));
		assertArrayEquals(logColorSpace, copyRange(emf, 100, logColorSpace.length));
	}

	@Test
	public void testColorMatchToTargetWWritesColormatchtotargetw() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] profile = new byte[] { 10, 20, 30 };

		gdi.colorMatchToTarget(1, 2, profile);
		byte[] emf = write(gdi);

		assertEquals(121, readInt32(emf, 88));
		assertEquals(1, readInt32(emf, 96));
		assertEquals(2, readInt32(emf, 100));
		assertEquals(profile.length, readInt32(emf, 104));
		assertEquals(0, readInt32(emf, 108));
		assertArrayEquals(profile, copyRange(emf, 112, profile.length));
	}

	private static byte[] write(EmfGdi gdi) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		return out.toByteArray();
	}

	private static byte[] createTopDown24BitDib() {
		byte[] dib = new byte[44];
		setInt32(dib, 0, 40);
		setInt32(dib, 4, 1);
		setInt32(dib, 8, -1);
		setUInt16(dib, 12, 1);
		setUInt16(dib, 14, 24);
		setInt32(dib, 20, 4);
		dib[40] = 3;
		dib[41] = 2;
		dib[42] = 1;
		return dib;
	}

	private static byte[] copyRange(byte[] data, int offset, int length) {
		byte[] dest = new byte[length];
		System.arraycopy(data, offset, dest, 0, length);
		return dest;
	}

	private static int readInt32(byte[] data, int pos) {
		return (data[pos] & 0xFF)
				| ((data[pos + 1] & 0xFF) << 8)
				| ((data[pos + 2] & 0xFF) << 16)
				| (data[pos + 3] << 24);
	}

	private static void setUInt16(byte[] data, int pos, int value) {
		data[pos] = (byte)(value & 0xFF);
		data[pos + 1] = (byte)((value >>> 8) & 0xFF);
	}

	private static void setInt32(byte[] data, int pos, int value) {
		data[pos] = (byte)(value & 0xFF);
		data[pos + 1] = (byte)((value >>> 8) & 0xFF);
		data[pos + 2] = (byte)((value >>> 16) & 0xFF);
		data[pos + 3] = (byte)((value >>> 24) & 0xFF);
	}
}

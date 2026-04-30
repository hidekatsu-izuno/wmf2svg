package net.arnx.wmf2svg.gdi.emf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiColorSpace;
import net.arnx.wmf2svg.gdi.GdiPatternBrush;
import net.arnx.wmf2svg.gdi.Point;

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
	public void testCreateMonoBrushWritesCreatemonobrush() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] dib = createTopDown24BitDib();

		GdiPatternBrush brush = gdi.createMonoBrush(dib, Gdi.DIB_RGB_COLORS);
		byte[] emf = write(gdi);

		assertNotNull(brush);
		assertEquals(93, readInt32(emf, 88));
		assertEquals(Gdi.DIB_RGB_COLORS, readInt32(emf, 100));
		assertArrayEquals(dib, copyRange(emf, 120, dib.length));
	}

	@Test
	public void testEscapeWritesExtescape() throws Exception {
		EmfGdi gdi = new EmfGdi();

		gdi.escape(new byte[]{0x34, 0x12, 0x03, 0x00, 1, 2, 3});
		byte[] emf = write(gdi);

		assertEquals(106, readInt32(emf, 88));
		assertEquals(0x1234, readInt32(emf, 96));
		assertArrayEquals(new byte[]{1, 2, 3}, copyRange(emf, 100, 3));
	}

	@Test
	public void testNamedEscapeWritesNamedescape() throws Exception {
		EmfGdi gdi = new EmfGdi();

		gdi.namedEscape(0x100, new byte[]{'D', 0, 'R', 0, 'V', 0, 0, 0}, new byte[]{5, 6});
		byte[] emf = write(gdi);

		assertEquals(110, readInt32(emf, 88));
		assertEquals(0x100, readInt32(emf, 96));
		assertEquals(8, readInt32(emf, 100));
		assertEquals(2, readInt32(emf, 104));
		assertArrayEquals(new byte[]{'D', 0, 'R', 0, 'V', 0, 0, 0, 5, 6}, copyRange(emf, 108, 10));
	}

	@Test
	public void testWorldTransformRecords() throws Exception {
		EmfGdi gdi = new EmfGdi();
		float[] xform = new float[]{1, 2, 3, 4, 5, 6};

		gdi.setWorldTransform(xform);
		gdi.modifyWorldTransform(xform, 2);
		byte[] emf = write(gdi);

		assertEquals(35, readInt32(emf, 88));
		assertEquals(Float.floatToIntBits(1), readInt32(emf, 96));
		assertEquals(Float.floatToIntBits(6), readInt32(emf, 116));
		int secondRecord = 88 + readInt32(emf, 92);
		assertEquals(36, readInt32(emf, secondRecord));
		assertEquals(Float.floatToIntBits(1), readInt32(emf, secondRecord + 8));
		assertEquals(Float.floatToIntBits(6), readInt32(emf, secondRecord + 28));
		assertEquals(2, readInt32(emf, secondRecord + 32));
	}

	@Test
	public void testPolyDrawWritesPolydraw() throws Exception {
		EmfGdi gdi = new EmfGdi();

		gdi.polyDraw(new Point[]{new Point(10, 20), new Point(30, 40)}, new byte[]{6, 2});
		byte[] emf = write(gdi);

		assertEquals(56, readInt32(emf, 88));
		assertEquals(2, readInt32(emf, 112));
		assertEquals(10, readInt32(emf, 116));
		assertEquals(20, readInt32(emf, 120));
		assertEquals(30, readInt32(emf, 124));
		assertEquals(40, readInt32(emf, 128));
		assertArrayEquals(new byte[]{6, 2}, copyRange(emf, 132, 2));
	}

	@Test
	public void testPolyTextOutWWritesPolytextoutw() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] text = new byte[]{'A', 0, 'B', 0};

		gdi.polyTextOutW(new Point[]{new Point(11, 22)}, null, null, new byte[][]{text}, null);
		byte[] emf = write(gdi);

		assertEquals(97, readInt32(emf, 88));
		assertEquals(1, readInt32(emf, 124));
		assertEquals(11, readInt32(emf, 128));
		assertEquals(22, readInt32(emf, 132));
		assertEquals(2, readInt32(emf, 136));
		assertEquals(80, readInt32(emf, 140));
		assertArrayEquals(text, copyRange(emf, 168, text.length));
	}

	@Test
	public void testSmallTextOutWritesSmalltextout() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] text = new byte[]{'A', 'B', 'C'};

		gdi.smallTextOut(11, 22, 0x00000300, null, text);
		byte[] emf = write(gdi);

		assertEquals(108, readInt32(emf, 88));
		assertEquals(11, readInt32(emf, 96));
		assertEquals(22, readInt32(emf, 100));
		assertEquals(3, readInt32(emf, 104));
		assertEquals(0x00000300, readInt32(emf, 108));
		assertArrayEquals(text, copyRange(emf, 124, text.length));
	}

	@Test
	public void testCreateColorSpaceWritesCreatecolorspace() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] logColorSpace = new byte[]{1, 2, 3, 4, 5};

		GdiColorSpace colorSpace = gdi.createColorSpace(logColorSpace);
		byte[] emf = write(gdi);

		assertNotNull(colorSpace);
		assertEquals(99, readInt32(emf, 88));
		assertArrayEquals(logColorSpace, copyRange(emf, 100, logColorSpace.length));
	}

	@Test
	public void testCreateColorSpaceWWritesCreatecolorspacew() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] logColorSpace = new byte[]{6, 7, 8, 9};

		GdiColorSpace colorSpace = gdi.createColorSpaceW(logColorSpace);
		byte[] emf = write(gdi);

		assertNotNull(colorSpace);
		assertEquals(122, readInt32(emf, 88));
		assertArrayEquals(logColorSpace, copyRange(emf, 100, logColorSpace.length));
	}

	@Test
	public void testColorMatchToTargetWWritesColormatchtotargetw() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] profile = new byte[]{10, 20, 30};

		gdi.colorMatchToTarget(1, 2, profile);
		byte[] emf = write(gdi);

		assertEquals(121, readInt32(emf, 88));
		assertEquals(1, readInt32(emf, 96));
		assertEquals(2, readInt32(emf, 100));
		assertEquals(profile.length, readInt32(emf, 104));
		assertEquals(0, readInt32(emf, 108));
		assertArrayEquals(profile, copyRange(emf, 112, profile.length));
	}

	@Test
	public void testSetICMProfileAWritesSeticmprofilea() throws Exception {
		EmfGdi gdi = new EmfGdi();
		byte[] profile = new byte[]{'s', 'R', 'G', 'B', 0};

		gdi.setICMProfileA(profile);
		byte[] emf = write(gdi);

		assertEquals(112, readInt32(emf, 88));
		assertEquals(profile.length, readInt32(emf, 100));
		assertArrayEquals(profile, copyRange(emf, 108, profile.length));
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
		return (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8) | ((data[pos + 2] & 0xFF) << 16)
				| (data[pos + 3] << 24);
	}

	private static void setUInt16(byte[] data, int pos, int value) {
		data[pos] = (byte) (value & 0xFF);
		data[pos + 1] = (byte) ((value >>> 8) & 0xFF);
	}

	private static void setInt32(byte[] data, int pos, int value) {
		data[pos] = (byte) (value & 0xFF);
		data[pos + 1] = (byte) ((value >>> 8) & 0xFF);
		data[pos + 2] = (byte) ((value >>> 16) & 0xFF);
		data[pos + 3] = (byte) ((value >>> 24) & 0xFF);
	}
}

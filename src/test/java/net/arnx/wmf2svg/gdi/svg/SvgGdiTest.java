package net.arnx.wmf2svg.gdi.svg;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiPen;

public class SvgGdiTest {
	private static final Pattern PNG_DATA_PATTERN = Pattern.compile("xlink:href=\"data:image/png;base64,([^\"]+)\"");

	@Test
	public void testPie() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 9000, 9000, 1440);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(200, 200, null);
		gdi.setBkMode(1);
		GdiBrush brush1 = gdi.createBrushIndirect(1, 0, 0);
		gdi.selectObject(brush1);
		gdi.rectangle(10, 10, 110, 110);
		GdiPen pen2 = gdi.createPenIndirect(0, 1, 0x0000FF);
		gdi.selectObject(pen2);
		gdi.pie(10, 10, 110, 110, 60, 10, 110, 60);
		gdi.footer();

		File file = new File(System.getProperty("user.home") + "/wmf2svg", "pie_test.svg");
		file.getParentFile().mkdirs();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			gdi.write(out);
		} finally {
			out.close();
		}
	}

	@Test
	public void testQuarterArcUsesSmallArcFlag() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(100, 100, null);
		gdi.arc(0, 0, 100, 100, 100, 50, 50, 0);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains(" A 50.0,50.0 0 0 "));
		Assert.assertFalse(svg.contains(" A 50.0,50.0 0 1 "));
	}

	@Test
	public void testConsecutiveSrcInvertImagesCreateTransparentMaskPng() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 2, 1, 96);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(2, 1, null);

		gdi.stretchDIBits(0, 0, 2, 1, 0, 0, 2, 1,
				createTopDown24BitDib(new int[] { 0xFF0000, 0x00FF00 }), Gdi.DIB_RGB_COLORS, Gdi.SRCINVERT);
		gdi.stretchDIBits(0, 0, 2, 1, 0, 0, 2, 1,
				createTopDown24BitDib(new int[] { 0xFF0000, 0x0000FF }), Gdi.DIB_RGB_COLORS, Gdi.SRCINVERT);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Matcher matcher = PNG_DATA_PATTERN.matcher(svg);
		Assert.assertTrue(matcher.find());
		String pngData = matcher.group(1);
		Assert.assertFalse(matcher.find());
		Assert.assertFalse(svg.contains("data-wmf2svg-rop"));

		BufferedImage image = ImageIO.read(new ByteArrayInputStream(java.util.Base64.getDecoder().decode(pngData)));
		Assert.assertEquals(2, image.getWidth());
		Assert.assertEquals(1, image.getHeight());
		Assert.assertEquals(0, (image.getRGB(0, 0) >>> 24) & 0xFF);
		Assert.assertEquals(0xFF0000FF, image.getRGB(1, 0));
	}

	@Test
	public void testConsecutiveSrcInvertTransparentMaskUsesRenderedSize() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 2, 1, 96);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(2, 1, null);

		gdi.stretchDIBits(0, 0, 2, 1, 0, 0, 2, 1,
				createTopDown24BitDib(new int[] { 0xFF0000 }), Gdi.DIB_RGB_COLORS, Gdi.SRCINVERT);
		gdi.stretchDIBits(0, 0, 2, 1, 0, 0, 2, 1,
				createTopDown24BitDib(new int[] { 0xFF0000, 0x0000FF }), Gdi.DIB_RGB_COLORS, Gdi.SRCINVERT);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Matcher matcher = PNG_DATA_PATTERN.matcher(svg);
		Assert.assertTrue(matcher.find());
		String pngData = matcher.group(1);
		Assert.assertFalse(matcher.find());

		BufferedImage image = ImageIO.read(new ByteArrayInputStream(java.util.Base64.getDecoder().decode(pngData)));
		Assert.assertEquals(2, image.getWidth());
		Assert.assertEquals(1, image.getHeight());
		Assert.assertEquals(0, (image.getRGB(0, 0) >>> 24) & 0xFF);
		Assert.assertEquals(0xFF0000FF, image.getRGB(1, 0));
	}

	@Test
	public void testNegativeWindowExtentFlipsCoordinatesWithoutViewportExtent() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 10, 10, 96);
		gdi.header();
		gdi.setWindowOrgEx(0, 10, null);
		gdi.setWindowExtEx(10, -10, null);
		gdi.moveToEx(0, 10, null);
		gdi.lineTo(0, 0);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("x1=\"0\""));
		Assert.assertTrue(svg.contains("y1=\"0\""));
		Assert.assertTrue(svg.contains("x2=\"0\""));
		Assert.assertTrue(svg.contains("y2=\"10\""));
		Assert.assertFalse(svg.contains("y2=\"-10\""));
	}

	@Test
	public void testCosmeticPenUsesLogicalPixelWidthWithPlaceableHeader() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 718, 572, 1000);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(69, 55, null);
		GdiPen pen = gdi.createPenIndirect(0, 0, 0);
		gdi.selectObject(pen);
		gdi.moveToEx(0, 0, null);
		gdi.lineTo(0, 55);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("stroke-width: 1.0;"));
		Assert.assertFalse(svg.contains("stroke-width: 11.0;"));
	}

	@Test
	public void testFooterDoesNotClipExistingContentToLaterSmallViewport() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 28000, 21000, 2540);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(28000, 21000, null);
		gdi.polygon(new net.arnx.wmf2svg.gdi.Point[] {
				new net.arnx.wmf2svg.gdi.Point(0, 0),
				new net.arnx.wmf2svg.gdi.Point(1058, 0),
				new net.arnx.wmf2svg.gdi.Point(1058, 793),
				new net.arnx.wmf2svg.gdi.Point(0, 793)
		});
		gdi.setViewportExtEx(96, 96, null);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("viewBox=\"0 0 1587 1191\""));
		Assert.assertFalse(svg.contains("viewBox=\"0 0 96 96\""));
	}

	@Test
	public void testFooterUsesEmbeddedEmfBoundsForPlaceableWrapper() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 28000, 21000, 2540);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(28000, 21000, null);
		gdi.comment(createEnhancedMetafileComment(createMinimalEmf(0, 0, 1057, 793)));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("width=\"16.53125in\""));
		Assert.assertTrue(svg.contains("height=\"12.40625in\""));
		Assert.assertTrue(svg.contains("viewBox=\"0 0 1057 793\""));
		Assert.assertFalse(svg.contains("viewBox=\"0 0 28000 21000\""));
	}

	@Test
	public void testFooterUsesDefaultCanvasWithoutPlaceableHeaderOrExtents() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(100, 100, null);
		gdi.moveToEx(10, 20, null);
		gdi.lineTo(30, 40);
		gdi.setWindowExtEx(0, 0, null);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("width=\"330\""));
		Assert.assertTrue(svg.contains("height=\"460\""));
		Assert.assertTrue(svg.contains("viewBox=\"0 0 330 460\""));
	}

	@Test
	public void testNoPlaceableHeaderUsesDefaultViewport() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(1024, 1024, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(1024, 1024, null);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(2048, 2048, null);
		gdi.moveToEx(0, 0, null);
		gdi.lineTo(2048, 2048);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("width=\"1024\""));
		Assert.assertTrue(svg.contains("height=\"1024\""));
		Assert.assertTrue(svg.contains("viewBox=\"0 0 1024 1024\""));
		Assert.assertTrue(svg.contains("x2=\"1024\""));
		Assert.assertTrue(svg.contains("y2=\"1024\""));
	}

	@Test
	public void testFooterKeepsTinyPlaceablePhysicalSize() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 718, 572, 1000);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(718, 572, null);
		gdi.rectangle(0, 0, 718, 572);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("width=\"1.0729166666666667in\""));
		Assert.assertTrue(svg.contains("height=\"0.8541666666666666in\""));
		Assert.assertTrue(svg.contains("viewBox=\"0 0 718 572\""));
	}

	@Test
	public void testFooterKeepsLargePhysicalSizeWhenContentFallsOutsideLogicalCanvas() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 27940, 21590, 2540);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(27940, 21590, null);
		gdi.rectangle(0, 0, 27940, 21590);
		gdi.ellipse(67000, 46000, 69500, 47700);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("width=\"16.5in\""));
		Assert.assertTrue(svg.contains("height=\"12.75in\""));
		Assert.assertTrue(svg.contains("viewBox=\"0 0 27940 21590\""));
		Assert.assertFalse(svg.contains("viewBox=\"0 0 69500 47700\""));
	}

	private byte[] createTopDown24BitDib(int[] pixels) {
		int width = pixels.length;
		int height = 1;
		int stride = ((width * 24 + 31) / 32) * 4;
		byte[] dib = new byte[40 + stride * height];
		setInt32(dib, 0, 40);
		setInt32(dib, 4, width);
		setInt32(dib, 8, -height);
		setUInt16(dib, 12, 1);
		setUInt16(dib, 14, 24);
		setInt32(dib, 20, stride * height);
		for (int x = 0; x < width; x++) {
			int rgb = pixels[x];
			int pos = 40 + x * 3;
			dib[pos] = (byte)(rgb & 0xFF);
			dib[pos + 1] = (byte)((rgb >>> 8) & 0xFF);
			dib[pos + 2] = (byte)((rgb >>> 16) & 0xFF);
		}
		return dib;
	}

	private byte[] createEnhancedMetafileComment(byte[] emf) {
		byte[] data = new byte[38 + emf.length];
		setUInt16(data, 0, 0x000F);
		setInt32(data, 4, 0x43464D57);
		setInt32(data, 8, 0x00000001);
		setInt32(data, 26, emf.length);
		setInt32(data, 34, emf.length);
		System.arraycopy(emf, 0, data, 38, emf.length);
		return data;
	}

	private byte[] createMinimalEmf(int left, int top, int right, int bottom) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		writeInt(data, left);
		writeInt(data, top);
		writeInt(data, right);
		writeInt(data, bottom);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, 1000);
		writeInt(data, 500);
		writeInt(data, 0x464D4520);
		writeInt(data, 0x00010000);
		writeInt(data, 108);
		writeInt(data, 2);
		writeShort(data, 1);
		writeShort(data, 0);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, right - left);
		writeInt(data, bottom - top);
		writeInt(data, right - left);
		writeInt(data, bottom - top);
		writeRecord(out, 1, data.toByteArray());
		writeRecord(out, 14, new byte[12]);
		return out.toByteArray();
	}

	private void writeRecord(ByteArrayOutputStream out, int type, byte[] data) {
		writeInt(out, type);
		writeInt(out, data.length + 8);
		out.write(data, 0, data.length);
	}

	private void writeInt(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		out.write((value >>> 8) & 0xFF);
		out.write((value >>> 16) & 0xFF);
		out.write((value >>> 24) & 0xFF);
	}

	private void writeShort(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		out.write((value >>> 8) & 0xFF);
	}

	private void setUInt16(byte[] data, int pos, int value) {
		data[pos] = (byte)(value & 0xFF);
		data[pos + 1] = (byte)((value >>> 8) & 0xFF);
	}

	private void setInt32(byte[] data, int pos, int value) {
		data[pos] = (byte)(value & 0xFF);
		data[pos + 1] = (byte)((value >>> 8) & 0xFF);
		data[pos + 2] = (byte)((value >>> 16) & 0xFF);
		data[pos + 3] = (byte)((value >>> 24) & 0xFF);
	}
}

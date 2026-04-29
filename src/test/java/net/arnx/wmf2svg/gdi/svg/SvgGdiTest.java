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
	public void testMaskBltCopyOnZeroMaskRendersSourceWhereMaskIsBlack() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(2, 1, null);

		gdi.maskBlt(createTopDown24BitDib(new int[] { 0xFF0000, 0x0000FF }), 0, 0, 2, 1, 0, 0,
				createMonochromeDib(2, 1, new int[] { 0x80 }), 0, 0, 0xCCAA0029L);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Matcher matcher = PNG_DATA_PATTERN.matcher(svg);
		Assert.assertTrue(matcher.find());
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(java.util.Base64.getDecoder().decode(matcher.group(1))));
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
	public void testEmbeddedEmfHeaderDoesNotOffsetDeferredContent() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEnhancedMetafileComment(createEmfWithPolyline(-10, -20, 90, 80)));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("points=\"-10,-20 90,80\""));
	}

	@Test
	public void testEmbeddedEmfPlusCommentKeepsGdiFallbackContent() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEnhancedMetafileComment(createEmfPlusWithPolyline(-10, -20, 90, 80)));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("points=\"-10,-20 90,80\""));
	}

	@Test
	public void testEmfPlusMetafileImageSuppressesDetachedGdiFallback() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusMetafileImageComment(0, createMinimalEmf(0, 0, 20, 10)));
		gdi.intersectClipRect(0, 0, 10, 10);
		gdi.moveToEx(1, 1, null);
		gdi.lineTo(9, 9);
		gdi.comment(createEmfPlusDrawImagePointsComment(0));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(2, count(svg, "<image "));
		Assert.assertFalse(svg.contains("<line "));
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
	public void testPlaceableWindowOriginDoesNotShiftRootViewBox() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(-671, -1308, 2450, 37, 1000);
		gdi.header();
		gdi.setWindowOrgEx(-671, 37, null);
		gdi.setWindowExtEx(3121, -1345, null);
		gdi.moveToEx(-671, 37, null);
		gdi.lineTo(2450, -1308);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("viewBox=\"0 0 3121 1345\""));
		Assert.assertFalse(svg.contains("viewBox=\"-671"));
	}

	@Test
	public void testNonPlaceableWindowOriginDefinesRootViewBox() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setWindowOrgEx(-4100, -1866, null);
		gdi.setWindowExtEx(8204, 3735, null);
		gdi.moveToEx(-4100, -1866, null);
		gdi.lineTo(4104, 1869);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("viewBox=\"-4100 -1866 8204 3735\""));
		Assert.assertFalse(svg.contains("viewBox=\"0 0 8204 3735\""));
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

	private byte[] createMonochromeDib(int width, int height, int[] rows) {
		int stride = ((width + 31) / 32) * 4;
		byte[] dib = new byte[48 + stride * height];
		setInt32(dib, 0, 40);
		setInt32(dib, 4, width);
		setInt32(dib, 8, -height);
		setUInt16(dib, 12, 1);
		setUInt16(dib, 14, 1);
		setInt32(dib, 20, stride * height);
		dib[40] = 0;
		dib[41] = 0;
		dib[42] = 0;
		dib[44] = (byte)0xFF;
		dib[45] = (byte)0xFF;
		dib[46] = (byte)0xFF;
		for (int y = 0; y < height; y++) {
			dib[48 + y * stride] = (byte)rows[y];
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

	private byte[] createEmfWithPolyline(int left, int top, int right, int bottom) {
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
		writeInt(data, 3);
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

		data.reset();
		writeInt(data, left);
		writeInt(data, top);
		writeInt(data, right);
		writeInt(data, bottom);
		writeInt(data, 2);
		writeInt(data, left);
		writeInt(data, top);
		writeInt(data, right);
		writeInt(data, bottom);
		writeRecord(out, 4, data.toByteArray());

		writeRecord(out, 14, new byte[12]);
		return out.toByteArray();
	}

	private byte[] createEmfPlusWithPolyline(int left, int top, int right, int bottom) {
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
		writeInt(data, 4);
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

		writeRecord(out, 70, createEmfPlusHeaderComment());

		data.reset();
		writeInt(data, left);
		writeInt(data, top);
		writeInt(data, right);
		writeInt(data, bottom);
		writeInt(data, 2);
		writeInt(data, left);
		writeInt(data, top);
		writeInt(data, right);
		writeInt(data, bottom);
		writeRecord(out, 4, data.toByteArray());

		writeRecord(out, 14, new byte[12]);
		return out.toByteArray();
	}

	private byte[] createEmfPlusHeaderComment() {
		ByteArrayOutputStream comment = new ByteArrayOutputStream();
		comment.write('E');
		comment.write('M');
		comment.write('F');
		comment.write('+');
		writeShort(comment, 0x4001);
		writeShort(comment, 0x0001);
		writeInt(comment, 28);
		writeInt(comment, 16);
		writeInt(comment, 0x00010000);
		writeInt(comment, 0);
		writeInt(comment, 0);
		writeInt(comment, 0);

		ByteArrayOutputStream data = new ByteArrayOutputStream();
		writeInt(data, comment.size());
		byte[] bytes = comment.toByteArray();
		data.write(bytes, 0, bytes.length);
		return data.toByteArray();
	}

	private byte[] createEmfPlusMetafileImageComment(int objectId, byte[] metafile) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, metafile.length);
		payload.write(metafile, 0, metafile.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500 | objectId, payload.toByteArray());
		writeEmfPlusDrawImagePointsRecord(comment, objectId);
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDrawImagePointsComment(int objectId) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		writeEmfPlusDrawImagePointsRecord(comment, objectId);
		return comment.toByteArray();
	}

	private ByteArrayOutputStream createEmfPlusComment() {
		ByteArrayOutputStream comment = new ByteArrayOutputStream();
		comment.write('E');
		comment.write('M');
		comment.write('F');
		comment.write('+');
		return comment;
	}

	private void writeEmfPlusDrawImagePointsRecord(ByteArrayOutputStream comment, int objectId) {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x401B, objectId, payload.toByteArray());
	}

	private void writeEmfPlusRecord(ByteArrayOutputStream out, int type, int flags, byte[] data) {
		writeShort(out, type);
		writeShort(out, flags);
		writeInt(out, data.length + 12);
		writeInt(out, data.length);
		out.write(data, 0, data.length);
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

	private void writeFloat(ByteArrayOutputStream out, float value) {
		writeInt(out, Float.floatToIntBits(value));
	}

	private int count(String value, String token) {
		int count = 0;
		int offset = 0;
		while ((offset = value.indexOf(token, offset)) >= 0) {
			count++;
			offset += token.length();
		}
		return count;
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

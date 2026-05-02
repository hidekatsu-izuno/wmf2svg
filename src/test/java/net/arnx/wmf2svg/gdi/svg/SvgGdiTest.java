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
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiPen;

public class SvgGdiTest {
	private static final Pattern PNG_DATA_PATTERN = Pattern.compile("xlink:href=\"data:image/png;base64,([^\"]+)\"");

	@Test
	public void testLogicalFontFallbackFamilies() throws Exception {
		assertFontFamily("Fixedsys", "font-family: Fixedsys, Consolas, monospace;");
		assertFontFamily("Modern", "font-family: Modern, \"Courier New\", monospace;");
		assertFontFamily("MS Sans Serif", "font-family: \"MS Sans Serif\", \"Microsoft Sans Serif\", monospace;");
		assertFontFamily("MS Serif", "font-family: \"MS Serif\", \"Times New Roman\", serif;");
		assertFontFamily("Roman", "font-family: Roman, \"Times New Roman\", serif;");
		assertFontFamily("Script", "font-family: Script, \"Segoe Script\", cursive;");
		assertFontFamily("Small Fonts", "font-family: \"Small Fonts\", \"Segoe UI\", sans-serif;");
		assertFontFamily("System", "font-family: System, \"Segoe UI\", sans-serif;");
		assertFontFamily("Terminal", "font-family: Terminal, \"Cascadia Mono\", monospace;");
	}

	@Test
	public void testTextOutFillsEstimatedOpaqueBackground() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setBkMode(Gdi.OPAQUE);
		gdi.setBkColor(0x0000FF);
		gdi.selectObject(gdi.createFontIndirect(-20, 0, 0, 0, GdiFont.FW_NORMAL, false, false, false,
				GdiFont.ANSI_CHARSET, 0, 0, 0, 0, "Unknown".getBytes("US-ASCII")));
		gdi.textOut(10, 20, "ABCD".getBytes("US-ASCII"));
		gdi.footer();

		String svg = writeSvg(gdi);
		Assert.assertTrue(svg.contains("<g>"));
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("fill=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("x=\"10\""));
		Assert.assertTrue(svg.contains("y=\"20\""));
		Assert.assertTrue(svg.contains("width=\"40\""));
		Assert.assertTrue(svg.contains("height=\"20\""));
		Assert.assertTrue(svg.indexOf("<rect ") < svg.indexOf(">ABCD</text>"));
	}

	@Test
	public void testExtTextOutFillsEstimatedOpaqueBackgroundWithoutRect() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setBkMode(Gdi.OPAQUE);
		gdi.setBkColor(0x00FF00);
		gdi.selectObject(gdi.createFontIndirect(-20, 0, 0, 0, GdiFont.FW_NORMAL, false, false, false,
				GdiFont.ANSI_CHARSET, 0, 0, 0, 0, "Unknown".getBytes("US-ASCII")));
		gdi.extTextOut(10, 20, 0, null, "ABCD".getBytes("US-ASCII"), null);
		gdi.footer();

		String svg = writeSvg(gdi);
		Assert.assertTrue(svg.contains("<g>"));
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("fill=\"rgb(0,255,0)\""));
		Assert.assertTrue(svg.contains("x=\"10\""));
		Assert.assertTrue(svg.contains("y=\"20\""));
		Assert.assertTrue(svg.contains("width=\"40\""));
		Assert.assertTrue(svg.contains("height=\"20\""));
		Assert.assertTrue(svg.indexOf("<rect ") < svg.indexOf(">ABCD</text>"));
	}

	@Test
	public void testTextOutEstimatesFullWidthBackground() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setBkMode(Gdi.OPAQUE);
		gdi.selectObject(gdi.createFontIndirect(-20, 0, 0, 0, GdiFont.FW_NORMAL, false, false, false,
				GdiFont.SHIFTJIS_CHARSET, 0, 0, 0, 0, "Unknown".getBytes("US-ASCII")));
		gdi.textOut(10, 20, "日本".getBytes("MS932"));
		gdi.footer();

		String svg = writeSvg(gdi);
		Assert.assertTrue(svg.contains("width=\"40\""));
		Assert.assertTrue(svg.contains(">日本</text>"));
	}

	@Test
	public void testTextOutEstimatesMixedWidthBackground() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setBkMode(Gdi.OPAQUE);
		gdi.selectObject(gdi.createFontIndirect(-20, 0, 0, 0, GdiFont.FW_NORMAL, false, false, false,
				GdiFont.SHIFTJIS_CHARSET, 0, 0, 0, 0, "Unknown".getBytes("US-ASCII")));
		gdi.textOut(10, 20, "A日".getBytes("MS932"));
		gdi.footer();

		String svg = writeSvg(gdi);
		Assert.assertTrue(svg.contains("width=\"30\""));
		Assert.assertTrue(svg.contains(">A日</text>"));
	}

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

		gdi.stretchDIBits(0, 0, 2, 1, 0, 0, 2, 1, createTopDown24BitDib(new int[]{0xFF0000, 0x00FF00}),
				Gdi.DIB_RGB_COLORS, Gdi.SRCINVERT);
		gdi.stretchDIBits(0, 0, 2, 1, 0, 0, 2, 1, createTopDown24BitDib(new int[]{0xFF0000, 0x0000FF}),
				Gdi.DIB_RGB_COLORS, Gdi.SRCINVERT);

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
	public void testMergeCopyCombinesSourceWithSolidBrush() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(1, 1, null);
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x454545, 0));

		gdi.stretchDIBits(0, 0, 1, 1, 0, 0, 1, 1, createTopDown24BitDib(new int[]{0xFF00FF}), Gdi.DIB_RGB_COLORS,
				Gdi.MERGECOPY);

		BufferedImage image = readFirstPng(gdi);
		Assert.assertEquals(1, image.getWidth());
		Assert.assertEquals(1, image.getHeight());
		Assert.assertEquals(0xFF450045, image.getRGB(0, 0));
	}

	@Test
	public void testDibBitBltWithoutSourceUsesPatternForPatternOnlyRop() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(10, 10, null);
		GdiBrush brush = gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x454545, 0);
		gdi.selectObject(brush);

		gdi.dibBitBlt(null, 1, 2, 3, 4, 0, 0, Gdi.PATCOPY);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("height=\"4\""));
		Assert.assertTrue(svg.contains("width=\"3\""));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
		Assert.assertFalse(svg.contains("<image"));
	}

	@Test
	public void testConsecutiveSrcInvertTransparentMaskUsesRenderedSize() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 2, 1, 96);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(2, 1, null);

		gdi.stretchDIBits(0, 0, 2, 1, 0, 0, 2, 1, createTopDown24BitDib(new int[]{0xFF0000}), Gdi.DIB_RGB_COLORS,
				Gdi.SRCINVERT);
		gdi.stretchDIBits(0, 0, 2, 1, 0, 0, 2, 1, createTopDown24BitDib(new int[]{0xFF0000, 0x0000FF}),
				Gdi.DIB_RGB_COLORS, Gdi.SRCINVERT);

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

		gdi.maskBlt(createTopDown24BitDib(new int[]{0xFF0000, 0x0000FF}), 0, 0, 2, 1, 0, 0,
				createMonochromeDib(2, 1, new int[]{0x80}), 0, 0, 0xCCAA0029L);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Matcher matcher = PNG_DATA_PATTERN.matcher(svg);
		Assert.assertTrue(matcher.find());
		BufferedImage image = ImageIO
				.read(new ByteArrayInputStream(java.util.Base64.getDecoder().decode(matcher.group(1))));
		Assert.assertEquals(0, (image.getRGB(0, 0) >>> 24) & 0xFF);
		Assert.assertEquals(0xFF0000FF, image.getRGB(1, 0));
	}

	@Test
	public void testMaskBltKeepsDestinationOnZeroMaskWithForegroundRop() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(2, 1, null);

		gdi.maskBlt(createTopDown24BitDib(new int[]{0xFF0000, 0x0000FF}), 0, 0, 2, 1, 0, 0,
				createMonochromeDib(2, 1, new int[]{0x80}), 0, 0, 0xAA000000L | Gdi.NOTSRCCOPY);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("NOTSRCCOPY_FILTER"));
		Matcher matcher = PNG_DATA_PATTERN.matcher(svg);
		Assert.assertTrue(matcher.find());
		BufferedImage image = ImageIO
				.read(new ByteArrayInputStream(java.util.Base64.getDecoder().decode(matcher.group(1))));
		Assert.assertEquals(0xFFFF0000, image.getRGB(0, 0));
		Assert.assertEquals(0, (image.getRGB(1, 0) >>> 24) & 0xFF);
	}

	@Test
	public void testMaskBltAcceptsPlainSourceCopyRop() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(2, 1, null);

		gdi.maskBlt(createTopDown24BitDib(new int[]{0xFF0000, 0x0000FF}), 0, 0, 2, 1, 0, 0,
				createMonochromeDib(2, 1, new int[]{0x80}), 0, 0, Gdi.SRCCOPY);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Matcher matcher = PNG_DATA_PATTERN.matcher(svg);
		Assert.assertTrue(matcher.find());
		BufferedImage image = ImageIO
				.read(new ByteArrayInputStream(java.util.Base64.getDecoder().decode(matcher.group(1))));
		Assert.assertEquals(0xFFFF0000, image.getRGB(0, 0));
		Assert.assertEquals(0, (image.getRGB(1, 0) >>> 24) & 0xFF);
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
	public void testPenWidthUsesXScaleInAnisotropicMap() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.setMapMode(8);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(10, 1, null);
		gdi.setViewportExtEx(10, 20, null);
		GdiPen pen = gdi.createPenIndirect(0, 2, 0);
		gdi.selectObject(pen);
		gdi.moveToEx(0, 0, null);
		gdi.lineTo(10, 0);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("stroke-width: 2.0;"));
		Assert.assertFalse(svg.contains("stroke-width: 40.0;"));
	}

	@Test
	public void testFooterDoesNotClipExistingContentToLaterSmallViewport() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 28000, 21000, 2540);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(28000, 21000, null);
		gdi.polygon(new net.arnx.wmf2svg.gdi.Point[]{new net.arnx.wmf2svg.gdi.Point(0, 0),
				new net.arnx.wmf2svg.gdi.Point(1058, 0), new net.arnx.wmf2svg.gdi.Point(1058, 793),
				new net.arnx.wmf2svg.gdi.Point(0, 793)});
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
	public void testFooterUsesEmbeddedEmfBoundsWithoutShiftingPlaceableOrigin() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 157, 6299, 5707, 1000);
		gdi.header();
		gdi.comment(createEnhancedMetafileComment(createEmfWithPolyline(0, 20, 799, 724)));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("width=\"9.447916666666666in\""));
		Assert.assertTrue(svg.contains("height=\"8.322916666666666in\""));
		Assert.assertTrue(svg.contains("viewBox=\"0 0 799 724\""));
		Assert.assertFalse(svg.contains("viewBox=\"0 20 799 704\""));
		Assert.assertFalse(svg.contains("viewBox=\"0 0 6299 5550\""));
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
	public void testEmfPlusGetDCKeepsSubsequentGdiContent() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusMetafileImageComment(0, createMinimalEmf(0, 0, 20, 10)));
		gdi.comment(createEmfPlusDrawImagePointsComment(0));
		gdi.comment(createEmfPlusGetDCComment());
		gdi.moveToEx(1, 1, null);
		gdi.lineTo(9, 9);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(2, count(svg, "<image "));
		Assert.assertTrue(svg.contains("<line "));
		Assert.assertTrue(svg.contains("x1=\"1\""));
		Assert.assertTrue(svg.contains("x2=\"9\""));
	}

	@Test
	public void testEmfPlusSolidFillRecordsRenderBasicShapes() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusSolidFillComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
		Assert.assertTrue(svg.contains("width=\"30\""));
		Assert.assertTrue(svg.contains("height=\"40\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("fill-opacity=\"0.5019607843137255\""));
		Assert.assertTrue(svg.contains("<ellipse "));
		Assert.assertTrue(svg.contains("points=\"1,1 10,1 10,8\""));
	}

	@Test
	public void testEmfPlusAntiAliasModeAppliesShapeRenderingAttribute() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusAntiAliasModeComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("shape-rendering=\"geometricPrecision\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
	}

	@Test
	public void testEmfPlusPixelOffsetModeOffsetsRenderedShape() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPixelOffsetModeComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("transform=\"translate(0.5 0.5)\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
	}

	@Test
	public void testEmfPlusCompositingQualityAppliesColorInterpolation() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusCompositingQualityComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("color-interpolation=\"linearRGB\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
	}

	@Test
	public void testEmfPlusCompositingModeIsPreservedOnRenderedShape() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusCompositingModeComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("data-emfplus-compositing-mode=\"source-copy\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
	}

	@Test
	public void testEmfPlusTSGraphicsCompositingModeIsPreservedOnRenderedShape() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTSGraphicsCompositingModeComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("data-emfplus-compositing-mode=\"source-copy\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
	}

	@Test
	public void testEmfPlusContainerRestoresGraphicsState() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusContainerStateComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(2, count(svg, "<rect "));
		Assert.assertEquals(1, count(svg, "transform=\"translate(0.5 0.5)\""));
		Assert.assertTrue(svg.contains("x=\"40\""));
	}

	@Test
	public void testEmfPlusBeginContainerAppliesRectTransform() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusBeginContainerTransformComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(2, count(svg, "<rect "));
		Assert.assertTrue(svg.contains("x=\"20\""));
		Assert.assertTrue(svg.contains("y=\"40\""));
		Assert.assertTrue(svg.contains("width=\"30\""));
		Assert.assertTrue(svg.contains("height=\"40\""));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
	}

	@Test
	public void testEmfPlusSetTSGraphicsAppliesGraphicsState() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTSGraphicsComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("x=\"11\""));
		Assert.assertTrue(svg.contains("y=\"22\""));
		Assert.assertTrue(svg.contains("shape-rendering=\"geometricPrecision\""));
		Assert.assertTrue(svg.contains("color-interpolation=\"linearRGB\""));
		Assert.assertTrue(svg.contains("transform=\"translate(0.5 0.5)\""));
	}

	@Test
	public void testEmfPlusSetTSClipCreatesMaskFromRectangles() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTSClipComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<mask "));
		Assert.assertTrue(svg.contains("<g mask=\"url(#mask"));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
		Assert.assertTrue(svg.contains("width=\"9\""));
		Assert.assertTrue(svg.contains("height=\"10\""));
		Assert.assertTrue(svg.contains("x=\"20\""));
		Assert.assertTrue(svg.contains("y=\"21\""));
	}

	@Test
	public void testEmfPlusCompressedTSClipCreatesMaskFromRectangles() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusCompressedTSClipComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<mask "));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
		Assert.assertTrue(svg.contains("width=\"9\""));
		Assert.assertTrue(svg.contains("height=\"10\""));
		Assert.assertTrue(svg.contains("x=\"20\""));
		Assert.assertTrue(svg.contains("y=\"21\""));
	}

	@Test
	public void testEmfPlusLinearGradientBrushFillsShape() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusLinearGradientBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<linearGradient "));
		Assert.assertTrue(svg.contains("gradientUnits=\"userSpaceOnUse\""));
		Assert.assertTrue(svg.contains("x1=\"0\""));
		Assert.assertTrue(svg.contains("y1=\"0\""));
		Assert.assertTrue(svg.contains("x2=\"20\""));
		Assert.assertTrue(svg.contains("y2=\"10\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(0,0,255)\""));
		Assert.assertTrue(svg.contains("fill=\"url(#gradient"));
	}

	@Test
	public void testEmfPlusLinearGradientBrushUsesWrapMode() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusLinearGradientBrushComment(1));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<linearGradient "));
		Assert.assertTrue(svg.contains("spreadMethod=\"reflect\""));
	}

	@Test
	public void testEmfPlusLinearGradientBrushUsesGammaCorrection() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusGammaCorrectedLinearGradientBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<linearGradient "));
		Assert.assertTrue(svg.contains("color-interpolation=\"linearRGB\""));
	}

	@Test
	public void testEmfPlusPathGradientBrushFillsShape() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPathGradientBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<radialGradient "));
		Assert.assertTrue(svg.contains("gradientUnits=\"userSpaceOnUse\""));
		Assert.assertTrue(svg.contains("cx=\"10\""));
		Assert.assertTrue(svg.contains("cy=\"5\""));
		Assert.assertTrue(svg.contains("r=\"10\""));
		Assert.assertTrue(svg.contains("gradientTransform=\"matrix(1 0 0 0.5 0 2.5)\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(0,0,255)\""));
		Assert.assertTrue(svg.contains("fill=\"url(#gradient"));
	}

	@Test
	public void testEmfPlusPathGradientBrushUsesWrapMode() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPathGradientBrushComment(4));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<radialGradient "));
		Assert.assertTrue(svg.contains("spreadMethod=\"pad\""));
	}

	@Test
	public void testEmfPlusPathGradientBrushUsesGammaCorrection() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPathGradientBrushComment(0, 0x00000081));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<radialGradient "));
		Assert.assertTrue(svg.contains("color-interpolation=\"linearRGB\""));
	}

	@Test
	public void testEmfPlusPathGradientBrushUsesFocusScale() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusFocusScalePathGradientBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<radialGradient "));
		Assert.assertTrue(svg.contains("offset=\"50%\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(0,0,255)\""));
	}

	@Test
	public void testEmfPlusPathGradientBrushUsesPresetColorStops() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPresetPathGradientBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<radialGradient "));
		Assert.assertTrue(svg.contains("offset=\"0%\""));
		Assert.assertTrue(svg.contains("offset=\"50%\""));
		Assert.assertTrue(svg.contains("offset=\"100%\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(0,255,0)\""));
		Assert.assertTrue(svg.contains("stop-opacity=\"0.5019607843137255\""));
	}

	@Test
	public void testEmfPlusPathGradientBrushUsesBlendFactors() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusBlendFactorPathGradientBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<radialGradient "));
		Assert.assertTrue(svg.contains("offset=\"25%\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(64,0,191)\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(0,0,255)\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusLinearGradientBrushUsesPresetColorStops() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPresetLinearGradientBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("offset=\"0%\""));
		Assert.assertTrue(svg.contains("offset=\"50%\""));
		Assert.assertTrue(svg.contains("offset=\"100%\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(0,255,0)\""));
		Assert.assertTrue(svg.contains("stop-opacity=\"0.5019607843137255\""));
	}

	@Test
	public void testEmfPlusLinearGradientBrushUsesBlendFactors() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusBlendFactorLinearGradientBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("offset=\"25%\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(64,0,191)\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(0,0,255)\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusLinearGradientBrushUsesVerticalBlendFactors() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusVerticalBlendFactorLinearGradientBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("offset=\"75%\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(191,0,64)\""));
	}

	@Test
	public void testEmfPlusLinearGradientBrushUsesTransform() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTransformLinearGradientBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("gradientTransform=\"matrix(1 0 0 1 5 7)\""));
		Assert.assertTrue(svg.contains("fill=\"url(#gradient"));
	}

	@Test
	public void testEmfPlusHatchBrushFillsShapeWithPattern() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusHatchBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<pattern "));
		Assert.assertTrue(svg.contains("patternUnits=\"userSpaceOnUse\""));
		Assert.assertTrue(svg.contains("fill=\"url(#pattern"));
		Assert.assertTrue(svg.contains("fill=\"rgb(0,0,255)\""));
		Assert.assertTrue(svg.contains("stroke=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("x1=\"0\""));
		Assert.assertTrue(svg.contains("y1=\"4\""));
		Assert.assertTrue(svg.contains("x1=\"4\""));
		Assert.assertTrue(svg.contains("y1=\"0\""));
	}

	@Test
	public void testEmfPlusRenderingOriginOffsetsHatchPattern() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusRenderingOriginHatchBrushComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<pattern "));
		Assert.assertTrue(svg.contains("x=\"3\""));
		Assert.assertTrue(svg.contains("y=\"5\""));
		Assert.assertTrue(svg.contains("fill=\"url(#pattern"));
	}

	@Test
	public void testEmfPlusTextureBrushFillsShapeWithImagePattern() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTextureBrushComment(createPng(2, 3)));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<pattern "));
		Assert.assertTrue(svg.contains("patternTransform=\"matrix(1 0 0 1 4 5)\""));
		Assert.assertTrue(svg.contains("<image "));
		Assert.assertTrue(svg.contains("width=\"2\""));
		Assert.assertTrue(svg.contains("height=\"3\""));
		Assert.assertTrue(svg.contains("xlink:href=\"data:image/png;base64,"));
		Assert.assertTrue(svg.contains("fill=\"url(#pattern"));
	}

	@Test
	public void testEmfPlusTextureBrushUsesGammaCorrection() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTextureBrushComment(createPng(2, 3), 0x00000082));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<pattern "));
		Assert.assertTrue(svg.contains("<image "));
		Assert.assertTrue(svg.contains("color-interpolation=\"linearRGB\""));
	}

	@Test
	public void testEmfPlusPenObjectDrawRecordsRenderBasicOutlines() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPenDrawComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<polyline "));
		Assert.assertTrue(svg.contains("points=\"0,0 10,5 20,0\""));
		Assert.assertTrue(svg.contains("stroke=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("stroke-width=\"3.5\""));
		Assert.assertTrue(svg.contains("<rect "));
		Assert.assertTrue(svg.contains("fill=\"none\""));
	}

	@Test
	public void testEmfPlusContinuedObjectRecordsAreCombinedBeforeUse() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusContinuedPenDrawComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<polyline "));
		Assert.assertTrue(svg.contains("points=\"0,0 10,5 20,0\""));
		Assert.assertTrue(svg.contains("stroke=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("stroke-width=\"3.5\""));
	}

	@Test
	public void testEmfPlusPenOptionalDataRendersDashCapsAndJoin() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusDashedPenComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("stroke-width=\"2\""));
		Assert.assertTrue(svg.contains("stroke-linecap=\"round\""));
		Assert.assertTrue(svg.contains("stroke-linejoin=\"bevel\""));
		Assert.assertTrue(svg.contains("stroke-miterlimit=\"7\""));
		Assert.assertTrue(svg.contains("stroke-dashoffset=\"3\""));
		Assert.assertTrue(svg.contains("stroke-dasharray=\"6,6,2,6\""));
	}

	@Test
	public void testEmfPlusPenTransformScalesStrokeAndDashPattern() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTransformedDashedPenComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("stroke-width=\"4\""));
		Assert.assertTrue(svg.contains("stroke-dashoffset=\"6\""));
		Assert.assertTrue(svg.contains("stroke-dasharray=\"12,12,4,12\""));
	}

	@Test
	public void testEmfPlusPenUnitConvertsStrokeWidth() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPointUnitPenComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("stroke-width=\"96\""));
	}

	@Test
	public void testEmfPlusPenCustomDashPatternRendersStrokeDasharray() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusCustomDashedPenComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("stroke-dasharray=\"4,2,8,2\""));
	}

	@Test
	public void testEmfPlusPenDashedLineCapRoundsDashes() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusRoundDashCapPenComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("stroke-linecap=\"round\""));
		Assert.assertTrue(svg.contains("stroke-dasharray=\"2,6\""));
	}

	@Test
	public void testEmfPlusDrawImageRecordRendersBitmapInDestinationRect() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusDrawImageComment(0, createPng(2, 1)));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<image "));
		Assert.assertTrue(svg.contains("xlink:href=\"data:image/png;base64,"));
		Assert.assertTrue(svg.contains("transform=\"matrix(15 0 0 40 5 7)\""));
	}

	@Test
	public void testEmfPlusDrawImageRecordClipsSourceRectangle() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusDrawImageSourceRectComment(0, createPng(2, 1)));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<clipPath "));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"0\""));
		Assert.assertTrue(svg.contains("width=\"1\""));
		Assert.assertTrue(svg.contains("height=\"1\""));
		Assert.assertTrue(svg.contains("clip-path=\"url(#clip"));
		Assert.assertTrue(svg.contains("transform=\"matrix(30 0 0 40 -25 7)\""));
		Assert.assertTrue(svg.contains("<image "));
		Assert.assertTrue(svg.contains("width=\"2\""));
		Assert.assertTrue(svg.contains("height=\"1\""));
	}

	@Test
	public void testEmfPlusGifCompressedImageConvertsToPng() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusDrawImageComment(0, createGif(2, 1)));
		gdi.footer();

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
		Assert.assertTrue(svg.contains("transform=\"matrix(15 0 0 40 5 7)\""));
	}

	@Test
	public void testEmfPlusRawBitmapImageRendersAsPng() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusRawBitmapDrawImageComment(0));
		gdi.footer();

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
		Assert.assertEquals(0xFFFF0000, image.getRGB(0, 0));
		Assert.assertEquals(0x8000FF00, image.getRGB(1, 0));
		Assert.assertTrue(svg.contains("transform=\"matrix(15 0 0 40 5 7)\""));
	}

	@Test
	public void testEmfPlusHighDepthRawBitmapImagesRenderAsPng() throws Exception {
		assertEmfPlusRawBitmapPixels(
				createEmfPlusRawBitmapDrawImageComment(0, 0x00061007, 4, new byte[]{0, (byte) 0xFC, (byte) 0xE0, 0x03}),
				0xFFFF0000, 0x0000FF00);
		assertEmfPlusRawBitmapPixels(
				createEmfPlusRawBitmapDrawImageComment(0, 0x0034400D, 16, new byte[]{0, 0, 0, 0, (byte) 0xFF,
						(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0, 0, (byte) 0xFF, (byte) 0xFF, 0, 0, 0, (byte) 0x80}),
				0xFFFF0000, 0x8000FF00);
		assertEmfPlusRawBitmapPixels(
				createEmfPlusRawBitmapDrawImageComment(0, 0x001A400E, 16, new byte[]{0, 0, 0, 0, (byte) 0xFF,
						(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0, 0, 0, (byte) 0x80, 0, 0, 0, (byte) 0x80}),
				0xFFFF0000, 0x8000FF00);
	}

	@Test
	public void testEmfPlusWideRawBitmapImagesRenderAsPng() throws Exception {
		assertEmfPlusRawBitmapPixels(
				createEmfPlusRawBitmapDrawImageComment(0, 0x0010300C, 12,
						new byte[]{0, 0, 0, 0, (byte) 0xFF, (byte) 0xFF, 0, 0, (byte) 0xFF, (byte) 0xFF, 0, 0}),
				0xFFFF0000, 0xFF00FF00);
		assertEmfPlusRawBitmapPixels(
				createEmfPlusRawBitmapDrawImageComment(0, 0x00101004, 4, new byte[]{0, 0, (byte) 0xFF, (byte) 0xFF}),
				0xFF000000, 0xFFFFFFFF);
	}

	@Test
	public void testEmfPlusIndexedBitmapImageRendersPaletteColors() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusIndexedBitmapDrawImageComment(0));
		gdi.footer();

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
		Assert.assertEquals(0xFFFF0000, image.getRGB(0, 0));
		Assert.assertEquals(0x8000FF00, image.getRGB(1, 0));
	}

	private void assertEmfPlusRawBitmapPixels(byte[] comment, int firstPixel, int secondPixel) throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(comment);
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		Matcher matcher = PNG_DATA_PATTERN.matcher(out.toString("UTF-8"));
		Assert.assertTrue(matcher.find());
		String pngData = matcher.group(1);
		Assert.assertFalse(matcher.find());

		BufferedImage image = ImageIO.read(new ByteArrayInputStream(java.util.Base64.getDecoder().decode(pngData)));
		Assert.assertEquals(2, image.getWidth());
		Assert.assertEquals(1, image.getHeight());
		Assert.assertEquals(firstPixel, image.getRGB(0, 0));
		Assert.assertEquals(secondPixel, image.getRGB(1, 0));
	}

	@Test
	public void testEmfPlusSetInterpolationModeNearestNeighborRendersPixelatedImage() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusInterpolationImageComment(0, 5, createPng(2, 1)));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<image "));
		Assert.assertTrue(svg.contains("image-rendering=\"pixelated\""));
	}

	@Test
	public void testEmfPlusSetInterpolationModeHighQualityRendersOptimizedImage() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusInterpolationImageComment(0, 7, createPng(2, 1)));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<image "));
		Assert.assertTrue(svg.contains("image-rendering=\"optimizeQuality\""));
	}

	@Test
	public void testEmfPlusArcAndPieRecordsRenderPaths() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusArcAndPieComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains(" A 50,25 0 0 1 "));
		Assert.assertTrue(svg.contains("stroke=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains(" Z\""));
	}

	@Test
	public void testEmfPlusPathObjectFillAndDrawRenderSvgPath() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPathComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(2, count(svg, "<path "));
		Assert.assertTrue(svg.contains("d=\"M 0,0 L 10,0 C 10,5 5,10 0,0 Z\""));
		Assert.assertTrue(svg.contains("fill-rule=\"evenodd\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("stroke=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusStrokeFillPathRendersSingleFilledAndStrokedPath() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusStrokeFillPathComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(1, count(svg, "<path "));
		Assert.assertTrue(svg.contains("d=\"M 0,0 L 10,0 L 0,10 L 0,0 Z\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("stroke=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("stroke-width=\"2\""));
	}

	@Test
	public void testEmfPlusRelativePathObjectFillAndDrawRenderSvgPath() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusRelativePathComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(2, count(svg, "<path "));
		Assert.assertTrue(svg.contains("d=\"M 0,0 L 10,0 L 10,10 L 0,10 Z\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("stroke=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusClearAndDrawBeziersRenderSvgElements() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusClearAndBeziersComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("fill-opacity=\"0.5019607843137255\""));
		Assert.assertTrue(svg.contains("d=\"M 0,0 C 10,0 10,10 20,10\""));
		Assert.assertTrue(svg.contains("stroke=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusClosedCurveRecordsRenderSvgPaths() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusClosedCurveComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(2, count(svg, "<path "));
		Assert.assertTrue(svg.contains("d=\"M 0,0 C 0,0 10,0 10,0 C 10,0 10,10 10,10"));
		Assert.assertTrue(svg.contains(" C 0,10 0,0 0,0 Z\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("stroke=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusDrawCurveRecordRendersSvgPath() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusDrawCurveComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(1, count(svg, "<path "));
		Assert.assertTrue(svg.contains("d=\"M 10,0 C 10,0 20,10 20,10 C 20,10 30,0 30,0\""));
		Assert.assertTrue(svg.contains("stroke=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("fill=\"none\""));
	}

	@Test
	public void testEmfPlusFontObjectAndDrawStringRenderSvgText() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusDrawStringComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<clipPath "));
		Assert.assertTrue(svg.contains("clip-path=\"url(#clip"));
		Assert.assertTrue(svg.contains("<text "));
		Assert.assertTrue(svg.contains("x=\"5\""));
		Assert.assertTrue(svg.contains("y=\"7\""));
		Assert.assertTrue(svg.contains("font-family=\"Arial\""));
		Assert.assertTrue(svg.contains("font-size=\"12pt\""));
		Assert.assertTrue(svg.contains("font-weight=\"bold\""));
		Assert.assertTrue(svg.contains("font-style=\"italic\""));
		Assert.assertTrue(svg.contains("text-decoration=\"underline\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains(">Hello EMF+</text>"));
	}

	@Test
	public void testEmfPlusFontObjectPreservesPhysicalFontUnits() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusFontUnitDrawStringComment(3, 12f, "Point"));
		gdi.comment(createEmfPlusFontUnitDrawStringComment(4, 0.5f, "Inch"));
		gdi.comment(createEmfPlusFontUnitDrawStringComment(5, 300f, "Document"));
		gdi.comment(createEmfPlusFontUnitDrawStringComment(6, 25.4f, "Millimeter"));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("font-size=\"12pt\""));
		Assert.assertTrue(svg.contains(">Point</text>"));
		Assert.assertTrue(svg.contains("font-size=\"0.5in\""));
		Assert.assertTrue(svg.contains(">Inch</text>"));
		Assert.assertTrue(svg.contains("font-size=\"1in\""));
		Assert.assertTrue(svg.contains(">Document</text>"));
		Assert.assertTrue(svg.contains("font-size=\"25.4mm\""));
		Assert.assertTrue(svg.contains(">Millimeter</text>"));
	}

	@Test
	public void testEmfPlusTextRenderingHintAppliesTextRenderingAttribute() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTextRenderingHintDrawStringComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<text "));
		Assert.assertTrue(svg.contains("text-rendering=\"optimizeLegibility\""));
		Assert.assertTrue(svg.contains(">Hinted</text>"));
	}

	@Test
	public void testEmfPlusTextContrastIsPreservedOnDrawString() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTextContrastDrawStringComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<text "));
		Assert.assertTrue(svg.contains("data-emfplus-text-contrast=\"1200\""));
		Assert.assertTrue(svg.contains(">Contrast</text>"));
	}

	@Test
	public void testEmfPlusTSGraphicsTextContrastIsPreservedOnDrawString() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTSGraphicsTextContrastDrawStringComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<text "));
		Assert.assertTrue(svg.contains("data-emfplus-text-contrast=\"8\""));
		Assert.assertTrue(svg.contains(">TS</text>"));
	}

	@Test
	public void testEmfPlusStringFormatAlignsDrawString() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusAlignedDrawStringComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<text "));
		Assert.assertTrue(svg.contains("x=\"65\""));
		Assert.assertTrue(svg.contains("y=\"17\""));
		Assert.assertTrue(svg.contains("text-anchor=\"middle\""));
		Assert.assertTrue(svg.contains("dominant-baseline=\"middle\""));
		Assert.assertTrue(svg.contains(">Centered</text>"));
	}

	@Test
	public void testEmfPlusNoClipStringFormatLeavesDrawStringUnclipped() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusNoClipDrawStringComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertFalse(svg.contains("<clipPath "));
		Assert.assertFalse(svg.contains("clip-path=\"url(#clip"));
		Assert.assertTrue(svg.contains("<text "));
		Assert.assertTrue(svg.contains(">No clip</text>"));
	}

	@Test
	public void testEmfPlusStringFormatShowsHotkeyPrefix() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusFormattedDrawStringComment(0, 1, "Save && E&xit"));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("Save &amp; E<tspan text-decoration=\"underline\">x</tspan>it"));
	}

	@Test
	public void testEmfPlusStringFormatHidesHotkeyPrefix() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusFormattedDrawStringComment(0, 2, "E&xit && Save"));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains(">Exit &amp; Save</text>"));
	}

	@Test
	public void testEmfPlusStringFormatAppliesRightToLeftDirection() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusFormattedDrawStringComment(1, 0, "RTL"));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("direction=\"rtl\""));
		Assert.assertTrue(svg.contains("unicode-bidi=\"bidi-override\""));
	}

	@Test
	public void testEmfPlusStringFormatAppliesTrackingAsLetterSpacing() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusFormattedDrawStringComment(0, 0, 1.25f, "Wide"));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("letter-spacing=\"0.25em\""));
		Assert.assertTrue(svg.contains(">Wide</text>"));
	}

	@Test
	public void testEmfPlusDrawDriverStringRendersPositionedSvgText() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusDrawDriverStringComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<text "));
		Assert.assertTrue(svg.contains("font-family=\"Arial\""));
		Assert.assertTrue(svg.contains("font-size=\"12pt\""));
		Assert.assertTrue(svg.contains("<tspan x=\"5\" y=\"7\">A</tspan>"));
		Assert.assertTrue(svg.contains("<tspan x=\"15\" y=\"7\">B</tspan>"));
		Assert.assertTrue(svg.contains("<tspan x=\"25\" y=\"7\">C</tspan>"));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
	}

	@Test
	public void testEmfPlusSetClipRectAndResetClipRenderMaskedGroup() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusClipRectComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<mask "));
		Assert.assertTrue(svg.contains("<g mask=\"url(#mask"));
		Assert.assertTrue(svg.contains("x=\"5\""));
		Assert.assertTrue(svg.contains("y=\"5\""));
		Assert.assertTrue(svg.contains("width=\"10\""));
		Assert.assertTrue(svg.contains("height=\"10\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusReplaceClipRectUsesLatestMask() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusReplaceClipRectComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<mask "));
		Assert.assertTrue(svg.contains("<g mask=\"url(#mask1)\">"));
		Assert.assertTrue(svg.contains("x=\"2\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
		Assert.assertTrue(svg.contains("width=\"4\""));
		Assert.assertTrue(svg.contains("height=\"4\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
	}

	@Test
	public void testEmfPlusXorClipRectSubtractsOverlapFromMask() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusCombinedClipRectComment(3));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<g mask=\"url(#mask1)\">"));
		Assert.assertTrue(svg.contains("mask=\"url(#mask0)\""));
		Assert.assertTrue(svg.contains("fill=\"black\""));
		Assert.assertTrue(svg.contains("x=\"5\""));
		Assert.assertTrue(svg.contains("width=\"10\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusComplementClipRectSubtractsOldMaskFromNewShape() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusCombinedClipRectComment(5));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<g mask=\"url(#mask1)\">"));
		Assert.assertTrue(svg.contains("mask=\"url(#mask0)\""));
		Assert.assertTrue(svg.contains("fill=\"black\""));
		Assert.assertTrue(svg.contains("x=\"-100000\""));
		Assert.assertTrue(svg.contains("width=\"200000\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusSetClipPathRendersPathMask() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusClipPathComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<mask "));
		Assert.assertTrue(svg.contains("<g mask=\"url(#mask"));
		Assert.assertTrue(svg.contains("d=\"M 0,0 L 10,0 L 0,10 L 0,0 Z\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
	}

	@Test
	public void testEmfPlusOffsetClipTranslatesCurrentMask() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusOffsetClipComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<mask "));
		Assert.assertTrue(svg.contains("transform=\"translate(3 4)\""));
		Assert.assertTrue(svg.contains("x=\"5\""));
		Assert.assertTrue(svg.contains("y=\"5\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
	}

	@Test
	public void testEmfPlusRegionObjectFillAndClipRenderSvgElements() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusRegionComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<mask "));
		Assert.assertTrue(svg.contains("<g mask=\"url(#mask"));
		Assert.assertTrue(svg.contains("x=\"2\""));
		Assert.assertTrue(svg.contains("y=\"3\""));
		Assert.assertTrue(svg.contains("width=\"10\""));
		Assert.assertTrue(svg.contains("height=\"12\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusEmptyRegionReplacesClipWithEmptyMask() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusEmptyClipRegionComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<mask "));
		Assert.assertTrue(svg.contains("<g mask=\"url(#mask1)\">"));
		Assert.assertTrue(svg.contains("width=\"0\""));
		Assert.assertTrue(svg.contains("height=\"0\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(255,0,0)\""));
	}

	@Test
	public void testEmfPlusPathRegionObjectFillsSvgPath() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPathRegionComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("d=\"M 0,0 L 10,0 L 0,10 L 0,0 Z\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
	}

	@Test
	public void testEmfPlusUnionRegionObjectFillsBothRects() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusUnionRegionComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(2, count(svg, "<rect "));
		Assert.assertTrue(svg.contains("<g fill=\"rgb(51,102,153)\" stroke=\"none\""));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
		Assert.assertTrue(svg.contains("width=\"3\""));
		Assert.assertTrue(svg.contains("height=\"4\""));
		Assert.assertTrue(svg.contains("x=\"10\""));
		Assert.assertTrue(svg.contains("y=\"20\""));
		Assert.assertTrue(svg.contains("width=\"30\""));
		Assert.assertTrue(svg.contains("height=\"40\""));
	}

	@Test
	public void testEmfPlusIntersectRegionObjectClipsFillToBothRects() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusIntersectRegionComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<clipPath "));
		Assert.assertTrue(svg.contains("clip-path=\"url(#clip"));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
		Assert.assertTrue(svg.contains("width=\"8\""));
		Assert.assertTrue(svg.contains("height=\"8\""));
		Assert.assertTrue(svg.contains("x=\"5\""));
		Assert.assertTrue(svg.contains("y=\"6\""));
		Assert.assertTrue(svg.contains("width=\"10\""));
		Assert.assertTrue(svg.contains("height=\"10\""));
	}

	@Test
	public void testEmfPlusExcludeRegionObjectMasksRightFromLeft() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusExcludeRegionComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<mask "));
		Assert.assertTrue(svg.contains("mask=\"url(#mask"));
		Assert.assertTrue(svg.contains("fill=\"black\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
		Assert.assertTrue(svg.contains("width=\"8\""));
		Assert.assertTrue(svg.contains("height=\"8\""));
		Assert.assertTrue(svg.contains("x=\"5\""));
		Assert.assertTrue(svg.contains("y=\"6\""));
		Assert.assertTrue(svg.contains("width=\"10\""));
		Assert.assertTrue(svg.contains("height=\"10\""));
	}

	@Test
	public void testEmfPlusComplementRegionObjectMasksLeftFromRight() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusComplementRegionComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<mask "));
		Assert.assertTrue(svg.contains("mask=\"url(#mask"));
		Assert.assertTrue(svg.contains("fill=\"black\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
		Assert.assertTrue(svg.contains("width=\"8\""));
		Assert.assertTrue(svg.contains("height=\"8\""));
		Assert.assertTrue(svg.contains("x=\"5\""));
		Assert.assertTrue(svg.contains("y=\"6\""));
		Assert.assertTrue(svg.contains("width=\"10\""));
		Assert.assertTrue(svg.contains("height=\"10\""));
	}

	@Test
	public void testEmfPlusXorRegionObjectUnionsBothDifferences() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusXorRegionComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertEquals(2, count(svg, "mask=\"url(#mask"));
		Assert.assertTrue(svg.contains("fill=\"black\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("x=\"1\""));
		Assert.assertTrue(svg.contains("y=\"2\""));
		Assert.assertTrue(svg.contains("width=\"8\""));
		Assert.assertTrue(svg.contains("height=\"8\""));
		Assert.assertTrue(svg.contains("x=\"5\""));
		Assert.assertTrue(svg.contains("y=\"6\""));
		Assert.assertTrue(svg.contains("width=\"10\""));
		Assert.assertTrue(svg.contains("height=\"10\""));
	}

	@Test
	public void testEmfPlusEmptyAndInfiniteRegionObjectsRenderExpectedArea() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusEmptyAndInfiniteRegionComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertFalse(svg.contains("rgb(255,0,0)"));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains("x=\"-100000\""));
		Assert.assertTrue(svg.contains("y=\"-100000\""));
		Assert.assertTrue(svg.contains("width=\"200000\""));
		Assert.assertTrue(svg.contains("height=\"200000\""));
	}

	@Test
	public void testEmfPlusTranslateAndScaleWorldTransformAffectShapes() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusTranslateAndScaleTransformComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("x=\"6\""));
		Assert.assertTrue(svg.contains("y=\"9\""));
		Assert.assertTrue(svg.contains("x=\"2\""));
		Assert.assertTrue(svg.contains("y=\"6\""));
		Assert.assertTrue(svg.contains("width=\"6\""));
		Assert.assertTrue(svg.contains("height=\"12\""));
	}

	@Test
	public void testEmfPlusSetPageTransformScalesPageCoordinates() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPageTransformComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("x=\"2\""));
		Assert.assertTrue(svg.contains("y=\"4\""));
		Assert.assertTrue(svg.contains("width=\"6\""));
		Assert.assertTrue(svg.contains("height=\"8\""));
		Assert.assertTrue(svg.contains("width=\"96\""));
		Assert.assertTrue(svg.contains("height=\"96\""));
	}

	@Test
	public void testEmfPlusSetPageTransformScalesWorldTransformResult() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusPageAndWorldTransformComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("x=\"12\""));
		Assert.assertTrue(svg.contains("y=\"18\""));
		Assert.assertTrue(svg.contains("width=\"6\""));
		Assert.assertTrue(svg.contains("height=\"8\""));
	}

	@Test
	public void testEmfPlusRelativePointRecordsRenderAbsoluteSvgPoints() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusRelativePointComment());
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("points=\"1,1 10,1 10,8\""));
		Assert.assertTrue(svg.contains("points=\"5,5 15,5 15,15 5,5\""));
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
			dib[pos] = (byte) (rgb & 0xFF);
			dib[pos + 1] = (byte) ((rgb >>> 8) & 0xFF);
			dib[pos + 2] = (byte) ((rgb >>> 16) & 0xFF);
		}
		return dib;
	}

	private BufferedImage readFirstPng(SvgGdi gdi) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		Matcher matcher = PNG_DATA_PATTERN.matcher(out.toString("UTF-8"));
		Assert.assertTrue(matcher.find());
		return ImageIO.read(new ByteArrayInputStream(java.util.Base64.getDecoder().decode(matcher.group(1))));
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
		dib[44] = (byte) 0xFF;
		dib[45] = (byte) 0xFF;
		dib[46] = (byte) 0xFF;
		for (int y = 0; y < height; y++) {
			dib[48 + y * stride] = (byte) rows[y];
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

	private byte[] createEmfPlusGetDCComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		writeEmfPlusRecord(comment, 0x4004, 0, new byte[0]);
		return comment.toByteArray();
	}

	private byte[] createEmfPlusSolidFillComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();

		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0x80336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400E, 0x8000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 3);
		writeFloat(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 10);
		writeFloat(payload, 1);
		writeFloat(payload, 10);
		writeFloat(payload, 8);
		writeEmfPlusRecord(comment, 0x400C, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusAntiAliasModeComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeEmfPlusRecord(comment, 0x401E, 0x0009, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPixelOffsetModeComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeEmfPlusRecord(comment, 0x4022, 0x0004, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusCompositingQualityComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeEmfPlusRecord(comment, 0x4024, 0x0005, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusCompositingModeComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeEmfPlusRecord(comment, 0x4023, 0x0001, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0x80336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusContainerStateComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 1);
		writeEmfPlusRecord(comment, 0x4028, 0, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4022, 0x0004, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeEmfPlusRecord(comment, 0x4029, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 40);
		writeFloat(payload, 2);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusBeginContainerTransformComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeFloat(payload, 100);
		writeFloat(payload, 100);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeInt(payload, 1);
		writeEmfPlusRecord(comment, 0x4027, 0x0002, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 3);
		writeFloat(payload, 4);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeEmfPlusRecord(comment, 0x4029, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 3);
		writeFloat(payload, 4);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTSGraphicsComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		payload.write(2);
		payload.write(0);
		payload.write(0);
		payload.write(5);
		writeShort(payload, 0);
		writeShort(payload, 0);
		writeShort(payload, 0);
		payload.write(0);
		payload.write(4);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x4039, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTSGraphicsCompositingModeComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		payload.write(0);
		payload.write(0);
		payload.write(1);
		payload.write(0);
		writeShort(payload, 0);
		writeShort(payload, 0);
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

		payload.reset();
		writeInt(payload, 0x80336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTSClipComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeShort(payload, 1);
		writeShort(payload, 2);
		writeShort(payload, 10);
		writeShort(payload, 12);
		writeShort(payload, 20);
		writeShort(payload, 21);
		writeShort(payload, 30);
		writeShort(payload, 31);
		writeEmfPlusRecord(comment, 0x403A, 2, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 40);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusCompressedTSClipComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		payload.write(0x81);
		payload.write(0x82);
		payload.write(0x8A);
		payload.write(0x8A);
		payload.write(0x93);
		payload.write(0x93);
		payload.write(0x94);
		payload.write(0x8A);
		writeEmfPlusRecord(comment, 0x403A, 0x8002, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 40);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusLinearGradientBrushComment() {
		return createEmfPlusLinearGradientBrushComment(0);
	}

	private byte[] createEmfPlusLinearGradientBrushComment(int wrapMode) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0);
		writeInt(payload, wrapMode);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPathGradientBrushComment() {
		return createEmfPlusPathGradientBrushComment(0);
	}

	private byte[] createEmfPlusGammaCorrectedLinearGradientBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0x00000080);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPathGradientBrushComment(int wrapMode) {
		return createEmfPlusPathGradientBrushComment(wrapMode, 0x00000001);
	}

	private byte[] createEmfPlusPathGradientBrushComment(int wrapMode, int brushDataFlags) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream path = new ByteArrayOutputStream();
		writeInt(path, 0);
		writeInt(path, 4);
		writeInt(path, 0);
		writeFloat(path, 0);
		writeFloat(path, 0);
		writeFloat(path, 20);
		writeFloat(path, 0);
		writeFloat(path, 20);
		writeFloat(path, 10);
		writeFloat(path, 0);
		writeFloat(path, 10);
		path.write(0);
		path.write(1);
		path.write(1);
		path.write(0x81);

		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeInt(payload, brushDataFlags);
		writeInt(payload, wrapMode);
		writeInt(payload, 0xFFFF0000);
		writeFloat(payload, 10);
		writeFloat(payload, 5);
		writeInt(payload, 1);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, path.size());
		payload.write(path.toByteArray(), 0, path.size());
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusFocusScalePathGradientBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream path = new ByteArrayOutputStream();
		writeInt(path, 0);
		writeInt(path, 4);
		writeInt(path, 0);
		writeFloat(path, 0);
		writeFloat(path, 0);
		writeFloat(path, 20);
		writeFloat(path, 0);
		writeFloat(path, 20);
		writeFloat(path, 10);
		writeFloat(path, 0);
		writeFloat(path, 10);
		path.write(0);
		path.write(1);
		path.write(1);
		path.write(0x81);

		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeInt(payload, 0x00000041);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeFloat(payload, 10);
		writeFloat(payload, 5);
		writeInt(payload, 1);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, path.size());
		payload.write(path.toByteArray(), 0, path.size());
		writeFloat(payload, 0.5f);
		writeFloat(payload, 0.5f);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPresetPathGradientBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream path = new ByteArrayOutputStream();
		writeInt(path, 0);
		writeInt(path, 4);
		writeInt(path, 0);
		writeFloat(path, 0);
		writeFloat(path, 0);
		writeFloat(path, 20);
		writeFloat(path, 0);
		writeFloat(path, 20);
		writeFloat(path, 10);
		writeFloat(path, 0);
		writeFloat(path, 10);
		path.write(0);
		path.write(1);
		path.write(1);
		path.write(0x81);

		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeInt(payload, 0x00000005);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeFloat(payload, 10);
		writeFloat(payload, 5);
		writeInt(payload, 1);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, path.size());
		payload.write(path.toByteArray(), 0, path.size());
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0.5f);
		writeFloat(payload, 1);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0x8000FF00);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusBlendFactorPathGradientBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream path = new ByteArrayOutputStream();
		writeInt(path, 0);
		writeInt(path, 4);
		writeInt(path, 0);
		writeFloat(path, 0);
		writeFloat(path, 0);
		writeFloat(path, 20);
		writeFloat(path, 0);
		writeFloat(path, 20);
		writeFloat(path, 10);
		writeFloat(path, 0);
		writeFloat(path, 10);
		path.write(0);
		path.write(1);
		path.write(1);
		path.write(0x81);

		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeInt(payload, 0x00000009);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeFloat(payload, 10);
		writeFloat(payload, 5);
		writeInt(payload, 1);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, path.size());
		payload.write(path.toByteArray(), 0, path.size());
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0.25f);
		writeFloat(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 0.25f);
		writeFloat(payload, 0);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPresetLinearGradientBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0x00000004);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0.5f);
		writeFloat(payload, 1);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0x8000FF00);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusBlendFactorLinearGradientBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0x00000008);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0.25f);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0.25f);
		writeFloat(payload, 1);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusVerticalBlendFactorLinearGradientBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0x00000010);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0.75f);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0.75f);
		writeFloat(payload, 1);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTransformLinearGradientBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0x00000002);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF0000FF);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusHatchBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 4);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusRenderingOriginHatchBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 3);
		writeInt(payload, 5);
		writeEmfPlusRecord(comment, 0x401D, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 4);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTextureBrushComment(byte[] png) {
		return createEmfPlusTextureBrushComment(png, 0x00000002);
	}

	private byte[] createEmfPlusTextureBrushComment(byte[] png, int brushDataFlags) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeInt(payload, brushDataFlags);
		writeInt(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 4);
		writeFloat(payload, 5);
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 2);
		writeInt(payload, png.length);
		payload.write(png, 0, png.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0101, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPenDrawComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 3.5f);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 5);
		writeFloat(payload, 20);
		writeFloat(payload, 0);
		writeEmfPlusRecord(comment, 0x400D, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 4);
		writeFloat(payload, 6);
		writeFloat(payload, 8);
		writeEmfPlusRecord(comment, 0x400B, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusContinuedPenDrawComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 3.5f);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		byte[] pen = payload.toByteArray();
		byte[] first = new byte[12];
		byte[] second = new byte[pen.length - first.length];
		System.arraycopy(pen, 0, first, 0, first.length);
		System.arraycopy(pen, first.length, second, 0, second.length);
		writeEmfPlusContinuedObjectRecord(comment, 0x0200, pen.length, first);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, second);

		payload.reset();
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 5);
		writeFloat(payload, 20);
		writeFloat(payload, 0);
		writeEmfPlusRecord(comment, 0x400D, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDashedPenComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x000000BE);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 2);
		writeInt(payload, 2);
		writeInt(payload, 1);
		writeFloat(payload, 7);
		writeInt(payload, 3);
		writeFloat(payload, 1.5f);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 0);
		writeEmfPlusRecord(comment, 0x400D, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTransformedDashedPenComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x000000BF);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeFloat(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeInt(payload, 2);
		writeInt(payload, 2);
		writeInt(payload, 1);
		writeFloat(payload, 7);
		writeInt(payload, 3);
		writeFloat(payload, 1.5f);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 0);
		writeEmfPlusRecord(comment, 0x400D, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPointUnitPenComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeFloat(payload, 72);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 0);
		writeEmfPlusRecord(comment, 0x400D, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusCustomDashedPenComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x00000100);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 4);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 4);
		writeFloat(payload, 1);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 5);
		writeFloat(payload, 20);
		writeFloat(payload, 5);
		writeEmfPlusRecord(comment, 0x400D, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusRoundDashCapPenComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x00000060);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 2);
		writeInt(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 5);
		writeFloat(payload, 20);
		writeFloat(payload, 5);
		writeEmfPlusRecord(comment, 0x400D, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDrawImageComment(int objectId, byte[] png) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 2);
		writeInt(payload, png.length);
		payload.write(png, 0, png.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500 | objectId, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x401A, objectId, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDrawImageSourceRectComment(int objectId, byte[] png) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 2);
		writeInt(payload, png.length);
		payload.write(png, 0, png.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500 | objectId, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x401A, objectId, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusRawBitmapDrawImageComment(int objectId) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 2);
		writeInt(payload, 1);
		writeInt(payload, 8);
		writeInt(payload, 0x0026200A);
		writeInt(payload, 0);
		payload.write(0);
		payload.write(0);
		payload.write(0xFF);
		payload.write(0xFF);
		payload.write(0);
		payload.write(0xFF);
		payload.write(0);
		payload.write(0x80);
		writeEmfPlusRecord(comment, 0x4008, 0x0500 | objectId, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x401A, objectId, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusRawBitmapDrawImageComment(int objectId, int pixelFormat, int stride, byte[] pixels) {
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
		writeEmfPlusRecord(comment, 0x4008, 0x0500 | objectId, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x401A, objectId, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusIndexedBitmapDrawImageComment(int objectId) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 2);
		writeInt(payload, 1);
		writeInt(payload, 4);
		writeInt(payload, 0x00030803);
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 2);
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 0x8000FF00);
		payload.write(0);
		payload.write(1);
		payload.write(0);
		payload.write(0);
		writeEmfPlusRecord(comment, 0x4008, 0x0500 | objectId, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x401A, objectId, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusInterpolationImageComment(int objectId, int interpolationMode, byte[] png) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		writeEmfPlusRecord(comment, 0x4021, interpolationMode, new byte[0]);

		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 2);
		writeInt(payload, png.length);
		payload.write(png, 0, png.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500 | objectId, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x401A, objectId, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusArcAndPieComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 0);
		writeFloat(payload, 90);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 100);
		writeFloat(payload, 50);
		writeEmfPlusRecord(comment, 0x4012, 0, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 90);
		writeFloat(payload, -45);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 100);
		writeFloat(payload, 50);
		writeEmfPlusRecord(comment, 0x4011, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeFloat(payload, 0);
		writeFloat(payload, 90);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 100);
		writeFloat(payload, 50);
		writeEmfPlusRecord(comment, 0x4010, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPathComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0201, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 5);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 5);
		writeFloat(payload, 5);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		payload.write(0);
		payload.write(1);
		payload.write(3);
		payload.write(3);
		payload.write(0x83);
		while (payload.size() % 4 != 0) {
			payload.write(0);
		}
		writeEmfPlusRecord(comment, 0x4008, 0x0300, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4014, 0x8000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeEmfPlusRecord(comment, 0x4015, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusStrokeFillPathComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0201, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		payload.write(0);
		payload.write(1);
		payload.write(1);
		payload.write(0x81);
		writeEmfPlusRecord(comment, 0x4008, 0x0300, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4037, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusRelativePathComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0201, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0x0800);
		payload.write(0);
		payload.write(0);
		payload.write(10);
		payload.write(0);
		payload.write(0);
		payload.write(10);
		payload.write(-10);
		payload.write(0);
		payload.write(0x41);
		payload.write(0);
		payload.write(0x42);
		payload.write(1);
		payload.write(0x41);
		payload.write(0x81);
		while (payload.size() % 4 != 0) {
			payload.write(0);
		}
		writeEmfPlusRecord(comment, 0x4008, 0x0300, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4014, 0x8000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 1);
		writeEmfPlusRecord(comment, 0x4015, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusClearAndBeziersComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0x80336699);
		writeEmfPlusRecord(comment, 0x4009, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 4);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x4019, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusClosedCurveComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeFloat(payload, 0);
		writeInt(payload, 4);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x4016, 0x8000, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 0);
		writeInt(payload, 4);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x4017, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDrawCurveComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 2);
		writeInt(payload, 4);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeFloat(payload, 30);
		writeFloat(payload, 0);
		writeEmfPlusRecord(comment, 0x4018, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDrawStringComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 3);
		writeInt(payload, 0x00000007);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 0);
		writeInt(payload, "Hello EMF+".length());
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 120);
		writeFloat(payload, 20);
		writeUtf16Le(payload, "Hello EMF+");
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusFontUnitDrawStringComment(int sizeUnit, float emSize, String text) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, emSize);
		writeInt(payload, sizeUnit);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 0);
		writeInt(payload, text.length());
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 120);
		writeFloat(payload, 40);
		writeUtf16Le(payload, text);
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTextRenderingHintDrawStringComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 3);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x401F, 5, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 0);
		writeInt(payload, "Hinted".length());
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 120);
		writeFloat(payload, 20);
		writeUtf16Le(payload, "Hinted");
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTextContrastDrawStringComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 3);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4020, 1200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 0);
		writeInt(payload, "Contrast".length());
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 120);
		writeFloat(payload, 20);
		writeUtf16Le(payload, "Contrast");
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusTSGraphicsTextContrastDrawStringComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 3);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		payload.write(0);
		payload.write(0);
		payload.write(0);
		payload.write(0);
		writeShort(payload, 0);
		writeShort(payload, 0);
		writeShort(payload, 8);
		payload.write(0);
		payload.write(0);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeEmfPlusRecord(comment, 0x4039, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 0);
		writeInt(payload, "TS".length());
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 120);
		writeFloat(payload, 20);
		writeUtf16Le(payload, "TS");
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusAlignedDrawStringComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 3);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 1);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 1);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeEmfPlusRecord(comment, 0x4008, 0x0701, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeInt(payload, "Centered".length());
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 120);
		writeFloat(payload, 20);
		writeUtf16Le(payload, "Centered");
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusNoClipDrawStringComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 3);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0x00004000);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 1);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeEmfPlusRecord(comment, 0x4008, 0x0701, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeInt(payload, "No clip".length());
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeUtf16Le(payload, "No clip");
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusFormattedDrawStringComment(int formatFlags, int hotkeyPrefix, String text) {
		return createEmfPlusFormattedDrawStringComment(formatFlags, hotkeyPrefix, 1.0f, text);
	}

	private byte[] createEmfPlusFormattedDrawStringComment(int formatFlags, int hotkeyPrefix, float tracking,
			String text) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 3);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, formatFlags);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeInt(payload, hotkeyPrefix);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, tracking);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeEmfPlusRecord(comment, 0x4008, 0x0701, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeInt(payload, text.length());
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 120);
		writeFloat(payload, 20);
		writeUtf16Le(payload, text);
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDrawDriverStringComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 3);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 0x00000001);
		writeInt(payload, 0);
		writeInt(payload, "ABC".length());
		writeUtf16Le(payload, "ABC");
		while (payload.size() % 4 != 0) {
			payload.write(0);
		}
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeFloat(payload, 15);
		writeFloat(payload, 7);
		writeFloat(payload, 25);
		writeFloat(payload, 7);
		writeEmfPlusRecord(comment, 0x4036, 0x8000, payload.toByteArray());
		return comment.toByteArray();
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
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4031, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusReplaceClipRectComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeFloat(payload, 50);
		writeFloat(payload, 60);
		writeFloat(payload, 70);
		writeFloat(payload, 80);
		writeEmfPlusRecord(comment, 0x4032, 0, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 2);
		writeFloat(payload, 2);
		writeFloat(payload, 4);
		writeFloat(payload, 4);
		writeEmfPlusRecord(comment, 0x4032, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusCombinedClipRectComment(int combineMode) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x4032, 0, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 5);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x4032, combineMode << 8, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusClipPathComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		payload.write(0);
		payload.write(1);
		payload.write(1);
		payload.write(0x81);
		writeEmfPlusRecord(comment, 0x4008, 0x0300, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4033, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusOffsetClipComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeFloat(payload, 5);
		writeFloat(payload, 5);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x4032, 0, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 3);
		writeFloat(payload, 4);
		writeEmfPlusRecord(comment, 0x4035, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusEmptyClipRegionComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x10000000);
		writeFloat(payload, 2);
		writeFloat(payload, 3);
		writeFloat(payload, 10);
		writeFloat(payload, 12);
		writeEmfPlusRecord(comment, 0x4008, 0x0400, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 0x10000002);
		writeEmfPlusRecord(comment, 0x4008, 0x0401, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4034, 0, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4034, 1, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusRegionComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x10000000);
		writeFloat(payload, 2);
		writeFloat(payload, 3);
		writeFloat(payload, 10);
		writeFloat(payload, 12);
		writeEmfPlusRecord(comment, 0x4008, 0x0400, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4013, 0x8000, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x4034, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPathRegionComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream path = createEmfPlusTrianglePath();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x10000001);
		writeInt(payload, path.size());
		payload.write(path.toByteArray(), 0, path.size());
		writeEmfPlusRecord(comment, 0x4008, 0x0401, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4013, 0x8001, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusUnionRegionComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeInt(payload, 2);
		writeInt(payload, 0x10000000);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 3);
		writeFloat(payload, 4);
		writeInt(payload, 0x10000000);
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeFloat(payload, 30);
		writeFloat(payload, 40);
		writeEmfPlusRecord(comment, 0x4008, 0x0400, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4013, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusIntersectRegionComment() {
		return createEmfPlusBooleanRegionComment(1);
	}

	private byte[] createEmfPlusExcludeRegionComment() {
		return createEmfPlusBooleanRegionComment(4);
	}

	private byte[] createEmfPlusXorRegionComment() {
		return createEmfPlusBooleanRegionComment(3);
	}

	private byte[] createEmfPlusComplementRegionComment() {
		return createEmfPlusBooleanRegionComment(5);
	}

	private byte[] createEmfPlusBooleanRegionComment(int type) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 3);
		writeInt(payload, type);
		writeInt(payload, 0x10000000);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 8);
		writeFloat(payload, 8);
		writeInt(payload, 0x10000000);
		writeFloat(payload, 5);
		writeFloat(payload, 6);
		writeFloat(payload, 10);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x4008, 0x0400, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4013, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusEmptyAndInfiniteRegionComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 0x10000002);
		writeEmfPlusRecord(comment, 0x4008, 0x0400, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4013, 0x8000, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 0x10000003);
		writeEmfPlusRecord(comment, 0x4008, 0x0401, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeEmfPlusRecord(comment, 0x4013, 0x8001, payload.toByteArray());
		return comment.toByteArray();
	}

	private ByteArrayOutputStream createEmfPlusTrianglePath() {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		payload.write(0);
		payload.write(1);
		payload.write(1);
		payload.write(0x81);
		return payload;
	}

	private byte[] createEmfPlusTranslateAndScaleTransformComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeEmfPlusRecord(comment, 0x402D, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 3);
		writeFloat(payload, 4);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeEmfPlusRecord(comment, 0x402B, 0, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 2);
		writeFloat(payload, 3);
		writeEmfPlusRecord(comment, 0x402E, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 3);
		writeFloat(payload, 4);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPageTransformComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeFloat(payload, 2);
		writeEmfPlusRecord(comment, 0x4030, 2, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 3);
		writeFloat(payload, 4);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 1);
		writeEmfPlusRecord(comment, 0x4030, 3, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFFFF0000);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 72);
		writeFloat(payload, 72);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusPageAndWorldTransformComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeFloat(payload, 2);
		writeEmfPlusRecord(comment, 0x4030, 2, payload.toByteArray());

		payload.reset();
		writeFloat(payload, 5);
		writeFloat(payload, 7);
		writeEmfPlusRecord(comment, 0x402D, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 1);
		writeFloat(payload, 2);
		writeFloat(payload, 3);
		writeFloat(payload, 4);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusRelativePointComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 3);
		payload.write(1);
		payload.write(1);
		payload.write(9);
		payload.write(0);
		payload.write(0);
		payload.write(7);
		while (payload.size() % 4 != 0) {
			payload.write(0);
		}
		writeEmfPlusRecord(comment, 0x400C, 0x8800, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFFFF0000);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 3);
		payload.write(5);
		payload.write(5);
		payload.write(10);
		payload.write(0);
		payload.write(0);
		payload.write(10);
		while (payload.size() % 4 != 0) {
			payload.write(0);
		}
		writeEmfPlusRecord(comment, 0x400D, 0x2800, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createPng(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, 0xFFFF0000);
		if (width > 1) {
			image.setRGB(1, 0, 0xFF00FF00);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	private byte[] createGif(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, 0xFFFF0000);
		if (width > 1) {
			image.setRGB(1, 0, 0xFF00FF00);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "gif", out);
		return out.toByteArray();
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

	private void writeEmfPlusContinuedObjectRecord(ByteArrayOutputStream out, int flags, int totalObjectSize,
			byte[] data) {
		writeShort(out, 0x4008);
		writeShort(out, flags | 0x8000);
		writeInt(out, data.length + 16);
		writeInt(out, totalObjectSize);
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

	private void writeUtf16Le(ByteArrayOutputStream out, String value) {
		for (int i = 0; i < value.length(); i++) {
			writeShort(out, value.charAt(i));
		}
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

	private void assertFontFamily(String faceName, String expectedFontFamily) throws Exception {
		Assert.assertTrue(renderFontStyle(faceName).contains(expectedFontFamily));
	}

	private String writeSvg(SvgGdi gdi) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		return out.toString("UTF-8");
	}

	private String renderFontStyle(String faceName) throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.createFontIndirect(-12, 0, 0, 0, GdiFont.FW_NORMAL, false, false, false, GdiFont.ANSI_CHARSET, 0, 0, 0, 0,
				faceName.getBytes("US-ASCII"));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		return out.toString("UTF-8");
	}

	private void setUInt16(byte[] data, int pos, int value) {
		data[pos] = (byte) (value & 0xFF);
		data[pos + 1] = (byte) ((value >>> 8) & 0xFF);
	}

	private void setInt32(byte[] data, int pos, int value) {
		data[pos] = (byte) (value & 0xFF);
		data[pos + 1] = (byte) ((value >>> 8) & 0xFF);
		data[pos + 2] = (byte) ((value >>> 16) & 0xFF);
		data[pos + 3] = (byte) ((value >>> 24) & 0xFF);
	}
}

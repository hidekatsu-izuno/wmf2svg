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
		Assert.assertTrue(svg.contains("stop-color=\"rgb(255,0,0)\""));
		Assert.assertTrue(svg.contains("stop-color=\"rgb(0,0,255)\""));
		Assert.assertTrue(svg.contains("fill=\"url(#gradient"));
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
		Assert.assertTrue(svg.contains("stroke-dashoffset=\"3\""));
		Assert.assertTrue(svg.contains("stroke-dasharray=\"6,6,2,6\""));
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
	public void testEmfPlusSetInterpolationModeNearestNeighborRendersPixelatedImage() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.header();
		gdi.comment(createEmfPlusNearestNeighborImageComment(0, createPng(2, 1)));
		gdi.footer();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = out.toString("UTF-8");
		Assert.assertTrue(svg.contains("<image "));
		Assert.assertTrue(svg.contains("image-rendering=\"pixelated\""));
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
		Assert.assertTrue(svg.contains("<text "));
		Assert.assertTrue(svg.contains("x=\"5\""));
		Assert.assertTrue(svg.contains("y=\"7\""));
		Assert.assertTrue(svg.contains("font-family=\"Arial\""));
		Assert.assertTrue(svg.contains("font-size=\"16\""));
		Assert.assertTrue(svg.contains("font-weight=\"bold\""));
		Assert.assertTrue(svg.contains("font-style=\"italic\""));
		Assert.assertTrue(svg.contains("text-decoration=\"underline\""));
		Assert.assertTrue(svg.contains("fill=\"rgb(51,102,153)\""));
		Assert.assertTrue(svg.contains(">Hello EMF+</text>"));
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
		Assert.assertTrue(svg.contains("font-size=\"16\""));
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

	private byte[] createEmfPlusLinearGradientBrushComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 4);
		writeInt(payload, 0);
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

	private byte[] createEmfPlusPathGradientBrushComment() {
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
		writeInt(payload, 0x00000001);
		writeInt(payload, 0);
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

	private byte[] createEmfPlusTextureBrushComment(byte[] png) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeInt(payload, 0x00000002);
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

	private byte[] createEmfPlusDashedPenComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x000000AE);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 2);
		writeInt(payload, 2);
		writeInt(payload, 1);
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

	private byte[] createEmfPlusNearestNeighborImageComment(int objectId, byte[] png) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		writeEmfPlusRecord(comment, 0x4021, 5, new byte[0]);

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

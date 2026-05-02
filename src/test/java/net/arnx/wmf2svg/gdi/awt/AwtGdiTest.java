package net.arnx.wmf2svg.gdi.awt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiColorSpace;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiPalette;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.GdiPatternBrush;
import net.arnx.wmf2svg.gdi.GdiRegion;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Size;
import net.arnx.wmf2svg.gdi.emf.EmfGdi;

public class AwtGdiTest {
	@Test
	public void testResolveFontFamilyKeepsRequestedInstalledFont() {
		assertEquals("Fixedsys", AwtGdi.resolveFontFamily("Fixedsys", new String[]{"Consolas", "Fixedsys"}));
	}

	@Test
	public void testResolveFontFamilyUsesFirstInstalledFallback() {
		assertEquals("Consolas", AwtGdi.resolveFontFamily("Fixedsys", new String[]{"Consolas"}));
		assertEquals("Courier New", AwtGdi.resolveFontFamily("Modern", new String[]{"Courier New"}));
		assertEquals("Times New Roman", AwtGdi.resolveFontFamily("MS Serif", new String[]{"Times New Roman"}));
		assertEquals("Cascadia Mono", AwtGdi.resolveFontFamily("@Terminal", new String[]{"Cascadia Mono"}));
	}

	@Test
	public void testResolveFontFamilySkipsGenericFamilies() {
		assertEquals(Font.SANS_SERIF, AwtGdi.resolveFontFamily("MS Serif", new String[]{"Serif"}));
		assertEquals(Font.SANS_SERIF, AwtGdi.resolveFontFamily("serif", new String[]{"Serif"}));
	}

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
	public void testPendingEmfUsesCapturedOuterMappingWhenMappingPrecedesComment() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(-10, -20, null);
		gdi.setWindowExtEx(100, 100, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(100, 100, null);
		gdi.escape(createEscapeRecord(0x1234, createBackgroundEmf()));
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0, countPaintedPixels(image, 0, 0, 5, 5));
		assertTrue(countPaintedPixels(image, 12, 22, 20, 20) > 0);
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
	public void testPolylineDoesNotCloseToFirstPoint() {
		AwtGdi gdi = createMappedGdi(30, 30);
		gdi.selectObject(gdi.createPenIndirect(GdiPen.PS_SOLID, 1, 0x0000FF));
		gdi.polyline(new Point[]{new Point(2, 2), new Point(20, 2), new Point(20, 20)});

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image, 2, 2, 19, 2) > 0);
		assertTrue(countPaintedPixels(image, 19, 2, 2, 19) > 0);
		assertEquals(0, countPaintedPixels(image, 2, 18, 4, 4));
	}

	@Test
	public void testFillPathUsesCurrentPolyFillMode() {
		AwtGdi gdi = createMappedGdi(100, 100);
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0, 0));
		gdi.selectObject(gdi.createPenIndirect(GdiPen.PS_NULL, 1, 0));
		gdi.setPolyFillMode(Gdi.ALTERNATE);
		gdi.beginPath();
		gdi.polygon(new Point[]{new Point(10, 10), new Point(90, 10), new Point(90, 90), new Point(10, 90)});
		gdi.polygon(new Point[]{new Point(30, 30), new Point(70, 30), new Point(70, 70), new Point(30, 70)});
		gdi.setPolyFillMode(Gdi.WINDING);
		gdi.fillPath();

		assertTrue(((gdi.getImage().getRGB(50, 50) >>> 24) & 0xFF) != 0);
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
	public void testClipRegionReturnValuesReflectCurrentClip() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(100, 100, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(100, 100, null);

		assertEquals(GdiRegion.NULLREGION, gdi.extSelectClipRgn(null, GdiRegion.RGN_COPY));
		assertEquals(GdiRegion.SIMPLEREGION,
				gdi.extSelectClipRgn(gdi.createRectRgn(10, 10, 90, 90), GdiRegion.RGN_COPY));
		assertEquals(GdiRegion.SIMPLEREGION, gdi.setMetaRgn());
		assertEquals(GdiRegion.COMPLEXREGION, gdi.excludeClipRect(30, 30, 70, 70));
		assertEquals(GdiRegion.NULLREGION, gdi.extSelectClipRgn(null, GdiRegion.RGN_COPY));
		assertEquals(GdiRegion.NULLREGION, gdi.setMetaRgn());
	}

	@Test
	public void testExtCreateRegionAppliesXform() {
		AwtGdi gdi = createMappedGdi(40, 40);
		GdiRegion rgn = gdi.extCreateRegion(new float[]{1, 0, 0, 1, 10, 5}, 1, createRegionData(0, 0, 4, 4));
		gdi.fillRgn(rgn, gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0, 0));

		BufferedImage image = gdi.getImage();
		assertEquals(0, countPaintedPixels(image, 0, 0, 5, 5));
		assertTrue(countPaintedPixels(image, 10, 5, 4, 4) > 0);
	}

	@Test
	public void testSetWindowOrgExResetsPriorOffset() {
		AwtGdi gdi = createMappedGdi(40, 40);
		Point old = new Point(0, 0);
		gdi.setWindowOrgEx(10, 0, null);
		gdi.offsetWindowOrgEx(5, 0, old);
		assertEquals(new Point(10, 0), old);

		gdi.setWindowOrgEx(20, 0, old);
		assertEquals(new Point(15, 0), old);
		gdi.setPixel(20, 0, 0);

		assertTrue(countPaintedPixels(gdi.getImage(), 0, 0, 1, 1) > 0);
	}

	@Test
	public void testSetViewportOrgExResetsPriorOffset() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		Point old = new Point(0, 0);
		gdi.setViewportOrgEx(1, 1, null);
		gdi.offsetViewportOrgEx(1, 1, old);
		assertEquals(new Point(1, 1), old);

		gdi.setViewportOrgEx(3, 2, old);
		assertEquals(new Point(2, 2), old);
		gdi.setPixel(0, 0, 0);

		assertTrue(countPaintedPixels(gdi.getImage(), 45, 30, 1, 1) > 0);
		assertEquals(0, countPaintedPixels(gdi.getImage(), 60, 45, 1, 1));
	}

	@Test
	public void testScaleExtExReturnsEffectivePreviousExtents() {
		AwtGdi gdi = createMappedGdi(100, 120);
		Size old = new Size(0, 0);

		gdi.scaleWindowExtEx(2, 1, 3, 2, old);
		assertEquals(new Size(100, 120), old);
		gdi.scaleWindowExtEx(1, 4, 1, 3, old);
		assertEquals(new Size(200, 180), old);

		gdi.scaleViewportExtEx(3, 2, 2, 1, old);
		assertEquals(new Size(100, 120), old);
		gdi.scaleViewportExtEx(1, 5, 1, 4, old);
		assertEquals(new Size(150, 240), old);
	}

	@Test
	public void testColorManagementBookkeepingFollowsDcState() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();

		byte[] colorAdjustment = new byte[]{1, 2, 3};
		gdi.setColorAdjustment(colorAdjustment);
		colorAdjustment[0] = 9;
		assertArrayEquals(new byte[]{1, 2, 3}, gdi.getColorAdjustment());

		byte[] returnedColorAdjustment = gdi.getColorAdjustment();
		returnedColorAdjustment[1] = 9;
		assertArrayEquals(new byte[]{1, 2, 3}, gdi.getColorAdjustment());

		assertEquals(0, gdi.setICMMode(2));
		assertEquals(2, gdi.getICMMode());
		assertEquals(2, gdi.setICMMode(3));
		assertEquals(3, gdi.getICMMode());

		byte[] profile = new byte[]{4, 5, 6};
		assertTrue(gdi.setICMProfile(profile));
		profile[0] = 9;
		assertArrayEquals(new byte[]{4, 5, 6}, gdi.getICMProfile());

		gdi.seveDC();
		gdi.setICMMode(7);
		gdi.setColorAdjustment(new byte[]{8, 9});
		assertTrue(gdi.colorMatchToTarget(1, 0, new byte[]{10, 11}));
		assertArrayEquals(new byte[]{10, 11}, gdi.getICMProfile());

		gdi.restoreDC(-1);
		assertEquals(3, gdi.getICMMode());
		assertArrayEquals(new byte[]{1, 2, 3}, gdi.getColorAdjustment());
		assertArrayEquals(new byte[]{4, 5, 6}, gdi.getICMProfile());
	}

	@Test
	public void testMiscDcStateBookkeepingFollowsSaveRestore() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setLayout(Gdi.LAYOUT_RTL | Gdi.LAYOUT_BITMAPORIENTATIONPRESERVED);
		gdi.setMapperFlags(0x80000001L);
		gdi.setRelAbs(Gdi.RELATIVE);

		assertEquals(Gdi.LAYOUT_RTL | Gdi.LAYOUT_BITMAPORIENTATIONPRESERVED, gdi.getLayout());
		assertEquals(0x80000001L, gdi.getMapperFlags());
		assertEquals(Gdi.RELATIVE, gdi.getRelAbs());

		gdi.seveDC();
		gdi.setLayout(0);
		gdi.setMapperFlags(0);
		gdi.setRelAbs(Gdi.ABSOLUTE);

		gdi.restoreDC(-1);
		assertEquals(Gdi.LAYOUT_RTL | Gdi.LAYOUT_BITMAPORIENTATIONPRESERVED, gdi.getLayout());
		assertEquals(0x80000001L, gdi.getMapperFlags());
		assertEquals(Gdi.RELATIVE, gdi.getRelAbs());
	}

	@Test
	public void testCoreDrawingStateBookkeepingFollowsSaveRestore() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setBkColor(0x0000FF);
		gdi.setBkMode(Gdi.TRANSPARENT);
		gdi.setTextColor(0x00FF00);
		gdi.setTextAlign(Gdi.TA_UPDATECP | Gdi.TA_RIGHT | Gdi.TA_BASELINE);
		gdi.setTextCharacterExtra(5);
		gdi.setPolyFillMode(Gdi.WINDING);
		gdi.setROP2(Gdi.R2_XORPEN);
		gdi.setStretchBltMode(Gdi.STRETCH_HALFTONE);
		gdi.setArcDirection(Gdi.AD_CLOCKWISE);
		gdi.setMiterLimit(3.5f);

		assertEquals(0x0000FF, gdi.getBkColor());
		assertEquals(Gdi.TRANSPARENT, gdi.getBkMode());
		assertEquals(0x00FF00, gdi.getTextColor());
		assertEquals(Gdi.TA_UPDATECP | Gdi.TA_RIGHT | Gdi.TA_BASELINE, gdi.getTextAlign());
		assertEquals(5, gdi.getTextCharacterExtra());
		assertEquals(Gdi.WINDING, gdi.getPolyFillMode());
		assertEquals(Gdi.R2_XORPEN, gdi.getROP2());
		assertEquals(Gdi.STRETCH_HALFTONE, gdi.getStretchBltMode());
		assertEquals(Gdi.AD_CLOCKWISE, gdi.getArcDirection());
		assertEquals(3.5f, gdi.getMiterLimit(), 0.0f);

		gdi.seveDC();
		gdi.setBkColor(0);
		gdi.setBkMode(Gdi.OPAQUE);
		gdi.setTextColor(0);
		gdi.setTextAlign(Gdi.TA_LEFT | Gdi.TA_TOP);
		gdi.setTextCharacterExtra(0);
		gdi.setPolyFillMode(Gdi.ALTERNATE);
		gdi.setROP2(Gdi.R2_COPYPEN);
		gdi.setStretchBltMode(Gdi.STRETCH_ANDSCANS);
		gdi.setArcDirection(Gdi.AD_COUNTERCLOCKWISE);
		gdi.setMiterLimit(10.0f);

		gdi.restoreDC(-1);
		assertEquals(0x0000FF, gdi.getBkColor());
		assertEquals(Gdi.TRANSPARENT, gdi.getBkMode());
		assertEquals(0x00FF00, gdi.getTextColor());
		assertEquals(Gdi.TA_UPDATECP | Gdi.TA_RIGHT | Gdi.TA_BASELINE, gdi.getTextAlign());
		assertEquals(5, gdi.getTextCharacterExtra());
		assertEquals(Gdi.WINDING, gdi.getPolyFillMode());
		assertEquals(Gdi.R2_XORPEN, gdi.getROP2());
		assertEquals(Gdi.STRETCH_HALFTONE, gdi.getStretchBltMode());
		assertEquals(Gdi.AD_CLOCKWISE, gdi.getArcDirection());
		assertEquals(3.5f, gdi.getMiterLimit(), 0.0f);
	}

	@Test
	public void testCurrentPositionBookkeepingFollowsSaveRestore() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.moveToEx(7, 9, null);

		assertEquals(7, gdi.getCurrentX());
		assertEquals(9, gdi.getCurrentY());

		gdi.seveDC();
		Point old = new Point(0, 0);
		gdi.moveToEx(11, 13, old);

		assertEquals(7, old.x);
		assertEquals(9, old.y);
		assertEquals(11, gdi.getCurrentX());
		assertEquals(13, gdi.getCurrentY());

		gdi.restoreDC(-1);
		assertEquals(7, gdi.getCurrentX());
		assertEquals(9, gdi.getCurrentY());
	}

	@Test
	public void testBrushOriginBookkeepingFollowsSaveRestore() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setBrushOrgEx(7, 9, null);

		assertEquals(7, gdi.getBrushOrgX());
		assertEquals(9, gdi.getBrushOrgY());

		gdi.seveDC();
		Point old = new Point(0, 0);
		gdi.setBrushOrgEx(11, 13, old);

		assertEquals(7, old.x);
		assertEquals(9, old.y);

		gdi.restoreDC(-1);
		assertEquals(7, gdi.getBrushOrgX());
		assertEquals(9, gdi.getBrushOrgY());
	}

	@Test
	public void testTextJustificationBookkeepingFollowsSaveRestore() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setTextJustification(12, 3);

		assertEquals(12, gdi.getTextJustificationExtra());
		assertEquals(3, gdi.getTextJustificationCount());

		gdi.seveDC();
		gdi.setTextJustification(4, 1);

		gdi.restoreDC(-1);
		assertEquals(12, gdi.getTextJustificationExtra());
		assertEquals(3, gdi.getTextJustificationCount());
	}

	@Test
	public void testSaveRestoreRestoresSelectedPaletteAndColorSpace() {
		AwtGdi gdi = createMappedGdi(1, 1);
		GdiPalette redPalette = gdi.createPalette(0x300, new int[]{0x000000FF});
		GdiPalette greenPalette = gdi.createPalette(0x300, new int[]{0x0000FF00});
		GdiColorSpace colorSpace1 = gdi.createColorSpace(new byte[]{1});
		GdiColorSpace colorSpace2 = gdi.createColorSpace(new byte[]{2});

		gdi.selectPalette(redPalette, false);
		gdi.setColorSpace(colorSpace1);
		gdi.seveDC();
		gdi.selectPalette(greenPalette, false);
		gdi.setColorSpace(colorSpace2);
		gdi.restoreDC(-1);

		assertSame(colorSpace1, gdi.setColorSpace(colorSpace2));
		gdi.setDIBitsToDevice(0, 0, 1, 1, 0, 0, 0, 1, createPaletted1BppDib(0), Gdi.DIB_PAL_COLORS);

		assertEquals(0xFF0000, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testSaveRestoreRestoresSelectedDrawingObjects() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		GdiBrush brush1 = gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x0000FF, 0);
		GdiBrush brush2 = gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00FF00, 0);
		GdiPen pen1 = gdi.createPenIndirect(GdiPen.PS_SOLID, 1, 0x0000FF);
		GdiPen pen2 = gdi.createPenIndirect(GdiPen.PS_SOLID, 1, 0x00FF00);
		GdiFont font1 = gdi.createFontIndirect(-12, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'A', 'r', 'i', 'a', 'l', 0});
		GdiFont font2 = gdi.createFontIndirect(-14, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'D', 'i', 'a', 'l', 'o', 'g', 0});

		gdi.selectObject(brush1);
		gdi.selectObject(pen1);
		gdi.selectObject(font1);
		assertSame(brush1, gdi.getSelectedBrush());
		assertSame(pen1, gdi.getSelectedPen());
		assertSame(font1, gdi.getSelectedFont());

		gdi.seveDC();
		gdi.selectObject(brush2);
		gdi.selectObject(pen2);
		gdi.selectObject(font2);

		gdi.restoreDC(-1);
		assertSame(brush1, gdi.getSelectedBrush());
		assertSame(pen1, gdi.getSelectedPen());
		assertSame(font1, gdi.getSelectedFont());
	}

	@Test
	public void testSelectObjectAcceptsPatternBrushAsBrush() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		GdiPatternBrush brush = gdi.dibCreatePatternBrush(createRgbDib(0x00FF00), Gdi.DIB_RGB_COLORS);

		gdi.selectObject(brush);

		assertSame(brush, gdi.getSelectedBrush());
	}

	@Test
	public void testSelectObjectIgnoresForeignDrawingObjects() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		GdiBrush brush = gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x0000FF, 0);
		GdiPen pen = gdi.createPenIndirect(GdiPen.PS_SOLID, 1, 0x0000FF);
		GdiFont font = gdi.createFontIndirect(-12, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'A', 'r', 'i', 'a', 'l', 0});

		gdi.selectObject(brush);
		gdi.selectObject(pen);
		gdi.selectObject(font);
		gdi.selectObject(new GdiBrush() {
			public int getStyle() {
				return GdiBrush.BS_SOLID;
			}

			public int getColor() {
				return 0x00FF00;
			}

			public int getHatch() {
				return 0;
			}
		});
		gdi.selectObject(new GdiPen() {
			public int getStyle() {
				return GdiPen.PS_SOLID;
			}

			public int getWidth() {
				return 1;
			}

			public int getColor() {
				return 0x00FF00;
			}
		});
		gdi.selectObject(new GdiFont() {
			public int getHeight() {
				return -14;
			}

			public int getWidth() {
				return 0;
			}

			public int getEscapement() {
				return 0;
			}

			public int getOrientation() {
				return 0;
			}

			public int getWeight() {
				return 400;
			}

			public boolean isItalic() {
				return false;
			}

			public boolean isUnderlined() {
				return false;
			}

			public boolean isStrikedOut() {
				return false;
			}

			public int getCharset() {
				return 0;
			}

			public int getOutPrecision() {
				return 0;
			}

			public int getClipPrecision() {
				return 0;
			}

			public int getQuality() {
				return 0;
			}

			public int getPitchAndFamily() {
				return 0;
			}

			public String getFaceName() {
				return "Dialog";
			}
		});

		assertSame(brush, gdi.getSelectedBrush());
		assertSame(pen, gdi.getSelectedPen());
		assertSame(font, gdi.getSelectedFont());
	}

	@Test
	public void testCreatePalettePreservesVersion() {
		AwtGdi gdi = new AwtGdi();
		GdiPalette palette = gdi.createPalette(0x200, new int[]{0x000000FF});

		assertEquals(0x200, palette.getVersion());
	}

	@Test
	public void testCreatePaletteKeepsEntriesSnapshot() {
		int[] entries = new int[]{0x000000FF};
		GdiPalette palette = new AwtGdi().createPalette(0x300, entries);
		entries[0] = 0x0000FF00;
		palette.getEntries()[0] = 0x00FF0000;

		AwtGdi gdi = createMappedGdi(1, 1);
		gdi.selectPalette(palette, false);
		gdi.setDIBitsToDevice(0, 0, 1, 1, 0, 0, 0, 1, createPaletted1BppDib(0), Gdi.DIB_PAL_COLORS);

		assertEquals(0xFF0000, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testSelectPaletteIgnoresForeignPalette() {
		AwtGdi gdi = createMappedGdi(1, 1);
		gdi.selectPalette(new GdiPalette() {
			public int getVersion() {
				return 0x300;
			}

			public int[] getEntries() {
				return new int[]{0x000000FF};
			}
		}, false);
		gdi.setDIBitsToDevice(0, 0, 1, 1, 0, 0, 0, 1, createPaletted1BppDib(0), Gdi.DIB_PAL_COLORS);

		assertEquals(0xFFFFFF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testDeleteColorSpaceOnlyAcceptsAwtColorSpace() {
		AwtGdi gdi = new AwtGdi();
		GdiColorSpace colorSpace1 = gdi.createColorSpace(new byte[]{1});
		GdiColorSpace colorSpace2 = gdi.createColorSpace(new byte[]{2});
		GdiColorSpace foreignColorSpace = new GdiColorSpace() {
		};

		gdi.setColorSpace(colorSpace1);
		assertFalse(gdi.deleteColorSpace(foreignColorSpace));
		assertSame(colorSpace1, gdi.setColorSpace(colorSpace2));

		assertTrue(gdi.deleteColorSpace(colorSpace2));
		assertSame(null, gdi.setColorSpace(colorSpace1));
		assertFalse(gdi.deleteColorSpace(null));
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
	public void testAngleArcInsidePathDoesNotDrawUntilStrokePath() {
		AwtGdi gdi = createMappedGdi(100, 100);
		gdi.selectObject(gdi.createPenIndirect(GdiPen.PS_SOLID, 2, 0));
		gdi.beginPath();
		gdi.moveToEx(50, 50, null);
		gdi.angleArc(50, 50, 20, 0, 90);

		assertEquals(0, countPaintedPixels(gdi.getImage()));

		gdi.strokePath();
		assertTrue(countPaintedPixels(gdi.getImage()) > 0);
	}

	@Test
	public void testBitmapRop3UsesTruthTable() {
		AwtGdi gdi = createOnePixelGdi();
		gdi.setPixel(0, 0, 0xFF0000);
		gdi.dibBitBlt(createRgbDib(0xFF0000), 0, 0, 1, 1, 0, 0, Gdi.SRCINVERT);

		assertEquals(0xFF00FF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testSetPixelGrowsCanvas() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setPixel(5, 5, 0x0000FF);
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(image.getWidth() >= 6);
		assertTrue(image.getHeight() >= 6);
		assertEquals(0xFF0000, image.getRGB(5, 5) & 0x00FFFFFF);
	}

	@Test
	public void testExtTextOutOpaqueGrowsCanvas() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setBkColor(0x0000FF);
		gdi.extTextOut(0, 0, Gdi.ETO_OPAQUE, new int[]{5, 5, 6, 6}, new byte[0], null);
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(image.getWidth() >= 6);
		assertTrue(image.getHeight() >= 6);
		assertEquals(0xFF0000, image.getRGB(5, 5) & 0x00FFFFFF);
	}

	@Test
	public void testExtTextOutClippedRestoresNullClip() {
		AwtGdi gdi = createMappedGdi(8, 8);
		gdi.extTextOut(0, 0, Gdi.ETO_CLIPPED, new int[]{0, 0, 1, 1}, new byte[]{'A'}, null);
		gdi.setPixel(5, 5, 0x0000FF);

		BufferedImage image = gdi.getImage();
		assertEquals(0xFF0000, image.getRGB(5, 5) & 0x00FFFFFF);
	}

	@Test
	public void testTextOutGrowsCanvasForForegroundText() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setBkMode(Gdi.TRANSPARENT);
		gdi.setTextColor(0x0000FF);
		gdi.selectObject(gdi.createFontIndirect(-18, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'A', 'r', 'i', 'a', 'l', 0}));
		gdi.textOut(5, 20, new byte[]{'A'});
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(image.getWidth() > 5);
		assertTrue(image.getHeight() > 20);
		assertTrue(countPaintedPixels(image) > 0);
	}

	@Test
	public void testTextOutClipsAtDefaultCanvasRightEdge() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setBkMode(Gdi.OPAQUE);
		gdi.setBkColor(0x0000FF);
		gdi.setTextColor(0x000000);
		gdi.selectObject(gdi.createFontIndirect(-18, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'D', 'i', 'a', 'l', 'o', 'g', 0}));
		gdi.textOut(320, 20, new byte[]{'A', 'B', 'C', 'D', 'E', 'F'});
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(330, image.getWidth());
		assertEquals(460, image.getHeight());
		assertTrue(countPaintedPixels(image, 320, 0, 10, 40) > 0);
	}

	@Test
	public void testTextOutIgnoresOrientationWhenEscapementIsZero() {
		AwtGdi horizontal = createMappedGdi(120, 120);
		horizontal.setBkMode(Gdi.OPAQUE);
		horizontal.setBkColor(0x0000FF);
		horizontal.selectObject(horizontal.createFontIndirect(-24, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'D', 'i', 'a', 'l', 'o', 'g', 0}));
		horizontal.textOut(60, 60, new byte[]{'H', 'I'});

		AwtGdi oriented = createMappedGdi(120, 120);
		oriented.setBkMode(Gdi.OPAQUE);
		oriented.setBkColor(0x0000FF);
		oriented.selectObject(oriented.createFontIndirect(-24, 0, 0, 900, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'D', 'i', 'a', 'l', 'o', 'g', 0}));
		oriented.textOut(60, 60, new byte[]{'H', 'I'});

		assertFalse(imagesDiffer(horizontal.getImage(), oriented.getImage()));
		assertTrue(countOpaqueColorPixels(oriented.getImage(), 0xFF0000) > 0);
	}

	@Test
	public void testTextOutKeepsEscapementAfterCanvasGrowth() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setBkMode(Gdi.OPAQUE);
		gdi.setBkColor(0x0000FF);
		gdi.setTextColor(0xFFFFFF);
		gdi.setTextCharacterExtra(10);
		gdi.selectObject(gdi.createFontIndirect(20, 0, 600, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'D', 'i', 'a', 'l', 'o', 'g', 0}));
		gdi.textOut(170, 120, new byte[]{'I', ' ', 'a', 'm', ' ', 't', 'h', 'e', ' ', 's', 't', 'r', 'i', 'n', 'g'});
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image, 170, 0, 160, 80) > 0);
	}

	@Test
	public void testNegativeDeviceBoundsShiftExistingCanvasContent() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.selectObject(gdi.createPenIndirect(GdiPen.PS_SOLID, 1, 0x0000FF));
		gdi.rectangle(0, -20, 20, 0);
		gdi.rectangle(0, 0, 20, 20);
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(330, image.getWidth());
		assertEquals(460, image.getHeight());
		assertTrue(countPaintedPixels(image, 0, 0, 25, 5) > 0);
		assertTrue(countPaintedPixels(image, 0, 20, 25, 5) > 0);
	}

	@Test
	public void testWindowExtPreventsTextFromGrowingCanvas() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(100, 40, null);
		gdi.setBkMode(Gdi.TRANSPARENT);
		gdi.setTextAlign(Gdi.TA_UPDATECP | Gdi.TA_LEFT | Gdi.TA_BASELINE);
		gdi.selectObject(gdi.createFontIndirect(-20, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'D', 'i', 'a', 'l', 'o', 'g', 0}));
		gdi.moveToEx(10, 25, null);
		gdi.extTextOut(0, 20, 0, null, new byte[]{'A'}, new int[]{200});
		gdi.extTextOut(0, 20, 0, null, new byte[]{'B'}, new int[]{20});
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(100, image.getWidth());
		assertEquals(40, image.getHeight());
		assertTrue(countPaintedPixels(image) > 0);
	}

	@Test
	public void testThickStrokeGrowsCanvasForStrokeBounds() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.selectObject(gdi.createPenIndirect(GdiPen.PS_SOLID, 10, 0x0000FF));
		gdi.moveToEx(5, 5, null);
		gdi.lineTo(20, 5);
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(image.getWidth() >= 21);
		assertTrue(image.getHeight() >= 10);
		assertTrue(countPaintedPixels(image) > 20);
	}

	@Test
	public void testInvertRgnGrowsCanvas() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.invertRgn(gdi.createRectRgn(5, 5, 6, 6));
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(image.getWidth() >= 6);
		assertTrue(image.getHeight() >= 6);
		assertEquals(0xFFFFFF, image.getRGB(5, 5) & 0x00FFFFFF);

		AwtGdi painted = createMappedGdi(1, 1);
		painted.setPixel(0, 0, 0x0000FF);
		painted.invertRgn(painted.createRectRgn(0, 0, 1, 1));

		assertEquals(0x00FFFF, painted.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testBitmapRop3RespectsClipRegion() {
		AwtGdi gdi = createMappedGdi(2, 1);
		gdi.setPixel(0, 0, 0xFF0000);
		gdi.setPixel(1, 0, 0xFF0000);
		gdi.extSelectClipRgn(gdi.createRectRgn(0, 0, 1, 1), GdiRegion.RGN_COPY);
		gdi.dibBitBlt(createRgbDib(0xFF0000, 0xFF0000), 0, 0, 2, 1, 0, 0, Gdi.SRCINVERT);

		assertEquals(0xFF00FF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0x0000FF, gdi.getImage().getRGB(1, 0) & 0x00FFFFFF);
	}

	@Test
	public void testBitmapRop3UsesPatternOperand() {
		AwtGdi gdi = createOnePixelGdi();
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00FF00, 0));
		gdi.dibBitBlt(createRgbDib(0xFFFFFF), 0, 0, 1, 1, 0, 0, Gdi.MERGECOPY);

		assertEquals(0x00FF00, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testMonochromeWmfPatternBrushReusesPreviousDibPatternBrush() {
		AwtGdi gdi = createOnePixelGdi();
		gdi.dibCreatePatternBrush(createRgbDib(0x00FF00), Gdi.DIB_RGB_COLORS);
		gdi.selectObject(gdi.createPatternBrush(createWmfMonoBitmap(0x80)));
		gdi.rectangle(0, 0, 1, 1);

		assertEquals(0x00FF00, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testPatternBrushKeepsCreatedBitmapSnapshot() {
		byte[] dib = createRgbDib(0x00FF00);
		GdiPatternBrush brush = new AwtGdi().dibCreatePatternBrush(dib, Gdi.DIB_RGB_COLORS);
		setRgbDibPixel(dib, 0xFF0000);
		setRgbDibPixel(brush.getPattern(), 0x0000FF);

		AwtGdi gdi = createOnePixelGdi();
		gdi.selectObject(brush);
		gdi.rectangle(0, 0, 1, 1);

		assertEquals(0x00FF00, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testMonochromeWmfPatternBrushFallbackKeepsDibSnapshot() {
		byte[] dib = createRgbDib(0x00FF00);
		AwtGdi gdi = createOnePixelGdi();
		gdi.dibCreatePatternBrush(dib, Gdi.DIB_RGB_COLORS);
		setRgbDibPixel(dib, 0xFF0000);
		gdi.selectObject(gdi.createPatternBrush(createWmfMonoBitmap(0x80)));
		gdi.rectangle(0, 0, 1, 1);

		assertEquals(0x00FF00, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testFillShapeRop2RespectsClipRegion() {
		AwtGdi gdi = createMappedGdi(2, 1);
		gdi.setPixel(0, 0, 0x0000FF);
		gdi.setPixel(1, 0, 0x0000FF);
		gdi.selectObject(gdi.createPenIndirect(GdiPen.PS_NULL, 1, 0));
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x0000FF, 0));
		gdi.setROP2(Gdi.R2_XORPEN);
		gdi.extSelectClipRgn(gdi.createRectRgn(0, 0, 1, 1), GdiRegion.RGN_COPY);
		gdi.rectangle(0, 0, 2, 1);

		assertEquals(0x000000, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0xFF0000, gdi.getImage().getRGB(1, 0) & 0x00FFFFFF);
	}

	@Test
	public void testStretchBltModeControlsBitmapInterpolation() {
		byte[] dib = createRgbDib(0xFF0000, 0x0000FF);
		AwtGdi nearest = createMappedGdi(4, 1);
		nearest.setStretchBltMode(Gdi.COLORONCOLOR);
		nearest.stretchDIBits(0, 0, 4, 1, 0, 0, 2, 1, dib, Gdi.DIB_RGB_COLORS, Gdi.SRCCOPY);

		AwtGdi halftone = createMappedGdi(4, 1);
		halftone.setStretchBltMode(Gdi.HALFTONE);
		halftone.stretchDIBits(0, 0, 4, 1, 0, 0, 2, 1, dib, Gdi.DIB_RGB_COLORS, Gdi.SRCCOPY);

		int nearestPixel = nearest.getImage().getRGB(1, 0) & 0x00FFFFFF;
		int halftonePixel = halftone.getImage().getRGB(1, 0) & 0x00FFFFFF;
		assertTrue(halftonePixel != nearestPixel);
		assertTrue((halftonePixel & 0xFF0000) != 0);
		assertTrue((halftonePixel & 0x0000FF) != 0);
	}

	@Test
	public void testSetDIBitsToDeviceUsesStretchBltInterpolation() {
		byte[] dib = createRgbDib(0xFF0000, 0x0000FF);
		AwtGdi nearest = createMappedGdi(2, 1, 4, 1);
		nearest.setStretchBltMode(Gdi.COLORONCOLOR);
		nearest.setDIBitsToDevice(0, 0, 2, 1, 0, 0, 0, 1, dib, Gdi.DIB_RGB_COLORS);

		AwtGdi halftone = createMappedGdi(2, 1, 4, 1);
		halftone.setStretchBltMode(Gdi.HALFTONE);
		halftone.setDIBitsToDevice(0, 0, 2, 1, 0, 0, 0, 1, dib, Gdi.DIB_RGB_COLORS);

		int nearestPixel = nearest.getImage().getRGB(1, 0) & 0x00FFFFFF;
		int halftonePixel = halftone.getImage().getRGB(1, 0) & 0x00FFFFFF;
		assertTrue(halftonePixel != nearestPixel);
		assertTrue((halftonePixel & 0xFF0000) != 0);
		assertTrue((halftonePixel & 0x0000FF) != 0);
	}

	@Test
	public void testBitmapRop4StyleValueUsesBackgroundRop() {
		AwtGdi gdi = createOnePixelGdi();
		gdi.dibBitBlt(createRgbDib(0xFF00FF), 0, 0, 1, 1, 0, 0, 0xCCAA0029L);

		assertEquals(0xFF00FF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testDibBitBltWithoutSourceIgnoresSourceDependentRop() {
		AwtGdi gdi = createMappedGdi(1, 1);
		gdi.setPixel(0, 0, 0x00FF00);
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x0000FF, 0));
		gdi.dibBitBlt(null, 0, 0, 1, 1, 0, 0, Gdi.SRCCOPY);

		assertEquals(0x00FF00, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testDibBitBltWithoutSourceKeepsSourceIndependentRop() {
		AwtGdi gdi = createMappedGdi(1, 1);
		gdi.setPixel(0, 0, 0x00FF00);
		gdi.dibBitBlt(null, 0, 0, 1, 1, 0, 0, Gdi.DSTINVERT);

		assertEquals(0xFF00FF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testHairlineStrokeDoesNotGrowCanvasPastShapeBounds() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.rectangle(0, 0, 700, 600);
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(700, image.getWidth());
		assertEquals(600, image.getHeight());
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
	public void testMaskBltRespectsClipRegion() {
		AwtGdi gdi = createMappedGdi(2, 1);
		gdi.setPixel(0, 0, 0xFF0000);
		gdi.setPixel(1, 0, 0xFF0000);
		gdi.extSelectClipRgn(gdi.createRectRgn(0, 0, 1, 1), GdiRegion.RGN_COPY);
		gdi.maskBlt(createRgbDib(0xFF0000, 0xFF0000), 0, 0, 2, 1, 0, 0, createRgbDib(0xFFFFFF, 0xFFFFFF), 0, 0,
				0xCC000000L | Gdi.NOTSRCCOPY);

		assertEquals(0x00FFFF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0x0000FF, gdi.getImage().getRGB(1, 0) & 0x00FFFFFF);
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
	public void testMaskBltUsesDefaultDibMaskColorsWhenColorTableIsOmitted() {
		AwtGdi gdi = createMappedGdi(2, 1);
		gdi.setPixel(0, 0, 0x00FF00);
		gdi.setPixel(1, 0, 0x00FF00);
		gdi.maskBlt(createRgbDib(0xFF00FF, 0x0000FF), 0, 0, 2, 1, 0, 0, createOneBppDibWithoutColorTable(0x80, 2, 1), 0,
				0, 0xCCAA0029L);

		assertEquals(0x00FF00, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0x0000FF, gdi.getImage().getRGB(1, 0) & 0x00FFFFFF);
	}

	@Test
	public void testWmfMonoBitmapUsesWmfBlackWhiteBitOrder() {
		AwtGdi gdi = createMappedGdi(2, 1);
		gdi.bitBlt(createWmfMonoBitmap(0x80), 0, 0, 2, 1, 0, 0, Gdi.SRCCOPY);

		assertEquals(0x000000, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0xFFFFFF, gdi.getImage().getRGB(1, 0) & 0x00FFFFFF);
	}

	@Test
	public void testPatBltUsesPatternRasterOpTruthTable() {
		AwtGdi gdi = createOnePixelGdi();
		gdi.setPixel(0, 0, 0x0000FF);
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00FF00, 0));
		gdi.patBlt(0, 0, 1, 1, Gdi.PATPAINT);

		assertEquals(0xFFFFFF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testFloodFillFillsConnectedSurfaceOnly() {
		AwtGdi gdi = createMappedGdi(5, 5);
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x0000FF, 0));
		gdi.selectObject(gdi.createPenIndirect(GdiPen.PS_NULL, 1, 0));
		gdi.rectangle(0, 0, 5, 5);
		for (int i = 0; i < 5; i++) {
			gdi.setPixel(i, 0, 0);
			gdi.setPixel(i, 4, 0);
			gdi.setPixel(0, i, 0);
			gdi.setPixel(4, i, 0);
		}
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00FF00, 0));
		gdi.floodFill(2, 2, 0);

		assertEquals(0x00FF00, gdi.getImage().getRGB(2, 2) & 0x00FFFFFF);
		assertEquals(0x000000, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0x000000, gdi.getImage().getRGB(4, 2) & 0x00FFFFFF);
	}

	@Test
	public void testFloodFillDoesNotTreatTransparentPixelsAsBlackBorder() {
		AwtGdi gdi = createMappedGdi(3, 3);
		for (int i = 0; i < 3; i++) {
			gdi.setPixel(i, 0, 0);
			gdi.setPixel(i, 2, 0);
			gdi.setPixel(0, i, 0);
			gdi.setPixel(2, i, 0);
		}
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00FF00, 0));
		gdi.floodFill(1, 1, 0);

		assertEquals(0x00FF00, gdi.getImage().getRGB(1, 1) & 0x00FFFFFF);
	}

	@Test
	public void testExtFloodFillSurfaceFillsMatchingColorOnly() {
		AwtGdi gdi = createMappedGdi(3, 1);
		gdi.setPixel(0, 0, 0x0000FF);
		gdi.setPixel(1, 0, 0x0000FF);
		gdi.setPixel(2, 0, 0x00FF00);
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0xFF0000, 0));
		gdi.extFloodFill(0, 0, 0x000000FF, 1);

		assertEquals(0x0000FF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0x0000FF, gdi.getImage().getRGB(1, 0) & 0x00FFFFFF);
		assertEquals(0x00FF00, gdi.getImage().getRGB(2, 0) & 0x00FFFFFF);
	}

	@Test
	public void testPatBltWithNullBrushDoesNotPaintPatternDependentRop() {
		AwtGdi gdi = createOnePixelGdi();
		gdi.setPixel(0, 0, 0xFF0000);
		gdi.selectObject(gdi.createBrushIndirect(GdiBrush.BS_NULL, 0x00FF00, 0));
		gdi.patBlt(0, 0, 1, 1, Gdi.PATCOPY);

		assertEquals(0x0000FF, gdi.getImage().getRGB(0, 0) & 0x00FFFFFF);
	}

	@Test
	public void testTextOutOpaqueBackgroundFillsTextBounds() {
		AwtGdi gdi = createMappedGdi(80, 30);
		gdi.selectObject(gdi.createFontIndirect(-12, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'A', 'r', 'i', 'a', 'l', 0}));
		gdi.setBkMode(Gdi.OPAQUE);
		gdi.setBkColor(0x0000FF00);
		gdi.textOut(10, 5, new byte[]{'A', 'B'});

		assertEquals(0x00FF00, gdi.getImage().getRGB(11, 7) & 0x00FFFFFF);
	}

	@Test
	public void testTextOutTransparentBackgroundDoesNotFillTextBounds() {
		AwtGdi gdi = createMappedGdi(80, 30);
		gdi.selectObject(gdi.createFontIndirect(-12, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'A', 'r', 'i', 'a', 'l', 0}));
		gdi.setBkMode(Gdi.TRANSPARENT);
		gdi.setBkColor(0x0000FF00);
		gdi.textOut(10, 5, new byte[]{'A', 'B'});

		assertEquals(0, (gdi.getImage().getRGB(11, 7) >>> 24) & 0xFF);
	}

	@Test
	public void testTextOutUsesRop2Mode() {
		AwtGdi gdi = createMappedGdi(80, 30);
		gdi.selectObject(gdi.createFontIndirect(-18, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'A', 'r', 'i', 'a', 'l', 0}));
		gdi.setBkMode(Gdi.TRANSPARENT);
		gdi.setTextColor(0x0000FF);
		gdi.setROP2(Gdi.R2_BLACK);
		gdi.textOut(5, 20, new byte[]{'A'});

		BufferedImage image = gdi.getImage();
		assertTrue(countOpaqueColorPixels(image, 0x000000) > 0);
		assertEquals(0, countOpaqueColorPixels(image, 0xFF0000));
	}

	@Test
	public void testExtTextOutPdyUsesVerticalAdvances() {
		AwtGdi gdi = createMappedGdi(40, 60);
		gdi.selectObject(gdi.createFontIndirect(-14, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'A', 'r', 'i', 'a', 'l', 0}));
		gdi.setBkMode(Gdi.TRANSPARENT);
		gdi.extTextOut(5, 16, Gdi.ETO_PDY, null, new byte[]{'H', 'H'}, new int[]{0, 20, 0, 0});

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image, 0, 4, 20, 16) > 0);
		assertTrue(countPaintedPixels(image, 0, 24, 20, 16) > 0);
	}

	@Test
	public void testExtTextOutPdyUpdateCpAdvancesY() {
		AwtGdi gdi = createMappedGdi(40, 60);
		gdi.selectObject(gdi.createFontIndirect(-14, 0, 0, 0, 400, false, false, false, 0, 0, 0, 0, 0,
				new byte[]{'A', 'r', 'i', 'a', 'l', 0}));
		gdi.setBkMode(Gdi.TRANSPARENT);
		gdi.setTextAlign(Gdi.TA_UPDATECP | Gdi.TA_LEFT | Gdi.TA_TOP);
		gdi.moveToEx(5, 16, null);
		gdi.extTextOut(99, 99, Gdi.ETO_PDY, null, new byte[]{'H'}, new int[]{0, 20});
		gdi.textOut(99, 99, new byte[]{'H'});

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image, 0, 4, 20, 16) > 0);
		assertTrue(countPaintedPixels(image, 0, 24, 20, 16) > 0);
	}

	@Test
	public void testExtTextOutRtlReadingUsesMiddleEasternRunDirection() {
		AwtGdi leftToRight = createHebrewTextGdi();
		leftToRight.extTextOut(5, 20, 0, null, new byte[]{(byte) 0xE0, (byte) 0xE1, (byte) 0xE2}, null);

		AwtGdi rightToLeft = createHebrewTextGdi();
		rightToLeft.extTextOut(5, 20, Gdi.ETO_RTLREADING, null, new byte[]{(byte) 0xE0, (byte) 0xE1, (byte) 0xE2},
				null);

		assertTrue(imagesDiffer(leftToRight.getImage(), rightToLeft.getImage()));
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
	public void testEmfPlusFillPolygonUsesWindingFillFlag() {
		AwtGdi alternate = new AwtGdi();
		alternate.header();
		alternate.comment(createEmfPlusStarFillPolygonComment(false));
		alternate.footer();

		AwtGdi winding = new AwtGdi();
		winding.header();
		winding.comment(createEmfPlusStarFillPolygonComment(true));
		winding.footer();

		assertEquals(0, (alternate.getImage().getRGB(20, 20) >>> 24) & 0xFF);
		assertEquals(0x336699, winding.getImage().getRGB(20, 20) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusFontUnderlineAndStrikeoutDecorateDrawString() {
		AwtGdi plain = new AwtGdi();
		plain.header();
		plain.comment(createEmfPlusDecoratedFontDrawStringComment(0));
		plain.footer();

		AwtGdi decorated = new AwtGdi();
		decorated.header();
		decorated.comment(createEmfPlusDecoratedFontDrawStringComment(0x0000000C));
		decorated.footer();

		int plainPixels = countPaintedPixels(plain.getImage());
		int decoratedPixels = countPaintedPixels(decorated.getImage());
		assertTrue("plain=" + plainPixels + ", decorated=" + decoratedPixels, decoratedPixels > plainPixels);
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
	public void testEmfPlusDrawImageUsesBitmapClampObjectClamp() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusBitmapClampedSourceDrawImageComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x00FF00, image.getRGB(12, 15) & 0x00FFFFFF);
		assertEquals(0x00FF00, image.getRGB(25, 15) & 0x00FFFFFF);
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
	public void testEmfPlusDrawImagePointsAppliesColorMatrixEffect() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusColorMatrixDrawImagePointsComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x000000, image.getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0x00FF00, image.getRGB(1, 0) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusDrawImageAppliesColorMatrixEffect() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusColorMatrixDrawImageComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x000000, image.getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0x00FF00, image.getRGB(1, 0) & 0x00FFFFFF);
	}

	@Test
	public void testEmfPlusDrawImagePointsAppliesColorLookupTableEffect() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusColorLookupTableDrawImagePointsComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertEquals(0x110033, image.getRGB(0, 0) & 0x00FFFFFF);
		assertEquals(0x002233, image.getRGB(1, 0) & 0x00FFFFFF);
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
	public void testEmfPlusMetafileDrawImageExpandsCanvas() throws Exception {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusMetafileDrawImageComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(image.getWidth() >= 140);
		assertTrue(image.getHeight() >= 140);
		assertTrue(countPaintedPixels(image, 108, 108, 36, 36) > 0);
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
	public void testEmfPlusDrawLinesUsesSeparateStartAndEndCaps() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusSquareStartFlatEndPenComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue((image.getRGB(8, 20) >>> 24) != 0);
		assertEquals(0, (image.getRGB(33, 20) >>> 24) & 0xFF);
	}

	@Test
	public void testEmfPlusDrawLinesUsesCustomArrowStartCap() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusCustomArrowStartCapPenComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue((image.getRGB(12, 20) >>> 24) != 0);
		assertEquals(0, (image.getRGB(8, 12) >>> 24) & 0xFF);
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
	public void testEmfPlusPixelOffsetHalfShiftsDrawing() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusPixelOffsetFillRectComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(image.getWidth() >= 2);
		assertTrue(image.getHeight() >= 2);
		assertTrue(countPaintedPixels(image) > 0);
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
	public void testEmfPlusClearTypeTextRenderingHintUsesLcdAntialiasing() {
		BufferedImage canvas = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = canvas.createGraphics();
		try {
			AwtGdi gdi = new AwtGdi(graphics, canvas.getWidth(), canvas.getHeight());
			gdi.header();
			gdi.comment(createEmfPlusTextRenderingHintComment(5));

			assertEquals(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB,
					graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
			gdi.footer();
		} finally {
			graphics.dispose();
		}
	}

	@Test
	public void testEmfPlusGridFitTextRenderingHintUsesGaspAntialiasing() {
		BufferedImage canvas = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = canvas.createGraphics();
		try {
			AwtGdi gdi = new AwtGdi(graphics, canvas.getWidth(), canvas.getHeight());
			gdi.header();
			gdi.comment(createEmfPlusTextRenderingHintComment(3));

			assertEquals(RenderingHints.VALUE_TEXT_ANTIALIAS_GASP,
					graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
			gdi.footer();
		} finally {
			graphics.dispose();
		}
	}

	@Test
	public void testEmfPlusAssumeLinearCompositingQualityUsesQualityInterpolation() {
		BufferedImage canvas = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = canvas.createGraphics();
		try {
			AwtGdi gdi = new AwtGdi(graphics, canvas.getWidth(), canvas.getHeight());
			gdi.header();
			gdi.comment(createEmfPlusCompositingQualityComment(5));

			assertEquals(RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY,
					graphics.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION));
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
	public void testEmfPlusDrawDriverStringRendersGlyphIndices() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusGlyphIndexDriverStringComment());
		gdi.footer();

		assertTrue(countPaintedPixels(gdi.getImage(), 4, 6, 50, 20) > 0);
	}

	@Test
	public void testEmfPlusDrawDriverStringAppliesRecordMatrixToGlyphs() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusTransformedDrawDriverStringComment());
		gdi.footer();

		BufferedImage image = gdi.getImage();
		assertTrue(countPaintedPixels(image, 5, 28, 90, 24) > 0);
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

	@Test
	public void testEmfPlusNoClipDrawStringExpandsCanvasPastLayoutRect() {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.comment(createEmfPlusNoClipExpandingDrawStringComment());
		gdi.footer();

		assertTrue(gdi.getImage().getWidth() > 330);
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

	private byte[] createEmfPlusStarFillPolygonComment(boolean winding) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 5);
		writeFloat(payload, 20);
		writeFloat(payload, 2);
		writeFloat(payload, 30);
		writeFloat(payload, 35);
		writeFloat(payload, 2);
		writeFloat(payload, 14);
		writeFloat(payload, 38);
		writeFloat(payload, 14);
		writeFloat(payload, 10);
		writeFloat(payload, 35);
		writeEmfPlusRecord(comment, 0x400C, winding ? 0xA000 : 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDecoratedFontDrawStringComment(int styleFlags) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 24);
		writeInt(payload, 2);
		writeInt(payload, styleFlags);
		writeInt(payload, 0);
		writeInt(payload, "Arial".length());
		writeUtf16Le(payload, "Arial");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 0);
		writeInt(payload, "Decorate".length());
		writeFloat(payload, 5);
		writeFloat(payload, 5);
		writeFloat(payload, 180);
		writeFloat(payload, 40);
		writeUtf16Le(payload, "Decorate");
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
		return createEmfPlusOversizedSourceDrawImageComment(clamp ? 4 : -1, 0xFF0000FF, 0);
	}

	private byte[] createEmfPlusColorMatrixDrawImagePointsComment() throws IOException {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		byte[] png = createTwoPixelPng();
		writeInt(payload, 0);
		writeInt(payload, 1);
		payload.write(png, 0, png.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500, payload.toByteArray());

		payload.reset();
		writeColorMatrixEffectGuid(payload);
		writeInt(payload, 100);
		for (int column = 0; column < 5; column++) {
			for (int row = 0; row < 5; row++) {
				writeFloat(payload, row == column && row != 0 ? 1 : 0);
			}
		}
		writeEmfPlusRecord(comment, 0x4038, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x401B, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusColorMatrixDrawImageComment() throws IOException {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		byte[] png = createTwoPixelPng();
		writeInt(payload, 0);
		writeInt(payload, 1);
		payload.write(png, 0, png.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500, payload.toByteArray());

		payload.reset();
		writeColorMatrixEffectGuid(payload);
		writeInt(payload, 100);
		for (int column = 0; column < 5; column++) {
			for (int row = 0; row < 5; row++) {
				writeFloat(payload, row == column && row != 0 ? 1 : 0);
			}
		}
		writeEmfPlusRecord(comment, 0x4038, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeEmfPlusRecord(comment, 0x401A, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private void writeColorMatrixEffectGuid(ByteArrayOutputStream out) {
		int[] guid = {0x15, 0x26, 0x8F, 0x71, 0x33, 0x79, 0xE3, 0x40, 0xA5, 0x11, 0x5F, 0x68, 0xFE, 0x14, 0xDD, 0x74};
		for (int value : guid) {
			out.write(value);
		}
	}

	private byte[] createEmfPlusColorLookupTableDrawImagePointsComment() throws IOException {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		byte[] png = createTwoPixelPng();
		writeInt(payload, 0);
		writeInt(payload, 1);
		payload.write(png, 0, png.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500, payload.toByteArray());

		payload.reset();
		writeColorLookupTableEffectGuid(payload);
		writeInt(payload, 1024);
		writeColorLookupTable(payload);
		writeEmfPlusRecord(comment, 0x4038, 0, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 2);
		writeFloat(payload, 1);
		writeInt(payload, 3);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 20);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 10);
		writeEmfPlusRecord(comment, 0x401B, 0x0000, payload.toByteArray());
		return comment.toByteArray();
	}

	private void writeColorLookupTableEffectGuid(ByteArrayOutputStream out) {
		int[] guid = {0xA9, 0x72, 0xCE, 0xA7, 0x7F, 0x0F, 0xD7, 0x40, 0xB3, 0xCC, 0xD0, 0xC0, 0x2D, 0x5C, 0x32, 0x12};
		for (int value : guid) {
			out.write(value);
		}
	}

	private void writeColorLookupTable(ByteArrayOutputStream out) {
		byte[] blue = createIdentityColorLookupTable();
		byte[] green = createIdentityColorLookupTable();
		byte[] red = createIdentityColorLookupTable();
		byte[] alpha = createIdentityColorLookupTable();
		blue[0] = 0x33;
		green[255] = 0x22;
		red[255] = 0x11;
		out.write(blue, 0, blue.length);
		out.write(green, 0, green.length);
		out.write(red, 0, red.length);
		out.write(alpha, 0, alpha.length);
	}

	private byte[] createIdentityColorLookupTable() {
		byte[] table = new byte[256];
		for (int i = 0; i < table.length; i++) {
			table[i] = (byte) i;
		}
		return table;
	}

	private byte[] createEmfPlusTiledSourceDrawImageComment() throws IOException {
		return createEmfPlusOversizedSourceDrawImageComment(0, 0, 0);
	}

	private byte[] createEmfPlusBitmapClampedSourceDrawImageComment() throws IOException {
		return createEmfPlusOversizedSourceDrawImageComment(4, 0xFF0000FF, 1);
	}

	private byte[] createEmfPlusOversizedSourceDrawImageComment(int wrapMode, int clampColor, int objectClamp)
			throws IOException {
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
			writeInt(payload, objectClamp);
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

	private byte[] createEmfPlusMetafileDrawImageComment() throws IOException {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		byte[] emf = createLineEmf();
		writeInt(payload, 0);
		writeInt(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, emf.length);
		payload.write(emf, 0, emf.length);
		writeEmfPlusRecord(comment, 0x4008, 0x0500, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 60);
		writeFloat(payload, 60);
		writeFloat(payload, 110);
		writeFloat(payload, 110);
		writeFloat(payload, 30);
		writeFloat(payload, 30);
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

	private byte[] createEmfPlusTextRenderingHintComment(int hint) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		writeEmfPlusRecord(comment, 0x401F, hint, new byte[0]);
		return comment.toByteArray();
	}

	private byte[] createEmfPlusCompositingQualityComment(int quality) {
		ByteArrayOutputStream comment = createEmfPlusComment();
		writeEmfPlusRecord(comment, 0x4024, quality, new byte[0]);
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

	private byte[] createEmfPlusSquareStartFlatEndPenComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x00000006);
		writeInt(payload, 2);
		writeFloat(payload, 6);
		writeInt(payload, 1);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 2);
		writeFloat(payload, 10);
		writeFloat(payload, 20);
		writeFloat(payload, 30);
		writeFloat(payload, 20);
		writeEmfPlusRecord(comment, 0x400D, 0, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusCustomArrowStartCapPenComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0x00000800);
		writeInt(payload, 2);
		writeFloat(payload, 2);
		writeInt(payload, 60);
		writeInt(payload, 0);
		writeInt(payload, 1);
		writeFloat(payload, 4);
		writeFloat(payload, 6);
		writeFloat(payload, 0);
		writeInt(payload, 1);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 10);
		writeFloat(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0xFF0000FF);
		writeEmfPlusRecord(comment, 0x4008, 0x0200, payload.toByteArray());

		payload.reset();
		writeInt(payload, 2);
		writeFloat(payload, 20);
		writeFloat(payload, 20);
		writeFloat(payload, 40);
		writeFloat(payload, 20);
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

	private byte[] createEmfPlusPixelOffsetFillRectComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeEmfPlusRecord(comment, 0x4022, 4, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 1);
		writeFloat(payload, 1);
		writeEmfPlusRecord(comment, 0x400A, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private byte[] createEmfPlusDrawDriverStringComment() {
		return createEmfPlusDrawDriverStringComment(1, new double[][]{{5, 6}, {18, 6}, {31, 6}});
	}

	private byte[] createEmfPlusGlyphIndexDriverStringComment() {
		ByteArrayOutputStream comment = createEmfPlusComment();
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		writeInt(payload, 0);
		writeFloat(payload, 12);
		writeInt(payload, 2);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, "Dialog".length());
		writeUtf16Le(payload, "Dialog");
		writeEmfPlusRecord(comment, 0x4008, 0x0600, payload.toByteArray());

		int[] glyphCodes = createGlyphCodes("Dialog", "ABC");
		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, glyphCodes.length);
		for (int i = 0; i < glyphCodes.length; i++) {
			writeShort(payload, glyphCodes[i]);
		}
		writeShort(payload, 0);
		writeFloat(payload, 5);
		writeFloat(payload, 6);
		writeFloat(payload, 18);
		writeFloat(payload, 6);
		writeFloat(payload, 31);
		writeFloat(payload, 6);
		writeEmfPlusRecord(comment, 0x4036, 0x8000, payload.toByteArray());
		return comment.toByteArray();
	}

	private int[] createGlyphCodes(String familyName, String text) {
		BufferedImage canvas = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = canvas.createGraphics();
		try {
			GlyphVector glyphs = new Font(familyName, Font.PLAIN, 12).createGlyphVector(graphics.getFontRenderContext(),
					text);
			int[] codes = new int[glyphs.getNumGlyphs()];
			for (int i = 0; i < codes.length; i++) {
				codes[i] = glyphs.getGlyphCode(i);
			}
			return codes;
		} finally {
			graphics.dispose();
		}
	}

	private byte[] createEmfPlusVerticalDrawDriverStringComment() {
		return createEmfPlusDrawDriverStringComment(7, new double[][]{{5, 6}});
	}

	private byte[] createEmfPlusTransformedDrawDriverStringComment() {
		return createEmfPlusDrawDriverStringComment(1, new double[][]{{5, 6}, {18, 6}, {31, 6}},
				new double[]{2, 0, 0, 2, 0, 0});
	}

	private byte[] createEmfPlusDrawDriverStringComment(int options, double[][] positions) {
		return createEmfPlusDrawDriverStringComment(options, positions, null);
	}

	private byte[] createEmfPlusDrawDriverStringComment(int options, double[][] positions, double[] matrix) {
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
		writeInt(payload, matrix != null ? 1 : 0);
		writeInt(payload, 3);
		writeUtf16Le(payload, "ABC");
		writeShort(payload, 0);
		for (int i = 0; i < positions.length; i++) {
			writeFloat(payload, (float) positions[i][0]);
			writeFloat(payload, (float) positions[i][1]);
		}
		if (matrix != null) {
			for (int i = 0; i < matrix.length; i++) {
				writeFloat(payload, (float) matrix[i]);
			}
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

	private byte[] createEmfPlusNoClipExpandingDrawStringComment() {
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
		writeInt(payload, 0);
		writeInt(payload, 0x00004000);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeFloat(payload, 0);
		writeInt(payload, 0);
		writeInt(payload, 0);
		writeEmfPlusRecord(comment, 0x4008, 0x0701, payload.toByteArray());

		payload.reset();
		writeInt(payload, 0xFF336699);
		writeInt(payload, 1);
		writeInt(payload, "Overflow text".length());
		writeFloat(payload, 320);
		writeFloat(payload, 0);
		writeFloat(payload, 4);
		writeFloat(payload, 20);
		writeUtf16Le(payload, "Overflow text");
		writeEmfPlusRecord(comment, 0x401C, 0x8000, payload.toByteArray());
		return comment.toByteArray();
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
		return createMappedGdi(width, height, width, height);
	}

	private AwtGdi createMappedGdi(int windowWidth, int windowHeight, int viewportWidth, int viewportHeight) {
		AwtGdi gdi = new AwtGdi();
		gdi.header();
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(windowWidth, windowHeight, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(viewportWidth, viewportHeight, null);
		return gdi;
	}

	private AwtGdi createHebrewTextGdi() {
		AwtGdi gdi = createMappedGdi(80, 30);
		gdi.selectObject(gdi.createFontIndirect(-20, 0, 0, 0, 400, false, false, false, GdiFont.HEBREW_CHARSET, 0, 0, 0,
				0, new byte[]{'D', 'i', 'a', 'l', 'o', 'g', 0}));
		gdi.setBkMode(Gdi.TRANSPARENT);
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

	private void setRgbDibPixel(byte[] dib, int rgb) {
		dib[40] = (byte) (rgb & 0xFF);
		dib[41] = (byte) ((rgb >>> 8) & 0xFF);
		dib[42] = (byte) ((rgb >>> 16) & 0xFF);
	}

	private byte[] createPaletted1BppDib(int paletteIndex) {
		byte[] dib = new byte[48];
		writeInt32(dib, 0, 40);
		writeInt32(dib, 4, 1);
		writeInt32(dib, 8, 1);
		writeUInt16(dib, 12, 1);
		writeUInt16(dib, 14, 1);
		writeInt32(dib, 20, 4);
		writeUInt16(dib, 40, paletteIndex);
		writeUInt16(dib, 42, paletteIndex);
		return dib;
	}

	private byte[] createOneBppDibWithoutColorTable(int bits, int width, int height) {
		int stride = ((width + 31) / 32) * 4;
		byte[] dib = new byte[40 + stride * height];
		writeInt32(dib, 0, 40);
		writeInt32(dib, 4, width);
		writeInt32(dib, 8, height);
		writeUInt16(dib, 12, 1);
		writeUInt16(dib, 14, 1);
		writeInt32(dib, 20, stride * height);
		dib[40] = (byte) bits;
		return dib;
	}

	private byte[] createWmfMonoBitmap(int bits) {
		byte[] bitmap = new byte[12];
		writeUInt16(bitmap, 0, 0);
		writeUInt16(bitmap, 2, 2);
		writeUInt16(bitmap, 4, 1);
		writeUInt16(bitmap, 6, 2);
		bitmap[8] = 1;
		bitmap[9] = 1;
		bitmap[10] = (byte) bits;
		return bitmap;
	}

	private byte[] createRegionData(int... rects) {
		byte[] data = new byte[32 + rects.length * 4];
		writeInt32(data, 8, rects.length / 4);
		for (int i = 0; i < rects.length; i++) {
			writeInt32(data, 32 + i * 4, rects[i]);
		}
		return data;
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

	private int countOpaqueColorPixels(BufferedImage image, int rgb) {
		int count = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				if (((argb >>> 24) & 0xFF) != 0 && (argb & 0x00FFFFFF) == rgb) {
					count++;
				}
			}
		}
		return count;
	}

	private boolean imagesDiffer(BufferedImage a, BufferedImage b) {
		if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
			return true;
		}
		for (int y = 0; y < a.getHeight(); y++) {
			for (int x = 0; x < a.getWidth(); x++) {
				if (a.getRGB(x, y) != b.getRGB(x, y)) {
					return true;
				}
			}
		}
		return false;
	}

	private int alphaAtBottomRight(BufferedImage image) {
		return (image.getRGB(image.getWidth() - 1, image.getHeight() - 1) >>> 24) & 0xFF;
	}
}

package net.arnx.wmf2svg.gdi.emf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiPalette;
import net.arnx.wmf2svg.gdi.GdiPatternBrush;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.GdiRegion;
import net.arnx.wmf2svg.gdi.GradientRect;
import net.arnx.wmf2svg.gdi.GradientTriangle;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Trivertex;

public class FulltestEmfGenerator {
	private static final int W = 1600;
	private static final int H = 1200;

	public static void main(String[] args) throws Exception {
		File out = args.length > 0 ? new File(args[0]) : new File("src/test/data/emf/fulltest.emf");
		File parent = out.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}

		EmfGdi gdi = new EmfGdi();
		gdi.placeableHeader(0, 0, 27940, 20955, 96);
		gdi.header();
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(W, H, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(1056, 790, null);
		gdi.setBkMode(Gdi.TRANSPARENT);
		gdi.setTextColor(rgb(0, 0, 0));

		GdiFont titleFont = gdi.createFontIndirect(-28, 0, 0, 0, 700, false, false, false,
				GdiFont.ANSI_CHARSET, 0, 0, 0, 0, ascii("Arial"));
		GdiFont labelFont = gdi.createFontIndirect(-17, 0, 0, 0, 400, false, false, false,
				GdiFont.ANSI_CHARSET, 0, 0, 0, 0, ascii("Arial"));
		GdiPen black = gdi.createPenIndirect(0, 2, rgb(0, 0, 0));
		GdiPen blue = gdi.createPenIndirect(0, 5, rgb(20, 70, 220));
		GdiPen red = gdi.createPenIndirect(0, 4, rgb(220, 30, 30));
		GdiPen green = gdi.createPenIndirect(0, 4, rgb(30, 140, 70));
		GdiBrush white = gdi.createBrushIndirect(0, rgb(255, 255, 255), 0);
		GdiBrush paleBlue = gdi.createBrushIndirect(0, rgb(190, 220, 240), 0);
		GdiBrush paleGreen = gdi.createBrushIndirect(0, rgb(205, 235, 205), 0);
		GdiBrush paleRed = gdi.createBrushIndirect(0, rgb(245, 200, 195), 0);
		GdiBrush yellow = gdi.createBrushIndirect(0, rgb(255, 245, 120), 0);
		GdiBrush hatch = gdi.createBrushIndirect(2, rgb(160, 160, 160), 4);
		GdiBrush blackBrush = gdi.createBrushIndirect(0, rgb(0, 0, 0), 0);

		gdi.selectObject(titleFont);
		gdi.textOut(24, 42, ascii("EMF GDI full command overview"));
		gdi.selectObject(labelFont);
		gdi.textOut(24, 72, ascii("A generated checklist image containing base EMR records plus EMF-specific records."));

		panel(gdi, black, white, 20, 100, 370, 295, "1 primitives");
		gdi.selectObject(paleBlue);
		gdi.rectangle(45, 145, 150, 210);
		gdi.ellipse(175, 140, 295, 215);
		gdi.selectObject(white);
		gdi.roundRect(305, 135, 355, 220, 22, 22);
		gdi.selectObject(red);
		gdi.moveToEx(50, 250, null);
		gdi.lineTo(170, 285);
		gdi.setPixel(340, 265, rgb(0, 0, 0));
		gdi.selectObject(blue);
		gdi.polyline(new Point[] {p(205, 280), p(250, 220), p(295, 285), p(345, 205)});

		panel(gdi, black, white, 395, 100, 745, 295, "2 arcs");
		gdi.selectObject(black);
		gdi.arc(425, 145, 545, 235, 425, 190, 545, 190);
		gdi.arcTo(580, 145, 710, 235, 580, 190, 710, 190);
		gdi.selectObject(paleGreen);
		gdi.chord(425, 225, 545, 315, 425, 270, 545, 270);
		gdi.pie(580, 225, 710, 315, 580, 270, 710, 270);
		gdi.angleArc(635, 190, 38, 25, 230);

		panel(gdi, black, white, 770, 100, 1120, 295, "3 paths and bezier");
		gdi.selectObject(blue);
		gdi.polyBezier(new Point[] {p(800, 230), p(850, 120), p(930, 315), p(990, 180)});
		gdi.beginPath();
		gdi.moveToEx(835, 250, null);
		gdi.polyBezierTo(new Point[] {p(880, 150), p(965, 330), p(1030, 190)});
		gdi.lineTo(1065, 260);
		gdi.closeFigure();
		gdi.endPath();
		gdi.selectObject(paleRed);
		gdi.strokeAndFillPath();
		gdi.beginPath();
		gdi.moveToEx(815, 150, null);
		gdi.lineTo(1100, 150);
		gdi.endPath();
		gdi.flattenPath();
		gdi.strokePath();
		gdi.beginPath();
		gdi.moveToEx(1080, 260, null);
		gdi.lineTo(1110, 260);
		gdi.lineTo(1095, 235);
		gdi.closeFigure();
		gdi.endPath();
		gdi.fillPath();

		panel(gdi, black, white, 1145, 100, 1575, 295, "4 polygon variants");
		gdi.selectObject(paleBlue);
		gdi.setPolyFillMode(Gdi.ALTERNATE);
		gdi.polygon(new Point[] {p(1175, 250), p(1225, 140), p(1285, 250), p(1230, 215)});
		gdi.selectObject(paleGreen);
		gdi.setPolyFillMode(Gdi.WINDING);
		gdi.polyPolygon(new Point[][] {
				new Point[] {p(1320, 245), p(1370, 140), p(1420, 245)},
				new Point[] {p(1450, 150), p(1545, 170), p(1500, 255), p(1430, 240)}
		});
		gdi.selectObject(red);
		gdi.polyPolyline(new Point[][] {
				new Point[] {p(1175, 130), p(1260, 120), p(1340, 135)},
				new Point[] {p(1175, 170), p(1260, 165), p(1340, 180)}
		});
		gdi.selectObject(green);
		gdi.moveToEx(1425, 170, null);
		gdi.polylineTo(new Point[] {p(1450, 135), p(1490, 120), p(1540, 150)});

		panel(gdi, black, white, 20, 320, 370, 530, "5 text records");
		gdi.selectObject(labelFont);
		gdi.setTextAlign(Gdi.TA_LEFT | Gdi.TA_TOP);
		gdi.textOut(45, 365, ascii("ExtTextOutA + TextOut"));
		gdi.extTextOut(45, 395, Gdi.ETO_OPAQUE, new int[] {40, 388, 235, 420}, ascii("opaque A text"), null);
		gdi.extTextOutW(45, 430, 0, null, utf16("ExtTextOutW"), null);
		gdi.setTextJustification(8, 2);
		gdi.textOut(45, 465, ascii("justified text"));
		gdi.setTextAlign(Gdi.TA_RIGHT | Gdi.TA_TOP);
		gdi.textOut(345, 495, ascii("right aligned"));
		gdi.setTextAlign(Gdi.TA_LEFT | Gdi.TA_TOP);

		panel(gdi, black, white, 395, 320, 745, 530, "6 raster transfer");
		byte[] dib = dib32(64, 48);
		gdi.bitBlt(dib, 420, 365, 82, 62, 0, 0, Gdi.SRCCOPY);
		gdi.stretchBlt(dib, 525, 355, 140, 70, 0, 0, 64, 48, Gdi.SRCCOPY);
		gdi.setDIBitsToDevice(420, 455, 64, 48, 0, 0, 0, 48, dib, Gdi.DIB_RGB_COLORS);
		gdi.stretchDIBits(520, 450, 120, 60, 0, 0, 64, 48, dib, Gdi.DIB_RGB_COLORS, Gdi.SRCCOPY);
		gdi.alphaBlend(dib, 650, 365, 70, 55, 0, 0, 64, 48, 0x00800000);
		gdi.transparentBlt(dib, 650, 455, 70, 55, 0, 0, 64, 48, rgb(255, 255, 255));
		gdi.textOut(650, 440, ascii("ALPHA / TRANS"));

		panel(gdi, black, white, 770, 320, 1120, 530, "7 mask blit");
		byte[] mask = mask1(64, 48);
		gdi.maskBlt(dib, 800, 370, 95, 60, 0, 0, mask, 0, 0, Gdi.SRCCOPY);
		gdi.plgBlt(dib, new Point[] {p(930, 360), p(1085, 395), p(900, 500)}, 0, 0, 64, 48, mask, 0, 0);

		panel(gdi, black, white, 1145, 320, 1575, 530, "8 gradients");
		gdi.gradientFill(new Trivertex[] {
				tv(1185, 380, 255, 40, 40), tv(1365, 455, 40, 40, 255)
		}, new GradientRect[] {new GradientRect(0, 1)}, Gdi.GRADIENT_FILL_RECT_H);
		gdi.gradientFill(new Trivertex[] {
				tv(1410, 370, 255, 70, 70), tv(1535, 400, 50, 200, 80), tv(1465, 500, 60, 80, 255)
		}, new GradientTriangle[] {new GradientTriangle(0, 1, 2)}, Gdi.GRADIENT_FILL_TRIANGLE);

		panel(gdi, black, white, 20, 555, 370, 790, "9 clipping and regions");
		GdiRegion region = gdi.createRectRgn(55, 620, 250, 745);
		gdi.selectObject(paleGreen);
		gdi.fillRgn(region, paleGreen);
		gdi.frameRgn(region, blackBrush, 6, 6);
		gdi.seveDC();
		gdi.intersectClipRect(75, 640, 225, 720);
		gdi.selectObject(red);
		gdi.rectangle(35, 600, 280, 760);
		gdi.restoreDC(-1);
		gdi.seveDC();
		gdi.excludeClipRect(125, 620, 190, 745);
		gdi.selectObject(blue);
		gdi.ellipse(80, 610, 260, 755);
		gdi.restoreDC(-1);

		panel(gdi, black, white, 395, 555, 745, 790, "10 palette and color");
		int[] paletteEntries = new int[] {rgb(255, 0, 0), rgb(0, 160, 0), rgb(0, 0, 255), rgb(255, 255, 0)};
		for (int i = 0; i < 4; i++) {
			gdi.selectObject(gdi.createBrushIndirect(0, paletteEntries[i], 0));
			gdi.rectangle(425 + i * 70, 625, 480 + i * 70, 690);
		}
		panel(gdi, black, white, 770, 555, 1120, 790, "11 16-bit records");
		gdi.selectObject(blue);
		gdi.polyBezier16(new Point[] {p(800, 720), p(850, 580), p(930, 790), p(990, 650)});
		gdi.polygon16(new Point[] {p(1020, 610), p(1090, 650), p(1050, 735), p(990, 700)});
		gdi.polyline16(new Point[] {p(800, 610), p(870, 630), p(940, 610)});
		gdi.moveToEx(805, 760, null);
		gdi.polyBezierTo16(new Point[] {p(860, 640), p(935, 800), p(1005, 660)});
		gdi.polylineTo16(new Point[] {p(1040, 710), p(1100, 680)});
		gdi.polyPolyline16(new Point[][] {new Point[] {p(795, 665), p(875, 680)}, new Point[] {p(795, 690), p(875, 705)}});
		gdi.polyPolygon16(new Point[][] {new Point[] {p(930, 590), p(965, 630), p(920, 650)}});

		panel(gdi, black, white, 1145, 555, 1575, 790, "12 DC and state");
		gdi.selectObject(blue);
		gdi.moveToEx(1185, 645, null);
		gdi.lineTo(1535, 735);
		gdi.selectObject(hatch);
		gdi.rectangle(1185, 700, 1315, 745);
		gdi.setBrushOrgEx(1160, 570, null);
		gdi.setMapperFlags(1);
		gdi.setStretchBltMode(Gdi.COLORONCOLOR);
		gdi.setROP2(Gdi.R2_XORPEN);
		gdi.setArcDirection(Gdi.AD_CLOCKWISE);
		gdi.setMiterLimit(8);
		gdi.scaleViewportExtEx(1, 1, 1, 1, null);
		gdi.scaleWindowExtEx(1, 1, 1, 1, null);
		gdi.setLayout(Gdi.LAYOUT_BITMAPORIENTATIONPRESERVED);
		gdi.setBkColor(rgb(235, 235, 235));
		GdiPalette palette = gdi.createPalette(0x300, paletteEntries);
		gdi.selectPalette(palette, false);
		gdi.resizePalette(palette, paletteEntries.length);
		gdi.realizePalette();
		gdi.deleteObject(red);

		panel(gdi, black, white, 20, 815, 1575, 1145, "13 direct EMR records");
		gdi.selectObject(labelFont);
		gdi.setTextAlign(Gdi.TA_LEFT | Gdi.TA_TOP);
		gdi.textOut(45, 855, ascii("Records generated by newly exposed EmfGdi APIs."));

		gdi.selectObject(blue);
		gdi.polyDraw(new Point[] {p(55, 920), p(110, 870), p(170, 945), p(230, 870), p(295, 930)},
				new byte[] {0x06, 0x02, 0x04, 0x04, 0x04});
		gdi.selectObject(green);
		gdi.polyDraw16(new Point[] {p(55, 995), p(120, 1040), p(190, 990), p(260, 1040)},
				new byte[] {0x06, 0x02, 0x02, 0x03});

		gdi.selectObject(labelFont);
		gdi.polyTextOutA(new Point[] {p(340, 895), p(340, 930)}, null, null,
				new byte[][] {ascii("PolyTextOutA"), ascii("with dx spacing")},
				new int[][] {null, new int[] {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10}});
		gdi.polyTextOutW(new Point[] {p(340, 970)}, null, null,
				new byte[][] {utf16("PolyTextOutW")}, null);
		gdi.smallTextOut(340, 1010, 0x00000300, null, ascii("SmallTextOut"));

		GdiPatternBrush monoBrush = gdi.createMonoBrush(mask1(32, 32), Gdi.DIB_RGB_COLORS);
		gdi.selectObject(monoBrush);
		gdi.selectObject(black);
		gdi.rectangle(600, 890, 725, 1015);
		gdi.selectObject(white);
		gdi.setBrushOrgEx(600, 890, null);
		gdi.textOut(600, 1040, ascii("CreateMonoBrush"));

		gdi.setWorldTransform(new float[] {1, 0, 0, 1, 0, 0});
		gdi.modifyWorldTransform(new float[] {1, 0, 0, 1, 0, 0}, 2);

		try (FileOutputStream fos = new FileOutputStream(out)) {
			gdi.write(fos);
		}
	}

	private static void panel(EmfGdi gdi, GdiPen pen, GdiBrush brush, int x1, int y1, int x2, int y2, String label) {
		gdi.selectObject(pen);
		gdi.selectObject(brush);
		gdi.rectangle(x1, y1, x2, y2);
		gdi.textOut(x1 + 12, y1 + 24, ascii(label));
	}

	private static Point p(int x, int y) {
		return new Point(x, y);
	}

	private static Trivertex tv(int x, int y, int r, int g, int b) {
		return new Trivertex(x, y, r << 8, g << 8, b << 8, 0);
	}

	private static int rgb(int r, int g, int b) {
		return (b << 16) | (g << 8) | r;
	}

	private static byte[] ascii(String s) {
		try {
			return s.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			return new byte[0];
		}
	}

	private static byte[] utf16(String s) {
		try {
			return s.getBytes("UTF-16LE");
		} catch (UnsupportedEncodingException e) {
			return new byte[0];
		}
	}

	private static byte[] dib32(int width, int height) {
		int stride = width * 4;
		byte[] dib = new byte[40 + stride * height];
		setInt(dib, 0, 40);
		setInt(dib, 4, width);
		setInt(dib, 8, height);
		setShort(dib, 12, 1);
		setShort(dib, 14, 32);
		setInt(dib, 20, stride * height);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int topY = height - 1 - y;
				int pos = 40 + y * stride + x * 4;
				boolean checker = ((x / 8) + (topY / 8)) % 2 == 0;
				dib[pos] = (byte)(checker ? 220 : 70);
				dib[pos + 1] = (byte)(x * 255 / Math.max(1, width - 1));
				dib[pos + 2] = (byte)(topY * 255 / Math.max(1, height - 1));
				dib[pos + 3] = (byte)255;
			}
		}
		return dib;
	}

	private static byte[] mask1(int width, int height) {
		int stride = ((width + 31) / 32) * 4;
		byte[] dib = new byte[40 + 8 + stride * height];
		setInt(dib, 0, 40);
		setInt(dib, 4, width);
		setInt(dib, 8, height);
		setShort(dib, 12, 1);
		setShort(dib, 14, 1);
		setInt(dib, 20, stride * height);
		setInt(dib, 32, 2);
		setInt(dib, 40, rgb(0, 0, 0));
		setInt(dib, 44, rgb(255, 255, 255));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if ((x - width / 2) * (x - width / 2) + (y - height / 2) * (y - height / 2) < 560) {
					int pos = 48 + y * stride + x / 8;
					dib[pos] |= (byte)(0x80 >>> (x % 8));
				}
			}
		}
		return dib;
	}

	private static void setShort(byte[] data, int pos, int value) {
		data[pos] = (byte)(value & 0xFF);
		data[pos + 1] = (byte)((value >>> 8) & 0xFF);
	}

	private static void setInt(byte[] data, int pos, int value) {
		data[pos] = (byte)(value & 0xFF);
		data[pos + 1] = (byte)((value >>> 8) & 0xFF);
		data[pos + 2] = (byte)((value >>> 16) & 0xFF);
		data[pos + 3] = (byte)((value >>> 24) & 0xFF);
	}
}

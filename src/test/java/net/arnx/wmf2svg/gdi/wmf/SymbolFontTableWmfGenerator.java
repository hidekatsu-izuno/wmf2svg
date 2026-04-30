package net.arnx.wmf2svg.gdi.wmf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiPen;

public class SymbolFontTableWmfGenerator {
	private static final int W = 15000;
	private static final int H = 13300;

	private static final int TABLE_W = 6800;
	private static final int TABLE_H = 6300;
	private static final int CELL = 360;
	private static final int HEADER = 520;
	private static final int TITLE_H = 620;

	public static void main(String[] args) throws Exception {
		File out = args.length > 0 ? new File(args[0]) : new File("src/test/data/symbol/symbol-font-tables.wmf");
		File parent = out.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}

		WmfGdi gdi = new WmfGdi();
		gdi.placeableHeader(0, 0, W, H, 2540);
		gdi.header();
		gdi.setMapMode(Gdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(W, H, null);
		gdi.setViewportOrgEx(0, 0, null);
		gdi.setViewportExtEx(W, H, null);
		gdi.setBkMode(Gdi.TRANSPARENT);
		gdi.setTextColor(rgb(0, 0, 0));
		gdi.setBkColor(rgb(255, 255, 255));

		GdiPen black = gdi.createPenIndirect(GdiPen.PS_SOLID, 24, rgb(0, 0, 0));
		GdiPen grid = gdi.createPenIndirect(GdiPen.PS_SOLID, 12, rgb(160, 160, 160));
		GdiBrush white = gdi.createBrushIndirect(GdiBrush.BS_SOLID, rgb(255, 255, 255), 0);
		GdiBrush hollow = gdi.createBrushIndirect(GdiBrush.BS_HOLLOW, rgb(0, 0, 0), 0);

		GdiFont titleFont = gdi.createFontIndirect(-380, 0, 0, 0, GdiFont.FW_BOLD, false, false, false,
				GdiFont.ANSI_CHARSET, 0, 0, 0, 0, ascii("Arial"));
		GdiFont labelFont = gdi.createFontIndirect(-210, 0, 0, 0, GdiFont.FW_NORMAL, false, false, false,
				GdiFont.ANSI_CHARSET, 0, 0, 0, 0, ascii("Arial"));
		GdiFont symbol = symbolFont(gdi, "Symbol");
		GdiFont wingdings = symbolFont(gdi, "Wingdings");
		GdiFont wingdings2 = symbolFont(gdi, "Wingdings 2");
		GdiFont wingdings3 = symbolFont(gdi, "Wingdings 3");

		drawTable(gdi, black, grid, white, hollow, titleFont, labelFont, symbol, 600, 500, "Symbol");
		drawTable(gdi, black, grid, white, hollow, titleFont, labelFont, wingdings, 7900, 500, "Wingdings");
		drawTable(gdi, black, grid, white, hollow, titleFont, labelFont, wingdings2, 600, 6850, "Wingdings 2");
		drawTable(gdi, black, grid, white, hollow, titleFont, labelFont, wingdings3, 7900, 6850, "Wingdings 3");

		gdi.footer();

		FileOutputStream fout = new FileOutputStream(out);
		try {
			gdi.write(fout);
		} finally {
			fout.close();
		}
	}

	private static void drawTable(WmfGdi gdi, GdiPen black, GdiPen grid, GdiBrush white, GdiBrush hollow,
			GdiFont titleFont, GdiFont labelFont, GdiFont tableFont, int x, int y, String title) {
		int left = x;
		int top = y + TITLE_H;
		int rowHeader = left + HEADER;
		int colHeader = top + HEADER;
		int right = rowHeader + CELL * 16;
		int bottom = colHeader + CELL * 14;

		gdi.selectObject(black);
		gdi.selectObject(white);
		gdi.rectangle(x, y, x + TABLE_W, y + TABLE_H);

		gdi.selectObject(titleFont);
		gdi.setTextAlign(Gdi.TA_LEFT | Gdi.TA_TOP);
		gdi.textOut(x + 220, y + 160, ascii(title));

		gdi.selectObject(grid);
		gdi.selectObject(hollow);
		for (int i = 0; i <= 16; i++) {
			int gx = rowHeader + i * CELL;
			gdi.moveToEx(gx, top, null);
			gdi.lineTo(gx, bottom);
		}
		for (int i = 0; i <= 14; i++) {
			int gy = colHeader + i * CELL;
			gdi.moveToEx(left, gy, null);
			gdi.lineTo(right, gy);
		}
		gdi.moveToEx(left, top, null);
		gdi.lineTo(right, top);
		gdi.moveToEx(left, top, null);
		gdi.lineTo(left, bottom);

		gdi.selectObject(black);
		gdi.rectangle(left, top, right, bottom);

		gdi.selectObject(labelFont);
		gdi.setTextAlign(Gdi.TA_CENTER | Gdi.TA_TOP);
		for (int col = 0; col < 16; col++) {
			gdi.textOut(rowHeader + col * CELL + CELL / 2, top + 130, ascii(hex(col)));
		}
		for (int row = 2; row <= 15; row++) {
			gdi.textOut(left + HEADER / 2, colHeader + (row - 2) * CELL + 80, ascii(hex(row)));
		}

		gdi.selectObject(tableFont);
		gdi.setTextAlign(Gdi.TA_CENTER | Gdi.TA_TOP);
		for (int row = 2; row <= 15; row++) {
			for (int col = 0; col < 16; col++) {
				int code = (row << 4) | col;
				gdi.textOut(rowHeader + col * CELL + CELL / 2, colHeader + (row - 2) * CELL + 45,
						new byte[]{(byte) code});
			}
		}
		gdi.setTextAlign(Gdi.TA_LEFT | Gdi.TA_TOP);
	}

	private static GdiFont symbolFont(WmfGdi gdi, String faceName) {
		return gdi.createFontIndirect(-260, 0, 0, 0, GdiFont.FW_NORMAL, false, false, false, GdiFont.SYMBOL_CHARSET, 0,
				0, 0, 0, ascii(faceName));
	}

	private static String hex(int value) {
		return Integer.toHexString(value).toUpperCase();
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
}

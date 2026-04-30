package net.arnx.wmf2svg.gdi.emf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiColorSpace;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiObject;
import net.arnx.wmf2svg.gdi.GdiPalette;
import net.arnx.wmf2svg.gdi.GdiPatternBrush;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.GdiRegion;
import net.arnx.wmf2svg.gdi.GradientRect;
import net.arnx.wmf2svg.gdi.GradientTriangle;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Size;
import net.arnx.wmf2svg.gdi.Trivertex;

public class EmfGdi implements Gdi, EmfConstants {
	private static final int EXT_LOG_FONT_W_SIZE = 320;
	private static final int ETO_NO_RECT = 0x00000100;
	private static final int ETO_SMALL_CHARS = 0x00000200;

	private final List<byte[]> records = new ArrayList<byte[]>();
	private int nextHandle = 1;
	private int left = Integer.MAX_VALUE;
	private int top = Integer.MAX_VALUE;
	private int right = Integer.MIN_VALUE;
	private int bottom = Integer.MIN_VALUE;
	private int frameLeft;
	private int frameTop;
	private int frameRight = 10000;
	private int frameBottom = 10000;

	public void write(OutputStream out) throws IOException {
		footer();
		byte[] headerRecord = createHeaderRecord();
		out.write(headerRecord);
		for (int i = 0; i < records.size(); i++) {
			out.write(records.get(i));
		}
		out.write(createEofRecord());
		out.flush();
	}

	public void placeableHeader(int vsx, int vsy, int vex, int vey, int dpi) {
		frameLeft = vsx;
		frameTop = vsy;
		frameRight = vex;
		frameBottom = vey;
	}

	public void header() {
	}

	public void footer() {
	}

	public void animatePalette(GdiPalette palette, int startIndex, int[] entries) {
		throw new UnsupportedOperationException("EMF does not support AnimatePalette.");
	}

	public void alphaBlend(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, int blendFunction) {
		writeBlendRecord(EMR_ALPHABLEND, image, dx, dy, dw, dh, sx, sy, sw, sh, blendFunction);
	}

	public void angleArc(int x, int y, int radius, float startAngle, float sweepAngle) {
		byte[] record = record(EMR_ANGLEARC, 20);
		setInt32(record, 8, x);
		setInt32(record, 12, y);
		setInt32(record, 16, radius);
		setFloat(record, 20, startAngle);
		setFloat(record, 24, sweepAngle);
		records.add(record);
		includeRect(x - radius, y - radius, x + radius, y + radius);
	}

	public void arc(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		arcRecord(EMR_ARC, sxr, syr, exr, eyr, sxa, sya, exa, eya);
	}

	public void arcTo(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		arcRecord(EMR_ARCTO, sxr, syr, exr, eyr, sxa, sya, exa, eya);
	}

	public void abortPath() {
		records.add(record(EMR_ABORTPATH, 0));
	}

	public void beginPath() {
		records.add(record(EMR_BEGINPATH, 0));
	}

	public void bitBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop) {
		writeBitBltRecord(image, dx, dy, dw, dh, sx, sy, rop);
	}

	public void maskBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, byte[] mask, int mx, int my, long rop) {
		writeMaskBltRecord(image, dx, dy, dw, dh, sx, sy, mask, mx, my, rop);
	}

	public void plgBlt(byte[] image, Point[] points, int sx, int sy, int sw, int sh,
			byte[] mask, int mx, int my) {
		writePlgBltRecord(image, points, sx, sy, sw, sh, mask, mx, my);
	}

	public void chord(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		arcRecord(EMR_CHORD, sxr, syr, exr, eyr, sxa, sya, exa, eya);
	}

	public void closeFigure() {
		records.add(record(EMR_CLOSEFIGURE, 0));
	}

	public void colorCorrectPalette(GdiPalette palette, int startIndex, int entries) {
		byte[] record = record(EMR_COLORCORRECTPALETTE, 12);
		setInt32(record, 8, objectId(palette));
		setInt32(record, 12, startIndex);
		setInt32(record, 16, entries);
		records.add(record);
	}

	public GdiBrush createBrushIndirect(int style, int color, int hatch) {
		EmfBrush brush = new EmfBrush(allocateHandle(), style, color, hatch);
		byte[] record = record(EMR_CREATEBRUSHINDIRECT, 16);
		setInt32(record, 8, brush.id);
		setInt32(record, 12, style);
		setInt32(record, 16, color);
		setInt32(record, 20, hatch);
		records.add(record);
		return brush;
	}

	public GdiColorSpace createColorSpace(byte[] logColorSpace) {
		return createColorSpaceRecord(EMR_CREATECOLORSPACE, logColorSpace);
	}

	public GdiColorSpace createColorSpaceW(byte[] logColorSpace) {
		return createColorSpaceRecord(EMR_CREATECOLORSPACEW, logColorSpace);
	}

	private GdiColorSpace createColorSpaceRecord(int type, byte[] logColorSpace) {
		EmfColorSpace colorSpace = new EmfColorSpace(allocateHandle(), copy(logColorSpace));
		byte[] record = record(type, 4 + colorSpace.data.length);
		setInt32(record, 8, colorSpace.id);
		setBytes(record, 12, colorSpace.data);
		records.add(record);
		return colorSpace;
	}

	public GdiFont createFontIndirect(int height, int width, int escapement,
			int orientation, int weight, boolean italic, boolean underline,
			boolean strikeout, int charset, int outPrecision, int clipPrecision,
			int quality, int pitchAndFamily, byte[] faceName) {
		EmfFont font = new EmfFont(allocateHandle(), height, width, escapement, orientation,
				weight, italic, underline, strikeout, charset, outPrecision,
				clipPrecision, quality, pitchAndFamily, faceName);
		byte[] record = record(EMR_EXTCREATEFONTINDIRECTW, 4 + EXT_LOG_FONT_W_SIZE);
		setInt32(record, 8, font.id);
		writeLogFont(record, 12, font);
		records.add(record);
		return font;
	}

	public GdiPalette createPalette(int version, int[] palEntry) {
		int[] entries = palEntry != null ? palEntry : new int[0];
		EmfPalette palette = new EmfPalette(allocateHandle(), version, entries);
		byte[] record = record(EMR_CREATEPALETTE, 8 + entries.length * 4);
		setInt32(record, 8, palette.id);
		setUInt16(record, 12, version);
		setUInt16(record, 14, entries.length);
		for (int i = 0; i < entries.length; i++) {
			setInt32(record, 16 + i * 4, entries[i]);
		}
		records.add(record);
		return palette;
	}

	public GdiPatternBrush createPatternBrush(byte[] image) {
		throw new UnsupportedOperationException("EMF does not support CreatePatternBrush.");
	}

	public GdiPen createPenIndirect(int style, int width, int color) {
		EmfPen pen = new EmfPen(allocateHandle(), style, width, color);
		int emfStyle = style & ~GdiPen.PS_DEVICE_WIDTH;
		byte[] record = record(EMR_CREATEPEN, 20);
		setInt32(record, 8, pen.id);
		setInt32(record, 12, emfStyle);
		setInt32(record, 16, width);
		setInt32(record, 20, 0);
		setInt32(record, 24, color);
		records.add(record);
		return pen;
	}

	public GdiRegion createRectRgn(int left, int top, int right, int bottom) {
		return new EmfRegion(0, createRectRegionData(left, top, right, bottom));
	}

	public void deleteObject(GdiObject obj) {
		int id = objectId(obj);
		byte[] record = record(EMR_DELETEOBJECT, 4);
		setInt32(record, 8, id);
		records.add(record);
	}

	public boolean deleteColorSpace(GdiColorSpace colorSpace) {
		byte[] record = record(EMR_DELETECOLORSPACE, 4);
		setInt32(record, 8, objectId(colorSpace));
		records.add(record);
		return true;
	}

	public void dibBitBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop) {
		throw new UnsupportedOperationException("EMF does not support DibBitBlt.");
	}

	public GdiPatternBrush dibCreatePatternBrush(byte[] image, int usage) {
		return dibPatternBrushRecord(EMR_CREATEDIBPATTERNBRUSHPT, image, usage);
	}

	public GdiPatternBrush createMonoBrush(byte[] image, int usage) {
		return dibPatternBrushRecord(EMR_CREATEMONOBRUSH, image, usage);
	}

	private GdiPatternBrush dibPatternBrushRecord(int type, byte[] image, int usage) {
		EmfPatternBrush brush = new EmfPatternBrush(allocateHandle(), copy(image));
		byte[] dib = brush.getPattern();
		int bitsOffset = getDibBitsOffset(dib, usage);
		int bmiSize = bitsOffset > 0 ? bitsOffset : dib.length;
		int bitsSize = Math.max(0, dib.length - bmiSize);
		int dibOffset = 32;
		byte[] record = record(type, dibOffset - 8 + dib.length);
		setInt32(record, 8, brush.id);
		setInt32(record, 12, usage);
		setInt32(record, 16, dibOffset);
		setInt32(record, 20, bmiSize);
		setInt32(record, 24, dibOffset + bmiSize);
		setInt32(record, 28, bitsSize);
		setBytes(record, dibOffset, dib);
		records.add(record);
		return brush;
	}

	public void dibStretchBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, long rop) {
		throw new UnsupportedOperationException("EMF does not support DibStretchBlt.");
	}

	public void ellipse(int sx, int sy, int ex, int ey) {
		rectangleRecord(EMR_ELLIPSE, sx, sy, ex, ey);
	}

	public void endPath() {
		records.add(record(EMR_ENDPATH, 0));
	}

	public void escape(byte[] data) {
		byte[] bytes = copy(data);
		if (bytes.length >= 4) {
			int escapeFunction = readUInt16(bytes, 0);
			int count = readUInt16(bytes, 2);
			int length = Math.min(count, bytes.length - 4);
			writeEscapeRecord(EMR_EXTESCAPE, escapeFunction, bytes, 4, length);
		} else {
			writeEscapeRecord(EMR_EXTESCAPE, 0, bytes, 0, bytes.length);
		}
	}

	public void drawEscape(int escapeFunction, byte[] data) {
		byte[] bytes = copy(data);
		writeEscapeRecord(EMR_DRAWESCAPE, escapeFunction, bytes, 0, bytes.length);
	}

	public void extEscape(int escapeFunction, byte[] data) {
		byte[] bytes = copy(data);
		writeEscapeRecord(EMR_EXTESCAPE, escapeFunction, bytes, 0, bytes.length);
	}

	public void namedEscape(int escapeFunction, byte[] driver, byte[] data) {
		byte[] driverBytes = unicodeNullTerminated(driver);
		byte[] dataBytes = copy(data);
		byte[] record = record(EMR_NAMEDESCAPE, 12 + driverBytes.length + dataBytes.length);
		setInt32(record, 8, escapeFunction);
		setInt32(record, 12, driverBytes.length);
		setInt32(record, 16, dataBytes.length);
		setBytes(record, 20, driverBytes);
		setBytes(record, 20 + driverBytes.length, dataBytes);
		records.add(record);
	}

	public void comment(byte[] data) {
		byte[] bytes = copy(data);
		byte[] record = record(EMR_GDICOMMENT, 4 + bytes.length);
		setInt32(record, 8, bytes.length);
		setBytes(record, 12, bytes);
		records.add(record);
	}

	public int excludeClipRect(int left, int top, int right, int bottom) {
		rectangleRecord(EMR_EXCLUDECLIPRECT, left, top, right, bottom);
		return GdiRegion.SIMPLEREGION;
	}

	public void extFloodFill(int x, int y, int color, int type) {
		byte[] record = record(EMR_EXTFLOODFILL, 16);
		setInt32(record, 8, x);
		setInt32(record, 12, y);
		setInt32(record, 16, color);
		setInt32(record, 20, type);
		records.add(record);
		includePoint(x, y);
	}

	public GdiRegion extCreateRegion(float[] xform, int count, byte[] rgnData) {
		return new EmfRegion(0, copy(rgnData));
	}

	public int extSelectClipRgn(GdiRegion rgn, int mode) {
		byte[] data = rgn instanceof EmfRegion ? ((EmfRegion)rgn).data : null;
		int dataSize = data != null ? data.length : 0;
		byte[] record = record(EMR_EXTSELECTCLIPRGN, 8 + dataSize);
		setInt32(record, 8, dataSize);
		setInt32(record, 12, mode);
		if (dataSize > 0) {
			setBytes(record, 16, data);
		}
		records.add(record);
		return GdiRegion.SIMPLEREGION;
	}

	public void extTextOut(int x, int y, int options, int[] rect, byte[] text, int[] lpdx) {
		extTextOutRecord(EMR_EXTTEXTOUTA, x, y, options, rect, copy(text), lpdx, 1);
	}

	public void extTextOutW(int x, int y, int options, int[] rect, byte[] text, int[] lpdx) {
		extTextOutRecord(EMR_EXTTEXTOUTW, x, y, options, rect, copy(text), lpdx, 2);
	}

	public void polyTextOutA(Point[] points, int[] options, int[][] rects, byte[][] texts, int[][] lpdx) {
		polyTextOutRecord(EMR_POLYTEXTOUTA, points, options, rects, texts, lpdx, 1);
	}

	public void polyTextOutW(Point[] points, int[] options, int[][] rects, byte[][] texts, int[][] lpdx) {
		polyTextOutRecord(EMR_POLYTEXTOUTW, points, options, rects, texts, lpdx, 2);
	}

	public void smallTextOut(int x, int y, int options, int[] rect, byte[] text) {
		byte[] bytes = copy(text);
		boolean smallChars = (options & ETO_SMALL_CHARS) != 0;
		int chars = smallChars ? bytes.length : bytes.length / 2;
		int rectSize = (options & ETO_NO_RECT) == 0 ? 16 : 0;
		byte[] record = record(EMR_SMALLTEXTOUT, 28 + rectSize + bytes.length);
		setInt32(record, 8, x);
		setInt32(record, 12, y);
		setInt32(record, 16, chars);
		setInt32(record, 20, options);
		setInt32(record, 24, 1);
		setFloat(record, 28, 1);
		setFloat(record, 32, 1);
		if (rectSize > 0) {
			writeRect(record, 36, rect);
		}
		setBytes(record, 36 + rectSize, bytes);
		records.add(record);
		includePoint(x, y);
		if (rectSize > 0 && rect != null && rect.length >= 4) {
			includeRect(rect[0], rect[1], rect[2], rect[3]);
		}
	}

	public GdiPen extCreatePen(int style, int width, int color) {
		EmfPen pen = new EmfPen(allocateHandle(), style, width, color);
		int emfStyle = style & ~GdiPen.PS_DEVICE_WIDTH;
		byte[] record = record(EMR_EXTCREATEPEN, 44);
		setInt32(record, 8, pen.id);
		setInt32(record, 28, emfStyle);
		setInt32(record, 32, width);
		setInt32(record, 36, GdiBrush.BS_SOLID);
		setInt32(record, 40, color);
		setInt32(record, 44, 0);
		setInt32(record, 48, 0);
		records.add(record);
		return pen;
	}

	public void polylineTo(Point[] points) {
		polyPointRecord(EMR_POLYLINETO, points);
	}

	public void polyDraw(Point[] points, byte[] types) {
		polyDrawRecord(EMR_POLYDRAW, points, types, false);
	}

	public void polyPolyline(Point[][] points) {
		polyPolyPointRecord(EMR_POLYPOLYLINE, points);
	}

	public void polyBezier16(Point[] points) {
		polyPointRecord16(EMR_POLYBEZIER16, points);
	}

	public void polyDraw16(Point[] points, byte[] types) {
		polyDrawRecord(EMR_POLYDRAW16, points, types, true);
	}

	public void polygon16(Point[] points) {
		polyPointRecord16(EMR_POLYGON16, points);
	}

	public void polyline16(Point[] points) {
		polyPointRecord16(EMR_POLYLINE16, points);
	}

	public void polyBezierTo16(Point[] points) {
		polyPointRecord16(EMR_POLYBEZIERTO16, points);
	}

	public void polylineTo16(Point[] points) {
		polyPointRecord16(EMR_POLYLINETO16, points);
	}

	public void polyPolyline16(Point[][] points) {
		polyPolyPointRecord16(EMR_POLYPOLYLINE16, points);
	}

	public void polyPolygon16(Point[][] points) {
		polyPolyPointRecord16(EMR_POLYPOLYGON16, points);
	}

	private void extTextOutRecord(int type, int x, int y, int options, int[] rect, byte[] bytes, int[] lpdx, int charSize) {
		int chars = bytes.length;
		if (charSize > 1) {
			chars /= charSize;
		}
		int dxSize = lpdx != null ? lpdx.length * 4 : 0;
		int stringOffset = 8 + 76;
		int byteCount = bytes.length;
		int dxOffset = dxSize > 0 ? align4(stringOffset + byteCount) : 0;
		int payload = dxSize > 0 ? dxOffset - 8 + dxSize : stringOffset - 8 + byteCount;
		byte[] record = record(type, payload);
		writeBounds(record, 8);
		setInt32(record, 24, 1);
		setFloat(record, 28, 1);
		setFloat(record, 32, 1);
		setInt32(record, 36, x);
		setInt32(record, 40, y);
		setInt32(record, 44, chars);
		setInt32(record, 48, stringOffset);
		setInt32(record, 52, options);
		writeRect(record, 56, rect);
		setInt32(record, 72, dxOffset);
		setBytes(record, stringOffset, bytes);
		if (lpdx != null) {
			for (int i = 0; i < lpdx.length; i++) {
				setInt32(record, dxOffset + i * 4, lpdx[i]);
			}
		}
		records.add(record);
		includePoint(x, y);
	}

	private void polyTextOutRecord(int type, Point[] points, int[] options, int[][] rects,
			byte[][] texts, int[][] lpdx, int charSize) {
		byte[][] bytes = texts != null ? texts : new byte[0][];
		int count = bytes.length;
		byte[][] textValues = new byte[count][];
		int stringOffset = align4(8 + 32 + count * 40);
		int payload = stringOffset - 8;
		for (int i = 0; i < count; i++) {
			textValues[i] = copy(bytes[i]);
			payload += textValues[i].length;
			int[] dx = getIntArray(lpdx, i);
			if (dx != null) {
				payload = align4(8 + payload) - 8 + dx.length * 4;
			}
			payload = align4(8 + payload) - 8;
		}

		byte[] record = record(type, payload);
		writeBounds(record, 8);
		setInt32(record, 24, 1);
		setFloat(record, 28, 1);
		setFloat(record, 32, 1);
		setInt32(record, 36, count);

		int dataOffset = stringOffset;
		for (int i = 0; i < count; i++) {
			Point point = getPoint(points, i);
			byte[] text = textValues[i];
			int[] dx = getIntArray(lpdx, i);
			int charCount = charSize > 1 ? text.length / charSize : text.length;
			int entryOffset = 40 + i * 40;
			setInt32(record, entryOffset, point.x);
			setInt32(record, entryOffset + 4, point.y);
			setInt32(record, entryOffset + 8, charCount);
			setInt32(record, entryOffset + 12, dataOffset);
			setInt32(record, entryOffset + 16, getIntValue(options, i));
			writeRect(record, entryOffset + 20, getRect(rects, i));
			setInt32(record, entryOffset + 36, dx != null ? align4(dataOffset + text.length) : 0);
			setBytes(record, dataOffset, text);
			if (dx != null) {
				int dxOffset = align4(dataOffset + text.length);
				for (int j = 0; j < dx.length; j++) {
					setInt32(record, dxOffset + j * 4, dx[j]);
				}
				dataOffset = align4(dxOffset + dx.length * 4);
			} else {
				dataOffset = align4(dataOffset + text.length);
			}
			includePoint(point.x, point.y);
		}
		records.add(record);
	}

	public void fillRgn(GdiRegion rgn, GdiBrush brush) {
		byte[] data = regionData(rgn);
		byte[] record = record(EMR_FILLRGN, 16 + 4 + 4 + data.length);
		writeRegionBounds(record, 8, data);
		setInt32(record, 24, data.length);
		setInt32(record, 28, objectId(brush));
		setBytes(record, 32, data);
		records.add(record);
		includeRegion(data);
	}

	public void flattenPath() {
		records.add(record(EMR_FLATTENPATH, 0));
	}

	public void widenPath() {
		records.add(record(EMR_WIDENPATH, 0));
	}

	public void floodFill(int x, int y, int color) {
		throw new UnsupportedOperationException("EMF does not support FloodFill.");
	}

	public void gradientFill(Trivertex[] vertex, GradientRect[] mesh, int mode) {
		writeGradientFill(vertex, mesh, null, mode);
	}

	public void gradientFill(Trivertex[] vertex, GradientTriangle[] mesh, int mode) {
		writeGradientFill(vertex, null, mesh, mode);
	}

	public void frameRgn(GdiRegion rgn, GdiBrush brush, int w, int h) {
		byte[] data = regionData(rgn);
		byte[] record = record(EMR_FRAMERGN, 16 + 4 + 4 + 8 + data.length);
		writeRegionBounds(record, 8, data);
		setInt32(record, 24, data.length);
		setInt32(record, 28, objectId(brush));
		setInt32(record, 32, w);
		setInt32(record, 36, h);
		setBytes(record, 40, data);
		records.add(record);
		includeRegion(data);
	}

	public void intersectClipRect(int left, int top, int right, int bottom) {
		rectangleRecord(EMR_INTERSECTCLIPRECT, left, top, right, bottom);
	}

	public void invertRgn(GdiRegion rgn) {
		writeRegionRecord(EMR_INVERTRGN, rgn);
	}

	public void lineTo(int ex, int ey) {
		pointRecord(EMR_LINETO, ex, ey);
	}

	public void moveToEx(int x, int y, Point old) {
		pointRecord(EMR_MOVETOEX, x, y);
	}

	public void offsetClipRgn(int x, int y) {
		pointRecord(EMR_OFFSETCLIPRGN, x, y);
	}

	public void offsetViewportOrgEx(int x, int y, Point point) {
		throw new UnsupportedOperationException("EMF does not support OffsetViewportOrgEx.");
	}

	public void offsetWindowOrgEx(int x, int y, Point point) {
		throw new UnsupportedOperationException("EMF does not support OffsetWindowOrgEx.");
	}

	public void paintRgn(GdiRegion rgn) {
		writeRegionRecord(EMR_PAINTRGN, rgn);
	}

	public void patBlt(int x, int y, int width, int height, long rop) {
		byte[] record = record(EMR_BITBLT, 92);
		writeRect(record, 8, x, y, x + width, y + height);
		setInt32(record, 24, x);
		setInt32(record, 28, y);
		setInt32(record, 32, width);
		setInt32(record, 36, height);
		setInt32(record, 40, (int)rop);
		setInt32(record, 44, x);
		setInt32(record, 48, y);
		setIdentityXForm(record, 52);
		records.add(record);
		includeRect(x, y, x + width, y + height);
	}

	public void pie(int sx, int sy, int ex, int ey, int sxr, int syr, int exr, int eyr) {
		arcRecord(EMR_PIE, sx, sy, ex, ey, sxr, syr, exr, eyr);
	}

	public void polyBezier(Point[] points) {
		polyPointRecord(EMR_POLYBEZIER, points);
	}

	public void polyBezierTo(Point[] points) {
		polyPointRecord(EMR_POLYBEZIERTO, points);
	}

	public void polygon(Point[] points) {
		polyPointRecord(EMR_POLYGON, points);
	}

	public void polyline(Point[] points) {
		polyPointRecord(EMR_POLYLINE, points);
	}

	public void polyPolygon(Point[][] points) {
		polyPolyPointRecord(EMR_POLYPOLYGON, points);
	}

	public void fillPath() {
		pathPaintRecord(EMR_FILLPATH);
	}

	public void strokePath() {
		pathPaintRecord(EMR_STROKEPATH);
	}

	public void strokeAndFillPath() {
		pathPaintRecord(EMR_STROKEANDFILLPATH);
	}

	public void realizePalette() {
		records.add(record(EMR_REALIZEPALETTE, 0));
	}

	public void restoreDC(int savedDC) {
		valueRecord(EMR_RESTOREDC, savedDC);
	}

	public void rectangle(int sx, int sy, int ex, int ey) {
		rectangleRecord(EMR_RECTANGLE, sx, sy, ex, ey);
	}

	public void resizePalette(GdiPalette palette, int entries) {
		byte[] record = record(EMR_RESIZEPALETTE, 8);
		setInt32(record, 8, objectId(palette));
		setInt32(record, 12, entries);
		records.add(record);
	}

	public void roundRect(int sx, int sy, int ex, int ey, int rw, int rh) {
		byte[] record = record(EMR_ROUNDRECT, 24);
		writeRect(record, 8, sx, sy, ex, ey);
		setInt32(record, 24, rw);
		setInt32(record, 28, rh);
		records.add(record);
		includeRect(sx, sy, ex, ey);
	}

	public void seveDC() {
		records.add(record(EMR_SAVEDC, 0));
	}

	public void scaleViewportExtEx(int x, int xd, int y, int yd, Size old) {
		scaleRecord(EMR_SCALEVIEWPORTEXTEX, x, xd, y, yd);
	}

	public void scaleWindowExtEx(int x, int xd, int y, int yd, Size old) {
		scaleRecord(EMR_SCALEWINDOWEXTEX, x, xd, y, yd);
	}

	public void setWorldTransform(float[] xform) {
		byte[] record = record(EMR_SETWORLDTRANSFORM, 24);
		writeXForm(record, 8, xform);
		records.add(record);
	}

	public void modifyWorldTransform(float[] xform, int mode) {
		byte[] record = record(EMR_MODIFYWORLDTRANSFORM, 28);
		writeXForm(record, 8, xform);
		setInt32(record, 32, mode);
		records.add(record);
	}

	public void selectClipRgn(GdiRegion rgn) {
		extSelectClipRgn(rgn, GdiRegion.RGN_COPY);
	}

	public void selectClipPath(int mode) {
		valueRecord(EMR_SELECTCLIPPATH, mode);
	}

	public GdiColorSpace setColorSpace(GdiColorSpace colorSpace) {
		byte[] record = record(EMR_SETCOLORSPACE, 4);
		setInt32(record, 8, objectId(colorSpace));
		records.add(record);
		return colorSpace;
	}

	public void selectObject(GdiObject obj) {
		byte[] record = record(EMR_SELECTOBJECT, 4);
		setInt32(record, 8, objectId(obj));
		records.add(record);
	}

	public void selectPalette(GdiPalette palette, boolean mode) {
		byte[] record = record(EMR_SELECTPALETTE, 4);
		setInt32(record, 8, objectId(palette));
		records.add(record);
	}

	public void setBkColor(int color) {
		valueRecord(EMR_SETBKCOLOR, color);
	}

	public void setBkMode(int mode) {
		valueRecord(EMR_SETBKMODE, mode);
	}

	public void setColorAdjustment(byte[] colorAdjustment) {
		byte[] bytes = copy(colorAdjustment);
		byte[] record = record(EMR_SETCOLORADJUSTMENT, bytes.length);
		setBytes(record, 8, bytes);
		records.add(record);
	}

	public void setArcDirection(int direction) {
		valueRecord(EMR_SETARCDIRECTION, direction);
	}

	public void setBrushOrgEx(int x, int y, Point old) {
		pointRecord(EMR_SETBRUSHORGEX, x, y);
	}

	public void setDIBitsToDevice(int dx, int dy, int dw, int dh, int sx, int sy,
			int startscan, int scanlines, byte[] image, int colorUse) {
		writeSetDIBitsToDevice(image, dx, dy, dw, dh, sx, sy, startscan, scanlines, colorUse);
	}

	public void setLayout(long layout) {
		valueRecord(EMR_SETLAYOUT, (int)layout);
	}

	public void setMapMode(int mode) {
		valueRecord(EMR_SETMAPMODE, mode);
	}

	public void setMapperFlags(long flags) {
		valueRecord(EMR_SETMAPPERFLAGS, (int)flags);
	}

	public int setICMMode(int mode) {
		valueRecord(EMR_SETICMMODE, mode);
		return mode;
	}

	public boolean setICMProfile(byte[] profileName) {
		return setICMProfileRecord(EMR_SETICMPROFILEW, profileName);
	}

	public boolean setICMProfileA(byte[] profileName) {
		return setICMProfileRecord(EMR_SETICMPROFILEA, profileName);
	}

	private boolean setICMProfileRecord(int type, byte[] profileName) {
		byte[] name = copy(profileName);
		byte[] record = record(type, 12 + name.length);
		setInt32(record, 8, 0);
		setInt32(record, 12, name.length);
		setInt32(record, 16, 0);
		setBytes(record, 20, name);
		records.add(record);
		return true;
	}

	public boolean colorMatchToTarget(int action, int flags, byte[] targetProfile) {
		byte[] data = copy(targetProfile);
		byte[] record = record(EMR_COLORMATCHTOTARGETW, 16 + data.length);
		setInt32(record, 8, action);
		setInt32(record, 12, flags);
		setInt32(record, 16, data.length);
		setInt32(record, 20, 0);
		setBytes(record, 24, data);
		records.add(record);
		return true;
	}

	public int setMetaRgn() {
		records.add(record(EMR_SETMETARGN, 0));
		return GdiRegion.SIMPLEREGION;
	}

	public void setMiterLimit(float limit) {
		byte[] record = record(EMR_SETMITERLIMIT, 4);
		setFloat(record, 8, limit);
		records.add(record);
	}

	public void setPaletteEntries(GdiPalette palette, int startIndex, int[] entries) {
		int[] values = entries != null ? entries : new int[0];
		byte[] record = record(EMR_SETPALETTEENTRIES, 12 + values.length * 4);
		setInt32(record, 8, objectId(palette));
		setInt32(record, 12, startIndex);
		setInt32(record, 16, values.length);
		for (int i = 0; i < values.length; i++) {
			setInt32(record, 20 + i * 4, values[i]);
		}
		records.add(record);
	}

	public void setPixel(int x, int y, int color) {
		byte[] record = record(EMR_SETPIXELV, 12);
		setInt32(record, 8, x);
		setInt32(record, 12, y);
		setInt32(record, 16, color);
		records.add(record);
		includePoint(x, y);
	}

	public void setPolyFillMode(int mode) {
		valueRecord(EMR_SETPOLYFILLMODE, mode);
	}

	public void setRelAbs(int mode) {
		throw new UnsupportedOperationException();
	}

	public void setROP2(int mode) {
		valueRecord(EMR_SETROP2, mode);
	}

	public void setStretchBltMode(int mode) {
		valueRecord(EMR_SETSTRETCHBLTMODE, mode);
	}

	public void setTextAlign(int align) {
		valueRecord(EMR_SETTEXTALIGN, align);
	}

	public void setTextCharacterExtra(int extra) {
		throw new UnsupportedOperationException();
	}

	public void setTextColor(int color) {
		valueRecord(EMR_SETTEXTCOLOR, color);
	}

	public void setTextJustification(int breakExtra, int breakCount) {
		byte[] record = record(EMR_SETTEXTJUSTIFICATION, 8);
		setInt32(record, 8, breakExtra);
		setInt32(record, 12, breakCount);
		records.add(record);
	}

	public void setViewportExtEx(int x, int y, Size old) {
		pointRecord(EMR_SETVIEWPORTEXTEX, x, y);
	}

	public void setViewportOrgEx(int x, int y, Point old) {
		pointRecord(EMR_SETVIEWPORTORGEX, x, y);
	}

	public void setWindowExtEx(int width, int height, Size old) {
		pointRecord(EMR_SETWINDOWEXTEX, width, height);
	}

	public void setWindowOrgEx(int x, int y, Point old) {
		pointRecord(EMR_SETWINDOWORGEX, x, y);
	}

	public void stretchBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, long rop) {
		writeStretchBltRecord(image, dx, dy, dw, dh, sx, sy, sw, sh, rop);
	}

	public void stretchDIBits(int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh,
			byte[] image, int usage, long rop) {
		writeBitmapRecord(EMR_STRETCHDIBITS, image, dx, dy, dw, dh, sx, sy, sw, sh, rop, 0, usage);
	}

	public void textOut(int x, int y, byte[] text) {
		extTextOut(x, y, 0, null, text, null);
	}

	public void transparentBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, int transparentColor) {
		writeBlendRecord(EMR_TRANSPARENTBLT, image, dx, dy, dw, dh, sx, sy, sw, sh, transparentColor);
	}

	private void arcRecord(int type, int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		byte[] record = record(type, 32);
		writeRect(record, 8, sxr, syr, exr, eyr);
		setInt32(record, 24, sxa);
		setInt32(record, 28, sya);
		setInt32(record, 32, exa);
		setInt32(record, 36, eya);
		records.add(record);
		includeRect(sxr, syr, exr, eyr);
	}

	private void rectangleRecord(int type, int sx, int sy, int ex, int ey) {
		byte[] record = record(type, 16);
		writeRect(record, 8, sx, sy, ex, ey);
		records.add(record);
		includeRect(sx, sy, ex, ey);
	}

	private void pointRecord(int type, int x, int y) {
		byte[] record = record(type, 8);
		setInt32(record, 8, x);
		setInt32(record, 12, y);
		records.add(record);
		includePoint(x, y);
	}

	private void scaleRecord(int type, int x, int xd, int y, int yd) {
		byte[] record = record(type, 16);
		setInt32(record, 8, x);
		setInt32(record, 12, xd);
		setInt32(record, 16, y);
		setInt32(record, 20, yd);
		records.add(record);
	}

	private void pathPaintRecord(int type) {
		byte[] record = record(type, 16);
		writeBounds(record, 8);
		records.add(record);
	}

	private void valueRecord(int type, int value) {
		byte[] record = record(type, 4);
		setInt32(record, 8, value);
		records.add(record);
	}

	private void writeEscapeRecord(int type, int escapeFunction, byte[] data, int offset, int length) {
		byte[] record = record(type, 4 + length);
		setInt32(record, 8, escapeFunction);
		setBytes(record, 12, data, offset, length);
		records.add(record);
	}

	private void polyPointRecord(int type, Point[] points) {
		Point[] values = points != null ? points : new Point[0];
		byte[] record = record(type, 20 + values.length * 8);
		writePointBounds(record, 8, values);
		setInt32(record, 24, values.length);
		writePoints(record, 28, values);
		records.add(record);
		includePoints(values);
	}

	private void polyPointRecord16(int type, Point[] points) {
		Point[] values = points != null ? points : new Point[0];
		byte[] record = record(type, 20 + values.length * 4);
		writePointBounds(record, 8, values);
		setInt32(record, 24, values.length);
		writePoints16(record, 28, values);
		records.add(record);
		includePoints(values);
	}

	private void polyDrawRecord(int type, Point[] points, byte[] types, boolean point16) {
		Point[] pointValues = points != null ? points : new Point[0];
		byte[] typeValues = types != null ? types : new byte[0];
		int count = Math.min(pointValues.length, typeValues.length);
		Point[] values = new Point[count];
		for (int i = 0; i < count; i++) {
			values[i] = pointValues[i];
		}
		int pointSize = point16 ? 4 : 8;
		byte[] record = record(type, 20 + count * pointSize + count);
		writePointBounds(record, 8, values);
		setInt32(record, 24, count);
		if (point16) {
			writePoints16(record, 28, values);
		} else {
			writePoints(record, 28, values);
		}
		setBytes(record, 28 + count * pointSize, typeValues, 0, count);
		records.add(record);
		includePoints(values);
	}

	private void polyPolyPointRecord(int type, Point[][] points) {
		Point[][] groups = points != null ? points : new Point[0][];
		int pointCount = 0;
		for (int i = 0; i < groups.length; i++) {
			pointCount += groups[i] != null ? groups[i].length : 0;
		}
		byte[] record = record(type, 24 + groups.length * 4 + pointCount * 8);
		writeBounds(record, 8);
		setInt32(record, 24, groups.length);
		setInt32(record, 28, pointCount);
		int pos = 32;
		for (int i = 0; i < groups.length; i++) {
			int count = groups[i] != null ? groups[i].length : 0;
			setInt32(record, pos, count);
			pos += 4;
		}
		for (int i = 0; i < groups.length; i++) {
			Point[] group = groups[i] != null ? groups[i] : new Point[0];
			writePoints(record, pos, group);
			pos += group.length * 8;
			includePoints(group);
		}
		records.add(record);
	}

	private void polyPolyPointRecord16(int type, Point[][] points) {
		Point[][] groups = points != null ? points : new Point[0][];
		int pointCount = 0;
		for (int i = 0; i < groups.length; i++) {
			pointCount += groups[i] != null ? groups[i].length : 0;
		}
		byte[] record = record(type, 24 + groups.length * 4 + pointCount * 4);
		writeBounds(record, 8);
		setInt32(record, 24, groups.length);
		setInt32(record, 28, pointCount);
		int pos = 32;
		for (int i = 0; i < groups.length; i++) {
			int count = groups[i] != null ? groups[i].length : 0;
			setInt32(record, pos, count);
			pos += 4;
		}
		for (int i = 0; i < groups.length; i++) {
			Point[] group = groups[i] != null ? groups[i] : new Point[0];
			writePoints16(record, pos, group);
			pos += group.length * 4;
			includePoints(group);
		}
		records.add(record);
	}

	private void writeGradientFill(Trivertex[] vertex, GradientRect[] rects, GradientTriangle[] triangles, int mode) {
		Trivertex[] vertices = vertex != null ? vertex : new Trivertex[0];
		int meshCount = rects != null ? rects.length : triangles != null ? triangles.length : 0;
		int meshStep = 12;
		byte[] record = record(EMR_GRADIENTFILL, 28 + vertices.length * 16 + meshCount * meshStep);
		writeGradientBounds(record, 8, vertices);
		setInt32(record, 24, vertices.length);
		setInt32(record, 28, meshCount);
		setInt32(record, 32, mode);
		int pos = 36;
		for (int i = 0; i < vertices.length; i++) {
			setInt32(record, pos, vertices[i].x);
			setInt32(record, pos + 4, vertices[i].y);
			setUInt16(record, pos + 8, vertices[i].red);
			setUInt16(record, pos + 10, vertices[i].green);
			setUInt16(record, pos + 12, vertices[i].blue);
			setUInt16(record, pos + 14, vertices[i].alpha);
			includePoint(vertices[i].x, vertices[i].y);
			pos += 16;
		}
		if (rects != null) {
			for (int i = 0; i < rects.length; i++) {
				setInt32(record, pos, rects[i].upperLeft);
				setInt32(record, pos + 4, rects[i].lowerRight);
				setInt32(record, pos + 8, 0);
				pos += meshStep;
			}
		} else if (triangles != null) {
			for (int i = 0; i < triangles.length; i++) {
				setInt32(record, pos, triangles[i].vertex1);
				setInt32(record, pos + 4, triangles[i].vertex2);
				setInt32(record, pos + 8, triangles[i].vertex3);
				pos += 12;
			}
		}
		records.add(record);
	}

	private void writeGradientBounds(byte[] record, int pos, Trivertex[] vertices) {
		if (vertices.length == 0) {
			writeBounds(record, pos);
			return;
		}

		int l = Integer.MAX_VALUE;
		int t = Integer.MAX_VALUE;
		int r = Integer.MIN_VALUE;
		int b = Integer.MIN_VALUE;
		for (int i = 0; i < vertices.length; i++) {
			l = Math.min(l, vertices[i].x);
			t = Math.min(t, vertices[i].y);
			r = Math.max(r, vertices[i].x);
			b = Math.max(b, vertices[i].y);
		}
		writeRect(record, pos, l, t, r, b);
	}

	private void writeSetDIBitsToDevice(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy,
			int startscan, int scanlines, int colorUse) {
		byte[] dib = copy(image);
		int bitsOffset = getDibBitsOffset(dib);
		int bmiSize = bitsOffset > 0 ? bitsOffset : dib.length;
		int bitsSize = Math.max(0, dib.length - bmiSize);
		int dibOffset = 76;
		byte[] record = record(EMR_SETDIBITSTODEVICE, dibOffset - 8 + dib.length);
		writeRect(record, 8, dx, dy, dx + dw, dy + dh);
		setInt32(record, 24, dx);
		setInt32(record, 28, dy);
		setInt32(record, 32, sx);
		setInt32(record, 36, sy);
		setInt32(record, 40, dw);
		setInt32(record, 44, dh);
		setInt32(record, 48, dibOffset);
		setInt32(record, 52, bmiSize);
		setInt32(record, 56, dibOffset + bmiSize);
		setInt32(record, 60, bitsSize);
		setInt32(record, 64, colorUse);
		setInt32(record, 68, startscan);
		setInt32(record, 72, scanlines);
		setBytes(record, dibOffset, dib);
		records.add(record);
		includeRect(dx, dy, dx + dw, dy + dh);
	}

	private void writeBitBltRecord(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop) {
		byte[] dib = copy(image);
		int bitsOffset = getDibBitsOffset(dib);
		int bmiSize = bitsOffset > 0 ? bitsOffset : dib.length;
		int bitsSize = Math.max(0, dib.length - bmiSize);
		int dibOffset = 100;
		byte[] record = record(EMR_BITBLT, dibOffset - 8 + dib.length);
		writeRect(record, 8, dx, dy, dx + dw, dy + dh);
		setInt32(record, 24, dx);
		setInt32(record, 28, dy);
		setInt32(record, 32, dw);
		setInt32(record, 36, dh);
		setInt32(record, 40, (int)rop);
		setInt32(record, 44, sx);
		setInt32(record, 48, sy);
		setIdentityXForm(record, 52);
		setInt32(record, 80, Gdi.DIB_RGB_COLORS);
		setInt32(record, 84, dibOffset);
		setInt32(record, 88, bmiSize);
		setInt32(record, 92, dibOffset + bmiSize);
		setInt32(record, 96, bitsSize);
		setBytes(record, dibOffset, dib);
		records.add(record);
		includeRect(dx, dy, dx + dw, dy + dh);
	}

	private void writeStretchBltRecord(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, long rop) {
		byte[] dib = copy(image);
		int bitsOffset = getDibBitsOffset(dib);
		int bmiSize = bitsOffset > 0 ? bitsOffset : dib.length;
		int bitsSize = Math.max(0, dib.length - bmiSize);
		int dibOffset = 116;
		byte[] record = record(EMR_STRETCHBLT, dibOffset - 8 + dib.length);
		writeRect(record, 8, dx, dy, dx + dw, dy + dh);
		setInt32(record, 24, dx);
		setInt32(record, 28, dy);
		setInt32(record, 32, dw);
		setInt32(record, 36, dh);
		setInt32(record, 40, (int)rop);
		setInt32(record, 44, sx);
		setInt32(record, 48, sy);
		setIdentityXForm(record, 52);
		setInt32(record, 80, Gdi.DIB_RGB_COLORS);
		setInt32(record, 84, dibOffset);
		setInt32(record, 88, bmiSize);
		setInt32(record, 92, dibOffset + bmiSize);
		setInt32(record, 96, bitsSize);
		setInt32(record, 100, sw);
		setInt32(record, 104, sh);
		setBytes(record, dibOffset, dib);
		records.add(record);
		includeRect(dx, dy, dx + dw, dy + dh);
	}

	private void writeMaskBltRecord(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, byte[] mask, int mx, int my, long rop) {
		byte[] dib = copy(image);
		byte[] maskDib = mask != null ? copy(mask) : new byte[0];
		int bitsOffset = getDibBitsOffset(dib);
		int bmiSize = bitsOffset > 0 ? bitsOffset : dib.length;
		int bitsSize = Math.max(0, dib.length - bmiSize);
		int maskBitsOffset = getDibBitsOffset(maskDib);
		int maskBmiSize = maskBitsOffset > 0 ? maskBitsOffset : maskDib.length;
		int maskBitsSize = Math.max(0, maskDib.length - maskBmiSize);
		int sourceOffset = 128;
		int maskOffset = align4(sourceOffset + dib.length);
		byte[] record = record(EMR_MASKBLT, maskOffset - 8 + maskDib.length);
		writeRect(record, 8, dx, dy, dx + dw, dy + dh);
		setInt32(record, 24, dx);
		setInt32(record, 28, dy);
		setInt32(record, 32, dw);
		setInt32(record, 36, dh);
		setInt32(record, 40, (int)rop);
		setInt32(record, 44, sx);
		setInt32(record, 48, sy);
		setIdentityXForm(record, 52);
		setInt32(record, 80, Gdi.DIB_RGB_COLORS);
		setInt32(record, 84, sourceOffset);
		setInt32(record, 88, bmiSize);
		setInt32(record, 92, sourceOffset + bmiSize);
		setInt32(record, 96, bitsSize);
		setInt32(record, 100, mx);
		setInt32(record, 104, my);
		if (maskDib.length > 0) {
			setInt32(record, 112, maskOffset);
			setInt32(record, 116, maskBmiSize);
			setInt32(record, 120, maskOffset + maskBmiSize);
			setInt32(record, 124, maskBitsSize);
			setBytes(record, maskOffset, maskDib);
		}
		setBytes(record, sourceOffset, dib);
		records.add(record);
		includeRect(dx, dy, dx + dw, dy + dh);
	}

	private void writePlgBltRecord(byte[] image, Point[] points, int sx, int sy, int sw, int sh,
			byte[] mask, int mx, int my) {
		if (points == null || points.length < 3) {
			throw new IllegalArgumentException("PLGBLT requires three destination points.");
		}
		byte[] dib = copy(image);
		byte[] maskDib = mask != null ? copy(mask) : new byte[0];
		int bitsOffset = getDibBitsOffset(dib);
		int bmiSize = bitsOffset > 0 ? bitsOffset : dib.length;
		int bitsSize = Math.max(0, dib.length - bmiSize);
		int maskBitsOffset = getDibBitsOffset(maskDib);
		int maskBmiSize = maskBitsOffset > 0 ? maskBitsOffset : maskDib.length;
		int maskBitsSize = Math.max(0, maskDib.length - maskBmiSize);
		int sourceOffset = 140;
		int maskOffset = align4(sourceOffset + dib.length);
		byte[] record = record(EMR_PLGBLT, maskOffset - 8 + maskDib.length);
		writePointBounds(record, 8, points);
		writePoints(record, 24, points);
		setInt32(record, 48, sx);
		setInt32(record, 52, sy);
		setInt32(record, 56, sw);
		setInt32(record, 60, sh);
		setIdentityXForm(record, 64);
		setInt32(record, 92, Gdi.DIB_RGB_COLORS);
		setInt32(record, 96, sourceOffset);
		setInt32(record, 100, bmiSize);
		setInt32(record, 104, sourceOffset + bmiSize);
		setInt32(record, 108, bitsSize);
		setInt32(record, 112, mx);
		setInt32(record, 116, my);
		if (maskDib.length > 0) {
			setInt32(record, 124, maskOffset);
			setInt32(record, 128, maskBmiSize);
			setInt32(record, 132, maskOffset + maskBmiSize);
			setInt32(record, 136, maskBitsSize);
			setBytes(record, maskOffset, maskDib);
		}
		setBytes(record, sourceOffset, dib);
		records.add(record);
		includePoints(points);
	}

	private void writeBlendRecord(int type, byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, int blendOrTransparentColor) {
		byte[] dib = copy(image);
		int bitsOffset = getDibBitsOffset(dib);
		int bmiSize = bitsOffset > 0 ? bitsOffset : dib.length;
		int bitsSize = Math.max(0, dib.length - bmiSize);
		byte[] record = record(type, 100 + dib.length);
		writeRect(record, 8, dx, dy, dx + dw, dy + dh);
		setInt32(record, 24, dx);
		setInt32(record, 28, dy);
		setInt32(record, 32, dw);
		setInt32(record, 36, dh);
		setInt32(record, 40, blendOrTransparentColor);
		setInt32(record, 44, sx);
		setInt32(record, 48, sy);
		setIdentityXForm(record, 52);
		setInt32(record, 80, Gdi.DIB_RGB_COLORS);
		setInt32(record, 84, 108);
		setInt32(record, 88, bmiSize);
		setInt32(record, 92, 108 + bmiSize);
		setInt32(record, 96, bitsSize);
		setInt32(record, 100, sw);
		setInt32(record, 104, sh);
		setBytes(record, 108, dib);
		records.add(record);
		includeRect(dx, dy, dx + dw, dy + dh);
	}

	private void writeBitmapRecord(int type, byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, long rop, int blendOrUsage, int usageOrTransparent) {
		byte[] dib = copy(image);
		int bitsOffset = getDibBitsOffset(dib);
		int bmiSize = bitsOffset > 0 ? bitsOffset : dib.length;
		int bitsSize = Math.max(0, dib.length - bmiSize);
		int dibOffset = type == EMR_STRETCHDIBITS ? 88 : 80;
		byte[] record = record(type, dibOffset - 8 + dib.length);
		writeRect(record, 8, dx, dy, dx + dw, dy + dh);
		setInt32(record, 24, dx);
		setInt32(record, 28, dy);
		setInt32(record, 32, sx);
		setInt32(record, 36, sy);
		setInt32(record, 40, sw);
		setInt32(record, 44, sh);
		setInt32(record, 48, dibOffset);
		setInt32(record, 52, bmiSize);
		setInt32(record, 56, dibOffset + bmiSize);
		setInt32(record, 60, bitsSize);
		setInt32(record, 64, usageOrTransparent);
		setInt32(record, 68, (int)rop);
		setInt32(record, 72, dw);
		setInt32(record, 76, dh);
		setBytes(record, dibOffset, dib);
		records.add(record);
		includeRect(dx, dy, dx + dw, dy + dh);
	}

	private void writeRegionRecord(int type, GdiRegion rgn) {
		byte[] data = regionData(rgn);
		byte[] record = record(type, 16 + 4 + data.length);
		writeRegionBounds(record, 8, data);
		setInt32(record, 24, data.length);
		setBytes(record, 28, data);
		records.add(record);
		includeRegion(data);
	}

	private byte[] regionData(GdiRegion rgn) {
		if (rgn instanceof EmfRegion) {
			byte[] data = ((EmfRegion)rgn).data;
			if (data != null) {
				return data;
			}
		}
		throw new IllegalArgumentException("Unknown GDI region: " + rgn);
	}

	private byte[] createRectRegionData(int left, int top, int right, int bottom) {
		byte[] data = new byte[48];
		setInt32(data, 0, 32);
		setInt32(data, 4, 1);
		setInt32(data, 8, 1);
		setInt32(data, 12, 16);
		writeRect(data, 16, left, top, right, bottom);
		writeRect(data, 32, left, top, right, bottom);
		return data;
	}

	private void writeRegionBounds(byte[] record, int pos, byte[] data) {
		if (data.length >= 32) {
			writeRect(record, pos, readInt32(data, 16), readInt32(data, 20), readInt32(data, 24), readInt32(data, 28));
		}
	}

	private void includeRegion(byte[] data) {
		if (data.length >= 32) {
			includeRect(readInt32(data, 16), readInt32(data, 20), readInt32(data, 24), readInt32(data, 28));
		}
	}

	private void setIdentityXForm(byte[] record, int pos) {
		setFloat(record, pos, 1);
		setFloat(record, pos + 4, 0);
		setFloat(record, pos + 8, 0);
		setFloat(record, pos + 12, 1);
		setFloat(record, pos + 16, 0);
		setFloat(record, pos + 20, 0);
	}

	private void writeXForm(byte[] record, int pos, float[] xform) {
		float[] values = new float[] { 1, 0, 0, 1, 0, 0 };
		if (xform != null) {
			int length = Math.min(values.length, xform.length);
			for (int i = 0; i < length; i++) {
				values[i] = xform[i];
			}
		}
		for (int i = 0; i < values.length; i++) {
			setFloat(record, pos + i * 4, values[i]);
		}
	}

	private byte[] createHeaderRecord() {
		byte[] record = new byte[88];
		setInt32(record, 0, EMR_HEADER);
		setInt32(record, 4, record.length);
		writeRect(record, 8, getLeft(), getTop(), getRight(), getBottom());
		writeRect(record, 24, frameLeft, frameTop, frameRight, frameBottom);
		setInt32(record, 40, 0x464D4520);
		setInt32(record, 44, 0x00010000);
		setInt32(record, 48, getTotalBytes());
		setInt32(record, 52, records.size() + 2);
		setUInt16(record, 56, nextHandle);
		setUInt16(record, 58, 0);
		setInt32(record, 60, 0);
		setInt32(record, 64, 0);
		setInt32(record, 68, 0);
		setInt32(record, 72, 1024);
		setInt32(record, 76, 768);
		setInt32(record, 80, 270);
		setInt32(record, 84, 203);
		return record;
	}

	private byte[] createEofRecord() {
		byte[] record = record(EMR_EOF, 12);
		setInt32(record, 8, 0);
		setInt32(record, 12, 0);
		setInt32(record, 16, 20);
		return record;
	}

	private int getTotalBytes() {
		int size = 88 + 20;
		for (int i = 0; i < records.size(); i++) {
			size += records.get(i).length;
		}
		return size;
	}

	private byte[] record(int type, int payloadSize) {
		int size = align4(8 + payloadSize);
		byte[] record = new byte[size];
		setInt32(record, 0, type);
		setInt32(record, 4, size);
		return record;
	}

	private int allocateHandle() {
		return nextHandle++;
	}

	private int objectId(GdiObject obj) {
		if (obj instanceof EmfObject) {
			return ((EmfObject)obj).id;
		}
		throw new IllegalArgumentException("Unknown GDI object: " + obj);
	}

	private void writeLogFont(byte[] record, int pos, EmfFont font) {
		setInt32(record, pos, font.height);
		setInt32(record, pos + 4, font.width);
		setInt32(record, pos + 8, font.escapement);
		setInt32(record, pos + 12, font.orientation);
		setInt32(record, pos + 16, font.weight);
		record[pos + 20] = (byte)(font.italic ? 1 : 0);
		record[pos + 21] = (byte)(font.underline ? 1 : 0);
		record[pos + 22] = (byte)(font.strikeout ? 1 : 0);
		record[pos + 23] = (byte)font.charset;
		record[pos + 24] = (byte)font.outPrecision;
		record[pos + 25] = (byte)font.clipPrecision;
		record[pos + 26] = (byte)font.quality;
		record[pos + 27] = (byte)font.pitchAndFamily;
		byte[] face;
		try {
			face = font.faceName.getBytes("UTF-16LE");
		} catch (UnsupportedEncodingException e) {
			face = new byte[0];
		}
		int length = Math.min(face.length, 62);
		setBytes(record, pos + 28, face, 0, length);
	}

	private void writeRect(byte[] record, int pos, int left, int top, int right, int bottom) {
		setInt32(record, pos, left);
		setInt32(record, pos + 4, top);
		setInt32(record, pos + 8, right);
		setInt32(record, pos + 12, bottom);
	}

	private void writeRect(byte[] record, int pos, int[] rect) {
		if (rect != null && rect.length >= 4) {
			writeRect(record, pos, rect[0], rect[1], rect[2], rect[3]);
		}
	}

	private static Point getPoint(Point[] points, int index) {
		if (points != null && index < points.length && points[index] != null) {
			return points[index];
		}
		return new Point(0, 0);
	}

	private static int getIntValue(int[] values, int index) {
		if (values != null && index < values.length) {
			return values[index];
		}
		return 0;
	}

	private static int[] getRect(int[][] rects, int index) {
		if (rects != null && index < rects.length) {
			return rects[index];
		}
		return null;
	}

	private static int[] getIntArray(int[][] values, int index) {
		if (values != null && index < values.length) {
			return values[index];
		}
		return null;
	}

	private void writeBounds(byte[] record, int pos) {
		writeRect(record, pos, getLeft(), getTop(), getRight(), getBottom());
	}

	private void writePointBounds(byte[] record, int pos, Point[] points) {
		if (points == null || points.length == 0) {
			writeBounds(record, pos);
			return;
		}
		int l = Integer.MAX_VALUE;
		int t = Integer.MAX_VALUE;
		int r = Integer.MIN_VALUE;
		int b = Integer.MIN_VALUE;
		for (int i = 0; i < points.length; i++) {
			l = Math.min(l, points[i].x);
			t = Math.min(t, points[i].y);
			r = Math.max(r, points[i].x);
			b = Math.max(b, points[i].y);
		}
		writeRect(record, pos, l, t, r, b);
	}

	private void writePoints(byte[] record, int pos, Point[] points) {
		for (int i = 0; i < points.length; i++) {
			setInt32(record, pos + i * 8, points[i].x);
			setInt32(record, pos + i * 8 + 4, points[i].y);
		}
	}

	private void writePoints16(byte[] record, int pos, Point[] points) {
		for (int i = 0; i < points.length; i++) {
			setInt16(record, pos + i * 4, points[i].x);
			setInt16(record, pos + i * 4 + 2, points[i].y);
		}
	}

	private void includePoints(Point[] points) {
		for (int i = 0; i < points.length; i++) {
			includePoint(points[i].x, points[i].y);
		}
	}

	private void includePoint(int x, int y) {
		includeRect(x, y, x, y);
	}

	private void includeRect(int x1, int y1, int x2, int y2) {
		left = Math.min(left, Math.min(x1, x2));
		top = Math.min(top, Math.min(y1, y2));
		right = Math.max(right, Math.max(x1, x2));
		bottom = Math.max(bottom, Math.max(y1, y2));
	}

	private int getLeft() {
		return left != Integer.MAX_VALUE ? left : 0;
	}

	private int getTop() {
		return top != Integer.MAX_VALUE ? top : 0;
	}

	private int getRight() {
		return right != Integer.MIN_VALUE ? right : 0;
	}

	private int getBottom() {
		return bottom != Integer.MIN_VALUE ? bottom : 0;
	}

	private static int getDibBitsOffset(byte[] dib) {
		return getDibBitsOffset(dib, Gdi.DIB_RGB_COLORS);
	}

	private static int getDibBitsOffset(byte[] dib, int usage) {
		if (dib.length < 40) {
			return dib.length;
		}
		int headerSize = readInt32(dib, 0);
		if (headerSize <= 0 || headerSize > dib.length) {
			return dib.length;
		}
		int bitCount = dib.length >= 16 ? readUInt16(dib, 14) : 0;
		int colorsUsed = dib.length >= 36 ? readInt32(dib, 32) : 0;
		int colors = colorsUsed;
		if (colors == 0 && bitCount > 0 && bitCount <= 8) {
			colors = 1 << bitCount;
		}
		int colorEntrySize = usage == Gdi.DIB_PAL_COLORS ? 2 : 4;
		int offset = headerSize + colors * colorEntrySize;
		return offset <= dib.length ? offset : dib.length;
	}

	private static int align4(int value) {
		return (value + 3) & ~3;
	}

	private static byte[] copy(byte[] src) {
		if (src == null) {
			return new byte[0];
		}
		byte[] dest = new byte[src.length];
		System.arraycopy(src, 0, dest, 0, src.length);
		return dest;
	}

	private static byte[] unicodeNullTerminated(byte[] src) {
		byte[] bytes = copy(src);
		boolean terminated = bytes.length >= 2
				&& bytes[bytes.length - 2] == 0
				&& bytes[bytes.length - 1] == 0;
		int length = bytes.length;
		if ((length & 1) != 0) {
			length++;
		}
		if (!terminated) {
			length += 2;
		}
		if (length == bytes.length) {
			return bytes;
		}
		byte[] dest = new byte[length];
		System.arraycopy(bytes, 0, dest, 0, bytes.length);
		return dest;
	}

	private static int readUInt16(byte[] data, int pos) {
		return (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8);
	}

	private static int readInt32(byte[] data, int pos) {
		return (data[pos] & 0xFF)
				| ((data[pos + 1] & 0xFF) << 8)
				| ((data[pos + 2] & 0xFF) << 16)
				| (data[pos + 3] << 24);
	}

	private static void setBytes(byte[] record, int pos, byte[] src) {
		setBytes(record, pos, src, 0, src.length);
	}

	private static void setBytes(byte[] record, int pos, byte[] src, int offset, int length) {
		System.arraycopy(src, offset, record, pos, length);
	}

	private static void setUInt16(byte[] record, int pos, int value) {
		record[pos] = (byte)(value & 0xFF);
		record[pos + 1] = (byte)((value >>> 8) & 0xFF);
	}

	private static void setInt16(byte[] record, int pos, int value) {
		record[pos] = (byte)(value & 0xFF);
		record[pos + 1] = (byte)((value >>> 8) & 0xFF);
	}

	private static void setInt32(byte[] record, int pos, int value) {
		record[pos] = (byte)(value & 0xFF);
		record[pos + 1] = (byte)((value >>> 8) & 0xFF);
		record[pos + 2] = (byte)((value >>> 16) & 0xFF);
		record[pos + 3] = (byte)((value >>> 24) & 0xFF);
	}

	private static void setFloat(byte[] record, int pos, float value) {
		setInt32(record, pos, Float.floatToIntBits(value));
	}
}

package net.arnx.wmf2svg.gdi.wmf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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
import net.arnx.wmf2svg.gdi.GdiUtils;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Size;
import net.arnx.wmf2svg.gdi.GradientRect;
import net.arnx.wmf2svg.gdi.GradientTriangle;
import net.arnx.wmf2svg.gdi.Trivertex;

public class WmfGdi implements Gdi, WmfConstants {
	private byte[] placeableHeader;
	private byte[] header;

	private List<GdiObject> objects = new ArrayList<GdiObject>();
	private List<byte[]> records = new ArrayList<byte[]>();

	private WmfDc dc = new WmfDc();
	private LinkedList<WmfDc> saveDC = new LinkedList<WmfDc>();

	private WmfBrush defaultBrush;

	private WmfPen defaultPen;

	private WmfFont defaultFont;

	public WmfGdi() {
		defaultBrush = (WmfBrush) createBrushIndirect(GdiBrush.BS_SOLID, 0x00FFFFFF, 0);
		defaultPen = (WmfPen) createPenIndirect(GdiPen.PS_SOLID, 1, 0x00000000);
		defaultFont = null;

		dc.setBrush(defaultBrush);
		dc.setPen(defaultPen);
		dc.setFont(defaultFont);
	}

	public void write(OutputStream out) throws IOException {
		footer();
		if (placeableHeader != null)
			out.write(placeableHeader);
		if (header != null)
			out.write(header);

		Iterator<?> i = records.iterator();
		while (i.hasNext()) {
			out.write((byte[]) i.next());
		}
		out.flush();
	}

	private int nextObjectID() {
		for (int i = 0; i < objects.size(); i++) {
			if (objects.get(i) == null) {
				return i;
			}
		}
		return objects.size();
	}

	private void setObject(WmfObject object) {
		int id = object.getID();
		while (objects.size() <= id) {
			objects.add(null);
		}
		objects.set(id, object);
	}

	public void placeableHeader(int vsx, int vsy, int vex, int vey, int dpi) {
		byte[] record = new byte[22];
		int pos = 0;
		pos = setUint32(record, pos, 0x9AC6CDD7);
		pos = setInt16(record, pos, 0x0000);
		pos = setInt16(record, pos, vsx);
		pos = setInt16(record, pos, vsy);
		pos = setInt16(record, pos, vex);
		pos = setInt16(record, pos, vey);
		pos = setUint16(record, pos, dpi);
		pos = setUint32(record, pos, 0x00000000);

		int checksum = 0;
		for (int i = 0; i < record.length - 2; i += 2) {
			checksum ^= (0xFF & record[i]) | ((0xFF & record[i + 1]) << 8);
		}

		pos = setUint16(record, pos, checksum);
		placeableHeader = record;
	}

	public void header() {
		byte[] record = new byte[18];
		int pos = 0;
		pos = setUint16(record, pos, 0x0001);
		pos = setUint16(record, pos, 0x0009);
		pos = setUint16(record, pos, 0x0300);
		pos = setUint32(record, pos, 0x0000); // dummy size
		pos = setUint16(record, pos, 0x0000); // dummy noObjects
		pos = setUint32(record, pos, 0x0000); // dummy maxRecords
		pos = setUint16(record, pos, 0x0000);
		header = record;
	}

	public void animatePalette(GdiPalette palette, int startIndex, int[] entries) {
		byte[] record = new byte[12 + entries.length * 4];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_ANIMATEPALETTE);
		pos = setUint16(record, pos, entries.length);
		pos = setUint16(record, pos, startIndex);
		pos = setUint16(record, pos, ((WmfPalette) palette).getID());
		for (int i = 0; i < entries.length; i++) {
			pos = setInt32(record, pos, entries[i]);
		}
		records.add(record);
	}

	public void alphaBlend(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh,
			int blendFunction) {
		throw new UnsupportedOperationException();
	}

	public void angleArc(int x, int y, int radius, float startAngle, float sweepAngle) {
		throw new UnsupportedOperationException();
	}

	public void arc(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		byte[] record = new byte[22];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_ARC);
		pos = setInt16(record, pos, eya);
		pos = setInt16(record, pos, exa);
		pos = setInt16(record, pos, sya);
		pos = setInt16(record, pos, sxa);
		pos = setInt16(record, pos, eyr);
		pos = setInt16(record, pos, exr);
		pos = setInt16(record, pos, syr);
		pos = setInt16(record, pos, sxr);
		records.add(record);
	}

	public void arcTo(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		throw new UnsupportedOperationException();
	}

	public void abortPath() {
		throw new UnsupportedOperationException();
	}

	public void beginPath() {
		throw new UnsupportedOperationException();
	}

	public void bitBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop) {
		byte[] record = new byte[22 + (image.length + image.length % 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_BITBLT);
		pos = setUint32(record, pos, rop);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length % 2 == 1)
			pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void maskBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, byte[] mask, int mx, int my,
			long rop) {
		throw new UnsupportedOperationException();
	}

	public void plgBlt(byte[] image, Point[] points, int sx, int sy, int sw, int sh, byte[] mask, int mx, int my) {
		throw new UnsupportedOperationException();
	}

	public void chord(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		byte[] record = new byte[22];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_CHORD);
		pos = setInt16(record, pos, eya);
		pos = setInt16(record, pos, exa);
		pos = setInt16(record, pos, sya);
		pos = setInt16(record, pos, sxa);
		pos = setInt16(record, pos, eyr);
		pos = setInt16(record, pos, exr);
		pos = setInt16(record, pos, syr);
		pos = setInt16(record, pos, sxr);
		records.add(record);
	}

	public void closeFigure() {
		throw new UnsupportedOperationException();
	}

	public void colorCorrectPalette(GdiPalette palette, int startIndex, int entries) {
		throw new UnsupportedOperationException();
	}

	public GdiBrush createBrushIndirect(int style, int color, int hatch) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_CREATEBRUSHINDIRECT);
		pos = setUint16(record, pos, style);
		pos = setInt32(record, pos, color);
		pos = setUint16(record, pos, hatch);
		records.add(record);

		WmfBrush brush = new WmfBrush(nextObjectID(), style, color, hatch);
		setObject(brush);
		return brush;
	}

	public GdiColorSpace createColorSpace(byte[] logColorSpace) {
		throw new UnsupportedOperationException();
	}

	public GdiColorSpace createColorSpaceW(byte[] logColorSpace) {
		throw new UnsupportedOperationException();
	}

	public GdiFont createFontIndirect(int height, int width, int escapement, int orientation, int weight,
			boolean italic, boolean underline, boolean strikeout, int charset, int outPrecision, int clipPrecision,
			int quality, int pitchAndFamily, byte[] faceName) {

		byte[] record = new byte[24 + (faceName.length + faceName.length % 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_CREATEFONTINDIRECT);
		pos = setInt16(record, pos, height);
		pos = setInt16(record, pos, width);
		pos = setInt16(record, pos, escapement);
		pos = setInt16(record, pos, orientation);
		pos = setInt16(record, pos, weight);
		pos = setByte(record, pos, (italic) ? 0x01 : 0x00);
		pos = setByte(record, pos, (underline) ? 0x01 : 0x00);
		pos = setByte(record, pos, (strikeout) ? 0x01 : 0x00);
		pos = setByte(record, pos, charset);
		pos = setByte(record, pos, outPrecision);
		pos = setByte(record, pos, clipPrecision);
		pos = setByte(record, pos, quality);
		pos = setByte(record, pos, pitchAndFamily);
		pos = setBytes(record, pos, faceName);
		if (faceName.length % 2 == 1)
			pos = setByte(record, pos, 0);
		records.add(record);

		WmfFont font = new WmfFont(nextObjectID(), height, width, escapement, orientation, weight, italic, underline,
				strikeout, charset, outPrecision, clipPrecision, quality, pitchAndFamily, faceName);
		setObject(font);
		return font;
	}

	public GdiPalette createPalette(int version, int[] entries) {
		byte[] record = new byte[10 + entries.length * 4];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_CREATEPALETTE);
		pos = setUint16(record, pos, version);
		pos = setUint16(record, pos, entries.length);
		for (int i = 0; i < entries.length; i++) {
			pos = setInt32(record, pos, entries[i]);
		}
		records.add(record);

		GdiPalette palette = new WmfPalette(nextObjectID(), version, entries);
		setObject((WmfObject) palette);
		return palette;
	}

	public GdiPatternBrush createPatternBrush(byte[] image) {
		byte[] record = new byte[6 + (image.length + image.length % 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_CREATEPATTERNBRUSH);
		pos = setBytes(record, pos, image);
		if (image.length % 2 == 1)
			pos = setByte(record, pos, 0);
		records.add(record);

		GdiPatternBrush brush = new WmfPatternBrush(nextObjectID(), image);
		setObject((WmfObject) brush);
		return brush;
	}

	public GdiPen createPenIndirect(int style, int width, int color) {
		byte[] record = new byte[16];
		int wmfStyle = style & ~GdiPen.PS_DEVICE_WIDTH;
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_CREATEPENINDIRECT);
		pos = setUint16(record, pos, wmfStyle);
		pos = setInt16(record, pos, width);
		pos = setInt16(record, pos, 0);
		pos = setInt32(record, pos, color);
		records.add(record);

		WmfPen pen = new WmfPen(nextObjectID(), style, width, color);
		setObject(pen);
		return pen;
	}

	public GdiRegion createRectRgn(int left, int top, int right, int bottom) {
		byte[] record = new byte[28];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_CREATEREGION);
		pos = setUint16(record, pos, 0);
		pos = setUint16(record, pos, 0x0006);
		pos = setUint32(record, pos, 0);
		pos = setUint16(record, pos, 0);
		pos = setUint16(record, pos, 0);
		pos = setUint16(record, pos, 0);
		pos = setInt16(record, pos, left);
		pos = setInt16(record, pos, top);
		pos = setInt16(record, pos, right);
		pos = setInt16(record, pos, bottom);
		records.add(record);

		WmfRectRegion rgn = new WmfRectRegion(nextObjectID(), left, top, right, bottom);
		setObject(rgn);
		return rgn;
	}

	public GdiRegion extCreateRegion(float[] xform, int count, byte[] rgnData) {
		throw new UnsupportedOperationException();
	}

	public int extSelectClipRgn(GdiRegion rgn, int mode) {
		if (mode == GdiRegion.RGN_COPY) {
			selectClipRgn(rgn);
			return (rgn != null) ? GdiRegion.SIMPLEREGION : GdiRegion.NULLREGION;
		}
		throw new UnsupportedOperationException();
	}

	public void deleteObject(GdiObject obj) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_DELETEOBJECT);
		pos = setUint16(record, pos, ((WmfObject) obj).getID());
		records.add(record);

		objects.set(((WmfObject) obj).getID(), null);

		if (dc.getBrush() == obj) {
			dc.setBrush(defaultBrush);
		} else if (dc.getFont() == obj) {
			dc.setFont(defaultFont);
		} else if (dc.getPen() == obj) {
			dc.setPen(defaultPen);
		}
	}

	public boolean deleteColorSpace(GdiColorSpace colorSpace) {
		throw new UnsupportedOperationException();
	}

	public void dibBitBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop) {
		int imageLength = (image != null) ? image.length : 0;
		byte[] record = new byte[((image != null) ? 22 : 24) + (imageLength + imageLength % 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_DIBBITBLT);
		pos = setUint32(record, pos, rop);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		if (image == null) {
			pos = setInt16(record, pos, 0);
		}
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		if (image != null) {
			pos = setBytes(record, pos, image);
			if (image.length % 2 == 1)
				pos = setByte(record, pos, 0);
		}
		records.add(record);
	}

	public GdiPatternBrush dibCreatePatternBrush(byte[] image, int usage) {
		byte[] record = new byte[10 + (image.length + image.length % 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_DIBCREATEPATTERNBRUSH);
		pos = setInt32(record, pos, usage);
		pos = setBytes(record, pos, image);
		if (image.length % 2 == 1)
			pos = setByte(record, pos, 0);
		records.add(record);

		// TODO usage
		GdiPatternBrush brush = new WmfPatternBrush(nextObjectID(), image);
		setObject((WmfObject) brush);
		return brush;
	}

	public void dibStretchBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, long rop) {
		byte[] record = new byte[26 + (image.length + image.length % 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_DIBSTRETCHBLT);
		pos = setUint32(record, pos, rop);
		pos = setInt16(record, pos, sh);
		pos = setInt16(record, pos, sw);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length % 2 == 1)
			pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void ellipse(int sx, int sy, int ex, int ey) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_ELLIPSE);
		pos = setInt16(record, pos, ey);
		pos = setInt16(record, pos, ex);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		records.add(record);
	}

	public void endPath() {
		throw new UnsupportedOperationException();
	}

	public void escape(byte[] data) {
		byte[] record = new byte[10 + (data.length + data.length % 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_ESCAPE);
		pos = setBytes(record, pos, data);
		if (data.length % 2 == 1)
			pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void comment(byte[] data) {
		escape(data);
	}

	public int excludeClipRect(int left, int top, int right, int bottom) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_EXCLUDECLIPRECT);
		pos = setInt16(record, pos, bottom);
		pos = setInt16(record, pos, right);
		pos = setInt16(record, pos, top);
		pos = setInt16(record, pos, left);
		records.add(record);

		// TODO
		return GdiRegion.COMPLEXREGION;
	}

	public void extFloodFill(int x, int y, int color, int type) {
		byte[] record = new byte[16];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_EXTFLOODFILL);
		pos = setUint16(record, pos, type);
		pos = setInt32(record, pos, color);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void extTextOut(int x, int y, int options, int[] rect, byte[] text, int[] dx) {
		if (rect != null && rect.length != 4) {
			throw new IllegalArgumentException("rect must be 4 length.");
		}
		int dxLength = (dx != null) ? dx.length : 0;
		byte[] record = new byte[14 + ((rect != null) ? 8 : 0) + (text.length + text.length % 2) + (dxLength * 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_EXTTEXTOUT);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		pos = setInt16(record, pos, text.length);
		pos = setInt16(record, pos, options);
		if (rect != null) {
			pos = setInt16(record, pos, rect[0]);
			pos = setInt16(record, pos, rect[1]);
			pos = setInt16(record, pos, rect[2]);
			pos = setInt16(record, pos, rect[3]);
		}
		pos = setBytes(record, pos, text);
		if (text.length % 2 == 1)
			pos = setByte(record, pos, 0);
		for (int i = 0; i < dxLength; i++) {
			pos = setInt16(record, pos, dx[i]);
		}
		records.add(record);

		boolean vertical = false;
		if (dc.getFont() != null) {
			if (dc.getFont().getFaceName().startsWith("@")) {
				vertical = true;
			}
		}

		int align = dc.getTextAlign();
		int width = 0;
		if (!vertical) {
			if (dc.getFont() != null) {
				dx = GdiUtils.fixTextDx(dc.getFont().getCharset(), text, dx);
			}

			if (dx != null && dx.length > 0) {
				for (int i = 0; i < dx.length; i++) {
					width += dx[i];
				}

				int tx = x;

				if ((align & (TA_LEFT | TA_CENTER | TA_RIGHT)) == TA_RIGHT) {
					tx -= (width - dx[dx.length - 1]);
				} else if ((align & (TA_LEFT | TA_CENTER | TA_RIGHT)) == TA_CENTER) {
					tx -= (width - dx[dx.length - 1]) / 2;
				}

				for (int i = 0; i < dx.length; i++) {
					tx += dx[i];
				}
				if ((align & (TA_NOUPDATECP | TA_UPDATECP)) == TA_UPDATECP) {
					dc.moveToEx(tx, y, null);
				}
			}
		}
	}

	public void fillRgn(GdiRegion rgn, GdiBrush brush) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_FILLREGION);
		pos = setUint16(record, pos, ((WmfRegion) rgn).getID());
		pos = setUint16(record, pos, ((WmfBrush) brush).getID());
		records.add(record);
	}

	public void floodFill(int x, int y, int color) {
		byte[] record = new byte[16];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_FLOODFILL);
		pos = setInt32(record, pos, color);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void gradientFill(Trivertex[] vertex, GradientRect[] mesh, int mode) {
		throw new UnsupportedOperationException();
	}

	public void gradientFill(Trivertex[] vertex, GradientTriangle[] mesh, int mode) {
		throw new UnsupportedOperationException();
	}

	public void frameRgn(GdiRegion rgn, GdiBrush brush, int w, int h) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_FRAMEREGION);
		pos = setUint16(record, pos, ((WmfRegion) rgn).getID());
		pos = setUint16(record, pos, ((WmfBrush) brush).getID());
		pos = setInt16(record, pos, h);
		pos = setInt16(record, pos, w);
		records.add(record);
	}

	public void intersectClipRect(int left, int top, int right, int bottom) {
		byte[] record = new byte[16];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_INTERSECTCLIPRECT);
		pos = setInt16(record, pos, bottom);
		pos = setInt16(record, pos, right);
		pos = setInt16(record, pos, top);
		pos = setInt16(record, pos, left);
		records.add(record);
	}

	public void invertRgn(GdiRegion rgn) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_INVERTREGION);
		pos = setUint16(record, pos, ((WmfRegion) rgn).getID());
		records.add(record);
	}

	public void lineTo(int ex, int ey) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_LINETO);
		pos = setInt16(record, pos, ey);
		pos = setInt16(record, pos, ex);
		records.add(record);

		dc.moveToEx(ex, ey, null);
	}

	public void moveToEx(int x, int y, Point old) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_MOVETO);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);

		dc.moveToEx(x, y, old);
	}

	public void offsetClipRgn(int x, int y) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_OFFSETCLIPRGN);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void offsetViewportOrgEx(int x, int y, Point point) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_OFFSETVIEWPORTORG);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);

		dc.offsetViewportOrgEx(x, y, point);
	}

	public void offsetWindowOrgEx(int x, int y, Point point) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_OFFSETWINDOWORG);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);

		dc.offsetWindowOrgEx(x, y, point);
	}

	public void paintRgn(GdiRegion rgn) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_PAINTREGION);
		pos = setUint16(record, pos, ((WmfRegion) rgn).getID());
		records.add(record);
	}

	public void patBlt(int x, int y, int width, int height, long rop) {
		byte[] record = new byte[18];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_PATBLT);
		pos = setUint32(record, pos, rop);
		pos = setInt16(record, pos, height);
		pos = setInt16(record, pos, width);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void pie(int sx, int sy, int ex, int ey, int sxr, int syr, int exr, int eyr) {
		byte[] record = new byte[22];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_PIE);
		pos = setInt16(record, pos, eyr);
		pos = setInt16(record, pos, exr);
		pos = setInt16(record, pos, syr);
		pos = setInt16(record, pos, sxr);
		pos = setInt16(record, pos, ey);
		pos = setInt16(record, pos, ex);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		records.add(record);
	}

	public void polyBezier(Point[] points) {
		throw new UnsupportedOperationException();
	}

	public void polyBezierTo(Point[] points) {
		throw new UnsupportedOperationException();
	}

	public void polygon(Point[] points) {
		byte[] record = new byte[8 + points.length * 4];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_POLYGON);
		pos = setInt16(record, pos, points.length);
		for (int i = 0; i < points.length; i++) {
			pos = setInt16(record, pos, points[i].x);
			pos = setInt16(record, pos, points[i].y);
		}
		records.add(record);
	}

	public void polyline(Point[] points) {
		byte[] record = new byte[8 + points.length * 4];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_POLYLINE);
		pos = setInt16(record, pos, points.length);
		for (int i = 0; i < points.length; i++) {
			pos = setInt16(record, pos, points[i].x);
			pos = setInt16(record, pos, points[i].y);
		}
		records.add(record);
	}

	public void polyPolygon(Point[][] points) {
		int length = 8;
		for (int i = 0; i < points.length; i++) {
			length += 2 + points[i].length * 4;
		}
		byte[] record = new byte[length];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_POLYPOLYGON);
		pos = setInt16(record, pos, points.length);
		for (int i = 0; i < points.length; i++) {
			pos = setInt16(record, pos, points[i].length);
		}
		for (int i = 0; i < points.length; i++) {
			for (int j = 0; j < points[i].length; j++) {
				pos = setInt16(record, pos, points[i][j].x);
				pos = setInt16(record, pos, points[i][j].y);
			}
		}
		records.add(record);
	}

	public void fillPath() {
		throw new UnsupportedOperationException();
	}

	public void flattenPath() {
		throw new UnsupportedOperationException();
	}

	public void widenPath() {
		throw new UnsupportedOperationException();
	}

	public void strokePath() {
		throw new UnsupportedOperationException();
	}

	public void strokeAndFillPath() {
		throw new UnsupportedOperationException();
	}

	public void realizePalette() {
		byte[] record = new byte[6];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_REALIZEPALETTE);
		records.add(record);
	}

	public void restoreDC(int savedDC) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_RESTOREDC);
		pos = setInt16(record, pos, savedDC);
		records.add(record);

		int limit = (savedDC < 0) ? -savedDC : saveDC.size() - savedDC;
		for (int i = 0; i < limit && !saveDC.isEmpty(); i++) {
			dc = saveDC.removeLast();
		}
	}

	public void rectangle(int sx, int sy, int ex, int ey) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_RECTANGLE);
		pos = setInt16(record, pos, ey);
		pos = setInt16(record, pos, ex);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		records.add(record);
	}

	public void resizePalette(GdiPalette palette, int entries) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_RESIZEPALETTE);
		pos = setUint16(record, pos, entries);
		records.add(record);
	}

	public void roundRect(int sx, int sy, int ex, int ey, int rw, int rh) {
		byte[] record = new byte[18];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_ROUNDRECT);
		pos = setInt16(record, pos, rh);
		pos = setInt16(record, pos, rw);
		pos = setInt16(record, pos, ey);
		pos = setInt16(record, pos, ex);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		records.add(record);
	}

	public void seveDC() {
		byte[] record = new byte[6];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SAVEDC);
		records.add(record);

		saveDC.add((WmfDc) dc.clone());
	}

	public void scaleViewportExtEx(int x, int xd, int y, int yd, Size old) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SCALEVIEWPORTEXT);
		pos = setInt16(record, pos, yd);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, xd);
		pos = setInt16(record, pos, x);
		records.add(record);

		dc.scaleViewportExtEx(x, xd, y, yd, old);
	}

	public void scaleWindowExtEx(int x, int xd, int y, int yd, Size old) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SCALEWINDOWEXT);
		pos = setInt16(record, pos, yd);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, xd);
		pos = setInt16(record, pos, x);
		records.add(record);

		dc.scaleWindowExtEx(x, xd, y, yd, old);
	}

	public void selectClipRgn(GdiRegion rgn) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SELECTCLIPREGION);
		pos = setUint16(record, pos, (rgn != null) ? ((WmfRegion) rgn).getID() : 0);
		records.add(record);
	}

	public void selectClipPath(int mode) {
		throw new UnsupportedOperationException();
	}

	public GdiColorSpace setColorSpace(GdiColorSpace colorSpace) {
		throw new UnsupportedOperationException();
	}

	public void selectObject(GdiObject obj) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SELECTOBJECT);
		pos = setUint16(record, pos, ((WmfObject) obj).getID());
		records.add(record);

		if (obj instanceof WmfBrush) {
			dc.setBrush((WmfBrush) obj);
		} else if (obj instanceof WmfFont) {
			dc.setFont((WmfFont) obj);
		} else if (obj instanceof WmfPen) {
			dc.setPen((WmfPen) obj);
		}
	}

	public void selectPalette(GdiPalette palette, boolean mode) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SELECTPALETTE);
		pos = setInt16(record, pos, mode ? 1 : 0);
		pos = setUint16(record, pos, ((WmfPalette) palette).getID());
		records.add(record);
	}

	public void setBkColor(int color) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETBKCOLOR);
		pos = setInt32(record, pos, color);
		records.add(record);
	}

	public void setBkMode(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETBKMODE);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setBrushOrgEx(int x, int y, Point old) {
		throw new UnsupportedOperationException();
	}

	public void setColorAdjustment(byte[] colorAdjustment) {
		throw new UnsupportedOperationException();
	}

	public void setArcDirection(int direction) {
		throw new UnsupportedOperationException();
	}

	public void setDIBitsToDevice(int dx, int dy, int dw, int dh, int sx, int sy, int startscan, int scanlines,
			byte[] image, int colorUse) {
		byte[] record = new byte[24 + (image.length + image.length % 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETDIBTODEV);
		pos = setUint16(record, pos, colorUse);
		pos = setUint16(record, pos, scanlines);
		pos = setUint16(record, pos, startscan);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length % 2 == 1)
			pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void setLayout(long layout) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETLAYOUT);
		pos = setUint32(record, pos, layout);
		records.add(record);
	}

	public void setMapMode(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETMAPMODE);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setMapperFlags(long flags) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETMAPPERFLAGS);
		pos = setUint32(record, pos, flags);
		records.add(record);
	}

	public int setICMMode(int mode) {
		throw new UnsupportedOperationException();
	}

	public boolean setICMProfile(byte[] profileName) {
		throw new UnsupportedOperationException();
	}

	public boolean colorMatchToTarget(int action, int flags, byte[] targetProfile) {
		throw new UnsupportedOperationException();
	}

	public int setMetaRgn() {
		throw new UnsupportedOperationException();
	}

	public void setMiterLimit(float limit) {
		throw new UnsupportedOperationException();
	}

	public void setPaletteEntries(GdiPalette palette, int startIndex, int[] entries) {
		byte[] record = new byte[12 + entries.length * 4];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETPALENTRIES);
		pos = setUint16(record, pos, entries.length);
		pos = setUint16(record, pos, startIndex);
		pos = setUint16(record, pos, ((WmfPalette) palette).getID());
		for (int i = 0; i < entries.length; i++) {
			pos = setInt32(record, pos, entries[i]);
		}
		records.add(record);
	}

	public void setPixel(int x, int y, int color) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETPIXEL);
		pos = setInt32(record, pos, color);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void setPolyFillMode(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETPOLYFILLMODE);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setRelAbs(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETRELABS);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setROP2(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETROP2);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setStretchBltMode(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETSTRETCHBLTMODE);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setTextAlign(int align) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETTEXTALIGN);
		pos = setInt16(record, pos, align);
		records.add(record);

		dc.setTextAlign(align);
	}

	public void setTextCharacterExtra(int extra) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETTEXTCHAREXTRA);
		pos = setInt16(record, pos, extra);
		records.add(record);
	}

	public void setTextColor(int color) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETTEXTCOLOR);
		pos = setInt32(record, pos, color);
		records.add(record);
	}

	public void setTextJustification(int breakExtra, int breakCount) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETTEXTJUSTIFICATION);
		pos = setInt16(record, pos, breakCount);
		pos = setInt16(record, pos, breakExtra);
		records.add(record);
	}

	public void setViewportExtEx(int x, int y, Size old) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETVIEWPORTEXT);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);

		dc.setViewportExtEx(x, y, old);
	}

	public void setViewportOrgEx(int x, int y, Point old) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETVIEWPORTORG);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);

		dc.setViewportOrgEx(x, y, old);
	}

	public void setWindowExtEx(int width, int height, Size old) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETWINDOWEXT);
		pos = setInt16(record, pos, height);
		pos = setInt16(record, pos, width);
		records.add(record);

		dc.setWindowExtEx(width, height, old);
	}

	public void setWindowOrgEx(int x, int y, Point old) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_SETWINDOWORG);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);

		dc.setWindowOrgEx(x, y, old);
	}

	public void stretchBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, long rop) {
		byte[] record = new byte[26 + (image.length + image.length % 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_STRETCHBLT);
		pos = setUint32(record, pos, rop);
		pos = setInt16(record, pos, sh);
		pos = setInt16(record, pos, sw);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length % 2 == 1)
			pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void stretchDIBits(int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, byte[] image, int usage,
			long rop) {
		byte[] record = new byte[28 + (image.length + image.length % 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_STRETCHDIB);
		pos = setUint32(record, pos, rop);
		pos = setUint16(record, pos, usage);
		pos = setInt16(record, pos, sh);
		pos = setInt16(record, pos, sw);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length % 2 == 1)
			pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void transparentBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh,
			int transparentColor) {
		throw new UnsupportedOperationException();
	}

	public void textOut(int x, int y, byte[] text) {
		byte[] record = new byte[12 + text.length + text.length % 2];
		int pos = 0;
		pos = setUint32(record, pos, record.length / 2);
		pos = setUint16(record, pos, META_TEXTOUT);
		pos = setInt16(record, pos, text.length);
		pos = setBytes(record, pos, text);
		if (text.length % 2 == 1)
			pos = setByte(record, pos, 0);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void footer() {
		int pos = 0;
		if (records.isEmpty() || !isEofRecord((byte[]) records.get(records.size() - 1))) {
			byte[] record = new byte[6];
			pos = setUint32(record, 0, record.length / 2);
			pos = setUint16(record, pos, 0x0000);
			records.add(record);
		}

		if (header != null) {
			long size = header.length;
			long maxRecordSize = 0;
			Iterator<?> i = records.iterator();
			while (i.hasNext()) {
				byte[] record = (byte[]) i.next();
				size += record.length;
				if (record.length > maxRecordSize)
					maxRecordSize = record.length;
			}

			pos = setUint32(header, 6, size / 2);
			pos = setUint16(header, pos, objects.size());
			pos = setUint32(header, pos, maxRecordSize / 2);
		}
	}

	private boolean isEofRecord(byte[] record) {
		return record.length == 6 && (record[0] & 0xFF) == 3 && record[1] == 0 && record[2] == 0 && record[3] == 0
				&& record[4] == 0 && record[5] == 0;
	}

	private int setByte(byte[] out, int pos, int value) {
		out[pos] = (byte) (0xFF & value);
		return pos + 1;
	}

	private int setBytes(byte[] out, int pos, byte[] data) {
		System.arraycopy(data, 0, out, pos, data.length);
		return pos + data.length;
	}

	private int setInt16(byte[] out, int pos, int value) {
		out[pos] = (byte) (0xFF & value);
		out[pos + 1] = (byte) (0xFF & (value >> 8));
		return pos + 2;
	}

	private int setInt32(byte[] out, int pos, int value) {
		out[pos] = (byte) (0xFF & value);
		out[pos + 1] = (byte) (0xFF & (value >> 8));
		out[pos + 2] = (byte) (0xFF & (value >> 16));
		out[pos + 3] = (byte) (0xFF & (value >> 24));
		return pos + 4;
	}

	private int setUint16(byte[] out, int pos, int value) {
		out[pos] = (byte) (0xFF & value);
		out[pos + 1] = (byte) (0xFF & (value >> 8));
		return pos + 2;
	}

	private int setUint32(byte[] out, int pos, long value) {
		out[pos] = (byte) (0xFF & value);
		out[pos + 1] = (byte) (0xFF & (value >> 8));
		out[pos + 2] = (byte) (0xFF & (value >> 16));
		out[pos + 3] = (byte) (0xFF & (value >> 24));
		return pos + 4;
	}
}

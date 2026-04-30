package net.arnx.wmf2svg.gdi.svg;

import org.w3c.dom.Element;

class SvgComplexRegion extends SvgRegion {
	private int[][] rects;

	public SvgComplexRegion(SvgGdi gdi, float[] xform, byte[] rgnData, int count) {
		super(gdi);
		this.rects = readRects(xform, rgnData, count);
	}

	public Element createElement() {
		Element elem = getGDI().getDocument().createElement("g");
		for (int i = 0; i < rects.length; i++) {
			elem.appendChild(createRectElement(rects[i]));
		}
		return elem;
	}

	int[][] getRects() {
		return rects;
	}

	private Element createRectElement(int[] rect) {
		Element elem = getGDI().getDocument().createElement("rect");
		elem.setAttribute("x", "" + rect[0]);
		elem.setAttribute("y", "" + rect[1]);
		elem.setAttribute("width", "" + (rect[2] - rect[0]));
		elem.setAttribute("height", "" + (rect[3] - rect[1]));
		elem.setAttribute("fill", "inherit");
		elem.setAttribute("stroke", "inherit");
		return elem;
	}

	private static int[][] readRects(float[] xform, byte[] data, int count) {
		if (data == null || count < 32 || count > data.length) {
			return new int[0][];
		}

		int headerSize = readInt32(data, 0);
		int rectCount = readInt32(data, 8);
		if (headerSize < 32 || rectCount <= 0 || headerSize + rectCount * 16 > count) {
			return new int[0][];
		}

		int[][] rects = new int[rectCount][];
		int offset = headerSize;
		for (int i = 0; i < rects.length; i++) {
			rects[i] = transformRect(xform, readInt32(data, offset), readInt32(data, offset + 4),
					readInt32(data, offset + 8), readInt32(data, offset + 12));
			offset += 16;
		}
		return rects;
	}

	private static int[] transformRect(float[] xform, int left, int top, int right, int bottom) {
		int[] p1 = transformPoint(xform, left, top);
		int[] p2 = transformPoint(xform, right, top);
		int[] p3 = transformPoint(xform, right, bottom);
		int[] p4 = transformPoint(xform, left, bottom);

		return new int[]{Math.min(Math.min(p1[0], p2[0]), Math.min(p3[0], p4[0])),
				Math.min(Math.min(p1[1], p2[1]), Math.min(p3[1], p4[1])),
				Math.max(Math.max(p1[0], p2[0]), Math.max(p3[0], p4[0])),
				Math.max(Math.max(p1[1], p2[1]), Math.max(p3[1], p4[1]))};
	}

	private static int[] transformPoint(float[] xform, int x, int y) {
		if (xform == null || xform.length < 6) {
			return new int[]{x, y};
		}
		return new int[]{(int) Math.round(xform[0] * x + xform[2] * y + xform[4]),
				(int) Math.round(xform[1] * x + xform[3] * y + xform[5])};
	}

	private static int readInt32(byte[] data, int offset) {
		return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16)
				| ((data[offset + 3] & 0xFF) << 24);
	}
}

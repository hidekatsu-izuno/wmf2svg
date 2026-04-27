package net.arnx.wmf2svg.gdi.emf;

import net.arnx.wmf2svg.gdi.GdiPen;

class EmfPen extends EmfObject implements GdiPen {
	final int style;
	final int width;
	final int color;

	EmfPen(int id, int style, int width, int color) {
		super(id);
		this.style = style;
		this.width = width;
		this.color = color;
	}

	public int getStyle() {
		return style;
	}

	public int getWidth() {
		return width;
	}

	public int getColor() {
		return color;
	}
}

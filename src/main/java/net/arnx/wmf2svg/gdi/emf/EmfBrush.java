package net.arnx.wmf2svg.gdi.emf;

import net.arnx.wmf2svg.gdi.GdiBrush;

class EmfBrush extends EmfObject implements GdiBrush {
	final int style;
	final int color;
	final int hatch;

	EmfBrush(int id, int style, int color, int hatch) {
		super(id);
		this.style = style;
		this.color = color;
		this.hatch = hatch;
	}

	public int getStyle() {
		return style;
	}

	public int getColor() {
		return color;
	}

	public int getHatch() {
		return hatch;
	}
}

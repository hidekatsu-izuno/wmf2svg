package net.arnx.wmf2svg.gdi.emf;

import net.arnx.wmf2svg.gdi.GdiPatternBrush;

class EmfPatternBrush extends EmfObject implements GdiPatternBrush {
	private byte[] image;

	EmfPatternBrush(int id, byte[] image) {
		super(id);
		this.image = image;
	}

	public byte[] getPattern() {
		return image;
	}
}

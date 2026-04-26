package net.arnx.wmf2svg.gdi.svg;

import net.arnx.wmf2svg.gdi.GdiColorSpace;

class SvgColorSpace extends SvgObject implements GdiColorSpace {
	private byte[] logColorSpace;

	public SvgColorSpace(SvgGdi gdi, byte[] logColorSpace) {
		super(gdi);
		this.logColorSpace = logColorSpace;
	}

	public byte[] getLogColorSpace() {
		return logColorSpace;
	}
}

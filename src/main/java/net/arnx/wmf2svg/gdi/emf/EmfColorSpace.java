package net.arnx.wmf2svg.gdi.emf;

import net.arnx.wmf2svg.gdi.GdiColorSpace;

class EmfColorSpace extends EmfObject implements GdiColorSpace {
	final byte[] data;

	EmfColorSpace(int id, byte[] data) {
		super(id);
		this.data = data;
	}
}

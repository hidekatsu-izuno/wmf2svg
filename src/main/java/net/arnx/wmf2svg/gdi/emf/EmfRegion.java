package net.arnx.wmf2svg.gdi.emf;

import net.arnx.wmf2svg.gdi.GdiRegion;

class EmfRegion extends EmfObject implements GdiRegion {
	final byte[] data;

	EmfRegion(int id, byte[] data) {
		super(id);
		this.data = data;
	}
}

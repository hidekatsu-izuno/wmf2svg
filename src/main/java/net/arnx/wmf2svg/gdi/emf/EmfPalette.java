package net.arnx.wmf2svg.gdi.emf;

import net.arnx.wmf2svg.gdi.GdiPalette;

class EmfPalette extends EmfObject implements GdiPalette {
	final int version;
	final int[] entries;

	EmfPalette(int id, int version, int[] entries) {
		super(id);
		this.version = version;
		this.entries = entries;
	}

	public int getVersion() {
		return version;
	}

	public int[] getEntries() {
		return entries;
	}
}

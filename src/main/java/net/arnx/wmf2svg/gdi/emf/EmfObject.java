package net.arnx.wmf2svg.gdi.emf;

import net.arnx.wmf2svg.gdi.GdiObject;

class EmfObject implements GdiObject {
	final int id;

	EmfObject(int id) {
		this.id = id;
	}

	int getID() {
		return id;
	}
}

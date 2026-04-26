package net.arnx.wmf2svg.gdi.svg;

import net.arnx.wmf2svg.gdi.GdiPalette;

class SvgPalette  extends SvgObject implements GdiPalette {
	private int version;
	private int[] entries;
	
	public SvgPalette(
		SvgGdi gdi,
		int version,
		int[] entries) {
		super(gdi);
		this.version = version;
		this.entries = entries;
	}
	
	public int getVersion() {
		return version;
	}
	
	public int[] getEntries() {
		return entries;
	}

	public void setEntries(int startIndex, int[] entries) {
		if (entries == null || entries.length == 0) {
			return;
		}
		int size = Math.max(this.entries.length, startIndex + entries.length);
		if (size != this.entries.length) {
			int[] newEntries = new int[size];
			System.arraycopy(this.entries, 0, newEntries, 0, this.entries.length);
			this.entries = newEntries;
		}
		System.arraycopy(entries, 0, this.entries, startIndex, entries.length);
	}
}

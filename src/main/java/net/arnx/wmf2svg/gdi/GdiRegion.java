package net.arnx.wmf2svg.gdi;

public interface GdiRegion extends GdiObject {
	public static final int NULLREGION = 1;
	public static final int SIMPLEREGION	 = 2;
	public static final int COMPLEXREGION = 3;
	public static final int ERROR = 0;

	public static final int RGN_AND = 1;
	public static final int RGN_OR = 2;
	public static final int RGN_XOR = 3;
	public static final int RGN_DIFF = 4;
	public static final int RGN_COPY = 5;
}

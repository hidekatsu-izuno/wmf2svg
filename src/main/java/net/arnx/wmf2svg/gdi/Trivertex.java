package net.arnx.wmf2svg.gdi;

public class Trivertex {
	public int x;
	public int y;
	public int red;
	public int green;
	public int blue;
	public int alpha;

	public Trivertex(int x, int y, int red, int green, int blue, int alpha) {
		this.x = x;
		this.y = y;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
	}

	public int getColor() {
		return ((blue >>> 8) << 16) | ((green >>> 8) << 8) | (red >>> 8);
	}
}

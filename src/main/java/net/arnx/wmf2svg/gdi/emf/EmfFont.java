package net.arnx.wmf2svg.gdi.emf;

import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiUtils;

class EmfFont extends EmfObject implements GdiFont {
	final int height;
	final int width;
	final int escapement;
	final int orientation;
	final int weight;
	final boolean italic;
	final boolean underline;
	final boolean strikeout;
	final int charset;
	final int outPrecision;
	final int clipPrecision;
	final int quality;
	final int pitchAndFamily;
	final String faceName;

	EmfFont(int id, int height, int width, int escapement, int orientation, int weight, boolean italic,
			boolean underline, boolean strikeout, int charset, int outPrecision, int clipPrecision, int quality,
			int pitchAndFamily, byte[] faceName) {
		super(id);
		this.height = height;
		this.width = width;
		this.escapement = escapement;
		this.orientation = orientation;
		this.weight = weight;
		this.italic = italic;
		this.underline = underline;
		this.strikeout = strikeout;
		this.charset = charset;
		this.outPrecision = outPrecision;
		this.clipPrecision = clipPrecision;
		this.quality = quality;
		this.pitchAndFamily = pitchAndFamily;
		this.faceName = GdiUtils.convertString(faceName, charset);
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public int getEscapement() {
		return escapement;
	}

	public int getOrientation() {
		return orientation;
	}

	public int getWeight() {
		return weight;
	}

	public boolean isItalic() {
		return italic;
	}

	public boolean isUnderlined() {
		return underline;
	}

	public boolean isStrikedOut() {
		return strikeout;
	}

	public int getCharset() {
		return charset;
	}

	public int getOutPrecision() {
		return outPrecision;
	}

	public int getClipPrecision() {
		return clipPrecision;
	}

	public int getQuality() {
		return quality;
	}

	public int getPitchAndFamily() {
		return pitchAndFamily;
	}

	public String getFaceName() {
		return faceName;
	}
}

package net.arnx.wmf2svg.gdi.svg;

import java.util.Arrays;

import org.w3c.dom.Element;

import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiPatternBrush;
import net.arnx.wmf2svg.util.ImageUtil;

class SvgPatternBrush extends SvgBrush implements GdiPatternBrush {
	private byte[] bmp;
	private int usage;

	public SvgPatternBrush(SvgGdi gdi, byte[] bmp, int usage) {
		super(gdi, GdiBrush.BS_PATTERN, 0, 0);
		this.bmp = bmp;
		this.usage = usage;
	}

	public byte[] getPattern() {
		return bmp;
	}

	public int hashCode() {
		return (31 * super.hashCode() + Arrays.hashCode(bmp)) * 31 + usage;
	}

	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		SvgPatternBrush brush = (SvgPatternBrush) obj;
		return usage == brush.usage && Arrays.equals(bmp, brush.bmp);
	}

	public Element createFillPattern(String id) {
		byte[] png = getGDI().convertBrushPatternToPng(bmp, usage);
		String data = getGDI().createPngDataUri(png);
		int[] size = ImageUtil.getSize(png);
		if (data == null || size == null) {
			return null;
		}

		int width = toPatternSize(size[0]);
		int height = toPatternSize(size[1]);

		Element pattern = getGDI().getDocument().createElement("pattern");
		pattern.setAttribute("id", id);
		pattern.setAttribute("patternUnits", "userSpaceOnUse");
		pattern.setAttribute("x", "" + (int) getGDI().getDC().toAbsoluteX(getGDI().getDC().getBrushOrgX()));
		pattern.setAttribute("y", "" + (int) getGDI().getDC().toAbsoluteY(getGDI().getDC().getBrushOrgY()));
		pattern.setAttribute("width", "" + width);
		pattern.setAttribute("height", "" + height);

		Element image = getGDI().getDocument().createElement("image");
		image.setAttribute("width", "" + width);
		image.setAttribute("height", "" + height);
		image.setAttribute("preserveAspectRatio", "none");
		image.setAttribute("xlink:href", data);
		pattern.appendChild(image);
		return pattern;
	}

	private int toPatternSize(int px) {
		if (getGDI().hasPlaceableHeader()) {
			return Math.max(1, toRealSize(px) * 3 / 4);
		}
		return Math.max(1, px);
	}
}

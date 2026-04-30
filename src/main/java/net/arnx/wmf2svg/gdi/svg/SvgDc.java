/*
 * Copyright 2007-2008 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package net.arnx.wmf2svg.gdi.svg;

import org.w3c.dom.*;

import net.arnx.wmf2svg.gdi.*;

/**
 * @author Hidekatsu Izuno
 */
public class SvgDc implements Cloneable {
	private static final double CSS_DPI = 96.0;

	private SvgGdi gdi;

	private int dpi = 1440;

	// window
	private int wx = 0;
	private int wy = 0;
	private int ww = 0;
	private int wh = 0;

	// window offset
	private int wox = 0;
	private int woy = 0;

	// window scale
	private double wsx = 1.0;
	private double wsy = 1.0;

	// mapping scale
	private double mx = 1.0;
	private double my = 1.0;

	// viewport
	private int vx = 0;
	private int vy = 0;
	private int vw = 0;
	private int vh = 0;

	// viewport offset
	private int vox = 0;
	private int voy = 0;

	// viewport scale
	private double vsx = 1.0;
	private double vsy = 1.0;

	// current location
	private int cx = 0;
	private int cy = 0;

	// clip offset
	private int cox = 0;
	private int coy = 0;

	// brush origin
	private int box = 0;
	private int boy = 0;

	private int mapMode = Gdi.MM_TEXT;
	private int bkColor = 0x00FFFFFF;
	private int bkMode = Gdi.OPAQUE;
	private int textColor = 0x00000000;
	private int textSpace = 0;
	private int textAlign = Gdi.TA_TOP | Gdi.TA_LEFT;
	private int textDx = 0;
	private int polyFillMode = Gdi.ALTERNATE;
	private int relAbsMode = 0;
	private int rop2Mode = Gdi.R2_COPYPEN;
	private int stretchBltMode = Gdi.STRETCH_ANDSCANS;
	private int arcDirection = Gdi.AD_COUNTERCLOCKWISE;
	private float miterLimit = 10.0f;
	private long layout = 0;
	private long mapperFlags = 0;
	private int icmMode = 0;
	private byte[] colorAdjustment = null;
	private byte[] icmProfile = null;

	private SvgBrush brush = null;
	private SvgFont font = null;
	private SvgPen pen = null;
	private SvgColorSpace colorSpace = null;

	private Element mask = null;

	public SvgDc(SvgGdi gdi) {
		this.gdi = gdi;
	}

	public int getDpi() {
		return dpi;
	}

	public void setWindowOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = wx;
			old.y = wy;
		}
		wx = x;
		wy = y;
	}

	public void setWindowExtEx(int width, int height, Size old) {
		if (old != null) {
			old.width = ww;
			old.height = wh;
		}
		ww = width;
		wh = height;
	}

	public void offsetWindowOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = wox;
			old.y = woy;
		}
		wox += x;
		woy += y;
	}

	public void scaleWindowExtEx(int x, int xd, int y, int yd, Size old) {
		// TODO
		wsx = (wsx * x) / xd;
		wsy = (wsy * y) / yd;
	}

	public int getWindowX() {
		return wx;
	}

	public int getWindowY() {
		return wy;
	}

	public int getWindowWidth() {
		return ww;
	}

	public int getWindowHeight() {
		return wh;
	}

	public void setViewportOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = vx;
			old.y = vy;
		}
		vx = x;
		vy = y;
	}

	public void setViewportExtEx(int width, int height, Size old) {
		if (old != null) {
			old.width = vw;
			old.height = vh;
		}
		vw = width;
		vh = height;
	}

	public int getViewportX() {
		return vx;
	}

	public int getViewportY() {
		return vy;
	}

	public int getViewportWidth() {
		return vw;
	}

	public int getViewportHeight() {
		return vh;
	}

	public void offsetViewportOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = vox;
			old.y = voy;
		}
		vox += x;
		voy += y;
	}

	public void scaleViewportExtEx(int x, int xd, int y, int yd, Size old) {
		// TODO
		vsx = (vsx * x) / xd;
		vsy = (vsy * y) / yd;
	}

	public void offsetClipRgn(int x, int y) {
		cox = x;
		coy = y;
	}

	public int getMapMode() {
		return mapMode;
	}

	public void setMapMode(int mode) {
		mapMode = mode;
		switch (mode) {
			case Gdi.MM_HIENGLISH :
				mx = 0.09;
				my = -0.09;
				break;
			case Gdi.MM_LOENGLISH :
				mx = 0.9;
				my = -0.9;
				break;
			case Gdi.MM_HIMETRIC :
				mx = 0.03543307;
				my = -0.03543307;
				break;
			case Gdi.MM_LOMETRIC :
				mx = 0.3543307;
				my = -0.3543307;
				break;
			case Gdi.MM_TWIPS :
				mx = 0.0625;
				my = -0.0625;
				break;
			default :
				mx = 1.0;
				my = 1.0;
		}
	}

	public int getCurrentX() {
		return cx;
	}

	public int getCurrentY() {
		return cy;
	}

	public int getOffsetClipX() {
		return cox;
	}

	public int getOffsetClipY() {
		return coy;
	}

	public void setBrushOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = box;
			old.y = boy;
		}
		box = x;
		boy = y;
	}

	public int getBrushOrgX() {
		return box;
	}

	public int getBrushOrgY() {
		return boy;
	}

	public void moveToEx(int x, int y, Point old) {
		if (old != null) {
			old.x = cx;
			old.y = cy;
		}
		cx = x;
		cy = y;
	}

	public double toAbsoluteX(double x) {
		return toViewportOriginX(vx + vox) + ((mx * x - (wx + wox)) / wsx) * viewportScaleX();
	}

	public double toAbsoluteY(double y) {
		return toViewportOriginY(vy + voy) + ((my * y - (wy + woy)) / wsy) * viewportScaleY();
	}

	public double toRelativeX(double x) {
		return (mx * x / wsx) * viewportScaleX();
	}

	public double toRelativeY(double y) {
		return (my * y / wsy) * viewportScaleY();
	}

	public double toStrokeWidth(double width) {
		return Math.max(1.0, Math.abs(toRelativeX(width)));
	}

	private double viewportScaleX() {
		return (vw != 0 && ww != 0) ? (vw * vsx) / ww : windowSignX();
	}

	private double viewportScaleY() {
		return (vh != 0 && wh != 0) ? (vh * vsy) / wh : windowSignY();
	}

	private double windowSignX() {
		return ww < 0 ? -1.0 : 1.0;
	}

	private double windowSignY() {
		return wh < 0 ? -1.0 : 1.0;
	}

	private double toViewportOriginX(int x) {
		return x * dpi / CSS_DPI;
	}

	private double toViewportOriginY(int y) {
		return y * dpi / CSS_DPI;
	}

	public void setDpi(int dpi) {
		this.dpi = (dpi > 0) ? dpi : 1440;
	}

	public int getBkColor() {
		return bkColor;
	}

	public void setBkColor(int color) {
		bkColor = color;
	}

	public int getBkMode() {
		return bkMode;
	}

	public void setBkMode(int mode) {
		bkMode = mode;
	}

	public int getTextColor() {
		return textColor;
	}

	public void setTextColor(int color) {
		textColor = color;
	}

	public int getPolyFillMode() {
		return polyFillMode;
	}

	public void setPolyFillMode(int mode) {
		polyFillMode = mode;
	}

	public int getRelAbs() {
		return relAbsMode;
	}

	public void setRelAbs(int mode) {
		relAbsMode = mode;
	}

	public int getROP2() {
		return rop2Mode;
	}

	public void setROP2(int mode) {
		rop2Mode = mode;
	}

	public int getStretchBltMode() {
		return stretchBltMode;
	}

	public void setStretchBltMode(int mode) {
		stretchBltMode = mode;
	}

	public int getArcDirection() {
		return arcDirection;
	}

	public void setArcDirection(int direction) {
		arcDirection = direction;
	}

	public float getMiterLimit() {
		return miterLimit;
	}

	public void setMiterLimit(float limit) {
		miterLimit = limit;
	}

	public int getTextSpace() {
		return textSpace;
	}

	public void setTextSpace(int space) {
		textSpace = space;
	}

	public int getTextAlign() {
		return textAlign;
	}

	public void setTextAlign(int align) {
		textAlign = align;
	}

	public int getTextCharacterExtra() {
		return textDx;
	}

	public void setTextCharacterExtra(int extra) {
		textDx = extra;
	}

	public long getLayout() {
		return layout;
	}

	public void setLayout(long layout) {
		this.layout = layout;
	}

	public long getMapperFlags() {
		return mapperFlags;
	}

	public void setMapperFlags(long flags) {
		mapperFlags = flags;
	}

	public int setICMMode(int mode) {
		int old = icmMode;
		icmMode = mode;
		return old;
	}

	public int getICMMode() {
		return icmMode;
	}

	public void setColorAdjustment(byte[] colorAdjustment) {
		if (colorAdjustment == null) {
			this.colorAdjustment = null;
		} else {
			this.colorAdjustment = new byte[colorAdjustment.length];
			System.arraycopy(colorAdjustment, 0, this.colorAdjustment, 0, colorAdjustment.length);
		}
	}

	public byte[] getColorAdjustment() {
		if (colorAdjustment == null) {
			return null;
		}
		byte[] copy = new byte[colorAdjustment.length];
		System.arraycopy(colorAdjustment, 0, copy, 0, colorAdjustment.length);
		return copy;
	}

	public void setICMProfile(byte[] profileName) {
		if (profileName == null) {
			icmProfile = null;
		} else {
			icmProfile = new byte[profileName.length];
			System.arraycopy(profileName, 0, icmProfile, 0, profileName.length);
		}
	}

	public byte[] getICMProfile() {
		if (icmProfile == null) {
			return null;
		}
		byte[] copy = new byte[icmProfile.length];
		System.arraycopy(icmProfile, 0, copy, 0, icmProfile.length);
		return copy;
	}

	public SvgBrush getBrush() {
		return brush;
	}

	public void setBrush(SvgBrush brush) {
		this.brush = brush;
	}

	public SvgColorSpace setColorSpace(SvgColorSpace colorSpace) {
		SvgColorSpace old = this.colorSpace;
		this.colorSpace = colorSpace;
		return old;
	}

	public SvgColorSpace getColorSpace() {
		return colorSpace;
	}

	public SvgFont getFont() {
		return font;
	}

	public void setFont(SvgFont font) {
		this.font = font;
	}

	public SvgPen getPen() {
		return pen;
	}

	public void setPen(SvgPen pen) {
		this.pen = pen;
	}

	public void setMask(Element mask) {
		this.mask = mask;
	}

	public Element getMask() {
		return mask;
	}

	private Element createRopFilter(Document doc, String name) {
		Element filter = doc.createElement("filter");
		filter.setAttribute("id", name);
		filter.setIdAttribute("id", true);
		filter.setAttribute("x", "0");
		filter.setAttribute("y", "0");
		filter.setAttribute("width", "100%");
		filter.setAttribute("height", "100%");
		filter.setAttribute("color-interpolation-filters", "sRGB");
		return filter;
	}

	public String getRopFilter(long rop) {
		String name = null;
		Document doc = gdi.getDocument();

		if (rop == Gdi.BLACKNESS) {
			name = "BLACKNESS_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feColorMatrix = doc.createElement("feColorMatrix");
				feColorMatrix.setAttribute("type", "matrix");
				feColorMatrix.setAttribute("in", "SourceGraphic");
				feColorMatrix.setAttribute("values", "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0");
				filter.appendChild(feColorMatrix);

				gdi.getDefsElement().appendChild(filter);
			}
		} else if (rop == Gdi.NOTSRCERASE) {
			name = "NOTSRCERASE_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feComposite = doc.createElement("feComposite");
				feComposite.setAttribute("in", "SourceGraphic");
				feComposite.setAttribute("in2", "BackgroundImage");
				feComposite.setAttribute("operator", "arithmetic");
				feComposite.setAttribute("k1", "1");
				feComposite.setAttribute("result", "result0");
				filter.appendChild(feComposite);

				Element feColorMatrix = doc.createElement("feColorMatrix");
				feColorMatrix.setAttribute("in", "result0");
				feColorMatrix.setAttribute("values", "-1 0 0 0 1 0 -1 0 0 1 0 0 -1 0 1 0 0 0 1 0");
				filter.appendChild(feColorMatrix);

				gdi.getDefsElement().appendChild(filter);
			}
		} else if (rop == Gdi.NOTSRCCOPY) {
			name = "NOTSRCCOPY_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feColorMatrix = doc.createElement("feColorMatrix");
				feColorMatrix.setAttribute("type", "matrix");
				feColorMatrix.setAttribute("in", "SourceGraphic");
				feColorMatrix.setAttribute("values", "-1 0 0 0 1 0 -1 0 0 1 0 0 -1 0 1 0 0 0 1 0");
				filter.appendChild(feColorMatrix);

				gdi.getDefsElement().appendChild(filter);
			}
		} else if (rop == Gdi.SRCERASE) {
			name = "SRCERASE_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feColorMatrix = doc.createElement("feColorMatrix");
				feColorMatrix.setAttribute("type", "matrix");
				feColorMatrix.setAttribute("in", "BackgroundImage");
				feColorMatrix.setAttribute("values", "-1 0 0 0 1 0 -1 0 0 1 0 0 -1 0 1 0 0 0 1 0");
				feColorMatrix.setAttribute("result", "result0");
				filter.appendChild(feColorMatrix);

				Element feComposite = doc.createElement("feComposite");
				feComposite.setAttribute("in", "SourceGraphic");
				feComposite.setAttribute("in2", "result0");
				feComposite.setAttribute("operator", "arithmetic");
				feComposite.setAttribute("k2", "1");
				feComposite.setAttribute("k3", "1");
				filter.appendChild(feComposite);

				gdi.getDefsElement().appendChild(filter);
			}
		} else if (rop == Gdi.PATINVERT) {
			name = "PATINVERT_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feBlend = doc.createElement("feBlend");
				feBlend.setAttribute("in", "SourceGraphic");
				feBlend.setAttribute("in2", "BackgroundImage");
				feBlend.setAttribute("mode", "difference");
				filter.appendChild(feBlend);

				gdi.getDefsElement().appendChild(filter);
			}
		} else if (rop == Gdi.SRCINVERT) {
			name = "SRCINVERT_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feBlend = doc.createElement("feBlend");
				feBlend.setAttribute("in", "SourceGraphic");
				feBlend.setAttribute("in2", "BackgroundImage");
				feBlend.setAttribute("mode", "difference");
				filter.appendChild(feBlend);

				gdi.getDefsElement().appendChild(filter);
			}
		} else if (rop == Gdi.DSTINVERT) {
			name = "DSTINVERT_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feColorMatrix = doc.createElement("feColorMatrix");
				feColorMatrix.setAttribute("type", "matrix");
				feColorMatrix.setAttribute("in", "BackgroundImage");
				feColorMatrix.setAttribute("values", "-1 0 0 0 1 0 -1 0 0 1 0 0 -1 0 1 0 0 0 1 0");
				filter.appendChild(feColorMatrix);

				gdi.getDefsElement().appendChild(filter);
			}
		} else if (rop == Gdi.SRCAND) {
			name = "SRCAND_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feComposite = doc.createElement("feComposite");
				feComposite.setAttribute("in", "SourceGraphic");
				feComposite.setAttribute("in2", "BackgroundImage");
				feComposite.setAttribute("operator", "arithmetic");
				feComposite.setAttribute("k1", "1");
				filter.appendChild(feComposite);

				gdi.getDefsElement().appendChild(filter);
			}
		} else if (rop == Gdi.MERGEPAINT) {
			name = "MERGEPAINT_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feColorMatrix = doc.createElement("feColorMatrix");
				feColorMatrix.setAttribute("type", "matrix");
				feColorMatrix.setAttribute("in", "SourceGraphic");
				feColorMatrix.setAttribute("values", "-1 0 0 0 1 0 -1 0 0 1 0 0 -1 0 1 0 0 0 1 0");
				feColorMatrix.setAttribute("result", "result0");
				filter.appendChild(feColorMatrix);

				Element feComposite = doc.createElement("feComposite");
				feComposite.setAttribute("in", "result0");
				feComposite.setAttribute("in2", "BackgroundImage");
				feComposite.setAttribute("operator", "arithmetic");
				feComposite.setAttribute("k1", "1");
				filter.appendChild(feComposite);

				gdi.getDefsElement().appendChild(filter);
			}
		} else if (rop == Gdi.MERGECOPY) {
			// TODO
		} else if (rop == Gdi.SRCPAINT) {
			name = "SRCPAINT_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feComposite = doc.createElement("feComposite");
				feComposite.setAttribute("in", "SourceGraphic");
				feComposite.setAttribute("in2", "BackgroundImage");
				feComposite.setAttribute("operator", "arithmetic");
				feComposite.setAttribute("k2", "1");
				feComposite.setAttribute("k3", "1");
				filter.appendChild(feComposite);

				gdi.getDefsElement().appendChild(filter);
			}
		} else if (rop == Gdi.PATCOPY) {
			// TODO
		} else if (rop == Gdi.PATPAINT) {
			// TODO
		} else if (rop == Gdi.WHITENESS) {
			name = "WHITENESS_FILTER";
			Element filter = doc.getElementById(name);
			if (filter == null) {
				filter = createRopFilter(doc, name);

				Element feColorMatrix = doc.createElement("feColorMatrix");
				feColorMatrix.setAttribute("type", "matrix");
				feColorMatrix.setAttribute("in", "SourceGraphic");
				feColorMatrix.setAttribute("values", "1 0 0 0 1 0 1 0 0 1 0 0 1 0 1 0 0 0 1 0");
				filter.appendChild(feColorMatrix);

				gdi.getDefsElement().appendChild(filter);
			}
		}

		if (name != null) {
			if (!doc.getDocumentElement().hasAttribute("enable-background")) {
				doc.getDocumentElement().setAttribute("enable-background", "new");
			}
			return "url(#" + name + ")";
		}
		return null;
	}

	public Object clone() {
		try {
			return (super.clone());
		} catch (CloneNotSupportedException e) {
			throw (new InternalError(e.getMessage()));
		}
	}

	public String toString() {
		return "SvgDc [gdi=" + gdi + ", dpi=" + dpi + ", wx=" + wx + ", wy=" + wy + ", ww=" + ww + ", wh=" + wh
				+ ", wox=" + wox + ", woy=" + woy + ", wsx=" + wsx + ", wsy=" + wsy + ", mx=" + mx + ", my=" + my
				+ ", vx=" + vx + ", vy=" + vy + ", vw=" + vw + ", vh=" + vh + ", vox=" + vox + ", voy=" + voy + ", vsx="
				+ vsx + ", vsy=" + vsy + ", cx=" + cx + ", cy=" + cy + ", mapMode=" + mapMode + ", bkColor=" + bkColor
				+ ", bkMode=" + bkMode + ", textColor=" + textColor + ", textSpace=" + textSpace + ", textAlign="
				+ textAlign + ", textDx=" + textDx + ", polyFillMode=" + polyFillMode + ", relAbsMode=" + relAbsMode
				+ ", rop2Mode=" + rop2Mode + ", stretchBltMode=" + stretchBltMode + ", brush=" + brush + ", font="
				+ font + ", pen=" + pen + "]";
	}
}

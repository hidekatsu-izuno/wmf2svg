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
package net.arnx.wmf2svg.gdi.awt;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Size;

class AwtDc implements Cloneable {
	private static final double CSS_DPI = 96.0;

	private int dpi = 1440;
	private int wx;
	private int wy;
	private int ww;
	private int wh;
	private int wox;
	private int woy;
	private double wsx = 1.0;
	private double wsy = 1.0;
	private double mx = 1.0;
	private double my = 1.0;
	private int vx;
	private int vy;
	private int vw;
	private int vh;
	private int vox;
	private int voy;
	private double vsx = 1.0;
	private double vsy = 1.0;
	private int cx;
	private int cy;
	private int box;
	private int boy;
	private int bkColor = 0x00FFFFFF;
	private int bkMode = Gdi.OPAQUE;
	private int textColor;
	private int textAlign = Gdi.TA_TOP | Gdi.TA_LEFT;
	private int textCharacterExtra;
	private int textJustificationExtra;
	private int textJustificationCount;
	private int polyFillMode = Gdi.ALTERNATE;
	private int relAbsMode;
	private int rop2Mode = Gdi.R2_COPYPEN;
	private int stretchBltMode = Gdi.STRETCH_ANDSCANS;
	private int arcDirection = Gdi.AD_COUNTERCLOCKWISE;
	private float miterLimit = 10.0f;
	private long layout;
	private long mapperFlags;
	private AwtBrush brush;
	private AwtPen pen;
	private AwtFont font;

	public void setDpi(int dpi) {
		this.dpi = dpi > 0 ? dpi : 1440;
	}

	public double toAbsoluteX(double x) {
		return (vx + vox) * dpi / CSS_DPI + ((mx * x - (wx + wox)) / wsx) * viewportScaleX();
	}

	public double toAbsoluteY(double y) {
		return (vy + voy) * dpi / CSS_DPI + ((my * y - (wy + woy)) / wsy) * viewportScaleY();
	}

	public double toRelativeX(double x) {
		return (mx * x / wsx) * viewportScaleX();
	}

	public double toRelativeY(double y) {
		return (my * y / wsy) * viewportScaleY();
	}

	private double viewportScaleX() {
		return vw != 0 && ww != 0 ? (vw * vsx) / ww : ww < 0 ? -1.0 : 1.0;
	}

	private double viewportScaleY() {
		return vh != 0 && wh != 0 ? (vh * vsy) / wh : wh < 0 ? -1.0 : 1.0;
	}

	public void setWindowOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = wx;
			old.y = wy;
		}
		wx = x;
		wy = y;
	}

	public int getWindowX() {
		return wx;
	}

	public int getWindowY() {
		return wy;
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
		wsx = (wsx * x) / xd;
		wsy = (wsy * y) / yd;
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

	public void offsetViewportOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = vox;
			old.y = voy;
		}
		vox += x;
		voy += y;
	}

	public void scaleViewportExtEx(int x, int xd, int y, int yd, Size old) {
		vsx = (vsx * x) / xd;
		vsy = (vsy * y) / yd;
	}

	public void setMapMode(int mode) {
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

	public void moveToEx(int x, int y, Point old) {
		if (old != null) {
			old.x = cx;
			old.y = cy;
		}
		cx = x;
		cy = y;
	}

	public void offsetClipRgn(int x, int y) {
	}

	public void setBrushOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = box;
			old.y = boy;
		}
		box = x;
		boy = y;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	public int getCurrentX() {
		return cx;
	}

	public int getCurrentY() {
		return cy;
	}

	public int getBrushOrgX() {
		return box;
	}

	public int getBrushOrgY() {
		return boy;
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

	public int getTextAlign() {
		return textAlign;
	}

	public void setTextAlign(int align) {
		textAlign = align;
	}

	public int getTextCharacterExtra() {
		return textCharacterExtra;
	}

	public void setTextCharacterExtra(int extra) {
		textCharacterExtra = extra;
	}

	public int getPolyFillMode() {
		return polyFillMode;
	}

	public void setPolyFillMode(int mode) {
		polyFillMode = mode;
	}

	public void setRelAbs(int mode) {
		relAbsMode = mode;
	}

	public void setROP2(int mode) {
		rop2Mode = mode;
	}

	public int getROP2() {
		return rop2Mode;
	}

	public void setStretchBltMode(int mode) {
		stretchBltMode = mode;
	}

	public void setArcDirection(int direction) {
		arcDirection = direction;
	}

	public int getArcDirection() {
		return arcDirection;
	}

	public void setMiterLimit(float limit) {
		miterLimit = limit;
	}

	public float getMiterLimit() {
		return miterLimit;
	}

	public void setLayout(long layout) {
		this.layout = layout;
	}

	public void setMapperFlags(long mapperFlags) {
		this.mapperFlags = mapperFlags;
	}

	public void setTextJustification(int breakExtra, int breakCount) {
		textJustificationExtra = breakExtra;
		textJustificationCount = breakCount;
	}

	public int getTextJustificationExtra() {
		return textJustificationExtra;
	}

	public int getTextJustificationCount() {
		return textJustificationCount;
	}

	public AwtBrush getBrush() {
		return brush;
	}

	public void setBrush(AwtBrush brush) {
		this.brush = brush;
	}

	public AwtPen getPen() {
		return pen;
	}

	public void setPen(AwtPen pen) {
		this.pen = pen;
	}

	public AwtFont getFont() {
		return font;
	}

	public void setFont(AwtFont font) {
		this.font = font;
	}
}

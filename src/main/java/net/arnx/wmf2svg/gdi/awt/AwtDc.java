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
	private boolean scaleWindowOrigin = true;
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
	private int icmMode;
	private byte[] colorAdjustment;
	private byte[] icmProfile;
	private AwtBrush brush;
	private AwtPen pen;
	private AwtFont font;

	public void setDpi(int dpi) {
		this.dpi = dpi > 0 ? dpi : 1440;
	}

	public double toAbsoluteX(double x) {
		return (vx + vox) * dpi / CSS_DPI + (scaleWindowOrigin
				? ((mx * x - (wx + wox)) / wsx) * viewportScaleX()
				: (mx * x / wsx) * viewportScaleX() - (wx + wox));
	}

	public double toAbsoluteY(double y) {
		return (vy + voy) * dpi / CSS_DPI + (scaleWindowOrigin
				? ((my * y - (wy + woy)) / wsy) * viewportScaleY()
				: (my * y / wsy) * viewportScaleY() - (wy + woy));
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
			old.x = effectiveWindowX();
			old.y = effectiveWindowY();
		}
		wx = x;
		wy = y;
		wox = 0;
		woy = 0;
	}

	public int getWindowX() {
		return effectiveWindowX();
	}

	public int getWindowY() {
		return effectiveWindowY();
	}

	public int getWindowWidth() {
		return ww;
	}

	public int getWindowHeight() {
		return wh;
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
			old.x = effectiveWindowX();
			old.y = effectiveWindowY();
		}
		wox += x;
		woy += y;
	}

	public void scaleWindowExtEx(int x, int xd, int y, int yd, Size old) {
		if (old != null) {
			old.width = effectiveWindowWidth();
			old.height = effectiveWindowHeight();
		}
		wsx = (wsx * x) / xd;
		wsy = (wsy * y) / yd;
	}

	private int effectiveWindowX() {
		return wx + wox;
	}

	private int effectiveWindowY() {
		return wy + woy;
	}

	private int effectiveWindowWidth() {
		return (int) Math.round(ww * wsx);
	}

	private int effectiveWindowHeight() {
		return (int) Math.round(wh * wsy);
	}

	public void setViewportOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = effectiveViewportX();
			old.y = effectiveViewportY();
		}
		vx = x;
		vy = y;
		vox = 0;
		voy = 0;
	}

	public int getViewportWidth() {
		return vw;
	}

	public int getViewportHeight() {
		return vh;
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
			old.x = effectiveViewportX();
			old.y = effectiveViewportY();
		}
		vox += x;
		voy += y;
	}

	public void scaleViewportExtEx(int x, int xd, int y, int yd, Size old) {
		if (old != null) {
			old.width = effectiveViewportWidth();
			old.height = effectiveViewportHeight();
		}
		vsx = (vsx * x) / xd;
		vsy = (vsy * y) / yd;
	}

	private int effectiveViewportX() {
		return vx + vox;
	}

	private int effectiveViewportY() {
		return vy + voy;
	}

	private int effectiveViewportWidth() {
		return (int) Math.round(vw * vsx);
	}

	private int effectiveViewportHeight() {
		return (int) Math.round(vh * vsy);
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

	public void setScaleWindowOrigin(boolean scaleWindowOrigin) {
		this.scaleWindowOrigin = scaleWindowOrigin;
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
			AwtDc clone = (AwtDc) super.clone();
			clone.colorAdjustment = copy(colorAdjustment);
			clone.icmProfile = copy(icmProfile);
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	private byte[] copy(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		byte[] copy = new byte[bytes.length];
		System.arraycopy(bytes, 0, copy, 0, bytes.length);
		return copy;
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

	public int getRelAbs() {
		return relAbsMode;
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

	public int getStretchBltMode() {
		return stretchBltMode;
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

	public long getLayout() {
		return layout;
	}

	public void setMapperFlags(long mapperFlags) {
		this.mapperFlags = mapperFlags;
	}

	public long getMapperFlags() {
		return mapperFlags;
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
		this.colorAdjustment = copy(colorAdjustment);
	}

	public byte[] getColorAdjustment() {
		return copy(colorAdjustment);
	}

	public void setICMProfile(byte[] profileName) {
		icmProfile = copy(profileName);
	}

	public byte[] getICMProfile() {
		return copy(icmProfile);
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

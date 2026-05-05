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

import org.w3c.dom.Text;

import net.arnx.wmf2svg.gdi.*;

/**
 * @author Hidekatsu Izuno
 */
class SvgPen extends SvgObject implements GdiPen {

	private int style;
	private int width;
	private int color;

	public SvgPen(SvgGdi gdi, int style, int width, int color) {

		super(gdi);
		this.style = style;
		this.width = (width > 0) ? width : 1;
		this.color = color;
	}

	public int getStyle() {
		return style;
	}

	public int getWidth() {
		return width;
	}

	public int getColor() {
		return color;
	}

	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + color;
		result = PRIME * result + style;
		result = PRIME * result + width;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SvgPen other = (SvgPen) obj;
		if (color != other.color)
			return false;
		if (style != other.style)
			return false;
		if (width != other.width)
			return false;
		return true;
	}

	public Text createTextNode(String id) {
		return getGDI().getDocument().createTextNode("." + id + " { " + toString() + " }\n");
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		int penStyle = style & PS_STYLE_MASK;

		if (penStyle == PS_NULL) {
			buffer.append("stroke: none; ");
		} else {
			// stroke
			buffer.append("stroke: " + toColor(color) + "; ");

			// stroke-width
			buffer.append("stroke-width: " + toStrokeWidth() + "; ");

			// stroke-linejoin
			buffer.append("stroke-linecap: round; ");
			buffer.append("stroke-linejoin: round; ");

			// stroke-dasharray
			if (PS_DASH <= penStyle && penStyle <= PS_DASHDOTDOT) {
				buffer.append("stroke-dasharray: ");
				switch (penStyle) {
					case PS_DASH :
						buffer.append("" + toStrokePatternSize(18) + "," + toStrokePatternSize(6));
						break;
					case PS_DOT :
						buffer.append("" + toStrokePatternSize(3) + "," + toStrokePatternSize(3));
						break;
					case PS_DASHDOT :
						buffer.append("" + toStrokePatternSize(9) + "," + toStrokePatternSize(3) + ","
								+ toStrokePatternSize(3) + "," + toStrokePatternSize(3));
						break;
					case PS_DASHDOTDOT :
						buffer.append("" + toStrokePatternSize(9) + "," + toStrokePatternSize(3) + ","
								+ toStrokePatternSize(3) + "," + toStrokePatternSize(3) + "," + toStrokePatternSize(3)
								+ "," + toStrokePatternSize(3));
						break;
				}
				buffer.append("; ");
			}
		}

		if (buffer.length() > 0)
			buffer.setLength(buffer.length() - 1);
		return buffer.toString();
	}

	private double toStrokePatternSize(int value) {
		return getGDI().getDC().toStrokeWidth(value * Math.max(width, 1));
	}

	private double toStrokeWidth() {
		if ((style & PS_DEVICE_WIDTH) != 0) {
			return Math.max(1.0, width);
		}
		return Math.max(getGDI().getDC().toStrokeWidth(width), getGDI().getPlaceableHorizontalPixelWidth());
	}
}

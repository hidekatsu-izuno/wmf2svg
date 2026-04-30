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

import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiUtils;

class AwtFont implements GdiFont {
	private final int height;
	private final int width;
	private final int escapement;
	private final int orientation;
	private final int weight;
	private final boolean italic;
	private final boolean underline;
	private final boolean strikeout;
	private final int charset;
	private final int outPrecision;
	private final int clipPrecision;
	private final int quality;
	private final int pitchAndFamily;
	private final String faceName;

	AwtFont(int height, int width, int escapement, int orientation, int weight, boolean italic, boolean underline,
			boolean strikeout, int charset, int outPrecision, int clipPrecision, int quality, int pitchAndFamily,
			byte[] faceName) {
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

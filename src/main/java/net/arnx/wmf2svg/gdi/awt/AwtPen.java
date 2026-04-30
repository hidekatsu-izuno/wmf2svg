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

import net.arnx.wmf2svg.gdi.GdiPen;

class AwtPen implements GdiPen {
	private final int style;
	private final int width;
	private final int color;

	AwtPen(int style, int width, int color) {
		this.style = style;
		this.width = width;
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
}

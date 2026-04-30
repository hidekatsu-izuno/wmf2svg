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
package net.arnx.wmf2svg.gdi;

/**
 * @author Hidekatsu Izuno
 */
public interface GdiPen extends GdiObject {
	public static final int PS_SOLID = 0;
	public static final int PS_DASH = 1;
	public static final int PS_DOT = 2;
	public static final int PS_DASHDOT = 3;
	public static final int PS_DASHDOTDOT = 4;
	public static final int PS_NULL = 5;
	public static final int PS_INSIDEFRAME = 6;
	public static final int PS_STYLE_MASK = 0x0000000F;
	public static final int PS_TYPE_MASK = 0x000F0000;
	public static final int PS_GEOMETRIC = 0x00010000;
	public static final int PS_DEVICE_WIDTH = 0x40000000;

	public int getStyle();
	public int getWidth();
	public int getColor();
}

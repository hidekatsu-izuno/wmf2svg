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

import net.arnx.wmf2svg.gdi.GdiPalette;

class AwtPalette implements GdiPalette {
	private int[] entries;

	AwtPalette(int[] entries) {
		this.entries = entries != null ? entries.clone() : new int[0];
	}

	public int getVersion() {
		return 0x300;
	}

	public int[] getEntries() {
		return entries;
	}

	void setEntries(int startIndex, int[] entries) {
		if (entries == null) {
			return;
		}
		int required = startIndex + entries.length;
		if (this.entries.length < required) {
			int[] resized = new int[required];
			System.arraycopy(this.entries, 0, resized, 0, this.entries.length);
			this.entries = resized;
		}
		System.arraycopy(entries, 0, this.entries, startIndex, entries.length);
	}

	void resize(int size) {
		if (size < 0 || size == entries.length) {
			return;
		}
		int[] resized = new int[size];
		System.arraycopy(entries, 0, resized, 0, Math.min(entries.length, resized.length));
		entries = resized;
	}
}

/*
 * Copyright 2026 Hidekatsu Izuno
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
package net.arnx.wmf2svg.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FontUtil {
	private static final Logger LOG = Logger.getLogger(FontUtil.class.getName());

	private FontUtil() {
	}

	public static int registerFonts(File dir) throws IOException {
		if (dir == null || !dir.isDirectory()) {
			throw new FileNotFoundException("font directory not found: " + dir);
		}

		File[] files = dir.listFiles();
		if (files == null) {
			return 0;
		}

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		int registered = 0;
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (!file.isFile() || !isFontFile(file)) {
				continue;
			}
			try {
				Font font = Font.createFont(fontType(file), file);
				if (ge.registerFont(font)) {
					registered++;
				}
			} catch (FontFormatException e) {
				LOG.log(Level.WARNING, "Skipping invalid font: " + file, e);
			}
		}
		return registered;
	}

	public static boolean isFontFile(File file) {
		String name = file.getName().toLowerCase(Locale.ROOT);
		return name.endsWith(".ttf") || name.endsWith(".ttc") || name.endsWith(".otf") || name.endsWith(".pfa")
				|| name.endsWith(".pfb");
	}

	public static int fontType(File file) {
		String name = file.getName().toLowerCase(Locale.ROOT);
		return name.endsWith(".pfa") || name.endsWith(".pfb") ? Font.TYPE1_FONT : Font.TRUETYPE_FONT;
	}
}

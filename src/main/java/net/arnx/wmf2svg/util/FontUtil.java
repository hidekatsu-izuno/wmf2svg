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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FontUtil {
	private static final Logger LOG = Logger.getLogger(FontUtil.class.getName());
	private static final String FONT_PROPERTIES_RESOURCE = "/net/arnx/wmf2svg/gdi/svg/SvgGdi.properties";
	private static final String ALTERNATIVE_FONT_PREFIX = "alternative-font.";
	private static final String FONT_CHARSET_PREFIX = "font-charset.";
	private static final String FONT_EMHEIGHT_PREFIX = "font-emheight.";
	private static final Properties FONT_PROPERTIES = loadFontProperties();

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

	public static String fontCharset(String fontFamily) {
		return fontProperty(FONT_CHARSET_PREFIX, fontFamily);
	}

	public static String fontEmHeight(String fontFamily) {
		return fontProperty(FONT_EMHEIGHT_PREFIX, fontFamily);
	}

	public static List<String> alternativeFonts(String fontFamily) {
		List<String> alternatives = new ArrayList<String>();
		String altfont = fontProperty(ALTERNATIVE_FONT_PREFIX, fontFamily);
		if (altfont == null || altfont.length() == 0) {
			return alternatives;
		}

		String[] names = altfont.split(",");
		for (int i = 0; i < names.length; i++) {
			String name = names[i].trim();
			if (name.length() != 0) {
				alternatives.add(name);
			}
		}
		return alternatives;
	}

	private static String fontProperty(String prefix, String fontFamily) {
		if (fontFamily == null) {
			return null;
		}

		String value = FONT_PROPERTIES.getProperty(prefix + fontFamily);
		if (value != null) {
			return value;
		}

		for (String key : FONT_PROPERTIES.stringPropertyNames()) {
			if (key.regionMatches(true, 0, prefix, 0, prefix.length())
					&& key.substring(prefix.length()).equalsIgnoreCase(fontFamily)) {
				return FONT_PROPERTIES.getProperty(key);
			}
		}
		return null;
	}

	private static Properties loadFontProperties() {
		Properties props = new Properties();
		InputStream in = null;
		try {
			in = FontUtil.class.getResourceAsStream(FONT_PROPERTIES_RESOURCE);
			if (in == null) {
				throw new IllegalStateException("missing resource: " + FONT_PROPERTIES_RESOURCE);
			}
			props.load(in);
			return props;
		} catch (IOException e) {
			throw new IllegalStateException("properties format error: " + FONT_PROPERTIES_RESOURCE, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// no handle
				}
			}
		}
	}
}

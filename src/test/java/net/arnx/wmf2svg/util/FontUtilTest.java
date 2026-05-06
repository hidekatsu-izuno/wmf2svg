package net.arnx.wmf2svg.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

public class FontUtilTest {
	@Test
	public void testFontFileExtensionDetection() {
		assertTrue(FontUtil.isFontFile(new File("Arial.ttf")));
		assertTrue(FontUtil.isFontFile(new File("collection.TTC")));
		assertTrue(FontUtil.isFontFile(new File("font.otf")));
		assertTrue(FontUtil.isFontFile(new File("font.pfb")));
		assertFalse(FontUtil.isFontFile(new File("font.txt")));
	}

	@Test
	public void testFontProperties() {
		List<String> alternatives = FontUtil.alternativeFonts("Fixedsys");
		assertEquals(2, alternatives.size());
		assertEquals("Consolas", alternatives.get(0));
		assertEquals("monospace", alternatives.get(1));
		assertEquals(alternatives, FontUtil.alternativeFonts("fixedsys"));
		assertEquals("2", FontUtil.fontCharset("Symbol"));
		assertEquals("0.854045037531276", FontUtil.fontEmHeight("Consolas"));
		assertTrue(FontUtil.alternativeFonts("No Such Font").isEmpty());
	}

	@Test
	public void testRegisterFontsIgnoresNonFontFiles() throws Exception {
		File dir = new File("target/test-fontdir-" + System.nanoTime());
		assertTrue(dir.mkdirs());
		try {
			assertTrue(new File(dir, "readme.txt").createNewFile());
			assertEquals(0, FontUtil.registerFonts(dir));
		} finally {
			deleteRecursively(dir);
		}
	}

	@Test(expected = FileNotFoundException.class)
	public void testRegisterFontsRejectsMissingDirectory() throws Exception {
		FontUtil.registerFonts(new File("target/missing-fontdir-" + System.nanoTime()));
	}

	private void deleteRecursively(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		File[] children = file.listFiles();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				deleteRecursively(children[i]);
			}
		}
		file.delete();
	}
}

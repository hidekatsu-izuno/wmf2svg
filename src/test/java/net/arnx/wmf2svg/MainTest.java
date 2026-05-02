package net.arnx.wmf2svg;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import net.arnx.wmf2svg.util.FontUtil;

public class MainTest {
	@Test
	public void testMain() {
		System.setProperty("java.util.logging.config.file", "./logging.properties");

		File dir = new File("../wmf-testcase/data/src");
		if (!dir.exists()) {
			dir = new File("./etc/data/src");
		}
		if (!dir.exists()) {
			System.out.println("Test data directory ./etc/data/src does not exist, skipping test.");
			return;
		}
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.getName().toLowerCase().endsWith(".wmf");
			}
		});

		if (files == null || files.length == 0) {
			System.out.println("No WMF files found in ./etc/data/src, skipping test.");
			return;
		}

		File outdir = new File("./etc/data/dst");
		outdir.mkdirs();

		for (int i = 0; i < files.length; i++) {
			String wmf = files[i].getAbsolutePath();
			String baseName = files[i].getName().substring(0, files[i].getName().length() - 4);
			String svg = new File(outdir, baseName + ".svg").getAbsolutePath();
			String png = new File(outdir, baseName + ".png").getAbsolutePath();
			System.out.println(wmf + " transforming...");
			Main.main(new String[]{"-debug", "-replace-symbol-font", wmf, svg});
			Main.main(new String[]{"-replace-symbol-font", wmf, png});
		}
	}

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

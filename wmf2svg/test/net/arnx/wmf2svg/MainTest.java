package net.arnx.wmf2svg;

import java.io.File;
import java.io.FileFilter;

import org.junit.Test;

public class MainTest {
	@Test
	public void testMain() {
		System.setProperty("java.util.logging.config.file", "./logging.properties");

		File dir = new File(System.getProperty("user.home"), "home/wmf2svg");
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.getName().toLowerCase().endsWith(".wmf");
			}
		});

		for (int i = 0; i < files.length; i++) {
			String wmf = files[i].getAbsolutePath();
			String svg = wmf.substring(0, wmf.length() - 4) + ".svg";
			System.out.println(wmf + " transforming...");
			Main.main(new String[] {"-debug", "-replace-symbol-font", wmf, svg});
		}
	}
}

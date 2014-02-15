package net.arnx.wmf2svg;

import java.io.File;
import java.io.FileFilter;

import junit.framework.TestCase;

public class MainTest extends TestCase {
	/*
	 * TestCase for 'net.arnx.wmf2svg.Main.main(String[])'
	 */
	public void testMain() {
		System.setProperty("java.util.logging.config.file", "./logging.properties");
		
		File dir = new File(System.getProperty("user.home"), "Documents/wmf2svg");
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.getName().toLowerCase().endsWith("bols.wmf");
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

package net.arnx.wmf2svg;

import java.io.File;
import java.io.FileFilter;

import org.junit.Test;

public class MainTest {
	@Test
	public void testMain() {
		System.setProperty("java.util.logging.config.file", "./logging.properties");

		File dir = new File("./etc/data/src");
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.getName().toLowerCase().endsWith(".wmf");
			}
		});

		File outdir = new File("./etc/data/dst");
		outdir.mkdirs();

		for (int i = 0; i < files.length; i++) {
			String wmf = files[i].getAbsolutePath();
			String svg = new File(outdir, files[i].getName().substring(0, files[i].getName().length() - 4) + ".svg").getAbsolutePath();
			System.out.println(wmf + " transforming...");
			Main.main(new String[] {"-debug", "-replace-symbol-font", wmf, svg});
		}
	}
}

package net.arnx.wmf2svg;

import java.io.File;
import java.io.FileFilter;

import org.junit.Test;

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
			String svg = new File(outdir, files[i].getName().substring(0, files[i].getName().length() - 4) + ".svg").getAbsolutePath();
			System.out.println(wmf + " transforming...");
			Main.main(new String[] {"-debug", wmf, svg});
		}
	}
}

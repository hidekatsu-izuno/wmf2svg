package net.arnx.wmf2svg;

import java.io.File;
import java.io.FileFilter;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MainTest {
	@Test
	public void testMain() {
		System.setProperty("java.util.logging.config.file", "./logging.properties");

		File dir = new File("../wmf-testcase/data/src");
		if (!dir.exists()) {
			System.out.println("Test data directory ../wmf-testcase/data/src does not exist, skipping test.");
			return;
		}
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				String name = file.getName().toLowerCase();
				return name.endsWith(".wmf") || name.endsWith(".emf");
			}
		});

		if (files == null || files.length == 0) {
			System.out.println("No wmf/emf files found in ../wmf-testcase/data/src, skipping test.");
			return;
		}

		File outdir = new File("../wmf-testcase/data/dst");
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

}

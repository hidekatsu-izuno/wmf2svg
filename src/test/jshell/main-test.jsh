import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import net.arnx.wmf2svg.Main;

System.setProperty("java.util.logging.config.file", "./logging.properties");

File dir = new File("../wmf-testcase/data/src");
if (!dir.exists()) {
	System.out.println("Test data directory ../wmf-testcase/data/src does not exist, skipping script.");
} else {
	File[] files = dir.listFiles(new FileFilter() {
		public boolean accept(File file) {
			String name = file.getName().toLowerCase();
			return name.endsWith(".wmf") || name.endsWith(".emf");
		}
	});

	if (files == null || files.length == 0) {
		System.out.println("No wmf/emf files found in ../wmf-testcase/data/src, skipping script.");
	} else {
		Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

		File outdir = new File("../wmf-testcase/data/dst");
		outdir.mkdirs();

		for (File file : files) {
			String wmf = file.getAbsolutePath();
			String baseName = file.getName().substring(0, file.getName().length() - 4);
			String svg = new File(outdir, baseName + ".svg").getAbsolutePath();
			String png = new File(outdir, baseName + ".png").getAbsolutePath();
			System.out.println(wmf + " transforming...");
			Main.main(new String[]{"-debug", "-replace-symbol-font", wmf, svg});
			Main.main(new String[]{"-replace-symbol-font", wmf, png});
		}
	}
}

/exit

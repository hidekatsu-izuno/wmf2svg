package net.arnx.wmf2svg;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class Issue22Test {
	@Test
	public void teacherOutlinePolygonsBecomeTransparent() throws Exception {
		File dataDir = new File("../wmf-testcase/data/src");
		File wmf = new File(dataDir, "TEACHER1.WMF");
		File outDir = new File("target/test-output");
		File svg = new File(outDir, "TEACHER1.svg");
		outDir.mkdirs();

		Main.main(new String[] {wmf.getAbsolutePath(), svg.getAbsolutePath()});

		String xml = new String(Files.readAllBytes(svg.toPath()), StandardCharsets.UTF_8);
		assertTrue(xml.contains(
				"<polygon class=\"pen1 brush0\" fill=\"none\" points=\"393,1645 418,1565 438,1478 454,1388 466,1294 473,1200 478,1105 479,1013 478,923 449,950 424,982 403,1019 386,1061 373,1106 363,1154 356,1204 352,1256 351,1309 352,1362 355,1415 360,1466 366,1515 374,1562 383,1605 393,1645\"/>"));
	}
}

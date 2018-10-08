package net.arnx.wmf2svg.gdi.svg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.junit.Test;

import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiPen;

public class SvgGdiTest {
	@Test
	public void testPie() throws Exception {
		SvgGdi gdi = new SvgGdi();
		gdi.placeableHeader(0, 0, 9000, 9000, 1440);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(200, 200, null);
		gdi.setBkMode(1);
		GdiBrush brush1 = gdi.createBrushIndirect(1, 0, 0);
		gdi.selectObject(brush1);
		gdi.rectangle(10, 10, 110, 110);
		GdiPen pen2 = gdi.createPenIndirect(0, 1, 0x0000FF);
		gdi.selectObject(pen2);
		gdi.pie(10, 10, 110, 110, 60, 10, 110, 60);
		gdi.footer();

		File file = new File(System.getProperty("user.home") + "/wmf2svg", "pie_test.svg");
		file.getParentFile().mkdirs();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			gdi.write(out);
		} finally {
			out.close();
		}
	}
}

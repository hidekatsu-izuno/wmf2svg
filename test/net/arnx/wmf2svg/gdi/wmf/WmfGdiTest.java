package net.arnx.wmf2svg.gdi.wmf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import net.arnx.wmf2svg.Main;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.GdiUtils;

public class WmfGdiTest {
	@Test
	public void testEllipse() throws IOException {
		WmfGdi gdi = new WmfGdi();
		gdi.placeableHeader(0, 0, 9000, 4493, 1440);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(200, 200, null);
		gdi.setBkMode(1);
		GdiBrush brush1 = gdi.createBrushIndirect(1, 0, 0);
		gdi.selectObject(brush1);
		gdi.rectangle(0, 0, 200, 200);
		gdi.moveToEx(10, 10, null);
		gdi.lineTo(100, 100);
		gdi.footer();

		File file = new File(System.getProperty("user.home") + "/wmf2svg", "ellipse_test.wmf");
		file.getParentFile().mkdirs();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			gdi.write(out);
		} finally {
			out.close();
		}

		convert(file);
	}

	@Test
	public void testExtTextOut() throws IOException {
		WmfGdi gdi = new WmfGdi();
		gdi.placeableHeader(0, 0, 500, 500, 96);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(500, 500, null);
		gdi.setBkMode(1);

		GdiBrush brush1 = gdi.createBrushIndirect(1, 0, 0);
		gdi.selectObject(brush1);
		gdi.rectangle(0, 0, 200, 72);
		gdi.moveToEx(10, 10, null);
		gdi.lineTo(100, 100);

		GdiFont font1 = gdi.createFontIndirect(72, 0, 0, 0, GdiFont.FW_NORMAL, false, false, false, GdiFont.ANSI_CHARSET,
				GdiFont.OUT_DEFAULT_PRECIS,
				GdiFont.CLIP_DEFAULT_PRECIS,
				GdiFont.DEFAULT_QUALITY,
				GdiFont.DEFAULT_PITCH,
				"Arial".getBytes(GdiUtils.getCharset(GdiFont.ANSI_CHARSET)));
		gdi.selectObject(font1);
		gdi.extTextOut(0, 0, 0, null,
				"ABCdefg".getBytes(GdiUtils.getCharset(font1.getCharset())),
				new int[] {30, 30, 30, 30, 30, 30, 20});

		gdi.footer();

		File file = new File(System.getProperty("user.home") + "/wmf2svg", "font_test.wmf");
		file.getParentFile().mkdirs();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			gdi.write(out);
		} finally {
			out.close();
		}

		convert(file);
	}
	private void convert(File file) {
		System.setProperty("java.util.logging.config.file", "./logging.properties");

		String name = file.getAbsolutePath();
		name = name.substring(0, name.length() - 4);
		System.out.println(name + " transforming...");
		Main.main(new String[] {"-debug", name + ".wmf", name + ".svg"});
	}

	@Test
	public void testPie() throws Exception {
		WmfGdi gdi = new WmfGdi();
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

		File file = new File(System.getProperty("user.home") + "/wmf2svg", "pie_test.wmf");
		file.getParentFile().mkdirs();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			gdi.write(out);
		} finally {
			out.close();
		}
	}

}

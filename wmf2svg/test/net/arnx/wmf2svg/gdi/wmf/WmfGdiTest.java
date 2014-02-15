package net.arnx.wmf2svg.gdi.wmf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import net.arnx.wmf2svg.Main;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiUtils;

import junit.framework.TestCase;

public class WmfGdiTest extends TestCase {
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
		
		File file = new File(System.getProperty("user.home") + "/My Documents/wmf2svg", "ellipse_test.wmf");
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			gdi.write(out);
		} finally {
			out.close();
		}
		
		convert(file);
	}
	
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
		
		File file = new File(System.getProperty("user.home") + "/My Documents/wmf2svg", "font_test.wmf");
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

}

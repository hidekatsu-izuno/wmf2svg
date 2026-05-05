package net.arnx.wmf2svg.gdi.wmf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import net.arnx.wmf2svg.Main;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.GdiRegion;
import net.arnx.wmf2svg.gdi.GdiUtils;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Size;
import net.arnx.wmf2svg.gdi.svg.SvgGdi;

public class WmfGdiTest {
	@Test
	public void testCommentWritesEscapeRecord() throws Exception {
		WmfGdi gdi = new WmfGdi();
		gdi.header();
		byte[] escape = new byte[]{0x0F, 0x00, 0x04, 0x00, 't', 'e', 's', 't'};
		gdi.comment(escape);

		byte[] record = findRecord(write(gdi), WmfConstants.META_ESCAPE);

		assertEquals(WmfConstants.META_ESCAPE, readUint16(record, 4));
		assertEquals(0x000F, readUint16(record, 6));
		assertEquals(4, readUint16(record, 8));
		assertEquals('t', record[10] & 0xFF);
		assertEquals('e', record[11] & 0xFF);
		assertEquals('s', record[12] & 0xFF);
		assertEquals('t', record[13] & 0xFF);
	}

	@Test
	public void testSelectClipRgnAcceptsNull() throws Exception {
		WmfGdi gdi = new WmfGdi();
		gdi.header();
		gdi.selectClipRgn(null);

		byte[] record = findRecord(write(gdi), WmfConstants.META_SELECTCLIPREGION);

		assertEquals(0, readUint16(record, 6));
	}

	@Test
	public void testExtSelectClipRgnCopyWritesSelectClipRegion() throws Exception {
		WmfGdi gdi = new WmfGdi();
		gdi.header();
		GdiRegion region = gdi.createRectRgn(1, 2, 3, 4);

		assertEquals(GdiRegion.SIMPLEREGION, gdi.extSelectClipRgn(region, GdiRegion.RGN_COPY));

		byte[] record = findRecord(write(gdi), WmfConstants.META_SELECTCLIPREGION);
		assertEquals(2, readUint16(record, 6));
	}

	@Test
	public void testRestoreDcRestoresWriterState() throws Exception {
		WmfGdi gdi = new WmfGdi();
		gdi.setTextAlign(WmfGdi.TA_RIGHT | WmfGdi.TA_UPDATECP);
		gdi.seveDC();
		gdi.setTextAlign(WmfGdi.TA_LEFT | WmfGdi.TA_UPDATECP);
		gdi.restoreDC(-1);
		gdi.extTextOut(100, 20, 0, null, new byte[]{'A', 'B'}, new int[]{10, 10});

		Point old = new Point(0, 0);
		gdi.moveToEx(0, 0, old);

		assertEquals(110, old.x);
		assertEquals(20, old.y);
	}

	@Test
	public void testViewportAndWindowOldValuesAreUpdated() {
		WmfGdi gdi = new WmfGdi();
		Point oldPoint = new Point(0, 0);
		Size oldSize = new Size(0, 0);

		gdi.setWindowOrgEx(10, 20, null);
		gdi.setWindowOrgEx(30, 40, oldPoint);
		assertEquals(10, oldPoint.x);
		assertEquals(20, oldPoint.y);

		gdi.setViewportExtEx(100, 200, null);
		gdi.setViewportExtEx(300, 400, oldSize);
		assertEquals(100, oldSize.width);
		assertEquals(200, oldSize.height);
	}

	@Test
	public void testScaleExtUpdatesOldValuesAndState() {
		WmfGdi gdi = new WmfGdi();
		Size oldSize = new Size(0, 0);

		gdi.setWindowExtEx(100, 200, null);
		gdi.scaleWindowExtEx(2, 1, 3, 2, oldSize);
		assertEquals(100, oldSize.width);
		assertEquals(200, oldSize.height);
		gdi.setWindowExtEx(1, 1, oldSize);
		assertEquals(200, oldSize.width);
		assertEquals(300, oldSize.height);

		gdi.setViewportExtEx(300, 400, null);
		gdi.scaleViewportExtEx(1, 3, 3, 2, oldSize);
		assertEquals(300, oldSize.width);
		assertEquals(400, oldSize.height);
		gdi.setViewportExtEx(1, 1, oldSize);
		assertEquals(100, oldSize.width);
		assertEquals(600, oldSize.height);
	}

	@Test
	public void testCreateRectRgnWritesParserCompatibleRecord() throws Exception {
		WmfGdi gdi = new WmfGdi();
		gdi.header();
		gdi.createRectRgn(10, 20, 30, 40);

		byte[] record = findRecord(write(gdi), WmfConstants.META_CREATEREGION);

		assertEquals(28, record.length);
		assertEquals(14, readUint32(record, 0));
		assertEquals(WmfConstants.META_CREATEREGION, readUint16(record, 4));
		assertEquals(0x0006, readUint16(record, 8));
		assertEquals(10, readInt16(record, 20));
		assertEquals(20, readInt16(record, 22));
		assertEquals(30, readInt16(record, 24));
		assertEquals(40, readInt16(record, 26));
	}

	@Test
	public void testRectRegionRoundTripsToSvg() throws Exception {
		WmfGdi gdi = new WmfGdi();
		gdi.placeableHeader(0, 0, 100, 100, 100);
		gdi.header();
		gdi.setMapMode(WmfGdi.MM_ANISOTROPIC);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(100, 100, null);
		GdiRegion region = gdi.createRectRgn(10, 20, 30, 40);
		GdiBrush brush = gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x0000FF, 0);
		gdi.fillRgn(region, brush);

		SvgGdi svgGdi = new SvgGdi();
		new WmfParser().parse(new ByteArrayInputStream(write(gdi)), svgGdi);
		ByteArrayOutputStream svg = new ByteArrayOutputStream();
		svgGdi.write(svg);
		String text = new String(svg.toByteArray(), "UTF-8");

		assertTrue(text.indexOf("height=\"20\" width=\"20\" x=\"10\" y=\"20\"") >= 0);
	}

	@Test
	public void testParserDrawsTruncatedExtTextOutTextWithoutDx() throws Exception {
		WmfGdi gdi = new WmfGdi();
		gdi.header();
		gdi.extTextOut(10, 20, 0, null, "Title".getBytes("US-ASCII"), new int[]{8, 8, 8, 8, 8});
		gdi.footer();

		byte[] wmf = write(gdi);
		int offset = findRecordOffset(wmf, WmfConstants.META_EXTTEXTOUT);
		int count = readUint16(wmf, offset + 10);
		int textEnd = offset + 14 + count + (count % 2);
		byte[] truncated = Arrays.copyOf(wmf, textEnd);

		SvgGdi svgGdi = new SvgGdi();
		new WmfParser().parse(new ByteArrayInputStream(truncated), svgGdi);

		ByteArrayOutputStream svg = new ByteArrayOutputStream();
		svgGdi.write(svg);
		String text = new String(svg.toByteArray(), "UTF-8");
		assertTrue(text.indexOf(">Title</text>") >= 0);
	}

	@Test
	public void testCreateObjectReusesDeletedObjectSlot() throws Exception {
		WmfGdi gdi = new WmfGdi();
		gdi.header();
		GdiBrush first = gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x000000, 0);
		int firstID = ((WmfObject) first).getID();
		gdi.deleteObject(first);

		GdiBrush second = gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x0000FF, 0);
		gdi.selectObject(second);

		assertEquals(firstID, ((WmfObject) second).getID());
		byte[] record = findLastRecord(write(gdi), WmfConstants.META_SELECTOBJECT);
		assertEquals(firstID, readUint16(record, 6));
	}

	@Test
	public void testPlaceableViewportScalesChangedWindowExt() throws Exception {
		WmfGdi gdi = new WmfGdi();
		gdi.placeableHeader(0, 0, 1000, 1000, 1000);
		gdi.header();
		gdi.setMapMode(8);
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(1000, 1000, null);
		gdi.setWindowOrgEx(10, 20, null);
		gdi.setWindowExtEx(100, 200, null);
		gdi.rectangle(20, 40, 30, 60);
		gdi.footer();

		ByteArrayOutputStream wmf = new ByteArrayOutputStream();
		gdi.write(wmf);

		SvgGdi svgGdi = new SvgGdi();
		new WmfParser().parse(new ByteArrayInputStream(wmf.toByteArray()), svgGdi);

		ByteArrayOutputStream svg = new ByteArrayOutputStream();
		svgGdi.write(svg);
		String text = new String(svg.toByteArray(), "UTF-8");

		assertTrue(text.indexOf("height=\"100\" width=\"100\" x=\"100\" y=\"100\"") >= 0);
	}

	@Test
	public void testParsedPenWidthUsesLogicalUnits() throws Exception {
		WmfGdi gdi = new WmfGdi();
		gdi.placeableHeader(0, 0, 222, 41, 96);
		gdi.header();
		gdi.setWindowOrgEx(0, 0, null);
		gdi.setWindowExtEx(4416, 736, null);
		GdiPen pen = gdi.createPenIndirect(0, 16, 0);
		gdi.selectObject(pen);
		gdi.moveToEx(1371, 416, null);
		gdi.lineTo(1666, 416);
		gdi.footer();

		ByteArrayOutputStream wmf = new ByteArrayOutputStream();
		gdi.write(wmf);

		SvgGdi svgGdi = new SvgGdi();
		new WmfParser().parse(new ByteArrayInputStream(wmf.toByteArray()), svgGdi);

		ByteArrayOutputStream svg = new ByteArrayOutputStream();
		svgGdi.write(svg);
		String text = new String(svg.toByteArray(), "UTF-8");

		assertTrue(text.indexOf("stroke-width: 1.0;") >= 0);
		assertFalse(text.indexOf("stroke-width: 16.0;") >= 0);
	}

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

		GdiFont font1 = gdi.createFontIndirect(72, 0, 0, 0, GdiFont.FW_NORMAL, false, false, false,
				GdiFont.ANSI_CHARSET, GdiFont.OUT_DEFAULT_PRECIS, GdiFont.CLIP_DEFAULT_PRECIS, GdiFont.DEFAULT_QUALITY,
				GdiFont.DEFAULT_PITCH, "Arial".getBytes(GdiUtils.getCharset(GdiFont.ANSI_CHARSET)));
		gdi.selectObject(font1);
		gdi.extTextOut(0, 0, 0, null, "ABCdefg".getBytes(GdiUtils.getCharset(font1.getCharset())),
				new int[]{30, 30, 30, 30, 30, 30, 20});

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
		Main.main(new String[]{"-debug", name + ".wmf", name + ".svg"});
	}

	private byte[] write(WmfGdi gdi) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		return out.toByteArray();
	}

	private byte[] findRecord(byte[] wmf, int function) {
		byte[] record = findLastRecord(wmf, function, false);
		if (record == null) {
			throw new AssertionError("WMF record not found: " + function);
		}
		return record;
	}

	private byte[] findLastRecord(byte[] wmf, int function) {
		byte[] record = findLastRecord(wmf, function, true);
		if (record == null) {
			throw new AssertionError("WMF record not found: " + function);
		}
		return record;
	}

	private byte[] findLastRecord(byte[] wmf, int function, boolean last) {
		int pos = 18;
		byte[] found = null;
		while (pos + 6 <= wmf.length) {
			int recordSize = readUint32(wmf, pos) * 2;
			if (recordSize < 6 || pos + recordSize > wmf.length) {
				break;
			}
			if (readUint16(wmf, pos + 4) == function) {
				byte[] record = new byte[recordSize];
				System.arraycopy(wmf, pos, record, 0, record.length);
				if (!last) {
					return record;
				}
				found = record;
			}
			pos += recordSize;
		}
		return found;
	}

	private int findRecordOffset(byte[] wmf, int function) {
		int pos = 18;
		while (pos + 6 <= wmf.length) {
			int recordSize = readUint32(wmf, pos) * 2;
			if (recordSize < 6 || pos + recordSize > wmf.length) {
				break;
			}
			if (readUint16(wmf, pos + 4) == function) {
				return pos;
			}
			pos += recordSize;
		}
		throw new AssertionError("WMF record not found: " + function);
	}

	private int readUint16(byte[] data, int pos) {
		return (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8);
	}

	private int readInt16(byte[] data, int pos) {
		int value = readUint16(data, pos);
		return value < 0x8000 ? value : value - 0x10000;
	}

	private int readUint32(byte[] data, int pos) {
		return readUint16(data, pos) | (readUint16(data, pos + 2) << 16);
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

package net.arnx.wmf2svg.gdi.emf;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import net.arnx.wmf2svg.gdi.svg.SvgGdi;

public class EmfParserTest {
	@Test
	public void testHeaderSetsSvgBounds() throws Exception {
		SvgGdi gdi = new SvgGdi();
		new EmfParser().parse(new ByteArrayInputStream(createMinimalEmf()), gdi);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = new String(out.toByteArray(), "UTF-8");

		assertTrue(svg.indexOf("width=\"100\"") >= 0);
		assertTrue(svg.indexOf("height=\"50\"") >= 0);
		assertTrue(svg.indexOf("viewBox=\"0 0 100 50\"") >= 0);
	}

	@Test
	public void testExtTextOutWKeepsUnicodeDxPerCharacter() throws Exception {
		SvgGdi gdi = new SvgGdi();
		new EmfParser().parse(new ByteArrayInputStream(createEmfWithUtf16DxText()), gdi);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = new String(out.toByteArray(), "UTF-8");

		assertTrue(svg.indexOf("x=\"0 14 28 42\"") >= 0);
		assertTrue(svg.indexOf(">徒然なる<") >= 0);
	}

	private static byte[] createMinimalEmf() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeHeader(out);
		writeRecord(out, 14, new byte[12]);
		return out.toByteArray();
	}

	private static byte[] createEmfWithUtf16DxText() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeHeader(out);
		writeFont(out);
		writeRecord(out, 37, intData(1));
		writeUtf16Text(out, "徒然なる", new int[] {14, 14, 14, 14, 14, 14});
		writeRecord(out, 14, new byte[12]);
		return out.toByteArray();
	}

	private static void writeHeader(ByteArrayOutputStream out) {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		writeInt(data, 10);
		writeInt(data, 20);
		writeInt(data, 110);
		writeInt(data, 70);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, 1000);
		writeInt(data, 500);
		writeInt(data, 0x464D4520);
		writeInt(data, 0x00010000);
		writeInt(data, 108);
		writeInt(data, 2);
		writeShort(data, 1);
		writeShort(data, 0);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, 100);
		writeInt(data, 50);
		writeInt(data, 100);
		writeInt(data, 50);
		writeRecord(out, 1, data.toByteArray());
	}

	private static void writeFont(ByteArrayOutputStream out) {
		byte[] data = new byte[96];
		setInt(data, 0, 1);
		setInt(data, 4, 14);
		data[27] = (byte)128;
		byte[] face = "MS PGothic".getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
		System.arraycopy(face, 0, data, 32, face.length);
		writeRecord(out, 82, data);
	}

	private static void writeUtf16Text(ByteArrayOutputStream out, String text, int[] dx) {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, 100);
		writeInt(data, 20);
		writeInt(data, 1);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, text.length());
		writeInt(data, 76);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, 0);
		writeInt(data, 100);
		writeInt(data, 20);
		writeInt(data, 76 + text.length() * 2);
		byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
		data.write(bytes, 0, bytes.length);
		for (int i = 0; i < dx.length; i++) {
			writeInt(data, dx[i]);
		}
		writeRecord(out, 84, data.toByteArray());
	}

	private static byte[] intData(int value) {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		writeInt(data, value);
		return data.toByteArray();
	}

	private static void writeRecord(ByteArrayOutputStream out, int type, byte[] data) {
		writeInt(out, type);
		writeInt(out, data.length + 8);
		out.write(data, 0, data.length);
	}

	private static void writeInt(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		out.write((value >>> 8) & 0xFF);
		out.write((value >>> 16) & 0xFF);
		out.write((value >>> 24) & 0xFF);
	}

	private static void writeShort(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		out.write((value >>> 8) & 0xFF);
	}

	private static void setInt(byte[] data, int pos, int value) {
		data[pos] = (byte)(value & 0xFF);
		data[pos + 1] = (byte)((value >>> 8) & 0xFF);
		data[pos + 2] = (byte)((value >>> 16) & 0xFF);
		data[pos + 3] = (byte)((value >>> 24) & 0xFF);
	}
}

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

	private static byte[] createMinimalEmf() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeHeader(out);
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
}

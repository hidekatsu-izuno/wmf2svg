package net.arnx.wmf2svg.gdi.emf;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

import net.arnx.wmf2svg.gdi.svg.SvgGdi;

public class FulltestEmfGeneratorTest {
	@Test
	public void testFulltestEmfRoundTripsNewEmfGdiRecordsToSvg() throws Exception {
		File emf = new File("target/fulltest-pipeline/fulltest.emf");
		FulltestEmfGenerator.main(new String[] {emf.getPath()});
		byte[] data = Files.readAllBytes(emf.toPath());

		assertTrue(hasRecord(data, EmfConstants.EMR_POLYDRAW));
		assertTrue(hasRecord(data, EmfConstants.EMR_POLYDRAW16));
		assertTrue(hasRecord(data, EmfConstants.EMR_CREATEMONOBRUSH));
		assertTrue(hasRecord(data, EmfConstants.EMR_POLYTEXTOUTA));
		assertTrue(hasRecord(data, EmfConstants.EMR_POLYTEXTOUTW));
		assertTrue(hasRecord(data, EmfConstants.EMR_SMALLTEXTOUT));
		assertTrue(hasRecord(data, EmfConstants.EMR_SETWORLDTRANSFORM));
		assertTrue(hasRecord(data, EmfConstants.EMR_MODIFYWORLDTRANSFORM));

		SvgGdi gdi = new SvgGdi();
		new EmfParser().parse(new ByteArrayInputStream(data), gdi);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		gdi.write(out);
		String svg = new String(out.toByteArray(), "UTF-8");

		assertTrue(svg.indexOf("PolyTextOutA") >= 0);
		assertTrue(svg.indexOf("PolyTextOutW") >= 0);
		assertTrue(svg.indexOf("SmallTextOut") >= 0);
		assertTrue(svg.indexOf("CreateMonoBrush") >= 0);
		assertTrue(svg.indexOf("data:image/png;base64,") >= 0);
	}

	private static boolean hasRecord(byte[] data, int type) {
		int pos = 0;
		while (pos + 8 <= data.length) {
			int recordType = readInt32(data, pos);
			int size = readInt32(data, pos + 4);
			if (recordType == type) {
				return true;
			}
			if (size < 8 || pos + size > data.length) {
				return false;
			}
			pos += size;
		}
		return false;
	}

	private static int readInt32(byte[] data, int pos) {
		return (data[pos] & 0xFF)
				| ((data[pos + 1] & 0xFF) << 8)
				| ((data[pos + 2] & 0xFF) << 16)
				| (data[pos + 3] << 24);
	}
}

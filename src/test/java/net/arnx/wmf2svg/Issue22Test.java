package net.arnx.wmf2svg;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Issue22Test {
	private static final Pattern BRUSH_PATTERN = Pattern.compile("\\.(\\S+)\\s*\\{\\s*fill:\\s*rgb\\(255,255,255\\);\\s*\\}");
	private static final Pattern PEN_PATTERN = Pattern.compile("\\.(\\S+)\\s*\\{\\s*stroke:\\s*none;\\s*\\}");

	@Test
	public void teacherOutlinePolygonsBecomeTransparent() throws Exception {
		File dataDir = new File("../wmf-testcase/data/src");
		File wmf = new File(dataDir, "TEACHER1.WMF");
		File outDir = new File("target/test-output");
		File svg = new File(outDir, "TEACHER1.svg");
		outDir.mkdirs();

		Main.main(new String[] {wmf.getAbsolutePath(), svg.getAbsolutePath()});

		String xml = new String(Files.readAllBytes(svg.toPath()), StandardCharsets.UTF_8);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

		Set<String> whiteBrushes = collectClasses(document, "style", BRUSH_PATTERN);
		Set<String> nullPens = collectClasses(document, "style", PEN_PATTERN);

		NodeList groups = document.getDocumentElement().getElementsByTagName("g");
		Element group = (Element) groups.item(0);
		int fixedPairs = 0;

		for (Node node = group.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (!(node instanceof Element)) {
				continue;
			}
			Element polygon = (Element) node;
			if (!"polygon".equals(polygon.getTagName()) || !isWhiteNullPenPolygon(polygon, whiteBrushes, nullPens)) {
				continue;
			}

			Node next = polygon.getNextSibling();
			while (next != null && !(next instanceof Element)) {
				next = next.getNextSibling();
			}
			if (!(next instanceof Element)) {
				continue;
			}

			Element polyline = (Element) next;
			if (!"polyline".equals(polyline.getTagName())) {
				continue;
			}
			if (!normalizePoints(polygon.getAttribute("points")).equals(normalizePoints(polyline.getAttribute("points")))) {
				continue;
			}

			assertEquals("none", polygon.getAttribute("fill"));
			fixedPairs++;
		}

		assertEquals(true, fixedPairs > 0);
	}

	private static Set<String> collectClasses(Document document, String tagName, Pattern pattern) {
		Set<String> classes = new HashSet<String>();
		NodeList styles = document.getElementsByTagName(tagName);
		for (int i = 0; i < styles.getLength(); i++) {
			String text = styles.item(i).getTextContent();
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				classes.add(matcher.group(1));
			}
		}
		return classes;
	}

	private static boolean isWhiteNullPenPolygon(Element polygon, Set<String> whiteBrushes, Set<String> nullPens) {
		String classAttr = polygon.getAttribute("class");
		if (classAttr == null || classAttr.isEmpty()) {
			return false;
		}

		boolean hasWhiteBrush = false;
		boolean hasNullPen = false;
		for (String className : classAttr.split("\\s+")) {
			if (whiteBrushes.contains(className)) {
				hasWhiteBrush = true;
			}
			if (nullPens.contains(className)) {
				hasNullPen = true;
			}
		}
		return hasWhiteBrush && hasNullPen;
	}

	private static String normalizePoints(String points) {
		String[] values = points.trim().split("\\s+");
		StringBuilder sb = new StringBuilder();
		String prev = null;
		for (String value : values) {
			if (value.equals(prev)) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(value);
			prev = value;
		}
		return sb.toString();
	}
}

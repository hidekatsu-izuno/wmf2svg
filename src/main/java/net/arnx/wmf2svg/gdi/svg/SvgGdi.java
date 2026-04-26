/*
 * Copyright 2007-2012 Hidekatsu Izuno, Shunsuke Mori
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package net.arnx.wmf2svg.gdi.svg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.imageio.ImageIO;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiColorSpace;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GradientRect;
import net.arnx.wmf2svg.gdi.GradientTriangle;
import net.arnx.wmf2svg.gdi.GdiObject;
import net.arnx.wmf2svg.gdi.GdiPalette;
import net.arnx.wmf2svg.gdi.GdiPatternBrush;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.GdiRegion;
import net.arnx.wmf2svg.gdi.GdiUtils;
import net.arnx.wmf2svg.gdi.emf.EmfParser;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Size;
import net.arnx.wmf2svg.gdi.Trivertex;
import net.arnx.wmf2svg.util.Base64;
import net.arnx.wmf2svg.util.ImageUtil;

/**
 * @author Hidekatsu Izuno
 * @author Shunsuke Mori
 */
public class SvgGdi implements Gdi {
	private static Logger log = Logger.getLogger(SvgGdi.class.getName());

	private boolean compatible;

	private boolean replaceSymbolFont = false;

	private Properties props = new Properties();

	private ByteArrayOutputStream emfBuffer;
	private int emfTotalSize;

	private SvgDc dc;

	private LinkedList<SvgDc> saveDC = new LinkedList<SvgDc>();

	private Document doc = null;

	private Element parentNode = null;

	private Element styleNode = null;

	private Element defsNode = null;

	private int brushNo = 0;

	private int fontNo = 0;

	private int penNo = 0;

	private int patternNo = 0;

	private int rgnNo = 0;

	private int clipPathNo = 0;

	private int maskNo = 0;

	private int gradientNo = 0;

	private Map<GdiObject, String> nameMap = new HashMap<GdiObject, String>();

	private StringBuffer buffer = new StringBuffer();

	private Element pendingOutlineOnlyPolygon;

	private String pendingOutlineOnlyPolygonPoints;

	private SvgBrush defaultBrush;

	private SvgPen defaultPen;

	private SvgFont defaultFont;

	private SvgPalette selectedPalette;

	private SvgPath currentPath;

	public SvgGdi() throws SvgGdiException {
		this(false);
	}

	public SvgGdi(boolean compatible) throws SvgGdiException {
		this.compatible = compatible;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}

		DOMImplementation dom = builder.getDOMImplementation();
		doc = dom.createDocument("http://www.w3.org/2000/svg", "svg", null);

		InputStream in = null;
		try {
			in = getClass().getResourceAsStream("SvgGdi.properties");
			props.load(in);
		} catch (Exception e) {
			throw new SvgGdiException("properties format error: SvgGDI.properties");
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {
				// no handle
			}
		}
	}

	public void write(OutputStream out) throws IOException {
		try {
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
					"-//W3C//DTD SVG 1.0//EN");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
					"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd");
			transformer.transform(new DOMSource(doc), new StreamResult(out));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		out.flush();
	}

	public void setCompatible(boolean flag) {
		compatible = flag;
	}

	public boolean isCompatible() {
		return compatible;
	}

	public void setReplaceSymbolFont(boolean flag) {
		replaceSymbolFont = flag;
	}

	public boolean isReplaceSymbolFont() {
		return replaceSymbolFont;
	}

	public SvgDc getDC() {
		return dc;
	}

	public String getProperty(String key) {
		return props.getProperty(key);
	}

	public Document getDocument() {
		return doc;
	}

	public Element getDefsElement() {
		return defsNode;
	}

	public Element getStyleElement() {
		return styleNode;
	}

	public void placeableHeader(int wsx, int wsy, int wex, int wey, int dpi) {
		if (parentNode == null) {
			init();
		}

		dc.setWindowExtEx(Math.abs(wex - wsx), Math.abs(wey - wsy), null);
		dc.setDpi(dpi);

		Element root = doc.getDocumentElement();
		root.setAttribute("width", ""
				+ (Math.abs(wex - wsx) / (double) dc.getDpi()) + "in");
		root.setAttribute("height", ""
				+ (Math.abs(wey - wsy) / (double) dc.getDpi()) + "in");
	}

	public void header() {
		if (parentNode == null) {
			init();
		}
	}

	private void init() {
		dc = new SvgDc(this);

		Element root = doc.getDocumentElement();
		root.setAttribute("xmlns", "http://www.w3.org/2000/svg");
		root.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");

		defsNode = doc.createElement("defs");
		root.appendChild(defsNode);

		styleNode = doc.createElement("style");
		styleNode.setAttribute("type", "text/css");
		root.appendChild(styleNode);

		parentNode = doc.createElement("g");
		doc.getDocumentElement().appendChild(parentNode);

		defaultBrush = (SvgBrush) createBrushIndirect(GdiBrush.BS_SOLID,
				0x00FFFFFF, 0);
		defaultPen = (SvgPen) createPenIndirect(GdiPen.PS_SOLID, 1,
				0x00000000);
		defaultFont = null;

		dc.setBrush(defaultBrush);
		dc.setPen(defaultPen);
		dc.setFont(defaultFont);
	}

	public void animatePalette(GdiPalette palette, int startIndex, int[] entries) {
		setPaletteEntries(palette, startIndex, entries);
	}

	public void alphaBlend(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, int blendFunction) {
		int sourceConstantAlpha = (blendFunction >> 16) & 0xFF;
		float opacity = sourceConstantAlpha / 255.0f;
		bmpToSvg(image, dx, dy, dw, dh, sx, sy, sw, sh, Gdi.DIB_RGB_COLORS, SRCCOPY, opacity, null);
	}

	public void angleArc(int x, int y, int radius, float startAngle, float sweepAngle) {
		Point start = circlePoint(x, y, radius, startAngle);
		Point end = circlePoint(x, y, radius, startAngle + sweepAngle);
		lineTo(start.x, start.y);
		arc(x - radius, y - radius, x + radius, y + radius, start.x, start.y, end.x, end.y);
		dc.moveToEx(end.x, end.y, null);
	}

	public void arc(int sxr, int syr, int exr, int eyr, int sxa, int sya,
			int exa, int eya) {

		double rx = Math.abs(exr - sxr) / 2.0;
		double ry = Math.abs(eyr - syr) / 2.0;
		if (rx <= 0 || ry <= 0) return;

		double cx = Math.min(sxr, exr) + rx;
		double cy = Math.min(syr, eyr) + ry;
		if (currentPath != null) {
			double ea = Math.atan2((eya - cy) * rx, (exa - cx) * ry);
			currentPath.lineTo(new Point((int)Math.round(rx * Math.cos(ea) + cx),
					(int)Math.round(ry * Math.sin(ea) + cy)));
			return;
		}

		Element elem = null;
		if (sxa == exa && sya == eya) {
			if (rx == ry) {
				elem = doc.createElement("circle");
				elem.setAttribute("cx", "" + dc.toAbsoluteX(cx));
				elem.setAttribute("cy", "" + dc.toAbsoluteY(cy));
				elem.setAttribute("r", "" + dc.toRelativeX(rx));
			} else {
				elem = doc.createElement("ellipse");
				elem.setAttribute("cx", "" + dc.toAbsoluteX(cx));
				elem.setAttribute("cy", "" + dc.toAbsoluteY(cy));
				elem.setAttribute("rx", "" + dc.toRelativeX(rx));
				elem.setAttribute("ry", "" + dc.toRelativeY(ry));
			}
		} else {
			double sa = Math.atan2((sya - cy) * rx, (sxa - cx) * ry);
			double sx = rx * Math.cos(sa);
			double sy = ry * Math.sin(sa);

			double ea = Math.atan2((eya - cy) * rx, (exa - cx) * ry);
			double ex = rx * Math.cos(ea);
			double ey = ry * Math.sin(ea);

			double a = Math.atan2((ex-sx) * (-sy) - (ey-sy) * (-sx), (ex-sx) * (-sx) + (ey-sy) * (-sy));

			elem = doc.createElement("path");
			elem.setAttribute("d", "M " + dc.toAbsoluteX(sx + cx) + "," + dc.toAbsoluteY(sy + cy)
					+ " A " + dc.toRelativeX(rx)  + "," + dc.toRelativeY(ry)
					+ " 0 " + (a > 0 ? "1" : "0") + " " + getArcSweepFlag()
					+ " " + dc.toAbsoluteX(ex + cx) + "," + dc.toAbsoluteY(ey + cy));
		}

		if (dc.getPen() != null) {
			elem.setAttribute("class", getClassString(dc.getPen()));
			setMiterLimit(elem);
		}
		elem.setAttribute("fill", "none");
		parentNode.appendChild(elem);
	}

	public void arcTo(int sxr, int syr, int exr, int eyr, int sxa, int sya,
			int exa, int eya) {
		arc(sxr, syr, exr, eyr, sxa, sya, exa, eya);
		dc.moveToEx(exa, eya, null);
	}

	public void abortPath() {
		currentPath = null;
	}

	public void beginPath() {
		currentPath = new SvgPath();
	}

	public void bitBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, long rop) {
		bmpToSvg(image, dx, dy, dw, dh, sx, sy, dw, dh, Gdi.DIB_RGB_COLORS, rop);
	}

	public void maskBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, byte[] mask, int mx, int my, long rop) {
		if (mask == null) {
			bitBlt(image, dx, dy, dw, dh, sx, sy, rop);
			return;
		}

		Element parent = parentNode;
		Element maskNode = createBitmapMask(mask, dx, dy, dw, dh, mx, my, dw, dh);
		if (maskNode == null) {
			bitBlt(image, dx, dy, dw, dh, sx, sy, rop);
			return;
		}
		try {
			parentNode = doc.createElement("g");
			parentNode.setAttribute("mask", "url(#" + maskNode.getAttribute("id") + ")");
			parent.appendChild(parentNode);
			bitBlt(image, dx, dy, dw, dh, sx, sy, rop);
		} finally {
			parentNode = parent;
		}
	}

	public void plgBlt(byte[] image, Point[] points, int sx, int sy, int sw, int sh,
			byte[] mask, int mx, int my) {
		if (points == null || points.length < 3) {
			return;
		}
		Point upperLeft = points[0];
		Point upperRight = points[1];
		Point lowerLeft = points[2];
		int sourceWidth = Math.abs(sw);
		int sourceHeight = Math.abs(sh);
		if (sourceWidth == 0 || sourceHeight == 0) {
			return;
		}

		image = convertDibToPng(image, sh < 0, null);
		String data = createPngDataUri(image);
		if (data == null) {
			return;
		}

		double x0 = dc.toAbsoluteX(upperLeft.x);
		double y0 = dc.toAbsoluteY(upperLeft.y);
		double x1 = dc.toAbsoluteX(upperRight.x);
		double y1 = dc.toAbsoluteY(upperRight.y);
		double x2 = dc.toAbsoluteX(lowerLeft.x);
		double y2 = dc.toAbsoluteY(lowerLeft.y);

		Element elem = doc.createElement("svg");
		elem.setAttribute("width", Integer.toString(sourceWidth));
		elem.setAttribute("height", Integer.toString(sourceHeight));
		elem.setAttribute("viewBox", (sw < 0 ? sx + sw : sx) + " "
				+ (sh < 0 ? sy + sh : sy) + " " + sourceWidth + " " + sourceHeight);
		elem.setAttribute("preserveAspectRatio", "none");
		elem.setAttribute("transform", "matrix("
				+ ((x1 - x0) / sourceWidth) + " "
				+ ((y1 - y0) / sourceWidth) + " "
				+ ((x2 - x0) / sourceHeight) + " "
				+ ((y2 - y0) / sourceHeight) + " "
				+ x0 + " " + y0 + ")");

		Element imageNode = doc.createElement("image");
		int[] imageSize = ImageUtil.getSize(image);
		if (imageSize != null) {
			imageNode.setAttribute("width", "" + imageSize[0]);
			imageNode.setAttribute("height", "" + imageSize[1]);
		} else {
			imageNode.setAttribute("width", Integer.toString(sourceWidth));
			imageNode.setAttribute("height", Integer.toString(sourceHeight));
		}
		imageNode.setAttribute("xlink:href", data);
		elem.appendChild(imageNode);
		if (mask != null) {
			Element maskNode = createTransformedBitmapMask(mask, mx, my, sourceWidth, sourceHeight,
					x0, y0, x1, y1, x2, y2);
			if (maskNode != null) {
				elem.setAttribute("mask", "url(#" + maskNode.getAttribute("id") + ")");
			}
		}
		parentNode.appendChild(elem);
	}

	public void chord(int sxr, int syr, int exr, int eyr, int sxa, int sya,
			int exa, int eya) {
		double rx = Math.abs(exr - sxr) / 2.0;
		double ry = Math.abs(eyr - syr) / 2.0;
		if (rx <= 0 || ry <= 0) return;

		double cx = Math.min(sxr, exr) + rx;
		double cy = Math.min(syr, eyr) + ry;

		Element elem = null;
		if (sxa == exa && sya == eya) {
			if (rx == ry) {
				elem = doc.createElement("circle");
				elem.setAttribute("cx", "" + dc.toAbsoluteX(cx));
				elem.setAttribute("cy", "" + dc.toAbsoluteY(cy));
				elem.setAttribute("r", "" + dc.toRelativeX(rx));
			} else {
				elem = doc.createElement("ellipse");
				elem.setAttribute("cx", "" + dc.toAbsoluteX(cx));
				elem.setAttribute("cy", "" + dc.toAbsoluteY(cy));
				elem.setAttribute("rx", "" + dc.toRelativeX(rx));
				elem.setAttribute("ry", "" + dc.toRelativeY(ry));
			}
		} else {
			double sa = Math.atan2((sya - cy) * rx, (sxa - cx) * ry);
			double sx = rx * Math.cos(sa);
			double sy = ry * Math.sin(sa);

			double ea = Math.atan2((eya - cy) * rx, (exa - cx) * ry);
			double ex = rx * Math.cos(ea);
			double ey = ry * Math.sin(ea);

			double a = Math.atan2((ex-sx) * (-sy) - (ey-sy) * (-sx), (ex-sx) * (-sx) + (ey-sy) * (-sy));

			elem = doc.createElement("path");
			elem.setAttribute("d", "M " + dc.toAbsoluteX(sx + cx) + "," + dc.toAbsoluteY(sy + cy)
					+ " A " + dc.toRelativeX(rx)  + "," + dc.toRelativeY(ry)
					+ " 0 " + (a > 0 ? "1" : "0") + " " + getArcSweepFlag()
					+ " " + dc.toAbsoluteX(ex + cx) + "," + dc.toAbsoluteY(ey + cy) + " Z");
		}

		if (dc.getPen() != null || dc.getBrush() != null) {
			elem.setAttribute("class", getClassString(dc.getPen(), dc.getBrush()));
			setMiterLimit(elem);
			if (dc.getBrush() != null
					&& dc.getBrush().getStyle() == GdiBrush.BS_HATCHED) {
				String id = "pattern" + (patternNo++);
				elem.setAttribute("fill", "url(#" + id + ")");
				defsNode.appendChild(dc.getBrush().createFillPattern(id));
			}
		}

		parentNode.appendChild(elem);
	}

	public void closeFigure() {
		if (currentPath != null) {
			currentPath.close();
		}
	}

	public void colorCorrectPalette(GdiPalette palette, int startIndex, int entries) {
		log.fine("unsupported in SVG output: colorCorrectPalette");
	}

	public GdiBrush createBrushIndirect(int style, int color, int hatch) {
		SvgBrush brush = new SvgBrush(this, style, color, hatch);
		if (!nameMap.containsKey(brush)) {
			String name = "brush" + (brushNo++);
			nameMap.put(brush, name);
			styleNode.appendChild(brush.createTextNode(name));
		}
		return brush;
	}

	public GdiColorSpace createColorSpace(byte[] logColorSpace) {
		return new SvgColorSpace(this, logColorSpace);
	}

	public GdiFont createFontIndirect(int height, int width, int escapement,
			int orientation, int weight, boolean italic, boolean underline,
			boolean strikeout, int charset, int outPrecision,
			int clipPrecision, int quality, int pitchAndFamily, byte[] faceName) {
		SvgFont font = new SvgFont(this, height, width, escapement,
				orientation, weight, italic, underline, strikeout, charset,
				outPrecision, clipPrecision, quality, pitchAndFamily, faceName);
		if (!nameMap.containsKey(font)) {
			String name = "font" + (fontNo++);
			nameMap.put(font, name);
			styleNode.appendChild(font.createTextNode(name));
		}
		return font;
	}

	public GdiPalette createPalette(int version, int[] entries) {
		return new SvgPalette(this, version, entries);
	}

	public GdiPatternBrush createPatternBrush(byte[] image) {
		return new SvgPatternBrush(this, image);
	}

	public GdiPen createPenIndirect(int style, int width, int color) {
		SvgPen pen = new SvgPen(this, style, width, color);
		if (!nameMap.containsKey(pen)) {
			String name = "pen" + (penNo++);
			nameMap.put(pen, name);
			styleNode.appendChild(pen.createTextNode(name));
		}
		return pen;
	}

	public GdiRegion createRectRgn(int left, int top, int right, int bottom) {
		SvgRectRegion rgn = new SvgRectRegion(this, left, top, right, bottom);
		if (!nameMap.containsKey(rgn)) {
			nameMap.put(rgn, "rgn" + (rgnNo++));
			defsNode.appendChild(rgn.createElement());
		}
		return rgn;
	}

	public GdiRegion extCreateRegion(float[] xform, int count, byte[] rgnData) {
		SvgRegion rgn = new SvgComplexRegion(this, xform, rgnData, count);
		nameMap.put(rgn, "rgn" + (rgnNo++));
		defsNode.appendChild(rgn.createElement());
		return rgn;
	}

	public void deleteObject(GdiObject obj) {
		if (dc.getBrush() == obj) {
			dc.setBrush(defaultBrush);
		} else if (dc.getFont() == obj) {
			dc.setFont(defaultFont);
		} else if (dc.getPen() == obj) {
			dc.setPen(defaultPen);
		} else if (dc.getColorSpace() == obj) {
			dc.setColorSpace(null);
		}
	}

	public boolean deleteColorSpace(GdiColorSpace colorSpace) {
		if (dc.getColorSpace() == colorSpace) {
			dc.setColorSpace(null);
		}
		return colorSpace instanceof SvgColorSpace;
	}

	public void dibBitBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, long rop) {
		bitBlt(image, dx, dy, dw, dh, sx, sy, rop);
	}

	public GdiPatternBrush dibCreatePatternBrush(byte[] image, int usage) {
		return new SvgPatternBrush(this, image);
	}

    public void dibStretchBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, long rop) {

    	this.stretchDIBits(dx, dy, dw, dh, sx, sy, sw, sh, image, Gdi.DIB_RGB_COLORS, rop);
    }

	public void ellipse(int sx, int sy, int ex, int ey) {
		if (currentPath != null) {
			currentPath.addClosedPolyline(new Point[] {
					new Point(sx, sy),
					new Point(ex, sy),
					new Point(ex, ey),
					new Point(sx, ey) });
			return;
		}

		Element elem = doc.createElement("ellipse");

		if (dc.getPen() != null || dc.getBrush() != null) {
			elem.setAttribute("class", getClassString(dc.getPen(), dc
					.getBrush()));
			if (dc.getBrush() != null
					&& dc.getBrush().getStyle() == GdiBrush.BS_HATCHED) {
				String id = "pattern" + (patternNo++);
				elem.setAttribute("fill", "url(#" + id + ")");
				defsNode.appendChild(dc.getBrush().createFillPattern(id));
			}
		}

		elem.setAttribute("cx", "" + (int)dc.toAbsoluteX((sx + ex) / 2));
		elem.setAttribute("cy", "" + (int)dc.toAbsoluteY((sy + ey) / 2));
		elem.setAttribute("rx", "" + (int)dc.toRelativeX((ex - sx) / 2));
		elem.setAttribute("ry", "" + (int)dc.toRelativeY((ey - sy) / 2));
		parentNode.appendChild(elem);
	}

	public void endPath() {
	}

	public void escape(byte[] data) {
	}

	public void comment(byte[] data) {
		if (!EmfParser.isEnhancedMetafileEscape(data)) {
			return;
		}

		if (emfBuffer == null) {
			emfTotalSize = EmfParser.getEnhancedMetafileTotalSize(data);
			emfBuffer = new ByteArrayOutputStream(Math.max(emfTotalSize, 1024));
		}

		byte[] bytes = EmfParser.getEnhancedMetafileBytes(data);
		emfBuffer.write(bytes, 0, bytes.length);

		if (emfBuffer.size() >= emfTotalSize) {
			try {
				new EmfParser(false).parse(new ByteArrayInputStream(emfBuffer.toByteArray(), 0, emfTotalSize), this);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				emfBuffer = null;
				emfTotalSize = 0;
			}
		}
	}

	private Element createMask() {
		Element mask = doc.createElement("mask");
		mask.setAttribute("id", "mask" + (maskNo++));
		mask.setIdAttribute("id", true);
		mask.setAttribute("maskUnits", "userSpaceOnUse");
		mask.setAttribute("x", "-100000");
		mask.setAttribute("y", "-100000");
		mask.setAttribute("width", "200000");
		mask.setAttribute("height", "200000");
		if (dc.getOffsetClipX() != 0 || dc.getOffsetClipY() != 0) {
			mask.setAttribute("transform", "translate(" + dc.getOffsetClipX() + "," + dc.getOffsetClipY() + ")");
		}
		defsNode.appendChild(mask);
		return mask;
	}

	private void appendFullMaskRect(Element mask, String fill) {
		Element rect = doc.createElement("rect");
		rect.setAttribute("x", "-100000");
		rect.setAttribute("y", "-100000");
		rect.setAttribute("width", "200000");
		rect.setAttribute("height", "200000");
		rect.setAttribute("fill", fill);
		mask.appendChild(rect);
	}

	private void beginMaskedGroup(Element mask) {
		beginMaskedGroup(mask, true);
	}

	private void beginMaskedGroup(Element mask, boolean nestInCurrentMask) {
		if (!parentNode.hasChildNodes()
				&& !parentNode.hasAttribute("mask")
				&& parentNode.getParentNode() != null) {
			parentNode.getParentNode().removeChild(parentNode);
		}

		Element parent = (Element)doc.getDocumentElement();
		if (nestInCurrentMask && parentNode.hasAttribute("mask")) {
			parent = parentNode;
		}

		parentNode = doc.createElement("g");
		parentNode.setAttribute("mask", "url(#" + mask.getAttribute("id") + ")");
		parent.appendChild(parentNode);
	}

	public int excludeClipRect(int left, int top, int right, int bottom) {
		Element mask = dc.getMask();
		if (mask != null) {
			mask = (Element)mask.cloneNode(true);
			String name = "mask" + (maskNo++);
			mask.setAttribute("id", name);
			mask.setIdAttribute("id", true);
			defsNode.appendChild(mask);

			Element unclip = doc.createElement("rect");
			unclip.setAttribute("x", "" + (int)dc.toAbsoluteX(left));
			unclip.setAttribute("y", "" + (int)dc.toAbsoluteY(top));
			unclip.setAttribute("width", "" + (int)dc.toRelativeX(right - left));
			unclip.setAttribute("height", "" + (int)dc.toRelativeY(bottom - top));
			unclip.setAttribute("fill", "black");
			mask.appendChild(unclip);
			dc.setMask(mask);
		} else {
			mask = createMask();
			appendFullMaskRect(mask, "white");

			Element unclip = doc.createElement("rect");
			unclip.setAttribute("x", "" + (int)dc.toAbsoluteX(left));
			unclip.setAttribute("y", "" + (int)dc.toAbsoluteY(top));
			unclip.setAttribute("width", "" + (int)dc.toRelativeX(right - left));
			unclip.setAttribute("height", "" + (int)dc.toRelativeY(bottom - top));
			unclip.setAttribute("fill", "black");
			mask.appendChild(unclip);
			dc.setMask(mask);
		}
		beginMaskedGroup(mask);
		return GdiRegion.COMPLEXREGION;
	}

	public void extFloodFill(int x, int y, int color, int type) {
		setPixel(x, y, color);
	}

	public void extTextOut(int x, int y, int options, int[] rect, byte[] text, int[] dx) {
		Element elem = doc.createElement("text");

		int escapement = 0;
		boolean vertical = false;
		if (dc.getFont() != null) {
			elem.setAttribute("class", getClassString(dc.getFont()));
			if (dc.getFont().getFaceName().startsWith("@")) {
				vertical = true;
				escapement = dc.getFont().getEscapement()-2700;
			} else {
				escapement = dc.getFont().getEscapement();
			}
		}
		elem.setAttribute("fill", SvgObject.toColor(dc.getTextColor()));

		// style
		buffer.setLength(0);
		int align = dc.getTextAlign();

		if ((align & (TA_LEFT|TA_CENTER|TA_RIGHT)) == TA_RIGHT) {
			buffer.append("text-anchor: end; ");
		} else if ((align & (TA_LEFT|TA_CENTER|TA_RIGHT)) == TA_CENTER) {
			buffer.append("text-anchor: middle; ");
		}

		if (compatible) {
			buffer.append("dominant-baseline: alphabetic; ");
		} else {
			if (vertical) {
				elem.setAttribute("writing-mode", "tb");
			} else {
				if ((align & (TA_BOTTOM|TA_TOP|TA_BASELINE)) == TA_BASELINE) {
					buffer.append("dominant-baseline: alphabetic; ");
				} else {
					buffer.append("dominant-baseline: text-before-edge; ");
				}
			}
		}

		if ((align & TA_RTLREADING) == TA_RTLREADING  || (options & ETO_RTLREADING) > 0) {
			buffer.append("unicode-bidi: bidi-override; direction: rtl; ");
		}

		if (dc.getTextSpace() > 0) {
			buffer.append("word-spacing: ").append(dc.getTextSpace()).append("; ");
		}

		if (buffer.length() > 0) {
			buffer.setLength(buffer.length()-1);
			elem.setAttribute("style", buffer.toString());
		}

		elem.setAttribute("stroke", "none");

		if ((align & (TA_NOUPDATECP|TA_UPDATECP)) == TA_UPDATECP) {
			x = dc.getCurrentX();
			y = dc.getCurrentY();
		}

		// x
		int ax = (int)dc.toAbsoluteX(x);
		int width = 0;
		if (vertical) {
			elem.setAttribute("x", Integer.toString(ax));
			if (dc.getFont() != null) width = Math.abs(dc.getFont().getFontSize());
		} else {
			if (dc.getFont() != null) {
				dx = GdiUtils.fixTextDx(dc.getFont().getCharset(), text, dx);
			}

			if (dx != null && dx.length > 0) {
				for (int i = 0; i < dx.length; i++) {
					width += dx[i];
				}

				int tx = x;

				if ((align & (TA_LEFT|TA_CENTER|TA_RIGHT)) == TA_RIGHT) {
					tx -= (width-dx[dx.length-1]);
				} else if ((align & (TA_LEFT|TA_CENTER|TA_RIGHT)) == TA_CENTER) {
					tx -= (width-dx[dx.length-1]) / 2;
				}

				buffer.setLength(0);
				for (int i = 0; i < dx.length; i++) {
					if (i > 0) buffer.append(" ");
					buffer.append((int)dc.toAbsoluteX(tx));
					tx += dx[i];
				}
				if ((align & (TA_NOUPDATECP|TA_UPDATECP)) == TA_UPDATECP) {
					dc.moveToEx(tx, y, null);
				}
				elem.setAttribute("x", buffer.toString());
			} else {
				if (dc.getFont() != null) width = Math.abs(dc.getFont().getFontSize() * text.length)/2;
				elem.setAttribute("x", Integer.toString(ax));
			}
		}

		// y
		int ay = (int)dc.toAbsoluteY(y);
		int height = 0;
		if (vertical) {
			if (dc.getFont() != null) {
				dx = GdiUtils.fixTextDx(dc.getFont().getCharset(), text, dx);
			}

			buffer.setLength(0);
			if(align == 0) {
				buffer.append(ay + (int)dc.toRelativeY(Math.abs(dc.getFont().getHeight())));
			} else {
				buffer.append(ay);
			}

			if (dx != null && dx.length > 0) {
				for (int i = 0; i < dx.length - 1; i++) {
					height += dx[i];
				}

				int ty = y;

				if ((align & (TA_LEFT|TA_CENTER|TA_RIGHT)) == TA_RIGHT) {
					ty -= (height-dx[dx.length-1]);
				} else if ((align & (TA_LEFT|TA_CENTER|TA_RIGHT)) == TA_CENTER) {
					ty -= (height-dx[dx.length-1]) / 2;
				}

				for (int i = 0; i < dx.length; i++) {
					buffer.append(" ");
					buffer.append((int)dc.toAbsoluteY(ty));
					ty += dx[i];
				}

				if ((align & (TA_NOUPDATECP|TA_UPDATECP)) == TA_UPDATECP) {
					dc.moveToEx(x, ty, null);
				}
			} else {
				if (dc.getFont() != null) height = Math.abs(dc.getFont().getFontSize() * text.length)/2;
			}
			elem.setAttribute("y", buffer.toString());
		} else {
			if (dc.getFont() != null) height = Math.abs(dc.getFont().getFontSize());
			if (compatible) {
				if ((align & (TA_BOTTOM|TA_TOP|TA_BASELINE)) == TA_TOP) {
					elem.setAttribute("y", Integer.toString(ay + (int)dc.toRelativeY(height*0.88)));
				} else if ((align & (TA_BOTTOM|TA_TOP|TA_BASELINE)) == TA_BOTTOM) {
					elem.setAttribute("y", Integer.toString(ay + rect[3] - rect[1] + (int)dc.toRelativeY(height*0.88)));
				} else {
					elem.setAttribute("y", Integer.toString(ay));
				}
			} else {
				if((align & (TA_BOTTOM|TA_TOP|TA_BASELINE)) == TA_BOTTOM && rect != null) {
					elem.setAttribute("y", Integer.toString(ay + rect[3] - rect[1] - (int)dc.toRelativeY(height)));
				} else {
					elem.setAttribute("y", Integer.toString(ay));
				}
			}
		}

		Element bk = null;
		if (dc.getBkMode() == OPAQUE || (options & ETO_OPAQUE) > 0) {
			if (rect == null && dc.getFont() != null) {
				rect = new int[4];
				if (vertical) {
					if ((align & (TA_BOTTOM|TA_TOP|TA_BASELINE)) == TA_BOTTOM) {
						rect[0] = x - width;
					} else if ((align & (TA_BOTTOM|TA_TOP|TA_BASELINE)) == TA_BASELINE) {
						rect[0] = x - (int)(width * 0.85);
					} else {
						rect[0] = x;
					}
					if ((align & (TA_LEFT|TA_RIGHT|TA_CENTER)) == TA_RIGHT) {
						rect[1] = y - height;
					} else if ((align & (TA_LEFT|TA_RIGHT|TA_CENTER)) == TA_CENTER) {
						rect[1] = y - height/2;
					} else {
						rect[1] = y;
					}
				} else {
					if ((align & (TA_LEFT|TA_RIGHT|TA_CENTER)) == TA_RIGHT) {
						rect[0] = x-width;
					} else if ((align & (TA_LEFT|TA_RIGHT|TA_CENTER)) == TA_CENTER) {
						rect[0] = x-width/2;
					} else {
						rect[0] = x;
					}
					if ((align & (TA_BOTTOM|TA_TOP|TA_BASELINE)) == TA_BOTTOM) {
						rect[1] = y - height;
					} else if ((align & (TA_BOTTOM|TA_TOP|TA_BASELINE)) == TA_BASELINE) {
						rect[1] = y - (int)(height * 0.85);
					} else {
						rect[1] = y;
					}
				}
				rect[2] = rect[0] + width;
				rect[3] = rect[1] + height;
			}
			bk = doc.createElement("rect");
			bk.setAttribute("x", Integer.toString((int)dc.toAbsoluteX(rect[0])));
			bk.setAttribute("y", Integer.toString((int)dc.toAbsoluteY(rect[1])));
			bk.setAttribute("width", Integer.toString((int)dc.toRelativeX(rect[2] - rect[0])));
			bk.setAttribute("height", Integer.toString((int)dc.toRelativeY(rect[3] - rect[1])));
			bk.setAttribute("fill", SvgObject.toColor(dc.getBkColor()));
		}

		Element clip = null;
		if ((options & ETO_CLIPPED) > 0) {
			String name = "clipPath" + (clipPathNo++);
			clip = doc.createElement("clipPath");
			clip.setAttribute("id", name);
			clip.setIdAttribute("id", true);

			Element clipRect = doc.createElement("rect");
			clipRect.setAttribute("x", Integer.toString((int)dc.toAbsoluteX(rect[0])));
			clipRect.setAttribute("y", Integer.toString((int)dc.toAbsoluteY(rect[1])));
			clipRect.setAttribute("width", Integer.toString((int)dc.toRelativeX(rect[2] - rect[0])));
			clipRect.setAttribute("height", Integer.toString((int)dc.toRelativeY(rect[3] - rect[1])));

			clip.appendChild(clipRect);
			elem.setAttribute("clip-path", "url(#" + name + ")");
		}

		String str = null;
		if (dc.getFont() != null) {
			str = GdiUtils.convertString(text, dc.getFont().getCharset());
		} else {
			str = GdiUtils.convertString(text, GdiFont.DEFAULT_CHARSET);
		}

		if (dc.getFont() != null && dc.getFont().getLang() != null) {
			elem.setAttribute("xml:lang", dc.getFont().getLang());
		}

		elem.setAttribute("xml:space", "preserve");
		appendText(elem, str);

		if (bk != null || clip != null) {
			Element g = doc.createElement("g");
			if (bk != null) g.appendChild(bk);
			if (clip != null) g.appendChild(clip);
			g.appendChild(elem);
			elem = g;
		}

		if (escapement != 0)  {
			elem.setAttribute("transform", "rotate(" + (-escapement/10.0) + ", " + ax + ", " + ay + ")");
		}
		parentNode.appendChild(elem);
	}

	public void fillRgn(GdiRegion rgn, GdiBrush brush) {
		if (rgn == null) return;

		Element elem = doc.createElement("use");
		elem.setAttribute("xlink:href", "url(#" + nameMap.get(rgn) + ")");
		elem.setAttribute("class", getClassString(brush));
		SvgBrush sbrush = (SvgBrush)brush;
		if(sbrush.getStyle() == GdiBrush.BS_HATCHED) {
			String id = "pattern" + (patternNo++);
			elem.setAttribute("fill", "url(#" + id + ")");
			defsNode.appendChild(sbrush.createFillPattern(id));
		}
		parentNode.appendChild(elem);
	}

	public void floodFill(int x, int y, int color) {
		setPixel(x, y, color);
	}

	public void gradientFill(Trivertex[] vertex, GradientRect[] mesh, int mode) {
		for (int i = 0; i < mesh.length; i++) {
			appendGradientRectangle(vertex, mesh[i], mode);
		}
	}

	public void gradientFill(Trivertex[] vertex, GradientTriangle[] mesh, int mode) {
		for (int i = 0; i < mesh.length; i++) {
			appendGradientTriangle(vertex, mesh[i]);
		}
	}

	public void frameRgn(GdiRegion rgn, GdiBrush brush, int width, int height) {
		if (!(rgn instanceof SvgRegion) || !(brush instanceof SvgBrush)) return;

		SvgBrush sbrush = (SvgBrush)brush;
		Element elem;
		if (rgn instanceof SvgRectRegion) {
			SvgRectRegion rectRgn = (SvgRectRegion)rgn;
			elem = doc.createElement("rect");
			elem.setAttribute("x", "" + (int)dc.toAbsoluteX(rectRgn.getLeft()));
			elem.setAttribute("y", "" + (int)dc.toAbsoluteY(rectRgn.getTop()));
			elem.setAttribute("width", "" + (int)dc.toRelativeX(rectRgn.getRight() - rectRgn.getLeft()));
			elem.setAttribute("height", "" + (int)dc.toRelativeY(rectRgn.getBottom() - rectRgn.getTop()));
		} else {
			elem = doc.createElement("use");
			elem.setAttribute("xlink:href", "url(#" + nameMap.get(rgn) + ")");
		}
		elem.setAttribute("fill", "none");
		elem.setAttribute("stroke", SvgObject.toColor(sbrush.getColor()));
		elem.setAttribute("stroke-width", "" + Math.max(
				Math.abs((int)dc.toRelativeX(width)),
				Math.abs((int)dc.toRelativeY(height))));
		parentNode.appendChild(elem);
	}

	public void intersectClipRect(int left, int top, int right, int bottom) {
		Element mask = createMask();

		Element clip = doc.createElement("rect");
		clip.setAttribute("x", "" + (int)dc.toAbsoluteX(left));
		clip.setAttribute("y", "" + (int)dc.toAbsoluteY(top));
		clip.setAttribute("width", "" + (int)dc.toRelativeX(right - left));
		clip.setAttribute("height", "" + (int)dc.toRelativeY(bottom - top));
		clip.setAttribute("fill", "white");
		mask.appendChild(clip);

		dc.setMask(mask);
		beginMaskedGroup(mask);
	}

	public void invertRgn(GdiRegion rgn) {
		if (rgn == null) return;

		Element elem = doc.createElement("use");
		elem.setAttribute("xlink:href", "url(#" + nameMap.get(rgn) + ")");
		String ropFilter = dc.getRopFilter(DSTINVERT);
		if (ropFilter != null) {
			elem.setAttribute("filter", ropFilter);
		}
		parentNode.appendChild(elem);
	}

	public void lineTo(int ex, int ey) {
		if (currentPath != null) {
			currentPath.lineTo(new Point(ex, ey));
			dc.moveToEx(ex, ey, null);
			return;
		}

		Element elem = doc.createElement("line");
		if (dc.getPen() != null) {
			elem.setAttribute("class", getClassString(dc.getPen()));
		}

		elem.setAttribute("fill", "none");

		elem.setAttribute("x1", "" + (int)dc.toAbsoluteX(dc.getCurrentX()));
		elem.setAttribute("y1", "" + (int)dc.toAbsoluteY(dc.getCurrentY()));
		elem.setAttribute("x2", "" + (int)dc.toAbsoluteX(ex));
		elem.setAttribute("y2", "" + (int)dc.toAbsoluteY(ey));
		parentNode.appendChild(elem);

		dc.moveToEx(ex, ey, null);
	}

	public void moveToEx(int x, int y, Point old) {
		if (currentPath != null) {
			currentPath.moveTo(new Point(x, y));
		}
		dc.moveToEx(x, y, old);
	}

	public void offsetClipRgn(int x, int y) {
		dc.offsetClipRgn(x, y);
		Element mask = dc.getMask();
		if (mask != null) {
			mask = (Element)mask.cloneNode(true);
			String name = "mask" + (maskNo++);
			mask.setAttribute("id", name);
			if (dc.getOffsetClipX() != 0 || dc.getOffsetClipY() != 0) {
				mask.setAttribute("transform", "translate(" + dc.getOffsetClipX() + "," + dc.getOffsetClipY() + ")");
			}
			defsNode.appendChild(mask);

			dc.setMask(mask);
			beginMaskedGroup(mask);
		}
	}

	public void offsetViewportOrgEx(int x, int y, Point point) {
		dc.offsetViewportOrgEx(x, y, point);
	}

	public void offsetWindowOrgEx(int x, int y, Point point) {
		dc.offsetWindowOrgEx(x, y, point);
	}

	public void paintRgn(GdiRegion rgn) {
		fillRgn(rgn, dc.getBrush());
	}

        public void patBlt(int x, int y, int width, int height, long rop) {
                Element elem = doc.createElement("rect");

                SvgBrush brush = dc.getBrush();
                if (brush != null) {
                        elem.setAttribute("class", getClassString(brush));
                        if (brush.getStyle() == GdiBrush.BS_HATCHED) {
                                String id = "pattern" + (patternNo++);
                                elem.setAttribute("fill", "url(#" + id + ")");
                                defsNode.appendChild(brush.createFillPattern(id));
                        }
                } else {
                        elem.setAttribute("fill", "none");
                }

                elem.setAttribute("stroke", "none");
                elem.setAttribute("x", "" + (int) dc.toAbsoluteX(x));
                elem.setAttribute("y", "" + (int) dc.toAbsoluteY(y));
                elem.setAttribute("width", "" + (int) dc.toRelativeX(width));
                elem.setAttribute("height", "" + (int) dc.toRelativeY(height));

                String ropFilter = dc.getRopFilter(rop);
                if (ropFilter != null) {
                        elem.setAttribute("filter", ropFilter);
                }

                parentNode.appendChild(elem);
        }

	public void pie(int sxr, int syr, int exr, int eyr, int sxa, int sya,
			int exa, int eya) {
		double rx = Math.abs(exr - sxr) / 2.0;
		double ry = Math.abs(eyr - syr) / 2.0;
		if (rx <= 0 || ry <= 0) return;

		double cx = Math.min(sxr, exr) + rx;
		double cy = Math.min(syr, eyr) + ry;

		Element elem = null;
		if (sxa == exa && sya == eya) {
			if (rx == ry) {
				elem = doc.createElement("circle");
				elem.setAttribute("cx", "" + dc.toAbsoluteX(cx));
				elem.setAttribute("cy", "" + dc.toAbsoluteY(cy));
				elem.setAttribute("r", "" + dc.toRelativeX(rx));
			} else {
				elem = doc.createElement("ellipse");
				elem.setAttribute("cx", "" + dc.toAbsoluteX(cx));
				elem.setAttribute("cy", "" + dc.toAbsoluteY(cy));
				elem.setAttribute("rx", "" + dc.toRelativeX(rx));
				elem.setAttribute("ry", "" + dc.toRelativeY(ry));
			}
		} else {
			double sa = Math.atan2((sya - cy) * rx, (sxa - cx) * ry);
			double sx = rx * Math.cos(sa);
			double sy = ry * Math.sin(sa);

			double ea = Math.atan2((eya - cy) * rx, (exa - cx) * ry);
			double ex = rx * Math.cos(ea);
			double ey = ry * Math.sin(ea);

			double a = Math.atan2((ex-sx) * (-sy) - (ey-sy) * (-sx), (ex-sx) * (-sx) + (ey-sy) * (-sy));

			elem = doc.createElement("path");
			elem.setAttribute("d", "M " + dc.toAbsoluteX(cx) + "," + dc.toAbsoluteY(cy)
					+ " L " + dc.toAbsoluteX(sx + cx) + "," + dc.toAbsoluteY(sy + cy)
					+ " A " + dc.toRelativeX(rx)  + "," + dc.toRelativeY(ry)
					+ " 0 " + (a > 0 ? "1" : "0") + " " + getArcSweepFlag()
					+ " " + dc.toAbsoluteX(ex + cx) + "," + dc.toAbsoluteY(ey + cy) + " Z");
		}

		if (dc.getPen() != null || dc.getBrush() != null) {
			elem.setAttribute("class", getClassString(dc.getPen(), dc
					.getBrush()));
			setMiterLimit(elem);
			if (dc.getBrush() != null
					&& dc.getBrush().getStyle() == GdiBrush.BS_HATCHED) {
				String id = "pattern" + (patternNo++);
				elem.setAttribute("fill", "url(#" + id + ")");
				defsNode.appendChild(dc.getBrush().createFillPattern(id));
			}
		}
		parentNode.appendChild(elem);
	}

	public void polyBezier(Point[] points) {
		if (currentPath != null) {
			currentPath.addPolyBezier(points);
			return;
		}
		appendBezier(points, false);
	}

	public void polyBezierTo(Point[] points) {
		if (currentPath != null) {
			currentPath.addPolyBezierTo(points);
			return;
		}
		appendBezier(points, true);
	}

	public void polygon(Point[] points) {
		if (currentPath != null) {
			currentPath.addClosedPolyline(points);
			return;
		}

		Element elem = doc.createElement("polygon");

		if (dc.getPen() != null || dc.getBrush() != null) {
			elem.setAttribute("class", getClassString(dc.getPen(), dc
					.getBrush()));
			if (dc.getBrush() != null
					&& dc.getBrush().getStyle() == GdiBrush.BS_HATCHED) {
				String id = "pattern" + (patternNo++);
				elem.setAttribute("fill", "url(#" + id + ")");
				defsNode.appendChild(dc.getBrush().createFillPattern(id));
			}
			if (dc.getPolyFillMode() == WINDING) {
				elem.setAttribute("fill-rule", "nonzero");
			}
		}

		String pointsValue = toSvgPoints(points, false);
		elem.setAttribute("points", pointsValue);
		parentNode.appendChild(elem);

		if (isOutlineOnlyPolygonCandidate()) {
			pendingOutlineOnlyPolygon = elem;
			pendingOutlineOnlyPolygonPoints = toSvgPoints(points, true);
		} else {
			pendingOutlineOnlyPolygon = null;
			pendingOutlineOnlyPolygonPoints = null;
		}
	}

	public void polyline(Point[] points) {
		if (currentPath != null) {
			currentPath.addPolyline(points);
			return;
		}

		String normalizedPointsValue = toSvgPoints(points, true);
		if (pendingOutlineOnlyPolygon != null
				&& parentNode.getLastChild() == pendingOutlineOnlyPolygon
				&& normalizedPointsValue.equals(pendingOutlineOnlyPolygonPoints)
				&& dc.getPen() != null
				&& dc.getPen().getStyle() != GdiPen.PS_NULL) {
			pendingOutlineOnlyPolygon.setAttribute("fill", "none");
		}

		for (Point[] segment : splitPolyline(points)) {
			if (segment.length < 2) {
				continue;
			}
			Element elem = doc.createElement("polyline");
			if (dc.getPen() != null) {
				elem.setAttribute("class", getClassString(dc.getPen()));
			}
			elem.setAttribute("fill", "none");
			elem.setAttribute("points", toSvgPoints(segment, false));
			parentNode.appendChild(elem);
		}

		pendingOutlineOnlyPolygon = null;
		pendingOutlineOnlyPolygonPoints = null;
	}

	public void polyPolygon(Point[][] points) {
		if (currentPath != null) {
			for (int i = 0; i < points.length; i++) {
				currentPath.addClosedPolyline(points[i]);
			}
			return;
		}

		Element elem = doc.createElement("path");

		if (dc.getPen() != null || dc.getBrush() != null) {
			elem.setAttribute("class", getClassString(dc.getPen(), dc
					.getBrush()));
			if (dc.getBrush() != null
					&& dc.getBrush().getStyle() == GdiBrush.BS_HATCHED) {
				String id = "pattern" + (patternNo++);
				elem.setAttribute("fill", "url(#" + id + ")");
				defsNode.appendChild(dc.getBrush().createFillPattern(id));
			}
			if (dc.getPolyFillMode() == WINDING) {
				elem.setAttribute("fill-rule", "nonzero");
			}
		}

		buffer.setLength(0);
		for (int i = 0; i < points.length; i++) {
			if (i != 0) {
				buffer.append(" ");
			}
			for (int j = 0; j < points[i].length; j++) {
				if (j == 0) {
					buffer.append("M ");
				} else if (j == 1) {
					buffer.append(" L ");
				}
				buffer.append((int)dc.toAbsoluteX(points[i][j].x)).append(",");
				buffer.append((int)dc.toAbsoluteY(points[i][j].y)).append(" ");
				if (j == points[i].length - 1) {
					buffer.append("z");
				}
			}
		}
		elem.setAttribute("d", buffer.toString());
		parentNode.appendChild(elem);
	}

	public void fillPath() {
		if (currentPath != null && currentPath.isWidened()) {
			appendWidenedPath(currentPath, dc.getBrush());
		} else {
			appendPath(currentPath, false, true);
		}
		currentPath = null;
	}

	public void flattenPath() {
		// SVG can render cubic Bezier path commands directly.
	}

	public void widenPath() {
		if (currentPath != null) {
			currentPath.widen();
		}
	}

	public void strokePath() {
		appendPath(currentPath, true, false);
		currentPath = null;
	}

	public void strokeAndFillPath() {
		if (currentPath != null && currentPath.isWidened()) {
			appendWidenedPath(currentPath, dc.getBrush());
		} else {
			appendPath(currentPath, true, true);
		}
		currentPath = null;
	}

	public void realizePalette() {
		// Palette realization has no direct SVG equivalent. Palette entries are
		// applied when DIB images are converted.
	}

	public void restoreDC(int savedDC) {
		int limit = (savedDC < 0) ? -savedDC : saveDC.size()-savedDC;
		for (int i = 0; i < limit; i++) {
			dc = (SvgDc)saveDC.removeLast();
		}

		if (!parentNode.hasChildNodes()) {
			doc.getDocumentElement().removeChild(parentNode);
		}
		parentNode = doc.createElement("g");
		Element mask = dc.getMask();
		if (mask != null) {
			parentNode.setAttribute("mask", "url(#" + mask.getAttribute("id") + ")");
		}
		doc.getDocumentElement().appendChild(parentNode);
	}

	public void rectangle(int sx, int sy, int ex, int ey) {
		if (currentPath != null) {
			currentPath.addClosedPolyline(new Point[] {
					new Point(sx, sy),
					new Point(ex, sy),
					new Point(ex, ey),
					new Point(sx, ey) });
			return;
		}

		Element elem = doc.createElement("rect");

		if (dc.getPen() != null || dc.getBrush() != null) {
			elem.setAttribute("class", getClassString(dc.getPen(), dc
					.getBrush()));
			if (dc.getBrush() != null
					&& dc.getBrush().getStyle() == GdiBrush.BS_HATCHED) {
				String id = "pattern" + (patternNo++);
				elem.setAttribute("fill", "url(#" + id + ")");
				defsNode.appendChild(dc.getBrush().createFillPattern(id));
			}
		}

		elem.setAttribute("x", "" + (int)dc.toAbsoluteX(sx));
		elem.setAttribute("y", "" + (int)dc.toAbsoluteY(sy));
		elem.setAttribute("width", "" + (int)dc.toRelativeX(ex - sx));
		elem.setAttribute("height", "" + (int)dc.toRelativeY(ey - sy));
		parentNode.appendChild(elem);
	}

	public void resizePalette(GdiPalette palette) {
		// The WMF parser does not expose the requested size, so keep current entries.
	}

	public void roundRect(int sx, int sy, int ex, int ey, int rw, int rh) {
		if (currentPath != null) {
			rectangle(sx, sy, ex, ey);
			return;
		}

		Element elem = doc.createElement("rect");

		if (dc.getPen() != null || dc.getBrush() != null) {
			elem.setAttribute("class", getClassString(dc.getPen(), dc
					.getBrush()));
			if (dc.getBrush() != null
					&& dc.getBrush().getStyle() == GdiBrush.BS_HATCHED) {
				String id = "pattern" + (patternNo++);
				elem.setAttribute("fill", "url(#" + id + ")");
				defsNode.appendChild(dc.getBrush().createFillPattern(id));
			}
		}

		elem.setAttribute("x", "" + (int)dc.toAbsoluteX(sx));
		elem.setAttribute("y", "" + (int)dc.toAbsoluteY(sy));
		elem.setAttribute("width", "" + (int)dc.toRelativeX(ex - sx));
		elem.setAttribute("height", "" + (int)dc.toRelativeY(ey - sy));
		elem.setAttribute("rx", "" + (int)dc.toRelativeX(rw));
		elem.setAttribute("ry", "" + (int)dc.toRelativeY(rh));
		parentNode.appendChild(elem);
	}

	public void seveDC() {
		saveDC.add((SvgDc)dc.clone());
	}

	public void scaleViewportExtEx(int x, int xd, int y, int yd, Size old) {
		dc.scaleViewportExtEx(x, xd, y, yd, old);
	}

	public void scaleWindowExtEx(int x, int xd, int y, int yd, Size old) {
		dc.scaleWindowExtEx(x, xd, y, yd, old);
	}

	public void selectClipRgn(GdiRegion rgn) {
		extSelectClipRgn(rgn, GdiRegion.RGN_COPY);
	}

	public int extSelectClipRgn(GdiRegion rgn, int mode) {
		if (rgn != null) {
			Element mask = createClipRgnMask(mode);
			mask.appendChild(createRegionUse(rgn, mode == GdiRegion.RGN_DIFF ? "black" : "white"));
			dc.setMask(mask);
			beginMaskedGroup(mask, mode == GdiRegion.RGN_AND);
			return GdiRegion.COMPLEXREGION;
		} else {
			if (mode == GdiRegion.RGN_COPY) {
				dc.setMask(null);
				if (!parentNode.hasChildNodes() && parentNode.getParentNode() != null) {
					parentNode.getParentNode().removeChild(parentNode);
				}
				parentNode = doc.createElement("g");
				doc.getDocumentElement().appendChild(parentNode);
			}
			return dc.getMask() != null ? GdiRegion.COMPLEXREGION : GdiRegion.NULLREGION;
		}
	}

	private Element createClipRgnMask(int mode) {
		Element mask;
		if (mode == GdiRegion.RGN_OR || mode == GdiRegion.RGN_XOR || mode == GdiRegion.RGN_DIFF) {
			mask = dc.getMask();
			if (mask != null) {
				mask = (Element)mask.cloneNode(true);
				String name = "mask" + (maskNo++);
				mask.setAttribute("id", name);
				mask.setIdAttribute("id", true);
				defsNode.appendChild(mask);
			} else {
				mask = createMask();
				if (mode == GdiRegion.RGN_DIFF) {
					appendFullMaskRect(mask, "white");
				}
			}
		} else {
			mask = createMask();
		}
		return mask;
	}

	private Element createRegionUse(GdiRegion rgn, String fill) {
		Element clip = doc.createElement("use");
		clip.setAttribute("xlink:href", "url(#" + nameMap.get(rgn) + ")");
		clip.setAttribute("fill", fill);
		return clip;
	}

	public void selectClipPath(int mode) {
		if (currentPath == null || currentPath.isEmpty()) {
			return;
		}

		Element mask = createClipRgnMask(mode);
		Element clip = createPathClip(mode == GdiRegion.RGN_DIFF ? "black" : "white");
		mask.appendChild(clip);

		dc.setMask(mask);
		beginMaskedGroup(mask, mode == GdiRegion.RGN_AND);
		currentPath = null;
	}

	private Element createPathClip(String fill) {
		Element clip = doc.createElement("path");
		clip.setAttribute("d", toSvgPath(currentPath));
		if (currentPath.isWidened() && dc.getPen() != null && dc.getPen().getStyle() != GdiPen.PS_NULL) {
			clip.setAttribute("fill", "none");
			clip.setAttribute("stroke", fill);
			clip.setAttribute("stroke-width", Integer.toString(dc.getPen().getWidth()));
			clip.setAttribute("stroke-linecap", "round");
			clip.setAttribute("stroke-linejoin", "round");
		} else {
			clip.setAttribute("fill", fill);
		}
		if (!currentPath.isWidened() && dc.getPolyFillMode() == WINDING) {
			clip.setAttribute("fill-rule", "nonzero");
		}
		return clip;
	}

	public GdiColorSpace setColorSpace(GdiColorSpace colorSpace) {
		if (colorSpace instanceof SvgColorSpace) {
			return dc.setColorSpace((SvgColorSpace)colorSpace);
		}
		return null;
	}

	public void selectObject(GdiObject obj) {
		if (obj instanceof SvgBrush) {
			dc.setBrush((SvgBrush) obj);
		} else if (obj instanceof SvgFont) {
			dc.setFont((SvgFont) obj);
		} else if (obj instanceof SvgPen) {
			dc.setPen((SvgPen) obj);
		}
	}

	public void selectPalette(GdiPalette palette, boolean mode) {
		selectedPalette = (SvgPalette)palette;
	}

	public void setBrushOrgEx(int x, int y, Point old) {
		dc.setBrushOrgEx(x, y, old);
	}

	public void setBkColor(int color) {
		dc.setBkColor(color);
	}

	private boolean isOutlineOnlyPolygonCandidate() {
		SvgBrush brush = dc.getBrush();
		SvgPen pen = dc.getPen();
		return brush != null
				&& brush.getStyle() == GdiBrush.BS_SOLID
				&& brush.getColor() == 0x00FFFFFF
				&& pen != null
				&& pen.getStyle() == GdiPen.PS_NULL;
	}

	private String getArcSweepFlag() {
		return (dc.getArcDirection() == Gdi.AD_CLOCKWISE) ? "1" : "0";
	}

	private void setMiterLimit(Element elem) {
		if (dc.getPen() != null && dc.getMiterLimit() > 0) {
			elem.setAttribute("stroke-miterlimit", Float.toString(dc.getMiterLimit()));
		}
	}

	private String toSvgPoints(Point[] points, boolean normalizeDuplicates) {
		buffer.setLength(0);
		int prevX = Integer.MIN_VALUE;
		int prevY = Integer.MIN_VALUE;
		boolean hasPrevious = false;
		for (int i = 0; i < points.length; i++) {
			int x = (int) dc.toAbsoluteX(points[i].x);
			int y = (int) dc.toAbsoluteY(points[i].y);
			if (normalizeDuplicates && hasPrevious && prevX == x && prevY == y) {
				continue;
			}
			if (buffer.length() > 0) {
				buffer.append(" ");
			}
			buffer.append(x).append(",").append(y);
			prevX = x;
			prevY = y;
			hasPrevious = true;
		}
		return buffer.toString();
	}

	private String toSvgPath(Point[][] points) {
		return toSvgPath(points, true);
	}

	private String toSvgPath(SvgPath path) {
		buffer.setLength(0);
		SvgPath.Command[] commands = path.getCommands();
		for (int i = 0; i < commands.length; i++) {
			SvgPath.Command command = commands[i];
			Point[] points = command.getPoints();
			switch (command.getType()) {
			case SvgPath.MOVE_TO:
				if (points.length > 0) {
					buffer.append("M ");
					appendSvgPoint(points[0]);
					buffer.append(" ");
				}
				break;
			case SvgPath.LINE_TO:
				if (points.length > 0) {
					buffer.append("L ");
					appendSvgPoint(points[0]);
					buffer.append(" ");
				}
				break;
			case SvgPath.BEZIER_TO:
				if (points.length >= 3) {
					buffer.append("C ");
					appendSvgPoint(points[0]);
					buffer.append(" ");
					appendSvgPoint(points[1]);
					buffer.append(" ");
					appendSvgPoint(points[2]);
					buffer.append(" ");
				}
				break;
			case SvgPath.CLOSE:
				buffer.append("z ");
				break;
			default:
				break;
			}
		}
		return buffer.toString();
	}

	private String toSvgPath(Point[][] points, boolean close) {
		buffer.setLength(0);
		for (int i = 0; i < points.length; i++) {
			if (points[i].length == 0) {
				continue;
			}
			if (buffer.length() > 0) {
				buffer.append(" ");
			}
			for (int j = 0; j < points[i].length; j++) {
				if (j == 0) {
					buffer.append("M ");
				} else if (j == 1) {
					buffer.append(" L ");
				}
				buffer.append((int) dc.toAbsoluteX(points[i][j].x)).append(",");
				buffer.append((int) dc.toAbsoluteY(points[i][j].y)).append(" ");
			}
			if (close) {
				buffer.append("z");
			}
		}
		return buffer.toString();
	}

	private void appendPath(SvgPath path, boolean stroke, boolean fill) {
		if (path == null || path.isEmpty()) {
			return;
		}

		Element elem = doc.createElement("path");
		elem.setAttribute("d", toSvgPath(path));
		if (stroke && fill) {
			if (dc.getPen() != null || dc.getBrush() != null) {
				elem.setAttribute("class", getClassString(dc.getPen(), dc.getBrush()));
				setMiterLimit(elem);
			}
		} else if (stroke) {
			if (dc.getPen() != null) {
				elem.setAttribute("class", getClassString(dc.getPen()));
				setMiterLimit(elem);
			}
			elem.setAttribute("fill", "none");
		} else if (fill) {
			if (dc.getBrush() != null) {
				elem.setAttribute("class", getClassString(dc.getBrush()));
			}
			elem.setAttribute("stroke", "none");
		}
		if (fill && dc.getBrush() != null && dc.getBrush().getStyle() == GdiBrush.BS_HATCHED) {
			String id = "pattern" + (patternNo++);
			elem.setAttribute("fill", "url(#" + id + ")");
			defsNode.appendChild(dc.getBrush().createFillPattern(id));
		}
		if (fill && dc.getPolyFillMode() == WINDING) {
			elem.setAttribute("fill-rule", "nonzero");
		}
		parentNode.appendChild(elem);
	}

	private void appendWidenedPath(SvgPath path, SvgBrush brush) {
		if (path == null || path.isEmpty() || dc.getPen() == null || dc.getPen().getStyle() == GdiPen.PS_NULL) {
			return;
		}

		Element elem = doc.createElement("path");
		elem.setAttribute("d", toSvgPath(path));
		elem.setAttribute("fill", "none");
		elem.setAttribute("stroke-width", Integer.toString(dc.getPen().getWidth()));
		elem.setAttribute("stroke-linecap", "round");
		elem.setAttribute("stroke-linejoin", "round");

		if (brush != null && brush.getStyle() == GdiBrush.BS_SOLID) {
			elem.setAttribute("stroke", SvgObject.toColor(brush.getColor()));
		} else if (brush != null && brush.getStyle() == GdiBrush.BS_HATCHED) {
			String id = "pattern" + (patternNo++);
			elem.setAttribute("stroke", "url(#" + id + ")");
			defsNode.appendChild(brush.createFillPattern(id));
		} else {
			elem.setAttribute("stroke", "none");
		}
		parentNode.appendChild(elem);
	}

	private void appendGradientRectangle(Trivertex[] vertex, GradientRect rect, int mode) {
		if (rect.upperLeft < 0 || rect.upperLeft >= vertex.length
				|| rect.lowerRight < 0 || rect.lowerRight >= vertex.length) {
			return;
		}
		Trivertex v1 = vertex[rect.upperLeft];
		Trivertex v2 = vertex[rect.lowerRight];

		Element gradient = doc.createElement("linearGradient");
		String id = "gradient" + (gradientNo++);
		gradient.setAttribute("id", id);
		gradient.setIdAttribute("id", true);
		gradient.setAttribute("gradientUnits", "userSpaceOnUse");
		if (mode == GRADIENT_FILL_RECT_V) {
			gradient.setAttribute("x1", "" + (int)dc.toAbsoluteX(v1.x));
			gradient.setAttribute("y1", "" + (int)dc.toAbsoluteY(v1.y));
			gradient.setAttribute("x2", "" + (int)dc.toAbsoluteX(v1.x));
			gradient.setAttribute("y2", "" + (int)dc.toAbsoluteY(v2.y));
		} else {
			gradient.setAttribute("x1", "" + (int)dc.toAbsoluteX(v1.x));
			gradient.setAttribute("y1", "" + (int)dc.toAbsoluteY(v1.y));
			gradient.setAttribute("x2", "" + (int)dc.toAbsoluteX(v2.x));
			gradient.setAttribute("y2", "" + (int)dc.toAbsoluteY(v1.y));
		}
		appendGradientStop(gradient, "0%", v1.getColor());
		appendGradientStop(gradient, "100%", v2.getColor());
		defsNode.appendChild(gradient);

		Element elem = doc.createElement("rect");
		double x1 = dc.toAbsoluteX(v1.x);
		double y1 = dc.toAbsoluteY(v1.y);
		double x2 = dc.toAbsoluteX(v2.x);
		double y2 = dc.toAbsoluteY(v2.y);
		elem.setAttribute("x", "" + (int)Math.min(x1, x2));
		elem.setAttribute("y", "" + (int)Math.min(y1, y2));
		elem.setAttribute("width", "" + (int)Math.abs(x2 - x1));
		elem.setAttribute("height", "" + (int)Math.abs(y2 - y1));
		elem.setAttribute("fill", "url(#" + id + ")");
		elem.setAttribute("stroke", "none");
		parentNode.appendChild(elem);
	}

	private void appendGradientTriangle(Trivertex[] vertex, GradientTriangle triangle) {
		if (triangle.vertex1 < 0 || triangle.vertex1 >= vertex.length
				|| triangle.vertex2 < 0 || triangle.vertex2 >= vertex.length
				|| triangle.vertex3 < 0 || triangle.vertex3 >= vertex.length) {
			return;
		}
		Trivertex v1 = vertex[triangle.vertex1];
		Trivertex v2 = vertex[triangle.vertex2];
		Trivertex v3 = vertex[triangle.vertex3];

		Element elem = doc.createElement("polygon");
		elem.setAttribute("points", toSvgPoints(new Point[] {
				new Point(v1.x, v1.y),
				new Point(v2.x, v2.y),
				new Point(v3.x, v3.y) }, false));
		elem.setAttribute("fill", SvgObject.toColor(averageColor(v1, v2, v3)));
		elem.setAttribute("stroke", "none");
		parentNode.appendChild(elem);
	}

	private void appendGradientStop(Element gradient, String offset, int color) {
		Element stop = doc.createElement("stop");
		stop.setAttribute("offset", offset);
		stop.setAttribute("stop-color", SvgObject.toColor(color));
		gradient.appendChild(stop);
	}

	private int averageColor(Trivertex v1, Trivertex v2, Trivertex v3) {
		int red = ((v1.red >>> 8) + (v2.red >>> 8) + (v3.red >>> 8)) / 3;
		int green = ((v1.green >>> 8) + (v2.green >>> 8) + (v3.green >>> 8)) / 3;
		int blue = ((v1.blue >>> 8) + (v2.blue >>> 8) + (v3.blue >>> 8)) / 3;
		return (blue << 16) | (green << 8) | red;
	}

	private void appendSvgPoint(Point point) {
		buffer.append((int)dc.toAbsoluteX(point.x)).append(",");
		buffer.append((int)dc.toAbsoluteY(point.y));
	}

	private static class SvgPath {
		private static final int MOVE_TO = 1;
		private static final int LINE_TO = 2;
		private static final int BEZIER_TO = 3;
		private static final int CLOSE = 4;

		private LinkedList<Command> commands = new LinkedList<Command>();
		private Point figureStart;
		private Point current;
		private boolean widened;

		public void moveTo(Point point) {
			commands.add(new Command(MOVE_TO, new Point[] { point }));
			figureStart = point;
			current = point;
		}

		public void lineTo(Point point) {
			if (current == null) {
				moveTo(point);
			} else {
				commands.add(new Command(LINE_TO, new Point[] { point }));
				current = point;
			}
		}

		public void bezierTo(Point control1, Point control2, Point end) {
			if (current == null) {
				moveTo(end);
			} else {
				commands.add(new Command(BEZIER_TO, new Point[] { control1, control2, end }));
				current = end;
			}
		}

		public void addPolyline(Point[] points) {
			if (points == null || points.length == 0) {
				return;
			}
			moveTo(points[0]);
			for (int i = 1; i < points.length; i++) {
				lineTo(points[i]);
			}
		}

		public void addClosedPolyline(Point[] points) {
			addPolyline(points);
			close();
		}

		public void addPolyBezier(Point[] points) {
			if (points == null || points.length < 4) {
				return;
			}
			moveTo(points[0]);
			for (int i = 1; i + 2 < points.length; i += 3) {
				bezierTo(points[i], points[i + 1], points[i + 2]);
			}
		}

		public void addPolyBezierTo(Point[] points) {
			if (points == null || points.length < 3) {
				return;
			}
			for (int i = 0; i + 2 < points.length; i += 3) {
				bezierTo(points[i], points[i + 1], points[i + 2]);
			}
		}

		public void close() {
			if (current != null) {
				commands.add(new Command(CLOSE, new Point[0]));
				current = figureStart;
			}
		}

		public boolean isEmpty() {
			return commands.isEmpty();
		}

		public void widen() {
			widened = true;
		}

		public boolean isWidened() {
			return widened;
		}

		public Command[] getCommands() {
			return commands.toArray(new Command[commands.size()]);
		}

		private static class Command {
			private int type;
			private Point[] points;

			private Command(int type, Point[] points) {
				this.type = type;
				this.points = points;
			}

			public int getType() {
				return type;
			}

			public Point[] getPoints() {
				return points;
			}
		}
	}

	private void appendBezier(Point[] points, boolean fromCurrentPosition) {
		if (points == null || points.length == 0 || (!fromCurrentPosition && points.length < 4)) {
			return;
		}

		Element elem = doc.createElement("path");
		buffer.setLength(0);
		int offset = 0;
		if (fromCurrentPosition) {
			buffer.append("M ");
			buffer.append((int) dc.toAbsoluteX(dc.getCurrentX())).append(",");
			buffer.append((int) dc.toAbsoluteY(dc.getCurrentY())).append(" ");
		} else {
			buffer.append("M ");
			buffer.append((int) dc.toAbsoluteX(points[0].x)).append(",");
			buffer.append((int) dc.toAbsoluteY(points[0].y)).append(" ");
			offset = 1;
		}

		Point last = null;
		for (int i = offset; i + 2 < points.length; i += 3) {
			buffer.append("C ");
			buffer.append((int) dc.toAbsoluteX(points[i].x)).append(",");
			buffer.append((int) dc.toAbsoluteY(points[i].y)).append(" ");
			buffer.append((int) dc.toAbsoluteX(points[i + 1].x)).append(",");
			buffer.append((int) dc.toAbsoluteY(points[i + 1].y)).append(" ");
			buffer.append((int) dc.toAbsoluteX(points[i + 2].x)).append(",");
			buffer.append((int) dc.toAbsoluteY(points[i + 2].y)).append(" ");
			last = points[i + 2];
		}

		if (last == null) {
			return;
		}
		if (dc.getPen() != null) {
			elem.setAttribute("class", getClassString(dc.getPen()));
			setMiterLimit(elem);
		}
		elem.setAttribute("fill", "none");
		elem.setAttribute("d", buffer.toString());
		parentNode.appendChild(elem);
		dc.moveToEx(last.x, last.y, null);
	}

	private Point circlePoint(int x, int y, int radius, double angle) {
		double radians = Math.toRadians(angle);
		int px = x + (int)Math.round(radius * Math.cos(radians));
		int py = y - (int)Math.round(radius * Math.sin(radians));
		return new Point(px, py);
	}

	private Point[][] splitPolyline(Point[] points) {
		java.util.List<Point[]> result = new java.util.ArrayList<Point[]>();
		java.util.List<Point> current = new java.util.ArrayList<Point>();
		Point previous = null;
		for (Point point : points) {
			if (previous != null && previous.x == point.x && previous.y == point.y) {
				if (!current.isEmpty()) {
					result.add(current.toArray(new Point[current.size()]));
					current.clear();
				}
				current.add(point);
				previous = point;
				continue;
			}
			current.add(point);
			previous = point;
		}
		if (!current.isEmpty()) {
			result.add(current.toArray(new Point[current.size()]));
		}
		return result.toArray(new Point[result.size()][]);
	}

	public void setBkMode(int mode) {
		dc.setBkMode(mode);
	}

	public void setColorAdjustment(byte[] colorAdjustment) {
		dc.setColorAdjustment(colorAdjustment);
	}

	public void setArcDirection(int direction) {
		dc.setArcDirection(direction);
	}

	public void setDIBitsToDevice(int dx, int dy, int dw, int dh, int sx,
			int sy, int startscan, int scanlines, byte[] image, int colorUse) {
		stretchDIBits(dx, dy, dw, dh, sx, sy, dw, dh, image, colorUse, SRCCOPY);
	}

	public void setLayout(long layout) {
		dc.setLayout(layout);
	}

	public void setMapMode(int mode) {
		dc.setMapMode(mode);
	}

	public void setMapperFlags(long flags) {
		dc.setMapperFlags(flags);
	}

	public int setICMMode(int mode) {
		return dc.setICMMode(mode);
	}

	public int setMetaRgn() {
		return dc.getMask() != null ? GdiRegion.SIMPLEREGION : GdiRegion.NULLREGION;
	}

	public void setMiterLimit(float limit) {
		dc.setMiterLimit(limit);
	}

	public void setPaletteEntries(GdiPalette palette, int startIndex, int[] entries) {
		if (palette instanceof SvgPalette) {
			((SvgPalette)palette).setEntries(startIndex, entries);
		}
	}

	public void setPixel(int x, int y, int color) {
		Element elem = doc.createElement("rect");
		elem.setAttribute("stroke", "none");
		elem.setAttribute("fill", SvgPen.toColor(color));
		elem.setAttribute("x", "" + (int)dc.toAbsoluteX(x));
		elem.setAttribute("y", "" + (int)dc.toAbsoluteY(y));
		elem.setAttribute("width", "" + (int)dc.toRelativeX(1));
		elem.setAttribute("height", "" + (int)dc.toRelativeY(1));
		parentNode.appendChild(elem);
	}

	public void setPolyFillMode(int mode) {
		dc.setPolyFillMode(mode);
	}

	public void setRelAbs(int mode) {
		dc.setRelAbs(mode);
	}

	public void setROP2(int mode) {
		dc.setROP2(mode);
	}

	public void setStretchBltMode(int mode) {
		dc.setStretchBltMode(mode);
	}

	public void setTextAlign(int align) {
		dc.setTextAlign(align);
	}

	public void setTextCharacterExtra(int extra) {
		dc.setTextCharacterExtra(extra);
	}

	public void setTextColor(int color) {
		dc.setTextColor(color);
	}

	public void setTextJustification(int breakExtra, int breakCount) {
		if (breakCount > 0) {
			dc.setTextSpace(Math.abs((int)dc.toRelativeX(breakExtra)) / breakCount);
		}
	}

	public void setViewportExtEx(int x, int y, Size old) {
		dc.setViewportExtEx(x, y, old);
	}

	public void setViewportOrgEx(int x, int y, Point old) {
		dc.setViewportOrgEx(x, y, old);
	}

	public void setWindowExtEx(int width, int height, Size old) {
		dc.setWindowExtEx(width, height, old);
	}

	public void setWindowOrgEx(int x, int y, Point old) {
		dc.setWindowOrgEx(x, y, old);
	}

	public void stretchBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy,
			int sw, int sh, long rop) {
		dibStretchBlt(image, dx, dy, dw, dh, sx, sy, sw, sh, rop);
	}

	public void stretchDIBits(int dx, int dy, int dw, int dh, int sx, int sy,
			int sw, int sh, byte[] image, int usage, long rop) {
		bmpToSvg(image, dx, dy, dw, dh, sx, sy, sw, sh, usage, rop);
	}

	public void transparentBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, int transparentColor) {
		bmpToSvg(image, dx, dy, dw, dh, sx, sy, sw, sh, Gdi.DIB_RGB_COLORS, SRCCOPY, 1.0f,
				Integer.valueOf(transparentColor));
	}

	public void textOut(int x, int y, byte[] text) {
		Element elem = doc.createElement("text");

		int escapement = 0;
		boolean vertical = false;
		if (dc.getFont() != null) {
			elem.setAttribute("class", getClassString(dc.getFont()));
			if (dc.getFont().getFaceName().startsWith("@")) {
				vertical = true;
				escapement = dc.getFont().getEscapement()-2700;
			} else {
				escapement = dc.getFont().getEscapement();
			}
		}
		elem.setAttribute("fill", SvgObject.toColor(dc.getTextColor()));

		// style
		buffer.setLength(0);
		int align = dc.getTextAlign();

		if ((align & (TA_LEFT|TA_RIGHT|TA_CENTER)) == TA_RIGHT) {
			buffer.append("text-anchor: end; ");
		} else if ((align & (TA_LEFT|TA_RIGHT|TA_CENTER)) == TA_CENTER) {
			buffer.append("text-anchor: middle; ");
		}

		if (vertical) {
			elem.setAttribute("writing-mode", "tb");
			buffer.append("dominant-baseline: ideographic; ");
		} else {
			if ((align & (TA_BOTTOM|TA_TOP|TA_BASELINE)) == TA_BASELINE) {
				buffer.append("dominant-baseline: alphabetic; ");
			} else {
				buffer.append("dominant-baseline: text-before-edge; ");
			}
		}

		if ((align & TA_RTLREADING) == TA_RTLREADING) {
			buffer.append("unicode-bidi: bidi-override; direction: rtl; ");
		}

		if (dc.getTextSpace() > 0) {
			buffer.append("word-spacing: " + dc.getTextSpace() + "; ");
		}

		if (buffer.length() > 0) {
			buffer.setLength(buffer.length()-1);
			elem.setAttribute("style", buffer.toString());
		}

		elem.setAttribute("stroke", "none");

		int ax = (int)dc.toAbsoluteX(x);
		int ay = (int)dc.toAbsoluteY(y);
		elem.setAttribute("x", Integer.toString(ax));
		elem.setAttribute("y", Integer.toString(ay));

		if (escapement != 0)  {
			elem.setAttribute("transform", "rotate(" + (-escapement/10.0) + ", " + ax + ", " + ay + ")");
		}

		String str = null;
		if (dc.getFont() != null) {
			str = GdiUtils.convertString(text, dc.getFont().getCharset());
		} else {
			str = GdiUtils.convertString(text, GdiFont.DEFAULT_CHARSET);
		}

		if (dc.getTextCharacterExtra() != 0) {
			buffer.setLength(0);

			for (int i = 0; i < str.length() - 1; i++) {
				if (i != 0) {
					buffer.append(" ");
				}
				buffer.append((int)dc.toRelativeX(dc.getTextCharacterExtra()));
			}

			elem.setAttribute("dx", buffer.toString());
		}

		if (dc.getFont() != null && dc.getFont().getLang() != null) {
			elem.setAttribute("xml:lang", dc.getFont().getLang());
		}
		elem.setAttribute("xml:space", "preserve");
		appendText(elem, str);
		parentNode.appendChild(elem);
	}

	public void footer() {
		Element root = doc.getDocumentElement();
		if (!root.hasAttribute("width") && dc.getWindowWidth() != 0) {
			root.setAttribute("width", "" + Math.abs(dc.getWindowWidth()));
		}
		if (!root.hasAttribute("height") && dc.getWindowHeight() != 0) {
			root.setAttribute("height", "" + Math.abs(dc.getWindowHeight()));
		}
		if (dc.getWindowWidth() != 0 && dc.getWindowHeight() != 0) {
			root.setAttribute("viewBox", "0 0 " + Math.abs(dc.getWindowWidth()) + " " + Math.abs(dc.getWindowHeight()));
			root.setAttribute("preserveAspectRatio", "none");
		}
		root.setAttribute("stroke-linecap", "butt");
		root.setAttribute("fill-rule", "evenodd");

		if (!styleNode.hasChildNodes()) {
			root.removeChild(styleNode);
		} else {
			styleNode.insertBefore(doc.createTextNode("\n"), styleNode.getFirstChild());
		}

		if (!defsNode.hasChildNodes()) {
			root.removeChild(defsNode);
		}
	}

	private String getClassString(GdiObject obj1, GdiObject obj2) {
		String name1 = getClassString(obj1);
		String name2 = getClassString(obj2);
		if (name1 != null && name2 != null) {
			return name1 + " " + name2;
		}
		if (name1 != null) {
			return name1;
		}
		if (name2 != null) {
			return name2;
		}
		return "";
	}

	private String getClassString(GdiObject style) {
		if (style == null) {
			return "";
		}

		return (String) nameMap.get(style);
	}

	private void appendText(Element elem, String str) {
		if (compatible) {
			str = str.replaceAll("\\r\\n|[\\t\\r\\n ]", "\u00A0");
		}
		SvgFont font = dc.getFont();
		if (replaceSymbolFont && font != null) {
			if ("Symbol".equals(font.getFaceName())) {
				int state = 0; // 0: default, 1: serif, 2: sans-serif
				int start = 0;
				char[] ca = str.toCharArray();
				for (int i = 0; i < ca.length; i++) {
					int nstate = state;
					switch (ca[i]) {
					case '"': ca[i] = '\u2200'; nstate = 1; break;
					case '$': ca[i] = '\u2203'; nstate = 1; break;
					case '\'': ca[i] = '\u220D'; nstate = 1; break;
					case '*': ca[i] = '\u2217'; nstate = 1; break;
					case '-': ca[i] = '\u2212'; nstate = 1; break;
					case '@': ca[i] = '\u2245'; nstate = 1; break;
					case 'A': ca[i] = '\u0391'; nstate = 1; break;
					case 'B': ca[i] = '\u0392'; nstate = 1; break;
					case 'C': ca[i] = '\u03A7'; nstate = 1; break;
					case 'D': ca[i] = '\u0394'; nstate = 1; break;
					case 'E': ca[i] = '\u0395'; nstate = 1; break;
					case 'F': ca[i] = '\u03A6'; nstate = 1; break;
					case 'G': ca[i] = '\u0393'; nstate = 1; break;
					case 'H': ca[i] = '\u0397'; nstate = 1; break;
					case 'I': ca[i] = '\u0399'; nstate = 1; break;
					case 'J': ca[i] = '\u03D1'; nstate = 1; break;
					case 'K': ca[i] = '\u039A'; nstate = 1; break;
					case 'L': ca[i] = '\u039B'; nstate = 1; break;
					case 'M': ca[i] = '\u039C'; nstate = 1; break;
					case 'N': ca[i] = '\u039D'; nstate = 1; break;
					case 'O': ca[i] = '\u039F'; nstate = 1; break;
					case 'P': ca[i] = '\u03A0'; nstate = 1; break;
					case 'Q': ca[i] = '\u0398'; nstate = 1; break;
					case 'R': ca[i] = '\u03A1'; nstate = 1; break;
					case 'S': ca[i] = '\u03A3'; nstate = 1; break;
					case 'T': ca[i] = '\u03A4'; nstate = 1; break;
					case 'U': ca[i] = '\u03A5'; nstate = 1; break;
					case 'V': ca[i] = '\u03C3'; nstate = 1; break;
					case 'W': ca[i] = '\u03A9'; nstate = 1; break;
					case 'X': ca[i] = '\u039E'; nstate = 1; break;
					case 'Y': ca[i] = '\u03A8'; nstate = 1; break;
					case 'Z': ca[i] = '\u0396'; nstate = 1; break;
					case '\\': ca[i] = '\u2234'; nstate = 1; break;
					case '^': ca[i] = '\u22A5'; nstate = 1; break;
					case '`': ca[i] = '\uF8E5'; nstate = 1; break;
					case 'a': ca[i] = '\u03B1'; nstate = 1; break;
					case 'b': ca[i] = '\u03B2'; nstate = 1; break;
					case 'c': ca[i] = '\u03C7'; nstate = 1; break;
					case 'd': ca[i] = '\u03B4'; nstate = 1; break;
					case 'e': ca[i] = '\u03B5'; nstate = 1; break;
					case 'f': ca[i] = '\u03C6'; nstate = 1; break;
					case 'g': ca[i] = '\u03B3'; nstate = 1; break;
					case 'h': ca[i] = '\u03B7'; nstate = 1; break;
					case 'i': ca[i] = '\u03B9'; nstate = 1; break;
					case 'j': ca[i] = '\u03D5'; nstate = 1; break;
					case 'k': ca[i] = '\u03BA'; nstate = 1; break;
					case 'l': ca[i] = '\u03BB'; nstate = 1; break;
					case 'm': ca[i] = '\u03BC'; nstate = 1; break;
					case 'n': ca[i] = '\u03BD'; nstate = 1; break;
					case 'o': ca[i] = '\u03BF'; nstate = 1; break;
					case 'p': ca[i] = '\u03C0'; nstate = 1; break;
					case 'q': ca[i] = '\u03B8'; nstate = 1; break;
					case 'r': ca[i] = '\u03C1'; nstate = 1; break;
					case 's': ca[i] = '\u03C3'; nstate = 1; break;
					case 't': ca[i] = '\u03C4'; nstate = 1; break;
					case 'u': ca[i] = '\u03C5'; nstate = 1; break;
					case 'v': ca[i] = '\u03D6'; nstate = 1; break;
					case 'w': ca[i] = '\u03C9'; nstate = 1; break;
					case 'x': ca[i] = '\u03BE'; nstate = 1; break;
					case 'y': ca[i] = '\u03C8'; nstate = 1; break;
					case 'z': ca[i] = '\u03B6'; nstate = 1; break;
					case '~': ca[i] = '\u223C'; nstate = 1; break;
					case '\u00A0': ca[i] = '\u20AC'; nstate = 1; break;
					case '\u00A1': ca[i] = '\u03D2'; nstate = 1; break;
					case '\u00A2': ca[i] = '\u2032'; nstate = 1; break;
					case '\u00A3': ca[i] = '\u2264'; nstate = 1; break;
					case '\u00A4': ca[i] = '\u2044'; nstate = 1; break;
					case '\u00A5': ca[i] = '\u221E'; nstate = 1; break;
					case '\u00A6': ca[i] = '\u0192'; nstate = 1; break;
					case '\u00A7': ca[i] = '\u2663'; nstate = 1; break;
					case '\u00A8': ca[i] = '\u2666'; nstate = 1; break;
					case '\u00A9': ca[i] = '\u2665'; nstate = 1; break;
					case '\u00AA': ca[i] = '\u2660'; nstate = 1; break;
					case '\u00AB': ca[i] = '\u2194'; nstate = 1; break;
					case '\u00AC': ca[i] = '\u2190'; nstate = 1; break;
					case '\u00AD': ca[i] = '\u2191'; nstate = 1; break;
					case '\u00AE': ca[i] = '\u2192'; nstate = 1; break;
					case '\u00AF': ca[i] = '\u2193'; nstate = 1; break;
					case '\u00B2': ca[i] = '\u2033'; nstate = 1; break;
					case '\u00B3': ca[i] = '\u2265'; nstate = 1; break;
					case '\u00B4': ca[i] = '\u00D7'; nstate = 1; break;
					case '\u00B5': ca[i] = '\u221D'; nstate = 1; break;
					case '\u00B6': ca[i] = '\u2202'; nstate = 1; break;
					case '\u00B7': ca[i] = '\u2022'; nstate = 1; break;
					case '\u00B8': ca[i] = '\u00F7'; nstate = 1; break;
					case '\u00B9': ca[i] = '\u2260'; nstate = 1; break;
					case '\u00BA': ca[i] = '\u2261'; nstate = 1; break;
					case '\u00BB': ca[i] = '\u2248'; nstate = 1; break;
					case '\u00BC': ca[i] = '\u2026'; nstate = 1; break;
					case '\u00BD': ca[i] = '\u23D0'; nstate = 1; break;
					case '\u00BE': ca[i] = '\u23AF'; nstate = 1; break;
					case '\u00BF': ca[i] = '\u21B5'; nstate = 1; break;
					case '\u00C0': ca[i] = '\u2135'; nstate = 1; break;
					case '\u00C1': ca[i] = '\u2111'; nstate = 1; break;
					case '\u00C2': ca[i] = '\u211C'; nstate = 1; break;
					case '\u00C3': ca[i] = '\u2118'; nstate = 1; break;
					case '\u00C4': ca[i] = '\u2297'; nstate = 1; break;
					case '\u00C5': ca[i] = '\u2295'; nstate = 1; break;
					case '\u00C6': ca[i] = '\u2205'; nstate = 1; break;
					case '\u00C7': ca[i] = '\u2229'; nstate = 1; break;
					case '\u00C8': ca[i] = '\u222A'; nstate = 1; break;
					case '\u00C9': ca[i] = '\u2283'; nstate = 1; break;
					case '\u00CA': ca[i] = '\u2287'; nstate = 1; break;
					case '\u00CB': ca[i] = '\u2284'; nstate = 1; break;
					case '\u00CC': ca[i] = '\u2282'; nstate = 1; break;
					case '\u00CD': ca[i] = '\u2286'; nstate = 1; break;
					case '\u00CE': ca[i] = '\u2208'; nstate = 1; break;
					case '\u00CF': ca[i] = '\u2209'; nstate = 1; break;
					case '\u00D0': ca[i] = '\u2220'; nstate = 1; break;
					case '\u00D1': ca[i] = '\u2207'; nstate = 1; break;
					case '\u00D2': ca[i] = '\u00AE'; nstate = 1; break;
					case '\u00D3': ca[i] = '\u00A9'; nstate = 1; break;
					case '\u00D4': ca[i] = '\u2122'; nstate = 1; break;
					case '\u00D5': ca[i] = '\u220F'; nstate = 1; break;
					case '\u00D6': ca[i] = '\u221A'; nstate = 1; break;
					case '\u00D7': ca[i] = '\u22C5'; nstate = 1; break;
					case '\u00D8': ca[i] = '\u00AC'; nstate = 1; break;
					case '\u00D9': ca[i] = '\u2227'; nstate = 1; break;
					case '\u00DA': ca[i] = '\u2228'; nstate = 1; break;
					case '\u00DB': ca[i] = '\u21D4'; nstate = 1; break;
					case '\u00DC': ca[i] = '\u21D0'; nstate = 1; break;
					case '\u00DD': ca[i] = '\u21D1'; nstate = 1; break;
					case '\u00DE': ca[i] = '\u21D2'; nstate = 1; break;
					case '\u00DF': ca[i] = '\u21D3'; nstate = 1; break;
					case '\u00E0': ca[i] = '\u25CA'; nstate = 1; break;
					case '\u00E1': ca[i] = '\u3008'; nstate = 1; break;
					case '\u00E2': ca[i] = '\u00AE'; nstate = 2; break;
					case '\u00E3': ca[i] = '\u00A9'; nstate = 2; break;
					case '\u00E4': ca[i] = '\u2122'; nstate = 2; break;
					case '\u00E5': ca[i] = '\u2211'; nstate = 1; break;
					case '\u00E6': ca[i] = '\u239B'; nstate = 1; break;
					case '\u00E7': ca[i] = '\u239C'; nstate = 1; break;
					case '\u00E8': ca[i] = '\u239D'; nstate = 1; break;
					case '\u00E9': ca[i] = '\u23A1'; nstate = 1; break;
					case '\u00EA': ca[i] = '\u23A2'; nstate = 1; break;
					case '\u00EB': ca[i] = '\u23A3'; nstate = 1; break;
					case '\u00EC': ca[i] = '\u23A7'; nstate = 1; break;
					case '\u00ED': ca[i] = '\u23A8'; nstate = 1; break;
					case '\u00EE': ca[i] = '\u23A9'; nstate = 1; break;
					case '\u00EF': ca[i] = '\u23AA'; nstate = 1; break;
					case '\u00F0': ca[i] = '\uF8FF'; nstate = 1; break;
					case '\u00F1': ca[i] = '\u3009'; nstate = 1; break;
					case '\u00F2': ca[i] = '\u222B'; nstate = 1; break;
					case '\u00F3': ca[i] = '\u2320'; nstate = 1; break;
					case '\u00F4': ca[i] = '\u23AE'; nstate = 1; break;
					case '\u00F5': ca[i] = '\u2321'; nstate = 1; break;
					case '\u00F6': ca[i] = '\u239E'; nstate = 1; break;
					case '\u00F7': ca[i] = '\u239F'; nstate = 1; break;
					case '\u00F8': ca[i] = '\u23A0'; nstate = 1; break;
					case '\u00F9': ca[i] = '\u23A4'; nstate = 1; break;
					case '\u00FA': ca[i] = '\u23A5'; nstate = 1; break;
					case '\u00FB': ca[i] = '\u23A6'; nstate = 1; break;
					case '\u00FC': ca[i] = '\u23AB'; nstate = 1; break;
					case '\u00FD': ca[i] = '\u23AC'; nstate = 1; break;
					case '\u00FE': ca[i] = '\u23AD'; nstate = 1; break;
					case '\u00FF': ca[i] = '\u2192'; nstate = 1; break;
					default: nstate = 0;
					}

					if (nstate != state) {
						if (start < i) {
							Node text = doc.createTextNode(String.valueOf(ca, start, i-start));
							if (state == 0) {
								elem.appendChild(text);
							} else if (state == 1) {
								Element span = doc.createElement("tspan");
								span.setAttribute("font-family", "serif");
								span.appendChild(text);
								elem.appendChild(span);
							} else if (state == 2) {
								Element span = doc.createElement("tspan");
								span.setAttribute("font-family", "sans-serif");
								span.appendChild(text);
								elem.appendChild(span);
							}
							start = i;
						}
						state = nstate;
					}
				}

				if (start < ca.length) {
					Node text = doc.createTextNode(String.valueOf(ca, start, ca.length-start));
					if (state == 0) {
						elem.appendChild(text);
					} else if (state == 1) {
						Element span = doc.createElement("tspan");
						span.setAttribute("font-family", "serif");
						span.appendChild(text);
						elem.appendChild(span);
					} else if (state == 2) {
						Element span = doc.createElement("tspan");
						span.setAttribute("font-family", "sans-serif");
						span.appendChild(text);
						elem.appendChild(span);
					}
				}
				return;
			}
		}

		elem.appendChild(doc.createTextNode(str));
	}

	private void bmpToSvg(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy,
			int sw, int sh, int usage, long rop) {
		bmpToSvg(image, dx, dy, dw, dh, sx, sy, sw, sh, usage, rop, 1.0f, null);
	}

	private void bmpToSvg(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy,
			int sw, int sh, int usage, long rop, float opacity, Integer transparentColor) {
		if (image == null || image.length == 0) {
			return;
		}

		image = convertDibToPng(image, dh < 0, transparentColor);
		if (image == null || image.length == 0) {
			return;
		}

		StringBuffer buffer = new StringBuffer("data:image/png;base64,");
		buffer.append(Base64.encode(image));
		String data = buffer.toString();
		if (data == null || data.equals("")) {
			return;
		}

		boolean clipSource = (sx != 0 || sy != 0 || sw != dw || sh != dh);
		Element elem = doc.createElement(clipSource ? "svg" : "image");
		int x = (int)dc.toAbsoluteX(dx);
		int y = (int)dc.toAbsoluteY(dy);
		int width = (int)dc.toRelativeX(dw);
		int height = (int)dc.toRelativeY(dh);

		if (width < 0 && height < 0) {
			elem.setAttribute("transform", "scale(-1, -1) translate(" + -x + ", " + -y + ")");
		} else if (width < 0) {
			elem.setAttribute("transform", "scale(-1, 1) translate(" + -x + ", " + y + ")");
		} else if (height < 0) {
			elem.setAttribute("transform", "scale(1, -1) translate(" + x + ", " + -y + ")");
		} else {
			elem.setAttribute("x", "" + x);
			elem.setAttribute("y", "" + y);
		}

		elem.setAttribute("width", "" + Math.abs(width));
		elem.setAttribute("height", "" + Math.abs(height));
		if (clipSource) {
			Element imageNode = doc.createElement("image");
			int[] imageSize = ImageUtil.getSize(image);
			if (imageSize != null) {
				imageNode.setAttribute("width", "" + imageSize[0]);
				imageNode.setAttribute("height", "" + imageSize[1]);
			} else {
				imageNode.setAttribute("width", "" + Math.abs(sw));
				imageNode.setAttribute("height", "" + Math.abs(sh));
			}
			imageNode.setAttribute("xlink:href", data);
			elem.setAttribute("viewBox", "" + sx + " " + sy + " " + sw + " " + sh);
			elem.setAttribute("preserveAspectRatio", "none");
			elem.appendChild(imageNode);
		} else {
			elem.setAttribute("xlink:href", data);
		}

		String ropFilter = dc.getRopFilter(rop);
		if (ropFilter != null) {
			elem.setAttribute("filter", ropFilter);
		}
		if (opacity >= 0.0f && opacity < 1.0f) {
			elem.setAttribute("opacity", Float.toString(opacity));
		}

		parentNode.appendChild(elem);
	}

	private String createPngDataUri(byte[] image) {
		if (image == null || image.length == 0) {
			return null;
		}

		StringBuffer buffer = new StringBuffer("data:image/png;base64,");
		buffer.append(Base64.encode(image));
		String data = buffer.toString();
		if (data == null || data.equals("")) {
			return null;
		}
		return data;
	}

	private Element createBitmapMask(byte[] maskImage, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh) {
		int x = (int)dc.toAbsoluteX(dx);
		int y = (int)dc.toAbsoluteY(dy);
		int width = (int)dc.toRelativeX(dw);
		int height = (int)dc.toRelativeY(dh);
		int maskX = Math.min(x, x + width);
		int maskY = Math.min(y, y + height);
		int maskWidth = Math.abs(width);
		int maskHeight = Math.abs(height);
		Element mask = createImageMask(maskX, maskY, maskWidth, maskHeight);
		Element svg = appendMaskImage(mask, maskImage, sx, sy, sw, sh, maskWidth, maskHeight);
		if (svg == null) {
			defsNode.removeChild(mask);
			return null;
		}
		svg.setAttribute("x", Integer.toString(maskX));
		svg.setAttribute("y", Integer.toString(maskY));
		return mask;
	}

	private Element createTransformedBitmapMask(byte[] maskImage, int sx, int sy, int sw, int sh,
			double x0, double y0, double x1, double y1, double x2, double y2) {
		int width = Math.abs(sw);
		int height = Math.abs(sh);
		Element mask = createImageMask(
				(int)Math.floor(Math.min(Math.min(x0, x1), x2)),
				(int)Math.floor(Math.min(Math.min(y0, y1), y2)),
				(int)Math.ceil(Math.max(Math.max(x0, x1), x2) - Math.min(Math.min(x0, x1), x2)),
				(int)Math.ceil(Math.max(Math.max(y0, y1), y2) - Math.min(Math.min(y0, y1), y2)));
		Element svg = appendMaskImage(mask, maskImage, sx, sy, sw, sh);
		if (svg == null) {
			defsNode.removeChild(mask);
			return null;
		}
		svg.setAttribute("transform", "matrix("
				+ ((x1 - x0) / width) + " "
				+ ((y1 - y0) / width) + " "
				+ ((x2 - x0) / height) + " "
				+ ((y2 - y0) / height) + " "
				+ x0 + " " + y0 + ")");
		return mask;
	}

	private Element createImageMask(int x, int y, int width, int height) {
		Element mask = doc.createElement("mask");
		mask.setAttribute("id", "mask" + (maskNo++));
		mask.setIdAttribute("id", true);
		mask.setAttribute("maskUnits", "userSpaceOnUse");
		mask.setAttribute("x", Integer.toString(x));
		mask.setAttribute("y", Integer.toString(y));
		mask.setAttribute("width", Integer.toString(Math.max(1, width)));
		mask.setAttribute("height", Integer.toString(Math.max(1, height)));
		defsNode.appendChild(mask);
		return mask;
	}

	private Element appendMaskImage(Element mask, byte[] maskImage, int sx, int sy, int sw, int sh) {
		return appendMaskImage(mask, maskImage, sx, sy, sw, sh, Math.abs(sw), Math.abs(sh));
	}

	private Element appendMaskImage(Element mask, byte[] maskImage, int sx, int sy, int sw, int sh,
			int width, int height) {
		if (maskImage == null || maskImage.length == 0) {
			return null;
		}
		maskImage = convertDibToPng(maskImage, sh < 0, null);
		String data = createPngDataUri(maskImage);
		if (data == null) {
			return null;
		}
		Element svg = doc.createElement("svg");
		svg.setAttribute("width", Integer.toString(width));
		svg.setAttribute("height", Integer.toString(height));
		svg.setAttribute("viewBox", (sw < 0 ? sx + sw : sx) + " "
				+ (sh < 0 ? sy + sh : sy) + " " + width + " " + height);
		svg.setAttribute("preserveAspectRatio", "none");
		Element imageNode = doc.createElement("image");
		int[] imageSize = ImageUtil.getSize(maskImage);
		if (imageSize != null) {
			imageNode.setAttribute("width", "" + imageSize[0]);
			imageNode.setAttribute("height", "" + imageSize[1]);
		} else {
			imageNode.setAttribute("width", Integer.toString(width));
			imageNode.setAttribute("height", Integer.toString(height));
		}
		imageNode.setAttribute("xlink:href", data);
		svg.appendChild(imageNode);
		mask.appendChild(svg);
		return svg;
	}

	private byte[] convertDibToPng(byte[] dib, boolean reverse, Integer transparentColor) {
		if (transparentColor == null) {
			return ImageUtil.convert(dibToBmp(dib), "png", reverse);
		}

		try {
			BufferedImage source = ImageIO.read(new ByteArrayInputStream(dibToBmp(dib)));
			if (source == null) {
				return null;
			}
			BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
			int transparentRgb = ((transparentColor.intValue() & 0x000000FF) << 16)
					| (transparentColor.intValue() & 0x0000FF00)
					| ((transparentColor.intValue() & 0x00FF0000) >> 16);
			for (int y = 0; y < image.getHeight(); y++) {
				int sourceY = reverse ? image.getHeight() - 1 - y : y;
				for (int x = 0; x < image.getWidth(); x++) {
					int rgb = source.getRGB(x, sourceY) & 0x00FFFFFF;
					image.setRGB(x, y, rgb == transparentRgb ? rgb : (0xFF000000 | rgb));
				}
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private byte[] dibToBmp(byte[] dib) {
		byte[] data = new byte[14 + dib.length];

		/* BitmapFileHeader */
		data[0] = 0x42; // 'B'
		data[1] = 0x4d; // 'M'

		long bfSize = data.length;
		data[2] = (byte) (bfSize & 0xff);
		data[3] = (byte) ((bfSize >> 8) & 0xff);
		data[4] = (byte) ((bfSize >> 16) & 0xff);
		data[5] = (byte) ((bfSize >> 24) & 0xff);

		// reserved 1
		data[6] = 0x00;
		data[7] = 0x00;

		// reserved 2
		data[8] = 0x00;
		data[9] = 0x00;

		// offset
		long bfOffBits = 14;

		/* BitmapInfoHeader */
		long biSize = (dib[0] & 0xff) + ((dib[1] & 0xff) << 8)
				+ ((dib[2] & 0xff) << 16) + ((dib[3] & 0xff) << 24);
		bfOffBits += biSize;

		int biBitCount = (dib[14] & 0xff) + ((dib[15] & 0xff) << 8);

		long clrUsed = (dib[32] & 0xff) + ((dib[33] & 0xff) << 8)
				+ ((dib[34] & 0xff) << 16) + ((dib[35] & 0xff) << 24);

		switch (biBitCount) {
		case 1:
			bfOffBits += (clrUsed == 0L ? 2 : clrUsed) * 4;
			break;
		case 4:
			bfOffBits += (clrUsed == 0L ? 16 : clrUsed) * 4;
			break;
		case 8:
			bfOffBits += (clrUsed == 0L ? 256 : clrUsed) * 4;
			break;
		}

		data[10] = (byte) (bfOffBits & 0xff);
		data[11] = (byte) ((bfOffBits >> 8) & 0xff);
		data[12] = (byte) ((bfOffBits >> 16) & 0xff);
		data[13] = (byte) ((bfOffBits >> 24) & 0xff);

		System.arraycopy(dib, 0, data, 14, dib.length);

		return data;
	}
}

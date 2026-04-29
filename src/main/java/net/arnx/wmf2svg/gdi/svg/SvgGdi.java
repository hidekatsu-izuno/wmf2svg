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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.arnx.wmf2svg.gdi.emf.EmfParseException;
import net.arnx.wmf2svg.gdi.emf.EmfParser;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Size;
import net.arnx.wmf2svg.gdi.Trivertex;
import net.arnx.wmf2svg.util.Base64;
import net.arnx.wmf2svg.util.ImageUtil;
import net.arnx.wmf2svg.util.SymbolFontMappings;

/**
 * @author Hidekatsu Izuno
 * @author Shunsuke Mori
 */
public class SvgGdi implements Gdi {
	private static Logger log = Logger.getLogger(SvgGdi.class.getName());
	private static final String TRANSPARENT_MASK_ROP_USER_DATA = "wmf2svg-transparent-mask-rop";
	private static final String TRANSPARENT_MASK_ROP_SRCINVERT = "SRCINVERT";
	private static final long MASKBLT_BACKGROUND_SRCCOPY = 0xCC000000L;
	private static final long MASKBLT_FOREGROUND_DSTCOPY = 0x00AA0029L;
	private static final String PNG_DATA_URI_PREFIX = "data:image/png;base64,";
	private static final String SVG_DATA_URI_PREFIX = "data:image/svg+xml;base64,";
	private static final int EMF_PLUS_HEADER_SIZE = 12;
	private static final int EMF_PLUS_OBJECT = 0x4008;
	private static final int EMF_PLUS_DRAW_IMAGE_POINTS = 0x401B;
	private static final int EMF_PLUS_SAVE = 0x4025;
	private static final int EMF_PLUS_RESTORE = 0x4026;
	private static final int EMF_PLUS_SET_WORLD_TRANSFORM = 0x402A;
	private static final int EMF_PLUS_RESET_WORLD_TRANSFORM = 0x402B;
	private static final int EMF_PLUS_MULTIPLY_WORLD_TRANSFORM = 0x402C;
	private static final int EMF_PLUS_OBJECT_TYPE_IMAGE = 5;
	private static final int EMF_PLUS_IMAGE_DATA_TYPE_METAFILE = 2;
	private static final int DEFAULT_CANVAS_WIDTH = 330;
	private static final int DEFAULT_CANVAS_HEIGHT = 460;
	private static final int TARGET_DPI = 144;
	private static final int MAX_CANVAS_SIZE = 8192;
	private static final Pattern SVG_NUMBER_PATTERN = Pattern.compile("[-+]?(?:\\d*\\.\\d+|\\d+)(?:[eE][-+]?\\d+)?");

	private boolean compatible;

	private boolean replaceSymbolFont = false;

	private boolean parseEmfPlusComments = true;

	private Properties props = new Properties();

	private ByteArrayOutputStream emfBuffer;
	private int emfTotalSize;
	private ArrayList<PendingEmf> pendingEmfList = new ArrayList<PendingEmf>();
	private Map<Integer, byte[]> emfPlusMetafileImages = new HashMap<Integer, byte[]>();
	private Map<Integer, byte[]> emfPlusBitmapImages = new HashMap<Integer, byte[]>();
	private double[] emfPlusWorldTransform = new double[] { 1, 0, 0, 1, 0, 0 };
	private LinkedList<double[]> emfPlusWorldTransformStack = new LinkedList<double[]>();
	private Node emfPlusFallbackParent = null;
	private Node emfPlusFallbackKeepNode = null;
	private Node emfPlusFallbackRootKeepNode = null;
	private boolean pendingEmfBoundsSet = false;
	private int pendingEmfBoundsLeft = 0;
	private int pendingEmfBoundsTop = 0;
	private int pendingEmfBoundsRight = 0;
	private int pendingEmfBoundsBottom = 0;
	private int pendingEmfBoundsWidth = 0;
	private int pendingEmfBoundsHeight = 0;

	private SvgDc dc;

	private LinkedList<SvgDc> saveDC = new LinkedList<SvgDc>();

	private Document doc = null;

	private Element parentNode = null;

	private Element styleNode = null;

	private Element defsNode = null;

	private boolean placeableHeader = false;

	private int initialDpi = 1440;

	private int targetCanvasWidth = 0;

	private int targetCanvasHeight = 0;

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

	private byte[] lastDibPatternBrushImage;

	private int lastDibPatternBrushUsage = Gdi.DIB_RGB_COLORS;

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

	private void setParseEmfPlusComments(boolean flag) {
		parseEmfPlusComments = flag;
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
		placeableHeader = true;
		initialDpi = dpi;
		if (parentNode == null) {
			init();
		}

		dc.setWindowExtEx(Math.abs(wex - wsx), Math.abs(wey - wsy), null);
		dc.setDpi(dpi);

		Element root = doc.getDocumentElement();
		targetCanvasWidth = unitsToPixels(wsx, wex, dpi);
		targetCanvasHeight = unitsToPixels(wsy, wey, dpi);
		root.setAttribute("width", pixelsToCssInches(targetCanvasWidth));
		root.setAttribute("height", pixelsToCssInches(targetCanvasHeight));
	}

	private int unitsToPixels(int start, int end, int inch) {
		long denominator = Math.max(inch, 1);
		long numerator = (long)Math.max(Math.abs(end - start), 1) * TARGET_DPI;
		long pixels = (2 * numerator + denominator - 1) / (2 * denominator);
		if (start == 0 && (2 * numerator) % denominator == 0
				&& (((2 * numerator) / denominator) & 1) == 1) {
			pixels++;
		}
		if (pixels < 1) {
			return 1;
		}
		if (pixels > MAX_CANVAS_SIZE) {
			return MAX_CANVAS_SIZE;
		}
		return (int)pixels;
	}

	private String pixelsToCssInches(int pixels) {
		return Double.toString(pixels / 96.0) + "in";
	}

	boolean hasPlaceableHeader() {
		return placeableHeader;
	}

	public void header() {
		if (parentNode == null) {
			init();
		}
	}

	private void init() {
		dc = new SvgDc(this);
		dc.setDpi(initialDpi);

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
		boolean sourceAlpha = ((blendFunction >>> 24) & 0x01) != 0;
		bmpToSvg(image, dx, dy, dw, dh, sx, sy, sw, sh, Gdi.DIB_RGB_COLORS, SRCCOPY, opacity, null,
				sourceAlpha);
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

			elem = doc.createElement("path");
			elem.setAttribute("d", "M " + dc.toAbsoluteX(sx + cx) + "," + dc.toAbsoluteY(sy + cy)
					+ " A " + dc.toRelativeX(rx)  + "," + dc.toRelativeY(ry)
					+ " 0 " + getArcLargeFlag(sx, sy, ex, ey) + " " + getArcSweepFlag()
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
		Point start = getArcPoint(sxr, syr, exr, eyr, sxa, sya);
		Point end = getArcPoint(sxr, syr, exr, eyr, exa, eya);
		lineTo(start.x, start.y);
		arc(sxr, syr, exr, eyr, sxa, sya, exa, eya);
		dc.moveToEx(end.x, end.y, null);
	}

	private Point getArcPoint(int sxr, int syr, int exr, int eyr, int x, int y) {
		double rx = Math.abs(exr - sxr) / 2.0;
		double ry = Math.abs(eyr - syr) / 2.0;
		if (rx <= 0 || ry <= 0) {
			return new Point(x, y);
		}

		double cx = Math.min(sxr, exr) + rx;
		double cy = Math.min(syr, eyr) + ry;
		double angle = Math.atan2((y - cy) * rx, (x - cx) * ry);
		return new Point(
				(int)Math.round(rx * Math.cos(angle) + cx),
				(int)Math.round(ry * Math.sin(angle) + cy));
	}

	public void abortPath() {
		currentPath = null;
	}

	public void beginPath() {
		currentPath = new SvgPath();
	}

	public void bitBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, long rop) {
		if (isWmfBitmap(image)) {
			return;
		}
		bmpToSvg(image, dx, dy, dw, dh, sx, sy, dw, dh, Gdi.DIB_RGB_COLORS, rop);
	}

	public void maskBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, byte[] mask, int mx, int my, long rop) {
		if (mask == null) {
			bitBlt(image, dx, dy, dw, dh, sx, sy, rop);
			return;
		}
		if (!isSupportedMaskBltRop(rop)) {
			log.fine("unsupported MASKBLT ROP4: " + Long.toHexString(rop));
			return;
		}

		byte[] masked = createMaskedPng(image, mask, mx, my, false, isMaskBltCopyOnZeroMask(rop));
		if (masked != null) {
			appendPngToSvg(masked, dx, dy, dw, dh, sx, sy, dw, dh, Gdi.SRCCOPY, 1.0f);
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

	private boolean isSupportedMaskBltRop(long rop) {
		return (rop & 0xFF000000L) != 0
				&& ((rop & 0x00FFFFFFL) == Gdi.SRCCOPY || isMaskBltCopyOnZeroMask(rop));
	}

	private boolean isMaskBltCopyOnZeroMask(long rop) {
		return (rop & 0xFF000000L) == MASKBLT_BACKGROUND_SRCCOPY
				&& (rop & 0x00FFFFFFL) == MASKBLT_FOREGROUND_DSTCOPY;
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

		if (mask != null) {
			byte[] masked = createMaskedPng(image, mask, mx, my, sh < 0);
			if (masked != null) {
				image = masked;
			} else {
				image = convertDibToPng(image, sh < 0, null);
			}
		} else {
			image = convertDibToPng(image, sh < 0, null);
		}
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

		Element elem = doc.createElement("g");
		elem.setAttribute("transform", "matrix("
				+ ((x1 - x0) / sourceWidth) + " "
				+ ((y1 - y0) / sourceWidth) + " "
				+ ((x2 - x0) / sourceHeight) + " "
				+ ((y2 - y0) / sourceHeight) + " "
				+ x0 + " " + y0 + ")");

		Element viewport = doc.createElement("svg");
		viewport.setAttribute("width", Integer.toString(sourceWidth));
		viewport.setAttribute("height", Integer.toString(sourceHeight));
		viewport.setAttribute("viewBox", (sw < 0 ? sx + sw : sx) + " "
				+ (sh < 0 ? sy + sh : sy) + " " + sourceWidth + " " + sourceHeight);
		viewport.setAttribute("preserveAspectRatio", "none");

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
		viewport.appendChild(imageNode);
		elem.appendChild(viewport);
		if (mask != null && image != null && !hasAlpha(image)) {
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

			elem = doc.createElement("path");
			elem.setAttribute("d", "M " + dc.toAbsoluteX(sx + cx) + "," + dc.toAbsoluteY(sy + cy)
					+ " A " + dc.toRelativeX(rx)  + "," + dc.toRelativeY(ry)
					+ " 0 " + getArcLargeFlag(sx, sy, ex, ey) + " " + getArcSweepFlag()
					+ " " + dc.toAbsoluteX(ex + cx) + "," + dc.toAbsoluteY(ey + cy) + " Z");
		}

		if (dc.getPen() != null || dc.getBrush() != null) {
			elem.setAttribute("class", getClassString(dc.getPen(), dc.getBrush()));
			setMiterLimit(elem);
			setFillPattern(elem, dc.getBrush());
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

	public GdiColorSpace createColorSpaceW(byte[] logColorSpace) {
		return createColorSpace(logColorSpace);
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
		if (isMonochromeWmfBitmap(image) && lastDibPatternBrushImage != null) {
			return new SvgPatternBrush(this, lastDibPatternBrushImage, lastDibPatternBrushUsage);
		}
		return new SvgPatternBrush(this, image, Gdi.DIB_RGB_COLORS);
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
			String name = "rgn" + (rgnNo++);
			nameMap.put(rgn, name);
			Element elem = rgn.createElement();
			elem.setAttribute("id", name);
			elem.setIdAttribute("id", true);
			defsNode.appendChild(elem);
		}
		return rgn;
	}

	public GdiRegion extCreateRegion(float[] xform, int count, byte[] rgnData) {
		SvgRegion rgn = new SvgComplexRegion(this, xform, rgnData, count);
		String name = "rgn" + (rgnNo++);
		nameMap.put(rgn, name);
		Element elem = rgn.createElement();
		elem.setAttribute("id", name);
		elem.setIdAttribute("id", true);
		defsNode.appendChild(elem);
		return rgn;
	}

	public void deleteObject(GdiObject obj) {
		// Deleting a selected GDI object fails on Windows.  SVG output does not
		// keep a reusable object table, so deletion has no rendering side effect.
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
		lastDibPatternBrushImage = image;
		lastDibPatternBrushUsage = usage;
		return new SvgPatternBrush(this, image, usage);
	}

    public void dibStretchBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, long rop) {

    	this.stretchDIBits(dx, dy, dw, dh, sx, sy, sw, sh, image, Gdi.DIB_RGB_COLORS, rop);
    }

	public void ellipse(int sx, int sy, int ex, int ey) {
		if (currentPath != null) {
			currentPath.addEllipse(sx, sy, ex, ey);
			return;
		}

		Element elem = doc.createElement("ellipse");

		if (dc.getPen() != null || dc.getBrush() != null) {
			elem.setAttribute("class", getClassString(dc.getPen(), dc
					.getBrush()));
			setFillPattern(elem, dc.getBrush());
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
		if (parseEmfPlusComments && isEmfPlusComment(data)) {
			removeEmfPlusFallbackAfterSupportedDraw();
			parseEmfPlusComment(data);
			return;
		}

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
			byte[] bytesToParse = new byte[emfTotalSize];
			System.arraycopy(emfBuffer.toByteArray(), 0, bytesToParse, 0, emfTotalSize);
			pendingEmfList.add(new PendingEmf(bytesToParse, (SvgDc)dc.clone()));
			includePendingEmfBounds(bytesToParse);
			emfBuffer = null;
			emfTotalSize = 0;
		}
	}

	private boolean isEmfPlusComment(byte[] data) {
		return data.length >= 4
				&& data[0] == 'E'
				&& data[1] == 'M'
				&& data[2] == 'F'
				&& data[3] == '+';
	}

	private void parseEmfPlusComment(byte[] data) {
		int offset = 4;
		while (offset + EMF_PLUS_HEADER_SIZE <= data.length) {
			int type = readUInt16(data, offset);
			int flags = readUInt16(data, offset + 2);
			int size = readInt32(data, offset + 4);
			int dataSize = readInt32(data, offset + 8);
			if (size < EMF_PLUS_HEADER_SIZE || dataSize < 0 || dataSize > size - EMF_PLUS_HEADER_SIZE
					|| offset + size > data.length) {
				break;
			}

			byte[] payload = new byte[dataSize];
			System.arraycopy(data, offset + EMF_PLUS_HEADER_SIZE, payload, 0, dataSize);
			if (type == EMF_PLUS_OBJECT) {
				handleEmfPlusObject(flags, payload);
			} else if (type == EMF_PLUS_DRAW_IMAGE_POINTS) {
				handleEmfPlusDrawImagePoints(flags, payload);
			} else if (type == EMF_PLUS_SAVE) {
				emfPlusWorldTransformStack.addFirst(emfPlusWorldTransform.clone());
			} else if (type == EMF_PLUS_RESTORE) {
				if (!emfPlusWorldTransformStack.isEmpty()) {
					emfPlusWorldTransform = emfPlusWorldTransformStack.removeFirst();
				}
			} else if (type == EMF_PLUS_SET_WORLD_TRANSFORM) {
				setEmfPlusWorldTransform(payload);
			} else if (type == EMF_PLUS_RESET_WORLD_TRANSFORM) {
				emfPlusWorldTransform = new double[] { 1, 0, 0, 1, 0, 0 };
			} else if (type == EMF_PLUS_MULTIPLY_WORLD_TRANSFORM) {
				multiplyEmfPlusWorldTransform(payload);
			}
			offset += size;
		}
	}

	private void setEmfPlusWorldTransform(byte[] payload) {
		if (payload.length < 24) {
			return;
		}
		emfPlusWorldTransform = new double[] {
				readFloat(payload, 0),
				readFloat(payload, 4),
				readFloat(payload, 8),
				readFloat(payload, 12),
				readFloat(payload, 16),
				readFloat(payload, 20)
		};
	}

	private void multiplyEmfPlusWorldTransform(byte[] payload) {
		if (payload.length < 24) {
			return;
		}
		double[] matrix = new double[] {
				readFloat(payload, 0),
				readFloat(payload, 4),
				readFloat(payload, 8),
				readFloat(payload, 12),
				readFloat(payload, 16),
				readFloat(payload, 20)
		};
		emfPlusWorldTransform = multiplyEmfPlusMatrix(matrix, emfPlusWorldTransform);
	}

	private double[] multiplyEmfPlusMatrix(double[] a, double[] b) {
		return new double[] {
				a[0] * b[0] + a[1] * b[2],
				a[0] * b[1] + a[1] * b[3],
				a[2] * b[0] + a[3] * b[2],
				a[2] * b[1] + a[3] * b[3],
				a[4] * b[0] + a[5] * b[2] + b[4],
				a[4] * b[1] + a[5] * b[3] + b[5]
		};
	}

	private void handleEmfPlusObject(int flags, byte[] payload) {
		int objectId = flags & 0xFF;
		int objectType = (flags >>> 8) & 0x7F;
		if (objectType != EMF_PLUS_OBJECT_TYPE_IMAGE || payload.length < 16) {
			return;
		}

		int imageDataType = readInt32(payload, 4);
		if (imageDataType == EMF_PLUS_IMAGE_DATA_TYPE_METAFILE) {
			int metafileSize = readInt32(payload, 12);
			if (metafileSize <= 0 || 16 + metafileSize > payload.length) {
				return;
			}

			byte[] metafile = new byte[metafileSize];
			System.arraycopy(payload, 16, metafile, 0, metafileSize);
			emfPlusMetafileImages.put(Integer.valueOf(objectId), metafile);
			includePendingEmfBounds(metafile);
			return;
		}

		byte[] bitmap = readEmfPlusBitmapImage(payload);
		if (bitmap == null) {
			return;
		}
		emfPlusBitmapImages.put(Integer.valueOf(objectId), bitmap);
	}

	private void handleEmfPlusDrawImagePoints(int flags, byte[] payload) {
		int objectId = flags & 0xFF;
		byte[] bitmap = emfPlusBitmapImages.get(Integer.valueOf(objectId));
		byte[] metafile = emfPlusMetafileImages.get(Integer.valueOf(objectId));
		if (bitmap == null && metafile == null) {
			return;
		}
		if (payload.length < 28) {
			return;
		}

		float srcX = readFloat(payload, 8);
		float srcY = readFloat(payload, 12);
		float srcWidth = readFloat(payload, 16);
		float srcHeight = readFloat(payload, 20);
		int count = readInt32(payload, 24);
		if (count < 3 || srcWidth == 0 || srcHeight == 0) {
			return;
		}

		double[][] points = readEmfPlusImagePoints(payload, 28, count);
		if (points == null) {
			return;
		}

		String href;
		boolean suppressFallback = false;
		if (bitmap != null) {
			href = PNG_DATA_URI_PREFIX + Base64.encode(bitmap);
		} else {
			href = createSvgDataUri(metafile);
			suppressFallback = true;
		}
		if (href == null) {
			return;
		}

		Element image = doc.createElement("image");
		image.setAttribute("x", formatDouble(srcX));
		image.setAttribute("y", formatDouble(srcY));
		image.setAttribute("width", formatDouble(srcWidth));
		image.setAttribute("height", formatDouble(srcHeight));
		image.setAttribute("preserveAspectRatio", "none");
		image.setAttribute("xlink:href", href);

		double[] p0 = toEmfPlusLogicalPoint(points[0][0], points[0][1]);
		double[] p1 = toEmfPlusLogicalPoint(points[1][0], points[1][1]);
		double[] p2 = toEmfPlusLogicalPoint(points[2][0], points[2][1]);
		double[] unit = getEmfPlusImageUnitScale(p0, p1, p2, srcWidth, srcHeight);
		p0 = normalizeEmfPlusImagePoint(p0, unit);
		p1 = normalizeEmfPlusImagePoint(p1, unit);
		p2 = normalizeEmfPlusImagePoint(p2, unit);
		double x0 = p0[0];
		double y0 = p0[1];
		double x1 = p1[0];
		double y1 = p1[1];
		double x2 = p2[0];
		double y2 = p2[1];
		image.setAttribute("transform", "matrix("
				+ formatDouble((x1 - x0) / srcWidth) + " "
				+ formatDouble((y1 - y0) / srcWidth) + " "
				+ formatDouble((x2 - x0) / srcHeight) + " "
				+ formatDouble((y2 - y0) / srcHeight) + " "
				+ formatDouble(x0) + " " + formatDouble(y0) + ")");
		parentNode.appendChild(image);
		if (suppressFallback) {
			emfPlusFallbackParent = parentNode;
			emfPlusFallbackKeepNode = image;
			emfPlusFallbackRootKeepNode = doc.getDocumentElement().getLastChild();
		}
	}

	private byte[] readEmfPlusBitmapImage(byte[] payload) {
		int pngOffset = findBytes(payload, new byte[] {
				(byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
		});
		if (pngOffset >= 0) {
			byte[] image = new byte[payload.length - pngOffset];
			System.arraycopy(payload, pngOffset, image, 0, image.length);
			return image;
		}

		int jpegOffset = findBytes(payload, new byte[] { (byte)0xFF, (byte)0xD8, (byte)0xFF });
		if (jpegOffset >= 0) {
			byte[] image = new byte[payload.length - jpegOffset];
			System.arraycopy(payload, jpegOffset, image, 0, image.length);
			return ImageUtil.convert(image, "png", false);
		}
		return null;
	}

	private int findBytes(byte[] data, byte[] pattern) {
		for (int i = 0; i <= data.length - pattern.length; i++) {
			boolean match = true;
			for (int j = 0; j < pattern.length; j++) {
				if (data[i + j] != pattern[j]) {
					match = false;
					break;
				}
			}
			if (match) {
				return i;
			}
		}
		return -1;
	}

	private double[] toEmfPlusLogicalPoint(double x, double y) {
		double tx = emfPlusWorldTransform[0] * x + emfPlusWorldTransform[2] * y + emfPlusWorldTransform[4];
		double ty = emfPlusWorldTransform[1] * x + emfPlusWorldTransform[3] * y + emfPlusWorldTransform[5];
		return new double[] { tx, ty };
	}

	private double[] getEmfPlusImageUnitScale(double[] p0, double[] p1, double[] p2,
			double srcWidth, double srcHeight) {
		double scaleX = Math.hypot(p1[0] - p0[0], p1[1] - p0[1]) / Math.abs(srcWidth);
		double scaleY = Math.hypot(p2[0] - p0[0], p2[1] - p0[1]) / Math.abs(srcHeight);
		if (scaleX <= 0 || Double.isNaN(scaleX) || Double.isInfinite(scaleX)) {
			scaleX = 1;
		}
		if (scaleY <= 0 || Double.isNaN(scaleY) || Double.isInfinite(scaleY)) {
			scaleY = 1;
		}
		return new double[] { scaleX, scaleY };
	}

	private double[] normalizeEmfPlusImagePoint(double[] point, double[] unit) {
		return new double[] { point[0] / unit[0], point[1] / unit[1] };
	}

	private void removeEmfPlusFallbackAfterSupportedDraw() {
		if (emfPlusFallbackParent == null || emfPlusFallbackKeepNode == null) {
			return;
		}
		while (emfPlusFallbackKeepNode.getNextSibling() != null) {
			emfPlusFallbackParent.removeChild(emfPlusFallbackKeepNode.getNextSibling());
		}
		if (emfPlusFallbackRootKeepNode != null) {
			while (emfPlusFallbackRootKeepNode.getNextSibling() != null) {
				doc.getDocumentElement().removeChild(emfPlusFallbackRootKeepNode.getNextSibling());
			}
			if (!isInDocument(parentNode)) {
				parentNode = doc.createElement("g");
				doc.getDocumentElement().appendChild(parentNode);
			}
		}
		emfPlusFallbackParent = null;
		emfPlusFallbackKeepNode = null;
		emfPlusFallbackRootKeepNode = null;
	}

	private boolean isInDocument(Node node) {
		while (node != null) {
			if (node == doc.getDocumentElement()) {
				return true;
			}
			node = node.getParentNode();
		}
		return false;
	}

	private double[][] readEmfPlusImagePoints(byte[] payload, int offset, int count) {
		if (payload.length >= offset + count * 8) {
			double[][] points = new double[count][2];
			for (int i = 0; i < count; i++) {
				points[i][0] = readFloat(payload, offset + i * 8);
				points[i][1] = readFloat(payload, offset + i * 8 + 4);
			}
			return points;
		} else if (payload.length >= offset + count * 4) {
			double[][] points = new double[count][2];
			for (int i = 0; i < count; i++) {
				points[i][0] = readInt16(payload, offset + i * 4);
				points[i][1] = readInt16(payload, offset + i * 4 + 2);
			}
			return points;
		}
		return null;
	}

	private String createSvgDataUri(byte[] metafile) {
		try {
			SvgGdi svg = new SvgGdi(compatible);
			svg.setReplaceSymbolFont(replaceSymbolFont);
			svg.setParseEmfPlusComments(false);
			new EmfParser().parse(new ByteArrayInputStream(metafile), svg);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			svg.write(out);
			return SVG_DATA_URI_PREFIX + Base64.encode(out.toByteArray());
		} catch (IOException e) {
			log.fine("unsupported EMF+ metafile image: " + e.getMessage());
			return null;
		} catch (EmfParseException e) {
			log.fine("unsupported EMF+ metafile image: " + e.getMessage());
			return null;
		} catch (SvgGdiException e) {
			log.fine("unsupported EMF+ metafile image: " + e.getMessage());
			return null;
		}
	}

	private void includePendingEmfBounds(byte[] data) {
		if (data.length < 32 || readInt32(data, 0) != 1) {
			return;
		}
		int left = readInt32(data, 8);
		int top = readInt32(data, 12);
		int right = readInt32(data, 16);
		int bottom = readInt32(data, 20);
		int boundsLeft = Math.min(left, right);
		int boundsTop = Math.min(top, bottom);
		int boundsRight = Math.max(left, right);
		int boundsBottom = Math.max(top, bottom);
		if (!pendingEmfBoundsSet) {
			pendingEmfBoundsLeft = boundsLeft;
			pendingEmfBoundsTop = boundsTop;
			pendingEmfBoundsRight = boundsRight;
			pendingEmfBoundsBottom = boundsBottom;
			pendingEmfBoundsSet = true;
		} else {
			pendingEmfBoundsLeft = Math.min(pendingEmfBoundsLeft, boundsLeft);
			pendingEmfBoundsTop = Math.min(pendingEmfBoundsTop, boundsTop);
			pendingEmfBoundsRight = Math.max(pendingEmfBoundsRight, boundsRight);
			pendingEmfBoundsBottom = Math.max(pendingEmfBoundsBottom, boundsBottom);
		}
		pendingEmfBoundsWidth = Math.max(pendingEmfBoundsWidth, boundsRight - boundsLeft);
		pendingEmfBoundsHeight = Math.max(pendingEmfBoundsHeight, boundsBottom - boundsTop);
	}

	private void flushPendingEmf() {
		if (pendingEmfList.isEmpty()) {
			return;
		}

		ArrayList<PendingEmf> list = pendingEmfList;
		pendingEmfList = new ArrayList<PendingEmf>();
		SvgDc savedDc = dc;
		for (PendingEmf pendingEmf : list) {
			try {
				dc = (SvgDc)pendingEmf.dc.clone();
				new EmfParser(false).parse(new ByteArrayInputStream(pendingEmf.data), this);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} catch (EmfParseException e) {
				throw new IllegalStateException(e);
			} finally {
				dc = savedDc;
			}
		}
	}

	private static class PendingEmf {
		private final byte[] data;
		private final SvgDc dc;

		private PendingEmf(byte[] data, SvgDc dc) {
			this.data = data;
			this.dc = dc;
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
		appendFloodFillSeed(x, y);
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

		Element elem = createRegionElement(rgn);
		elem.setAttribute("class", getClassString(brush));
		SvgBrush sbrush = (SvgBrush)brush;
		setFillPattern(elem, sbrush);
		parentNode.appendChild(elem);
	}

	public void floodFill(int x, int y, int color) {
		appendFloodFillSeed(x, y);
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
			elem = createFrameRgnPath(new int[][] {new int[] {
					rectRgn.getLeft(), rectRgn.getTop(), rectRgn.getRight(), rectRgn.getBottom()
			}}, width, height);
		} else if (rgn instanceof SvgComplexRegion) {
			elem = createFrameRgnPath(((SvgComplexRegion)rgn).getRects(), width, height);
		} else {
			elem = doc.createElement("use");
			elem.setAttribute("xlink:href", "#" + nameMap.get(rgn));
			elem.setAttribute("fill", "none");
			elem.setAttribute("stroke-width", "" + Math.max(
					Math.abs((int)dc.toRelativeX(width)),
					Math.abs((int)dc.toRelativeY(height))));
		}
		if (elem == null) {
			return;
		}
		if (rgn instanceof SvgRectRegion || rgn instanceof SvgComplexRegion) {
			elem.setAttribute("stroke", "none");
			elem.setAttribute("class", getClassString(sbrush));
			setFillPattern(elem, sbrush);
		} else {
			elem.setAttribute("stroke", SvgObject.toColor(sbrush.getColor()));
		}
		parentNode.appendChild(elem);
	}

	private Element createFrameRgnPath(int[][] rects, int width, int height) {
		if (rects == null || rects.length == 0) {
			return null;
		}

		StringBuffer path = new StringBuffer();
		for (int i = 0; i < rects.length; i++) {
			int[] rect = rects[i];
			if (rect == null || rect.length < 4) {
				continue;
			}
			appendFrameRectPath(path, rect[0], rect[1], rect[2], rect[3], width, height);
		}
		if (path.length() == 0) {
			return null;
		}

		Element elem = doc.createElement("path");
		elem.setAttribute("d", path.toString());
		elem.setAttribute("fill-rule", "evenodd");
		return elem;
	}

	private void appendFrameRectPath(StringBuffer path, int left, int top, int right, int bottom,
			int width, int height) {
		double x1 = dc.toAbsoluteX(left);
		double y1 = dc.toAbsoluteY(top);
		double x2 = dc.toAbsoluteX(right);
		double y2 = dc.toAbsoluteY(bottom);
		double outerLeft = Math.min(x1, x2);
		double outerTop = Math.min(y1, y2);
		double outerRight = Math.max(x1, x2);
		double outerBottom = Math.max(y1, y2);
		double frameWidth = Math.min(Math.abs(dc.toRelativeX(width)), (outerRight - outerLeft) / 2.0);
		double frameHeight = Math.min(Math.abs(dc.toRelativeY(height)), (outerBottom - outerTop) / 2.0);
		if (frameWidth <= 0 || frameHeight <= 0) {
			return;
		}

		path.append("M ").append(outerLeft).append(",").append(outerTop)
				.append(" L ").append(outerRight).append(",").append(outerTop)
				.append(" L ").append(outerRight).append(",").append(outerBottom)
				.append(" L ").append(outerLeft).append(",").append(outerBottom)
				.append(" Z M ").append(outerLeft + frameWidth).append(",").append(outerTop + frameHeight)
				.append(" L ").append(outerLeft + frameWidth).append(",").append(outerBottom - frameHeight)
				.append(" L ").append(outerRight - frameWidth).append(",").append(outerBottom - frameHeight)
				.append(" L ").append(outerRight - frameWidth).append(",").append(outerTop + frameHeight)
				.append(" Z ");
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

		Element elem = createRegionElement(rgn);
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
			setFillPattern(elem, brush);
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

			elem = doc.createElement("path");
			elem.setAttribute("d", "M " + dc.toAbsoluteX(cx) + "," + dc.toAbsoluteY(cy)
					+ " L " + dc.toAbsoluteX(sx + cx) + "," + dc.toAbsoluteY(sy + cy)
					+ " A " + dc.toRelativeX(rx)  + "," + dc.toRelativeY(ry)
					+ " 0 " + getArcLargeFlag(sx, sy, ex, ey) + " " + getArcSweepFlag()
					+ " " + dc.toAbsoluteX(ex + cx) + "," + dc.toAbsoluteY(ey + cy) + " Z");
		}

		if (dc.getPen() != null || dc.getBrush() != null) {
			elem.setAttribute("class", getClassString(dc.getPen(), dc
					.getBrush()));
			setMiterLimit(elem);
			setFillPattern(elem, dc.getBrush());
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
			setFillPattern(elem, dc.getBrush());
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
			setFillPattern(elem, dc.getBrush());
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

		if (!parentNode.hasChildNodes() && parentNode.getParentNode() == doc.getDocumentElement()) {
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
			setFillPattern(elem, dc.getBrush());
		}

		elem.setAttribute("x", "" + (int)dc.toAbsoluteX(sx));
		elem.setAttribute("y", "" + (int)dc.toAbsoluteY(sy));
		elem.setAttribute("width", "" + (int)dc.toRelativeX(ex - sx));
		elem.setAttribute("height", "" + (int)dc.toRelativeY(ey - sy));
		parentNode.appendChild(elem);
	}

	public void resizePalette(GdiPalette palette, int entries) {
		// SVG output does not use palette capacity, so keep current entries.
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
			setFillPattern(elem, dc.getBrush());
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
		Element clip = createRegionElement(rgn);
		clip.setAttribute("fill", fill);
		return clip;
	}

	private Element createRegionElement(GdiRegion rgn) {
		if (rgn instanceof SvgRegion) {
			return ((SvgRegion)rgn).createElement();
		}
		Element elem = doc.createElement("use");
		elem.setAttribute("xlink:href", "#" + nameMap.get(rgn));
		return elem;
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
			clip.setAttribute("stroke-width", Double.toString(dc.toStrokeWidth(dc.getPen().getWidth())));
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

	private String getArcLargeFlag(double sx, double sy, double ex, double ey) {
		double start = Math.atan2(sy, sx);
		double end = Math.atan2(ey, ex);
		double sweep;
		if (dc.getArcDirection() == Gdi.AD_CLOCKWISE) {
			sweep = normalizeSweep(end - start);
		} else {
			sweep = normalizeSweep(start - end);
		}
		return (sweep > Math.PI) ? "1" : "0";
	}

	private double normalizeSweep(double angle) {
		double sweep = angle % (Math.PI * 2.0);
		if (sweep < 0) {
			sweep += Math.PI * 2.0;
		}
		return sweep;
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
		if (fill) {
			setFillPattern(elem, dc.getBrush());
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
		elem.setAttribute("stroke-width", Double.toString(dc.toStrokeWidth(dc.getPen().getWidth())));
		elem.setAttribute("stroke-linecap", "round");
		elem.setAttribute("stroke-linejoin", "round");

		if (brush != null && brush.getStyle() == GdiBrush.BS_SOLID) {
			elem.setAttribute("stroke", SvgObject.toColor(brush.getColor()));
		} else if (hasFillPattern(brush)) {
			String id = "pattern" + (patternNo++);
			Element pattern = brush.createFillPattern(id);
			if (pattern != null) {
				elem.setAttribute("stroke", "url(#" + id + ")");
				defsNode.appendChild(pattern);
			} else {
				elem.setAttribute("stroke", "none");
			}
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

		Element group = doc.createElement("g");
		group.setAttribute("stroke", "none");
		int steps = 24;
		for (int row = 0; row < steps; row++) {
			for (int col = 0; col < steps - row; col++) {
				double a1 = row / (double)steps;
				double b1 = col / (double)steps;
				double a2 = (row + 1) / (double)steps;
				double b2 = col / (double)steps;
				double a3 = row / (double)steps;
				double b3 = (col + 1) / (double)steps;
				appendGradientSubTriangle(group, v1, v2, v3, a1, b1, a2, b2, a3, b3);

				if (col < steps - row - 1) {
					double a4 = (row + 1) / (double)steps;
					double b4 = col / (double)steps;
					double a5 = (row + 1) / (double)steps;
					double b5 = (col + 1) / (double)steps;
					double a6 = row / (double)steps;
					double b6 = (col + 1) / (double)steps;
					appendGradientSubTriangle(group, v1, v2, v3, a4, b4, a5, b5, a6, b6);
				}
			}
		}
		parentNode.appendChild(group);
	}

	private void appendGradientSubTriangle(Element group, Trivertex v1, Trivertex v2, Trivertex v3,
			double a1, double b1, double a2, double b2, double a3, double b3) {
		Element elem = doc.createElement("polygon");
		elem.setAttribute("points",
				gradientPoint(v1, v2, v3, a1, b1) + " "
				+ gradientPoint(v1, v2, v3, a2, b2) + " "
				+ gradientPoint(v1, v2, v3, a3, b3));
		elem.setAttribute("fill", SvgObject.toColor(interpolateColor(v1, v2, v3,
				(a1 + a2 + a3) / 3.0, (b1 + b2 + b3) / 3.0)));
		group.appendChild(elem);
	}

	private String gradientPoint(Trivertex v1, Trivertex v2, Trivertex v3, double a, double b) {
		double c = 1.0 - a - b;
		double x = c * v1.x + a * v2.x + b * v3.x;
		double y = c * v1.y + a * v2.y + b * v3.y;
		return dc.toAbsoluteX(x) + "," + dc.toAbsoluteY(y);
	}

	private int interpolateColor(Trivertex v1, Trivertex v2, Trivertex v3, double a, double b) {
		double c = 1.0 - a - b;
		int red = (int)Math.round(c * (v1.red >>> 8) + a * (v2.red >>> 8) + b * (v3.red >>> 8));
		int green = (int)Math.round(c * (v1.green >>> 8) + a * (v2.green >>> 8) + b * (v3.green >>> 8));
		int blue = (int)Math.round(c * (v1.blue >>> 8) + a * (v2.blue >>> 8) + b * (v3.blue >>> 8));
		return (blue << 16) | (green << 8) | red;
	}

	private void appendGradientStop(Element gradient, String offset, int color) {
		Element stop = doc.createElement("stop");
		stop.setAttribute("offset", offset);
		stop.setAttribute("stop-color", SvgObject.toColor(color));
		gradient.appendChild(stop);
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

		public void addEllipse(int sx, int sy, int ex, int ey) {
			double kappa = 0.5522847498307936;
			double left = Math.min(sx, ex);
			double right = Math.max(sx, ex);
			double top = Math.min(sy, ey);
			double bottom = Math.max(sy, ey);
			double cx = (left + right) / 2.0;
			double cy = (top + bottom) / 2.0;
			double rx = (right - left) / 2.0;
			double ry = (bottom - top) / 2.0;
			double ox = rx * kappa;
			double oy = ry * kappa;

			moveTo(new Point((int)Math.round(cx + rx), (int)Math.round(cy)));
			bezierTo(
					new Point((int)Math.round(cx + rx), (int)Math.round(cy + oy)),
					new Point((int)Math.round(cx + ox), (int)Math.round(cy + ry)),
					new Point((int)Math.round(cx), (int)Math.round(cy + ry)));
			bezierTo(
					new Point((int)Math.round(cx - ox), (int)Math.round(cy + ry)),
					new Point((int)Math.round(cx - rx), (int)Math.round(cy + oy)),
					new Point((int)Math.round(cx - rx), (int)Math.round(cy)));
			bezierTo(
					new Point((int)Math.round(cx - rx), (int)Math.round(cy - oy)),
					new Point((int)Math.round(cx - ox), (int)Math.round(cy - ry)),
					new Point((int)Math.round(cx), (int)Math.round(cy - ry)));
			bezierTo(
					new Point((int)Math.round(cx + ox), (int)Math.round(cy - ry)),
					new Point((int)Math.round(cx + rx), (int)Math.round(cy - oy)),
					new Point((int)Math.round(cx + rx), (int)Math.round(cy)));
			close();
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
		int[] size = getDibSize(image);
		if (size != null && (sx < 0 || sy < 0 || dw < 0 || dh < 0
				|| sx + dw > size[0] || sy + scanlines > size[1])) {
			return;
		}
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

	public boolean setICMProfile(byte[] profileName) {
		dc.setICMProfile(profileName);
		return true;
	}

	public boolean colorMatchToTarget(int action, int flags, byte[] targetProfile) {
		dc.setICMProfile(targetProfile);
		return true;
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
		elem.setAttribute("width", "1");
		elem.setAttribute("height", "1");
		parentNode.appendChild(elem);
	}

	private void appendFloodFillSeed(int x, int y) {
		SvgBrush brush = dc.getBrush();
		if (brush == null) {
			return;
		}

		Element elem = doc.createElement("rect");
		elem.setAttribute("stroke", "none");
		elem.setAttribute("x", "" + (int)dc.toAbsoluteX(x));
		elem.setAttribute("y", "" + (int)dc.toAbsoluteY(y));
		elem.setAttribute("width", "" + (int)dc.toRelativeX(1));
		elem.setAttribute("height", "" + (int)dc.toRelativeY(1));
		if (hasFillPattern(brush)) {
			setFillPattern(elem, brush);
		} else {
			elem.setAttribute("class", getClassString(brush));
		}
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
		} else {
			dc.setTextSpace(0);
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
		if (isWmfBitmap(image)) {
			return;
		}
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
		flushPendingEmf();
		removeEmfPlusFallbackAfterSupportedDraw();

		Element root = doc.getDocumentElement();
		int width = dc.getViewportWidth() != 0 ? dc.getViewportWidth() : dc.getWindowWidth();
		int height = dc.getViewportHeight() != 0 ? dc.getViewportHeight() : dc.getWindowHeight();
		int x = dc.getViewportWidth() != 0 ? dc.getViewportX() : (placeableHeader ? 0 : dc.getWindowX());
		int y = dc.getViewportHeight() != 0 ? dc.getViewportY() : (placeableHeader ? 0 : dc.getWindowY());
		double[] contentBounds = getRootContentBounds(root);
		double[] physicalCanvasBounds = getPhysicalCanvasBounds(root);
		double[] canvasBounds = getCanvasBounds(x, y, width, height, contentBounds, physicalCanvasBounds);
		if (canvasBounds != null) {
			x = (int)Math.floor(canvasBounds[0]);
			y = (int)Math.floor(canvasBounds[1]);
			width = (int)Math.ceil(canvasBounds[2]) - x;
			height = (int)Math.ceil(canvasBounds[3]) - y;
		}
		if (!root.hasAttribute("width") && width != 0) {
			root.setAttribute("width", "" + Math.abs(width));
		}
		if (!root.hasAttribute("height") && height != 0) {
			root.setAttribute("height", "" + Math.abs(height));
		}
		if (!root.hasAttribute("width")) {
			root.setAttribute("width", "" + DEFAULT_CANVAS_WIDTH);
		}
		if (!root.hasAttribute("height")) {
			root.setAttribute("height", "" + DEFAULT_CANVAS_HEIGHT);
		}
		if (width != 0 && height != 0) {
			root.setAttribute("viewBox", x + " " + y + " " + Math.abs(width) + " " + Math.abs(height));
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

	private double[] getCanvasBounds(int x, int y, int width, int height, double[] contentBounds,
			double[] physicalCanvasBounds) {
		if (width != 0 && height != 0) {
			double logicalRight = x + Math.abs(width);
			double logicalBottom = y + Math.abs(height);
			double[] bounds = new double[] { x, y, logicalRight, logicalBottom };
			if (placeableHeader && pendingEmfBoundsSet
					&& pendingEmfBoundsLeft == 0 && pendingEmfBoundsTop == 0) {
				bounds = getPendingEmfBounds();
			} else if (physicalCanvasBounds != null && isPhysicalCanvasLargerThanLogical(width, height, physicalCanvasBounds)) {
				bounds = physicalCanvasBounds;
			} else if (!placeableHeader && dc.getWindowX() == 0 && dc.getWindowY() == 0
					&& pendingEmfBoundsWidth > 0 && pendingEmfBoundsHeight > 0) {
				bounds[2] = Math.max(bounds[2], x + pendingEmfBoundsWidth);
				bounds[3] = Math.max(bounds[3], y + pendingEmfBoundsHeight);
			}
			return bounds;
		}
		if (physicalCanvasBounds != null) {
			return physicalCanvasBounds;
		}
		if (!placeableHeader && contentBounds != null) {
			return addBounds(new double[] { 0.0, 0.0, DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT },
					contentBounds);
		}
		if (contentBounds != null) {
			return contentBounds;
		}
		if (!placeableHeader) {
			return new double[] { 0.0, 0.0, DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT };
		}
		return null;
	}

	private double[] getPendingEmfBounds() {
		return new double[] {
				pendingEmfBoundsLeft,
				pendingEmfBoundsTop,
				pendingEmfBoundsRight,
				pendingEmfBoundsBottom
		};
	}

	private double[] getContentBounds(Element elem) {
		if (elem == null) {
			return null;
		}
		double[] bounds = null;
		if (elem.hasAttribute("points")) {
			bounds = addNumbersToBounds(bounds, elem.getAttribute("points"), true);
		}
		if (elem.hasAttribute("d")) {
			bounds = addNumbersToBounds(bounds, elem.getAttribute("d"), true);
		}
		if (elem.hasAttribute("x") && elem.hasAttribute("y")) {
			bounds = addCoordinateListsToBounds(bounds, elem.getAttribute("x"),
					elem.getAttribute("y"));
		}
		if (elem.hasAttribute("x1") && elem.hasAttribute("y1")) {
			bounds = addPointToBounds(bounds, readFirstNumber(elem.getAttribute("x1")),
					readFirstNumber(elem.getAttribute("y1")));
		}
		if (elem.hasAttribute("x2") && elem.hasAttribute("y2")) {
			bounds = addPointToBounds(bounds, readFirstNumber(elem.getAttribute("x2")),
					readFirstNumber(elem.getAttribute("y2")));
		}
		if (elem.hasAttribute("cx") && elem.hasAttribute("cy")) {
			Double cx = readFirstNumber(elem.getAttribute("cx"));
			Double cy = readFirstNumber(elem.getAttribute("cy"));
			Double rx = elem.hasAttribute("rx") ? readFirstNumber(elem.getAttribute("rx"))
					: readFirstNumber(elem.getAttribute("r"));
			Double ry = elem.hasAttribute("ry") ? readFirstNumber(elem.getAttribute("ry")) : rx;
			if (cx != null && cy != null && rx != null && ry != null) {
				bounds = addPointToBounds(bounds, cx.doubleValue() - Math.abs(rx.doubleValue()),
						cy.doubleValue() - Math.abs(ry.doubleValue()));
				bounds = addPointToBounds(bounds, cx.doubleValue() + Math.abs(rx.doubleValue()),
						cy.doubleValue() + Math.abs(ry.doubleValue()));
			}
		}
		if (elem.hasAttribute("width") && elem.hasAttribute("height")) {
			Double x = readFirstNumber(elem.getAttribute("x"));
			Double y = readFirstNumber(elem.getAttribute("y"));
			Double width = readFirstNumber(elem.getAttribute("width"));
			Double height = readFirstNumber(elem.getAttribute("height"));
			if (x != null && y != null && width != null && height != null) {
				bounds = addPointToBounds(bounds, x.doubleValue() + width.doubleValue(),
						y.doubleValue() + height.doubleValue());
			}
		}
		for (Node node = elem.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element) {
				bounds = addBounds(bounds, getContentBounds((Element)node));
			}
		}
		return bounds;
	}

	private double[] getPhysicalCanvasBounds(Element root) {
		if (targetCanvasWidth > 0 && targetCanvasHeight > 0) {
			return new double[] { 0.0, 0.0, targetCanvasWidth, targetCanvasHeight };
		}
		Double width = readInches(root.getAttribute("width"));
		Double height = readInches(root.getAttribute("height"));
		if (width == null || height == null) {
			return null;
		}
		return new double[] { 0.0, 0.0, Math.round(width.doubleValue() * 96.0),
				Math.round(height.doubleValue() * 96.0) };
	}

	private boolean isPhysicalCanvasLargerThanLogical(int width, int height, double[] physicalCanvasBounds) {
		double physicalWidth = physicalCanvasBounds[2] - physicalCanvasBounds[0];
		double physicalHeight = physicalCanvasBounds[3] - physicalCanvasBounds[1];
		return physicalWidth > Math.abs(width) * 2.0 || physicalHeight > Math.abs(height) * 2.0;
	}

	private Double readInches(String value) {
		if (value == null || !value.endsWith("in")) {
			return null;
		}
		try {
			return Double.valueOf(value.substring(0, value.length() - 2));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private double[] getRootContentBounds(Element root) {
		double[] bounds = null;
		for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element && node != defsNode && node != styleNode) {
				bounds = addBounds(bounds, getContentBounds((Element)node));
			}
		}
		return bounds;
	}


	private double[] addNumbersToBounds(double[] bounds, String value, boolean paired) {
		double[] numbers = readNumbers(value);
		for (int i = 0; i + 1 < numbers.length; i += paired ? 2 : 1) {
			bounds = addPointToBounds(bounds, numbers[i], numbers[i + 1]);
		}
		return bounds;
	}

	private double[] addCoordinateListsToBounds(double[] bounds, String xValue, String yValue) {
		double[] xs = readNumbers(xValue);
		double[] ys = readNumbers(yValue);
		if (xs.length == 0 || ys.length == 0) {
			return bounds;
		}
		int count = Math.max(xs.length, ys.length);
		for (int i = 0; i < count; i++) {
			double x = xs[Math.min(i, xs.length - 1)];
			double y = ys[Math.min(i, ys.length - 1)];
			bounds = addPointToBounds(bounds, x, y);
		}
		return bounds;
	}

	private double[] readNumbers(String value) {
		if (value == null || value.length() == 0) {
			return new double[0];
		}
		ArrayList<Double> values = new ArrayList<Double>();
		Matcher matcher = SVG_NUMBER_PATTERN.matcher(value);
		while (matcher.find()) {
			values.add(Double.valueOf(matcher.group()));
		}
		double[] numbers = new double[values.size()];
		for (int i = 0; i < values.size(); i++) {
			numbers[i] = values.get(i).doubleValue();
		}
		return numbers;
	}

	private Double readFirstNumber(String value) {
		double[] numbers = readNumbers(value);
		if (numbers.length == 0) {
			return null;
		}
		return Double.valueOf(numbers[0]);
	}

	private double[] addPointToBounds(double[] bounds, Double x, Double y) {
		if (x == null || y == null) {
			return bounds;
		}
		return addPointToBounds(bounds, x.doubleValue(), y.doubleValue());
	}

	private double[] addPointToBounds(double[] bounds, double x, double y) {
		if (bounds == null) {
			return new double[] { x, y, x, y };
		}
		if (x < bounds[0]) bounds[0] = x;
		if (y < bounds[1]) bounds[1] = y;
		if (x > bounds[2]) bounds[2] = x;
		if (y > bounds[3]) bounds[3] = y;
		return bounds;
	}

	private double[] addBounds(double[] bounds, double[] other) {
		if (other == null) {
			return bounds;
		}
		bounds = addPointToBounds(bounds, other[0], other[1]);
		return addPointToBounds(bounds, other[2], other[3]);
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

	private boolean hasFillPattern(SvgBrush brush) {
		return brush != null && (brush.getStyle() == GdiBrush.BS_HATCHED
				|| brush instanceof SvgPatternBrush);
	}

	private void setFillPattern(Element elem, SvgBrush brush) {
		if (!hasFillPattern(brush)) {
			return;
		}

		String id = "pattern" + (patternNo++);
		Element pattern = brush.createFillPattern(id);
		if (pattern != null) {
			elem.setAttribute("fill", "url(#" + id + ")");
			defsNode.appendChild(pattern);
		}
	}

	private void appendText(Element elem, String str) {
		if (compatible) {
			str = str.replaceAll("\\r\\n|[\\t\\r\\n ]", "\u00A0");
		}
		SvgFont font = dc.getFont();
		if (replaceSymbolFont && font != null) {
			if (SymbolFontMappings.isMappedFont(font.getFaceName())) {
				Element span = doc.createElement("tspan");
				span.setAttribute("font-family", SymbolFontMappings.REPLACEMENT_FONT_FAMILY);
				span.appendChild(doc.createTextNode(SymbolFontMappings.replace(font.getFaceName(), str)));
				elem.appendChild(span);
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
		bmpToSvg(image, dx, dy, dw, dh, sx, sy, sw, sh, usage, rop, opacity, transparentColor, false);
	}

	private void bmpToSvg(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy,
			int sw, int sh, int usage, long rop, float opacity, Integer transparentColor,
			boolean preserveAlpha) {
		if (image == null || image.length == 0) {
			return;
		}

		image = convertDibToPng(image, usage, sh < 0, transparentColor, preserveAlpha);
		if (image == null || image.length == 0) {
			return;
		}

		String data = createPngDataUri(image);
		if (data == null) {
			return;
		}

		appendPngToSvg(image, dx, dy, dw, dh, sx, sy, sw, sh, rop, opacity);
	}

	private void appendPngToSvg(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy,
			int sw, int sh, long rop, float opacity) {
		String data = createPngDataUri(image);
		if (data == null) {
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
			elem.setAttribute("viewBox", "" + (sw < 0 ? sx + sw : sx) + " "
					+ (sh < 0 ? sy + sh : sy) + " " + Math.abs(sw) + " " + Math.abs(sh));
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

		if (!clipSource && rop == Gdi.SRCINVERT) {
			if (mergeTransparentMaskWithPreviousImage(elem, image)) {
				return;
			}
			elem.setUserData(TRANSPARENT_MASK_ROP_USER_DATA, TRANSPARENT_MASK_ROP_SRCINVERT, null);
		}

		parentNode.appendChild(elem);
	}

	String createPngDataUri(byte[] image) {
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

	private boolean mergeTransparentMaskWithPreviousImage(Element elem, byte[] image) {
		Node previous = parentNode.getLastChild();
		if (!(previous instanceof Element)) {
			return false;
		}

		Element previousElem = (Element)previous;
		if (!"image".equals(previousElem.getTagName())
				|| !TRANSPARENT_MASK_ROP_SRCINVERT.equals(previousElem.getUserData(TRANSPARENT_MASK_ROP_USER_DATA))
				|| !hasSameImageGeometry(previousElem, elem)) {
			return false;
		}

		byte[] previousImage = readPngDataUri(previousElem.getAttribute("xlink:href"));
		byte[] merged = createTransparentMaskPng(previousImage, image);
		if (merged == null) {
			return false;
		}

		previousElem.setAttribute("xlink:href", createPngDataUri(merged));
		previousElem.setUserData(TRANSPARENT_MASK_ROP_USER_DATA, null, null);
		return true;
	}

	private boolean hasSameImageGeometry(Element first, Element second) {
		return first.getAttribute("x").equals(second.getAttribute("x"))
				&& first.getAttribute("y").equals(second.getAttribute("y"))
				&& first.getAttribute("width").equals(second.getAttribute("width"))
				&& first.getAttribute("height").equals(second.getAttribute("height"))
				&& first.getAttribute("transform").equals(second.getAttribute("transform"))
				&& first.getAttribute("opacity").equals(second.getAttribute("opacity"))
				&& first.getAttribute("filter").equals(second.getAttribute("filter"));
	}

	private byte[] readPngDataUri(String data) {
		if (data == null || !data.startsWith(PNG_DATA_URI_PREFIX)) {
			return null;
		}
		try {
			return java.util.Base64.getDecoder().decode(data.substring(PNG_DATA_URI_PREFIX.length()));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private byte[] createTransparentMaskPng(byte[] maskImage, byte[] currentImage) {
		if (maskImage == null || currentImage == null) {
			return null;
		}
		try {
			BufferedImage mask = ImageIO.read(new ByteArrayInputStream(maskImage));
			BufferedImage current = ImageIO.read(new ByteArrayInputStream(currentImage));
			if (mask == null || current == null) {
				return null;
			}
			if (mask.getWidth() != current.getWidth() || mask.getHeight() != current.getHeight()) {
				mask = scaleImage(mask, current.getWidth(), current.getHeight());
			}

			BufferedImage result = new BufferedImage(current.getWidth(), current.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < result.getHeight(); y++) {
				for (int x = 0; x < result.getWidth(); x++) {
					int maskRgb = mask.getRGB(x, y) & 0x00FFFFFF;
					int currentRgb = current.getRGB(x, y) & 0x00FFFFFF;
					if (maskRgb == currentRgb) {
						result.setRGB(x, y, currentRgb);
					} else {
						result.setRGB(x, y, 0xFF000000 | currentRgb);
					}
				}
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(result, "png", out);
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private BufferedImage scaleImage(BufferedImage source, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try {
			g.drawImage(source, 0, 0, width, height, null);
		} finally {
			g.dispose();
		}
		return image;
	}

	byte[] convertBrushPatternToPng(byte[] image, int usage) {
		try {
			return convertDibToPng(image, usage, false, null);
		} catch (RuntimeException e) {
			log.fine("unsupported pattern brush bitmap: " + e.getMessage());
			return null;
		}
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
		double x3 = x1 + x2 - x0;
		double y3 = y1 + y2 - y0;
		double minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
		double minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
		double maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
		double maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));
		Element mask = createImageMask(
				(int)Math.floor(minX),
				(int)Math.floor(minY),
				(int)Math.ceil(maxX - minX),
				(int)Math.ceil(maxY - minY));
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
		mask.setAttribute("mask-type", "luminance");
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

	private byte[] createMaskedPng(byte[] image, byte[] mask, int mx, int my, boolean reverse) {
		return createMaskedPng(image, mask, mx, my, reverse, false);
	}

	private byte[] createMaskedPng(byte[] image, byte[] mask, int mx, int my, boolean reverse, boolean invertMask) {
		BufferedImage source = decodeDib(applyPaletteToDib(image, Gdi.DIB_RGB_COLORS), reverse, null, true);
		if (source == null) {
			source = decodeWmfBitmap(image, reverse, null, true);
		}
		BufferedImage maskImage = decodeDib(applyPaletteToDib(mask, Gdi.DIB_RGB_COLORS), false, null, false);
		if (source == null || maskImage == null) {
			return null;
		}

		BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < result.getHeight(); y++) {
			int maskY = Math.max(0,
					Math.min(maskImage.getHeight() - 1, my + y * maskImage.getHeight() / result.getHeight()));
			for (int x = 0; x < result.getWidth(); x++) {
				int maskX = Math.max(0,
						Math.min(maskImage.getWidth() - 1, mx + x * maskImage.getWidth() / result.getWidth()));
				int maskRgb = maskImage.getRGB(maskX, maskY);
				int red = (maskRgb >> 16) & 0xFF;
				int green = (maskRgb >> 8) & 0xFF;
				int blue = maskRgb & 0xFF;
				int alpha = (red + green + blue) / 3;
				if (invertMask) {
					alpha = 0xFF - alpha;
				}
				result.setRGB(x, y, (source.getRGB(x, y) & 0x00FFFFFF) | (alpha << 24));
			}
		}

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(result, "png", out);
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private boolean hasAlpha(byte[] image) {
		try {
			BufferedImage source = ImageIO.read(new ByteArrayInputStream(image));
			return source != null && source.getColorModel().hasAlpha();
		} catch (IOException e) {
			return false;
		}
	}

	private byte[] convertDibToPng(byte[] dib, boolean reverse, Integer transparentColor) {
		return convertDibToPng(dib, Gdi.DIB_RGB_COLORS, reverse, transparentColor, false);
	}

	private byte[] convertDibToPng(byte[] dib, int usage, boolean reverse, Integer transparentColor) {
		return convertDibToPng(dib, usage, reverse, transparentColor, false);
	}

	private byte[] convertDibToPng(byte[] dib, int usage, boolean reverse, Integer transparentColor,
			boolean preserveAlpha) {
		dib = applyPaletteToDib(dib, usage);
		BufferedImage decoded = decodeWmfBitmap(dib, reverse, transparentColor, preserveAlpha);
		if (decoded == null) {
			decoded = decodeDib(dib, reverse, transparentColor, preserveAlpha);
		}
		if (decoded != null) {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.write(decoded, "png", out);
				return out.toByteArray();
			} catch (IOException e) {
				return null;
			}
		}

		if (transparentColor == null && !preserveAlpha) {
			return ImageUtil.convert(dibToBmp(dib), "png", reverse);
		}

		try {
			BufferedImage source = ImageIO.read(new ByteArrayInputStream(dibToBmp(dib)));
			if (source == null) {
				return null;
			}
			BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
			int transparentRgb = transparentColor != null
					? ((transparentColor.intValue() & 0x000000FF) << 16)
							| (transparentColor.intValue() & 0x0000FF00)
							| ((transparentColor.intValue() & 0x00FF0000) >> 16)
					: -1;
			for (int y = 0; y < image.getHeight(); y++) {
				int sourceY = reverse ? image.getHeight() - 1 - y : y;
				for (int x = 0; x < image.getWidth(); x++) {
					int argb = source.getRGB(x, sourceY);
					int rgb = argb & 0x00FFFFFF;
					if (transparentColor != null && rgb == transparentRgb) {
						image.setRGB(x, y, rgb);
					} else if (preserveAlpha) {
						image.setRGB(x, y, argb);
					} else {
						image.setRGB(x, y, 0xFF000000 | rgb);
					}
				}
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private BufferedImage decodeWmfBitmap(byte[] bitmap, boolean reverse, Integer transparentColor,
			boolean preserveAlpha) {
		if (bitmap == null || bitmap.length < 10 || readUInt16(bitmap, 0) != 0) {
			return null;
		}

		int width = readUInt16(bitmap, 2);
		int height = readUInt16(bitmap, 4);
		int stride = readUInt16(bitmap, 6);
		int planes = bitmap[8] & 0xFF;
		int bitCount = bitmap[9] & 0xFF;
		if (width <= 0 || height <= 0 || planes <= 0 || stride <= 0
				|| (bitCount != 1 && bitCount != 4 && bitCount != 8 && bitCount != 24 && bitCount != 32)) {
			return null;
		}

		int bitsOffset = 10;
		if (bitmap.length < bitsOffset + stride * height) {
			return null;
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			int sourceY = reverse ? height - 1 - y : y;
			int row = bitsOffset + sourceY * stride;
			for (int x = 0; x < width; x++) {
				image.setRGB(x, y, readWmfBitmapPixel(bitmap, row, x, bitCount, transparentColor, preserveAlpha));
			}
		}
		return image;
	}

	private int readWmfBitmapPixel(byte[] bitmap, int row, int x, int bitCount,
			Integer transparentColor, boolean preserveAlpha) {
		if (bitCount == 1) {
			int index = (bitmap[row + x / 8] >>> (7 - (x % 8))) & 0x01;
			return index == 0 ? 0xFFFFFFFF : 0xFF000000;
		} else if (bitCount == 4) {
			int value = bitmap[row + x / 2] & 0xFF;
			int index = (x % 2 == 0) ? (value >>> 4) : (value & 0x0F);
			int gray = (index << 4) | index;
			return applyAlpha(gray, gray, gray, 0xFF, transparentColor, preserveAlpha);
		} else if (bitCount == 8) {
			int gray = bitmap[row + x] & 0xFF;
			return applyAlpha(gray, gray, gray, 0xFF, transparentColor, preserveAlpha);
		} else if (bitCount == 24) {
			int pos = row + x * 3;
			int blue = bitmap[pos] & 0xFF;
			int green = bitmap[pos + 1] & 0xFF;
			int red = bitmap[pos + 2] & 0xFF;
			return applyAlpha(red, green, blue, 0xFF, transparentColor, preserveAlpha);
		} else if (bitCount == 32) {
			int pos = row + x * 4;
			int blue = bitmap[pos] & 0xFF;
			int green = bitmap[pos + 1] & 0xFF;
			int red = bitmap[pos + 2] & 0xFF;
			int alpha = preserveAlpha ? (bitmap[pos + 3] & 0xFF) : 0xFF;
			return applyAlpha(red, green, blue, alpha, transparentColor, preserveAlpha);
		}
		return 0x00000000;
	}

	private boolean isMonochromeWmfBitmap(byte[] bitmap) {
		return bitmap != null
				&& bitmap.length >= 10
				&& readUInt16(bitmap, 0) == 0
				&& (bitmap[8] & 0xFF) > 0
				&& (bitmap[9] & 0xFF) == 1;
	}

	private boolean isWmfBitmap(byte[] bitmap) {
		return bitmap != null
				&& bitmap.length >= 10
				&& readUInt16(bitmap, 0) == 0
				&& (bitmap[8] & 0xFF) > 0;
	}

	private int[] getDibSize(byte[] dib) {
		if (dib == null || dib.length < 16 || readInt32(dib, 0) < 40) {
			return null;
		}
		int width = readInt32(dib, 4);
		int height = Math.abs(readInt32(dib, 8));
		return width > 0 && height > 0 ? new int[] { width, height } : null;
	}

	private BufferedImage decodeDib(byte[] dib, boolean reverse, Integer transparentColor, boolean preserveAlpha) {
		if (dib == null || dib.length < 40) {
			return null;
		}

		int headerSize = readInt32(dib, 0);
		if (headerSize < 40 || dib.length < headerSize) {
			return null;
		}

		int width = readInt32(dib, 4);
		int heightValue = readInt32(dib, 8);
		int planes = readUInt16(dib, 12);
		int bitCount = readUInt16(dib, 14);
		int compression = readInt32(dib, 16);
		if (width <= 0 || heightValue == 0 || planes != 1 || compression != 0) {
			return null;
		}

		int height = Math.abs(heightValue);
		int colorCount = getDibColorCount(dib, headerSize, bitCount);
		int bitsOffset = headerSize + colorCount * 4;
		if (dib.length < bitsOffset) {
			return null;
		}

		int stride = ((width * bitCount + 31) / 32) * 4;
		if (colorCount > 0 && dib.length < bitsOffset + stride * height
				&& dib.length >= headerSize + stride * height) {
			colorCount = 0;
			bitsOffset = headerSize;
		}
		if (stride <= 0 || dib.length < bitsOffset + stride * height) {
			return null;
		}

		int[] colors = null;
		if (colorCount > 0) {
			colors = new int[colorCount];
			for (int i = 0; i < colorCount; i++) {
				int pos = headerSize + i * 4;
				int blue = dib[pos] & 0xFF;
				int green = dib[pos + 1] & 0xFF;
				int red = dib[pos + 2] & 0xFF;
				colors[i] = applyAlpha(red, green, blue, 0xFF, transparentColor, preserveAlpha);
			}
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		boolean bottomUp = heightValue > 0;
		for (int y = 0; y < height; y++) {
			int dibY = bottomUp ? height - 1 - y : y;
			if (reverse) {
				dibY = height - 1 - dibY;
			}
			int row = bitsOffset + dibY * stride;
			for (int x = 0; x < width; x++) {
				image.setRGB(x, y, readDibPixel(dib, row, x, bitCount, colors, transparentColor, preserveAlpha));
			}
		}
		return image;
	}

	private int readDibPixel(byte[] dib, int row, int x, int bitCount, int[] colors,
			Integer transparentColor, boolean preserveAlpha) {
		if (bitCount == 1) {
			int index = (dib[row + x / 8] >>> (7 - (x % 8))) & 0x01;
			if (colors != null && index < colors.length) {
				return colors[index];
			}
			return index == 0 ? 0xFF000000 : 0xFFFFFFFF;
		} else if (bitCount == 4) {
			int value = dib[row + x / 2] & 0xFF;
			int index = (x % 2 == 0) ? (value >>> 4) : (value & 0x0F);
			return colors != null && index < colors.length ? colors[index] : 0xFF000000;
		} else if (bitCount == 8) {
			int index = dib[row + x] & 0xFF;
			return colors != null && index < colors.length ? colors[index] : 0xFF000000;
		} else if (bitCount == 24) {
			int pos = row + x * 3;
			int blue = dib[pos] & 0xFF;
			int green = dib[pos + 1] & 0xFF;
			int red = dib[pos + 2] & 0xFF;
			return applyAlpha(red, green, blue, 0xFF, transparentColor, preserveAlpha);
		} else if (bitCount == 32) {
			int pos = row + x * 4;
			int blue = dib[pos] & 0xFF;
			int green = dib[pos + 1] & 0xFF;
			int red = dib[pos + 2] & 0xFF;
			int alpha = preserveAlpha ? (dib[pos + 3] & 0xFF) : 0xFF;
			return applyAlpha(red, green, blue, alpha, transparentColor, preserveAlpha);
		}
		return 0x00000000;
	}

	private int applyAlpha(int red, int green, int blue, int alpha, Integer transparentColor, boolean preserveAlpha) {
		int rgb = (red << 16) | (green << 8) | blue;
		if (transparentColor != null) {
			int transparentRgb = ((transparentColor.intValue() & 0x000000FF) << 16)
					| (transparentColor.intValue() & 0x0000FF00)
					| ((transparentColor.intValue() & 0x00FF0000) >> 16);
			if (rgb == transparentRgb) {
				return rgb;
			}
		}
		return ((preserveAlpha ? alpha : 0xFF) << 24) | rgb;
	}

	private byte[] applyPaletteToDib(byte[] dib, int usage) {
		if (usage != Gdi.DIB_PAL_COLORS || selectedPalette == null || dib == null || dib.length < 40) {
			return dib;
		}

		int headerSize = readInt32(dib, 0);
		if (headerSize < 40 || dib.length < headerSize) {
			return dib;
		}

		int bitCount = readUInt16(dib, 14);
		int colorCount = getDibColorCount(dib, headerSize, bitCount);
		int indexTableSize = colorCount * 2;
		if (colorCount == 0 || dib.length < headerSize + indexTableSize) {
			return dib;
		}

		int[] entries = selectedPalette.getEntries();
		byte[] rgbDib = new byte[dib.length + colorCount * 2];
		System.arraycopy(dib, 0, rgbDib, 0, headerSize);
		for (int i = 0; i < colorCount; i++) {
			int paletteIndex = readUInt16(dib, headerSize + i * 2);
			int entry = (paletteIndex >= 0 && paletteIndex < entries.length) ? entries[paletteIndex] : 0;
			int red = entry & 0xFF;
			int green = (entry >>> 8) & 0xFF;
			int blue = (entry >>> 16) & 0xFF;
			int offset = headerSize + i * 4;
			rgbDib[offset] = (byte)blue;
			rgbDib[offset + 1] = (byte)green;
			rgbDib[offset + 2] = (byte)red;
			rgbDib[offset + 3] = 0;
		}
		System.arraycopy(dib, headerSize + indexTableSize, rgbDib, headerSize + colorCount * 4,
				dib.length - headerSize - indexTableSize);
		return rgbDib;
	}

	private int getDibColorCount(byte[] dib, int headerSize, int bitCount) {
		long clrUsed = readUInt32(dib, 32);
		if (clrUsed > 0 && clrUsed <= Integer.MAX_VALUE) {
			return (int)clrUsed;
		}

		switch (bitCount) {
		case 1:
			return 2;
		case 4:
			return 16;
		case 8:
			return 256;
		default:
			return 0;
		}
	}

	private static int readInt32(byte[] data, int offset) {
		return (data[offset] & 0xFF)
				| ((data[offset + 1] & 0xFF) << 8)
				| ((data[offset + 2] & 0xFF) << 16)
				| (data[offset + 3] << 24);
	}

	private static int readInt16(byte[] data, int offset) {
		return (short)((data[offset] & 0xFF) | (data[offset + 1] << 8));
	}

	private static int readUInt16(byte[] data, int offset) {
		return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
	}

	private static long readUInt32(byte[] data, int offset) {
		return readInt32(data, offset) & 0xFFFFFFFFL;
	}

	private static float readFloat(byte[] data, int offset) {
		return Float.intBitsToFloat(readInt32(data, offset));
	}

	private static String formatDouble(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return "0";
		}
		if (Math.rint(value) == value) {
			return Long.toString((long)value);
		}
		return Double.toString(value);
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

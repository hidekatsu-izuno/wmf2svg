/*
 * Copyright 2007-2008 Hidekatsu Izuno
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
package net.arnx.wmf2svg.gdi.awt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiColorSpace;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiObject;
import net.arnx.wmf2svg.gdi.GdiPalette;
import net.arnx.wmf2svg.gdi.GdiPatternBrush;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.GdiRegion;
import net.arnx.wmf2svg.gdi.GdiUtils;
import net.arnx.wmf2svg.gdi.GradientRect;
import net.arnx.wmf2svg.gdi.GradientTriangle;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Size;
import net.arnx.wmf2svg.gdi.Trivertex;
import net.arnx.wmf2svg.gdi.emf.EmfParseException;
import net.arnx.wmf2svg.gdi.emf.EmfParser;

public class AwtGdi implements Gdi {
	private static final int DEFAULT_CANVAS_WIDTH = 330;
	private static final int DEFAULT_CANVAS_HEIGHT = 460;
	private static final int TARGET_DPI = 144;
	private static final int MAX_CANVAS_SIZE = 32767;
	private static final long MAX_CANVAS_PIXELS = 64_000_000L;
	private static final int EMR_HEADER_RECORD_TYPE = 1;
	private static final int EMF_HEADER_MIN_SIZE = 88;
	private static final long EMF_SIGNATURE = 0x464D4520L;

	private AwtDc dc;
	private LinkedList<AwtSavedDc> saveDC = new LinkedList<AwtSavedDc>();
	private BufferedImage image;
	private Graphics2D graphics;
	private boolean placeableHeader;
	private boolean growCanvas = true;
	private int initialDpi = 1440;
	private int canvasWidth = DEFAULT_CANVAS_WIDTH;
	private int canvasHeight = DEFAULT_CANVAS_HEIGHT;
	private boolean opaqueBackground;
	private AwtBrush defaultBrush;
	private AwtPen defaultPen;
	private AwtFont defaultFont;
	private GdiPalette selectedPalette;
	private GdiColorSpace selectedColorSpace;
	private Path2D.Double currentPath;
	private ByteArrayOutputStream emfBuffer;
	private int emfTotalSize;
	private ArrayList<PendingEmf> pendingEmfList = new ArrayList<PendingEmf>();
	private boolean replayingPendingEmf;

	public AwtGdi() {
	}

	public AwtGdi(Graphics2D graphics, int width, int height) {
		this.graphics = graphics;
		this.canvasWidth = Math.max(width, 1);
		this.canvasHeight = Math.max(height, 1);
		initDc();
		configureGraphics(graphics);
	}

	public BufferedImage getImage() {
		ensureGraphics();
		return image;
	}

	public void setOpaqueBackground(boolean opaqueBackground) {
		this.opaqueBackground = opaqueBackground;
	}

	public void write(OutputStream out, String format) throws IOException {
		ensureGraphics();
		String imageFormat = normalizeFormat(format);
		BufferedImage output = image;
		if ("jpeg".equals(imageFormat)) {
			output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = output.createGraphics();
			try {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, output.getWidth(), output.getHeight());
				g.drawImage(image, 0, 0, null);
			} finally {
				g.dispose();
			}
		}
		ImageIO.write(output, imageFormat, out);
		out.flush();
	}

	private String normalizeFormat(String format) {
		if (format == null) {
			return "png";
		}
		format = format.toLowerCase();
		return "jpg".equals(format) ? "jpeg" : format;
	}

	public void placeableHeader(int vsx, int vsy, int vex, int vey, int dpi) {
		placeableHeader = true;
		growCanvas = false;
		initialDpi = dpi;
		canvasWidth = unitsToPixels(vsx, vex, dpi);
		canvasHeight = unitsToPixels(vsy, vey, dpi);
		initDc();
		dc.setWindowExtEx(Math.abs(vex - vsx), Math.abs(vey - vsy), null);
		dc.setViewportExtEx(Math.abs(vex - vsx), Math.abs(vey - vsy), null);
		dc.setDpi(dpi);
	}

	public void header() {
		initDc();
	}

	private int unitsToPixels(int start, int end, int inch) {
		long denominator = Math.max(inch, 1);
		long numerator = (long) Math.max(Math.abs(end - start), 1) * TARGET_DPI;
		long pixels = (2 * numerator + denominator - 1) / (2 * denominator);
		if (start == 0 && (2 * numerator) % denominator == 0 && (((2 * numerator) / denominator) & 1) == 1) {
			pixels++;
		}
		if (pixels < 1) {
			return 1;
		}
		if (pixels > MAX_CANVAS_SIZE) {
			return MAX_CANVAS_SIZE;
		}
		return (int) pixels;
	}

	private void ensureGraphics() {
		initDc();
		if (graphics == null) {
			createGraphics();
		}
	}

	private void initDc() {
		if (dc == null) {
			dc = new AwtDc();
			dc.setDpi(initialDpi);
			defaultBrush = new AwtBrush(GdiBrush.BS_SOLID, 0x00FFFFFF, 0);
			defaultPen = new AwtPen(GdiPen.PS_SOLID, 1, 0x00000000);
			defaultFont = null;
			dc.setBrush(defaultBrush);
			dc.setPen(defaultPen);
			dc.setFont(defaultFont);
		}
	}

	private void createGraphics() {
		if (graphics == null) {
			image = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
			graphics = image.createGraphics();
		}
		configureGraphics(graphics);
		if (opaqueBackground) {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, canvasWidth, canvasHeight);
		}
	}

	private void ensureCanvasContains(Shape shape) {
		if (!growCanvas || shape == null || image == null) {
			return;
		}
		Rectangle2D bounds = shape.getBounds2D();
		Shape clip = graphics.getClip();
		if (clip != null) {
			Rectangle2D clippedBounds = bounds.createIntersection(clip.getBounds2D());
			if (clippedBounds.isEmpty()) {
				return;
			}
			bounds = clippedBounds;
		}
		int requiredWidth = (int) Math.ceil(Math.max(canvasWidth, bounds.getMaxX()));
		int requiredHeight = (int) Math.ceil(Math.max(canvasHeight, bounds.getMaxY()));
		if ((requiredWidth > canvasWidth || requiredHeight > canvasHeight)
				&& canAllocateCanvas(requiredWidth, requiredHeight)) {
			resizeCanvas(requiredWidth, requiredHeight);
		}
	}

	private void ensureCanvasContains(int x1, int y1, int x2, int y2) {
		if (!growCanvas) {
			return;
		}
		int requiredWidth = Math.max(canvasWidth, Math.max(Math.max(x1, x2), 0));
		int requiredHeight = Math.max(canvasHeight, Math.max(Math.max(y1, y2), 0));
		if ((requiredWidth > canvasWidth || requiredHeight > canvasHeight)
				&& canAllocateCanvas(requiredWidth, requiredHeight)) {
			resizeCanvas(requiredWidth, requiredHeight);
		}
	}

	private void resizeCanvas(int width, int height) {
		width = Math.max(1, Math.min(width, MAX_CANVAS_SIZE));
		height = Math.max(1, Math.min(height, MAX_CANVAS_SIZE));
		if ((width == canvasWidth && height == canvasHeight) || !canAllocateCanvas(width, height)) {
			return;
		}

		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = resized.createGraphics();
		try {
			configureGraphics(g);
			if (opaqueBackground) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, width, height);
			}
			if (image != null) {
				g.drawImage(image, 0, 0, null);
			}
		} finally {
			g.dispose();
		}
		if (graphics != null) {
			graphics.dispose();
		}
		image = resized;
		graphics = image.createGraphics();
		configureGraphics(graphics);
		canvasWidth = width;
		canvasHeight = height;
	}

	private boolean canAllocateCanvas(int width, int height) {
		return (long) width * height <= MAX_CANVAS_PIXELS;
	}

	private void configureGraphics(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	}

	public void animatePalette(GdiPalette palette, int startIndex, int[] entries) {
		setPaletteEntries(palette, startIndex, entries);
	}

	public void alphaBlend(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh,
			int blendFunction) {
		int sourceConstantAlpha = (blendFunction >>> 16) & 0xFF;
		boolean sourceAlpha = ((blendFunction >>> 24) & 0x01) != 0;
		drawAlphaBlend(image, dx, dy, dw, dh, sx, sy, sw, sh, sourceConstantAlpha, sourceAlpha);
	}

	public void angleArc(int x, int y, int radius, float startAngle, float sweepAngle) {
		Point start = circlePoint(x, y, radius, startAngle);
		Point end = circlePoint(x, y, radius, startAngle + sweepAngle);
		lineTo(start.x, start.y);
		strokeShape(
				createArc(x - radius, y - radius, x + radius, y + radius, start.x, start.y, end.x, end.y, Arc2D.OPEN),
				dc.getPen());
		dc.moveToEx(end.x, end.y, null);
	}

	public void arc(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		Arc2D arc = createArc(sxr, syr, exr, eyr, sxa, sya, exa, eya, Arc2D.OPEN);
		if (arc == null) {
			return;
		}
		if (currentPath != null) {
			currentPath.append(arc, true);
			return;
		}
		strokeShape(arc, dc.getPen());
	}

	public void arcTo(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		Point start = getArcPoint(sxr, syr, exr, eyr, sxa, sya);
		Point end = getArcPoint(sxr, syr, exr, eyr, exa, eya);
		lineTo(start.x, start.y);
		arc(sxr, syr, exr, eyr, sxa, sya, exa, eya);
		dc.moveToEx(end.x, end.y, null);
	}

	public void abortPath() {
		currentPath = null;
	}

	public void beginPath() {
		currentPath = new Path2D.Double(
				dc.getPolyFillMode() == Gdi.WINDING ? Path2D.WIND_NON_ZERO : Path2D.WIND_EVEN_ODD);
	}

	public void bitBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop) {
		if (image == null || image.length == 0) {
			patBlt(dx, dy, dw, dh, rop);
			return;
		}
		drawBitmap(image, dx, dy, dw, dh, sx, sy, dw, dh, Gdi.DIB_RGB_COLORS, Long.valueOf(rop), false);
	}

	public void maskBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, byte[] mask, int mx, int my,
			long rop) {
		if (mask != null && rop == Gdi.SRCCOPY) {
			drawMaskedSrcCopy(image, dx, dy, dw, dh, sx, sy, dw, dh, mask, mx, my);
			return;
		}
		bitBlt(image, dx, dy, dw, dh, sx, sy, rop);
	}

	public void plgBlt(byte[] image, Point[] points, int sx, int sy, int sw, int sh, byte[] mask, int mx, int my) {
		if (points == null || points.length < 3) {
			return;
		}
		drawPlgBlt(image, points, sx, sy, sw, sh, mask, mx, my);
	}

	public void chord(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		Arc2D chord = createArc(sxr, syr, exr, eyr, sxa, sya, exa, eya, Arc2D.CHORD);
		if (chord != null) {
			drawShape(chord);
		}
	}

	public void closeFigure() {
		if (currentPath != null) {
			currentPath.closePath();
		}
	}

	public void colorCorrectPalette(GdiPalette palette, int startIndex, int entries) {
		// Palette color correction has no direct Graphics2D state equivalent.
	}

	public GdiBrush createBrushIndirect(int style, int color, int hatch) {
		return new AwtBrush(style, color, hatch);
	}

	public GdiColorSpace createColorSpace(byte[] logColorSpace) {
		return new AwtColorSpace(logColorSpace);
	}

	public GdiColorSpace createColorSpaceW(byte[] logColorSpace) {
		return createColorSpace(logColorSpace);
	}

	public GdiFont createFontIndirect(int height, int width, int escapement, int orientation, int weight,
			boolean italic, boolean underline, boolean strikeout, int charset, int outPrecision, int clipPrecision,
			int quality, int pitchAndFamily, byte[] faceName) {
		return new AwtFont(height, width, escapement, orientation, weight, italic, underline, strikeout, charset,
				outPrecision, clipPrecision, quality, pitchAndFamily, faceName);
	}

	public GdiPalette createPalette(int version, int[] palEntry) {
		return new AwtPalette(palEntry);
	}

	public GdiPatternBrush createPatternBrush(byte[] image) {
		return new AwtPatternBrush(image, Gdi.DIB_RGB_COLORS);
	}

	public GdiPen createPenIndirect(int style, int width, int color) {
		return new AwtPen(style, width, color);
	}

	public GdiRegion createRectRgn(int left, int top, int right, int bottom) {
		return new AwtRegion(toRectangle(left, top, right - left, bottom - top));
	}

	public void deleteObject(GdiObject obj) {
		// AWT GDI objects are ordinary Java objects; deletion has no rendering side
		// effect.
	}

	public boolean deleteColorSpace(GdiColorSpace colorSpace) {
		if (selectedColorSpace == colorSpace) {
			selectedColorSpace = null;
		}
		return true;
	}

	public void dibBitBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop) {
		if (image == null || image.length == 0) {
			patBlt(dx, dy, dw, dh, rop);
			return;
		}
		drawBitmap(image, dx, dy, dw, dh, sx, sy, dw, dh, Gdi.DIB_RGB_COLORS, Long.valueOf(rop), false);
	}

	public GdiPatternBrush dibCreatePatternBrush(byte[] image, int usage) {
		return new AwtPatternBrush(image, usage);
	}

	public void dibStretchBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, long rop) {
		stretchDIBits(dx, dy, dw, dh, sx, sy, sw, sh, image, Gdi.DIB_RGB_COLORS, rop);
	}

	public void ellipse(int sx, int sy, int ex, int ey) {
		Shape ellipse = new Ellipse2D.Double(Math.min(tx(sx), tx(ex)), Math.min(ty(sy), ty(ey)), Math.abs(rx(ex - sx)),
				Math.abs(ry(ey - sy)));
		if (currentPath != null) {
			currentPath.append(ellipse, false);
			return;
		}
		drawShape(ellipse);
	}

	public void endPath() {
		// The accumulated path remains available for
		// fillPath/strokePath/selectClipPath.
	}

	public void escape(byte[] data) {
		if (data == null || data.length == 0) {
			return;
		}
		if (EmfParser.parseEscape(data, this)) {
			return;
		}

		EscapeRecord record = readEscapeRecord(data);
		if (record != null && record.escapeFunction != 0 && isStandaloneEmf(record.payload)) {
			addPendingEmf(record.payload);
		} else if (isStandaloneEmf(data)) {
			addPendingEmf(data);
		}
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
			byte[] bytesToParse = new byte[emfTotalSize];
			System.arraycopy(emfBuffer.toByteArray(), 0, bytesToParse, 0, emfTotalSize);
			addPendingEmf(bytesToParse);
			emfBuffer = null;
			emfTotalSize = 0;
		}
	}

	public int excludeClipRect(int left, int top, int right, int bottom) {
		ensureGraphics();
		Shape oldClip = graphics.getClip();
		Area area = oldClip != null
				? new Area(oldClip)
				: new Area(new Rectangle2D.Double(0, 0, canvasWidth, canvasHeight));
		area.subtract(new Area(toRectangle(left, top, right - left, bottom - top)));
		graphics.setClip(area);
		return GdiRegion.SIMPLEREGION;
	}

	public void extFloodFill(int x, int y, int color, int type) {
		floodFill(x, y, color);
	}

	public GdiRegion extCreateRegion(float[] xform, int count, byte[] rgnData) {
		Area area = createRegionArea(rgnData, count);
		return area != null ? new AwtRegion(area) : null;
	}

	public int extSelectClipRgn(GdiRegion rgn, int mode) {
		ensureGraphics();
		applyClip(rgn instanceof AwtRegion ? ((AwtRegion) rgn).shape : null, mode);
		return GdiRegion.SIMPLEREGION;
	}

	public void extTextOut(int x, int y, int options, int[] rect, byte[] text, int[] lpdx) {
		ensureGraphics();
		if (rect != null && (options & Gdi.ETO_OPAQUE) != 0) {
			Paint old = graphics.getPaint();
			graphics.setPaint(toColor(dc.getBkColor()));
			graphics.fill(toRectangle(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]));
			graphics.setPaint(old);
		}
		Shape oldClip = null;
		if (rect != null && (options & Gdi.ETO_CLIPPED) != 0) {
			oldClip = graphics.getClip();
			graphics.clip(toRectangle(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]));
		}
		drawText(x, y, GdiUtils.convertString(text, dc.getFont() != null ? dc.getFont().getCharset() : 0));
		if (oldClip != null) {
			graphics.setClip(oldClip);
		}
	}

	public void fillRgn(GdiRegion rgn, GdiBrush brush) {
		if (rgn instanceof AwtRegion) {
			fillShape(((AwtRegion) rgn).shape, brush);
		}
	}

	public void flattenPath() {
		if (currentPath != null) {
			Path2D.Double flattened = new Path2D.Double();
			flattened.append(currentPath.getPathIterator(null, 0.25), false);
			currentPath = flattened;
		}
	}

	public void widenPath() {
		if (currentPath != null) {
			Shape widened = toStroke(dc.getPen()).createStrokedShape(currentPath);
			currentPath = new Path2D.Double(widened);
		}
	}

	public void floodFill(int x, int y, int color) {
		ensureGraphics();
		Paint old = graphics.getPaint();
		graphics.setPaint(toPaint(dc.getBrush()));
		graphics.fill(toRectangle(x, y, 1, 1));
		graphics.setPaint(old);
	}

	public void gradientFill(Trivertex[] vertex, GradientRect[] mesh, int mode) {
		if (vertex == null || mesh == null) {
			return;
		}
		for (int i = 0; i < mesh.length; i++) {
			GradientRect rect = mesh[i];
			if (!isValidVertex(vertex, rect.upperLeft) || !isValidVertex(vertex, rect.lowerRight)) {
				continue;
			}
			fillGradientRectangle(vertex[rect.upperLeft], vertex[rect.lowerRight], mode);
		}
	}

	public void gradientFill(Trivertex[] vertex, GradientTriangle[] mesh, int mode) {
		if (vertex == null || mesh == null) {
			return;
		}
		for (int i = 0; i < mesh.length; i++) {
			GradientTriangle triangle = mesh[i];
			if (!isValidVertex(vertex, triangle.vertex1) || !isValidVertex(vertex, triangle.vertex2)
					|| !isValidVertex(vertex, triangle.vertex3)) {
				continue;
			}
			fillGradientTriangle(vertex[triangle.vertex1], vertex[triangle.vertex2], vertex[triangle.vertex3]);
		}
	}

	public void frameRgn(GdiRegion rgn, GdiBrush brush, int w, int h) {
		if (rgn instanceof AwtRegion) {
			fillShape(createFrameArea(((AwtRegion) rgn).shape, w, h), brush);
		}
	}

	public void intersectClipRect(int left, int top, int right, int bottom) {
		ensureGraphics();
		graphics.clip(toRectangle(left, top, right - left, bottom - top));
	}

	public void invertRgn(GdiRegion rgn) {
		if (rgn instanceof AwtRegion) {
			xorFill(((AwtRegion) rgn).shape, Color.WHITE);
		}
	}

	public void lineTo(int ex, int ey) {
		if (currentPath != null) {
			currentPath.lineTo(tx(ex), ty(ey));
			dc.moveToEx(ex, ey, null);
			return;
		}
		Shape line = new java.awt.geom.Line2D.Double(tx(dc.getCurrentX()), ty(dc.getCurrentY()), tx(ex), ty(ey));
		strokeShape(line, dc.getPen());
		dc.moveToEx(ex, ey, null);
	}

	public void moveToEx(int x, int y, Point old) {
		if (currentPath != null) {
			currentPath.moveTo(tx(x), ty(y));
		}
		dc.moveToEx(x, y, old);
	}

	public void offsetClipRgn(int x, int y) {
		ensureGraphics();
		Shape clip = graphics.getClip();
		if (clip != null) {
			graphics.setClip(AffineTransform.getTranslateInstance(rx(x), ry(y)).createTransformedShape(clip));
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
		ensureGraphics();
		Rectangle2D rect = toRectangle(x, y, width, height);
		if (rop == Gdi.BLACKNESS) {
			graphics.setPaint(Color.BLACK);
			graphics.fill(rect);
		} else if (rop == Gdi.WHITENESS) {
			graphics.setPaint(Color.WHITE);
			graphics.fill(rect);
		} else if (rop == Gdi.PATCOPY || rop == Gdi.SRCCOPY) {
			fillShape(rect, dc.getBrush());
		} else if (rop == Gdi.DSTINVERT) {
			xorFill(rect, Color.WHITE);
		} else if (rop == Gdi.PATINVERT) {
			xorFill(rect, toColor(dc.getBrush() != null ? dc.getBrush().getColor() : 0x00FFFFFF));
		}
	}

	public void pie(int sxr, int syr, int exr, int eyr, int sx, int sy, int ex, int ey) {
		Arc2D pie = createArc(sxr, syr, exr, eyr, sx, sy, ex, ey, Arc2D.PIE);
		if (pie != null) {
			if (currentPath != null) {
				currentPath.append(pie, false);
				return;
			}
			drawShape(pie);
		}
	}

	public void polyBezier(Point[] points) {
		if (currentPath != null) {
			appendBezier(points, false);
			return;
		}
		Path2D.Double path = createBezierPath(points, false);
		if (path != null) {
			strokeShape(path, dc.getPen());
		}
	}

	public void polyBezierTo(Point[] points) {
		if (currentPath != null) {
			appendBezier(points, true);
			if (points != null && points.length > 0) {
				dc.moveToEx(points[points.length - 1].x, points[points.length - 1].y, null);
			}
			return;
		}
		Path2D.Double path = createBezierPath(points, true);
		if (path != null) {
			strokeShape(path, dc.getPen());
			if (points != null && points.length > 0) {
				dc.moveToEx(points[points.length - 1].x, points[points.length - 1].y, null);
			}
		}
	}

	public void polygon(Point[] points) {
		if (currentPath != null) {
			appendPoints(points, true);
			return;
		}
		Polygon polygon = toPolygon(points);
		if (polygon != null) {
			drawShape(polygon);
		}
	}

	public void polyline(Point[] points) {
		if (currentPath != null) {
			appendPoints(points, false);
			if (points != null && points.length > 0) {
				dc.moveToEx(points[points.length - 1].x, points[points.length - 1].y, null);
			}
			return;
		}
		Polygon polygon = toPolygon(points);
		if (polygon != null) {
			strokeShape(polygon, dc.getPen());
			if (points.length > 0) {
				dc.moveToEx(points[points.length - 1].x, points[points.length - 1].y, null);
			}
		}
	}

	public void polyPolygon(Point[][] points) {
		if (points != null) {
			for (int i = 0; i < points.length; i++) {
				polygon(points[i]);
			}
		}
	}

	public void fillPath() {
		if (currentPath != null) {
			fillShape(currentPath, dc.getBrush());
			currentPath = null;
		}
	}

	public void strokePath() {
		if (currentPath != null) {
			strokeShape(currentPath, dc.getPen());
			currentPath = null;
		}
	}

	public void strokeAndFillPath() {
		if (currentPath != null) {
			fillShape(currentPath, dc.getBrush());
			strokeShape(currentPath, dc.getPen());
			currentPath = null;
		}
	}

	public void realizePalette() {
		// Palette entries are resolved when indexed DIB data is decoded.
	}

	public void restoreDC(int savedDC) {
		if (saveDC.isEmpty()) {
			return;
		}
		if (savedDC == 0) {
			while (!saveDC.isEmpty()) {
				restoreLastDC();
			}
			return;
		}
		int count = savedDC < -1 ? Math.min(-savedDC, saveDC.size()) : 1;
		for (int i = 0; i < count; i++) {
			restoreLastDC();
		}
	}

	private void restoreLastDC() {
		AwtSavedDc saved = saveDC.removeLast();
		dc = saved.dc;
		if (graphics != null) {
			graphics.setClip(saved.clip);
		}
	}

	public void rectangle(int sx, int sy, int ex, int ey) {
		Shape rect = toRectangle(sx, sy, ex - sx, ey - sy);
		if (currentPath != null) {
			currentPath.append(rect, false);
			return;
		}
		drawShape(rect);
	}

	public void resizePalette(GdiPalette palette, int entries) {
		if (palette instanceof AwtPalette) {
			((AwtPalette) palette).resize(entries);
		}
	}

	public void roundRect(int sx, int sy, int ex, int ey, int rw, int rh) {
		Shape rect = new java.awt.geom.RoundRectangle2D.Double(Math.min(tx(sx), tx(ex)), Math.min(ty(sy), ty(ey)),
				Math.abs(rx(ex - sx)), Math.abs(ry(ey - sy)), Math.abs(rx(rw)), Math.abs(ry(rh)));
		if (currentPath != null) {
			currentPath.append(rect, false);
			return;
		}
		drawShape(rect);
	}

	public void seveDC() {
		saveDC.add(new AwtSavedDc((AwtDc) dc.clone(), graphics != null ? graphics.getClip() : null));
	}

	public void scaleViewportExtEx(int x, int xd, int y, int yd, Size old) {
		dc.scaleViewportExtEx(x, xd, y, yd, old);
	}

	public void scaleWindowExtEx(int x, int xd, int y, int yd, Size old) {
		dc.scaleWindowExtEx(x, xd, y, yd, old);
	}

	public void selectClipRgn(GdiRegion rgn) {
		ensureGraphics();
		if (rgn instanceof AwtRegion) {
			applyClip(((AwtRegion) rgn).shape, GdiRegion.RGN_COPY);
		} else if (rgn == null) {
			graphics.setClip(null);
		}
	}

	public void selectClipPath(int mode) {
		if (currentPath != null) {
			ensureGraphics();
			applyClip(currentPath, mode);
			currentPath = null;
		}
	}

	public GdiColorSpace setColorSpace(GdiColorSpace colorSpace) {
		GdiColorSpace old = selectedColorSpace;
		selectedColorSpace = colorSpace;
		return old;
	}

	public void selectObject(GdiObject obj) {
		if (obj instanceof AwtBrush) {
			dc.setBrush((AwtBrush) obj);
		} else if (obj instanceof AwtPen) {
			dc.setPen((AwtPen) obj);
		} else if (obj instanceof AwtFont) {
			dc.setFont((AwtFont) obj);
		} else if (obj instanceof AwtPatternBrush) {
			dc.setBrush((AwtPatternBrush) obj);
		} else if (obj instanceof AwtRegion) {
			selectClipRgn((AwtRegion) obj);
		}
	}

	public void selectPalette(GdiPalette palette, boolean mode) {
		selectedPalette = palette;
	}

	public void setBkColor(int color) {
		dc.setBkColor(color);
	}

	public void setBkMode(int mode) {
		dc.setBkMode(mode);
	}

	public void setColorAdjustment(byte[] colorAdjustment) {
		// Color adjustment records are currently not mapped to a Graphics2D color
		// transform.
	}

	public void setArcDirection(int direction) {
		dc.setArcDirection(direction);
	}

	public void setBrushOrgEx(int x, int y, Point old) {
		dc.setBrushOrgEx(x, y, old);
	}

	public void setDIBitsToDevice(int dx, int dy, int dw, int dh, int sx, int sy, int startscan, int scanlines,
			byte[] image, int colorUse) {
		drawDIBitsToDevice(image, dx, dy, dw, dh, sx, sy, startscan, scanlines, colorUse);
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
		return mode;
	}

	public boolean setICMProfile(byte[] profileName) {
		return true;
	}

	public boolean colorMatchToTarget(int action, int flags, byte[] targetProfile) {
		return true;
	}

	public int setMetaRgn() {
		return GdiRegion.SIMPLEREGION;
	}

	public void setMiterLimit(float limit) {
		dc.setMiterLimit(limit);
	}

	public void setPaletteEntries(GdiPalette palette, int startIndex, int[] entries) {
		if (palette instanceof AwtPalette) {
			((AwtPalette) palette).setEntries(startIndex, entries);
		}
	}

	public void setPixel(int x, int y, int color) {
		ensureGraphics();
		graphics.setPaint(toColor(color));
		graphics.fill(toRectangle(x, y, 1, 1));
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
		dc.setTextJustification(breakExtra, breakCount);
	}

	public void setViewportExtEx(int x, int y, Size old) {
		dc.setViewportExtEx(x, y, old);
		if (!replayingPendingEmf && !placeableHeader && x != 0 && y != 0 && image == null) {
			growCanvas = false;
			canvasWidth = Math.min(Math.max(Math.abs(x), 1), MAX_CANVAS_SIZE);
			canvasHeight = Math.min(Math.max(Math.abs(y), 1), MAX_CANVAS_SIZE);
		}
	}

	public void setViewportOrgEx(int x, int y, Point old) {
		dc.setViewportOrgEx(x, y, old);
	}

	public void setWindowExtEx(int width, int height, Size old) {
		dc.setWindowExtEx(width, height, old);
		if (!replayingPendingEmf && !placeableHeader && width != 0 && height != 0) {
			growCanvas = dc.getWindowX() == 0 && dc.getWindowY() == 0;
			if (image == null) {
				canvasWidth = windowExtentToCanvasSize(width, dc.getWindowX());
				canvasHeight = windowExtentToCanvasSize(height, dc.getWindowY());
			}
		}
	}

	private int windowExtentToCanvasSize(int extent, int origin) {
		int size = Math.max(Math.abs(extent), 1);
		if (origin == 0 && extent > 0 && (size & 1) == 1) {
			size++;
		}
		return Math.min(size, MAX_CANVAS_SIZE);
	}

	public void setWindowOrgEx(int x, int y, Point old) {
		dc.setWindowOrgEx(x, y, old);
	}

	public void stretchBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, long rop) {
		drawBitmap(image, dx, dy, dw, dh, sx, sy, sw, sh, Gdi.DIB_RGB_COLORS, Long.valueOf(rop), false);
	}

	public void stretchDIBits(int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, byte[] image, int usage,
			long rop) {
		drawBitmap(image, dx, dy, dw, dh, sx, sy, sw, sh, usage, Long.valueOf(rop), false);
	}

	public void textOut(int x, int y, byte[] text) {
		drawText(x, y, GdiUtils.convertString(text, dc.getFont() != null ? dc.getFont().getCharset() : 0));
	}

	public void transparentBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh,
			int transparentColor) {
		drawBitmap(image, dx, dy, dw, dh, sx, sy, sw, sh, Gdi.DIB_RGB_COLORS, Integer.valueOf(transparentColor), false);
	}

	public void footer() {
		flushPendingEmf();
		ensureGraphics();
	}

	private void flushPendingEmf() {
		if (pendingEmfList.isEmpty()) {
			return;
		}

		ArrayList<PendingEmf> list = pendingEmfList;
		pendingEmfList = new ArrayList<PendingEmf>();
		AwtDc savedDc = dc;
		for (PendingEmf pendingEmf : list) {
			try {
				dc = (AwtDc) pendingEmf.dc.clone();
				replayingPendingEmf = true;
				applyPendingEmfFrame(pendingEmf.header);
				new EmfParser(false).parse(new ByteArrayInputStream(pendingEmf.data), this);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} catch (EmfParseException e) {
				throw new IllegalStateException(e);
			} finally {
				replayingPendingEmf = false;
				dc = savedDc;
			}
		}
	}

	private void addPendingEmf(byte[] data) {
		if (data == null || data.length == 0) {
			return;
		}
		initDc();
		byte[] copy = new byte[data.length];
		System.arraycopy(data, 0, copy, 0, data.length);
		pendingEmfList.add(new PendingEmf(copy, (AwtDc) dc.clone(), readEmfHeader(copy)));
	}

	private void applyPendingEmfFrame(EmfHeader header) {
		if (header == null || header.boundsWidth <= 0 || header.boundsHeight <= 0 || header.frameWidth <= 0
				|| header.frameHeight <= 0) {
			return;
		}
		dc.setMapMode(Gdi.MM_TEXT);
		dc.setWindowOrgEx(header.boundsLeft, header.boundsTop, null);
		dc.setWindowExtEx(header.boundsWidth, header.boundsHeight, null);
		dc.setViewportOrgEx(0, 0, null);
		dc.setViewportExtEx(frameToPixels(header.frameWidth), frameToPixels(header.frameHeight), null);
	}

	private boolean isStandaloneEmf(byte[] data) {
		if (data == null || data.length < EMF_HEADER_MIN_SIZE || readInt32(data, 0) != EMR_HEADER_RECORD_TYPE
				|| readUInt32(data, 40) != EMF_SIGNATURE) {
			return false;
		}

		long totalSize = readUInt32(data, 48);
		return totalSize >= EMF_HEADER_MIN_SIZE && totalSize <= data.length;
	}

	private EmfHeader readEmfHeader(byte[] data) {
		if (!isStandaloneEmf(data)) {
			return null;
		}
		return new EmfHeader(readInt32(data, 8), readInt32(data, 12), readInt32(data, 16), readInt32(data, 20),
				readInt32(data, 24), readInt32(data, 28), readInt32(data, 32), readInt32(data, 36));
	}

	private int frameToPixels(int frameSize) {
		return Math.max(1, (int) Math.round(Math.abs(frameSize) * TARGET_DPI / 5080.0) + 1);
	}

	private EscapeRecord readEscapeRecord(byte[] data) {
		if (data == null || data.length < 4) {
			return null;
		}
		int escapeFunction = readUInt16(data, 0);
		int count = readUInt16(data, 2);
		if (count > data.length - 4) {
			return null;
		}
		byte[] payload = new byte[count];
		System.arraycopy(data, 4, payload, 0, count);
		return new EscapeRecord(escapeFunction, payload);
	}

	private static class EscapeRecord {
		private final int escapeFunction;
		private final byte[] payload;

		private EscapeRecord(int escapeFunction, byte[] payload) {
			this.escapeFunction = escapeFunction;
			this.payload = payload;
		}
	}

	private static class EmfHeader {
		private final int boundsLeft;
		private final int boundsTop;
		private final int boundsWidth;
		private final int boundsHeight;
		private final int frameWidth;
		private final int frameHeight;

		private EmfHeader(int boundsLeft, int boundsTop, int boundsRight, int boundsBottom, int frameLeft, int frameTop,
				int frameRight, int frameBottom) {
			this.boundsLeft = Math.min(boundsLeft, boundsRight);
			this.boundsTop = Math.min(boundsTop, boundsBottom);
			this.boundsWidth = Math.abs(boundsRight - boundsLeft);
			this.boundsHeight = Math.abs(boundsBottom - boundsTop);
			this.frameWidth = Math.abs(frameRight - frameLeft);
			this.frameHeight = Math.abs(frameBottom - frameTop);
		}
	}

	private static class PendingEmf {
		private final byte[] data;
		private final AwtDc dc;
		private final EmfHeader header;

		private PendingEmf(byte[] data, AwtDc dc, EmfHeader header) {
			this.data = data;
			this.dc = dc;
			this.header = header;
		}
	}

	private static class AwtSavedDc {
		private final AwtDc dc;
		private final Shape clip;

		private AwtSavedDc(AwtDc dc, Shape clip) {
			this.dc = dc;
			this.clip = clip;
		}
	}

	private Arc2D createArc(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya, int type) {
		double rx = Math.abs(exr - sxr) / 2.0;
		double ry = Math.abs(eyr - syr) / 2.0;
		if (rx <= 0 || ry <= 0) {
			return null;
		}
		RectangularShape frame = toRectangle(Math.min(sxr, exr), Math.min(syr, eyr), Math.abs(exr - sxr),
				Math.abs(eyr - syr));
		double cx = frame.getCenterX();
		double cy = frame.getCenterY();
		double start = toArcAngle(tx(sxa), ty(sya), cx, cy);
		double end = toArcAngle(tx(exa), ty(eya), cx, cy);
		double extent = dc.getArcDirection() == Gdi.AD_CLOCKWISE
				? normalizeDegrees(start - end)
				: -normalizeDegrees(end - start);
		if (sxa == exa && sya == eya) {
			extent = dc.getArcDirection() == Gdi.AD_CLOCKWISE ? 360.0 : -360.0;
		}
		return new Arc2D.Double(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight(), start, extent, type);
	}

	private double toArcAngle(double x, double y, double cx, double cy) {
		double angle = Math.toDegrees(Math.atan2(cy - y, x - cx));
		return angle < 0.0 ? angle + 360.0 : angle;
	}

	private double normalizeDegrees(double degrees) {
		double normalized = degrees % 360.0;
		return normalized < 0.0 ? normalized + 360.0 : normalized;
	}

	private Point getArcPoint(int sxr, int syr, int exr, int eyr, int x, int y) {
		double rx = Math.abs(exr - sxr) / 2.0;
		double ry = Math.abs(eyr - syr) / 2.0;
		double cx = Math.min(sxr, exr) + rx;
		double cy = Math.min(syr, eyr) + ry;
		double angle = Math.atan2((y - cy) * rx, (x - cx) * ry);
		return new Point((int) Math.round(rx * Math.cos(angle) + cx), (int) Math.round(ry * Math.sin(angle) + cy));
	}

	private Point circlePoint(int x, int y, int radius, double angle) {
		double radians = Math.toRadians(angle);
		int px = x + (int) Math.round(radius * Math.cos(radians));
		int py = y - (int) Math.round(radius * Math.sin(radians));
		return new Point(px, py);
	}

	private Area createRegionArea(byte[] rgnData, int count) {
		if (rgnData == null || rgnData.length < 32) {
			return null;
		}
		int rectCount = Math.min(count > 0 ? count : readInt32(rgnData, 8), Math.max(0, (rgnData.length - 32) / 16));
		Area area = new Area();
		for (int i = 0; i < rectCount; i++) {
			int offset = 32 + i * 16;
			int left = readInt32(rgnData, offset);
			int top = readInt32(rgnData, offset + 4);
			int right = readInt32(rgnData, offset + 8);
			int bottom = readInt32(rgnData, offset + 12);
			area.add(new Area(toRectangle(left, top, right - left, bottom - top)));
		}
		return area;
	}

	private boolean isValidVertex(Trivertex[] vertex, int index) {
		return index >= 0 && index < vertex.length;
	}

	private Area createFrameArea(Shape shape, int w, int h) {
		Area frame = new Area(shape);
		Rectangle2D bounds = shape.getBounds2D();
		double insetX = Math.abs(rx(w));
		double insetY = Math.abs(ry(h));
		if (insetX <= 0.0 || insetY <= 0.0 || bounds.getWidth() <= insetX * 2.0 || bounds.getHeight() <= insetY * 2.0) {
			return frame;
		}

		AffineTransform shrink = new AffineTransform();
		shrink.translate(bounds.getX() + insetX, bounds.getY() + insetY);
		shrink.scale((bounds.getWidth() - insetX * 2.0) / bounds.getWidth(),
				(bounds.getHeight() - insetY * 2.0) / bounds.getHeight());
		shrink.translate(-bounds.getX(), -bounds.getY());
		frame.subtract(new Area(shrink.createTransformedShape(shape)));
		return frame;
	}

	private void fillGradientRectangle(Trivertex upperLeft, Trivertex lowerRight, int mode) {
		Rectangle2D rect = toRectangle(upperLeft.x, upperLeft.y, lowerRight.x - upperLeft.x,
				lowerRight.y - upperLeft.y);
		float x1 = (float) rect.getX();
		float y1 = (float) rect.getY();
		float x2 = mode == Gdi.GRADIENT_FILL_RECT_H ? (float) (rect.getX() + rect.getWidth()) : x1;
		float y2 = mode == Gdi.GRADIENT_FILL_RECT_V ? (float) (rect.getY() + rect.getHeight()) : y1;
		fillWithPaint(rect,
				new GradientPaint(x1, y1, toRgbColor(upperLeft.getColor()), x2, y2, toRgbColor(lowerRight.getColor())));
	}

	private void fillGradientTriangle(Trivertex v1, Trivertex v2, Trivertex v3) {
		double x1 = tx(v1.x);
		double y1 = ty(v1.y);
		double x2 = tx(v2.x);
		double y2 = ty(v2.y);
		double x3 = tx(v3.x);
		double y3 = ty(v3.y);
		Path2D.Double triangle = new Path2D.Double();
		triangle.moveTo(x1, y1);
		triangle.lineTo(x2, y2);
		triangle.lineTo(x3, y3);
		triangle.closePath();
		ensureGraphics();
		ensureCanvasContains(triangle);

		Rectangle2D bounds = triangle.getBounds2D();
		int left = (int) Math.floor(bounds.getMinX());
		int top = (int) Math.floor(bounds.getMinY());
		int width = (int) Math.ceil(bounds.getMaxX()) - left;
		int height = (int) Math.ceil(bounds.getMaxY()) - top;
		double denominator = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3);
		if (width <= 0 || height <= 0 || denominator == 0.0) {
			return;
		}

		BufferedImage patch = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int c1 = v1.getColor();
		int c2 = v2.getColor();
		int c3 = v3.getColor();
		for (int y = 0; y < height; y++) {
			double py = top + y + 0.5;
			for (int x = 0; x < width; x++) {
				double px = left + x + 0.5;
				if (!triangle.contains(px, py)) {
					continue;
				}
				double w1 = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / denominator;
				double w2 = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / denominator;
				double w3 = 1.0 - w1 - w2;
				int red = interpolateTriangleChannel(c1 >>> 16, c2 >>> 16, c3 >>> 16, w1, w2, w3);
				int green = interpolateTriangleChannel(c1 >>> 8, c2 >>> 8, c3 >>> 8, w1, w2, w3);
				int blue = interpolateTriangleChannel(c1, c2, c3, w1, w2, w3);
				patch.setRGB(x, y, 0xFF000000 | (red << 16) | (green << 8) | blue);
			}
		}
		graphics.drawImage(patch, left, top, null);
	}

	private int interpolateTriangleChannel(int c1, int c2, int c3, double w1, double w2, double w3) {
		return Math.max(0, Math.min(255, (int) Math.round((c1 & 0xFF) * w1 + (c2 & 0xFF) * w2 + (c3 & 0xFF) * w3)));
	}

	private Color toRgbColor(int rgb) {
		return new Color((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF);
	}

	private void xorFill(Shape shape, Color xorColor) {
		ensureGraphics();
		Color oldColor = graphics.getColor();
		Paint oldPaint = graphics.getPaint();
		graphics.setXORMode(xorColor);
		graphics.setColor(Color.WHITE);
		graphics.fill(shape);
		graphics.setPaintMode();
		graphics.setPaint(oldPaint);
		graphics.setColor(oldColor);
	}

	private void drawShape(Shape shape) {
		fillShape(shape, dc.getBrush());
		strokeShape(shape, dc.getPen());
	}

	private void appendPoints(Point[] points, boolean close) {
		if (currentPath == null || points == null || points.length == 0) {
			return;
		}
		currentPath.moveTo(tx(points[0].x), ty(points[0].y));
		for (int i = 1; i < points.length; i++) {
			currentPath.lineTo(tx(points[i].x), ty(points[i].y));
		}
		if (close) {
			currentPath.closePath();
		}
	}

	private void appendBezier(Point[] points, boolean connect) {
		if (currentPath == null || points == null || points.length == 0) {
			return;
		}
		int offset = 0;
		if (!connect) {
			currentPath.moveTo(tx(points[0].x), ty(points[0].y));
			offset = 1;
		}
		for (int i = offset; i + 2 < points.length; i += 3) {
			currentPath.curveTo(tx(points[i].x), ty(points[i].y), tx(points[i + 1].x), ty(points[i + 1].y),
					tx(points[i + 2].x), ty(points[i + 2].y));
		}
	}

	private Path2D.Double createBezierPath(Point[] points, boolean connect) {
		if (points == null || points.length == 0) {
			return null;
		}
		Path2D.Double path = new Path2D.Double();
		int offset = 0;
		if (connect) {
			path.moveTo(tx(dc.getCurrentX()), ty(dc.getCurrentY()));
		} else {
			path.moveTo(tx(points[0].x), ty(points[0].y));
			offset = 1;
		}
		for (int i = offset; i + 2 < points.length; i += 3) {
			path.curveTo(tx(points[i].x), ty(points[i].y), tx(points[i + 1].x), ty(points[i + 1].y),
					tx(points[i + 2].x), ty(points[i + 2].y));
		}
		return path;
	}

	private void fillWithPaint(Shape shape, Paint paint) {
		ensureGraphics();
		ensureCanvasContains(shape);
		Paint old = graphics.getPaint();
		graphics.setPaint(paint);
		graphics.fill(shape);
		graphics.setPaint(old);
	}

	private void applyClip(Shape shape, int mode) {
		if (shape == null) {
			if (mode == GdiRegion.RGN_COPY) {
				graphics.setClip(null);
			}
			return;
		}

		Area newArea = new Area(shape);
		Shape oldClip = graphics.getClip();
		if (oldClip == null || mode == GdiRegion.RGN_COPY) {
			graphics.setClip(newArea);
			return;
		}

		Area oldArea = new Area(oldClip);
		if (mode == GdiRegion.RGN_OR) {
			oldArea.add(newArea);
		} else if (mode == GdiRegion.RGN_XOR) {
			oldArea.exclusiveOr(newArea);
		} else if (mode == GdiRegion.RGN_DIFF) {
			oldArea.subtract(newArea);
		} else {
			oldArea.intersect(newArea);
		}
		graphics.setClip(oldArea);
	}

	private void fillShape(Shape shape, GdiBrush brush) {
		ensureGraphics();
		if (brush == null || brush.getStyle() == GdiBrush.BS_NULL || brush.getStyle() == GdiBrush.BS_HOLLOW) {
			return;
		}
		ensureCanvasContains(shape);
		Paint old = graphics.getPaint();
		graphics.setPaint(toPaint(brush));
		graphics.fill(shape);
		graphics.setPaint(old);
	}

	private void strokeShape(Shape shape, GdiPen pen) {
		ensureGraphics();
		if (pen == null || (pen.getStyle() & GdiPen.PS_STYLE_MASK) == GdiPen.PS_NULL) {
			return;
		}
		BasicStroke stroke = toStroke(pen);
		ensureCanvasContains(shape);
		if (dc.getROP2() != Gdi.R2_COPYPEN) {
			strokeShapeRop2(shape, stroke, pen.getColor());
			return;
		}
		Paint oldPaint = graphics.getPaint();
		java.awt.Stroke oldStroke = graphics.getStroke();
		graphics.setPaint(toColor(pen.getColor()));
		graphics.setStroke(stroke);
		graphics.draw(shape);
		graphics.setStroke(oldStroke);
		graphics.setPaint(oldPaint);
	}

	private void strokeShapeRop2(Shape shape, BasicStroke stroke, int color) {
		if (image == null || dc.getROP2() == Gdi.R2_COPYPEN) {
			return;
		}
		if (dc.getROP2() == Gdi.R2_NOP) {
			return;
		}

		BufferedImage mask = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D mg = mask.createGraphics();
		try {
			configureGraphics(mg);
			mg.setClip(graphics.getClip());
			mg.setColor(Color.WHITE);
			mg.setStroke(stroke);
			mg.draw(shape);
		} finally {
			mg.dispose();
		}

		Rectangle2D bounds = stroke.createStrokedShape(shape).getBounds2D();
		int left = Math.max(0, (int) Math.floor(bounds.getMinX()));
		int top = Math.max(0, (int) Math.floor(bounds.getMinY()));
		int right = Math.min(canvasWidth, (int) Math.ceil(bounds.getMaxX()));
		int bottom = Math.min(canvasHeight, (int) Math.ceil(bounds.getMaxY()));
		int penRgb = toColor(color).getRGB() & 0x00FFFFFF;
		for (int y = top; y < bottom; y++) {
			for (int x = left; x < right; x++) {
				if ((mask.getRGB(x, y) & 0x00FFFFFF) == 0) {
					continue;
				}
				int dest = image.getRGB(x, y) & 0x00FFFFFF;
				int rgb = applyRop2(penRgb, dest, dc.getROP2());
				image.setRGB(x, y, 0xFF000000 | rgb);
			}
		}
	}

	private int applyRop2(int pen, int dest, int mode) {
		int rgb;
		switch (mode) {
			case Gdi.R2_BLACK :
				rgb = 0;
				break;
			case Gdi.R2_NOTMERGEPEN :
				rgb = ~(dest | pen);
				break;
			case Gdi.R2_MASKNOTPEN :
				rgb = dest & ~pen;
				break;
			case Gdi.R2_NOTCOPYPEN :
				rgb = ~pen;
				break;
			case Gdi.R2_MASKPENNOT :
				rgb = pen & ~dest;
				break;
			case Gdi.R2_NOT :
				rgb = ~dest;
				break;
			case Gdi.R2_XORPEN :
				rgb = dest ^ pen;
				break;
			case Gdi.R2_NOTMASKPEN :
				rgb = ~(dest & pen);
				break;
			case Gdi.R2_MASKPEN :
				rgb = dest & pen;
				break;
			case Gdi.R2_NOTXORPEN :
				rgb = ~(dest ^ pen);
				break;
			case Gdi.R2_MERGENOTPEN :
				rgb = dest | ~pen;
				break;
			case Gdi.R2_MERGEPENNOT :
				rgb = pen | ~dest;
				break;
			case Gdi.R2_MERGEPEN :
				rgb = dest | pen;
				break;
			case Gdi.R2_WHITE :
				rgb = 0x00FFFFFF;
				break;
			case Gdi.R2_COPYPEN :
			default :
				rgb = pen;
				break;
		}
		return rgb & 0x00FFFFFF;
	}

	private BasicStroke toStroke(GdiPen pen) {
		int style = pen.getStyle() & GdiPen.PS_STYLE_MASK;
		float width = (float) Math.max(1.0, Math.abs(rx(Math.max(pen.getWidth(), 1))));
		float[] dash = null;
		if (style == GdiPen.PS_DASH) {
			dash = new float[]{4 * width, 2 * width};
		} else if (style == GdiPen.PS_DOT) {
			dash = new float[]{width, 2 * width};
		} else if (style == GdiPen.PS_DASHDOT) {
			dash = new float[]{4 * width, 2 * width, width, 2 * width};
		} else if (style == GdiPen.PS_DASHDOTDOT) {
			dash = new float[]{4 * width, 2 * width, width, 2 * width, width, 2 * width};
		}
		return dash == null
				? new BasicStroke(width)
				: new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
						Math.max(dc.getMiterLimit(), 1.0f), dash, 0.0f);
	}

	private Paint toPaint(GdiBrush brush) {
		if (brush instanceof AwtPatternBrush) {
			BufferedImage pattern = decodeBitmap(((AwtPatternBrush) brush).image, ((AwtPatternBrush) brush).usage,
					false, null, false);
			if (pattern != null) {
				return new TexturePaint(pattern, new Rectangle2D.Double(tx(dc.getBrushOrgX()), ty(dc.getBrushOrgY()),
						pattern.getWidth(), pattern.getHeight()));
			}
		}
		if (brush.getStyle() == GdiBrush.BS_HATCHED) {
			return createHatchPaint(brush);
		}
		return toColor(brush.getColor());
	}

	private Paint createHatchPaint(GdiBrush brush) {
		BufferedImage pattern = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = pattern.createGraphics();
		try {
			configureGraphics(g);
			if (dc.getBkMode() == Gdi.OPAQUE) {
				g.setColor(toColor(dc.getBkColor()));
				g.fillRect(0, 0, 8, 8);
			}
			g.setColor(toColor(brush.getColor()));
			int hatch = brush.getHatch();
			if (hatch == GdiBrush.HS_HORIZONTAL || hatch == GdiBrush.HS_CROSS) {
				g.drawLine(0, 4, 7, 4);
			}
			if (hatch == GdiBrush.HS_VERTICAL || hatch == GdiBrush.HS_CROSS) {
				g.drawLine(4, 0, 4, 7);
			}
			if (hatch == GdiBrush.HS_FDIAGONAL || hatch == GdiBrush.HS_DIAGCROSS) {
				g.drawLine(0, 7, 7, 0);
			}
			if (hatch == GdiBrush.HS_BDIAGONAL || hatch == GdiBrush.HS_DIAGCROSS) {
				g.drawLine(0, 0, 7, 7);
			}
		} finally {
			g.dispose();
		}
		return new TexturePaint(pattern, new Rectangle2D.Double(tx(dc.getBrushOrgX()), ty(dc.getBrushOrgY()),
				pattern.getWidth(), pattern.getHeight()));
	}

	private Color toColor(int gdiColor) {
		int red = gdiColor & 0xFF;
		int green = (gdiColor >>> 8) & 0xFF;
		int blue = (gdiColor >>> 16) & 0xFF;
		return new Color(red, green, blue);
	}

	private void drawText(int x, int y, String text) {
		ensureGraphics();
		if (text == null || text.length() == 0) {
			return;
		}
		int align = dc.getTextAlign();
		if ((align & (Gdi.TA_NOUPDATECP | Gdi.TA_UPDATECP)) == Gdi.TA_UPDATECP) {
			x = dc.getCurrentX();
			y = dc.getCurrentY();
		}
		AwtFont gdiFont = dc.getFont();
		Font font = toFont(gdiFont);
		graphics.setFont(font);
		graphics.setPaint(toColor(dc.getTextColor()));
		FontMetrics metrics = graphics.getFontMetrics(font);
		int drawX = (int) tx(x);
		int drawY = (int) ty(y);
		if ((align & Gdi.TA_CENTER) == Gdi.TA_CENTER) {
			drawX -= metrics.stringWidth(text) / 2;
		} else if ((align & Gdi.TA_RIGHT) == Gdi.TA_RIGHT) {
			drawX -= metrics.stringWidth(text);
		}
		if ((align & Gdi.TA_BASELINE) != Gdi.TA_BASELINE) {
			if ((align & Gdi.TA_BOTTOM) == Gdi.TA_BOTTOM) {
				drawY -= metrics.getDescent();
			} else {
				drawY += metrics.getAscent();
			}
		}

		AttributedString attributed = new AttributedString(text);
		attributed.addAttribute(TextAttribute.FONT, font);
		if (gdiFont != null && gdiFont.isUnderlined()) {
			attributed.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		}
		if (gdiFont != null && gdiFont.isStrikedOut()) {
			attributed.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		}
		AffineTransform old = graphics.getTransform();
		if (gdiFont != null && gdiFont.getEscapement() != 0) {
			graphics.rotate(-Math.toRadians(gdiFont.getEscapement() / 10.0), drawX, drawY);
		}
		graphics.drawString(attributed.getIterator(), drawX, drawY);
		graphics.setTransform(old);
		if ((align & (Gdi.TA_NOUPDATECP | Gdi.TA_UPDATECP)) == Gdi.TA_UPDATECP) {
			dc.moveToEx(x + toLogicalTextAdvance(metrics.stringWidth(text)), y, null);
		}
	}

	private int toLogicalTextAdvance(int deviceWidth) {
		double unit = rx(1.0);
		if (unit == 0.0) {
			return deviceWidth;
		}
		return (int) Math.round(deviceWidth / unit);
	}

	private Font toFont(AwtFont gdiFont) {
		if (gdiFont == null) {
			return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
		}
		int style = Font.PLAIN;
		if (gdiFont.getWeight() >= GdiFont.FW_BOLD) {
			style |= Font.BOLD;
		}
		if (gdiFont.isItalic()) {
			style |= Font.ITALIC;
		}
		int size = Math.max(1, (int) Math.round(Math.abs(ry(gdiFont.getHeight()))));
		String name = gdiFont.getFaceName();
		if (name == null || name.length() == 0) {
			name = Font.SANS_SERIF;
		}
		return new Font(name, style, size);
	}

	private void drawBitmap(byte[] data, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, int usage,
			Object ropOrTransparentColor, boolean preserveAlpha) {
		ensureGraphics();
		if (data == null || data.length == 0) {
			return;
		}
		Integer transparentColor = ropOrTransparentColor instanceof Integer ? (Integer) ropOrTransparentColor : null;
		Long rop = ropOrTransparentColor instanceof Long ? (Long) ropOrTransparentColor : Long.valueOf(Gdi.SRCCOPY);
		if (rop.longValue() == Gdi.PATCOPY) {
			patBlt(dx, dy, dw, dh, rop.longValue());
			return;
		}
		if (!isSupportedBitmapRop(rop.longValue())) {
			return;
		}
		BufferedImage source = decodeBitmap(data, usage, sh < 0, transparentColor, preserveAlpha);
		if (source == null) {
			return;
		}
		int srcX = sw < 0 ? sx + sw : sx;
		int srcY = sh < 0 ? sy + sh : sy;
		int srcW = Math.abs(sw);
		int srcH = Math.abs(sh);
		srcX = Math.max(0, Math.min(source.getWidth(), srcX));
		srcY = Math.max(0, Math.min(source.getHeight(), srcY));
		srcW = Math.max(0, Math.min(source.getWidth() - srcX, srcW));
		srcH = Math.max(0, Math.min(source.getHeight() - srcY, srcH));
		if (srcW == 0 || srcH == 0) {
			return;
		}
		Image subImage = source.getSubimage(srcX, srcY, srcW, srcH);
		int x1 = (int) tx(dx);
		int y1 = (int) ty(dy);
		int x2 = (int) tx(dx + dw);
		int y2 = (int) ty(dy + dh);
		ensureCanvasContains(x1, y1, x2, y2);
		if (rop.longValue() == Gdi.SRCCOPY && transparentColor == null) {
			graphics.drawImage(subImage, x1, y1, x2, y2, 0, 0, srcW, srcH, null);
		} else {
			composeBitmapRop(subImage, x1, y1, x2, y2, rop.longValue());
		}
	}

	private void drawAlphaBlend(byte[] data, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh,
			int sourceConstantAlpha, boolean preserveAlpha) {
		ensureGraphics();
		if (data == null || data.length == 0 || sourceConstantAlpha == 0) {
			return;
		}
		BufferedImage source = decodeBitmap(data, Gdi.DIB_RGB_COLORS, sh < 0, null, preserveAlpha);
		if (source == null) {
			return;
		}
		int srcX = sw < 0 ? sx + sw : sx;
		int srcY = sh < 0 ? sy + sh : sy;
		int srcW = Math.abs(sw);
		int srcH = Math.abs(sh);
		srcX = Math.max(0, Math.min(source.getWidth(), srcX));
		srcY = Math.max(0, Math.min(source.getHeight(), srcY));
		srcW = Math.max(0, Math.min(source.getWidth() - srcX, srcW));
		srcH = Math.max(0, Math.min(source.getHeight() - srcY, srcH));
		if (srcW == 0 || srcH == 0) {
			return;
		}

		int x1 = (int) tx(dx);
		int y1 = (int) ty(dy);
		int x2 = (int) tx(dx + dw);
		int y2 = (int) ty(dy + dh);
		int left = Math.min(x1, x2);
		int top = Math.min(y1, y2);
		int width = Math.abs(x2 - x1);
		int height = Math.abs(y2 - y1);
		if (width <= 0 || height <= 0) {
			return;
		}
		ensureCanvasContains(x1, y1, x2, y2);

		BufferedImage blended = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D bg = blended.createGraphics();
		try {
			configureGraphics(bg);
			bg.drawImage(source.getSubimage(srcX, srcY, srcW, srcH), x1 < x2 ? 0 : width, y1 < y2 ? 0 : height,
					x1 < x2 ? width : 0, y1 < y2 ? height : 0, null);
		} finally {
			bg.dispose();
		}
		if (sourceConstantAlpha < 255) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int argb = blended.getRGB(x, y);
					int alpha = (argb >>> 24) * sourceConstantAlpha / 255;
					blended.setRGB(x, y, (alpha << 24) | (argb & 0x00FFFFFF));
				}
			}
		}
		graphics.drawImage(blended, left, top, null);
	}

	private void drawDIBitsToDevice(byte[] data, int dx, int dy, int dw, int dh, int sx, int sy, int startscan,
			int scanlines, int usage) {
		ensureGraphics();
		if (data == null || data.length == 0) {
			return;
		}
		BufferedImage source = decodeBitmap(data, usage, false, null, false);
		if (source == null) {
			return;
		}
		int srcX = Math.max(0, Math.min(source.getWidth(), sx));
		int srcW = Math.max(0, Math.min(source.getWidth() - srcX, dw));
		int srcH = Math.max(0, Math.min(source.getHeight(), scanlines > 0 ? scanlines : dh));
		if (srcW == 0 || srcH == 0) {
			return;
		}

		int srcY = source.getHeight() - Math.max(0, startscan) - srcH;
		srcY = Math.max(0, Math.min(source.getHeight() - srcH, srcY - sy));
		Image subImage = source.getSubimage(srcX, srcY, srcW, srcH);
		int x1 = (int) tx(dx);
		int y1 = (int) ty(dy);
		int x2 = (int) tx(dx + srcW);
		int y2 = (int) ty(dy + srcH);
		ensureCanvasContains(x1, y1, x2, y2);
		graphics.drawImage(subImage, x1, y1, x2, y2, 0, 0, srcW, srcH, null);
	}

	private void drawPlgBlt(byte[] data, Point[] points, int sx, int sy, int sw, int sh, byte[] mask, int mx, int my) {
		ensureGraphics();
		if (data == null || data.length == 0) {
			return;
		}
		BufferedImage source = decodeBitmap(data, Gdi.DIB_RGB_COLORS, sh < 0, null, true);
		if (source == null) {
			return;
		}
		if (mask != null) {
			source = applyMask(source, mask, mx, my);
		}
		int srcX = sw < 0 ? sx + sw : sx;
		int srcY = sh < 0 ? sy + sh : sy;
		int srcW = Math.abs(sw);
		int srcH = Math.abs(sh);
		srcX = Math.max(0, Math.min(source.getWidth(), srcX));
		srcY = Math.max(0, Math.min(source.getHeight(), srcY));
		srcW = Math.max(0, Math.min(source.getWidth() - srcX, srcW));
		srcH = Math.max(0, Math.min(source.getHeight() - srcY, srcH));
		if (srcW == 0 || srcH == 0) {
			return;
		}

		double x0 = tx(points[0].x);
		double y0 = ty(points[0].y);
		double x1 = tx(points[1].x);
		double y1 = ty(points[1].y);
		double x2 = tx(points[2].x);
		double y2 = ty(points[2].y);
		Path2D.Double bounds = new Path2D.Double();
		bounds.moveTo(x0, y0);
		bounds.lineTo(x1, y1);
		bounds.lineTo(x1 + x2 - x0, y1 + y2 - y0);
		bounds.lineTo(x2, y2);
		bounds.closePath();
		ensureCanvasContains(bounds);

		AffineTransform old = graphics.getTransform();
		graphics.transform(
				new AffineTransform((x1 - x0) / srcW, (y1 - y0) / srcW, (x2 - x0) / srcH, (y2 - y0) / srcH, x0, y0));
		graphics.drawImage(source.getSubimage(srcX, srcY, srcW, srcH), 0, 0, null);
		graphics.setTransform(old);
	}

	private void drawMaskedSrcCopy(byte[] data, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh,
			byte[] mask, int mx, int my) {
		ensureGraphics();
		if (data == null || data.length == 0) {
			return;
		}
		BufferedImage source = decodeBitmap(data, Gdi.DIB_RGB_COLORS, sh < 0, null, true);
		if (source == null) {
			return;
		}
		source = applyMask(source, mask, mx, my);
		int srcX = sw < 0 ? sx + sw : sx;
		int srcY = sh < 0 ? sy + sh : sy;
		int srcW = Math.abs(sw);
		int srcH = Math.abs(sh);
		srcX = Math.max(0, Math.min(source.getWidth(), srcX));
		srcY = Math.max(0, Math.min(source.getHeight(), srcY));
		srcW = Math.max(0, Math.min(source.getWidth() - srcX, srcW));
		srcH = Math.max(0, Math.min(source.getHeight() - srcY, srcH));
		if (srcW == 0 || srcH == 0) {
			return;
		}
		int x1 = (int) tx(dx);
		int y1 = (int) ty(dy);
		int x2 = (int) tx(dx + dw);
		int y2 = (int) ty(dy + dh);
		ensureCanvasContains(x1, y1, x2, y2);
		graphics.drawImage(source.getSubimage(srcX, srcY, srcW, srcH), x1, y1, x2, y2, 0, 0, srcW, srcH, null);
	}

	private BufferedImage applyMask(BufferedImage source, byte[] mask, int mx, int my) {
		BufferedImage maskImage = decodeBitmap(mask, Gdi.DIB_RGB_COLORS, false, null, false);
		if (maskImage == null) {
			return source;
		}
		BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < result.getHeight(); y++) {
			int maskY = Math.max(0,
					Math.min(maskImage.getHeight() - 1, my + y * maskImage.getHeight() / result.getHeight()));
			for (int x = 0; x < result.getWidth(); x++) {
				int maskX = Math.max(0,
						Math.min(maskImage.getWidth() - 1, mx + x * maskImage.getWidth() / result.getWidth()));
				int maskRgb = maskImage.getRGB(maskX, maskY);
				int alpha = (((maskRgb >>> 16) & 0xFF) + ((maskRgb >>> 8) & 0xFF) + (maskRgb & 0xFF)) / 3;
				result.setRGB(x, y, (source.getRGB(x, y) & 0x00FFFFFF) | (alpha << 24));
			}
		}
		return result;
	}

	private boolean isSupportedBitmapRop(long rop) {
		return rop == Gdi.BLACKNESS || rop == 0x001100A6L || rop == Gdi.NOTSRCCOPY || rop == 0x00440328L
				|| rop == Gdi.DSTINVERT || rop == Gdi.SRCINVERT || rop == 0x008800C6L || rop == Gdi.MERGEPAINT
				|| rop == Gdi.MERGECOPY || rop == Gdi.SRCCOPY || rop == Gdi.SRCPAINT || rop == Gdi.PATPAINT
				|| rop == Gdi.WHITENESS;
	}

	private void composeBitmapRop(Image source, int x1, int y1, int x2, int y2, long rop) {
		int left = Math.min(x1, x2);
		int top = Math.min(y1, y2);
		int width = Math.abs(x2 - x1);
		int height = Math.abs(y2 - y1);
		if (width <= 0 || height <= 0) {
			return;
		}

		BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = src.createGraphics();
		try {
			configureGraphics(sg);
			sg.drawImage(source, x1 < x2 ? 0 : width, y1 < y2 ? 0 : height, x1 < x2 ? width : 0, y1 < y2 ? height : 0,
					null);
		} finally {
			sg.dispose();
		}

		BufferedImage pat = null;
		if (rop == Gdi.MERGECOPY || rop == Gdi.PATPAINT) {
			pat = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D pg = pat.createGraphics();
			try {
				configureGraphics(pg);
				pg.translate(-left, -top);
				pg.setPaint(toPaint(dc.getBrush()));
				pg.fillRect(left, top, width, height);
			} finally {
				pg.dispose();
			}
		}

		int clipLeft = Math.max(left, 0);
		int clipTop = Math.max(top, 0);
		int clipRight = Math.min(left + width, canvasWidth);
		int clipBottom = Math.min(top + height, canvasHeight);
		for (int y = clipTop; y < clipBottom; y++) {
			for (int x = clipLeft; x < clipRight; x++) {
				int sx = x - left;
				int sy = y - top;
				int s = src.getRGB(sx, sy) & 0x00FFFFFF;
				int d = image.getRGB(x, y) & 0x00FFFFFF;
				int p = pat != null ? pat.getRGB(sx, sy) & 0x00FFFFFF : 0;
				image.setRGB(x, y, 0xFF000000 | applyBitmapRop(s, d, p, rop));
			}
		}
	}

	private int applyBitmapRop(int s, int d, int p, long rop) {
		int rgb;
		if (rop == Gdi.BLACKNESS) {
			rgb = 0;
		} else if (rop == 0x001100A6L) {
			rgb = ~(s | d);
		} else if (rop == Gdi.NOTSRCCOPY) {
			rgb = ~s;
		} else if (rop == 0x00440328L) {
			rgb = s & ~d;
		} else if (rop == Gdi.DSTINVERT) {
			rgb = ~d;
		} else if (rop == Gdi.SRCINVERT) {
			rgb = s ^ d;
		} else if (rop == 0x008800C6L) {
			rgb = s & d;
		} else if (rop == Gdi.MERGEPAINT) {
			rgb = ~s | d;
		} else if (rop == Gdi.MERGECOPY) {
			rgb = s & p;
		} else if (rop == Gdi.SRCPAINT) {
			rgb = s | d;
		} else if (rop == Gdi.PATPAINT) {
			rgb = d | p | ~s;
		} else if (rop == Gdi.WHITENESS) {
			rgb = 0x00FFFFFF;
		} else {
			rgb = s;
		}
		return rgb & 0x00FFFFFF;
	}

	private BufferedImage decodeBitmap(byte[] data, int usage, boolean reverse, Integer transparentColor,
			boolean preserveAlpha) {
		BufferedImage decoded = decodeWmfBitmap(data, reverse, transparentColor, preserveAlpha);
		if (decoded == null) {
			decoded = decodeDib(applyPaletteToDib(data, usage), reverse, transparentColor, preserveAlpha);
		}
		if (decoded != null) {
			return decoded;
		}
		try {
			return ImageIO.read(new ByteArrayInputStream(data));
		} catch (IOException e) {
			return null;
		}
	}

	private Rectangle2D toRectangle(int x, int y, int width, int height) {
		double x1 = tx(x);
		double y1 = ty(y);
		double x2 = tx(x + width);
		double y2 = ty(y + height);
		return new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
	}

	private Polygon toPolygon(Point[] points) {
		if (points == null || points.length == 0) {
			return null;
		}
		Polygon polygon = new Polygon();
		for (int i = 0; i < points.length; i++) {
			polygon.addPoint((int) tx(points[i].x), (int) ty(points[i].y));
		}
		return polygon;
	}

	private double tx(double x) {
		return dc.toAbsoluteX(x);
	}

	private double ty(double y) {
		return dc.toAbsoluteY(y);
	}

	private double rx(double x) {
		return dc.toRelativeX(x);
	}

	private double ry(double y) {
		return dc.toRelativeY(y);
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
			int entry = paletteIndex >= 0 && paletteIndex < entries.length ? entries[paletteIndex] : 0;
			int offset = headerSize + i * 4;
			rgbDib[offset] = (byte) ((entry >>> 16) & 0xFF);
			rgbDib[offset + 1] = (byte) ((entry >>> 8) & 0xFF);
			rgbDib[offset + 2] = (byte) (entry & 0xFF);
			rgbDib[offset + 3] = 0;
		}
		System.arraycopy(dib, headerSize + indexTableSize, rgbDib, headerSize + colorCount * 4,
				dib.length - headerSize - indexTableSize);
		return rgbDib;
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
				|| (bitCount != 1 && bitCount != 4 && bitCount != 8 && bitCount != 24 && bitCount != 32)
				|| bitmap.length < 10 + stride * height) {
			return null;
		}
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			int sourceY = reverse ? height - 1 - y : y;
			int row = 10 + sourceY * stride;
			for (int x = 0; x < width; x++) {
				image.setRGB(x, y, readBitmapPixel(bitmap, row, x, bitCount, null, transparentColor, preserveAlpha));
			}
		}
		return image;
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
		if (width <= 0 || heightValue == 0 || planes != 1 || (compression != 0 && compression != 3)) {
			return null;
		}
		int height = Math.abs(heightValue);
		int[] bitMasks = readDibBitMasks(dib, headerSize, compression, bitCount);
		int colorCount = getDibColorCount(dib, headerSize, bitCount);
		int bitsOffset = headerSize + (compression == 3 && headerSize == 40 ? 12 : 0) + colorCount * 4;
		int stride = ((width * bitCount + 31) / 32) * 4;
		if (colorCount > 0 && dib.length < bitsOffset + stride * height && dib.length >= headerSize + stride * height) {
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
				colors[i] = applyAlpha(dib[pos + 2] & 0xFF, dib[pos + 1] & 0xFF, dib[pos] & 0xFF, 0xFF,
						transparentColor, preserveAlpha);
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
				image.setRGB(x, y,
						readBitmapPixel(dib, row, x, bitCount, colors, bitMasks, transparentColor, preserveAlpha));
			}
		}
		return image;
	}

	private int readBitmapPixel(byte[] data, int row, int x, int bitCount, int[] colors, Integer transparentColor,
			boolean preserveAlpha) {
		return readBitmapPixel(data, row, x, bitCount, colors, null, transparentColor, preserveAlpha);
	}

	private int readBitmapPixel(byte[] data, int row, int x, int bitCount, int[] colors, int[] bitMasks,
			Integer transparentColor, boolean preserveAlpha) {
		if (bitCount == 1) {
			int index = (data[row + x / 8] >>> (7 - (x % 8))) & 0x01;
			return colors != null && index < colors.length ? colors[index] : index == 0 ? 0xFF000000 : 0xFFFFFFFF;
		} else if (bitCount == 4) {
			int value = data[row + x / 2] & 0xFF;
			int index = x % 2 == 0 ? value >>> 4 : value & 0x0F;
			return colors != null && index < colors.length ? colors[index] : 0xFF000000;
		} else if (bitCount == 8) {
			int index = data[row + x] & 0xFF;
			return colors != null && index < colors.length
					? colors[index]
					: applyAlpha(index, index, index, 0xFF, transparentColor, preserveAlpha);
		} else if (bitCount == 24) {
			int pos = row + x * 3;
			return applyAlpha(data[pos + 2] & 0xFF, data[pos + 1] & 0xFF, data[pos] & 0xFF, 0xFF, transparentColor,
					preserveAlpha);
		} else if (bitCount == 32) {
			int pos = row + x * 4;
			if (bitMasks != null) {
				int value = readInt32(data, pos);
				return applyAlpha(extractMaskedChannel(value, bitMasks[0]), extractMaskedChannel(value, bitMasks[1]),
						extractMaskedChannel(value, bitMasks[2]), 0xFF, transparentColor, preserveAlpha);
			}
			return applyAlpha(data[pos + 2] & 0xFF, data[pos + 1] & 0xFF, data[pos] & 0xFF,
					preserveAlpha ? data[pos + 3] & 0xFF : 0xFF, transparentColor, preserveAlpha);
		} else if (bitCount == 16 && bitMasks != null) {
			int value = readUInt16(data, row + x * 2);
			return applyAlpha(extractMaskedChannel(value, bitMasks[0]), extractMaskedChannel(value, bitMasks[1]),
					extractMaskedChannel(value, bitMasks[2]), 0xFF, transparentColor, preserveAlpha);
		}
		return 0x00000000;
	}

	private int[] readDibBitMasks(byte[] dib, int headerSize, int compression, int bitCount) {
		if (compression != 3 || (bitCount != 16 && bitCount != 32)) {
			return null;
		}
		int offset = headerSize >= 52 ? 40 : headerSize == 40 ? 40 : -1;
		if (offset < 0 || dib.length < offset + 12) {
			return null;
		}
		return new int[]{readInt32(dib, offset), readInt32(dib, offset + 4), readInt32(dib, offset + 8)};
	}

	private int extractMaskedChannel(int value, int mask) {
		if (mask == 0) {
			return 0;
		}
		int shift = Integer.numberOfTrailingZeros(mask);
		int bits = Integer.bitCount(mask);
		int channel = (value & mask) >>> shift;
		int max = (1 << bits) - 1;
		return max > 0 ? (channel * 255 + max / 2) / max : 0;
	}

	private int applyAlpha(int red, int green, int blue, int alpha, Integer transparentColor, boolean preserveAlpha) {
		int rgb = (red << 16) | (green << 8) | blue;
		if (transparentColor != null) {
			int transparentRgb = ((transparentColor.intValue() & 0x000000FF) << 16)
					| (transparentColor.intValue() & 0x0000FF00) | ((transparentColor.intValue() & 0x00FF0000) >> 16);
			if (rgb == transparentRgb) {
				return rgb;
			}
		}
		return ((preserveAlpha ? alpha : 0xFF) << 24) | rgb;
	}

	private int getDibColorCount(byte[] dib, int headerSize, int bitCount) {
		long clrUsed = readUInt32(dib, 32);
		if (clrUsed > 0 && clrUsed <= Integer.MAX_VALUE) {
			return (int) clrUsed;
		}
		switch (bitCount) {
			case 1 :
				return 2;
			case 4 :
				return 16;
			case 8 :
				return 256;
			default :
				return 0;
		}
	}

	private static int readInt32(byte[] data, int offset) {
		return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16)
				| (data[offset + 3] << 24);
	}

	private static int readUInt16(byte[] data, int offset) {
		return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
	}

	private static long readUInt32(byte[] data, int offset) {
		return readInt32(data, offset) & 0xFFFFFFFFL;
	}

}

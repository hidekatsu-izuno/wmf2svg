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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.AttributedString;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
import net.arnx.wmf2svg.gdi.emf.EmfPlusConstants;
import net.arnx.wmf2svg.gdi.emf.EmfPlusParser;
import net.arnx.wmf2svg.gdi.emf.EmfParseException;
import net.arnx.wmf2svg.gdi.emf.EmfParser;
import net.arnx.wmf2svg.gdi.wmf.WmfParseException;
import net.arnx.wmf2svg.gdi.wmf.WmfParser;
import net.arnx.wmf2svg.util.FontUtil;
import net.arnx.wmf2svg.util.SymbolFontMappings;

public class AwtGdi implements Gdi, EmfPlusConstants {
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
	private boolean emfHeaderCanvas;
	private int emfHeaderFrameCanvasWidth;
	private int emfHeaderFrameCanvasHeight;
	private double placeableViewportScaleX = 1.0;
	private double placeableViewportScaleY = 1.0;
	private boolean growCanvas = true;
	private int initialDpi = 1440;
	private int canvasWidth = DEFAULT_CANVAS_WIDTH;
	private int canvasHeight = DEFAULT_CANVAS_HEIGHT;
	private int canvasMinX;
	private int canvasMinY;
	private Rectangle2D contentBounds;
	private boolean opaqueBackground;
	private boolean replaceSymbolFont;
	private AwtBrush defaultBrush;
	private AwtPen defaultPen;
	private AwtFont defaultFont;
	private AwtPalette selectedPalette;
	private GdiColorSpace selectedColorSpace;
	private byte[] lastDibPatternBrushImage;
	private int lastDibPatternBrushUsage = Gdi.DIB_RGB_COLORS;
	private Path2D.Double currentPath;
	private ByteArrayOutputStream emfBuffer;
	private int emfTotalSize;
	private ArrayList<PendingEmf> pendingEmfList = new ArrayList<PendingEmf>();
	private boolean replayingPendingEmf;
	private int ignoredPendingEmfHeaderMappings;
	private boolean parseEmfPlusComments = true;
	private EmfPlusParser emfPlusParser = new EmfPlusParser();
	private Map<Integer, byte[]> emfPlusMetafileImages = new HashMap<Integer, byte[]>();
	private Map<Integer, BufferedImage> emfPlusBitmapImages = new HashMap<Integer, BufferedImage>();
	private Map<Integer, PendingEmfPlusObject> pendingEmfPlusObjects = new HashMap<Integer, PendingEmfPlusObject>();
	private Map<Integer, EmfPlusBrush> emfPlusBrushes = new HashMap<Integer, EmfPlusBrush>();
	private Map<Integer, EmfPlusPen> emfPlusPens = new HashMap<Integer, EmfPlusPen>();
	private Map<Integer, EmfPlusPath> emfPlusPaths = new HashMap<Integer, EmfPlusPath>();
	private Map<Integer, EmfPlusRegion> emfPlusRegions = new HashMap<Integer, EmfPlusRegion>();
	private Map<Integer, EmfPlusFont> emfPlusFonts = new HashMap<Integer, EmfPlusFont>();
	private Map<Integer, EmfPlusStringFormat> emfPlusStringFormats = new HashMap<Integer, EmfPlusStringFormat>();
	private Map<Integer, EmfPlusImageAttributes> emfPlusImageAttributes = new HashMap<Integer, EmfPlusImageAttributes>();
	private EmfPlusImageEffect pendingEmfPlusImageEffect;
	private boolean suppressEmfPlusFallback;
	private boolean emfPlusGetDCActive;
	private double[] emfPlusWorldTransform = new double[]{1, 0, 0, 1, 0, 0};
	private double emfPlusPageScale = 1.0;
	private double emfPlusPageUnitScale = 1.0;
	private Object emfPlusAntiAliasingHint = RenderingHints.VALUE_ANTIALIAS_OFF;
	private Object emfPlusTextAntiAliasingHint = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
	private Object emfPlusInterpolationHint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
	private Object emfPlusAlphaInterpolationHint = RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED;
	private Object emfPlusStrokeControlHint = RenderingHints.VALUE_STROKE_DEFAULT;
	private Composite emfPlusComposite = AlphaComposite.SrcOver;
	private int emfPlusTextContrast = 0;
	private int emfPlusRenderingOriginX = 0;
	private int emfPlusRenderingOriginY = 0;
	private double emfPlusPixelOffsetX = 0.0;
	private double emfPlusPixelOffsetY = 0.0;
	private LinkedList<double[]> emfPlusWorldTransformStack = new LinkedList<double[]>();
	private LinkedList<double[]> emfPlusPageTransformStack = new LinkedList<double[]>();
	private LinkedList<Shape> emfPlusClipStack = new LinkedList<Shape>();
	private LinkedList<EmfPlusRenderingState> emfPlusRenderingStateStack = new LinkedList<EmfPlusRenderingState>();

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

	public void setReplaceSymbolFont(boolean flag) {
		replaceSymbolFont = flag;
	}

	public boolean isReplaceSymbolFont() {
		return replaceSymbolFont;
	}

	private void setParseEmfPlusComments(boolean flag) {
		parseEmfPlusComments = flag;
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
		emfHeaderCanvas = false;
		growCanvas = false;
		initialDpi = dpi;
		canvasWidth = unitsToPixels(vsx, vex, dpi);
		canvasHeight = unitsToPixels(vsy, vey, dpi);
		placeableViewportScaleX = (double) canvasWidth / Math.max(Math.abs(vex - vsx), 1);
		placeableViewportScaleY = (double) canvasHeight / Math.max(Math.abs(vey - vsy), 1);
		initDc();
		dc.setWindowExtEx(Math.abs(vex - vsx), Math.abs(vey - vsy), null);
		setViewportExtEx(Math.abs(vex - vsx), Math.abs(vey - vsy), null);
		dc.setDpi(dpi);
	}

	public void emfHeader(int left, int top, int right, int bottom, int frameLeft, int frameTop, int frameRight,
			int frameBottom) {
		emfHeader(left, top, right, bottom, frameLeft, frameTop, frameRight, frameBottom, 0, 0, 0, 0);
	}

	public void emfHeader(int left, int top, int right, int bottom, int frameLeft, int frameTop, int frameRight,
			int frameBottom, int deviceWidth, int deviceHeight, int millimetersWidth, int millimetersHeight) {
		int width = Math.abs(right - left);
		int height = Math.abs(bottom - top);
		if (replayingPendingEmf || placeableHeader || width == 0 || height == 0 || image != null) {
			return;
		}
		emfHeaderFrameCanvasWidth = emfHeaderFrameCanvasSize(Math.abs(frameRight - frameLeft), deviceWidth,
				millimetersWidth);
		emfHeaderFrameCanvasHeight = emfHeaderFrameCanvasSize(Math.abs(frameBottom - frameTop), deviceHeight,
				millimetersHeight);
		emfHeaderCanvas = true;
		growCanvas = false;
		canvasMinX = left;
		canvasMinY = top;
		canvasWidth = emfHeaderCanvasSize(width, emfHeaderFrameCanvasWidth);
		canvasHeight = emfHeaderCanvasSize(height, emfHeaderFrameCanvasHeight);
		if (frameTop < 0 && emfHeaderFrameCanvasHeight > height) {
			canvasMinY -= emfHeaderFrameCanvasHeight - height;
		}
	}

	private int emfHeaderFrameCanvasSize(int frameSize, int deviceSize, int millimetersSize) {
		if (frameSize <= 0 || deviceSize <= 0 || millimetersSize <= 0) {
			return 0;
		}
		return Math.max(1, (int) Math.round((double) frameSize * deviceSize / (millimetersSize * 100.0)) + 1);
	}

	private int emfHeaderCanvasSize(int boundsSize, int frameCanvasSize) {
		int size = frameCanvasSize > 0 ? Math.max(boundsSize, frameCanvasSize) : boundsSize;
		return Math.min(size, MAX_CANVAS_SIZE);
	}

	private void useEmfPlusHeaderCanvas() {
		if (!emfHeaderCanvas || emfHeaderFrameCanvasWidth <= 0 || emfHeaderFrameCanvasHeight <= 0 || image != null) {
			return;
		}
		canvasMinX = 0;
		canvasMinY = 0;
		canvasWidth = Math.min(emfHeaderFrameCanvasWidth, MAX_CANVAS_SIZE);
		canvasHeight = Math.min(emfHeaderFrameCanvasHeight, MAX_CANVAS_SIZE);
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
		applyCanvasTransform(graphics);
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
		ensureCanvasContains(bounds);
	}

	private void ensureCanvasContains(int x1, int y1, int x2, int y2) {
		if (!growCanvas || image == null) {
			return;
		}
		ensureCanvasContains(
				new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1)));
	}

	private void ensureCanvasContains(Rectangle2D bounds) {
		if (!growCanvas || image == null || bounds == null || bounds.isEmpty()) {
			return;
		}
		contentBounds = contentBounds == null ? (Rectangle2D) bounds.clone() : contentBounds.createUnion(bounds);
		int requiredMinX = Math.min(0, (int) Math.floor(contentBounds.getMinX()));
		int requiredMinY = Math.min(0, (int) Math.floor(contentBounds.getMinY()));
		int requiredMaxX = (int) Math.ceil(contentBounds.getMaxX());
		int requiredMaxY = (int) Math.ceil(contentBounds.getMaxY());
		int requiredWidth = Math.max(canvasWidth, requiredMaxX - requiredMinX);
		int requiredHeight = Math.max(canvasHeight, requiredMaxY - requiredMinY);
		if ((requiredWidth > canvasWidth || requiredHeight > canvasHeight || requiredMinX != canvasMinX
				|| requiredMinY != canvasMinY) && canAllocateCanvas(requiredWidth, requiredHeight)) {
			resizeCanvas(requiredWidth, requiredHeight, requiredMinX, requiredMinY);
		}
	}

	private void resizeCanvas(int width, int height, int minX, int minY) {
		width = Math.max(1, Math.min(width, MAX_CANVAS_SIZE));
		height = Math.max(1, Math.min(height, MAX_CANVAS_SIZE));
		if ((width == canvasWidth && height == canvasHeight && minX == canvasMinX && minY == canvasMinY)
				|| !canAllocateCanvas(width, height)) {
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
				g.drawImage(image, canvasMinX - minX, canvasMinY - minY, null);
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
		canvasMinX = minX;
		canvasMinY = minY;
		applyCanvasTransform(graphics);
		canvasWidth = width;
		canvasHeight = height;
	}

	private boolean canAllocateCanvas(int width, int height) {
		return (long) width * height <= MAX_CANVAS_PIXELS;
	}

	private void configureGraphics(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, emfPlusAntiAliasingHint);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, emfPlusTextAntiAliasingHint);
		g.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, toAwtTextContrast(emfPlusTextContrast));
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, emfPlusInterpolationHint);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, emfPlusAlphaInterpolationHint);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, emfPlusStrokeControlHint);
		g.setComposite(emfPlusComposite);
	}

	private void applyCanvasTransform(Graphics2D g) {
		if (canvasMinX != 0 || canvasMinY != 0) {
			g.translate(-canvasMinX, -canvasMinY);
		}
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
		Arc2D arc = createArc(x - radius, y - radius, x + radius, y + radius, start.x, start.y, end.x, end.y,
				Arc2D.OPEN);
		if (arc != null) {
			if (currentPath != null) {
				currentPath.append(arc, true);
			} else {
				strokeShape(arc, dc.getPen());
			}
		}
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
		maskBlt(image, dx, dy, dw, dh, sx, sy, mask, mx, my, rop, null);
	}

	public void maskBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, byte[] mask, int mx, int my,
			long rop, Integer sourceBackgroundColor) {
		if (mask != null) {
			drawMaskedBitmapRop(image, dx, dy, dw, dh, sx, sy, dw, dh, mask, mx, my, rop, sourceBackgroundColor);
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
		return new AwtPalette(version, palEntry);
	}

	public GdiPatternBrush createPatternBrush(byte[] image) {
		if (isMonochromeWmfBitmap(image) && lastDibPatternBrushImage != null) {
			return new AwtPatternBrush(lastDibPatternBrushImage, lastDibPatternBrushUsage);
		}
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
		return colorSpace instanceof AwtColorSpace;
	}

	public void dibBitBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop) {
		if (image == null || image.length == 0) {
			if (canPatBltWithoutSource(rop)) {
				patBlt(dx, dy, dw, dh, rop);
			}
			return;
		}
		drawBitmap(image, dx, dy, dw, dh, sx, sy, dw, dh, Gdi.DIB_RGB_COLORS, Long.valueOf(rop), false);
	}

	public GdiPatternBrush dibCreatePatternBrush(byte[] image, int usage) {
		lastDibPatternBrushImage = image != null ? image.clone() : null;
		lastDibPatternBrushUsage = usage;
		return new AwtPatternBrush(image, usage);
	}

	public void dibStretchBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, long rop) {
		stretchDIBits(dx, dy, dw, dh, sx, sy, sw, sh, image, Gdi.DIB_RGB_COLORS, rop);
	}

	public void ellipse(int sx, int sy, int ex, int ey) {
		Rectangle2D rect = toRectangle(sx, sy, ex - sx, ey - sy);
		Shape ellipse = new Ellipse2D.Double(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
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
		if (parseEmfPlusComments && EmfPlusParser.isEmfPlusComment(data)) {
			useEmfPlusHeaderCanvas();
			if (emfPlusGetDCActive) {
				endEmfPlusGetDCMode();
			} else {
				suppressEmfPlusFallback = false;
			}
			emfPlusParser.parse(data, new EmfPlusParser.Handler() {
				public void handleEmfPlusRecord(int type, int flags, byte[] payload, boolean continuableObject,
						int totalObjectSize) {
					AwtGdi.this.handleEmfPlusRecord(type, flags, payload, continuableObject, totalObjectSize);
				}
			});
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
		return getClipRegionType();
	}

	public void extFloodFill(int x, int y, int color, int type) {
		floodFill(x, y, color, type == 1);
	}

	public GdiRegion extCreateRegion(float[] xform, int count, byte[] rgnData) {
		Area area = createRegionArea(rgnData, count, xform);
		return area != null ? new AwtRegion(area) : null;
	}

	public int extSelectClipRgn(GdiRegion rgn, int mode) {
		ensureGraphics();
		applyClip(rgn instanceof AwtRegion ? ((AwtRegion) rgn).shape : null, mode);
		return getClipRegionType();
	}

	public void extTextOut(int x, int y, int options, int[] rect, byte[] text, int[] lpdx) {
		ensureGraphics();
		if (rect != null && (options & Gdi.ETO_OPAQUE) != 0) {
			Shape background = toRectangle(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]);
			ensureCanvasContains(background);
			Paint old = graphics.getPaint();
			graphics.setPaint(toColor(dc.getBkColor()));
			graphics.fill(background);
			graphics.setPaint(old);
		}
		Shape oldClip = null;
		boolean clipped = false;
		if (rect != null && (options & Gdi.ETO_CLIPPED) != 0) {
			oldClip = graphics.getClip();
			graphics.clip(toRectangle(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]));
			clipped = true;
		}
		try {
			drawText(x, y, GdiUtils.convertString(text, dc.getFont() != null ? dc.getFont().getCharset() : 0), lpdx,
					options);
		} finally {
			if (clipped) {
				graphics.setClip(oldClip);
			}
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
		floodFill(x, y, color, false);
	}

	private void floodFill(int x, int y, int color, boolean surfaceFill) {
		ensureGraphics();
		int seedX = ixi(x);
		int seedY = iyi(y);
		if (!isCanvasPixel(seedX, seedY) || !isInClip(seedX, seedY)) {
			return;
		}
		int targetRgb = toColor(color).getRGB() & 0x00FFFFFF;
		boolean seedMatches = isOpaqueColorMatch(seedX, seedY, targetRgb);
		if (surfaceFill && !seedMatches || !surfaceFill && seedMatches) {
			return;
		}

		boolean[] visited = new boolean[canvasWidth * canvasHeight];
		ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
		queue.add(Integer.valueOf(seedY * canvasWidth + seedX));
		while (!queue.isEmpty()) {
			int index = queue.removeFirst().intValue();
			if (visited[index]) {
				continue;
			}
			visited[index] = true;
			int px = index % canvasWidth;
			int py = index / canvasWidth;
			if (!isInClip(px, py) || !isFloodFillTarget(px, py, targetRgb, surfaceFill)) {
				continue;
			}
			fillDevicePixel(px, py, dc.getBrush());
			addFloodFillNeighbor(queue, visited, px - 1, py);
			addFloodFillNeighbor(queue, visited, px + 1, py);
			addFloodFillNeighbor(queue, visited, px, py - 1);
			addFloodFillNeighbor(queue, visited, px, py + 1);
		}
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
		if (dc.getCurrentX() == ex && dc.getCurrentY() == ey) {
			return;
		}
		if (currentPath != null) {
			currentPath.lineTo(ix(ex), iy(ey));
			dc.moveToEx(ex, ey, null);
			return;
		}
		Shape line = new java.awt.geom.Line2D.Double(ix(dc.getCurrentX()), iy(dc.getCurrentY()), ix(ex), iy(ey));
		strokeShape(line, dc.getPen());
		dc.moveToEx(ex, ey, null);
	}

	public void moveToEx(int x, int y, Point old) {
		if (currentPath != null) {
			currentPath.moveTo(ix(x), iy(y));
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
		if (placeableHeader && !replayingPendingEmf) {
			x = placeableViewportX(x);
			y = placeableViewportY(y);
		}
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
		ensureCanvasContains(rect);
		int[] bounds = toDeviceBounds(rect);
		int left = bounds[0];
		int top = bounds[1];
		int right = bounds[2];
		int bottom = bounds[3];
		if (left >= right || top >= bottom) {
			return;
		}

		int rop3 = getBitmapRop3(rop);
		boolean needsPattern = usesPattern(rop3);
		if (needsPattern && isNullBrush(dc.getBrush())) {
			return;
		}
		BufferedImage pattern = needsPattern
				? createPatternImage(toLogicalXi(left), toLogicalYi(top), right - left, bottom - top)
				: null;
		for (int py = top; py < bottom; py++) {
			for (int px = left; px < right; px++) {
				if (!isDeviceInClip(px, py)) {
					continue;
				}
				int p = pattern != null ? pattern.getRGB(px - left, py - top) & 0x00FFFFFF : 0;
				int d = image.getRGB(px, py) & 0x00FFFFFF;
				image.setRGB(px, py, 0xFF000000 | applyBitmapRop(0, d, p, rop3));
			}
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

	private void handleEmfPlusRecord(int type, int flags, byte[] payload, boolean continuableObject,
			int totalObjectSize) {
		if (emfPlusGetDCActive && type != EMF_PLUS_GET_DC) {
			endEmfPlusGetDCMode();
		}
		if (type == EMF_PLUS_GET_DC) {
			emfPlusGetDCActive = true;
			suppressEmfPlusFallback = false;
		} else if (type == EMF_PLUS_OBJECT) {
			handleEmfPlusObjectRecord(flags, payload, continuableObject, totalObjectSize);
		} else if (type == EMF_PLUS_CLEAR) {
			handleEmfPlusClear(payload);
		} else if (type == EMF_PLUS_FILL_RECTS) {
			handleEmfPlusRects(flags, payload, true);
		} else if (type == EMF_PLUS_DRAW_RECTS) {
			handleEmfPlusRects(flags, payload, false);
		} else if (type == EMF_PLUS_FILL_POLYGON) {
			handleEmfPlusPolygon(flags, payload);
		} else if (type == EMF_PLUS_DRAW_LINES) {
			handleEmfPlusDrawLines(flags, payload);
		} else if (type == EMF_PLUS_FILL_ELLIPSE) {
			handleEmfPlusEllipse(flags, payload, true);
		} else if (type == EMF_PLUS_DRAW_ELLIPSE) {
			handleEmfPlusEllipse(flags, payload, false);
		} else if (type == EMF_PLUS_FILL_PIE) {
			handleEmfPlusPie(flags, payload, true);
		} else if (type == EMF_PLUS_DRAW_PIE) {
			handleEmfPlusPie(flags, payload, false);
		} else if (type == EMF_PLUS_DRAW_ARC) {
			handleEmfPlusDrawArc(flags, payload);
		} else if (type == EMF_PLUS_FILL_PATH) {
			handleEmfPlusFillPath(flags, payload);
		} else if (type == EMF_PLUS_DRAW_PATH) {
			handleEmfPlusDrawPath(flags, payload);
		} else if (type == EMF_PLUS_FILL_REGION) {
			handleEmfPlusFillRegion(flags, payload);
		} else if (type == EMF_PLUS_STROKE_FILL_PATH) {
			handleEmfPlusStrokeFillPath(flags, payload);
		} else if (type == EMF_PLUS_DRAW_BEZIERS) {
			handleEmfPlusDrawBeziers(flags, payload);
		} else if (type == EMF_PLUS_FILL_CLOSED_CURVE) {
			handleEmfPlusClosedCurve(flags, payload, true);
		} else if (type == EMF_PLUS_DRAW_CLOSED_CURVE) {
			handleEmfPlusClosedCurve(flags, payload, false);
		} else if (type == EMF_PLUS_DRAW_CURVE) {
			handleEmfPlusDrawCurve(flags, payload);
		} else if (type == EMF_PLUS_DRAW_IMAGE) {
			handleEmfPlusDrawImage(flags, payload);
		} else if (type == EMF_PLUS_DRAW_IMAGE_POINTS) {
			handleEmfPlusDrawImagePoints(flags, payload);
		} else if (type == EMF_PLUS_DRAW_STRING) {
			handleEmfPlusDrawString(flags, payload);
		} else if (type == EMF_PLUS_DRAW_DRIVER_STRING) {
			handleEmfPlusDrawDriverString(flags, payload);
		} else if (type == EMF_PLUS_SERIALIZABLE_OBJECT) {
			pendingEmfPlusImageEffect = readEmfPlusSerializableObject(payload);
		} else if (type == EMF_PLUS_SET_RENDERING_ORIGIN) {
			if (payload.length >= 8) {
				emfPlusRenderingOriginX = readInt32(payload, 0);
				emfPlusRenderingOriginY = readInt32(payload, 4);
			}
		} else if (type == EMF_PLUS_SET_ANTI_ALIAS_MODE) {
			setEmfPlusAntiAliasMode(flags);
		} else if (type == EMF_PLUS_SET_TEXT_RENDERING_HINT) {
			setEmfPlusTextRenderingHint(flags);
		} else if (type == EMF_PLUS_SET_TEXT_CONTRAST) {
			setEmfPlusTextContrast(flags);
		} else if (type == EMF_PLUS_SET_INTERPOLATION_MODE) {
			setEmfPlusInterpolationMode(flags);
		} else if (type == EMF_PLUS_SET_PIXEL_OFFSET_MODE) {
			setEmfPlusPixelOffsetMode(flags);
		} else if (type == EMF_PLUS_SET_COMPOSITING_MODE) {
			setEmfPlusCompositingMode(flags);
		} else if (type == EMF_PLUS_SET_COMPOSITING_QUALITY) {
			setEmfPlusCompositingQuality(flags);
		} else if (type == EMF_PLUS_SAVE || type == EMF_PLUS_BEGIN_CONTAINER
				|| type == EMF_PLUS_BEGIN_CONTAINER_NO_PARAMS) {
			saveEmfPlusState();
			if (type == EMF_PLUS_BEGIN_CONTAINER) {
				beginEmfPlusContainer(flags, payload);
			}
		} else if (type == EMF_PLUS_RESTORE || type == EMF_PLUS_END_CONTAINER) {
			restoreEmfPlusState();
		} else if (type == EMF_PLUS_SET_WORLD_TRANSFORM) {
			setEmfPlusWorldTransform(payload);
		} else if (type == EMF_PLUS_RESET_WORLD_TRANSFORM) {
			emfPlusWorldTransform = new double[]{1, 0, 0, 1, 0, 0};
		} else if (type == EMF_PLUS_MULTIPLY_WORLD_TRANSFORM) {
			multiplyEmfPlusWorldTransform(flags, payload);
		} else if (type == EMF_PLUS_TRANSLATE_WORLD_TRANSFORM) {
			translateEmfPlusWorldTransform(flags, payload);
		} else if (type == EMF_PLUS_SCALE_WORLD_TRANSFORM) {
			scaleEmfPlusWorldTransform(flags, payload);
		} else if (type == EMF_PLUS_ROTATE_WORLD_TRANSFORM) {
			rotateEmfPlusWorldTransform(flags, payload);
		} else if (type == EMF_PLUS_SET_PAGE_TRANSFORM) {
			setEmfPlusPageTransform(flags, payload);
		} else if (type == EMF_PLUS_RESET_CLIP) {
			resetEmfPlusClip();
		} else if (type == EMF_PLUS_SET_CLIP_RECT) {
			handleEmfPlusSetClipRect(flags, payload);
		} else if (type == EMF_PLUS_SET_CLIP_PATH) {
			handleEmfPlusSetClipPath(flags);
		} else if (type == EMF_PLUS_SET_CLIP_REGION) {
			handleEmfPlusSetClipRegion(flags);
		} else if (type == EMF_PLUS_OFFSET_CLIP) {
			offsetEmfPlusClip(payload);
		} else if (type == EMF_PLUS_SET_TS_GRAPHICS) {
			setEmfPlusTSGraphics(payload);
		} else if (type == EMF_PLUS_SET_TS_CLIP) {
			handleEmfPlusSetTSClip(flags, payload);
		}
	}

	private void handleEmfPlusObjectRecord(int flags, byte[] payload, boolean continuable, int totalObjectSize) {
		int objectId = flags & 0xFF;
		int objectType = (flags >>> 8) & 0x7F;
		PendingEmfPlusObject pending = pendingEmfPlusObjects.get(Integer.valueOf(objectId));
		if (pending == null && !continuable) {
			handleEmfPlusObject(flags, payload);
			return;
		}

		if (pending == null || pending.objectType != objectType) {
			pending = new PendingEmfPlusObject(objectType, totalObjectSize);
			pendingEmfPlusObjects.put(Integer.valueOf(objectId), pending);
		} else if (continuable && totalObjectSize > 0) {
			pending.totalObjectSize = totalObjectSize;
		}

		pending.write(payload);
		if (!continuable || (pending.totalObjectSize > 0 && pending.size() >= pending.totalObjectSize)) {
			pendingEmfPlusObjects.remove(Integer.valueOf(objectId));
			handleEmfPlusObject((objectType << 8) | objectId, pending.toByteArray());
		}
	}

	private void endEmfPlusGetDCMode() {
		emfPlusGetDCActive = false;
		suppressEmfPlusFallback = false;
	}

	private void saveEmfPlusState() {
		emfPlusWorldTransformStack.addFirst(emfPlusWorldTransform.clone());
		emfPlusPageTransformStack.addFirst(new double[]{emfPlusPageScale, emfPlusPageUnitScale});
		emfPlusClipStack.addFirst(graphics != null ? graphics.getClip() : null);
		emfPlusRenderingStateStack.addFirst(new EmfPlusRenderingState(emfPlusAntiAliasingHint,
				emfPlusTextAntiAliasingHint, emfPlusInterpolationHint, emfPlusAlphaInterpolationHint,
				emfPlusStrokeControlHint, emfPlusComposite, emfPlusTextContrast, emfPlusRenderingOriginX,
				emfPlusRenderingOriginY, emfPlusPixelOffsetX, emfPlusPixelOffsetY));
	}

	private void restoreEmfPlusState() {
		if (!emfPlusWorldTransformStack.isEmpty()) {
			emfPlusWorldTransform = emfPlusWorldTransformStack.removeFirst();
		}
		if (!emfPlusPageTransformStack.isEmpty()) {
			double[] pageTransform = emfPlusPageTransformStack.removeFirst();
			emfPlusPageScale = pageTransform[0];
			emfPlusPageUnitScale = pageTransform[1];
		}
		if (!emfPlusClipStack.isEmpty()) {
			Shape clip = emfPlusClipStack.removeFirst();
			if (graphics != null) {
				graphics.setClip(clip);
			}
		}
		if (!emfPlusRenderingStateStack.isEmpty()) {
			EmfPlusRenderingState state = emfPlusRenderingStateStack.removeFirst();
			emfPlusAntiAliasingHint = state.antiAliasingHint;
			emfPlusTextAntiAliasingHint = state.textAntiAliasingHint;
			emfPlusInterpolationHint = state.interpolationHint;
			emfPlusAlphaInterpolationHint = state.alphaInterpolationHint;
			emfPlusStrokeControlHint = state.strokeControlHint;
			emfPlusComposite = state.composite;
			emfPlusTextContrast = state.textContrast;
			emfPlusRenderingOriginX = state.renderingOriginX;
			emfPlusRenderingOriginY = state.renderingOriginY;
			emfPlusPixelOffsetX = state.pixelOffsetX;
			emfPlusPixelOffsetY = state.pixelOffsetY;
			if (graphics != null) {
				configureGraphics(graphics);
			}
		}
	}

	private void handleEmfPlusObject(int flags, byte[] payload) {
		int objectId = flags & 0xFF;
		int objectType = (flags >>> 8) & 0x7F;
		if (objectType == EMF_PLUS_OBJECT_TYPE_BRUSH) {
			EmfPlusBrush brush = readEmfPlusBrush(payload, 0);
			if (brush != null) {
				emfPlusBrushes.put(Integer.valueOf(objectId), brush);
			}
			return;
		}
		if (objectType == EMF_PLUS_OBJECT_TYPE_PEN) {
			EmfPlusPen pen = readEmfPlusPen(payload);
			if (pen != null) {
				emfPlusPens.put(Integer.valueOf(objectId), pen);
			}
			return;
		}
		if (objectType == EMF_PLUS_OBJECT_TYPE_PATH) {
			EmfPlusPath path = readEmfPlusPath(payload);
			if (path != null) {
				emfPlusPaths.put(Integer.valueOf(objectId), path);
			}
			return;
		}
		if (objectType == EMF_PLUS_OBJECT_TYPE_REGION) {
			EmfPlusRegion region = readEmfPlusRegion(payload);
			if (region != null) {
				emfPlusRegions.put(Integer.valueOf(objectId), region);
			}
			return;
		}
		if (objectType == EMF_PLUS_OBJECT_TYPE_FONT) {
			EmfPlusFont font = readEmfPlusFont(payload);
			if (font != null) {
				emfPlusFonts.put(Integer.valueOf(objectId), font);
			}
			return;
		}
		if (objectType == EMF_PLUS_OBJECT_TYPE_STRING_FORMAT) {
			EmfPlusStringFormat format = readEmfPlusStringFormat(payload);
			if (format != null) {
				emfPlusStringFormats.put(Integer.valueOf(objectId), format);
			}
			return;
		}
		if (objectType == EMF_PLUS_OBJECT_TYPE_IMAGE_ATTRIBUTES) {
			EmfPlusImageAttributes attributes = readEmfPlusImageAttributes(payload);
			if (attributes != null) {
				emfPlusImageAttributes.put(Integer.valueOf(objectId), attributes);
			}
			return;
		}
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
			return;
		}
		if (imageDataType == EMF_PLUS_IMAGE_DATA_TYPE_BITMAP) {
			BufferedImage bitmap = readEmfPlusBitmapImage(payload, 8);
			if (bitmap != null) {
				emfPlusBitmapImages.put(Integer.valueOf(objectId), bitmap);
			}
		}
	}

	private void handleEmfPlusClear(byte[] payload) {
		if (payload.length < 4) {
			return;
		}
		Paint paint = toEmfPlusPaint(new EmfPlusBrush(readInt32(payload, 0)));
		ensureGraphics();
		Paint oldPaint = graphics.getPaint();
		graphics.setPaint(paint);
		graphics.fillRect(0, 0, canvasWidth, canvasHeight);
		graphics.setPaint(oldPaint);
		suppressEmfPlusFallback = true;
	}

	private void handleEmfPlusRects(int flags, byte[] payload, boolean fill) {
		int dataOffset = fill ? 8 : 4;
		if (payload.length < dataOffset) {
			return;
		}
		EmfPlusBrush brush = fill ? getEmfPlusBrush(flags, readInt32(payload, 0)) : null;
		EmfPlusPen pen = fill ? null : emfPlusPens.get(Integer.valueOf(flags & 0xFF));
		if (fill && brush == null || !fill && pen == null) {
			return;
		}
		int count = readInt32(payload, dataOffset - 4);
		double[][] rects = readEmfPlusRects(payload, dataOffset, count, isEmfPlusCompressed(flags));
		if (rects == null) {
			return;
		}
		for (int i = 0; i < rects.length; i++) {
			Shape shape = createEmfPlusRect(rects[i]);
			if (fill) {
				fillEmfPlusShape(shape, brush);
			} else {
				strokeEmfPlusShape(shape, pen);
			}
		}
	}

	private void handleEmfPlusEllipse(int flags, byte[] payload, boolean fill) {
		int dataOffset = fill ? 4 : 0;
		if (payload.length < dataOffset) {
			return;
		}
		EmfPlusBrush brush = fill ? getEmfPlusBrush(flags, readInt32(payload, 0)) : null;
		EmfPlusPen pen = fill ? null : emfPlusPens.get(Integer.valueOf(flags & 0xFF));
		if (fill && brush == null || !fill && pen == null) {
			return;
		}
		double[][] rects = readEmfPlusRects(payload, dataOffset, 1, isEmfPlusCompressed(flags));
		if (rects == null) {
			return;
		}
		Shape shape = createEmfPlusEllipse(rects[0]);
		if (fill) {
			fillEmfPlusShape(shape, brush);
		} else {
			strokeEmfPlusShape(shape, pen);
		}
	}

	private void handleEmfPlusPolygon(int flags, byte[] payload) {
		if (payload.length < 8) {
			return;
		}
		EmfPlusBrush brush = getEmfPlusBrush(flags, readInt32(payload, 0));
		int count = readInt32(payload, 4);
		double[][] points = readEmfPlusDrawingPoints(payload, 8, count, flags);
		if (brush == null || points == null || points.length < 3) {
			return;
		}
		fillEmfPlusShape(createEmfPlusPolyline(points, true, toEmfPlusFillWindingRule(flags)), brush);
	}

	private void handleEmfPlusDrawLines(int flags, byte[] payload) {
		EmfPlusPen pen = emfPlusPens.get(Integer.valueOf(flags & 0xFF));
		if (pen == null || payload.length < 4) {
			return;
		}
		int count = readInt32(payload, 0);
		double[][] points = readEmfPlusDrawingPoints(payload, 4, count, flags);
		if (points == null || points.length < 2) {
			return;
		}
		strokeEmfPlusPolyline(points, (flags & EMF_PLUS_FLAG_CLOSE) != 0, pen);
	}

	private void handleEmfPlusFillPath(int flags, byte[] payload) {
		if (payload.length < 4) {
			return;
		}
		EmfPlusBrush brush = getEmfPlusBrush(flags, readInt32(payload, 0));
		EmfPlusPath path = emfPlusPaths.get(Integer.valueOf(flags & 0xFF));
		if (brush == null || path == null) {
			return;
		}
		Shape shape = createEmfPlusPath(path);
		if (shape != null) {
			fillEmfPlusShape(shape, brush);
		}
	}

	private void handleEmfPlusDrawPath(int flags, byte[] payload) {
		if (payload.length < 4) {
			return;
		}
		EmfPlusPen pen = emfPlusPens.get(Integer.valueOf(readInt32(payload, 0) & 0xFF));
		EmfPlusPath path = emfPlusPaths.get(Integer.valueOf(flags & 0xFF));
		if (pen == null || path == null) {
			return;
		}
		Shape shape = createEmfPlusPath(path);
		if (shape != null) {
			strokeEmfPlusShape(shape, pen);
		}
	}

	private void handleEmfPlusFillRegion(int flags, byte[] payload) {
		if (payload.length < 4) {
			return;
		}
		EmfPlusBrush brush = getEmfPlusBrush(flags, readInt32(payload, 0));
		EmfPlusRegion region = emfPlusRegions.get(Integer.valueOf(flags & 0xFF));
		Area area = createEmfPlusRegionArea(region);
		if (brush != null && area != null && !area.isEmpty()) {
			fillEmfPlusShape(area, brush);
		}
	}

	private void handleEmfPlusStrokeFillPath(int flags, byte[] payload) {
		if (payload.length < 8) {
			return;
		}
		EmfPlusPen pen = emfPlusPens.get(Integer.valueOf(readInt32(payload, 0) & 0xFF));
		EmfPlusBrush brush = getEmfPlusBrush(flags, readInt32(payload, 4));
		EmfPlusPath path = emfPlusPaths.get(Integer.valueOf(flags & 0xFF));
		Shape shape = path != null ? createEmfPlusPath(path) : null;
		if (shape != null && brush != null) {
			fillEmfPlusShape(shape, brush);
		}
		if (shape != null && pen != null) {
			strokeEmfPlusShape(shape, pen);
		}
	}

	private void handleEmfPlusPie(int flags, byte[] payload, boolean fill) {
		int dataOffset = fill ? 12 : 8;
		if (payload.length < dataOffset) {
			return;
		}
		EmfPlusBrush brush = fill ? getEmfPlusBrush(flags, readInt32(payload, 0)) : null;
		EmfPlusPen pen = fill ? null : emfPlusPens.get(Integer.valueOf(flags & 0xFF));
		if (fill && brush == null || !fill && pen == null) {
			return;
		}
		float startAngle = readFloat(payload, fill ? 4 : 0);
		float sweepAngle = readFloat(payload, fill ? 8 : 4);
		double[][] rects = readEmfPlusRects(payload, dataOffset, 1, isEmfPlusCompressed(flags));
		Shape shape = rects != null ? createEmfPlusArc(rects[0], startAngle, sweepAngle, Arc2D.PIE) : null;
		if (shape == null) {
			return;
		}
		if (fill) {
			fillEmfPlusShape(shape, brush);
		} else {
			strokeEmfPlusShape(shape, pen);
		}
	}

	private void handleEmfPlusDrawArc(int flags, byte[] payload) {
		EmfPlusPen pen = emfPlusPens.get(Integer.valueOf(flags & 0xFF));
		if (pen == null || payload.length < 8) {
			return;
		}
		float startAngle = readFloat(payload, 0);
		float sweepAngle = readFloat(payload, 4);
		double[][] rects = readEmfPlusRects(payload, 8, 1, isEmfPlusCompressed(flags));
		Shape shape = rects != null ? createEmfPlusArc(rects[0], startAngle, sweepAngle, Arc2D.OPEN) : null;
		if (shape != null) {
			strokeEmfPlusShape(shape, pen);
		}
	}

	private void handleEmfPlusDrawBeziers(int flags, byte[] payload) {
		EmfPlusPen pen = emfPlusPens.get(Integer.valueOf(flags & 0xFF));
		if (pen == null || payload.length < 4) {
			return;
		}
		int count = readInt32(payload, 0);
		double[][] points = readEmfPlusDrawingPoints(payload, 4, count, flags);
		Path2D.Double path = points != null ? createEmfPlusBezierPath(points) : null;
		if (path != null) {
			strokeEmfPlusShape(path, pen);
		}
	}

	private void handleEmfPlusClosedCurve(int flags, byte[] payload, boolean fill) {
		int dataOffset = fill ? 12 : 8;
		if (payload.length < dataOffset) {
			return;
		}
		EmfPlusBrush brush = fill ? getEmfPlusBrush(flags, readInt32(payload, 0)) : null;
		EmfPlusPen pen = fill ? null : emfPlusPens.get(Integer.valueOf(flags & 0xFF));
		if (fill && brush == null || !fill && pen == null) {
			return;
		}
		float tension = readFloat(payload, fill ? 4 : 0);
		int count = readInt32(payload, fill ? 8 : 4);
		double[][] points = readEmfPlusDrawingPoints(payload, dataOffset, count, flags);
		Path2D.Double path = points != null && points.length >= 3
				? createEmfPlusClosedCurvePath(points, tension,
						fill ? toEmfPlusFillWindingRule(flags) : Path2D.WIND_NON_ZERO)
				: null;
		if (path == null) {
			return;
		}
		if (fill) {
			fillEmfPlusShape(path, brush);
		} else {
			strokeEmfPlusShape(path, pen);
		}
	}

	private void handleEmfPlusDrawCurve(int flags, byte[] payload) {
		EmfPlusPen pen = emfPlusPens.get(Integer.valueOf(flags & 0xFF));
		if (pen == null || payload.length < 16) {
			return;
		}
		float tension = readFloat(payload, 0);
		int offset = readInt32(payload, 4);
		int numberOfSegments = readInt32(payload, 8);
		int count = readInt32(payload, 12);
		double[][] points = readEmfPlusDrawingPoints(payload, 16, count, flags);
		if (points == null || points.length < 2 || offset < 0 || numberOfSegments < 1
				|| offset + numberOfSegments >= points.length) {
			return;
		}
		Path2D.Double path = createEmfPlusCurvePath(points, offset, numberOfSegments, tension);
		if (path != null) {
			strokeEmfPlusShape(path, pen);
		}
	}

	private void handleEmfPlusDrawString(int flags, byte[] payload) {
		if (payload.length < 28) {
			return;
		}
		EmfPlusFont emfPlusFont = emfPlusFonts.get(Integer.valueOf(flags & 0xFF));
		EmfPlusBrush brush = getEmfPlusBrush(flags, readInt32(payload, 0));
		if (emfPlusFont == null || brush == null) {
			return;
		}
		EmfPlusStringFormat format = emfPlusStringFormats.get(Integer.valueOf(readInt32(payload, 4) & 0xFF));
		int length = readInt32(payload, 8);
		double[][] rects = readEmfPlusRects(payload, 12, 1, false);
		String text = readUtf16Le(payload, 28, length);
		Paint paint = toEmfPlusPaint(brush);
		if (rects == null || text == null || paint == null) {
			return;
		}

		EmfPlusText textRun = createEmfPlusTextRun(text, format, emfPlusFont);
		double[] p = toEmfPlusLogicalPoint(rects[0][0], rects[0][1]);
		Font font = createEmfPlusFont(emfPlusFont);
		ensureGraphics();
		Font oldFont = graphics.getFont();
		Paint oldPaint = graphics.getPaint();
		graphics.setFont(font);
		graphics.setPaint(paint);
		FontMetrics metrics = graphics.getFontMetrics(font);
		float x = (float) toEmfPlusStringX(p[0], rects[0][2], textRun.text, metrics, format);
		float y = (float) toEmfPlusStringY(p[1], rects[0][3], metrics, format);
		boolean noClip = format != null && (format.flags & EMF_PLUS_STRING_FORMAT_NO_CLIP) != 0;
		boolean vertical = format != null && (format.flags & EMF_PLUS_STRING_FORMAT_DIRECTION_VERTICAL) != 0;
		Shape layoutBounds = createEmfPlusRect(rects[0]);
		ensureCanvasContains(noClip ? createEmfPlusTextBounds(x, y, textRun.text, metrics, vertical) : layoutBounds);
		Shape oldClip = graphics.getClip();
		if (!noClip) {
			graphics.clip(layoutBounds);
		}
		if (vertical) {
			AffineTransform oldTransform = graphics.getTransform();
			graphics.rotate(Math.PI / 2.0, x, y);
			graphics.drawString(textRun.attributed.getIterator(), x, y);
			graphics.setTransform(oldTransform);
		} else {
			graphics.drawString(textRun.attributed.getIterator(), x, y);
		}
		graphics.setClip(oldClip);
		graphics.setPaint(oldPaint);
		graphics.setFont(oldFont);
		suppressEmfPlusFallback = true;
	}

	private void handleEmfPlusDrawDriverString(int flags, byte[] payload) {
		if (payload.length < 16) {
			return;
		}
		EmfPlusFont emfPlusFont = emfPlusFonts.get(Integer.valueOf(flags & 0xFF));
		EmfPlusBrush brush = getEmfPlusBrush(flags, readInt32(payload, 0));
		Paint paint = toEmfPlusPaint(brush);
		if (emfPlusFont == null || paint == null) {
			return;
		}

		int options = readInt32(payload, 4);
		int matrixPresent = readInt32(payload, 8);
		int glyphCount = readInt32(payload, 12);
		if (glyphCount <= 0) {
			return;
		}
		boolean cmapLookup = (options & EMF_PLUS_DRIVER_STRING_CMAP_LOOKUP) != 0;
		String text = cmapLookup ? readUtf16Le(payload, 16, glyphCount) : null;
		int[] glyphCodes = cmapLookup ? null : readEmfPlusGlyphCodes(payload, 16, glyphCount);
		if (cmapLookup && text == null || !cmapLookup && glyphCodes == null) {
			return;
		}

		int glyphPosOffset = align4(16 + glyphCount * 2);
		int positionCount = (options & EMF_PLUS_DRIVER_STRING_REALIZED_ADVANCE) != 0 ? 1 : glyphCount;
		double[][] points = readEmfPlusDrawingPoints(payload, glyphPosOffset, positionCount, false);
		if (points == null) {
			return;
		}

		double[] matrix = null;
		int matrixOffset = glyphPosOffset + positionCount * 8;
		if (matrixPresent != 0) {
			if (payload.length < matrixOffset + 24) {
				return;
			}
			matrix = new double[]{readFloat(payload, matrixOffset), readFloat(payload, matrixOffset + 4),
					readFloat(payload, matrixOffset + 8), readFloat(payload, matrixOffset + 12),
					readFloat(payload, matrixOffset + 16), readFloat(payload, matrixOffset + 20)};
		}

		Font font = createEmfPlusFont(emfPlusFont);
		ensureGraphics();
		Font oldFont = graphics.getFont();
		Paint oldPaint = graphics.getPaint();
		graphics.setFont(font);
		graphics.setPaint(paint);
		FontMetrics metrics = graphics.getFontMetrics(font);
		boolean transformed = matrix != null;
		if ((options & EMF_PLUS_DRIVER_STRING_REALIZED_ADVANCE) != 0) {
			double[] point = transformed ? points[0] : toEmfPlusDriverStringPoint(points[0], null);
			if ((options & EMF_PLUS_DRIVER_STRING_VERTICAL) != 0) {
				ensureCanvasContains(createEmfPlusDriverStringBounds(point[0], point[1], Math.max(1, font.getSize2D()),
						Math.max(1, glyphCount * metrics.getHeight()), matrix));
				graphics.setFont(font);
				graphics.setPaint(paint);
				AffineTransform oldTransform = applyEmfPlusDriverStringTransform(matrix);
				for (int i = 0; i < glyphCount; i++) {
					drawEmfPlusDriverGlyph(font, text, glyphCodes, i, point[0],
							point[1] + metrics.getAscent() + i * metrics.getHeight());
				}
				restoreEmfPlusDriverStringTransform(oldTransform);
			} else {
				ensureCanvasContains(createEmfPlusDriverStringBounds(point[0], point[1],
						Math.max(1, glyphCount * font.getSize2D()), Math.max(1, font.getSize2D()), matrix));
				graphics.setFont(font);
				graphics.setPaint(paint);
				AffineTransform oldTransform = applyEmfPlusDriverStringTransform(matrix);
				if (cmapLookup) {
					graphics.drawString(text, (float) point[0], (float) (point[1] + font.getSize2D()));
				} else {
					GlyphVector glyphs = font.createGlyphVector(graphics.getFontRenderContext(), glyphCodes);
					graphics.drawGlyphVector(glyphs, (float) point[0], (float) (point[1] + font.getSize2D()));
				}
				restoreEmfPlusDriverStringTransform(oldTransform);
			}
		} else {
			for (int i = 0; i < glyphCount; i++) {
				double[] point = transformed ? points[i] : toEmfPlusDriverStringPoint(points[i], null);
				ensureCanvasContains(createEmfPlusDriverStringBounds(point[0], point[1], Math.max(1, font.getSize2D()),
						Math.max(1, font.getSize2D()), matrix));
			}
			graphics.setFont(font);
			graphics.setPaint(paint);
			AffineTransform oldTransform = applyEmfPlusDriverStringTransform(matrix);
			for (int i = 0; i < glyphCount; i++) {
				double[] point = transformed ? points[i] : toEmfPlusDriverStringPoint(points[i], null);
				drawEmfPlusDriverGlyph(font, text, glyphCodes, i, point[0], point[1] + metrics.getAscent());
			}
			restoreEmfPlusDriverStringTransform(oldTransform);
		}
		graphics.setPaint(oldPaint);
		graphics.setFont(oldFont);
		suppressEmfPlusFallback = true;
	}

	private AffineTransform applyEmfPlusDriverStringTransform(double[] matrix) {
		if (matrix == null) {
			return null;
		}
		AffineTransform oldTransform = graphics.getTransform();
		graphics.transform(createEmfPlusDriverStringTransform(matrix));
		return oldTransform;
	}

	private void restoreEmfPlusDriverStringTransform(AffineTransform oldTransform) {
		if (oldTransform != null) {
			graphics.setTransform(oldTransform);
		}
	}

	private void drawEmfPlusDriverGlyph(Font font, String text, int[] glyphCodes, int index, double x, double y) {
		if (text != null) {
			graphics.drawString(text.substring(index, index + 1), (float) x, (float) y);
			return;
		}
		GlyphVector glyph = font.createGlyphVector(graphics.getFontRenderContext(), new int[]{glyphCodes[index]});
		graphics.drawGlyphVector(glyph, (float) x, (float) y);
	}

	private void setEmfPlusAntiAliasMode(int flags) {
		int mode = flags & 0xFF;
		emfPlusAntiAliasingHint = mode == EMF_PLUS_SMOOTHING_MODE_NONE
				? RenderingHints.VALUE_ANTIALIAS_OFF
				: RenderingHints.VALUE_ANTIALIAS_ON;
		if (graphics != null) {
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, emfPlusAntiAliasingHint);
		}
	}

	private void setEmfPlusTextRenderingHint(int flags) {
		int hint = flags & 0xFF;
		if (hint == EMF_PLUS_TEXT_RENDERING_HINT_SINGLE_BIT_PER_PIXEL
				|| hint == EMF_PLUS_TEXT_RENDERING_HINT_SINGLE_BIT_PER_PIXEL_GRID_FIT) {
			emfPlusTextAntiAliasingHint = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
		} else if (hint == EMF_PLUS_TEXT_RENDERING_HINT_ANTIALIAS_GRID_FIT) {
			emfPlusTextAntiAliasingHint = RenderingHints.VALUE_TEXT_ANTIALIAS_GASP;
		} else if (hint == EMF_PLUS_TEXT_RENDERING_HINT_CLEAR_TYPE_GRID_FIT) {
			emfPlusTextAntiAliasingHint = RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
		} else {
			emfPlusTextAntiAliasingHint = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
		}
		if (graphics != null) {
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, emfPlusTextAntiAliasingHint);
		}
	}

	private void setEmfPlusTextContrast(int flags) {
		emfPlusTextContrast = flags & 0xFFFF;
		if (graphics != null) {
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, toAwtTextContrast(emfPlusTextContrast));
		}
	}

	private int toAwtTextContrast(int contrast) {
		if (contrast <= 0) {
			return 140;
		}
		if (contrast <= 12) {
			return clampInt(100 + contrast * 10, 100, 250);
		}
		if (contrast <= 250) {
			return clampInt(contrast, 100, 250);
		}
		return clampInt(contrast / 10, 100, 250);
	}

	private int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private void setEmfPlusInterpolationMode(int flags) {
		int mode = flags & 0xFF;
		if (mode == EMF_PLUS_INTERPOLATION_MODE_NEAREST_NEIGHBOR) {
			emfPlusInterpolationHint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
		} else if (mode == EMF_PLUS_INTERPOLATION_MODE_BICUBIC
				|| mode == EMF_PLUS_INTERPOLATION_MODE_HIGH_QUALITY_BICUBIC) {
			emfPlusInterpolationHint = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
		} else {
			emfPlusInterpolationHint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
		}
		if (graphics != null) {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, emfPlusInterpolationHint);
		}
	}

	private void setEmfPlusPixelOffsetMode(int flags) {
		int mode = flags & 0xFF;
		emfPlusStrokeControlHint = mode == EMF_PLUS_PIXEL_OFFSET_MODE_HIGH_QUALITY
				|| mode == EMF_PLUS_PIXEL_OFFSET_MODE_HALF
						? RenderingHints.VALUE_STROKE_PURE
						: RenderingHints.VALUE_STROKE_DEFAULT;
		emfPlusPixelOffsetX = mode == EMF_PLUS_PIXEL_OFFSET_MODE_HIGH_QUALITY || mode == EMF_PLUS_PIXEL_OFFSET_MODE_HALF
				? 0.5
				: 0.0;
		emfPlusPixelOffsetY = emfPlusPixelOffsetX;
		if (graphics != null) {
			graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, emfPlusStrokeControlHint);
		}
	}

	private void setEmfPlusCompositingMode(int flags) {
		int mode = flags & 0xFF;
		emfPlusComposite = mode == EMF_PLUS_COMPOSITING_MODE_SOURCE_COPY ? AlphaComposite.Src : AlphaComposite.SrcOver;
		if (graphics != null) {
			graphics.setComposite(emfPlusComposite);
		}
	}

	private void setEmfPlusCompositingQuality(int flags) {
		int quality = flags & 0xFF;
		emfPlusAlphaInterpolationHint = quality == EMF_PLUS_COMPOSITING_QUALITY_HIGH_QUALITY
				|| quality == EMF_PLUS_COMPOSITING_QUALITY_GAMMA_CORRECTED
				|| quality == EMF_PLUS_COMPOSITING_QUALITY_ASSUME_LINEAR
						? RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
						: RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED;
		if (graphics != null) {
			graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, emfPlusAlphaInterpolationHint);
		}
	}

	private void setEmfPlusTSGraphics(byte[] payload) {
		if (payload.length < 36) {
			return;
		}
		setEmfPlusAntiAliasMode((payload[0] & 0xFF) << 1);
		setEmfPlusTextRenderingHint(payload[1] & 0xFF);
		setEmfPlusCompositingMode(payload[2] & 0xFF);
		setEmfPlusCompositingQuality(payload[3] & 0xFF);
		emfPlusRenderingOriginX = readInt16(payload, 4);
		emfPlusRenderingOriginY = readInt16(payload, 6);
		setEmfPlusTextContrast(readUInt16(payload, 8));
		setEmfPlusInterpolationMode(payload[10] & 0xFF);
		setEmfPlusPixelOffsetMode(payload[11] & 0xFF);
		emfPlusWorldTransform = new double[]{readFloat(payload, 12), readFloat(payload, 16), readFloat(payload, 20),
				readFloat(payload, 24), readFloat(payload, 28), readFloat(payload, 32)};
	}

	private void beginEmfPlusContainer(int flags, byte[] payload) {
		if (payload.length < 36) {
			return;
		}
		double destX = readFloat(payload, 0);
		double destY = readFloat(payload, 4);
		double destWidth = readFloat(payload, 8);
		double destHeight = readFloat(payload, 12);
		double srcX = readFloat(payload, 16);
		double srcY = readFloat(payload, 20);
		double srcWidth = readFloat(payload, 24);
		double srcHeight = readFloat(payload, 28);
		if (srcWidth == 0.0 || srcHeight == 0.0) {
			return;
		}
		double scaleX = destWidth / srcWidth;
		double scaleY = destHeight / srcHeight;
		emfPlusPageUnitScale = toEmfPlusPageUnitScale(flags & 0xFF);
		applyEmfPlusWorldTransform(EMF_PLUS_FLAG_MATRIX_ORDER_APPEND,
				new double[]{scaleX, 0, 0, scaleY, destX - srcX * scaleX, destY - srcY * scaleY});
	}

	private void handleEmfPlusSetClipRect(int flags, byte[] payload) {
		double[][] rects = readEmfPlusRects(payload, 0, 1, false);
		if (rects == null) {
			return;
		}
		applyEmfPlusClip(createEmfPlusRect(rects[0]), (flags >>> 8) & 0xFF);
	}

	private void handleEmfPlusSetClipPath(int flags) {
		EmfPlusPath path = emfPlusPaths.get(Integer.valueOf(flags & 0xFF));
		if (path != null) {
			applyEmfPlusClip(createEmfPlusPath(path), (flags >>> 8) & 0xFF);
		}
	}

	private void handleEmfPlusSetClipRegion(int flags) {
		EmfPlusRegion region = emfPlusRegions.get(Integer.valueOf(flags & 0xFF));
		Area area = createEmfPlusRegionArea(region);
		if (area != null) {
			applyEmfPlusClip(area, (flags >>> 8) & 0xFF);
		}
	}

	private void resetEmfPlusClip() {
		ensureGraphics();
		graphics.setClip(null);
	}

	private void offsetEmfPlusClip(byte[] payload) {
		if (payload.length < 8 || graphics == null || graphics.getClip() == null) {
			return;
		}
		double[] offset = toEmfPlusLogicalSize(readFloat(payload, 0), readFloat(payload, 4));
		AffineTransform transform = AffineTransform.getTranslateInstance(offset[0], offset[1]);
		graphics.setClip(transform.createTransformedShape(graphics.getClip()));
	}

	private void handleEmfPlusSetTSClip(int flags, byte[] payload) {
		boolean compressed = (flags & 0x8000) != 0;
		int count = flags & 0x7FFF;
		if (count <= 0) {
			return;
		}
		double[][] rects = compressed
				? readCompressedEmfPlusTSClipRects(payload, count)
				: readEmfPlusTSClipRects(payload, count);
		if (rects == null) {
			return;
		}
		Area clip = new Area();
		for (int i = 0; i < rects.length; i++) {
			double[] p0 = toEmfPlusLogicalPoint(rects[i][0], rects[i][1]);
			double[] size = toEmfPlusLogicalSize(rects[i][2] - rects[i][0], rects[i][3] - rects[i][1]);
			clip.add(new Area(new Rectangle2D.Double(p0[0], p0[1], size[0], size[1])));
		}
		ensureGraphics();
		graphics.setClip(clip);
	}

	private void applyEmfPlusClip(Shape shape, int combineMode) {
		ensureGraphics();
		if (shape == null) {
			return;
		}
		if (combineMode == EMF_PLUS_COMBINE_MODE_REPLACE || graphics.getClip() == null) {
			graphics.setClip(shape);
			return;
		}

		Area oldClip = new Area(graphics.getClip());
		Area newClip = new Area(shape);
		if (combineMode == EMF_PLUS_COMBINE_MODE_UNION) {
			oldClip.add(newClip);
		} else if (combineMode == EMF_PLUS_COMBINE_MODE_XOR) {
			oldClip.exclusiveOr(newClip);
		} else if (combineMode == EMF_PLUS_COMBINE_MODE_EXCLUDE) {
			oldClip.subtract(newClip);
		} else if (combineMode == EMF_PLUS_COMBINE_MODE_COMPLEMENT) {
			newClip.subtract(oldClip);
			oldClip = newClip;
		} else {
			oldClip.intersect(newClip);
		}
		graphics.setClip(oldClip);
	}

	private void handleEmfPlusDrawImage(int flags, byte[] payload) {
		if (payload.length < 32) {
			return;
		}
		int objectId = flags & 0xFF;
		int imageAttributesId = readInt32(payload, 0);
		double srcX = readFloat(payload, 8);
		double srcY = readFloat(payload, 12);
		double srcWidth = readFloat(payload, 16);
		double srcHeight = readFloat(payload, 20);
		if (srcWidth == 0 || srcHeight == 0) {
			return;
		}
		double[][] rects = readEmfPlusRects(payload, 24, 1, isEmfPlusCompressed(flags));
		if (rects == null) {
			return;
		}
		double[] rect = rects[0];
		EmfPlusImageEffect effect = pendingEmfPlusImageEffect;
		pendingEmfPlusImageEffect = null;
		drawEmfPlusImage(objectId, imageAttributesId, srcX, srcY, srcWidth, srcHeight,
				new double[][]{{rect[0], rect[1]}, {rect[0] + rect[2], rect[1]}, {rect[0], rect[1] + rect[3]}}, false,
				effect);
	}

	private void handleEmfPlusDrawImagePoints(int flags, byte[] payload) {
		int objectId = flags & 0xFF;
		if (payload.length < 28) {
			return;
		}
		int imageAttributesId = readInt32(payload, 0);
		int srcUnit = readInt32(payload, 4);
		double srcX = readFloat(payload, 8);
		double srcY = readFloat(payload, 12);
		double srcWidth = readFloat(payload, 16);
		double srcHeight = readFloat(payload, 20);
		int count = readInt32(payload, 24);
		if (count < 3 || srcWidth == 0 || srcHeight == 0) {
			return;
		}
		double[][] points = readEmfPlusDrawingPoints(payload, 28, count, flags);
		if (points == null) {
			return;
		}
		EmfPlusImageEffect effect = pendingEmfPlusImageEffect;
		pendingEmfPlusImageEffect = null;
		drawEmfPlusImage(objectId, imageAttributesId, srcX, srcY, srcWidth, srcHeight, points,
				shouldNormalizeEmfPlusImagePoints(srcUnit), effect);
	}

	private boolean shouldNormalizeEmfPlusImagePoints(int srcUnit) {
		return srcUnit != EMF_PLUS_UNIT_PIXEL;
	}

	private void drawEmfPlusImage(int objectId, int imageAttributesId, double srcX, double srcY, double srcWidth,
			double srcHeight, double[][] points, boolean normalizeUnit, EmfPlusImageEffect effect) {
		EmfPlusImageAttributes attributes = emfPlusImageAttributes.get(Integer.valueOf(imageAttributesId));
		BufferedImage bitmapImage = emfPlusBitmapImages.get(Integer.valueOf(objectId));
		if (bitmapImage != null) {
			drawEmfPlusBitmapImage(applyEmfPlusImageEffect(bitmapImage, effect), attributes, srcX, srcY, srcWidth,
					srcHeight, points, normalizeUnit);
			return;
		}
		drawEmfPlusMetafileImage(objectId, attributes, srcX, srcY, srcWidth, srcHeight, points, normalizeUnit, effect);
	}

	private void drawEmfPlusMetafileImage(int objectId, EmfPlusImageAttributes attributes, double srcX, double srcY,
			double srcWidth, double srcHeight, double[][] points, boolean normalizeUnit, EmfPlusImageEffect effect) {
		byte[] metafile = emfPlusMetafileImages.get(Integer.valueOf(objectId));
		if (metafile == null) {
			return;
		}
		BufferedImage metafileImage = renderEmfPlusMetafile(metafile);
		if (metafileImage == null) {
			return;
		}
		drawEmfPlusBitmapImage(applyEmfPlusImageEffect(metafileImage, effect), attributes, srcX, srcY, srcWidth,
				srcHeight, points, normalizeUnit);
	}

	private BufferedImage applyEmfPlusImageEffect(BufferedImage source, EmfPlusImageEffect effect) {
		if (source == null || effect == null) {
			return source;
		}
		BufferedImage filtered = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				filtered.setRGB(x, y, applyEmfPlusImageEffect(source.getRGB(x, y), effect));
			}
		}
		return filtered;
	}

	private int applyEmfPlusImageEffect(int argb, EmfPlusImageEffect effect) {
		if (effect.colorMatrix != null) {
			return applyEmfPlusColorMatrix(argb, effect.colorMatrix);
		}
		if (effect.lookupTables != null) {
			return applyEmfPlusColorLookupTable(argb, effect.lookupTables);
		}
		return argb;
	}

	private int applyEmfPlusColorMatrix(int argb, double[][] matrix) {
		double r = ((argb >>> 16) & 0xFF) / 255.0;
		double g = ((argb >>> 8) & 0xFF) / 255.0;
		double b = (argb & 0xFF) / 255.0;
		double a = ((argb >>> 24) & 0xFF) / 255.0;
		double outR = r * matrix[0][0] + g * matrix[1][0] + b * matrix[2][0] + a * matrix[3][0] + matrix[4][0];
		double outG = r * matrix[0][1] + g * matrix[1][1] + b * matrix[2][1] + a * matrix[3][1] + matrix[4][1];
		double outB = r * matrix[0][2] + g * matrix[1][2] + b * matrix[2][2] + a * matrix[3][2] + matrix[4][2];
		double outA = r * matrix[0][3] + g * matrix[1][3] + b * matrix[2][3] + a * matrix[3][3] + matrix[4][3];
		return (clampEmfPlusColorChannel(outA) << 24) | (clampEmfPlusColorChannel(outR) << 16)
				| (clampEmfPlusColorChannel(outG) << 8) | clampEmfPlusColorChannel(outB);
	}

	private int clampEmfPlusColorChannel(double value) {
		if (value <= 0.0 || Double.isNaN(value)) {
			return 0;
		}
		if (value >= 1.0) {
			return 0xFF;
		}
		return (int) Math.round(value * 255.0);
	}

	private int applyEmfPlusColorLookupTable(int argb, byte[][] lookupTables) {
		int b = lookupTables[0][argb & 0xFF] & 0xFF;
		int g = lookupTables[1][(argb >>> 8) & 0xFF] & 0xFF;
		int r = lookupTables[2][(argb >>> 16) & 0xFF] & 0xFF;
		int a = lookupTables[3][(argb >>> 24) & 0xFF] & 0xFF;
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private void drawEmfPlusBitmapImage(BufferedImage bitmapImage, EmfPlusImageAttributes attributes, double srcX,
			double srcY, double srcWidth, double srcHeight, double[][] points, boolean normalizeUnit) {
		double sourceLeft = Math.min(srcX, srcX + srcWidth);
		double sourceTop = Math.min(srcY, srcY + srcHeight);
		double sourceRight = Math.max(srcX, srcX + srcWidth);
		double sourceBottom = Math.max(srcY, srcY + srcHeight);
		double visibleLeft = Math.max(0, sourceLeft);
		double visibleTop = Math.max(0, sourceTop);
		double visibleRight = Math.min(bitmapImage.getWidth(), sourceRight);
		double visibleBottom = Math.min(bitmapImage.getHeight(), sourceBottom);
		boolean intersectsBitmap = visibleLeft < visibleRight && visibleTop < visibleBottom;

		double[] p0 = toEmfPlusLogicalPoint(points[0][0], points[0][1]);
		double[] p1 = toEmfPlusLogicalPoint(points[1][0], points[1][1]);
		double[] p2 = toEmfPlusLogicalPoint(points[2][0], points[2][1]);
		if (normalizeUnit) {
			double[] unit = getEmfPlusImageUnitScale(p0, p1, p2, srcWidth, srcHeight);
			p0 = normalizeEmfPlusImagePoint(p0, unit);
			p1 = normalizeEmfPlusImagePoint(p1, unit);
			p2 = normalizeEmfPlusImagePoint(p2, unit);
		}

		double a = (p1[0] - p0[0]) / srcWidth;
		double b = (p1[1] - p0[1]) / srcWidth;
		double c = (p2[0] - p0[0]) / srcHeight;
		double d = (p2[1] - p0[1]) / srcHeight;

		ensureGraphics();
		Shape destination = createEmfPlusImageDestination(p0, p1, p2);
		ensureCanvasContains(destination);
		Shape oldClip = graphics.getClip();
		graphics.clip(destination);
		if (attributes != null && isEmfPlusTileWrapMode(attributes.wrapMode)) {
			BufferedImage source = createEmfPlusWrappedBitmap(bitmapImage, srcX, srcY, srcWidth, srcHeight,
					attributes.wrapMode);
			if (source != null) {
				AffineTransform old = graphics.getTransform();
				graphics.drawImage(source,
						new AffineTransform((p1[0] - p0[0]) / source.getWidth(), (p1[1] - p0[1]) / source.getWidth(),
								(p2[0] - p0[0]) / source.getHeight(), (p2[1] - p0[1]) / source.getHeight(), p0[0],
								p0[1]),
						null);
				graphics.setTransform(old);
			}
		} else {
			if (attributes != null && attributes.wrapMode == EMF_PLUS_WRAP_MODE_CLAMP
					&& attributes.objectClamp == EMF_PLUS_OBJECT_CLAMP_BITMAP) {
				BufferedImage source = createEmfPlusClampedBitmap(bitmapImage, srcX, srcY, srcWidth, srcHeight);
				if (source != null) {
					AffineTransform old = graphics.getTransform();
					graphics.drawImage(source,
							new AffineTransform((p1[0] - p0[0]) / source.getWidth(),
									(p1[1] - p0[1]) / source.getWidth(), (p2[0] - p0[0]) / source.getHeight(),
									(p2[1] - p0[1]) / source.getHeight(), p0[0], p0[1]),
							null);
					graphics.setTransform(old);
				}
			} else {
				if (attributes != null && attributes.wrapMode == EMF_PLUS_WRAP_MODE_CLAMP) {
					Paint oldPaint = graphics.getPaint();
					graphics.setPaint(toEmfPlusColor(attributes.clampColor));
					graphics.fill(destination);
					graphics.setPaint(oldPaint);
				}
				if (intersectsBitmap) {
					double e = p0[0] - srcX * a - srcY * c;
					double f = p0[1] - srcX * b - srcY * d;
					AffineTransform old = graphics.getTransform();
					graphics.drawImage(bitmapImage, new AffineTransform(a, b, c, d, e, f), null);
					graphics.setTransform(old);
				}
			}
		}
		graphics.setClip(oldClip);
		suppressEmfPlusFallback = true;
	}

	private BufferedImage createEmfPlusClampedBitmap(BufferedImage image, double srcX, double srcY, double srcWidth,
			double srcHeight) {
		int width = Math.max(1, (int) Math.ceil(Math.abs(srcWidth)));
		int height = Math.max(1, (int) Math.ceil(Math.abs(srcHeight)));
		if (image.getWidth() <= 0 || image.getHeight() <= 0) {
			return null;
		}
		BufferedImage clamped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			int sourceY = clampIndex((int) Math.floor(srcHeight >= 0 ? srcY + y : srcY - y), image.getHeight());
			for (int x = 0; x < width; x++) {
				int sourceX = clampIndex((int) Math.floor(srcWidth >= 0 ? srcX + x : srcX - x), image.getWidth());
				clamped.setRGB(x, y, image.getRGB(sourceX, sourceY));
			}
		}
		return clamped;
	}

	private int clampIndex(int value, int size) {
		if (value < 0) {
			return 0;
		}
		if (value >= size) {
			return size - 1;
		}
		return value;
	}

	private boolean isEmfPlusTileWrapMode(int wrapMode) {
		return wrapMode == EMF_PLUS_WRAP_MODE_TILE || wrapMode == EMF_PLUS_WRAP_MODE_TILE_FLIP_X
				|| wrapMode == EMF_PLUS_WRAP_MODE_TILE_FLIP_Y || wrapMode == EMF_PLUS_WRAP_MODE_TILE_FLIP_XY;
	}

	private BufferedImage createEmfPlusWrappedBitmap(BufferedImage image, double srcX, double srcY, double srcWidth,
			double srcHeight, int wrapMode) {
		int width = Math.max(1, (int) Math.ceil(Math.abs(srcWidth)));
		int height = Math.max(1, (int) Math.ceil(Math.abs(srcHeight)));
		if (image.getWidth() <= 0 || image.getHeight() <= 0) {
			return null;
		}
		BufferedImage wrapped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			int sourceY = toEmfPlusWrappedIndex((int) Math.floor(srcHeight >= 0 ? srcY + y : srcY - y),
					image.getHeight(),
					wrapMode == EMF_PLUS_WRAP_MODE_TILE_FLIP_Y || wrapMode == EMF_PLUS_WRAP_MODE_TILE_FLIP_XY);
			for (int x = 0; x < width; x++) {
				int sourceX = toEmfPlusWrappedIndex((int) Math.floor(srcWidth >= 0 ? srcX + x : srcX - x),
						image.getWidth(),
						wrapMode == EMF_PLUS_WRAP_MODE_TILE_FLIP_X || wrapMode == EMF_PLUS_WRAP_MODE_TILE_FLIP_XY);
				wrapped.setRGB(x, y, image.getRGB(sourceX, sourceY));
			}
		}
		return wrapped;
	}

	private int toEmfPlusWrappedIndex(int value, int size, boolean flip) {
		if (!flip) {
			return floorMod(value, size);
		}
		int period = size * 2;
		int wrapped = floorMod(value, period);
		return wrapped < size ? wrapped : period - wrapped - 1;
	}

	private int floorMod(int value, int size) {
		int result = value % size;
		return result < 0 ? result + size : result;
	}

	private BufferedImage renderEmfPlusMetafile(byte[] metafile) {
		try {
			AwtGdi gdi = new AwtGdi();
			gdi.setReplaceSymbolFont(replaceSymbolFont);
			if (isPlaceableWmf(metafile)) {
				new WmfParser(false).parse(new ByteArrayInputStream(normalizePlaceableWmf(metafile)), gdi);
			} else {
				new EmfParser().parse(new ByteArrayInputStream(metafile), gdi);
			}
			return cropTransparentBounds(gdi.getImage());
		} catch (IOException e) {
			return null;
		} catch (EmfParseException e) {
			return null;
		} catch (WmfParseException e) {
			return null;
		}
	}

	private boolean isPlaceableWmf(byte[] data) {
		return data.length >= 4 && readInt32(data, 0) == 0x9AC6CDD7;
	}

	private byte[] normalizePlaceableWmf(byte[] data) {
		if (data.length >= 28 && readInt16(data, 22) == 0 && readInt16(data, 24) == 1 && readInt16(data, 26) == 9) {
			byte[] normalized = new byte[data.length - 2];
			System.arraycopy(data, 0, normalized, 0, 22);
			System.arraycopy(data, 24, normalized, 22, data.length - 24);
			return normalized;
		}
		return data;
	}

	private BufferedImage cropTransparentBounds(BufferedImage source) {
		BufferedImage cropped = cropBounds(source, false);
		if (cropped != source) {
			return cropped;
		}
		return cropBounds(source, true);
	}

	private BufferedImage cropBounds(BufferedImage source, boolean ignoreBackgroundColor) {
		int background = source.getRGB(0, 0) & 0x00FFFFFF;
		int minX = source.getWidth();
		int minY = source.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int argb = source.getRGB(x, y);
				int alpha = (argb >>> 24) & 0xFF;
				if (alpha != 0 && (!ignoreBackgroundColor || (argb & 0x00FFFFFF) != background)) {
					if (x < minX) {
						minX = x;
					}
					if (y < minY) {
						minY = y;
					}
					if (x > maxX) {
						maxX = x;
					}
					if (y > maxY) {
						maxY = y;
					}
				}
			}
		}
		if (maxX < minX || maxY < minY) {
			return source;
		}
		if (minX == 0 && minY == 0 && maxX == source.getWidth() - 1 && maxY == source.getHeight() - 1) {
			return source;
		}

		BufferedImage cropped = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = cropped.createGraphics();
		try {
			configureGraphics(g);
			g.drawImage(source, -minX, -minY, null);
		} finally {
			g.dispose();
		}
		return cropped;
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
		Path2D.Double path = toPolyline(points);
		if (path != null) {
			strokeShape(path, dc.getPen());
			if (points.length > 0) {
				dc.moveToEx(points[points.length - 1].x, points[points.length - 1].y, null);
			}
		}
	}

	public void polyPolygon(Point[][] points) {
		if (points == null || points.length == 0) {
			return;
		}
		if (currentPath != null) {
			for (int i = 0; i < points.length; i++) {
				appendPoints(points[i], true);
			}
			return;
		}
		Path2D.Double path = new Path2D.Double(
				dc.getPolyFillMode() == Gdi.WINDING ? Path2D.WIND_NON_ZERO : Path2D.WIND_EVEN_ODD);
		for (int i = 0; i < points.length; i++) {
			appendPoints(path, points[i], true);
		}
		if (path.getCurrentPoint() != null) {
			drawShape(path);
		}
	}

	public void fillPath() {
		if (currentPath != null) {
			fillShape(currentPathWithPolyFillMode(), dc.getBrush());
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
			fillShape(currentPathWithPolyFillMode(), dc.getBrush());
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
		selectedPalette = saved.selectedPalette;
		selectedColorSpace = saved.selectedColorSpace;
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
		Rectangle2D bounds = toRectangle(sx, sy, ex - sx, ey - sy);
		Shape rect = new java.awt.geom.RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(),
				bounds.getHeight(), Math.abs(irx(rw)), Math.abs(iry(rh)));
		if (currentPath != null) {
			currentPath.append(rect, false);
			return;
		}
		drawShape(rect);
	}

	public void seveDC() {
		saveDC.add(new AwtSavedDc((AwtDc) dc.clone(), graphics != null ? graphics.getClip() : null, selectedPalette,
				selectedColorSpace));
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
			applyClip(currentPathWithPolyFillMode(), mode);
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
		} else if (obj instanceof AwtRegion) {
			selectClipRgn((AwtRegion) obj);
		}
	}

	public void selectPalette(GdiPalette palette, boolean mode) {
		selectedPalette = palette instanceof AwtPalette ? (AwtPalette) palette : null;
	}

	public void setBkColor(int color) {
		dc.setBkColor(color);
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

	int getICMMode() {
		return dc.getICMMode();
	}

	byte[] getICMProfile() {
		return dc.getICMProfile();
	}

	byte[] getColorAdjustment() {
		return dc.getColorAdjustment();
	}

	long getLayout() {
		return dc.getLayout();
	}

	long getMapperFlags() {
		return dc.getMapperFlags();
	}

	int getRelAbs() {
		return dc.getRelAbs();
	}

	int getCurrentX() {
		return dc.getCurrentX();
	}

	int getCurrentY() {
		return dc.getCurrentY();
	}

	int getBrushOrgX() {
		return dc.getBrushOrgX();
	}

	int getBrushOrgY() {
		return dc.getBrushOrgY();
	}

	int getTextJustificationExtra() {
		return dc.getTextJustificationExtra();
	}

	int getTextJustificationCount() {
		return dc.getTextJustificationCount();
	}

	int getBkColor() {
		return dc.getBkColor();
	}

	int getBkMode() {
		return dc.getBkMode();
	}

	int getTextColor() {
		return dc.getTextColor();
	}

	int getTextAlign() {
		return dc.getTextAlign();
	}

	int getTextCharacterExtra() {
		return dc.getTextCharacterExtra();
	}

	int getPolyFillMode() {
		return dc.getPolyFillMode();
	}

	int getROP2() {
		return dc.getROP2();
	}

	int getStretchBltMode() {
		return dc.getStretchBltMode();
	}

	int getArcDirection() {
		return dc.getArcDirection();
	}

	float getMiterLimit() {
		return dc.getMiterLimit();
	}

	AwtBrush getSelectedBrush() {
		return dc.getBrush();
	}

	AwtPen getSelectedPen() {
		return dc.getPen();
	}

	AwtFont getSelectedFont() {
		return dc.getFont();
	}

	public int setMetaRgn() {
		ensureGraphics();
		return getClipRegionType();
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
		Rectangle2D rect = toRectangle(x, y, 1, 1);
		ensureCanvasContains(rect);
		graphics.setPaint(toColor(color));
		graphics.fill(rect);
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
		if (ignorePendingEmfHeaderMapping()) {
			return;
		}
		if (placeableHeader && !replayingPendingEmf) {
			x = placeableViewportX(x);
			y = placeableViewportY(y);
		}
		dc.setViewportExtEx(x, y, old);
		if (!replayingPendingEmf && !placeableHeader && !emfHeaderCanvas && x != 0 && y != 0 && image == null) {
			growCanvas = false;
			canvasWidth = Math.min(Math.max(Math.abs(x), 1), MAX_CANVAS_SIZE);
			canvasHeight = Math.min(Math.max(Math.abs(y), 1), MAX_CANVAS_SIZE);
		}
	}

	public void setViewportOrgEx(int x, int y, Point old) {
		if (ignorePendingEmfHeaderMapping()) {
			return;
		}
		if (placeableHeader && !replayingPendingEmf) {
			x = placeableViewportX(x);
			y = placeableViewportY(y);
		}
		dc.setViewportOrgEx(x, y, old);
	}

	private int placeableViewportX(int value) {
		return (int) Math.round(value * placeableViewportScaleX);
	}

	private int placeableViewportY(int value) {
		return (int) Math.round(value * placeableViewportScaleY);
	}

	public void setWindowExtEx(int width, int height, Size old) {
		if (ignorePendingEmfHeaderMapping()) {
			return;
		}
		dc.setWindowExtEx(width, height, old);
		if (!replayingPendingEmf && !placeableHeader && !emfHeaderCanvas && width != 0 && height != 0) {
			growCanvas = false;
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

	private boolean ignorePendingEmfHeaderMapping() {
		if (replayingPendingEmf && ignoredPendingEmfHeaderMappings > 0) {
			ignoredPendingEmfHeaderMappings--;
			return true;
		}
		return false;
	}

	public void setWindowOrgEx(int x, int y, Point old) {
		if (ignorePendingEmfHeaderMapping()) {
			return;
		}
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
		drawText(x, y, GdiUtils.convertString(text, dc.getFont() != null ? dc.getFont().getCharset() : 0), null, 0);
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
			Shape oldClip = null;
			boolean restoreClip = false;
			try {
				boolean useCapturedDc = pendingEmf.hasExplicitDc();
				boolean useFooterDc = !useCapturedDc && hasExplicitMapping(savedDc);
				AwtDc replayDc = useFooterDc ? savedDc : pendingEmf.dc;
				dc = (AwtDc) replayDc.clone();
				replayingPendingEmf = true;
				ignoredPendingEmfHeaderMappings = useCapturedDc || useFooterDc ? 2 : 0;
				if (!useCapturedDc && !useFooterDc) {
					applyPendingEmfFrame(pendingEmf.header);
				} else if (pendingEmf.header != null && useCapturedDc) {
					ensureGraphics();
					oldClip = graphics.getClip();
					graphics.clip(toRectangle(pendingEmf.header.boundsLeft, pendingEmf.header.boundsTop,
							pendingEmf.header.boundsWidth, pendingEmf.header.boundsHeight));
					restoreClip = true;
				}
				new EmfParser(false).parse(new ByteArrayInputStream(pendingEmf.data), this);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} catch (EmfParseException e) {
				throw new IllegalStateException(e);
			} finally {
				if (restoreClip && graphics != null) {
					graphics.setClip(oldClip);
				}
				replayingPendingEmf = false;
				ignoredPendingEmfHeaderMappings = 0;
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
		pendingEmfList.add(new PendingEmf(copy, (AwtDc) dc.clone(), readEmfHeader(copy),
				hasExplicitMapping() && !placeableHeader));
	}

	private boolean hasExplicitMapping() {
		return hasExplicitMapping(dc);
	}

	private boolean hasExplicitMapping(AwtDc dc) {
		return dc != null && (dc.getWindowWidth() != 0 || dc.getWindowHeight() != 0 || dc.getViewportWidth() != 0
				|| dc.getViewportHeight() != 0);
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
		private final boolean explicitDc;

		private PendingEmf(byte[] data, AwtDc dc, EmfHeader header, boolean explicitDc) {
			this.data = data;
			this.dc = dc;
			this.header = header;
			this.explicitDc = explicitDc;
		}

		private boolean hasExplicitDc() {
			return explicitDc;
		}
	}

	private static class AwtSavedDc {
		private final AwtDc dc;
		private final Shape clip;
		private final AwtPalette selectedPalette;
		private final GdiColorSpace selectedColorSpace;

		private AwtSavedDc(AwtDc dc, Shape clip, AwtPalette selectedPalette, GdiColorSpace selectedColorSpace) {
			this.dc = dc;
			this.clip = clip;
			this.selectedPalette = selectedPalette;
			this.selectedColorSpace = selectedColorSpace;
		}
	}

	private static class TextAdvances {
		private final double[] x;
		private final double[] y;

		private TextAdvances(double[] x, double[] y) {
			this.x = x;
			this.y = y;
		}

		private double sumX() {
			double width = 0.0;
			for (int i = 0; i < x.length; i++) {
				width += x[i];
			}
			return width;
		}

		private double sumY() {
			if (y == null) {
				return 0.0;
			}
			double height = 0.0;
			for (int i = 0; i < y.length; i++) {
				height += y[i];
			}
			return height;
		}

		private boolean hasY() {
			return y != null;
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
		double start = toArcAngle(ix(sxa), iy(sya), cx, cy);
		double end = toArcAngle(ix(exa), iy(eya), cx, cy);
		double extent = dc.getArcDirection() == Gdi.AD_CLOCKWISE
				? -normalizeDegrees(start - end)
				: normalizeDegrees(end - start);
		if (sxa == exa && sya == eya) {
			extent = dc.getArcDirection() == Gdi.AD_CLOCKWISE ? -360.0 : 360.0;
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

	private Area createRegionArea(byte[] rgnData, int count, float[] xform) {
		if (rgnData == null || rgnData.length < 32) {
			return null;
		}
		int rectCount = Math.min(count > 0 ? count : readInt32(rgnData, 8), Math.max(0, (rgnData.length - 32) / 16));
		AffineTransform transform = toRegionTransform(xform);
		Area area = new Area();
		for (int i = 0; i < rectCount; i++) {
			int offset = 32 + i * 16;
			int left = readInt32(rgnData, offset);
			int top = readInt32(rgnData, offset + 4);
			int right = readInt32(rgnData, offset + 8);
			int bottom = readInt32(rgnData, offset + 12);
			area.add(new Area(transform != null
					? toTransformedRectangle(left, top, right, bottom, transform)
					: toRectangle(left, top, right - left, bottom - top)));
		}
		return area;
	}

	private AffineTransform toRegionTransform(float[] xform) {
		if (xform == null || xform.length < 6) {
			return null;
		}
		if (xform[0] == 1.0f && xform[1] == 0.0f && xform[2] == 0.0f && xform[3] == 1.0f && xform[4] == 0.0f
				&& xform[5] == 0.0f) {
			return null;
		}
		return new AffineTransform(xform[0], xform[1], xform[2], xform[3], xform[4], xform[5]);
	}

	private Shape toTransformedRectangle(int left, int top, int right, int bottom, AffineTransform transform) {
		Path2D.Double path = new Path2D.Double();
		appendTransformedRegionPoint(path, transform, left, top, true);
		appendTransformedRegionPoint(path, transform, right, top, false);
		appendTransformedRegionPoint(path, transform, right, bottom, false);
		appendTransformedRegionPoint(path, transform, left, bottom, false);
		path.closePath();
		return path;
	}

	private void appendTransformedRegionPoint(Path2D.Double path, AffineTransform transform, int x, int y,
			boolean move) {
		Point2D.Double point = new Point2D.Double(x, y);
		transform.transform(point, point);
		double px = ix(point.x);
		double py = iy(point.y);
		if (move) {
			path.moveTo(px, py);
		} else {
			path.lineTo(px, py);
		}
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
		double x1 = ix(v1.x);
		double y1 = iy(v1.y);
		double x2 = ix(v2.x);
		double y2 = iy(v2.y);
		double x3 = ix(v3.x);
		double y3 = iy(v3.y);
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
		ensureCanvasContains(shape);

		BufferedImage mask = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D mg = mask.createGraphics();
		try {
			configureGraphics(mg);
			mg.setClip(graphics.getClip());
			mg.setColor(Color.WHITE);
			mg.fill(shape);
		} finally {
			mg.dispose();
		}

		Rectangle2D bounds = shape.getBounds2D();
		int left = Math.max(0, (int) Math.floor(bounds.getMinX()));
		int top = Math.max(0, (int) Math.floor(bounds.getMinY()));
		int right = Math.min(canvasWidth, (int) Math.ceil(bounds.getMaxX()));
		int bottom = Math.min(canvasHeight, (int) Math.ceil(bounds.getMaxY()));
		int xorRgb = xorColor.getRGB() & 0x00FFFFFF;
		for (int y = top; y < bottom; y++) {
			for (int x = left; x < right; x++) {
				if ((mask.getRGB(x, y) & 0x00FFFFFF) == 0) {
					continue;
				}
				int rgb = (image.getRGB(x, y) & 0x00FFFFFF) ^ xorRgb;
				image.setRGB(x, y, 0xFF000000 | rgb);
			}
		}
	}

	private void drawShape(Shape shape) {
		fillShape(shape, dc.getBrush());
		strokeShape(shape, dc.getPen());
	}

	private void appendPoints(Point[] points, boolean close) {
		if (currentPath == null || points == null || points.length == 0) {
			return;
		}
		appendPoints(currentPath, points, close);
	}

	private void appendPoints(Path2D.Double path, Point[] points, boolean close) {
		if (path == null || points == null || points.length == 0) {
			return;
		}
		path.moveTo(ix(points[0].x), iy(points[0].y));
		for (int i = 1; i < points.length; i++) {
			path.lineTo(ix(points[i].x), iy(points[i].y));
		}
		if (close) {
			path.closePath();
		}
	}

	private void appendBezier(Point[] points, boolean connect) {
		if (currentPath == null || points == null || points.length == 0) {
			return;
		}
		int offset = 0;
		if (!connect) {
			currentPath.moveTo(ix(points[0].x), iy(points[0].y));
			offset = 1;
		}
		for (int i = offset; i + 2 < points.length; i += 3) {
			currentPath.curveTo(ix(points[i].x), iy(points[i].y), ix(points[i + 1].x), iy(points[i + 1].y),
					ix(points[i + 2].x), iy(points[i + 2].y));
		}
	}

	private Path2D.Double createBezierPath(Point[] points, boolean connect) {
		if (points == null || points.length == 0) {
			return null;
		}
		Path2D.Double path = new Path2D.Double();
		int offset = 0;
		if (connect) {
			path.moveTo(ix(dc.getCurrentX()), iy(dc.getCurrentY()));
		} else {
			path.moveTo(ix(points[0].x), iy(points[0].y));
			offset = 1;
		}
		for (int i = offset; i + 2 < points.length; i += 3) {
			path.curveTo(ix(points[i].x), iy(points[i].y), ix(points[i + 1].x), iy(points[i + 1].y),
					ix(points[i + 2].x), iy(points[i + 2].y));
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

		Shape oldClip = graphics.getClip();
		if (oldClip == null || mode == GdiRegion.RGN_COPY) {
			graphics.setClip(shape);
			return;
		}

		Area newArea = new Area(shape);
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

	private int getClipRegionType() {
		Shape clip = graphics.getClip();
		if (clip == null || clip.getBounds2D().isEmpty()) {
			return GdiRegion.NULLREGION;
		}
		if (clip instanceof Area && ((Area) clip).isRectangular()) {
			return GdiRegion.SIMPLEREGION;
		}
		if (clip instanceof Rectangle2D || clip instanceof java.awt.Rectangle) {
			return GdiRegion.SIMPLEREGION;
		}
		return GdiRegion.COMPLEXREGION;
	}

	private Path2D.Double currentPathWithPolyFillMode() {
		Path2D.Double path = new Path2D.Double(
				dc.getPolyFillMode() == Gdi.WINDING ? Path2D.WIND_NON_ZERO : Path2D.WIND_EVEN_ODD);
		path.append(currentPath.getPathIterator(null), false);
		return path;
	}

	private void fillShape(Shape shape, GdiBrush brush) {
		ensureGraphics();
		if (shouldSuppressEmfPlusFallback()) {
			return;
		}
		if (brush == null || brush.getStyle() == GdiBrush.BS_NULL || brush.getStyle() == GdiBrush.BS_HOLLOW) {
			return;
		}
		ensureCanvasContains(shape);
		if (dc.getROP2() != Gdi.R2_COPYPEN) {
			fillShapeRop2(shape, brush);
			return;
		}
		Paint old = graphics.getPaint();
		graphics.setPaint(toPaint(brush));
		graphics.fill(shape);
		graphics.setPaint(old);
	}

	private void fillShapeRop2(Shape shape, GdiBrush brush) {
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
			mg.fill(shape);
		} finally {
			mg.dispose();
		}

		Rectangle2D bounds = shape.getBounds2D();
		int left = Math.max(0, (int) Math.floor(bounds.getMinX()));
		int top = Math.max(0, (int) Math.floor(bounds.getMinY()));
		int right = Math.min(canvasWidth, (int) Math.ceil(bounds.getMaxX()));
		int bottom = Math.min(canvasHeight, (int) Math.ceil(bounds.getMaxY()));
		if (left >= right || top >= bottom) {
			return;
		}

		BufferedImage source = new BufferedImage(right - left, bottom - top, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = source.createGraphics();
		try {
			configureGraphics(sg);
			sg.translate(-left, -top);
			sg.setPaint(toPaint(brush));
			sg.fill(shape);
		} finally {
			sg.dispose();
		}

		for (int y = top; y < bottom; y++) {
			for (int x = left; x < right; x++) {
				if ((mask.getRGB(x, y) & 0x00FFFFFF) == 0) {
					continue;
				}
				int brushRgb = source.getRGB(x - left, y - top) & 0x00FFFFFF;
				int dest = image.getRGB(x, y) & 0x00FFFFFF;
				int rgb = applyRop2(brushRgb, dest, dc.getROP2());
				image.setRGB(x, y, 0xFF000000 | rgb);
			}
		}
	}

	private void strokeShape(Shape shape, GdiPen pen) {
		ensureGraphics();
		if (shouldSuppressEmfPlusFallback()) {
			return;
		}
		if (pen == null || (pen.getStyle() & GdiPen.PS_STYLE_MASK) == GdiPen.PS_NULL) {
			return;
		}
		BasicStroke stroke = toStroke(pen);
		ensureCanvasContains(stroke.getLineWidth() <= 1.0f ? shape : stroke.createStrokedShape(shape));
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
				? new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
				: new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
						Math.max(dc.getMiterLimit(), 1.0f), dash, 0.0f);
	}

	private Paint toPaint(GdiBrush brush) {
		if (brush instanceof AwtPatternBrush) {
			BufferedImage pattern = decodeBitmap(((AwtPatternBrush) brush).image, ((AwtPatternBrush) brush).usage,
					false, null, false);
			if (pattern != null) {
				return new TexturePaint(pattern, new Rectangle2D.Double(ix(dc.getBrushOrgX()), iy(dc.getBrushOrgY()),
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
		return new TexturePaint(pattern, new Rectangle2D.Double(ix(dc.getBrushOrgX()), iy(dc.getBrushOrgY()),
				pattern.getWidth(), pattern.getHeight()));
	}

	private Color toColor(int gdiColor) {
		int red = gdiColor & 0xFF;
		int green = (gdiColor >>> 8) & 0xFF;
		int blue = (gdiColor >>> 16) & 0xFF;
		return new Color(red, green, blue);
	}

	private void drawText(int x, int y, String text, int[] lpdx, int options) {
		ensureGraphics();
		if (shouldSuppressEmfPlusFallback()) {
			return;
		}
		if (text == null || text.length() == 0) {
			return;
		}
		int align = dc.getTextAlign();
		if ((align & (Gdi.TA_NOUPDATECP | Gdi.TA_UPDATECP)) == Gdi.TA_UPDATECP) {
			x = dc.getCurrentX();
			y = dc.getCurrentY();
		}
		AwtFont gdiFont = dc.getFont();
		Font advanceFont = toFont(gdiFont);
		String mappedText = mapSymbolText(gdiFont, text);
		if (mappedText != text) {
			if (mappedText.length() != text.length()) {
				lpdx = null;
			}
			text = mappedText;
			advanceFont = toSymbolReplacementFont(advanceFont);
		}
		Font font = advanceFont;
		graphics.setFont(font);
		graphics.setPaint(toColor(dc.getTextColor()));
		FontMetrics metrics = graphics.getFontMetrics(advanceFont);
		boolean middleEasternText = isMiddleEasternFont(gdiFont);
		boolean rightToLeft = middleEasternText
				&& ((align & Gdi.TA_RTLREADING) != 0 || (options & Gdi.ETO_RTLREADING) != 0);
		TextAdvances advances = createTextAdvances(text, lpdx, metrics, options, middleEasternText);
		double textWidth = advances != null ? advances.sumX() : getTextWidth(advanceFont, text);
		int referenceX = ixi(x);
		int referenceY = iyi(y);
		int drawX = referenceX;
		int drawY = referenceY;
		if ((align & Gdi.TA_CENTER) == Gdi.TA_CENTER) {
			drawX -= (int) Math.round(textWidth / 2);
		} else if ((align & Gdi.TA_RIGHT) == Gdi.TA_RIGHT) {
			drawX -= (int) Math.round(textWidth);
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
		Object runDirection = getTextRunDirection(gdiFont, align, options);
		if (runDirection != null) {
			attributed.addAttribute(TextAttribute.RUN_DIRECTION, runDirection);
		}
		if (gdiFont != null && gdiFont.isUnderlined()) {
			attributed.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		}
		if (gdiFont != null && gdiFont.isStrikedOut()) {
			attributed.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		}
		ensureTextCanvasContains(drawX, drawY, text, textWidth, font, metrics, advances, gdiFont, referenceX,
				referenceY);
		AffineTransform old = graphics.getTransform();
		int escapement = getTextEscapement(gdiFont);
		if (escapement != 0) {
			graphics.rotate(-Math.toRadians(escapement / 10.0), referenceX, referenceY);
		}
		fillTextBackground(drawX, drawY, text, textWidth, font, metrics, advances);
		if (dc.getROP2() != Gdi.R2_COPYPEN) {
			drawTextRop2(attributed, text, font, gdiFont, advances, rightToLeft, drawX, drawY, textWidth, metrics);
		} else {
			drawTextForeground(graphics, attributed, text, gdiFont, advances, rightToLeft, drawX, drawY);
		}
		graphics.setTransform(old);
		if ((align & (Gdi.TA_NOUPDATECP | Gdi.TA_UPDATECP)) == Gdi.TA_UPDATECP) {
			dc.moveToEx(x + toLogicalTextAdvanceX(textWidth),
					y + toLogicalTextAdvanceY(advances != null ? advances.sumY() : 0.0), null);
		}
	}

	private double getTextWidth(Font font, String text) {
		return Math.max(font.getStringBounds(text, graphics.getFontRenderContext()).getWidth(), 1.0);
	}

	private int getTextEscapement(AwtFont gdiFont) {
		return gdiFont != null ? gdiFont.getEscapement() : 0;
	}

	private void ensureTextCanvasContains(int drawX, int baselineY, String text, double textWidth, Font font,
			FontMetrics metrics, TextAdvances advances, AwtFont gdiFont, int referenceX, int referenceY) {
		Rectangle2D bounds = createTextLogicalBounds(drawX, baselineY, text, textWidth, font, metrics, advances);
		int escapement = getTextEscapement(gdiFont);
		if (escapement != 0) {
			AffineTransform transform = AffineTransform.getRotateInstance(-Math.toRadians(escapement / 10.0),
					referenceX, referenceY);
			ensureTextCanvasContains(transform.createTransformedShape(bounds).getBounds2D());
		} else {
			ensureTextCanvasContains(bounds);
		}
	}

	private void ensureTextCanvasContains(Rectangle2D bounds) {
		double minX = bounds.getMinX();
		double minY = bounds.getMinY();
		double maxX = Math.min(bounds.getMaxX(), canvasMinX + canvasWidth);
		double maxY = Math.min(bounds.getMaxY(), canvasMinY + canvasHeight);
		if (minX < maxX && minY < maxY) {
			ensureCanvasContains(new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY));
		}
	}

	private Rectangle2D createTextLogicalBounds(int x, int baselineY, String text, double textWidth, Font font,
			FontMetrics metrics, TextAdvances advances) {
		Rectangle2D.Double bounds = new Rectangle2D.Double(x, baselineY - metrics.getAscent(), Math.max(textWidth, 1.0),
				metrics.getAscent() + metrics.getDescent());
		if (advances != null) {
			double ax = x;
			double ay = baselineY;
			for (int i = 0; i < text.length(); i++) {
				addTextBounds(bounds, font, text.substring(i, i + 1), ax, ay);
				if (advances.hasY()) {
					bounds.add(new Rectangle2D.Double(ax, ay - metrics.getAscent(),
							Math.max(metrics.charWidth(text.charAt(i)), 1),
							metrics.getAscent() + metrics.getDescent()));
				}
				ax += advances.x[i];
				if (advances.y != null) {
					ay += advances.y[i];
				}
			}
		} else {
			addTextBounds(bounds, font, text, x, baselineY);
		}
		return bounds;
	}

	private void addTextBounds(Rectangle2D.Double bounds, Font font, String text, double x, double baselineY) {
		Rectangle2D textBounds = font.getStringBounds(text, graphics.getFontRenderContext());
		bounds.add(new Rectangle2D.Double(x + textBounds.getX(), baselineY + textBounds.getY(), textBounds.getWidth(),
				textBounds.getHeight()));
	}

	private void drawTextRop2(AttributedString attributed, String text, Font font, AwtFont gdiFont,
			TextAdvances advances, boolean rightToLeft, int x, int y, double textWidth, FontMetrics metrics) {
		if (image == null || dc.getROP2() == Gdi.R2_COPYPEN || dc.getROP2() == Gdi.R2_NOP) {
			return;
		}

		Rectangle2D logicalBounds = createTextLogicalBounds(x, y, text, textWidth, font, metrics, advances);
		Rectangle2D bounds = graphics.getTransform().createTransformedShape(logicalBounds).getBounds2D();
		int left = Math.max(0, (int) Math.floor(bounds.getMinX()));
		int top = Math.max(0, (int) Math.floor(bounds.getMinY()));
		int right = Math.min(canvasWidth, (int) Math.ceil(bounds.getMaxX()));
		int bottom = Math.min(canvasHeight, (int) Math.ceil(bounds.getMaxY()));
		if (left >= right || top >= bottom) {
			return;
		}

		BufferedImage mask = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D mg = mask.createGraphics();
		try {
			configureGraphics(mg);
			mg.setTransform(graphics.getTransform());
			mg.setClip(graphics.getClip());
			mg.setColor(Color.WHITE);
			mg.setFont(graphics.getFont());
			drawTextForeground(mg, attributed, text, gdiFont, advances, rightToLeft, x, y);
		} finally {
			mg.dispose();
		}

		int textRgb = toColor(dc.getTextColor()).getRGB() & 0x00FFFFFF;
		for (int py = top; py < bottom; py++) {
			for (int px = left; px < right; px++) {
				if ((mask.getRGB(px, py) & 0x00FFFFFF) == 0) {
					continue;
				}
				int dest = image.getRGB(px, py) & 0x00FFFFFF;
				int rgb = applyRop2(textRgb, dest, dc.getROP2());
				image.setRGB(px, py, 0xFF000000 | rgb);
			}
		}
	}

	private void fillTextBackground(int drawX, int baselineY, String text, double textWidth, Font font,
			FontMetrics metrics, TextAdvances advances) {
		if (dc.getBkMode() != Gdi.OPAQUE) {
			return;
		}
		Rectangle2D bounds = createTextLogicalBounds(drawX, baselineY, text, textWidth, font, metrics, advances);
		Paint old = graphics.getPaint();
		graphics.setPaint(toColor(dc.getBkColor()));
		graphics.fill(bounds);
		graphics.setPaint(old);
	}

	private TextAdvances createTextAdvances(String text, int[] lpdx, FontMetrics metrics, int options,
			boolean forceCharacterAdvances) {
		if (!forceCharacterAdvances && (lpdx == null || lpdx.length == 0) && dc.getTextCharacterExtra() == 0
				&& dc.getTextJustificationExtra() == 0) {
			return null;
		}

		boolean pdy = (options & Gdi.ETO_PDY) != 0;
		double[] xAdvances = new double[text.length()];
		double[] yAdvances = pdy ? new double[text.length()] : null;
		for (int i = 0; i < xAdvances.length; i++) {
			if (pdy && lpdx != null && i * 2 + 1 < lpdx.length) {
				xAdvances[i] = rx(lpdx[i * 2]);
				yAdvances[i] = ry(lpdx[i * 2 + 1]);
			} else if (!pdy && lpdx != null && i < lpdx.length) {
				xAdvances[i] = rx(lpdx[i]);
			} else {
				xAdvances[i] = metrics.charWidth(text.charAt(i));
			}
			xAdvances[i] += rx(dc.getTextCharacterExtra());
			if (text.charAt(i) == ' ' && dc.getTextJustificationCount() > 0) {
				xAdvances[i] += rx(dc.getTextJustificationExtra()) / dc.getTextJustificationCount();
			}
		}
		return new TextAdvances(xAdvances, yAdvances);
	}

	private void drawTextForeground(Graphics2D target, AttributedString attributed, String text, AwtFont gdiFont,
			TextAdvances advances, boolean rightToLeft, int x, int y) {
		if (advances != null) {
			drawAttributedTextWithAdvances(target, text, gdiFont, advances, rightToLeft, x, y);
		} else {
			target.drawString(attributed.getIterator(), x, y);
		}
	}

	private void drawAttributedTextWithAdvances(Graphics2D target, String text, AwtFont gdiFont, TextAdvances advances,
			boolean rightToLeft, int x, int y) {
		double ax = x;
		double ay = y;
		int start = rightToLeft ? text.length() - 1 : 0;
		int end = rightToLeft ? -1 : text.length();
		int step = rightToLeft ? -1 : 1;
		for (int i = start; i != end; i += step) {
			AttributedString ch = new AttributedString(text.substring(i, i + 1));
			ch.addAttribute(TextAttribute.FONT, target.getFont());
			if (gdiFont != null && gdiFont.isUnderlined()) {
				ch.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
			}
			if (gdiFont != null && gdiFont.isStrikedOut()) {
				ch.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
			}
			target.drawString(ch.getIterator(), (float) ax, (float) ay);
			ax += advances.x[i];
			if (advances.y != null) {
				ay += advances.y[i];
			}
		}
	}

	private Object getTextRunDirection(AwtFont font, int align, int options) {
		if (!isMiddleEasternFont(font)) {
			return null;
		}
		return ((align & Gdi.TA_RTLREADING) != 0 || (options & Gdi.ETO_RTLREADING) != 0)
				? TextAttribute.RUN_DIRECTION_RTL
				: TextAttribute.RUN_DIRECTION_LTR;
	}

	private boolean isMiddleEasternFont(AwtFont font) {
		if (font == null) {
			return false;
		}
		int charset = font.getCharset();
		return charset == GdiFont.HEBREW_CHARSET || charset == GdiFont.ARABIC_CHARSET;
	}

	private int toLogicalTextAdvanceX(double deviceWidth) {
		double unit = rx(1.0);
		if (unit == 0.0) {
			return (int) Math.round(deviceWidth);
		}
		return (int) Math.round(deviceWidth / unit);
	}

	private int toLogicalTextAdvanceY(double deviceHeight) {
		double unit = ry(1.0);
		if (unit == 0.0) {
			return (int) Math.round(deviceHeight);
		}
		return (int) Math.round(deviceHeight / unit);
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
		String name = resolveFontFamily(gdiFont.getFaceName(),
				GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
		Font font = new Font(name, style, size);
		return fitFontHeight(font, gdiFont.getHeight(), size);
	}

	static String resolveFontFamily(String requestedName, String[] availableFamilies) {
		String name = normalizeFontFamilyName(requestedName);
		if (name.length() == 0 || isLogicalFontFamily(name)) {
			return Font.SANS_SERIF;
		}

		String available = findAvailableFontFamily(name, availableFamilies);
		if (available != null) {
			return available;
		}

		String[] alternatives = findLogicalFontFallbacks(name);
		if (alternatives != null) {
			for (int i = 0; i < alternatives.length; i++) {
				if (isLogicalFontFamily(alternatives[i])) {
					continue;
				}
				available = findAvailableFontFamily(alternatives[i], availableFamilies);
				if (available != null) {
					return available;
				}
			}
		}
		return Font.SANS_SERIF;
	}

	private static String normalizeFontFamilyName(String name) {
		if (name == null) {
			return "";
		}
		name = name.trim();
		if (name.startsWith("@")) {
			name = name.substring(1);
		}
		return name;
	}

	private static String findAvailableFontFamily(String name, String[] availableFamilies) {
		if (availableFamilies == null) {
			return null;
		}
		for (int i = 0; i < availableFamilies.length; i++) {
			if (name.equalsIgnoreCase(availableFamilies[i])) {
				return availableFamilies[i];
			}
		}
		return null;
	}

	private static String[] findLogicalFontFallbacks(String name) {
		ArrayList<String> alternatives = new ArrayList<String>(FontUtil.alternativeFonts(name));
		if (alternatives.isEmpty()) {
			return null;
		}
		return alternatives.toArray(new String[alternatives.size()]);
	}

	private static boolean isLogicalFontFamily(String name) {
		return Font.SERIF.equalsIgnoreCase(name) || Font.SANS_SERIF.equalsIgnoreCase(name)
				|| Font.MONOSPACED.equalsIgnoreCase(name) || "monospace".equalsIgnoreCase(name)
				|| "sans-serif".equalsIgnoreCase(name) || "cursive".equalsIgnoreCase(name)
				|| "fantasy".equalsIgnoreCase(name);
	}

	private Font fitFontHeight(Font font, int logicalHeight, int targetHeight) {
		if (graphics == null || logicalHeight == 0 || targetHeight <= 0) {
			return font;
		}
		FontMetrics metrics = graphics.getFontMetrics(font);
		int actualHeight = logicalHeight < 0 ? metrics.getAscent() + metrics.getDescent() : metrics.getHeight();
		if (actualHeight <= 0) {
			return font;
		}
		float fittedSize = Math.max(1.0f, font.getSize2D() * targetHeight / actualHeight);
		return font.deriveFont(fittedSize);
	}

	private String mapSymbolText(AwtFont font, String text) {
		if (replaceSymbolFont && font != null && SymbolFontMappings.isMappedFont(font.getFaceName())) {
			return SymbolFontMappings.replace(font.getFaceName(), text);
		}
		return text;
	}

	private Font toSymbolReplacementFont(Font font) {
		Font replacement = new Font(getSymbolReplacementFontFamily(), font.getStyle(), Math.max(1, font.getSize()));
		return replacement.deriveFont(font.getSize2D());
	}

	private String getSymbolReplacementFontFamily() {
		String[] candidates = {"Noto Sans Symbols 2", "Noto Sans Symbols", "Noto Sans", Font.SANS_SERIF};
		String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (int i = 0; i < candidates.length; i++) {
			for (int j = 0; j < families.length; j++) {
				if (candidates[i].equalsIgnoreCase(families[j])) {
					return families[j];
				}
			}
		}
		return Font.SANS_SERIF;
	}

	private void drawBitmap(byte[] data, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, int usage,
			Object ropOrTransparentColor, boolean preserveAlpha) {
		ensureGraphics();
		if (shouldSuppressEmfPlusFallback()) {
			return;
		}
		if (data == null || data.length == 0) {
			return;
		}
		Integer transparentColor = ropOrTransparentColor instanceof Integer ? (Integer) ropOrTransparentColor : null;
		Long rop = ropOrTransparentColor instanceof Long ? (Long) ropOrTransparentColor : Long.valueOf(Gdi.SRCCOPY);
		if (rop.longValue() == Gdi.PATCOPY) {
			patBlt(dx, dy, dw, dh, rop.longValue());
			return;
		}
		int rop3 = getBitmapRop3(rop.longValue());
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
		int x1 = ixi(dx);
		int y1 = iyi(dy);
		int x2 = ixi(dx + dw);
		int y2 = iyi(dy + dh);
		ensureCanvasContains(x1, y1, x2, y2);
		if (rop3 == getBitmapRop3(Gdi.SRCCOPY)) {
			Object oldInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, getStretchBltInterpolationHint());
			graphics.drawImage(subImage, x1, y1, x2, y2, 0, 0, srcW, srcH, null);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
		} else {
			composeBitmapRop(subImage, x1, y1, x2, y2, rop3);
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

		int x1 = ixi(dx);
		int y1 = iyi(dy);
		int x2 = ixi(dx + dw);
		int y2 = iyi(dy + dh);
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
			bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, getStretchBltInterpolationHint());
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
		int x1 = ixi(dx);
		int y1 = iyi(dy);
		int x2 = ixi(dx + srcW);
		int y2 = iyi(dy + srcH);
		ensureCanvasContains(x1, y1, x2, y2);
		Object oldInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, getStretchBltInterpolationHint());
			graphics.drawImage(subImage, x1, y1, x2, y2, 0, 0, srcW, srcH, null);
		} finally {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
		}
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

		double x0 = ix(points[0].x);
		double y0 = iy(points[0].y);
		double x1 = ix(points[1].x);
		double y1 = iy(points[1].y);
		double x2 = ix(points[2].x);
		double y2 = iy(points[2].y);
		Path2D.Double bounds = new Path2D.Double();
		bounds.moveTo(x0, y0);
		bounds.lineTo(x1, y1);
		bounds.lineTo(x1 + x2 - x0, y1 + y2 - y0);
		bounds.lineTo(x2, y2);
		bounds.closePath();
		ensureCanvasContains(bounds);

		AffineTransform old = graphics.getTransform();
		Object oldInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		try {
			graphics.transform(new AffineTransform((x1 - x0) / srcW, (y1 - y0) / srcW, (x2 - x0) / srcH,
					(y2 - y0) / srcH, x0, y0));
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, getStretchBltInterpolationHint());
			graphics.drawImage(source.getSubimage(srcX, srcY, srcW, srcH), 0, 0, null);
		} finally {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
			graphics.setTransform(old);
		}
	}

	private void drawMaskedBitmapRop(byte[] data, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh,
			byte[] mask, int mx, int my, long rop, Integer sourceBackgroundColor) {
		ensureGraphics();
		if (data == null || data.length == 0) {
			return;
		}
		BufferedImage source = decodeBitmap(data, Gdi.DIB_RGB_COLORS, sh < 0, null, true);
		BufferedImage maskImage = decodeBitmap(mask, Gdi.DIB_RGB_COLORS, false, null, false);
		if (source == null || maskImage == null) {
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
		int x1 = ixi(dx);
		int y1 = iyi(dy);
		int x2 = ixi(dx + dw);
		int y2 = iyi(dy + dh);
		ensureCanvasContains(x1, y1, x2, y2);

		int foregroundRop3 = getBitmapRop3(rop & 0x00FFFFFFL);
		int backgroundRop3 = (rop & 0xFF000000L) != 0 ? (int) ((rop >>> 24) & 0xFF) : 0xAA;
		BufferedImage subImage = source.getSubimage(srcX, srcY, srcW, srcH);
		Integer sourceBackgroundRgb = sourceBackgroundColor != null
				? Integer.valueOf(toRgb(sourceBackgroundColor.intValue()))
				: null;
		if (isMaskBltCopySourceOnZeroMask(rop)) {
			sourceBackgroundRgb = Integer.valueOf(subImage.getRGB(0, 0) & 0x00FFFFFF);
		}
		composeMaskedBitmapRop(subImage, maskImage, mx, my, x1, y1, x2, y2, foregroundRop3, backgroundRop3,
				sourceBackgroundRgb);
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

	private int getBitmapRop3(long rop) {
		if ((rop & 0xFF000000L) != 0) {
			return (int) ((rop >>> 24) & 0xFF);
		}
		return (int) ((rop >>> 16) & 0xFF);
	}

	private int toRgb(int gdiColor) {
		return ((gdiColor & 0xFF) << 16) | (gdiColor & 0x0000FF00) | ((gdiColor >>> 16) & 0xFF);
	}

	private boolean isMaskBltCopySourceOnZeroMask(long rop) {
		return (rop & 0xFF000000L) == 0xCC000000L && (rop & 0x00FFFFFFL) == 0x00AA0029L;
	}

	private boolean canPatBltWithoutSource(long rop) {
		return rop == Gdi.BLACKNESS || rop == Gdi.DSTINVERT || rop == Gdi.PATCOPY || rop == Gdi.PATINVERT
				|| rop == Gdi.WHITENESS;
	}

	private void composeBitmapRop(Image source, int x1, int y1, int x2, int y2, int rop3) {
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
			sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, getStretchBltInterpolationHint());
			sg.drawImage(source, x1 < x2 ? 0 : width, y1 < y2 ? 0 : height, x1 < x2 ? width : 0, y1 < y2 ? height : 0,
					null);
		} finally {
			sg.dispose();
		}

		BufferedImage pat = null;
		if (usesPattern(rop3)) {
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
				if (!isInClip(x, y)) {
					continue;
				}
				int sx = x - left;
				int sy = y - top;
				int s = src.getRGB(sx, sy) & 0x00FFFFFF;
				int d = image.getRGB(x, y) & 0x00FFFFFF;
				int p = pat != null ? pat.getRGB(sx, sy) & 0x00FFFFFF : 0;
				image.setRGB(x, y, 0xFF000000 | applyBitmapRop(s, d, p, rop3));
			}
		}
	}

	private void composeMaskedBitmapRop(Image source, BufferedImage maskImage, int mx, int my, int x1, int y1, int x2,
			int y2, int foregroundRop3, int backgroundRop3, Integer sourceBackgroundRgb) {
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
			sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, getStretchBltInterpolationHint());
			sg.drawImage(source, x1 < x2 ? 0 : width, y1 < y2 ? 0 : height, x1 < x2 ? width : 0, y1 < y2 ? height : 0,
					null);
		} finally {
			sg.dispose();
		}

		BufferedImage pat = null;
		if (usesPattern(foregroundRop3) || usesPattern(backgroundRop3)) {
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
				if (!isInClip(x, y)) {
					continue;
				}
				int sx = x - left;
				int sy = y - top;
				int maskX = positiveModulo(mx + sx * maskImage.getWidth() / width, maskImage.getWidth());
				int maskY = positiveModulo(my + sy * maskImage.getHeight() / height, maskImage.getHeight());
				int maskRgb = maskImage.getRGB(maskX, maskY);
				int maskValue = (((maskRgb >>> 16) & 0xFF) + ((maskRgb >>> 8) & 0xFF) + (maskRgb & 0xFF)) / 3;
				int rop3 = maskValue >= 128 ? foregroundRop3 : backgroundRop3;
				int s = src.getRGB(sx, sy) & 0x00FFFFFF;
				int d = image.getRGB(x, y) & 0x00FFFFFF;
				int p = pat != null ? pat.getRGB(sx, sy) & 0x00FFFFFF : 0;
				if (sourceBackgroundRgb != null && rop3 == getBitmapRop3(Gdi.SRCCOPY)
						&& s == sourceBackgroundRgb.intValue()) {
					s = d;
				}
				image.setRGB(x, y, 0xFF000000 | applyBitmapRop(s, d, p, rop3));
			}
		}
	}

	private int positiveModulo(int value, int divisor) {
		int result = value % divisor;
		return result < 0 ? result + divisor : result;
	}

	private boolean usesPattern(int rop3) {
		return (rop3 & 0x0F) != ((rop3 >>> 4) & 0x0F);
	}

	private Object getStretchBltInterpolationHint() {
		return dc.getStretchBltMode() == Gdi.STRETCH_HALFTONE
				? RenderingHints.VALUE_INTERPOLATION_BILINEAR
				: RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
	}

	private BufferedImage createPatternImage(int left, int top, int width, int height) {
		BufferedImage pattern = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D pg = pattern.createGraphics();
		try {
			configureGraphics(pg);
			pg.translate(-left, -top);
			pg.setPaint(toPaint(dc.getBrush()));
			pg.fillRect(left, top, width, height);
		} finally {
			pg.dispose();
		}
		return pattern;
	}

	private boolean isCanvasPixel(int x, int y) {
		return x >= 0 && y >= 0 && x < canvasWidth && y < canvasHeight;
	}

	private int[] toDeviceBounds(Rectangle2D bounds) {
		return new int[]{Math.max(0, (int) Math.floor(bounds.getMinX() - canvasMinX)),
				Math.max(0, (int) Math.floor(bounds.getMinY() - canvasMinY)),
				Math.min(canvasWidth, (int) Math.ceil(bounds.getMaxX() - canvasMinX)),
				Math.min(canvasHeight, (int) Math.ceil(bounds.getMaxY() - canvasMinY))};
	}

	private double toLogicalX(int deviceX) {
		return deviceX + canvasMinX;
	}

	private double toLogicalY(int deviceY) {
		return deviceY + canvasMinY;
	}

	private int toLogicalXi(int deviceX) {
		return deviceX + canvasMinX;
	}

	private int toLogicalYi(int deviceY) {
		return deviceY + canvasMinY;
	}

	private boolean isDeviceInClip(int x, int y) {
		Shape clip = graphics.getClip();
		return clip == null || clip.contains(toLogicalX(x) + 0.5, toLogicalY(y) + 0.5);
	}

	private boolean isInClip(int x, int y) {
		Shape clip = graphics.getClip();
		return clip == null || clip.contains(x + 0.5, y + 0.5);
	}

	private boolean isFloodFillTarget(int x, int y, int targetRgb, boolean surfaceFill) {
		boolean matches = isOpaqueColorMatch(x, y, targetRgb);
		return surfaceFill ? matches : !matches;
	}

	private boolean isOpaqueColorMatch(int x, int y, int targetRgb) {
		int argb = image.getRGB(x, y);
		return ((argb >>> 24) & 0xFF) != 0 && (argb & 0x00FFFFFF) == targetRgb;
	}

	private void addFloodFillNeighbor(ArrayDeque<Integer> queue, boolean[] visited, int x, int y) {
		if (!isCanvasPixel(x, y)) {
			return;
		}
		int index = y * canvasWidth + x;
		if (!visited[index]) {
			queue.add(Integer.valueOf(index));
		}
	}

	private void fillDevicePixel(int x, int y, GdiBrush brush) {
		if (isNullBrush(brush)) {
			return;
		}
		Paint old = graphics.getPaint();
		graphics.setPaint(toPaint(brush));
		graphics.fillRect(x, y, 1, 1);
		graphics.setPaint(old);
	}

	private boolean isNullBrush(GdiBrush brush) {
		return brush == null || brush.getStyle() == GdiBrush.BS_NULL || brush.getStyle() == GdiBrush.BS_HOLLOW;
	}

	private int applyBitmapRop(int s, int d, int p, int rop3) {
		int rgb = 0;
		for (int bit = 0; bit < 24; bit++) {
			int mask = 1 << bit;
			int index = ((p & mask) != 0 ? 4 : 0) | ((s & mask) != 0 ? 2 : 0) | ((d & mask) != 0 ? 1 : 0);
			if (((rop3 >>> index) & 1) != 0) {
				rgb |= mask;
			}
		}
		return rgb & 0x00FFFFFF;
	}

	private BufferedImage decodeBitmap(byte[] data, int usage, boolean reverse, Integer transparentColor,
			boolean preserveAlpha) {
		data = applyMonochromePatternColors(data, usage);
		BufferedImage decoded = decodeWmfBitmap(data, reverse, transparentColor, preserveAlpha);
		if (decoded == null) {
			decoded = decodeDib(applyPaletteToDib(data, usage), reverse, transparentColor, preserveAlpha,
					usage != Gdi.DIB_PAL_COLORS);
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

	private byte[] applyMonochromePatternColors(byte[] dib, int usage) {
		if (usage != 3 || dib == null || dib.length < 48) {
			return dib;
		}
		int headerSize = readInt32(dib, 0);
		if (headerSize < 40 || dib.length < headerSize + 8 || readUInt16(dib, 14) != 1
				|| getDibColorCount(dib, headerSize, 1) < 2) {
			return dib;
		}
		byte[] rgbDib = dib.clone();
		writeRgbQuad(rgbDib, headerSize, dc.getTextColor());
		writeRgbQuad(rgbDib, headerSize + 4, dc.getBkColor());
		return rgbDib;
	}

	private void writeRgbQuad(byte[] dib, int offset, int color) {
		dib[offset] = (byte) ((color >>> 16) & 0xFF);
		dib[offset + 1] = (byte) ((color >>> 8) & 0xFF);
		dib[offset + 2] = (byte) (color & 0xFF);
		dib[offset + 3] = 0;
	}

	private Rectangle2D toRectangle(int x, int y, int width, int height) {
		double x1 = ix(x);
		double y1 = iy(y);
		double x2 = ix(x + width);
		double y2 = iy(y + height);
		return new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
	}

	private Polygon toPolygon(Point[] points) {
		if (points == null || points.length == 0) {
			return null;
		}
		Polygon polygon = new Polygon();
		for (int i = 0; i < points.length; i++) {
			polygon.addPoint(ixi(points[i].x), iyi(points[i].y));
		}
		return polygon;
	}

	private Path2D.Double toPolyline(Point[] points) {
		if (points == null || points.length == 0) {
			return null;
		}
		Path2D.Double path = new Path2D.Double();
		path.moveTo(ix(points[0].x), iy(points[0].y));
		for (int i = 1; i < points.length; i++) {
			path.lineTo(ix(points[i].x), iy(points[i].y));
		}
		return path;
	}

	private int ixi(double x) {
		return (int) ix(x);
	}

	private int iyi(double y) {
		return (int) iy(y);
	}

	private double ix(double x) {
		return Math.round(tx(x));
	}

	private double iy(double y) {
		return Math.round(ty(y));
	}

	private double irx(double x) {
		return Math.round(rx(x));
	}

	private double iry(double y) {
		return Math.round(ry(y));
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

	private boolean isMonochromeWmfBitmap(byte[] bitmap) {
		return bitmap != null && bitmap.length >= 10 && readUInt16(bitmap, 0) == 0 && (bitmap[8] & 0xFF) > 0
				&& (bitmap[9] & 0xFF) == 1;
	}

	private BufferedImage decodeDib(byte[] dib, boolean reverse, Integer transparentColor, boolean preserveAlpha,
			boolean useDefaultMissingColorTable) {
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
		boolean missingColorTable = false;
		if (colorCount > 0 && dib.length < bitsOffset + stride * height && dib.length >= headerSize + stride * height) {
			missingColorTable = true;
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
		} else if (missingColorTable && bitCount == 1 && useDefaultMissingColorTable) {
			colors = new int[]{0xFF000000, 0xFFFFFFFF};
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
			return colors != null && index < colors.length ? colors[index] : index == 0 ? 0xFFFFFFFF : 0xFF000000;
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

	private boolean shouldSuppressEmfPlusFallback() {
		return suppressEmfPlusFallback && !emfPlusGetDCActive;
	}

	private EmfPlusBrush getEmfPlusBrush(int flags, int brushId) {
		if ((flags & EMF_PLUS_FLAG_SOLID_COLOR) != 0) {
			return new EmfPlusBrush(brushId);
		}
		return emfPlusBrushes.get(Integer.valueOf(brushId));
	}

	private EmfPlusBrush readEmfPlusBrush(byte[] data, int offset) {
		if (data.length < offset + 12) {
			return null;
		}
		int brushType = readInt32(data, offset + 4);
		if (brushType == EMF_PLUS_BRUSH_TYPE_SOLID_COLOR) {
			return new EmfPlusBrush(readInt32(data, offset + 8));
		}
		if (brushType == EMF_PLUS_BRUSH_TYPE_HATCH_FILL) {
			if (data.length < offset + 20) {
				return null;
			}
			return new EmfPlusBrush(readInt32(data, offset + 8), readInt32(data, offset + 12),
					readInt32(data, offset + 16));
		}
		if (brushType == EMF_PLUS_BRUSH_TYPE_TEXTURE_FILL) {
			if (data.length < offset + 16) {
				return null;
			}
			int brushDataFlags = readInt32(data, offset + 8);
			int optionalOffset = offset + 16;
			double[] brushTransform = null;
			if ((brushDataFlags & EMF_PLUS_BRUSH_DATA_TRANSFORM) != 0) {
				if (data.length < optionalOffset + 24) {
					return null;
				}
				brushTransform = new double[]{readFloat(data, optionalOffset), readFloat(data, optionalOffset + 4),
						readFloat(data, optionalOffset + 8), readFloat(data, optionalOffset + 12),
						readFloat(data, optionalOffset + 16), readFloat(data, optionalOffset + 20)};
				optionalOffset += 24;
			}
			BufferedImage texture = readEmfPlusBitmapImage(data, optionalOffset);
			return texture != null ? new EmfPlusBrush(texture, brushTransform) : null;
		}
		if (brushType == EMF_PLUS_BRUSH_TYPE_PATH_GRADIENT) {
			if (data.length < offset + 32) {
				return null;
			}
			int brushDataFlags = readInt32(data, offset + 8);
			int wrapMode = readInt32(data, offset + 12);
			int centerColor = readInt32(data, offset + 16);
			double[] center = new double[]{readFloat(data, offset + 20), readFloat(data, offset + 24)};
			int colorCount = readInt32(data, offset + 28);
			if (colorCount < 1 || data.length < offset + 32 + colorCount * 4) {
				return null;
			}
			int surroundColor = readInt32(data, offset + 32);
			int boundaryOffset = offset + 32 + colorCount * 4;
			double[] bounds = readEmfPlusPathGradientBounds(data, boundaryOffset, brushDataFlags);
			if (bounds == null) {
				return null;
			}
			if ((brushDataFlags & EMF_PLUS_BRUSH_DATA_PATH) != 0) {
				boundaryOffset += 4 + readInt32(data, boundaryOffset);
			} else {
				boundaryOffset += 4 + readInt32(data, boundaryOffset) * 8;
			}

			double[] brushTransform = null;
			if ((brushDataFlags & EMF_PLUS_BRUSH_DATA_TRANSFORM) != 0 && data.length >= boundaryOffset + 24) {
				brushTransform = new double[]{readFloat(data, boundaryOffset), readFloat(data, boundaryOffset + 4),
						readFloat(data, boundaryOffset + 8), readFloat(data, boundaryOffset + 12),
						readFloat(data, boundaryOffset + 16), readFloat(data, boundaryOffset + 20)};
				boundaryOffset += 24;
			}
			double focusScaleX = 0.0;
			double focusScaleY = 0.0;
			if ((brushDataFlags & EMF_PLUS_BRUSH_DATA_FOCUS_SCALES) != 0) {
				if (data.length < boundaryOffset + 8) {
					return null;
				}
				focusScaleX = readFloat(data, boundaryOffset);
				focusScaleY = readFloat(data, boundaryOffset + 4);
				boundaryOffset += 8;
			}

			double[] blendPositions = null;
			int[] blendColors = null;
			if ((brushDataFlags & EMF_PLUS_BRUSH_DATA_PRESET_COLORS) != 0) {
				EmfPlusBlendColors colors = readEmfPlusBlendColors(data, boundaryOffset);
				if (colors != null) {
					blendPositions = colors.positions;
					blendColors = colors.colors;
				}
			} else if ((brushDataFlags
					& (EMF_PLUS_BRUSH_DATA_BLEND_FACTORS_H | EMF_PLUS_BRUSH_DATA_BLEND_FACTORS_V)) != 0) {
				EmfPlusBlendFactors factors = readEmfPlusBlendFactors(data, boundaryOffset);
				if (factors != null) {
					blendPositions = factors.positions;
					blendColors = toEmfPlusBlendFactorColors(centerColor, surroundColor, factors.factors);
				}
			}
			boolean gammaCorrected = (brushDataFlags & EMF_PLUS_BRUSH_DATA_IS_GAMMA_CORRECTED) != 0;
			return new EmfPlusBrush(center, bounds, centerColor, surroundColor, blendPositions, blendColors,
					brushTransform, wrapMode, gammaCorrected, focusScaleX, focusScaleY);
		}
		if (brushType == EMF_PLUS_BRUSH_TYPE_LINEAR_GRADIENT) {
			if (data.length < offset + 48) {
				return null;
			}
			int brushDataFlags = readInt32(data, offset + 8);
			int wrapMode = readInt32(data, offset + 12);
			double[] rect = new double[]{readFloat(data, offset + 16), readFloat(data, offset + 20),
					readFloat(data, offset + 24), readFloat(data, offset + 28)};
			int startColor = readInt32(data, offset + 32);
			int endColor = readInt32(data, offset + 36);
			int optionalOffset = offset + 48;
			double[] brushTransform = null;
			double[] blendPositions = null;
			int[] blendColors = null;
			if ((brushDataFlags & EMF_PLUS_BRUSH_DATA_TRANSFORM) != 0) {
				if (data.length < optionalOffset + 24) {
					return null;
				}
				brushTransform = new double[]{readFloat(data, optionalOffset), readFloat(data, optionalOffset + 4),
						readFloat(data, optionalOffset + 8), readFloat(data, optionalOffset + 12),
						readFloat(data, optionalOffset + 16), readFloat(data, optionalOffset + 20)};
				optionalOffset += 24;
			}
			if ((brushDataFlags & EMF_PLUS_BRUSH_DATA_PRESET_COLORS) != 0) {
				EmfPlusBlendColors colors = readEmfPlusBlendColors(data, optionalOffset);
				if (colors != null) {
					blendPositions = colors.positions;
					blendColors = colors.colors;
				}
			} else if ((brushDataFlags
					& (EMF_PLUS_BRUSH_DATA_BLEND_FACTORS_H | EMF_PLUS_BRUSH_DATA_BLEND_FACTORS_V)) != 0) {
				EmfPlusBlendFactors factors = readEmfPlusBlendFactors(data, optionalOffset);
				if (factors != null) {
					blendPositions = factors.positions;
					blendColors = toEmfPlusBlendFactorColors(startColor, endColor, factors.factors);
				}
			}
			boolean gammaCorrected = (brushDataFlags & EMF_PLUS_BRUSH_DATA_IS_GAMMA_CORRECTED) != 0;
			return new EmfPlusBrush(rect, startColor, endColor, blendPositions, blendColors, brushTransform, wrapMode,
					gammaCorrected);
		}
		return null;
	}

	private double[] readEmfPlusPathGradientBounds(byte[] data, int offset, int brushDataFlags) {
		if ((brushDataFlags & EMF_PLUS_BRUSH_DATA_PATH) != 0) {
			if (data.length < offset + 4) {
				return null;
			}
			int boundarySize = readInt32(data, offset);
			if (boundarySize < 0 || data.length < offset + 4 + boundarySize) {
				return null;
			}
			byte[] pathData = new byte[boundarySize];
			System.arraycopy(data, offset + 4, pathData, 0, boundarySize);
			EmfPlusPath path = readEmfPlusPath(pathData);
			return path != null ? getEmfPlusPointBounds(path.points) : null;
		}

		if (data.length < offset + 4) {
			return null;
		}
		int pointCount = readInt32(data, offset);
		double[][] points = readEmfPlusDrawingPoints(data, offset + 4, pointCount, false);
		return points != null ? getEmfPlusPointBounds(points) : null;
	}

	private EmfPlusBlendColors readEmfPlusBlendColors(byte[] data, int offset) {
		if (data.length < offset + 4) {
			return null;
		}
		int count = readInt32(data, offset);
		if (count < 2 || data.length - offset - 4 < count * 8) {
			return null;
		}
		double[] positions = new double[count];
		int[] colors = new int[count];
		for (int i = 0; i < count; i++) {
			positions[i] = readFloat(data, offset + 4 + i * 4);
		}
		int colorsOffset = offset + 4 + count * 4;
		for (int i = 0; i < count; i++) {
			colors[i] = readInt32(data, colorsOffset + i * 4);
		}
		return new EmfPlusBlendColors(positions, colors);
	}

	private EmfPlusBlendFactors readEmfPlusBlendFactors(byte[] data, int offset) {
		if (data.length < offset + 4) {
			return null;
		}
		int count = readInt32(data, offset);
		if (count < 2 || data.length - offset - 4 < count * 8) {
			return null;
		}
		double[] positions = new double[count];
		double[] factors = new double[count];
		for (int i = 0; i < count; i++) {
			positions[i] = readFloat(data, offset + 4 + i * 4);
		}
		int factorsOffset = offset + 4 + count * 4;
		for (int i = 0; i < count; i++) {
			factors[i] = readFloat(data, factorsOffset + i * 4);
		}
		return new EmfPlusBlendFactors(positions, factors);
	}

	private int[] toEmfPlusBlendFactorColors(int startColor, int endColor, double[] factors) {
		int[] colors = new int[factors.length];
		for (int i = 0; i < factors.length; i++) {
			double factor = Math.max(0.0, Math.min(1.0, factors[i]));
			colors[i] = interpolateEmfPlusColor(startColor, endColor, factor);
		}
		return colors;
	}

	private int interpolateEmfPlusColor(int startColor, int endColor, double startWeight) {
		int a = interpolateEmfPlusChannel(startColor >>> 24, endColor >>> 24, startWeight);
		int r = interpolateEmfPlusChannel(startColor >>> 16, endColor >>> 16, startWeight);
		int g = interpolateEmfPlusChannel(startColor >>> 8, endColor >>> 8, startWeight);
		int b = interpolateEmfPlusChannel(startColor, endColor, startWeight);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private int interpolateEmfPlusChannel(int startColor, int endColor, double startWeight) {
		int start = startColor & 0xFF;
		int end = endColor & 0xFF;
		return (int) Math.round(end + (start - end) * startWeight);
	}

	private EmfPlusPen readEmfPlusPen(byte[] payload) {
		if (payload.length < 32 || readInt32(payload, 4) != 0) {
			return null;
		}
		int penDataFlags = readInt32(payload, 8);
		int penUnit = readInt32(payload, 12);
		double width = readFloat(payload, 16) * toEmfPlusPageUnitScale(penUnit);
		int optionalOffset = 20;
		int startCap = 0;
		int endCap = 0;
		int lineJoin = 0;
		double miterLimit = 0.0;
		int lineStyle = 0;
		int dashCap = 0;
		double dashOffset = 0.0;
		double[] dashPattern = null;
		double[] penTransform = null;
		EmfPlusCustomLineCap customStartCap = null;
		EmfPlusCustomLineCap customEndCap = null;
		if ((penDataFlags & EMF_PLUS_PEN_DATA_TRANSFORM) != 0) {
			if (payload.length < optionalOffset + 24) {
				return null;
			}
			penTransform = new double[]{readFloat(payload, optionalOffset), readFloat(payload, optionalOffset + 4),
					readFloat(payload, optionalOffset + 8), readFloat(payload, optionalOffset + 12),
					readFloat(payload, optionalOffset + 16), readFloat(payload, optionalOffset + 20)};
			optionalOffset += 24;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_START_CAP) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			startCap = readInt32(payload, optionalOffset);
			optionalOffset += 4;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_END_CAP) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			endCap = readInt32(payload, optionalOffset);
			optionalOffset += 4;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_JOIN) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			lineJoin = readInt32(payload, optionalOffset);
			optionalOffset += 4;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_MITER_LIMIT) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			miterLimit = readFloat(payload, optionalOffset);
			optionalOffset += 4;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_LINE_STYLE) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			lineStyle = readInt32(payload, optionalOffset);
			optionalOffset += 4;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_DASHED_LINE_CAP) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			dashCap = readInt32(payload, optionalOffset);
			optionalOffset += 4;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_DASHED_LINE_OFFSET) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			dashOffset = readFloat(payload, optionalOffset);
			optionalOffset += 4;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_DASHED_LINE) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			int count = readInt32(payload, optionalOffset);
			optionalOffset += 4;
			if (count < 0 || payload.length < optionalOffset + count * 4) {
				return null;
			}
			dashPattern = new double[count];
			for (int i = 0; i < count; i++) {
				dashPattern[i] = readFloat(payload, optionalOffset + i * 4);
			}
			optionalOffset += count * 4;
			lineStyle = EMF_PLUS_LINE_STYLE_CUSTOM;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_NON_CENTER) != 0) {
			optionalOffset += 4;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_COMPOUND_LINE) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			optionalOffset += 4 + Math.max(0, readInt32(payload, optionalOffset)) * 4;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_CUSTOM_START_CAP) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			int size = readInt32(payload, optionalOffset);
			if (size < 0 || payload.length < optionalOffset + 4 + size) {
				return null;
			}
			customStartCap = readEmfPlusCustomLineCap(payload, optionalOffset + 4, size);
			optionalOffset += 4 + size;
		}
		if ((penDataFlags & EMF_PLUS_PEN_DATA_CUSTOM_END_CAP) != 0) {
			if (payload.length < optionalOffset + 4) {
				return null;
			}
			int size = readInt32(payload, optionalOffset);
			if (size < 0 || payload.length < optionalOffset + 4 + size) {
				return null;
			}
			customEndCap = readEmfPlusCustomLineCap(payload, optionalOffset + 4, size);
			optionalOffset += 4 + size;
		}
		if (payload.length < optionalOffset) {
			return null;
		}
		EmfPlusBrush brush = readEmfPlusBrush(payload, optionalOffset);
		return brush != null
				? new EmfPlusPen(width, brush, startCap, endCap, lineJoin, miterLimit, lineStyle, dashCap, dashOffset,
						dashPattern, penTransform, customStartCap, customEndCap)
				: null;
	}

	private EmfPlusCustomLineCap readEmfPlusCustomLineCap(byte[] payload, int offset, int size) {
		if (size < 60 || payload.length < offset + size
				|| readInt32(payload, offset + 4) != EMF_PLUS_CUSTOM_LINE_CAP_DATA_TYPE_ADJUSTABLE_ARROW) {
			return null;
		}
		int dataOffset = offset + 8;
		double width = readFloat(payload, dataOffset);
		double height = readFloat(payload, dataOffset + 4);
		boolean fill = readInt32(payload, dataOffset + 12) != 0;
		double widthScale = readFloat(payload, dataOffset + 32);
		if (width <= 0.0 || height <= 0.0 || widthScale <= 0.0) {
			return null;
		}
		return new EmfPlusCustomLineCap(width, height, fill, widthScale);
	}

	private EmfPlusPath readEmfPlusPath(byte[] payload) {
		if (payload.length < 12) {
			return null;
		}
		int fillMode = readInt32(payload, 0);
		int count = readInt32(payload, 4);
		int pathPointFlags = readInt32(payload, 8);
		if (count < 0) {
			return null;
		}
		boolean relative = (pathPointFlags & EMF_PLUS_PATH_FLAG_RELATIVE) != 0;
		boolean compressed = (pathPointFlags & EMF_PLUS_FLAG_COMPRESSED) != 0;
		double[][] points = readEmfPlusDrawingPoints(payload, 12, count, pathPointFlags);
		if (points == null) {
			return null;
		}

		int typesOffset = 12 + count * (relative ? 2 : (compressed ? 4 : 8));
		if (payload.length < typesOffset || payload.length - typesOffset < count) {
			return null;
		}
		byte[] types = new byte[count];
		System.arraycopy(payload, typesOffset, types, 0, count);
		return new EmfPlusPath(fillMode, points, types);
	}

	private EmfPlusRegion readEmfPlusRegion(byte[] payload) {
		if (payload.length < 12) {
			return null;
		}
		int nodeCount = readInt32(payload, 4);
		if (nodeCount < 0) {
			return null;
		}
		EmfPlusRegionNode node = readEmfPlusRegionNode(payload, 8);
		return node != null ? node.region : null;
	}

	private EmfPlusRegionNode readEmfPlusRegionNode(byte[] payload, int offset) {
		if (payload.length < offset + 4) {
			return null;
		}
		int type = readInt32(payload, offset);
		if (type == EMF_PLUS_REGION_NODE_TYPE_AND || type == EMF_PLUS_REGION_NODE_TYPE_OR
				|| type == EMF_PLUS_REGION_NODE_TYPE_XOR || type == EMF_PLUS_REGION_NODE_TYPE_EXCLUDE
				|| type == EMF_PLUS_REGION_NODE_TYPE_COMPLEMENT) {
			EmfPlusRegionNode left = readEmfPlusRegionNode(payload, offset + 4);
			if (left == null) {
				return null;
			}
			EmfPlusRegionNode right = readEmfPlusRegionNode(payload, offset + 4 + left.length);
			if (right == null) {
				return null;
			}
			return new EmfPlusRegionNode(new EmfPlusRegion(type, left.region, right.region),
					4 + left.length + right.length);
		}
		if (type == EMF_PLUS_REGION_NODE_TYPE_RECT) {
			double[][] rects = readEmfPlusRects(payload, offset + 4, 1, false);
			return rects != null ? new EmfPlusRegionNode(new EmfPlusRegion(rects[0], null), 20) : null;
		}
		if (type == EMF_PLUS_REGION_NODE_TYPE_PATH) {
			if (payload.length < offset + 8) {
				return null;
			}
			int pathSize = readInt32(payload, offset + 4);
			if (pathSize < 0 || payload.length < offset + 8 + pathSize) {
				return null;
			}
			byte[] pathData = new byte[pathSize];
			System.arraycopy(payload, offset + 8, pathData, 0, pathSize);
			EmfPlusPath path = readEmfPlusPath(pathData);
			return path != null ? new EmfPlusRegionNode(new EmfPlusRegion(null, path), 8 + pathSize) : null;
		}
		if (type == EMF_PLUS_REGION_NODE_TYPE_EMPTY) {
			return new EmfPlusRegionNode(new EmfPlusRegion(true, false), 4);
		}
		if (type == EMF_PLUS_REGION_NODE_TYPE_INFINITE) {
			return new EmfPlusRegionNode(new EmfPlusRegion(false, true), 4);
		}
		return null;
	}

	private EmfPlusFont readEmfPlusFont(byte[] payload) {
		if (payload.length < 24) {
			return null;
		}
		float emSize = readFloat(payload, 4);
		int sizeUnit = readInt32(payload, 8);
		int styleFlags = readInt32(payload, 12);
		int length = readInt32(payload, 20);
		String familyName = readUtf16Le(payload, 24, length);
		return familyName != null ? new EmfPlusFont(emSize, sizeUnit, styleFlags, familyName) : null;
	}

	private EmfPlusStringFormat readEmfPlusStringFormat(byte[] payload) {
		if (payload.length < 60) {
			return null;
		}
		int flags = readInt32(payload, 4);
		int alignment = readInt32(payload, 12);
		int lineAlign = readInt32(payload, 16);
		int hotkeyPrefix = readInt32(payload, 32);
		double tracking = readFloat(payload, 44);
		return new EmfPlusStringFormat(flags, alignment, lineAlign, hotkeyPrefix, tracking);
	}

	private EmfPlusImageAttributes readEmfPlusImageAttributes(byte[] payload) {
		if (payload.length < 24) {
			return null;
		}
		int wrapMode = readInt32(payload, 8);
		int clampColor = readInt32(payload, 12);
		int objectClamp = readInt32(payload, 16);
		return new EmfPlusImageAttributes(wrapMode, clampColor, objectClamp);
	}

	private EmfPlusImageEffect readEmfPlusSerializableObject(byte[] payload) {
		if (payload.length < 20) {
			return null;
		}
		int bufferSize = readInt32(payload, 16);
		if (bufferSize < 0 || payload.length < 20 + bufferSize) {
			return null;
		}
		if (isEmfPlusColorMatrixEffectGuid(payload, 0) && bufferSize >= 100) {
			return new EmfPlusImageEffect(readEmfPlusColorMatrixEffect(payload, 20), null);
		}
		if (isEmfPlusColorLookupTableEffectGuid(payload, 0) && bufferSize >= 1024) {
			return new EmfPlusImageEffect(null, readEmfPlusColorLookupTableEffect(payload, 20));
		}
		return null;
	}

	private boolean isEmfPlusColorMatrixEffectGuid(byte[] payload, int offset) {
		return payload[offset] == 0x15 && payload[offset + 1] == 0x26 && payload[offset + 2] == (byte) 0x8F
				&& payload[offset + 3] == 0x71 && payload[offset + 4] == 0x33 && payload[offset + 5] == 0x79
				&& payload[offset + 6] == (byte) 0xE3 && payload[offset + 7] == 0x40
				&& payload[offset + 8] == (byte) 0xA5 && payload[offset + 9] == 0x11 && payload[offset + 10] == 0x5F
				&& payload[offset + 11] == 0x68 && payload[offset + 12] == (byte) 0xFE && payload[offset + 13] == 0x14
				&& payload[offset + 14] == (byte) 0xDD && payload[offset + 15] == 0x74;
	}

	private boolean isEmfPlusColorLookupTableEffectGuid(byte[] payload, int offset) {
		return payload[offset] == (byte) 0xA9 && payload[offset + 1] == 0x72 && payload[offset + 2] == (byte) 0xCE
				&& payload[offset + 3] == (byte) 0xA7 && payload[offset + 4] == 0x7F && payload[offset + 5] == 0x0F
				&& payload[offset + 6] == (byte) 0xD7 && payload[offset + 7] == 0x40
				&& payload[offset + 8] == (byte) 0xB3 && payload[offset + 9] == (byte) 0xCC
				&& payload[offset + 10] == (byte) 0xD0 && payload[offset + 11] == (byte) 0xC0
				&& payload[offset + 12] == 0x2D && payload[offset + 13] == 0x5C && payload[offset + 14] == 0x32
				&& payload[offset + 15] == 0x12;
	}

	private double[][] readEmfPlusColorMatrixEffect(byte[] payload, int offset) {
		double[][] matrix = new double[5][5];
		for (int column = 0; column < 5; column++) {
			for (int row = 0; row < 5; row++) {
				matrix[row][column] = readFloat(payload, offset + (column * 5 + row) * 4);
			}
		}
		return matrix;
	}

	private byte[][] readEmfPlusColorLookupTableEffect(byte[] payload, int offset) {
		byte[][] lookupTables = new byte[4][256];
		for (int table = 0; table < lookupTables.length; table++) {
			System.arraycopy(payload, offset + table * 256, lookupTables[table], 0, 256);
		}
		return lookupTables;
	}

	private BufferedImage readEmfPlusBitmapImage(byte[] payload, int offset) {
		int imageOffset = findBytes(payload, offset, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
		if (imageOffset < 0) {
			imageOffset = findBytes(payload, offset, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
		}
		if (imageOffset < 0) {
			imageOffset = findBytes(payload, offset, new byte[]{'G', 'I', 'F', '8'});
		}
		if (imageOffset < 0) {
			imageOffset = findBytes(payload, offset, new byte[]{'I', 'I', 0x2A, 0x00});
		}
		if (imageOffset < 0) {
			imageOffset = findBytes(payload, offset, new byte[]{'M', 'M', 0x00, 0x2A});
		}
		if (imageOffset >= 0) {
			try {
				return ImageIO.read(new ByteArrayInputStream(payload, imageOffset, payload.length - imageOffset));
			} catch (IOException e) {
				return null;
			}
		}

		if (offset < 0 || payload.length < offset + 20) {
			return null;
		}
		int width = readInt32(payload, offset);
		int height = readInt32(payload, offset + 4);
		int stride = readInt32(payload, offset + 8);
		int pixelFormat = readInt32(payload, offset + 12);
		int type = readInt32(payload, offset + 16);
		if (type != EMF_PLUS_BITMAP_DATA_TYPE_PIXEL || width <= 0 || height == 0 || stride == 0) {
			return null;
		}

		int rowCount = Math.abs(height);
		int rowStride = Math.abs(stride);
		int pixelDataOffset = offset + 20;
		int[] palette = null;
		if ((pixelFormat & EMF_PLUS_PIXEL_FORMAT_INDEXED) != 0) {
			if (payload.length < pixelDataOffset + 8) {
				return null;
			}
			int paletteCount = readInt32(payload, pixelDataOffset + 4);
			if (paletteCount <= 0 || paletteCount > 256 || payload.length < pixelDataOffset + 8 + paletteCount * 4) {
				return null;
			}
			palette = new int[paletteCount];
			for (int i = 0; i < paletteCount; i++) {
				palette[i] = readInt32(payload, pixelDataOffset + 8 + i * 4);
			}
			pixelDataOffset += 8 + paletteCount * 4;
		}
		if ((long) rowStride * rowCount > payload.length - pixelDataOffset) {
			return null;
		}

		BufferedImage bitmap = new BufferedImage(width, rowCount, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < rowCount; y++) {
			int rowOffset = pixelDataOffset + (stride < 0 ? rowCount - 1 - y : y) * rowStride;
			for (int x = 0; x < width; x++) {
				Integer argb = readEmfPlusPixel(payload, rowOffset, x, pixelFormat, rowStride, palette);
				if (argb == null) {
					return null;
				}
				bitmap.setRGB(x, y, argb.intValue());
			}
		}
		return bitmap;
	}

	private Integer readEmfPlusPixel(byte[] payload, int rowOffset, int x, int pixelFormat, int rowStride,
			int[] palette) {
		if ((pixelFormat & EMF_PLUS_PIXEL_FORMAT_INDEXED) != 0) {
			return readEmfPlusIndexedPixel(payload, rowOffset, x, pixelFormat, rowStride, palette);
		}

		int pixelOffset;
		int a = 0xFF;
		int r;
		int g;
		int b;
		if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_24BPP_RGB) {
			pixelOffset = rowOffset + x * 3;
			if (x * 3 + 2 >= rowStride || payload.length < pixelOffset + 3) {
				return null;
			}
			b = payload[pixelOffset] & 0xFF;
			g = payload[pixelOffset + 1] & 0xFF;
			r = payload[pixelOffset + 2] & 0xFF;
		} else if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_32BPP_RGB || pixelFormat == EMF_PLUS_PIXEL_FORMAT_32BPP_ARGB
				|| pixelFormat == EMF_PLUS_PIXEL_FORMAT_32BPP_PARGB) {
			pixelOffset = rowOffset + x * 4;
			if (x * 4 + 3 >= rowStride || payload.length < pixelOffset + 4) {
				return null;
			}
			b = payload[pixelOffset] & 0xFF;
			g = payload[pixelOffset + 1] & 0xFF;
			r = payload[pixelOffset + 2] & 0xFF;
			if (pixelFormat != EMF_PLUS_PIXEL_FORMAT_32BPP_RGB) {
				a = payload[pixelOffset + 3] & 0xFF;
				if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_32BPP_PARGB && a > 0 && a < 0xFF) {
					r = Math.min(0xFF, r * 0xFF / a);
					g = Math.min(0xFF, g * 0xFF / a);
					b = Math.min(0xFF, b * 0xFF / a);
				}
			}
		} else if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_16BPP_RGB_555
				|| pixelFormat == EMF_PLUS_PIXEL_FORMAT_16BPP_RGB_565
				|| pixelFormat == EMF_PLUS_PIXEL_FORMAT_16BPP_ARGB_1555
				|| pixelFormat == EMF_PLUS_PIXEL_FORMAT_16BPP_GRAYSCALE) {
			pixelOffset = rowOffset + x * 2;
			if (x * 2 + 1 >= rowStride || payload.length < pixelOffset + 2) {
				return null;
			}
			int value = readUInt16(payload, pixelOffset);
			if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_16BPP_GRAYSCALE) {
				r = expandEmfPlusChannel(value, 16);
				g = r;
				b = r;
			} else if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_16BPP_ARGB_1555) {
				a = (value & 0x8000) != 0 ? 0xFF : 0;
				r = expandEmfPlusChannel((value >>> 10) & 0x1F, 5);
				g = expandEmfPlusChannel((value >>> 5) & 0x1F, 5);
				b = expandEmfPlusChannel(value & 0x1F, 5);
			} else if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_16BPP_RGB_565) {
				r = expandEmfPlusChannel((value >>> 11) & 0x1F, 5);
				g = expandEmfPlusChannel((value >>> 5) & 0x3F, 6);
				b = expandEmfPlusChannel(value & 0x1F, 5);
			} else {
				r = expandEmfPlusChannel((value >>> 10) & 0x1F, 5);
				g = expandEmfPlusChannel((value >>> 5) & 0x1F, 5);
				b = expandEmfPlusChannel(value & 0x1F, 5);
			}
		} else if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_48BPP_RGB) {
			pixelOffset = rowOffset + x * 6;
			if (x * 6 + 5 >= rowStride || payload.length < pixelOffset + 6) {
				return null;
			}
			b = expandEmfPlusChannel(readUInt16(payload, pixelOffset), 16);
			g = expandEmfPlusChannel(readUInt16(payload, pixelOffset + 2), 16);
			r = expandEmfPlusChannel(readUInt16(payload, pixelOffset + 4), 16);
		} else if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_64BPP_ARGB
				|| pixelFormat == EMF_PLUS_PIXEL_FORMAT_64BPP_PARGB) {
			pixelOffset = rowOffset + x * 8;
			if (x * 8 + 7 >= rowStride || payload.length < pixelOffset + 8) {
				return null;
			}
			int b16 = readUInt16(payload, pixelOffset);
			int g16 = readUInt16(payload, pixelOffset + 2);
			int r16 = readUInt16(payload, pixelOffset + 4);
			int a16 = readUInt16(payload, pixelOffset + 6);
			if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_64BPP_PARGB && a16 > 0 && a16 < 0xFFFF) {
				r16 = Math.min(0xFFFF, r16 * 0xFFFF / a16);
				g16 = Math.min(0xFFFF, g16 * 0xFFFF / a16);
				b16 = Math.min(0xFFFF, b16 * 0xFFFF / a16);
			}
			a = expandEmfPlusChannel(a16, 16);
			r = expandEmfPlusChannel(r16, 16);
			g = expandEmfPlusChannel(g16, 16);
			b = expandEmfPlusChannel(b16, 16);
		} else {
			return null;
		}
		return Integer.valueOf((a << 24) | (r << 16) | (g << 8) | b);
	}

	private Integer readEmfPlusIndexedPixel(byte[] payload, int rowOffset, int x, int pixelFormat, int rowStride,
			int[] palette) {
		if (palette == null) {
			return null;
		}
		int index;
		if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_8BPP_INDEXED) {
			if (x >= rowStride || payload.length <= rowOffset + x) {
				return null;
			}
			index = payload[rowOffset + x] & 0xFF;
		} else if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_4BPP_INDEXED) {
			int pixelOffset = rowOffset + x / 2;
			if (x / 2 >= rowStride || payload.length <= pixelOffset) {
				return null;
			}
			int value = payload[pixelOffset] & 0xFF;
			index = (x & 1) == 0 ? (value >>> 4) & 0x0F : value & 0x0F;
		} else if (pixelFormat == EMF_PLUS_PIXEL_FORMAT_1BPP_INDEXED) {
			int pixelOffset = rowOffset + x / 8;
			if (x / 8 >= rowStride || payload.length <= pixelOffset) {
				return null;
			}
			int value = payload[pixelOffset] & 0xFF;
			index = (value >>> (7 - (x & 7))) & 0x01;
		} else {
			return null;
		}
		return index < palette.length ? Integer.valueOf(palette[index]) : null;
	}

	private int expandEmfPlusChannel(int value, int bits) {
		return (value * 0xFF + ((1 << bits) - 1) / 2) / ((1 << bits) - 1);
	}

	private int findBytes(byte[] data, int offset, byte[] pattern) {
		for (int i = Math.max(0, offset); i <= data.length - pattern.length; i++) {
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

	private String readUtf16Le(byte[] data, int offset, int length) {
		if (length < 0 || data.length < offset || data.length - offset < length * 2) {
			return null;
		}
		return new String(data, offset, length * 2, StandardCharsets.UTF_16LE);
	}

	private Paint toEmfPlusPaint(EmfPlusBrush brush) {
		if (brush == null) {
			return null;
		}
		if (brush.hatchStyle >= 0) {
			return createEmfPlusHatchPaint(brush);
		}
		if (brush.linearGradientRect != null) {
			double[] p1 = toEmfPlusLogicalPoint(brush.linearGradientRect[0], brush.linearGradientRect[1]);
			double[] p2 = toEmfPlusLogicalPoint(brush.linearGradientRect[0] + brush.linearGradientRect[2],
					brush.linearGradientRect[1]);
			if (brush.blendPositions != null && brush.blendColors != null) {
				return createEmfPlusLinearGradientPaint(p1, p2, brush);
			}
			return new GradientPaint((float) p1[0], (float) p1[1], toEmfPlusColor(brush.startColor), (float) p2[0],
					(float) p2[1], toEmfPlusColor(brush.endColor));
		}
		if (brush.textureImage != null) {
			return createEmfPlusTexturePaint(brush);
		}
		if (brush.pathGradientCenter != null) {
			return createEmfPlusPathGradientPaint(brush);
		}
		return toEmfPlusColor(brush.argb);
	}

	private TexturePaint createEmfPlusHatchPaint(EmfPlusBrush brush) {
		BufferedImage pattern = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = pattern.createGraphics();
		try {
			configureGraphics(g);
			g.setColor(toEmfPlusColor(brush.backColor));
			g.fillRect(0, 0, 8, 8);
			g.setColor(toEmfPlusColor(brush.foreColor));
			if (brush.hatchStyle == EMF_PLUS_HATCH_STYLE_HORIZONTAL || brush.hatchStyle == EMF_PLUS_HATCH_STYLE_CROSS) {
				g.drawLine(0, 4, 7, 4);
			}
			if (brush.hatchStyle == EMF_PLUS_HATCH_STYLE_VERTICAL || brush.hatchStyle == EMF_PLUS_HATCH_STYLE_CROSS) {
				g.drawLine(4, 0, 4, 7);
			}
			if (brush.hatchStyle == EMF_PLUS_HATCH_STYLE_FORWARD_DIAGONAL
					|| brush.hatchStyle == EMF_PLUS_HATCH_STYLE_DIAGONAL_CROSS) {
				g.drawLine(0, 0, 7, 7);
			}
			if (brush.hatchStyle == EMF_PLUS_HATCH_STYLE_BACKWARD_DIAGONAL
					|| brush.hatchStyle == EMF_PLUS_HATCH_STYLE_DIAGONAL_CROSS) {
				g.drawLine(0, 7, 7, 0);
			}
		} finally {
			g.dispose();
		}
		double[] p = toEmfPlusLogicalPoint(emfPlusRenderingOriginX, emfPlusRenderingOriginY);
		double[] size = toEmfPlusLogicalSize(8, 8);
		return new TexturePaint(pattern,
				new Rectangle2D.Double(p[0], p[1], Math.max(1, size[0]), Math.max(1, size[1])));
	}

	private Paint createEmfPlusLinearGradientPaint(double[] p1, double[] p2, EmfPlusBrush brush) {
		int count = brush.blendPositions.length;
		float[] fractions = new float[count];
		Color[] colors = new Color[count];
		float previous = 0.0f;
		for (int i = 0; i < count; i++) {
			float fraction = (float) Math.max(0.0, Math.min(1.0, brush.blendPositions[i]));
			if (i > 0 && fraction <= previous) {
				fraction = Math.min(1.0f, previous + 0.0001f);
			}
			fractions[i] = fraction;
			colors[i] = toEmfPlusColor(brush.blendColors[i]);
			previous = fraction;
		}
		if (fractions[0] != 0.0f) {
			fractions[0] = 0.0f;
		}
		if (fractions[count - 1] != 1.0f) {
			fractions[count - 1] = 1.0f;
		}
		if (p1[0] == p2[0] && p1[1] == p2[1]) {
			return colors[count - 1];
		}
		AffineTransform transform = brush.textureTransform != null
				? toEmfPlusAffineTransform(brush.textureTransform)
				: new AffineTransform();
		return new LinearGradientPaint(new Point2D.Double(p1[0], p1[1]), new Point2D.Double(p2[0], p2[1]), fractions,
				colors, toEmfPlusCycleMethod(brush.wrapMode), toEmfPlusColorSpaceType(brush.gammaCorrected), transform);
	}

	private Paint createEmfPlusPathGradientPaint(EmfPlusBrush brush) {
		double[] center = toEmfPlusLogicalPoint(brush.pathGradientCenter[0], brush.pathGradientCenter[1]);
		double[] radius = toEmfPlusLogicalSize(brush.pathGradientBounds[2] / 2.0, brush.pathGradientBounds[3] / 2.0);
		float r = (float) Math.max(1.0, Math.max(radius[0], radius[1]));
		double[] gradientTransform = brush.textureTransform;
		if (r > 0.0 && Math.abs(radius[0] - radius[1]) > 0.000001) {
			double sx = radius[0] / r;
			double sy = radius[1] / r;
			double[] ellipseTransform = new double[]{sx, 0.0, 0.0, sy, center[0] * (1.0 - sx), center[1] * (1.0 - sy)};
			gradientTransform = gradientTransform != null
					? multiplyEmfPlusMatrix(ellipseTransform, gradientTransform)
					: ellipseTransform;
		}
		float[] fractions;
		Color[] colors;
		if (brush.blendPositions != null && brush.blendColors != null) {
			fractions = toEmfPlusGradientFractions(brush.blendPositions);
			colors = toEmfPlusGradientColors(brush.blendColors);
		} else {
			fractions = new float[]{0.0f, 1.0f};
			colors = new Color[]{toEmfPlusColor(brush.startColor), toEmfPlusColor(brush.endColor)};
			if (isEmfPlusUniformFocusScale(brush.focusScaleX, brush.focusScaleY)) {
				fractions = new float[]{0.0f, (float) clampEmfPlusUnit(brush.focusScaleX), 1.0f};
				colors = new Color[]{toEmfPlusColor(brush.startColor), toEmfPlusColor(brush.startColor),
						toEmfPlusColor(brush.endColor)};
			}
		}
		return new RadialGradientPaint(new Point2D.Double(center[0], center[1]), r,
				new Point2D.Double(center[0], center[1]), fractions, colors, toEmfPlusCycleMethod(brush.wrapMode),
				toEmfPlusColorSpaceType(brush.gammaCorrected),
				gradientTransform != null ? toEmfPlusAffineTransform(gradientTransform) : new AffineTransform());
	}

	private AffineTransform toEmfPlusAffineTransform(double[] matrix) {
		return new AffineTransform(matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
	}

	private MultipleGradientPaint.CycleMethod toEmfPlusCycleMethod(int wrapMode) {
		if (wrapMode == EMF_PLUS_WRAP_MODE_TILE) {
			return MultipleGradientPaint.CycleMethod.REPEAT;
		}
		if (wrapMode == EMF_PLUS_WRAP_MODE_TILE_FLIP_X || wrapMode == EMF_PLUS_WRAP_MODE_TILE_FLIP_Y
				|| wrapMode == EMF_PLUS_WRAP_MODE_TILE_FLIP_XY) {
			return MultipleGradientPaint.CycleMethod.REFLECT;
		}
		return MultipleGradientPaint.CycleMethod.NO_CYCLE;
	}

	private MultipleGradientPaint.ColorSpaceType toEmfPlusColorSpaceType(boolean gammaCorrected) {
		return gammaCorrected
				? MultipleGradientPaint.ColorSpaceType.LINEAR_RGB
				: MultipleGradientPaint.ColorSpaceType.SRGB;
	}

	private float[] toEmfPlusGradientFractions(double[] positions) {
		float[] fractions = new float[positions.length];
		float previous = 0.0f;
		for (int i = 0; i < positions.length; i++) {
			float fraction = (float) Math.max(0.0, Math.min(1.0, positions[i]));
			if (i > 0 && fraction <= previous) {
				fraction = Math.min(1.0f, previous + 0.0001f);
			}
			fractions[i] = fraction;
			previous = fraction;
		}
		fractions[0] = 0.0f;
		fractions[fractions.length - 1] = 1.0f;
		return fractions;
	}

	private Color[] toEmfPlusGradientColors(int[] argb) {
		Color[] colors = new Color[argb.length];
		for (int i = 0; i < argb.length; i++) {
			colors[i] = toEmfPlusColor(argb[i]);
		}
		return colors;
	}

	private boolean isEmfPlusUniformFocusScale(double xScale, double yScale) {
		return xScale > 0.0 && yScale > 0.0 && Math.abs(xScale - yScale) < 0.000001;
	}

	private double clampEmfPlusUnit(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}

	private Color toEmfPlusColor(int argb) {
		return new Color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
	}

	private TexturePaint createEmfPlusTexturePaint(EmfPlusBrush brush) {
		double x = emfPlusRenderingOriginX;
		double y = emfPlusRenderingOriginY;
		double width = brush.textureImage.getWidth();
		double height = brush.textureImage.getHeight();
		if (brush.textureTransform != null) {
			x += brush.textureTransform[4];
			y += brush.textureTransform[5];
			width *= Math.hypot(brush.textureTransform[0], brush.textureTransform[1]);
			height *= Math.hypot(brush.textureTransform[2], brush.textureTransform[3]);
		}
		double[] p = toEmfPlusLogicalPoint(x, y);
		double[] size = toEmfPlusLogicalSize(width, height);
		return new TexturePaint(brush.textureImage,
				new Rectangle2D.Double(p[0], p[1], Math.max(1, size[0]), Math.max(1, size[1])));
	}

	private Font createEmfPlusFont(EmfPlusFont emfPlusFont) {
		double fontSize = Math.max(1.0, emfPlusFont.emSize * toEmfPlusPageUnitScale(emfPlusFont.sizeUnit));
		int style = Font.PLAIN;
		if ((emfPlusFont.styleFlags & EMF_PLUS_FONT_STYLE_BOLD) != 0) {
			style |= Font.BOLD;
		}
		if ((emfPlusFont.styleFlags & EMF_PLUS_FONT_STYLE_ITALIC) != 0) {
			style |= Font.ITALIC;
		}
		Font font = new Font(emfPlusFont.familyName, style, Math.max(1, (int) Math.round(fontSize)));
		if ((emfPlusFont.styleFlags & (EMF_PLUS_FONT_STYLE_UNDERLINE | EMF_PLUS_FONT_STYLE_STRIKEOUT)) == 0) {
			return font;
		}
		Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
		if ((emfPlusFont.styleFlags & EMF_PLUS_FONT_STYLE_UNDERLINE) != 0) {
			attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		}
		if ((emfPlusFont.styleFlags & EMF_PLUS_FONT_STYLE_STRIKEOUT) != 0) {
			attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		}
		return font.deriveFont(attributes);
	}

	private double toEmfPlusStringX(double x, double rectWidth, String text, FontMetrics metrics,
			EmfPlusStringFormat format) {
		if (format == null) {
			return x;
		}
		double width = Math.abs(rectWidth * emfPlusPageScale * emfPlusPageUnitScale);
		if (format.alignment == EMF_PLUS_STRING_ALIGNMENT_CENTER) {
			return x + (width - metrics.stringWidth(text)) / 2.0;
		}
		if (format.alignment == EMF_PLUS_STRING_ALIGNMENT_FAR) {
			return x + width - metrics.stringWidth(text);
		}
		return x;
	}

	private double toEmfPlusStringY(double y, double rectHeight, FontMetrics metrics, EmfPlusStringFormat format) {
		double height = Math.abs(rectHeight * emfPlusPageScale * emfPlusPageUnitScale);
		if (format != null && format.lineAlign == EMF_PLUS_STRING_ALIGNMENT_CENTER) {
			return y + (height - metrics.getHeight()) / 2.0 + metrics.getAscent();
		}
		if (format != null && format.lineAlign == EMF_PLUS_STRING_ALIGNMENT_FAR) {
			return y + height - metrics.getDescent();
		}
		return y + metrics.getAscent();
	}

	private Shape createEmfPlusTextBounds(float x, float y, String text, FontMetrics metrics, boolean vertical) {
		Shape bounds = new Rectangle2D.Double(x, y - metrics.getAscent(), Math.max(1, metrics.stringWidth(text)),
				Math.max(1, metrics.getHeight()));
		if (!vertical) {
			return bounds;
		}
		AffineTransform transform = AffineTransform.getRotateInstance(Math.PI / 2.0, x, y);
		return transform.createTransformedShape(bounds);
	}

	private EmfPlusText createEmfPlusTextRun(String text, EmfPlusStringFormat format, EmfPlusFont font) {
		StringBuilder plain = new StringBuilder();
		ArrayList<Integer> underlineIndexes = new ArrayList<Integer>();
		int hotkeyPrefix = format != null ? format.hotkeyPrefix : 0;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch != '&'
					|| hotkeyPrefix != EMF_PLUS_HOTKEY_PREFIX_SHOW && hotkeyPrefix != EMF_PLUS_HOTKEY_PREFIX_HIDE) {
				plain.append(ch);
				continue;
			}
			if (i + 1 < text.length() && text.charAt(i + 1) == '&') {
				plain.append('&');
				i++;
			} else if (i + 1 < text.length()) {
				if (hotkeyPrefix == EMF_PLUS_HOTKEY_PREFIX_SHOW) {
					underlineIndexes.add(Integer.valueOf(plain.length()));
				}
				plain.append(text.charAt(++i));
			}
		}

		String display = plain.toString();
		AttributedString attributed = new AttributedString(display);
		if (format != null && format.tracking != 0.0 && Math.abs(format.tracking - 1.0) >= 0.000001) {
			attributed.addAttribute(TextAttribute.TRACKING, Float.valueOf((float) (format.tracking - 1.0)));
		}
		if (font != null && (font.styleFlags & EMF_PLUS_FONT_STYLE_UNDERLINE) != 0) {
			attributed.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		}
		if (font != null && (font.styleFlags & EMF_PLUS_FONT_STYLE_STRIKEOUT) != 0) {
			attributed.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		}
		for (int i = 0; i < underlineIndexes.size(); i++) {
			int index = underlineIndexes.get(i).intValue();
			if (index < display.length()) {
				attributed.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, index, index + 1);
			}
		}
		if (format != null && (format.flags & EMF_PLUS_STRING_FORMAT_DIRECTION_RIGHT_TO_LEFT) != 0) {
			attributed.addAttribute(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL);
		}
		return new EmfPlusText(display, attributed);
	}

	private void fillEmfPlusShape(Shape shape, EmfPlusBrush brush) {
		Paint paint = toEmfPlusPaint(brush);
		if (shape == null || paint == null) {
			return;
		}
		ensureGraphics();
		ensureCanvasContains(shape);
		Paint oldPaint = graphics.getPaint();
		graphics.setPaint(paint);
		graphics.fill(shape);
		graphics.setPaint(oldPaint);
		suppressEmfPlusFallback = true;
	}

	private void strokeEmfPlusShape(Shape shape, EmfPlusPen pen) {
		Paint paint = pen != null ? toEmfPlusPaint(pen.brush) : null;
		if (shape == null || paint == null) {
			return;
		}
		ensureGraphics();
		ensureCanvasContains(shape);
		Paint oldPaint = graphics.getPaint();
		java.awt.Stroke oldStroke = graphics.getStroke();
		graphics.setPaint(paint);
		graphics.setStroke(toEmfPlusStroke(pen));
		graphics.draw(shape);
		graphics.setStroke(oldStroke);
		graphics.setPaint(oldPaint);
		suppressEmfPlusFallback = true;
	}

	private void strokeEmfPlusPolyline(double[][] points, boolean closed, EmfPlusPen pen) {
		if (closed || !hasDistinctSolidLineCaps(pen)) {
			strokeEmfPlusShape(createEmfPlusPolyline(points, closed, Path2D.WIND_NON_ZERO), pen);
			return;
		}
		Paint paint = pen != null ? toEmfPlusPaint(pen.brush) : null;
		if (paint == null) {
			return;
		}

		Shape line = createEmfPlusPolyline(points, false, Path2D.WIND_NON_ZERO);
		float width = (float) Math.max(1.0, pen.width * getEmfPlusPenScale(pen));
		Shape startCap = createEmfPlusLineCapShape(pen.startCap, pen.customStartCap, points[0], points[1], width);
		Shape endCap = createEmfPlusLineCapShape(pen.endCap, pen.customEndCap, points[points.length - 1],
				points[points.length - 2], width);

		ensureGraphics();
		ensureCanvasContains(line);
		ensureCanvasContains(startCap);
		ensureCanvasContains(endCap);
		Paint oldPaint = graphics.getPaint();
		java.awt.Stroke oldStroke = graphics.getStroke();
		graphics.setPaint(paint);
		graphics.setStroke(toEmfPlusStroke(pen, BasicStroke.CAP_BUTT));
		graphics.draw(line);
		if (startCap != null) {
			drawEmfPlusLineCap(startCap, pen.customStartCap);
		}
		if (endCap != null) {
			drawEmfPlusLineCap(endCap, pen.customEndCap);
		}
		graphics.setStroke(oldStroke);
		graphics.setPaint(oldPaint);
		suppressEmfPlusFallback = true;
	}

	private void drawEmfPlusLineCap(Shape shape, EmfPlusCustomLineCap customCap) {
		if (customCap == null || customCap.fill) {
			graphics.fill(shape);
		} else {
			graphics.draw(shape);
		}
	}

	private boolean hasDistinctSolidLineCaps(EmfPlusPen pen) {
		return pen != null && !hasEmfPlusDash(pen)
				&& (pen.customStartCap != null || pen.customEndCap != null || pen.startCap != pen.endCap)
				&& (isSupportedEmfPlusLineCap(pen.startCap) || isSupportedEmfPlusLineCap(pen.endCap));
	}

	private boolean hasEmfPlusDash(EmfPlusPen pen) {
		return pen.dashPattern != null || pen.lineStyle == EMF_PLUS_LINE_STYLE_DASH
				|| pen.lineStyle == EMF_PLUS_LINE_STYLE_DOT || pen.lineStyle == EMF_PLUS_LINE_STYLE_DASH_DOT
				|| pen.lineStyle == EMF_PLUS_LINE_STYLE_DASH_DOT_DOT || pen.lineStyle == EMF_PLUS_LINE_STYLE_CUSTOM;
	}

	private boolean isSupportedEmfPlusLineCap(int cap) {
		return cap == EMF_PLUS_LINE_CAP_FLAT || cap == EMF_PLUS_LINE_CAP_SQUARE || cap == EMF_PLUS_LINE_CAP_ROUND;
	}

	private Shape createEmfPlusLineCapShape(int cap, EmfPlusCustomLineCap customCap, double[] end, double[] adjacent,
			float width) {
		if (customCap != null) {
			return createEmfPlusCustomLineCapShape(customCap, end, adjacent, width);
		}
		if (cap == EMF_PLUS_LINE_CAP_FLAT || !isSupportedEmfPlusLineCap(cap)) {
			return null;
		}
		double dx = end[0] - adjacent[0];
		double dy = end[1] - adjacent[1];
		double length = Math.hypot(dx, dy);
		if (length <= 0.0 || Double.isNaN(length) || Double.isInfinite(length)) {
			return null;
		}
		double half = width / 2.0;
		if (cap == EMF_PLUS_LINE_CAP_ROUND) {
			return new Ellipse2D.Double(end[0] - half, end[1] - half, width, width);
		}

		double ux = dx / length * half;
		double uy = dy / length * half;
		double nx = -dy / length * half;
		double ny = dx / length * half;
		Path2D path = new Path2D.Double();
		path.moveTo(end[0] - ux - nx, end[1] - uy - ny);
		path.lineTo(end[0] + ux - nx, end[1] + uy - ny);
		path.lineTo(end[0] + ux + nx, end[1] + uy + ny);
		path.lineTo(end[0] - ux + nx, end[1] - uy + ny);
		path.closePath();
		return path;
	}

	private Shape createEmfPlusCustomLineCapShape(EmfPlusCustomLineCap cap, double[] end, double[] adjacent,
			float penWidth) {
		double dx = end[0] - adjacent[0];
		double dy = end[1] - adjacent[1];
		double length = Math.hypot(dx, dy);
		if (length <= 0.0 || Double.isNaN(length) || Double.isInfinite(length)) {
			return null;
		}
		double ux = dx / length;
		double uy = dy / length;
		double nx = -uy;
		double ny = ux;
		double width = cap.width * penWidth * cap.widthScale;
		double height = cap.height * penWidth * cap.widthScale;
		Path2D path = new Path2D.Double();
		path.moveTo(end[0] + ux * height, end[1] + uy * height);
		path.lineTo(end[0] + nx * width / 2.0, end[1] + ny * width / 2.0);
		path.lineTo(end[0] - nx * width / 2.0, end[1] - ny * width / 2.0);
		path.closePath();
		return path;
	}

	private BasicStroke toEmfPlusStroke(EmfPlusPen pen) {
		return toEmfPlusStroke(pen, toEmfPlusStrokeCap(pen));
	}

	private BasicStroke toEmfPlusStroke(EmfPlusPen pen, int cap) {
		float width = (float) Math.max(1.0, pen.width * getEmfPlusPenScale(pen));
		float[] dash = toEmfPlusDash(pen, width);
		if (dash != null && pen.dashCap == EMF_PLUS_DASH_CAP_ROUND) {
			cap = BasicStroke.CAP_ROUND;
		}
		int join = pen.lineJoin == EMF_PLUS_LINE_JOIN_BEVEL
				? BasicStroke.JOIN_BEVEL
				: pen.lineJoin == EMF_PLUS_LINE_JOIN_ROUND ? BasicStroke.JOIN_ROUND : BasicStroke.JOIN_MITER;
		float miterLimit = (float) Math.max(1.0, pen.miterLimit > 0.0 ? pen.miterLimit : 10.0);
		return dash != null
				? new BasicStroke(width, cap, join, miterLimit, dash, (float) (pen.dashOffset * width))
				: new BasicStroke(width, cap, join, miterLimit);
	}

	private int toEmfPlusStrokeCap(EmfPlusPen pen) {
		return pen.startCap == EMF_PLUS_LINE_CAP_SQUARE || pen.endCap == EMF_PLUS_LINE_CAP_SQUARE
				? BasicStroke.CAP_SQUARE
				: pen.startCap == EMF_PLUS_LINE_CAP_ROUND || pen.endCap == EMF_PLUS_LINE_CAP_ROUND
						? BasicStroke.CAP_ROUND
						: BasicStroke.CAP_BUTT;
	}

	private double getEmfPlusPenScale(EmfPlusPen pen) {
		if (pen.transform == null) {
			return 1.0;
		}
		double xScale = Math.hypot(pen.transform[0], pen.transform[1]);
		double yScale = Math.hypot(pen.transform[2], pen.transform[3]);
		double scale = (xScale + yScale) / 2.0;
		if (scale <= 0.0 || Double.isNaN(scale) || Double.isInfinite(scale)) {
			return 1.0;
		}
		return scale;
	}

	private float[] toEmfPlusDash(EmfPlusPen pen, float width) {
		double[] source = pen.dashPattern;
		if (source == null) {
			if (pen.lineStyle == EMF_PLUS_LINE_STYLE_DASH) {
				source = new double[]{3, 1};
			} else if (pen.lineStyle == EMF_PLUS_LINE_STYLE_DOT) {
				source = new double[]{1, 1};
			} else if (pen.lineStyle == EMF_PLUS_LINE_STYLE_DASH_DOT) {
				source = new double[]{3, 1, 1, 1};
			} else if (pen.lineStyle == EMF_PLUS_LINE_STYLE_DASH_DOT_DOT) {
				source = new double[]{3, 1, 1, 1, 1, 1};
			}
		}
		if (source == null || source.length == 0) {
			return null;
		}
		float[] dash = new float[source.length];
		for (int i = 0; i < source.length; i++) {
			dash[i] = (float) Math.max(1.0, source[i] * width);
		}
		return dash;
	}

	private int toEmfPlusFillWindingRule(int flags) {
		return (flags & EMF_PLUS_FLAG_WINDING_FILL) != 0 ? Path2D.WIND_NON_ZERO : Path2D.WIND_EVEN_ODD;
	}

	private Shape createEmfPlusRect(double[] rect) {
		Path2D.Double path = new Path2D.Double();
		double[] p0 = toEmfPlusLogicalPoint(rect[0], rect[1]);
		double[] p1 = toEmfPlusLogicalPoint(rect[0] + rect[2], rect[1]);
		double[] p2 = toEmfPlusLogicalPoint(rect[0] + rect[2], rect[1] + rect[3]);
		double[] p3 = toEmfPlusLogicalPoint(rect[0], rect[1] + rect[3]);
		path.moveTo(p0[0], p0[1]);
		path.lineTo(p1[0], p1[1]);
		path.lineTo(p2[0], p2[1]);
		path.lineTo(p3[0], p3[1]);
		path.closePath();
		return path;
	}

	private Shape createEmfPlusEllipse(double[] rect) {
		double[] p = toEmfPlusLogicalPoint(rect[0], rect[1]);
		double[] size = toEmfPlusLogicalSize(rect[2], rect[3]);
		return new Ellipse2D.Double(p[0], p[1], size[0], size[1]);
	}

	private Shape createEmfPlusArc(double[] rect, double startAngle, double sweepAngle, int type) {
		double sweep = clampEmfPlusSweepAngle(sweepAngle);
		if (rect[2] == 0 || rect[3] == 0 || sweep == 0) {
			return null;
		}
		double[] p = toEmfPlusLogicalPoint(rect[0], rect[1]);
		double[] size = toEmfPlusLogicalSize(rect[2], rect[3]);
		return new Arc2D.Double(p[0], p[1], size[0], size[1], -startAngle, -sweep, type);
	}

	private double clampEmfPlusSweepAngle(double angle) {
		if (angle > 360.0) {
			return 360.0;
		}
		if (angle < -360.0) {
			return -360.0;
		}
		return angle;
	}

	private Path2D.Double createEmfPlusPolyline(double[][] points, boolean close, int windingRule) {
		Path2D.Double path = new Path2D.Double(windingRule);
		if (points == null || points.length == 0) {
			return path;
		}
		double[] p = toEmfPlusLogicalPoint(points[0][0], points[0][1]);
		path.moveTo(p[0], p[1]);
		for (int i = 1; i < points.length; i++) {
			p = toEmfPlusLogicalPoint(points[i][0], points[i][1]);
			path.lineTo(p[0], p[1]);
		}
		if (close) {
			path.closePath();
		}
		return path;
	}

	private Shape createEmfPlusImageDestination(double[] p0, double[] p1, double[] p2) {
		Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
		path.moveTo(p0[0], p0[1]);
		path.lineTo(p1[0], p1[1]);
		path.lineTo(p1[0] + p2[0] - p0[0], p1[1] + p2[1] - p0[1]);
		path.lineTo(p2[0], p2[1]);
		path.closePath();
		return path;
	}

	private Path2D.Double createEmfPlusPath(EmfPlusPath source) {
		Path2D.Double path = new Path2D.Double(
				source.fillMode == EMF_PLUS_FILL_MODE_ALTERNATE ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
		int i = 0;
		while (i < source.points.length) {
			int rawType = source.types[i] & 0xFF;
			int type = rawType & EMF_PLUS_PATH_POINT_TYPE_MASK;
			if (type == EMF_PLUS_PATH_POINT_TYPE_START || path.getCurrentPoint() == null) {
				double[] p = toEmfPlusLogicalPoint(source.points[i][0], source.points[i][1]);
				path.moveTo(p[0], p[1]);
				if ((rawType & EMF_PLUS_PATH_POINT_TYPE_CLOSE) != 0) {
					path.closePath();
				}
				i++;
			} else if (type == EMF_PLUS_PATH_POINT_TYPE_LINE) {
				double[] p = toEmfPlusLogicalPoint(source.points[i][0], source.points[i][1]);
				path.lineTo(p[0], p[1]);
				if ((rawType & EMF_PLUS_PATH_POINT_TYPE_CLOSE) != 0) {
					path.closePath();
				}
				i++;
			} else if (type == EMF_PLUS_PATH_POINT_TYPE_BEZIER && i + 2 < source.points.length) {
				double[] p1 = toEmfPlusLogicalPoint(source.points[i][0], source.points[i][1]);
				double[] p2 = toEmfPlusLogicalPoint(source.points[i + 1][0], source.points[i + 1][1]);
				double[] p3 = toEmfPlusLogicalPoint(source.points[i + 2][0], source.points[i + 2][1]);
				path.curveTo(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
				if (((source.types[i + 2] & 0xFF) & EMF_PLUS_PATH_POINT_TYPE_CLOSE) != 0) {
					path.closePath();
				}
				i += 3;
			} else {
				i++;
			}
		}
		return path;
	}

	private Area createEmfPlusRegionArea(EmfPlusRegion region) {
		if (region == null) {
			return null;
		}
		if (region.empty) {
			return new Area();
		}
		if (region.infinite) {
			return new Area(new Rectangle2D.Double(-1000000, -1000000, 2000000, 2000000));
		}
		if (region.rect != null) {
			return new Area(createEmfPlusRect(region.rect));
		}
		if (region.path != null) {
			return new Area(createEmfPlusPath(region.path));
		}

		Area left = createEmfPlusRegionArea(region.left);
		Area right = createEmfPlusRegionArea(region.right);
		if (left == null || right == null) {
			return null;
		}
		if (region.combineMode == EMF_PLUS_REGION_NODE_TYPE_AND) {
			left.intersect(right);
		} else if (region.combineMode == EMF_PLUS_REGION_NODE_TYPE_OR) {
			left.add(right);
		} else if (region.combineMode == EMF_PLUS_REGION_NODE_TYPE_XOR) {
			left.exclusiveOr(right);
		} else if (region.combineMode == EMF_PLUS_REGION_NODE_TYPE_EXCLUDE) {
			left.subtract(right);
		} else if (region.combineMode == EMF_PLUS_REGION_NODE_TYPE_COMPLEMENT) {
			right.subtract(left);
			left = right;
		}
		return left;
	}

	private Path2D.Double createEmfPlusBezierPath(double[][] points) {
		if (points.length < 4) {
			return null;
		}
		Path2D.Double path = new Path2D.Double();
		double[] p = toEmfPlusLogicalPoint(points[0][0], points[0][1]);
		path.moveTo(p[0], p[1]);
		for (int i = 1; i + 2 < points.length; i += 3) {
			double[] p1 = toEmfPlusLogicalPoint(points[i][0], points[i][1]);
			double[] p2 = toEmfPlusLogicalPoint(points[i + 1][0], points[i + 1][1]);
			double[] p3 = toEmfPlusLogicalPoint(points[i + 2][0], points[i + 2][1]);
			path.curveTo(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
		}
		return path;
	}

	private Path2D.Double createEmfPlusClosedCurvePath(double[][] points, double tension, int windingRule) {
		Path2D.Double path = new Path2D.Double(windingRule);
		double[] first = toEmfPlusLogicalPoint(points[0][0], points[0][1]);
		path.moveTo(first[0], first[1]);
		double factor = tension / 3.0;
		for (int i = 0; i < points.length; i++) {
			double[] previous = points[(i + points.length - 1) % points.length];
			double[] current = points[i];
			double[] next = points[(i + 1) % points.length];
			double[] afterNext = points[(i + 2) % points.length];
			double[] control1 = new double[]{current[0] + (next[0] - previous[0]) * factor,
					current[1] + (next[1] - previous[1]) * factor};
			double[] control2 = new double[]{next[0] - (afterNext[0] - current[0]) * factor,
					next[1] - (afterNext[1] - current[1]) * factor};
			double[] p1 = toEmfPlusLogicalPoint(control1[0], control1[1]);
			double[] p2 = toEmfPlusLogicalPoint(control2[0], control2[1]);
			double[] p3 = toEmfPlusLogicalPoint(next[0], next[1]);
			path.curveTo(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
		}
		path.closePath();
		return path;
	}

	private Path2D.Double createEmfPlusCurvePath(double[][] points, int offset, int numberOfSegments, double tension) {
		Path2D.Double path = new Path2D.Double();
		double[] start = toEmfPlusLogicalPoint(points[offset][0], points[offset][1]);
		path.moveTo(start[0], start[1]);
		double factor = tension / 3.0;
		for (int i = offset; i < offset + numberOfSegments; i++) {
			double[] previous = points[i == 0 ? i : i - 1];
			double[] current = points[i];
			double[] next = points[i + 1];
			double[] afterNext = points[i + 2 < points.length ? i + 2 : i + 1];
			double[] control1 = new double[]{current[0] + (next[0] - previous[0]) * factor,
					current[1] + (next[1] - previous[1]) * factor};
			double[] control2 = new double[]{next[0] - (afterNext[0] - current[0]) * factor,
					next[1] - (afterNext[1] - current[1]) * factor};
			double[] p1 = toEmfPlusLogicalPoint(control1[0], control1[1]);
			double[] p2 = toEmfPlusLogicalPoint(control2[0], control2[1]);
			double[] p3 = toEmfPlusLogicalPoint(next[0], next[1]);
			path.curveTo(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
		}
		return path;
	}

	private boolean isEmfPlusCompressed(int flags) {
		return (flags & EMF_PLUS_FLAG_COMPRESSED) != 0;
	}

	private double[][] readEmfPlusRects(byte[] payload, int offset, int count, boolean compressed) {
		if (count < 0) {
			return null;
		}
		int elementSize = compressed ? 8 : 16;
		if (payload.length < offset || (payload.length - offset) / elementSize < count) {
			return null;
		}
		double[][] rects = new double[count][4];
		for (int i = 0; i < count; i++) {
			int pos = offset + i * elementSize;
			if (compressed) {
				rects[i][0] = readInt16(payload, pos);
				rects[i][1] = readInt16(payload, pos + 2);
				rects[i][2] = readInt16(payload, pos + 4);
				rects[i][3] = readInt16(payload, pos + 6);
			} else {
				rects[i][0] = readFloat(payload, pos);
				rects[i][1] = readFloat(payload, pos + 4);
				rects[i][2] = readFloat(payload, pos + 8);
				rects[i][3] = readFloat(payload, pos + 12);
			}
		}
		return rects;
	}

	private double[][] readEmfPlusTSClipRects(byte[] payload, int count) {
		if (payload.length < count * 8) {
			return null;
		}
		double[][] rects = new double[count][4];
		for (int i = 0; i < count; i++) {
			int offset = i * 8;
			rects[i][0] = readInt16(payload, offset);
			rects[i][1] = readInt16(payload, offset + 2);
			rects[i][2] = readInt16(payload, offset + 4);
			rects[i][3] = readInt16(payload, offset + 6);
		}
		return rects;
	}

	private double[][] readCompressedEmfPlusTSClipRects(byte[] payload, int count) {
		double[][] rects = new double[count][4];
		int[] offset = new int[]{0};
		int left = 0;
		int top = 0;
		int right = 0;
		for (int i = 0; i < count; i++) {
			Integer leftDelta = readCompressedEmfPlusTSClipValue(payload, offset);
			Integer topDelta = readCompressedEmfPlusTSClipValue(payload, offset);
			Integer rightDelta = readCompressedEmfPlusTSClipValue(payload, offset);
			Integer height = readCompressedEmfPlusTSClipValue(payload, offset);
			if (leftDelta == null || topDelta == null || rightDelta == null || height == null) {
				return null;
			}
			left += leftDelta.intValue();
			top += topDelta.intValue();
			right += rightDelta.intValue();
			rects[i][0] = left;
			rects[i][1] = top;
			rects[i][2] = right;
			rects[i][3] = top + height.intValue();
		}
		return rects;
	}

	private Integer readCompressedEmfPlusTSClipValue(byte[] payload, int[] offset) {
		if (offset[0] >= payload.length) {
			return null;
		}
		int first = payload[offset[0]++] & 0xFF;
		if ((first & 0x80) != 0) {
			int value = first & 0x7F;
			if ((value & 0x40) != 0) {
				value |= ~0x7F;
			}
			return Integer.valueOf(value);
		}
		if (offset[0] >= payload.length) {
			return null;
		}
		int value = ((first & 0x7F) << 8) | (payload[offset[0]++] & 0xFF);
		if ((value & 0x4000) != 0) {
			value |= ~0x7FFF;
		}
		return Integer.valueOf(value);
	}

	private double[][] readEmfPlusDrawingPoints(byte[] payload, int offset, int count, int flags) {
		if ((flags & EMF_PLUS_FLAG_RELATIVE) == 0) {
			return readEmfPlusDrawingPoints(payload, offset, count, isEmfPlusCompressed(flags));
		}
		if (count < 0 || payload.length < offset || payload.length - offset < count * 2) {
			return null;
		}
		double[][] points = new double[count][2];
		int x = 0;
		int y = 0;
		for (int i = 0; i < count; i++) {
			x += payload[offset + i * 2];
			y += payload[offset + i * 2 + 1];
			points[i][0] = x;
			points[i][1] = y;
		}
		return points;
	}

	private double[][] readEmfPlusDrawingPoints(byte[] payload, int offset, int count, boolean compressed) {
		if (count < 0) {
			return null;
		}
		int elementSize = compressed ? 4 : 8;
		if (payload.length < offset || (payload.length - offset) / elementSize < count) {
			return null;
		}
		double[][] points = new double[count][2];
		for (int i = 0; i < count; i++) {
			int pos = offset + i * elementSize;
			if (compressed) {
				points[i][0] = readInt16(payload, pos);
				points[i][1] = readInt16(payload, pos + 2);
			} else {
				points[i][0] = readFloat(payload, pos);
				points[i][1] = readFloat(payload, pos + 4);
			}
		}
		return points;
	}

	private int[] readEmfPlusGlyphCodes(byte[] payload, int offset, int count) {
		if (count < 0 || payload.length < offset || payload.length - offset < count * 2) {
			return null;
		}
		int[] glyphCodes = new int[count];
		for (int i = 0; i < count; i++) {
			glyphCodes[i] = readUInt16(payload, offset + i * 2);
		}
		return glyphCodes;
	}

	private double[] toEmfPlusLogicalPoint(double x, double y) {
		double scale = emfPlusPageScale * emfPlusPageUnitScale;
		double tx = emfPlusWorldTransform[0] * x + emfPlusWorldTransform[2] * y + emfPlusWorldTransform[4];
		double ty = emfPlusWorldTransform[1] * x + emfPlusWorldTransform[3] * y + emfPlusWorldTransform[5];
		return new double[]{tx * scale + emfPlusPixelOffsetX, ty * scale + emfPlusPixelOffsetY};
	}

	private double[] toEmfPlusLogicalSize(double width, double height) {
		double scale = emfPlusPageScale * emfPlusPageUnitScale;
		double x = emfPlusWorldTransform[0] * width + emfPlusWorldTransform[2] * height;
		double y = emfPlusWorldTransform[1] * width + emfPlusWorldTransform[3] * height;
		return new double[]{Math.abs(x * scale), Math.abs(y * scale)};
	}

	private double[] getEmfPlusPointBounds(double[][] points) {
		if (points == null || points.length == 0) {
			return null;
		}
		double minX = points[0][0];
		double minY = points[0][1];
		double maxX = points[0][0];
		double maxY = points[0][1];
		for (int i = 1; i < points.length; i++) {
			minX = Math.min(minX, points[i][0]);
			minY = Math.min(minY, points[i][1]);
			maxX = Math.max(maxX, points[i][0]);
			maxY = Math.max(maxY, points[i][1]);
		}
		return new double[]{minX, minY, maxX - minX, maxY - minY};
	}

	private double[] toEmfPlusDriverStringPoint(double[] point, double[] matrix) {
		double x = point[0];
		double y = point[1];
		if (matrix != null) {
			double tx = matrix[0] * x + matrix[2] * y + matrix[4];
			double ty = matrix[1] * x + matrix[3] * y + matrix[5];
			x = tx;
			y = ty;
		}
		return toEmfPlusLogicalPoint(x, y);
	}

	private Shape createEmfPlusDriverStringBounds(double x, double y, double width, double height, double[] matrix) {
		Shape bounds = new Rectangle2D.Double(x, y, width, height);
		if (matrix == null) {
			return bounds;
		}
		return createEmfPlusDriverStringTransform(matrix).createTransformedShape(bounds);
	}

	private AffineTransform createEmfPlusDriverStringTransform(double[] matrix) {
		double scale = emfPlusPageScale * emfPlusPageUnitScale;
		double[] combined = multiplyEmfPlusMatrix(matrix, emfPlusWorldTransform);
		return new AffineTransform(combined[0] * scale, combined[1] * scale, combined[2] * scale, combined[3] * scale,
				combined[4] * scale + emfPlusPixelOffsetX, combined[5] * scale + emfPlusPixelOffsetY);
	}

	private double[] getEmfPlusImageUnitScale(double[] p0, double[] p1, double[] p2, double srcWidth,
			double srcHeight) {
		double scaleX = Math.hypot(p1[0] - p0[0], p1[1] - p0[1]) / Math.abs(srcWidth);
		double scaleY = Math.hypot(p2[0] - p0[0], p2[1] - p0[1]) / Math.abs(srcHeight);
		if (scaleX <= 0 || Double.isNaN(scaleX) || Double.isInfinite(scaleX)) {
			scaleX = 1;
		}
		if (scaleY <= 0 || Double.isNaN(scaleY) || Double.isInfinite(scaleY)) {
			scaleY = 1;
		}
		return new double[]{scaleX, scaleY};
	}

	private double[] normalizeEmfPlusImagePoint(double[] point, double[] unit) {
		return new double[]{point[0] / unit[0], point[1] / unit[1]};
	}

	private void setEmfPlusWorldTransform(byte[] payload) {
		if (payload.length < 24) {
			return;
		}
		emfPlusWorldTransform = new double[]{readFloat(payload, 0), readFloat(payload, 4), readFloat(payload, 8),
				readFloat(payload, 12), readFloat(payload, 16), readFloat(payload, 20)};
	}

	private void multiplyEmfPlusWorldTransform(int flags, byte[] payload) {
		if (payload.length < 24) {
			return;
		}
		applyEmfPlusWorldTransform(flags, new double[]{readFloat(payload, 0), readFloat(payload, 4),
				readFloat(payload, 8), readFloat(payload, 12), readFloat(payload, 16), readFloat(payload, 20)});
	}

	private void translateEmfPlusWorldTransform(int flags, byte[] payload) {
		if (payload.length < 8) {
			return;
		}
		applyEmfPlusWorldTransform(flags, new double[]{1, 0, 0, 1, readFloat(payload, 0), readFloat(payload, 4)});
	}

	private void scaleEmfPlusWorldTransform(int flags, byte[] payload) {
		if (payload.length < 8) {
			return;
		}
		applyEmfPlusWorldTransform(flags, new double[]{readFloat(payload, 0), 0, 0, readFloat(payload, 4), 0, 0});
	}

	private void rotateEmfPlusWorldTransform(int flags, byte[] payload) {
		if (payload.length < 4) {
			return;
		}
		double radians = Math.toRadians(readFloat(payload, 0));
		applyEmfPlusWorldTransform(flags,
				new double[]{Math.cos(radians), Math.sin(radians), -Math.sin(radians), Math.cos(radians), 0, 0});
	}

	private void applyEmfPlusWorldTransform(int flags, double[] matrix) {
		if ((flags & EMF_PLUS_FLAG_MATRIX_ORDER_APPEND) != 0) {
			emfPlusWorldTransform = multiplyEmfPlusMatrix(emfPlusWorldTransform, matrix);
		} else {
			emfPlusWorldTransform = multiplyEmfPlusMatrix(matrix, emfPlusWorldTransform);
		}
	}

	private double[] multiplyEmfPlusMatrix(double[] a, double[] b) {
		return new double[]{a[0] * b[0] + a[1] * b[2], a[0] * b[1] + a[1] * b[3], a[2] * b[0] + a[3] * b[2],
				a[2] * b[1] + a[3] * b[3], a[4] * b[0] + a[5] * b[2] + b[4], a[4] * b[1] + a[5] * b[3] + b[5]};
	}

	private void setEmfPlusPageTransform(int flags, byte[] payload) {
		if (payload.length < 4) {
			return;
		}
		emfPlusPageScale = readFloat(payload, 0);
		emfPlusPageUnitScale = toEmfPlusPageUnitScale(flags & 0xFF);
	}

	private double toEmfPlusPageUnitScale(int unit) {
		if (unit == EMF_PLUS_UNIT_POINT) {
			return 4.0 / 3.0;
		}
		if (unit == EMF_PLUS_UNIT_INCH) {
			return 96.0;
		}
		if (unit == EMF_PLUS_UNIT_DOCUMENT) {
			return 96.0 / 300.0;
		}
		if (unit == EMF_PLUS_UNIT_MILLIMETER) {
			return 96.0 / 25.4;
		}
		return 1.0;
	}

	private static float readFloat(byte[] data, int offset) {
		return Float.intBitsToFloat(readInt32(data, offset));
	}

	private static int readInt16(byte[] data, int offset) {
		return (short) readUInt16(data, offset);
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

	private static int align4(int value) {
		return (value + 3) & ~3;
	}

	private static class PendingEmfPlusObject {
		private final int objectType;
		private int totalObjectSize;
		private final ByteArrayOutputStream out = new ByteArrayOutputStream();

		private PendingEmfPlusObject(int objectType, int totalObjectSize) {
			this.objectType = objectType;
			this.totalObjectSize = totalObjectSize;
		}

		private void write(byte[] data) {
			out.write(data, 0, data.length);
		}

		private int size() {
			return out.size();
		}

		private byte[] toByteArray() {
			return out.toByteArray();
		}
	}

	private static class EmfPlusRenderingState {
		private final Object antiAliasingHint;
		private final Object textAntiAliasingHint;
		private final Object interpolationHint;
		private final Object alphaInterpolationHint;
		private final Object strokeControlHint;
		private final Composite composite;
		private final int textContrast;
		private final int renderingOriginX;
		private final int renderingOriginY;
		private final double pixelOffsetX;
		private final double pixelOffsetY;

		private EmfPlusRenderingState(Object antiAliasingHint, Object textAntiAliasingHint, Object interpolationHint,
				Object alphaInterpolationHint, Object strokeControlHint, Composite composite, int textContrast,
				int renderingOriginX, int renderingOriginY, double pixelOffsetX, double pixelOffsetY) {
			this.antiAliasingHint = antiAliasingHint;
			this.textAntiAliasingHint = textAntiAliasingHint;
			this.interpolationHint = interpolationHint;
			this.alphaInterpolationHint = alphaInterpolationHint;
			this.strokeControlHint = strokeControlHint;
			this.composite = composite;
			this.textContrast = textContrast;
			this.renderingOriginX = renderingOriginX;
			this.renderingOriginY = renderingOriginY;
			this.pixelOffsetX = pixelOffsetX;
			this.pixelOffsetY = pixelOffsetY;
		}
	}

	private static class EmfPlusBrush {
		private final int argb;
		private final int hatchStyle;
		private final int foreColor;
		private final int backColor;
		private final double[] linearGradientRect;
		private final int startColor;
		private final int endColor;
		private final double[] blendPositions;
		private final int[] blendColors;
		private final BufferedImage textureImage;
		private final double[] textureTransform;
		private final int wrapMode;
		private final boolean gammaCorrected;
		private final double[] pathGradientCenter;
		private final double[] pathGradientBounds;
		private final double focusScaleX;
		private final double focusScaleY;

		private EmfPlusBrush(int argb) {
			this.argb = argb;
			this.hatchStyle = -1;
			this.foreColor = 0;
			this.backColor = 0;
			this.linearGradientRect = null;
			this.startColor = 0;
			this.endColor = 0;
			this.blendPositions = null;
			this.blendColors = null;
			this.textureImage = null;
			this.textureTransform = null;
			this.wrapMode = EMF_PLUS_WRAP_MODE_CLAMP;
			this.gammaCorrected = false;
			this.pathGradientCenter = null;
			this.pathGradientBounds = null;
			this.focusScaleX = 0;
			this.focusScaleY = 0;
		}

		private EmfPlusBrush(double[] linearGradientRect, int startColor, int endColor) {
			this(linearGradientRect, startColor, endColor, null, null, null);
		}

		private EmfPlusBrush(double[] linearGradientRect, int startColor, int endColor, double[] blendPositions,
				int[] blendColors) {
			this(linearGradientRect, startColor, endColor, blendPositions, blendColors, null);
		}

		private EmfPlusBrush(double[] linearGradientRect, int startColor, int endColor, double[] blendPositions,
				int[] blendColors, double[] brushTransform) {
			this(linearGradientRect, startColor, endColor, blendPositions, blendColors, brushTransform,
					EMF_PLUS_WRAP_MODE_TILE, false);
		}

		private EmfPlusBrush(double[] linearGradientRect, int startColor, int endColor, double[] blendPositions,
				int[] blendColors, double[] brushTransform, int wrapMode, boolean gammaCorrected) {
			this.argb = 0;
			this.hatchStyle = -1;
			this.foreColor = 0;
			this.backColor = 0;
			this.linearGradientRect = linearGradientRect;
			this.startColor = startColor;
			this.endColor = endColor;
			this.blendPositions = blendPositions;
			this.blendColors = blendColors;
			this.textureImage = null;
			this.textureTransform = brushTransform;
			this.wrapMode = wrapMode;
			this.gammaCorrected = gammaCorrected;
			this.pathGradientCenter = null;
			this.pathGradientBounds = null;
			this.focusScaleX = 0;
			this.focusScaleY = 0;
		}

		private EmfPlusBrush(BufferedImage textureImage, double[] textureTransform) {
			this.argb = 0;
			this.hatchStyle = -1;
			this.foreColor = 0;
			this.backColor = 0;
			this.linearGradientRect = null;
			this.startColor = 0;
			this.endColor = 0;
			this.blendPositions = null;
			this.blendColors = null;
			this.textureImage = textureImage;
			this.textureTransform = textureTransform;
			this.wrapMode = EMF_PLUS_WRAP_MODE_TILE;
			this.gammaCorrected = false;
			this.pathGradientCenter = null;
			this.pathGradientBounds = null;
			this.focusScaleX = 0;
			this.focusScaleY = 0;
		}

		private EmfPlusBrush(double[] pathGradientCenter, double[] pathGradientBounds, int centerColor,
				int surroundColor, double[] blendPositions, int[] blendColors, double[] brushTransform,
				double focusScaleX, double focusScaleY) {
			this(pathGradientCenter, pathGradientBounds, centerColor, surroundColor, blendPositions, blendColors,
					brushTransform, EMF_PLUS_WRAP_MODE_TILE, false, focusScaleX, focusScaleY);
		}

		private EmfPlusBrush(double[] pathGradientCenter, double[] pathGradientBounds, int centerColor,
				int surroundColor, double[] blendPositions, int[] blendColors, double[] brushTransform, int wrapMode,
				boolean gammaCorrected, double focusScaleX, double focusScaleY) {
			this.argb = 0;
			this.hatchStyle = -1;
			this.foreColor = 0;
			this.backColor = 0;
			this.linearGradientRect = null;
			this.startColor = centerColor;
			this.endColor = surroundColor;
			this.blendPositions = blendPositions;
			this.blendColors = blendColors;
			this.textureImage = null;
			this.textureTransform = brushTransform;
			this.wrapMode = wrapMode;
			this.gammaCorrected = gammaCorrected;
			this.pathGradientCenter = pathGradientCenter;
			this.pathGradientBounds = pathGradientBounds;
			this.focusScaleX = focusScaleX;
			this.focusScaleY = focusScaleY;
		}

		private EmfPlusBrush(int hatchStyle, int foreColor, int backColor) {
			this.argb = 0;
			this.hatchStyle = hatchStyle;
			this.foreColor = foreColor;
			this.backColor = backColor;
			this.linearGradientRect = null;
			this.startColor = 0;
			this.endColor = 0;
			this.blendPositions = null;
			this.blendColors = null;
			this.textureImage = null;
			this.textureTransform = null;
			this.wrapMode = EMF_PLUS_WRAP_MODE_TILE;
			this.gammaCorrected = false;
			this.pathGradientCenter = null;
			this.pathGradientBounds = null;
			this.focusScaleX = 0;
			this.focusScaleY = 0;
		}
	}

	private static class EmfPlusBlendColors {
		private final double[] positions;
		private final int[] colors;

		private EmfPlusBlendColors(double[] positions, int[] colors) {
			this.positions = positions;
			this.colors = colors;
		}
	}

	private static class EmfPlusBlendFactors {
		private final double[] positions;
		private final double[] factors;

		private EmfPlusBlendFactors(double[] positions, double[] factors) {
			this.positions = positions;
			this.factors = factors;
		}
	}

	private static class EmfPlusPen {
		private final double width;
		private final EmfPlusBrush brush;
		private final int startCap;
		private final int endCap;
		private final int lineJoin;
		private final double miterLimit;
		private final int lineStyle;
		private final int dashCap;
		private final double dashOffset;
		private final double[] dashPattern;
		private final double[] transform;
		private final EmfPlusCustomLineCap customStartCap;
		private final EmfPlusCustomLineCap customEndCap;

		private EmfPlusPen(double width, EmfPlusBrush brush, int startCap, int endCap, int lineJoin, double miterLimit,
				int lineStyle, int dashCap, double dashOffset, double[] dashPattern, double[] transform,
				EmfPlusCustomLineCap customStartCap, EmfPlusCustomLineCap customEndCap) {
			this.width = width;
			this.brush = brush;
			this.startCap = startCap;
			this.endCap = endCap;
			this.lineJoin = lineJoin;
			this.miterLimit = miterLimit;
			this.lineStyle = lineStyle;
			this.dashCap = dashCap;
			this.dashOffset = dashOffset;
			this.dashPattern = dashPattern;
			this.transform = transform;
			this.customStartCap = customStartCap;
			this.customEndCap = customEndCap;
		}
	}

	private static class EmfPlusCustomLineCap {
		private final double width;
		private final double height;
		private final boolean fill;
		private final double widthScale;

		private EmfPlusCustomLineCap(double width, double height, boolean fill, double widthScale) {
			this.width = width;
			this.height = height;
			this.fill = fill;
			this.widthScale = widthScale;
		}
	}

	private static class EmfPlusPath {
		private final int fillMode;
		private final double[][] points;
		private final byte[] types;

		private EmfPlusPath(int fillMode, double[][] points, byte[] types) {
			this.fillMode = fillMode;
			this.points = points;
			this.types = types;
		}
	}

	private static class EmfPlusRegion {
		private final int combineMode;
		private final double[] rect;
		private final EmfPlusPath path;
		private final EmfPlusRegion left;
		private final EmfPlusRegion right;
		private final boolean empty;
		private final boolean infinite;

		private EmfPlusRegion(double[] rect, EmfPlusPath path) {
			this(0, rect, path, null, null, false, false);
		}

		private EmfPlusRegion(boolean empty, boolean infinite) {
			this(0, null, null, null, null, empty, infinite);
		}

		private EmfPlusRegion(int combineMode, EmfPlusRegion left, EmfPlusRegion right) {
			this(combineMode, null, null, left, right, false, false);
		}

		private EmfPlusRegion(int combineMode, double[] rect, EmfPlusPath path, EmfPlusRegion left, EmfPlusRegion right,
				boolean empty, boolean infinite) {
			this.combineMode = combineMode;
			this.rect = rect;
			this.path = path;
			this.left = left;
			this.right = right;
			this.empty = empty;
			this.infinite = infinite;
		}
	}

	private static class EmfPlusRegionNode {
		private final EmfPlusRegion region;
		private final int length;

		private EmfPlusRegionNode(EmfPlusRegion region, int length) {
			this.region = region;
			this.length = length;
		}
	}

	private static class EmfPlusFont {
		private final double emSize;
		private final int sizeUnit;
		private final int styleFlags;
		private final String familyName;

		private EmfPlusFont(double emSize, int sizeUnit, int styleFlags, String familyName) {
			this.emSize = emSize;
			this.sizeUnit = sizeUnit;
			this.styleFlags = styleFlags;
			this.familyName = familyName;
		}
	}

	private static class EmfPlusStringFormat {
		private final int flags;
		private final int alignment;
		private final int lineAlign;
		private final int hotkeyPrefix;
		private final double tracking;

		private EmfPlusStringFormat(int flags, int alignment, int lineAlign) {
			this(flags, alignment, lineAlign, 0, 0.0);
		}

		private EmfPlusStringFormat(int flags, int alignment, int lineAlign, int hotkeyPrefix, double tracking) {
			this.flags = flags;
			this.alignment = alignment;
			this.lineAlign = lineAlign;
			this.hotkeyPrefix = hotkeyPrefix;
			this.tracking = tracking;
		}
	}

	private static class EmfPlusImageAttributes {
		private final int wrapMode;
		private final int clampColor;
		private final int objectClamp;

		private EmfPlusImageAttributes(int wrapMode, int clampColor, int objectClamp) {
			this.wrapMode = wrapMode;
			this.clampColor = clampColor;
			this.objectClamp = objectClamp;
		}
	}

	private static class EmfPlusImageEffect {
		private final double[][] colorMatrix;
		private final byte[][] lookupTables;

		private EmfPlusImageEffect(double[][] colorMatrix, byte[][] lookupTables) {
			this.colorMatrix = colorMatrix;
			this.lookupTables = lookupTables;
		}
	}

	private static class EmfPlusText {
		private final String text;
		private final AttributedString attributed;

		private EmfPlusText(String text, AttributedString attributed) {
			this.text = text;
			this.attributed = attributed;
		}
	}
}

/*
 * Copyright 2026 Hidekatsu Izuno
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
package net.arnx.wmf2svg.gdi.emf;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiColorSpace;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GradientRect;
import net.arnx.wmf2svg.gdi.GradientTriangle;
import net.arnx.wmf2svg.gdi.GdiObject;
import net.arnx.wmf2svg.gdi.GdiPalette;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.GdiRegion;
import net.arnx.wmf2svg.gdi.GdiUtils;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Trivertex;
import net.arnx.wmf2svg.io.DataInput;
import net.arnx.wmf2svg.io.Parser;

/**
 * Minimal EMF parser scaffold focused on overlay geometry stored in
 * META_ESCAPE_ENHANCED_METAFILE comments.
 */
public class EmfParser implements Parser {
	private static final int EMR_HEADER = 1;
	private static final int EMR_POLYBEZIER = 2;
	private static final int EMR_POLYGON = 3;
	private static final int EMR_POLYLINE = 4;
	private static final int EMR_POLYBEZIERTO = 5;
	private static final int EMR_POLYLINETO = 6;
	private static final int EMR_POLYPOLYLINE = 7;
	private static final int EMR_POLYPOLYGON = 8;
	private static final int EMR_SETWINDOWEXTEX = 9;
	private static final int EMR_SETWINDOWORGEX = 10;
	private static final int EMR_SETVIEWPORTEXTEX = 11;
	private static final int EMR_SETVIEWPORTORGEX = 12;
	private static final int EMR_SETBRUSHORGEX = 13;
	private static final int EMR_SETPIXELV = 15;
	private static final int EMR_SETMAPPERFLAGS = 16;
	private static final int EMR_SETMAPMODE = 17;
	private static final int EMR_SETBKMODE = 18;
	private static final int EMR_SETPOLYFILLMODE = 19;
	private static final int EMR_SETROP2 = 20;
	private static final int EMR_SETSTRETCHBLTMODE = 21;
	private static final int EMR_SETTEXTALIGN = 22;
	private static final int EMR_SETCOLORADJUSTMENT = 23;
	private static final int EMR_SETTEXTCOLOR = 24;
	private static final int EMR_SETBKCOLOR = 25;
	private static final int EMR_OFFSETCLIPRGN = 26;
	private static final int EMR_MOVETOEX = 27;
	private static final int EMR_SETMETARGN = 28;
	private static final int EMR_EXCLUDECLIPRECT = 29;
	private static final int EMR_INTERSECTCLIPRECT = 30;
	private static final int EMR_SCALEVIEWPORTEXTEX = 31;
	private static final int EMR_SCALEWINDOWEXTEX = 32;
	private static final int EMR_SAVEDC = 33;
	private static final int EMR_RESTOREDC = 34;
	private static final int EMR_SETWORLDTRANSFORM = 35;
	private static final int EMR_MODIFYWORLDTRANSFORM = 36;
	private static final int EMR_SELECTOBJECT = 37;
	private static final int EMR_CREATEPEN = 38;
	private static final int EMR_CREATEBRUSHINDIRECT = 39;
	private static final int EMR_DELETEOBJECT = 40;
	private static final int EMR_ANGLEARC = 41;
	private static final int EMR_ELLIPSE = 42;
	private static final int EMR_RECTANGLE = 43;
	private static final int EMR_ROUNDRECT = 44;
	private static final int EMR_ARC = 45;
	private static final int EMR_CHORD = 46;
	private static final int EMR_PIE = 47;
	private static final int EMR_SELECTPALETTE = 48;
	private static final int EMR_CREATEPALETTE = 49;
	private static final int EMR_SETPALETTEENTRIES = 50;
	private static final int EMR_RESIZEPALETTE = 51;
	private static final int EMR_REALIZEPALETTE = 52;
	private static final int EMR_EXTFLOODFILL = 53;
	private static final int EMR_LINETO = 54;
	private static final int EMR_ARCTO = 55;
	private static final int EMR_POLYDRAW = 56;
	private static final int EMR_SETARCDIRECTION = 57;
	private static final int EMR_SETMITERLIMIT = 58;
	private static final int EMR_BEGINPATH = 59;
	private static final int EMR_ENDPATH = 60;
	private static final int EMR_CLOSEFIGURE = 61;
	private static final int EMR_FILLPATH = 62;
	private static final int EMR_STROKEANDFILLPATH = 63;
	private static final int EMR_STROKEPATH = 64;
	private static final int EMR_FLATTENPATH = 65;
	private static final int EMR_WIDENPATH = 66;
	private static final int EMR_SELECTCLIPPATH = 67;
	private static final int EMR_ABORTPATH = 68;
	private static final int EMR_GDICOMMENT = 70;
	private static final int EMR_FILLRGN = 71;
	private static final int EMR_FRAMERGN = 72;
	private static final int EMR_INVERTRGN = 73;
	private static final int EMR_PAINTRGN = 74;
	private static final int EMR_EXTSELECTCLIPRGN = 75;
	private static final int EMR_BITBLT = 76;
	private static final int EMR_STRETCHBLT = 77;
	private static final int EMR_MASKBLT = 78;
	private static final int EMR_PLGBLT = 79;
	private static final int EMR_SETDIBITSTODEVICE = 80;
	private static final int EMR_STRETCHDIBITS = 81;
	private static final int EMR_EXTCREATEFONTINDIRECTW = 82;
	private static final int EMR_EXTTEXTOUTA = 83;
	private static final int EMR_EXTTEXTOUTW = 84;
	private static final int EMR_POLYBEZIER16 = 85;
	private static final int EMR_POLYGON16 = 86;
	private static final int EMR_POLYLINE16 = 87;
	private static final int EMR_POLYBEZIERTO16 = 88;
	private static final int EMR_POLYLINETO16 = 89;
	private static final int EMR_POLYPOLYLINE16 = 90;
	private static final int EMR_POLYPOLYGON16 = 91;
	private static final int EMR_POLYDRAW16 = 92;
	private static final int EMR_CREATEMONOBRUSH = 93;
	private static final int EMR_CREATEDIBPATTERNBRUSHPT = 94;
	private static final int EMR_EXTCREATEPEN = 95;
	private static final int EMR_POLYTEXTOUTA = 96;
	private static final int EMR_POLYTEXTOUTW = 97;
	private static final int EMR_SETICMMODE = 98;
	private static final int EMR_CREATECOLORSPACE = 99;
	private static final int EMR_SETCOLORSPACE = 100;
	private static final int EMR_DELETECOLORSPACE = 101;
	private static final int EMR_GLSRECORD = 102;
	private static final int EMR_GLSBOUNDEDRECORD = 103;
	private static final int EMR_PIXELFORMAT = 104;
	private static final int EMR_DRAWESCAPE = 105;
	private static final int EMR_EXTESCAPE = 106;
	private static final int EMR_STARTDOC = 107;
	private static final int EMR_SMALLTEXTOUT = 108;
	private static final int EMR_FORCEUFIMAPPING = 109;
	private static final int EMR_NAMEDESCAPE = 110;
	private static final int EMR_COLORCORRECTPALETTE = 111;
	private static final int EMR_SETICMPROFILEA = 112;
	private static final int EMR_SETICMPROFILEW = 113;
	private static final int EMR_ALPHABLEND = 114;
	private static final int EMR_SETLAYOUT = 115;
	private static final int EMR_TRANSPARENTBLT = 116;
	private static final int EMR_RESERVED_117 = 117;
	private static final int EMR_GRADIENTFILL = 118;
	private static final int EMR_SETLINKEDUFIS = 119;
	private static final int EMR_SETTEXTJUSTIFICATION = 120;
	private static final int EMR_COLORMATCHTOTARGETW = 121;
	private static final int EMR_CREATECOLORSPACEW = 122;
	private static final int EMR_EOF = 14;

	private static final int META_ESCAPE_ENHANCED_METAFILE = 0x000F;
	private static final long WMF_COMMENT_IDENTIFIER = 0x43464D57L;
	private static final long ENHANCED_METAFILE_COMMENT = 0x00000001L;

	private static final int MWT_IDENTITY = 1;
	private static final int MWT_LEFTMULTIPLY = 2;
	private static final int MWT_RIGHTMULTIPLY = 3;

	private static final int PT_CLOSEFIGURE = 0x01;
	private static final int PT_LINETO = 0x02;
	private static final int PT_BEZIERTO = 0x04;
	private static final int PT_MOVETO = 0x06;

	private static final int ETO_NO_RECT = 0x00000100;
	private static final int ETO_SMALL_CHARS = 0x00000200;

	private static final int STOCK_WHITE_BRUSH = 0x80000000;
	private static final int STOCK_LTGRAY_BRUSH = 0x80000001;
	private static final int STOCK_GRAY_BRUSH = 0x80000002;
	private static final int STOCK_DKGRAY_BRUSH = 0x80000003;
	private static final int STOCK_BLACK_BRUSH = 0x80000004;
	private static final int STOCK_NULL_BRUSH = 0x80000005;
	private static final int STOCK_WHITE_PEN = 0x80000006;
	private static final int STOCK_BLACK_PEN = 0x80000007;
	private static final int STOCK_NULL_PEN = 0x80000008;

	private final boolean manageLifecycle;

	public EmfParser() {
		this(true);
	}

	public EmfParser(boolean manageLifecycle) {
		this.manageLifecycle = manageLifecycle;
	}

	public void parse(InputStream is, Gdi gdi) throws IOException, EmfParseException {
		DataInput in = new DataInput(new BufferedInputStream(is), ByteOrder.LITTLE_ENDIAN);
		Map<Integer, GdiObject> objects = new HashMap<Integer, GdiObject>();
		Map<Integer, GdiObject> stockObjects = new HashMap<Integer, GdiObject>();
		Map<Integer, Integer> fontCharsets = new HashMap<Integer, Integer>();
		LinkedList<double[]> transforms = new LinkedList<double[]>();
		double[] transform = identity();
		int textCharset = GdiFont.ANSI_CHARSET;

		while (true) {
			int type = (int)in.readUint32();
			int size = (int)in.readUint32();
			if (size < 8) {
				throw new EmfParseException("invalid EMF record size: " + size);
			}

			byte[] data = in.readBytes(size - 8);
			switch (type) {
			case EMR_HEADER:
				if (manageLifecycle) {
					gdi.header();
					readHeader(data, gdi);
				}
				break;
			case EMR_POLYBEZIER: {
				Point[] points = readPoints32(data, transform);
				gdi.polyBezier(points);
				break;
			}
			case EMR_POLYBEZIERTO: {
				Point[] points = readPoints32(data, transform);
				gdi.polyBezierTo(points);
				break;
			}
			case EMR_POLYGON: {
				Point[] points = readPoints32(data, transform);
				gdi.polygon(points);
				break;
			}
			case EMR_POLYLINE:
			case EMR_POLYLINETO: {
				Point[] points = readPoints32(data, transform);
				gdi.polyline(points);
				break;
			}
			case EMR_POLYPOLYLINE:
				for (Point[] points : readPolyPoints32(data, transform)) {
					gdi.polyline(points);
				}
				break;
			case EMR_POLYPOLYGON:
				gdi.polyPolygon(readPolyPoints32(data, transform));
				break;
			case EMR_SETWINDOWEXTEX:
				gdi.setWindowExtEx(readInt32(data, 0), readInt32(data, 4), null);
				break;
			case EMR_SETWINDOWORGEX:
				gdi.setWindowOrgEx(readInt32(data, 0), readInt32(data, 4), null);
				break;
			case EMR_SETVIEWPORTEXTEX:
				gdi.setViewportExtEx(readInt32(data, 0), readInt32(data, 4), null);
				break;
			case EMR_SETVIEWPORTORGEX:
				gdi.setViewportOrgEx(readInt32(data, 0), readInt32(data, 4), null);
				break;
			case EMR_SETBRUSHORGEX:
				gdi.setBrushOrgEx(readInt32(data, 0), readInt32(data, 4), null);
				break;
			case EMR_SETPIXELV: {
				Point point = transformPoint(transform, readInt32(data, 0), readInt32(data, 4));
				gdi.setPixel(point.x, point.y, readInt32(data, 8));
				break;
			}
			case EMR_SETMAPPERFLAGS:
				gdi.setMapperFlags(readUInt32(data, 0));
				break;
			case EMR_SETMAPMODE:
				gdi.setMapMode(readInt32(data, 0));
				break;
			case EMR_SETBKMODE:
				gdi.setBkMode(readInt32(data, 0));
				break;
			case EMR_SETPOLYFILLMODE:
				gdi.setPolyFillMode(readInt32(data, 0));
				break;
			case EMR_SETROP2:
				gdi.setROP2(readInt32(data, 0));
				break;
			case EMR_SETSTRETCHBLTMODE:
				gdi.setStretchBltMode(readInt32(data, 0));
				break;
			case EMR_SETTEXTALIGN:
				gdi.setTextAlign(readInt32(data, 0));
				break;
			case EMR_SETCOLORADJUSTMENT:
				gdi.setColorAdjustment(copyRange(data, 0, data.length));
				break;
			case EMR_SETTEXTCOLOR:
				gdi.setTextColor(readInt32(data, 0));
				break;
			case EMR_SETBKCOLOR:
				gdi.setBkColor(readInt32(data, 0));
				break;
			case EMR_OFFSETCLIPRGN:
				gdi.offsetClipRgn(readInt32(data, 0), readInt32(data, 4));
				break;
			case EMR_MOVETOEX: {
				Point point = transformPoint(transform, readInt32(data, 0), readInt32(data, 4));
				gdi.moveToEx(point.x, point.y, null);
				break;
			}
			case EMR_SETMETARGN:
				gdi.setMetaRgn();
				break;
			case EMR_EXCLUDECLIPRECT: {
				int[] rect = transformRect(transform, data, 0);
				gdi.excludeClipRect(rect[0], rect[1], rect[2], rect[3]);
				break;
			}
			case EMR_INTERSECTCLIPRECT: {
				int[] rect = transformRect(transform, data, 0);
				gdi.intersectClipRect(rect[0], rect[1], rect[2], rect[3]);
				break;
			}
			case EMR_SCALEVIEWPORTEXTEX:
				gdi.scaleViewportExtEx(readInt32(data, 0), readInt32(data, 4), readInt32(data, 8), readInt32(data, 12), null);
				break;
			case EMR_SCALEWINDOWEXTEX:
				gdi.scaleWindowExtEx(readInt32(data, 0), readInt32(data, 4), readInt32(data, 8), readInt32(data, 12), null);
				break;
			case EMR_SAVEDC:
				transforms.push(copy(transform));
				gdi.seveDC();
				break;
			case EMR_RESTOREDC: {
				int saved = readInt32(data, 0);
				if (saved == -1 && !transforms.isEmpty()) {
					transform = transforms.pop();
				} else if (saved < -1 && transforms.size() >= -saved) {
					for (int i = -1; i >= saved; i--) {
						transform = transforms.pop();
					}
				} else if (saved == 0) {
					transform = identity();
					transforms.clear();
				}
				gdi.restoreDC(saved);
				break;
			}
			case EMR_SETWORLDTRANSFORM:
				transform = readTransform(data, 0);
				break;
			case EMR_MODIFYWORLDTRANSFORM:
				transform = modifyWorldTransform(transform, readTransform(data, 0), readInt32(data, 24));
				break;
			case EMR_SELECTOBJECT: {
				int id = readInt32(data, 0);
				selectObject(gdi, objects, stockObjects, id);
				Integer charset = fontCharsets.get(id);
				if (charset != null) {
					textCharset = charset.intValue();
				}
				break;
			}
			case EMR_CREATEPEN:
				objects.put(readInt32(data, 0), gdi.createPenIndirect(readInt32(data, 4) & 0xFF, Math.max(1, Math.abs(readInt32(data, 8))), readInt32(data, 16)));
				break;
			case EMR_CREATEBRUSHINDIRECT:
				objects.put(readInt32(data, 0), gdi.createBrushIndirect(readInt32(data, 4), readInt32(data, 8), readInt32(data, 12)));
				break;
			case EMR_DELETEOBJECT: {
				int id = readInt32(data, 0);
				GdiObject obj = objects.remove(id);
				fontCharsets.remove(id);
				if (obj != null) {
					gdi.deleteObject(obj);
				}
				break;
			}
			case EMR_ANGLEARC:
				gdi.angleArc(readInt32(data, 0), readInt32(data, 4), readInt32(data, 8),
						readFloat32(data, 12), readFloat32(data, 16));
				break;
			case EMR_ELLIPSE: {
				int[] rect = transformRect(transform, data, 0);
				gdi.ellipse(rect[0], rect[1], rect[2], rect[3]);
				break;
			}
			case EMR_RECTANGLE: {
				int[] rect = transformRect(transform, data, 0);
				gdi.rectangle(rect[0], rect[1], rect[2], rect[3]);
				break;
			}
			case EMR_ROUNDRECT: {
				int[] rect = transformRect(transform, data, 0);
				gdi.roundRect(rect[0], rect[1], rect[2], rect[3], readInt32(data, 16), readInt32(data, 20));
				break;
			}
			case EMR_ARC: {
				int[] rect = transformRect(transform, data, 0);
				Point start = transformPoint(transform, readInt32(data, 16), readInt32(data, 20));
				Point end = transformPoint(transform, readInt32(data, 24), readInt32(data, 28));
				gdi.arc(rect[0], rect[1], rect[2], rect[3], start.x, start.y, end.x, end.y);
				break;
			}
			case EMR_ARCTO: {
				int[] rect = transformRect(transform, data, 0);
				Point start = transformPoint(transform, readInt32(data, 16), readInt32(data, 20));
				Point end = transformPoint(transform, readInt32(data, 24), readInt32(data, 28));
				gdi.arcTo(rect[0], rect[1], rect[2], rect[3], start.x, start.y, end.x, end.y);
				break;
			}
			case EMR_CHORD: {
				int[] rect = transformRect(transform, data, 0);
				Point start = transformPoint(transform, readInt32(data, 16), readInt32(data, 20));
				Point end = transformPoint(transform, readInt32(data, 24), readInt32(data, 28));
				gdi.chord(rect[0], rect[1], rect[2], rect[3], start.x, start.y, end.x, end.y);
				break;
			}
			case EMR_PIE: {
				int[] rect = transformRect(transform, data, 0);
				Point start = transformPoint(transform, readInt32(data, 16), readInt32(data, 20));
				Point end = transformPoint(transform, readInt32(data, 24), readInt32(data, 28));
				gdi.pie(rect[0], rect[1], rect[2], rect[3], start.x, start.y, end.x, end.y);
				break;
			}
			case EMR_SELECTPALETTE: {
				GdiObject obj = getObject(objects, stockObjects, readInt32(data, 0));
				if (obj instanceof GdiPalette) {
					gdi.selectPalette((GdiPalette)obj, false);
				}
				break;
			}
			case EMR_CREATEPALETTE:
				objects.put(readInt32(data, 0), readPalette(data, 4, gdi));
				break;
			case EMR_SETPALETTEENTRIES: {
				GdiObject obj = getObject(objects, stockObjects, readInt32(data, 0));
				if (obj instanceof GdiPalette) {
					int startIndex = readInt32(data, 4);
					int count = readInt32(data, 8);
					gdi.setPaletteEntries((GdiPalette)obj, startIndex, readPaletteEntries(data, 12, count));
				}
				break;
			}
			case EMR_RESIZEPALETTE: {
				GdiObject obj = getObject(objects, stockObjects, readInt32(data, 0));
				if (obj instanceof GdiPalette) {
					gdi.resizePalette((GdiPalette)obj, readInt32(data, 4));
				}
				break;
			}
			case EMR_REALIZEPALETTE:
				gdi.realizePalette();
				break;
			case EMR_EXTFLOODFILL: {
				Point point = transformPoint(transform, readInt32(data, 0), readInt32(data, 4));
				gdi.extFloodFill(point.x, point.y, readInt32(data, 8), readInt32(data, 12));
				break;
			}
			case EMR_LINETO: {
				Point point = transformPoint(transform, readInt32(data, 0), readInt32(data, 4));
				gdi.lineTo(point.x, point.y);
				break;
			}
			case EMR_POLYDRAW:
				readPolyDraw(data, transform, gdi, false);
				break;
			case EMR_SETARCDIRECTION:
				gdi.setArcDirection(readInt32(data, 0));
				break;
			case EMR_SETMITERLIMIT:
				gdi.setMiterLimit(readFloat32(data, 0));
				break;
			case EMR_POLYGON16:
				gdi.polygon(readPoints16(data, transform));
				break;
			case EMR_POLYLINE16:
			case EMR_POLYLINETO16:
				gdi.polyline(readPoints16(data, transform));
				break;
			case EMR_POLYBEZIER16:
				gdi.polyBezier(readPoints16(data, transform));
				break;
			case EMR_POLYBEZIERTO16:
				gdi.polyBezierTo(readPoints16(data, transform));
				break;
			case EMR_POLYPOLYLINE16:
				for (Point[] points : readPolyPoints16(data, transform)) {
					gdi.polyline(points);
				}
				break;
			case EMR_POLYPOLYGON16:
				gdi.polyPolygon(readPolyPoints16(data, transform));
				break;
			case EMR_POLYDRAW16:
				readPolyDraw(data, transform, gdi, true);
				break;
			case EMR_CREATEMONOBRUSH:
			case EMR_CREATEDIBPATTERNBRUSHPT: {
				byte[] image = readBitmap(data, 8, 12, 16, 20);
				if (image != null) {
					objects.put(readInt32(data, 0), gdi.dibCreatePatternBrush(image, readInt32(data, 4)));
				}
				break;
			}
			case EMR_EXTCREATEPEN:
				objects.put(readInt32(data, 0), gdi.createPenIndirect(readInt32(data, 20) & 0xFF, Math.max(1, Math.abs(readInt32(data, 24))), readInt32(data, 32)));
				break;
			case EMR_SETICMMODE:
				gdi.setICMMode(readInt32(data, 0));
				break;
			case EMR_CREATECOLORSPACE:
				objects.put(readInt32(data, 0), gdi.createColorSpace(copyRange(data, 4, data.length - 4)));
				break;
			case EMR_SETCOLORSPACE: {
				GdiObject obj = getObject(objects, stockObjects, readInt32(data, 0));
				if (obj instanceof GdiColorSpace) {
					gdi.setColorSpace((GdiColorSpace)obj);
				}
				break;
			}
			case EMR_DELETECOLORSPACE: {
				GdiObject obj = objects.remove(readInt32(data, 0));
				if (obj instanceof GdiColorSpace) {
					gdi.deleteColorSpace((GdiColorSpace)obj);
				}
				break;
			}
			case EMR_GLSRECORD:
			case EMR_GLSBOUNDEDRECORD:
			case EMR_PIXELFORMAT:
			case EMR_STARTDOC:
			case EMR_FORCEUFIMAPPING:
			case EMR_RESERVED_117:
			case EMR_SETLINKEDUFIS:
				break;
			case EMR_COLORMATCHTOTARGETW:
				readColorMatchToTarget(data, gdi);
				break;
			case EMR_DRAWESCAPE:
			case EMR_EXTESCAPE:
				readEscape(data, gdi);
				break;
			case EMR_NAMEDESCAPE:
				readNamedEscape(data, gdi);
				break;
			case EMR_COLORCORRECTPALETTE: {
				if (data.length >= 12) {
					GdiObject obj = getObject(objects, stockObjects, readInt32(data, 0));
					if (obj instanceof GdiPalette) {
						gdi.colorCorrectPalette((GdiPalette)obj, readInt32(data, 4), readInt32(data, 8));
					}
				}
				break;
			}
			case EMR_SETICMPROFILEA:
			case EMR_SETICMPROFILEW:
				readSetICMProfile(data, gdi);
				break;
			case EMR_GDICOMMENT: {
				if (data.length >= 4) {
					int commentSize = readInt32(data, 0);
					if (commentSize > 0 && commentSize <= data.length - 4) {
						byte[] comment = new byte[commentSize];
						System.arraycopy(data, 4, comment, 0, commentSize);
						gdi.comment(comment);
					}
				}
				break;
			}
			case EMR_FILLRGN: {
				GdiRegion region = readRegion(data, 24, readInt32(data, 16), transform, gdi);
				GdiObject obj = getObject(gdi, objects, stockObjects, readInt32(data, 20));
				if (region != null && obj instanceof GdiBrush) {
					gdi.fillRgn(region, (GdiBrush)obj);
				}
				break;
			}
			case EMR_FRAMERGN: {
				GdiRegion region = readRegion(data, 32, readInt32(data, 16), transform, gdi);
				GdiObject obj = getObject(gdi, objects, stockObjects, readInt32(data, 20));
				if (region != null && obj instanceof GdiBrush) {
					gdi.frameRgn(region, (GdiBrush)obj, readInt32(data, 24), readInt32(data, 28));
				}
				break;
			}
			case EMR_INVERTRGN: {
				GdiRegion region = readRegion(data, 20, readInt32(data, 16), transform, gdi);
				if (region != null) {
					gdi.invertRgn(region);
				}
				break;
			}
			case EMR_PAINTRGN: {
				GdiRegion region = readRegion(data, 20, readInt32(data, 16), transform, gdi);
				if (region != null) {
					gdi.paintRgn(region);
				}
				break;
			}
			case EMR_EXTSELECTCLIPRGN: {
				int regionSize = readInt32(data, 0);
				int mode = readInt32(data, 4);
				if (regionSize == 0) {
					gdi.extSelectClipRgn(null, mode);
				} else {
					GdiRegion region = readRegion(data, 8, regionSize, transform, gdi);
					if (region != null) {
						gdi.extSelectClipRgn(region, mode);
					}
				}
				break;
			}
			case EMR_BITBLT:
				readBitBlt(data, transform, gdi);
				break;
			case EMR_STRETCHBLT:
				readStretchBlt(data, transform, gdi);
				break;
			case EMR_MASKBLT:
				readMaskBlt(data, transform, gdi);
				break;
			case EMR_PLGBLT:
				readPlgBlt(data, transform, gdi);
				break;
			case EMR_SETDIBITSTODEVICE:
				readSetDIBitsToDevice(data, transform, gdi);
				break;
			case EMR_STRETCHDIBITS:
				readStretchDIBits(data, transform, gdi);
				break;
			case EMR_EXTCREATEFONTINDIRECTW: {
				int id = readInt32(data, 0);
				int charset = readUInt8(data, 27);
				fontCharsets.put(id, Integer.valueOf(charset));
				objects.put(id, gdi.createFontIndirect(
						readInt32(data, 4),
						readInt32(data, 8),
						readInt32(data, 12),
						readInt32(data, 16),
						readInt32(data, 20),
						readUInt8(data, 24) != 0,
						readUInt8(data, 25) != 0,
						readUInt8(data, 26) != 0,
						charset,
						readUInt8(data, 28),
						readUInt8(data, 29),
						readUInt8(data, 30),
						readUInt8(data, 31),
						readUtf16StringBytes(data, 32, 32, charset)));
				break;
			}
			case EMR_EXTTEXTOUTA:
				readExtTextOut(data, transform, gdi, false, textCharset);
				break;
			case EMR_EXTTEXTOUTW:
				readExtTextOut(data, transform, gdi, true, textCharset);
				break;
			case EMR_POLYTEXTOUTA:
				readPolyTextOut(data, transform, gdi, false, textCharset);
				break;
			case EMR_POLYTEXTOUTW:
				readPolyTextOut(data, transform, gdi, true, textCharset);
				break;
			case EMR_ALPHABLEND:
				readAlphaBlend(data, transform, gdi);
				break;
			case EMR_SETLAYOUT:
				gdi.setLayout(readUInt32(data, 0));
				break;
			case EMR_TRANSPARENTBLT:
				readTransparentBlt(data, transform, gdi);
				break;
			case EMR_GRADIENTFILL:
				readGradientFill(data, transform, gdi);
				break;
			case EMR_SMALLTEXTOUT:
				readSmallTextOut(data, transform, gdi, textCharset);
				break;
			case EMR_SETTEXTJUSTIFICATION:
				gdi.setTextJustification(readInt32(data, 0), readInt32(data, 4));
				break;
			case EMR_CREATECOLORSPACEW:
				objects.put(readInt32(data, 0), gdi.createColorSpaceW(copyRange(data, 4, data.length - 4)));
				break;
			case EMR_BEGINPATH:
				gdi.beginPath();
				break;
			case EMR_ENDPATH:
				gdi.endPath();
				break;
			case EMR_CLOSEFIGURE:
				gdi.closeFigure();
				break;
			case EMR_FILLPATH:
				gdi.fillPath();
				break;
			case EMR_STROKEANDFILLPATH:
				gdi.strokeAndFillPath();
				break;
			case EMR_STROKEPATH:
				gdi.strokePath();
				break;
			case EMR_FLATTENPATH:
				gdi.flattenPath();
				break;
			case EMR_WIDENPATH:
				gdi.widenPath();
				break;
			case EMR_SELECTCLIPPATH:
				gdi.selectClipPath(readInt32(data, 0));
				break;
			case EMR_ABORTPATH:
				gdi.abortPath();
				break;
			case EMR_EOF:
				if (manageLifecycle) {
					gdi.footer();
				}
				return;
			default:
				break;
			}
		}
	}

	public static boolean parseEscape(byte[] data, Gdi gdi) {
		if (!isEnhancedMetafileEscape(data)) {
			return false;
		}
		gdi.comment(data);
		return true;
	}

	public static boolean isEnhancedMetafileEscape(byte[] data) {
		if (data == null || data.length < 38) {
			return false;
		}
		return readInt16(data, 0) == META_ESCAPE_ENHANCED_METAFILE
				&& readUInt32(data, 4) == WMF_COMMENT_IDENTIFIER
				&& readUInt32(data, 8) == ENHANCED_METAFILE_COMMENT;
	}

	public static int getEnhancedMetafileTotalSize(byte[] data) {
		return readInt32(data, 34);
	}

	public static byte[] getEnhancedMetafileBytes(byte[] data) {
		int size = readInt32(data, 26);
		byte[] bytes = new byte[size];
		System.arraycopy(data, 38, bytes, 0, size);
		return bytes;
	}

	private static void readHeader(byte[] data, Gdi gdi) {
		if (data.length < 16) {
			return;
		}

		int left = readInt32(data, 0);
		int top = readInt32(data, 4);
		int right = readInt32(data, 8);
		int bottom = readInt32(data, 12);
		int width = right - left;
		int height = bottom - top;
		if (width != 0 && height != 0) {
			gdi.setWindowOrgEx(0, 0, null);
			gdi.setWindowExtEx(width, height, null);
		}
	}

	private GdiObject getObject(Map<Integer, GdiObject> objects, Map<Integer, GdiObject> stockObjects, int id) {
		GdiObject obj = objects.get(id);
		if (obj == null) {
			obj = stockObjects.get(id);
		}
		return obj;
	}

	private GdiObject getObject(Gdi gdi, Map<Integer, GdiObject> objects, Map<Integer, GdiObject> stockObjects, int id) {
		GdiObject obj = getObject(objects, stockObjects, id);
		if (obj == null) {
			obj = createStockObject(gdi, id);
			if (obj != null) {
				stockObjects.put(id, obj);
			}
		}
		return obj;
	}

	private void selectObject(Gdi gdi, Map<Integer, GdiObject> objects, Map<Integer, GdiObject> stockObjects, int id) {
		GdiObject obj = getObject(gdi, objects, stockObjects, id);
		if (obj != null) {
			gdi.selectObject(obj);
		}
	}

	private GdiObject createStockObject(Gdi gdi, int id) {
		switch (id) {
		case STOCK_WHITE_BRUSH:
			return gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00FFFFFF, 0);
		case STOCK_LTGRAY_BRUSH:
			return gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00C0C0C0, 0);
		case STOCK_GRAY_BRUSH:
			return gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00808080, 0);
		case STOCK_DKGRAY_BRUSH:
			return gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00404040, 0);
		case STOCK_BLACK_BRUSH:
			return gdi.createBrushIndirect(GdiBrush.BS_SOLID, 0x00000000, 0);
		case STOCK_NULL_BRUSH:
			return gdi.createBrushIndirect(GdiBrush.BS_NULL, 0x00000000, 0);
		case STOCK_WHITE_PEN:
			return gdi.createPenIndirect(GdiPen.PS_SOLID, 1, 0x00FFFFFF);
		case STOCK_BLACK_PEN:
			return gdi.createPenIndirect(GdiPen.PS_SOLID, 1, 0x00000000);
		case STOCK_NULL_PEN:
			return gdi.createPenIndirect(GdiPen.PS_NULL, 1, 0x00000000);
		default:
			return null;
		}
	}

	private static GdiPalette readPalette(byte[] data, int offset, Gdi gdi) {
		int version = readUInt16(data, offset);
		int count = readUInt16(data, offset + 2);
		return gdi.createPalette(version, readPaletteEntries(data, offset + 4, count));
	}

	private static int[] readPaletteEntries(byte[] data, int offset, int count) {
		if (count <= 0 || offset < 0 || offset + count * 4 > data.length) {
			return new int[0];
		}
		int[] entries = new int[count];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = readInt32(data, offset + i * 4);
		}
		return entries;
	}

	private static void readEscape(byte[] data, Gdi gdi) {
		if (data.length < 8) {
			return;
		}
		int escapeFunction = readInt32(data, 0);
		int count = readInt32(data, 4);
		if (count < 0 || count > data.length - 8) {
			return;
		}

		byte[] escape = new byte[4 + count];
		escape[0] = (byte)escapeFunction;
		escape[1] = (byte)(escapeFunction >>> 8);
		escape[2] = (byte)count;
		escape[3] = (byte)(count >>> 8);
		System.arraycopy(data, 8, escape, 4, count);
		if (!parseEscape(escape, gdi)) {
			gdi.escape(escape);
		}
	}

	private static void readNamedEscape(byte[] data, Gdi gdi) {
		if (data.length < 12) {
			return;
		}
		int escapeFunction = readInt32(data, 0);
		int driverLength = readInt32(data, 4);
		int count = readInt32(data, 8);
		int dataOffset = 12 + driverLength;
		if (driverLength < 0 || count < 0 || dataOffset < 12 || dataOffset > data.length
				|| count > data.length - dataOffset) {
			return;
		}

		byte[] escape = new byte[4 + count];
		escape[0] = (byte)escapeFunction;
		escape[1] = (byte)(escapeFunction >>> 8);
		escape[2] = (byte)count;
		escape[3] = (byte)(count >>> 8);
		System.arraycopy(data, dataOffset, escape, 4, count);
		if (!parseEscape(escape, gdi)) {
			gdi.escape(escape);
		}
	}

	private static void readSetICMProfile(byte[] data, Gdi gdi) {
		if (data.length < 12) {
			return;
		}
		int nameSize = readInt32(data, 4);
		if (nameSize < 0 || nameSize > data.length - 12) {
			return;
		}
		gdi.setICMProfile(copyRange(data, 12, nameSize));
	}

	private static void readColorMatchToTarget(byte[] data, Gdi gdi) {
		if (data.length < 16) {
			return;
		}
		int profileSize = readInt32(data, 8);
		if (profileSize < 0 || profileSize > data.length - 16) {
			return;
		}
		gdi.colorMatchToTarget(readInt32(data, 0), readInt32(data, 4), copyRange(data, 16, profileSize));
	}

	private static void readPolyDraw(byte[] data, double[] transform, Gdi gdi, boolean shortPoints) {
		int count = readInt32(data, 16);
		int pointOffset = 20;
		int typeOffset = pointOffset + count * (shortPoints ? 4 : 8);
		if (count <= 0 || typeOffset + count > data.length) {
			return;
		}

		Point figureStart = null;
		for (int i = 0; i < count; i++) {
			Point point = shortPoints
					? transformPoint(transform, readInt16(data, pointOffset), readInt16(data, pointOffset + 2))
					: transformPoint(transform, readInt32(data, pointOffset), readInt32(data, pointOffset + 4));
			int type = readUInt8(data, typeOffset + i);
			int closeType = type;
			int operation = type & ~PT_CLOSEFIGURE;

			switch (operation) {
			case PT_MOVETO:
				gdi.moveToEx(point.x, point.y, null);
				figureStart = point;
				break;
			case PT_LINETO:
				gdi.lineTo(point.x, point.y);
				break;
			case PT_BEZIERTO:
				Point[] bezier = null;
				if (i + 2 < count) {
					Point control1 = point;
					Point control2 = shortPoints
							? transformPoint(transform, readInt16(data, pointOffset + 4), readInt16(data, pointOffset + 6))
							: transformPoint(transform, readInt32(data, pointOffset + 8), readInt32(data, pointOffset + 12));
					i += 2;
					pointOffset += (shortPoints ? 8 : 16);
					closeType = readUInt8(data, typeOffset + i);
					point = shortPoints
							? transformPoint(transform, readInt16(data, pointOffset), readInt16(data, pointOffset + 2))
							: transformPoint(transform, readInt32(data, pointOffset), readInt32(data, pointOffset + 4));
					bezier = new Point[] { control1, control2, point };
				}
				if (bezier != null) {
					gdi.polyBezierTo(bezier);
				} else {
					gdi.lineTo(point.x, point.y);
				}
				break;
			default:
				break;
			}

			if ((closeType & PT_CLOSEFIGURE) != 0) {
				if (figureStart != null) {
					gdi.lineTo(figureStart.x, figureStart.y);
				}
			}
			pointOffset += (shortPoints ? 4 : 8);
		}
	}

	private static void readBitBlt(byte[] data, double[] transform, Gdi gdi) {
		int[] rect = transformDestRect(transform, data, 16, 20, 24, 28);
		long rop = readUInt32(data, 32);
		byte[] image = readBitmap(data, 76, 80, 84, 88);
		if (image == null) {
			if (canPatBltWithoutSource(rop)) {
				gdi.patBlt(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1], rop);
			}
			return;
		}
		gdi.bitBlt(image, rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1],
				readInt32(data, 36), readInt32(data, 40), rop);
	}

	private static void readStretchBlt(byte[] data, double[] transform, Gdi gdi) {
		int[] rect = transformDestRect(transform, data, 16, 20, 24, 28);
		long rop = readUInt32(data, 32);
		byte[] image = readBitmap(data, 76, 80, 84, 88);
		if (image == null) {
			if (canPatBltWithoutSource(rop)) {
				gdi.patBlt(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1], rop);
			}
			return;
		}
		gdi.stretchBlt(image, rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1],
				readInt32(data, 36), readInt32(data, 40), readInt32(data, 92), readInt32(data, 96),
				rop);
	}

	private static void readMaskBlt(byte[] data, double[] transform, Gdi gdi) {
		if (data.length < 120) {
			return;
		}

		int[] rect = transformDestRect(transform, data, 16, 20, 24, 28);
		long rop = readUInt32(data, 32);
		byte[] image = readBitmap(data, 76, 80, 84, 88);
		if (image == null) {
			if (canPatBltWithoutSource(rop)) {
				gdi.patBlt(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1], rop);
			}
			return;
		}

		gdi.maskBlt(image, rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1],
				readInt32(data, 36), readInt32(data, 40),
				readBitmap(data, 104, 108, 112, 116),
				readInt32(data, 92), readInt32(data, 96), rop);
	}

	private static void readPlgBlt(byte[] data, double[] transform, Gdi gdi) {
		if (data.length < 132) {
			return;
		}

		byte[] image = readBitmap(data, 88, 92, 96, 100);
		if (image == null) {
			return;
		}

		Point[] points = new Point[3];
		int offset = 16;
		for (int i = 0; i < points.length; i++) {
			points[i] = transformPoint(transform, readInt32(data, offset), readInt32(data, offset + 4));
			offset += 8;
		}
		gdi.plgBlt(image, points,
				readInt32(data, 40), readInt32(data, 44), readInt32(data, 48), readInt32(data, 52),
				readBitmap(data, 116, 120, 124, 128),
				readInt32(data, 104), readInt32(data, 108));
	}

	private static boolean canPatBltWithoutSource(long rop) {
		return rop == Gdi.BLACKNESS
				|| rop == Gdi.DSTINVERT
				|| rop == Gdi.PATCOPY
				|| rop == Gdi.PATINVERT
				|| rop == Gdi.WHITENESS;
	}

	private static void readSetDIBitsToDevice(byte[] data, double[] transform, Gdi gdi) {
		Point point = transformPoint(transform, readInt32(data, 16), readInt32(data, 20));
		byte[] image = readBitmap(data, 40, 44, 48, 52);
		if (image == null) {
			return;
		}
		gdi.setDIBitsToDevice(point.x, point.y, readInt32(data, 32), readInt32(data, 36),
				readInt32(data, 24), readInt32(data, 28), readInt32(data, 60), readInt32(data, 64),
				image, readInt32(data, 56));
	}

	private static void readStretchDIBits(byte[] data, double[] transform, Gdi gdi) {
		int[] rect = transformDestRect(transform, data, 16, 20, 64, 68);
		byte[] image = readBitmap(data, 40, 44, 48, 52);
		if (image == null) {
			return;
		}
		gdi.stretchDIBits(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1],
				readInt32(data, 24), readInt32(data, 28), readInt32(data, 32), readInt32(data, 36),
				image, readInt32(data, 56), readUInt32(data, 60));
	}

	private static void readAlphaBlend(byte[] data, double[] transform, Gdi gdi) {
		int[] rect = transformDestRect(transform, data, 16, 20, 24, 28);
		byte[] image = readBitmap(data, 76, 80, 84, 88);
		if (image == null) {
			return;
		}
		gdi.alphaBlend(image, rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1],
				readInt32(data, 36), readInt32(data, 40), readInt32(data, 92), readInt32(data, 96),
				readInt32(data, 32));
	}

	private static void readTransparentBlt(byte[] data, double[] transform, Gdi gdi) {
		int[] rect = transformDestRect(transform, data, 16, 20, 24, 28);
		byte[] image = readBitmap(data, 76, 80, 84, 88);
		if (image == null) {
			return;
		}
		gdi.transparentBlt(image, rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1],
				readInt32(data, 36), readInt32(data, 40), readInt32(data, 92), readInt32(data, 96),
				readInt32(data, 32));
	}

	private static void readGradientFill(byte[] data, double[] transform, Gdi gdi) {
		if (data.length < 28) {
			return;
		}
		int vertexCount = readInt32(data, 16);
		int meshCount = readInt32(data, 20);
		int mode = readInt32(data, 24);
		int vertexOffset = 28;
		if (vertexCount < 0 || meshCount < 0 || vertexOffset + vertexCount * 16 > data.length) {
			return;
		}

		Trivertex[] vertex = new Trivertex[vertexCount];
		for (int i = 0; i < vertex.length; i++) {
			int offset = vertexOffset + i * 16;
			Point point = transformPoint(transform, readInt32(data, offset), readInt32(data, offset + 4));
			vertex[i] = new Trivertex(point.x, point.y,
					readUInt16(data, offset + 8),
					readUInt16(data, offset + 10),
					readUInt16(data, offset + 12),
					readUInt16(data, offset + 14));
		}

		int meshOffset = vertexOffset + vertexCount * 16;
		int meshStep = mode == Gdi.GRADIENT_FILL_TRIANGLE ? 12
				: (meshOffset + meshCount * 12 <= data.length ? 12 : 8);
		if (meshOffset + meshCount * meshStep > data.length) {
			return;
		}

		if (mode == Gdi.GRADIENT_FILL_TRIANGLE) {
			GradientTriangle[] mesh = new GradientTriangle[meshCount];
			for (int i = 0; i < mesh.length; i++) {
				int offset = meshOffset + i * meshStep;
				mesh[i] = new GradientTriangle(readInt32(data, offset), readInt32(data, offset + 4),
						readInt32(data, offset + 8));
			}
			gdi.gradientFill(vertex, mesh, mode);
			return;
		}

		GradientRect[] mesh = new GradientRect[meshCount];
		for (int i = 0; i < mesh.length; i++) {
			int offset = meshOffset + i * meshStep;
			mesh[i] = new GradientRect(readInt32(data, offset), readInt32(data, offset + 4));
		}
		gdi.gradientFill(vertex, mesh, mode);
	}

	private static void readSmallTextOut(byte[] data, double[] transform, Gdi gdi, int charset) {
		if (data.length < 28) {
			return;
		}
		int count = readInt32(data, 8);
		int options = readInt32(data, 12);
		int textOffset = 28;

		int[] rect = null;
		if ((options & ETO_NO_RECT) == 0) {
			if (textOffset + 16 > data.length) {
				return;
			}
			if ((options & (Gdi.ETO_CLIPPED | Gdi.ETO_OPAQUE)) != 0) {
				rect = transformRect(transform, data, textOffset);
			}
			textOffset += 16;
		}

		Point point = transformPoint(transform, readInt32(data, 0), readInt32(data, 4));
		byte[] text;
		if ((options & ETO_SMALL_CHARS) != 0) {
			text = copyRange(data, textOffset, count);
		} else {
			text = readUtf16StringBytes(data, textOffset, count, charset);
		}
		gdi.extTextOut(point.x, point.y, options, rect, text, null);
	}

	private static void readExtTextOut(byte[] data, double[] transform, Gdi gdi, boolean unicode, int charset) {
		readEmrText(data, 28, transform, gdi, unicode, charset);
	}

	private static void readPolyTextOut(byte[] data, double[] transform, Gdi gdi, boolean unicode, int charset) {
		if (data.length < 32) {
			return;
		}
		int count = readInt32(data, 28);
		int textOffset = 32;
		for (int i = 0; i < count; i++) {
			if (textOffset + 40 > data.length) {
				return;
			}
			readEmrText(data, textOffset, transform, gdi, unicode, charset);
			textOffset += 40;
		}
	}

	private static void readEmrText(byte[] data, int textOffset, double[] transform, Gdi gdi, boolean unicode, int charset) {
		if (textOffset < 0 || textOffset + 40 > data.length) {
			return;
		}
		Point point = transformPoint(transform, readInt32(data, textOffset), readInt32(data, textOffset + 4));
		int count = readInt32(data, textOffset + 8);
		int stringOffset = readInt32(data, textOffset + 12) - 8;
		int options = readInt32(data, textOffset + 16);
		int dxOffset = readInt32(data, textOffset + 36) - 8;

		int[] rect = null;
		if ((options & (Gdi.ETO_CLIPPED | Gdi.ETO_OPAQUE)) != 0) {
			rect = transformRect(transform, data, textOffset + 20);
		}

		byte[] text;
		int[] dx = null;
		if (unicode) {
			text = readUtf16StringBytes(data, stringOffset, count, charset);
			if (dxOffset >= 0 && dxOffset + count * 4 <= data.length) {
				dx = readUtf16Dx(data, stringOffset, count, charset, dxOffset);
			}
		} else {
			text = copyRange(data, stringOffset, count);
			if (dxOffset >= 0 && dxOffset + count * 4 <= data.length) {
				dx = new int[count];
				for (int i = 0; i < dx.length; i++) {
					dx[i] = readInt32(data, dxOffset + i * 4);
				}
			}
		}
		gdi.extTextOut(point.x, point.y, options, rect, text, dx);
	}

	private static int[] transformDestRect(double[] transform, byte[] data, int xOffset, int yOffset, int widthOffset, int heightOffset) {
		int x = readInt32(data, xOffset);
		int y = readInt32(data, yOffset);
		int width = readInt32(data, widthOffset);
		int height = readInt32(data, heightOffset);
		return transformRect(transform, x, y, x + width, y + height);
	}

	private static byte[] readBitmap(byte[] data, int bmiOffsetOffset, int bmiSizeOffset, int bitsOffsetOffset, int bitsSizeOffset) {
		int bmiOffset = readInt32(data, bmiOffsetOffset) - 8;
		int bmiSize = readInt32(data, bmiSizeOffset);
		int bitsOffset = readInt32(data, bitsOffsetOffset) - 8;
		int bitsSize = readInt32(data, bitsSizeOffset);
		if (bmiOffset < 0 || bmiSize <= 0 || bmiOffset + bmiSize > data.length
				|| bitsOffset < 0 || bitsSize <= 0 || bitsOffset + bitsSize > data.length) {
			return null;
		}

		byte[] image = new byte[bmiSize + bitsSize];
		System.arraycopy(data, bmiOffset, image, 0, bmiSize);
		System.arraycopy(data, bitsOffset, image, bmiSize, bitsSize);
		return image;
	}

	private static byte[] readUtf16StringBytes(byte[] data, int offset, int chars, int charset) {
		if (offset < 0 || chars <= 0 || offset + chars * 2 > data.length) {
			return new byte[0];
		}
		try {
			String value = new String(data, offset, chars * 2, "UTF-16LE");
			int zero = value.indexOf('\0');
			if (zero >= 0) {
				value = value.substring(0, zero);
			}
			return value.getBytes(GdiUtils.getCharset(charset));
		} catch (UnsupportedEncodingException e) {
			return new byte[0];
		}
	}

	private static int[] readUtf16Dx(byte[] data, int textOffset, int chars, int charset, int dxOffset) {
		if (textOffset < 0 || chars <= 0 || textOffset + chars * 2 > data.length) {
			return null;
		}
		try {
			String charsetName = GdiUtils.getCharset(charset);
			int length = chars;
			for (int i = 0; i < chars; i++) {
				if (readUInt16(data, textOffset + i * 2) == 0) {
					length = i;
					break;
				}
			}

			int dxLength = 0;
			for (int i = 0; i < length; i++) {
				String ch = new String(data, textOffset + i * 2, 2, "UTF-16LE");
				dxLength += ch.getBytes(charsetName).length;
			}

			int[] dx = new int[dxLength];
			int pos = 0;
			for (int i = 0; i < length; i++) {
				String ch = new String(data, textOffset + i * 2, 2, "UTF-16LE");
				int bytes = ch.getBytes(charsetName).length;
				if (bytes == 0) {
					continue;
				}
				dx[pos++] = readInt32(data, dxOffset + i * 4);
				for (int j = 1; j < bytes; j++) {
					dx[pos++] = 0;
				}
			}
			return dx;
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	private static GdiRegion readRegion(byte[] data, int offset, int count, double[] transform, Gdi gdi) {
		if (offset < 0 || count <= 0 || offset + count > data.length) {
			return null;
		}

		byte[] rgnData = new byte[count];
		System.arraycopy(data, offset, rgnData, 0, count);
		return gdi.extCreateRegion(toXForm(transform), count, rgnData);
	}

	private static byte[] copyRange(byte[] data, int offset, int length) {
		if (offset < 0 || length <= 0 || offset + length > data.length) {
			return new byte[0];
		}
		byte[] copy = new byte[length];
		System.arraycopy(data, offset, copy, 0, length);
		return copy;
	}

	private static Point[] readPoints16(byte[] data, double[] transform) {
		int count = readInt32(data, 16);
		Point[] points = new Point[count];
		int offset = 20;
		for (int i = 0; i < count; i++) {
			points[i] = transformPoint(transform, readInt16(data, offset), readInt16(data, offset + 2));
			offset += 4;
		}
		return points;
	}

	private static Point[] readPoints32(byte[] data, double[] transform) {
		int count = readInt32(data, 16);
		Point[] points = new Point[count];
		int offset = 20;
		for (int i = 0; i < count; i++) {
			points[i] = transformPoint(transform, readInt32(data, offset), readInt32(data, offset + 4));
			offset += 8;
		}
		return points;
	}

	private static Point[][] readPolyPoints16(byte[] data, double[] transform) {
		int polygons = readInt32(data, 16);
		int[] counts = new int[polygons];
		int offset = 24;
		for (int i = 0; i < counts.length; i++) {
			counts[i] = readInt32(data, offset);
			offset += 4;
		}

		Point[][] points = new Point[polygons][];
		for (int i = 0; i < points.length; i++) {
			points[i] = new Point[counts[i]];
			for (int j = 0; j < points[i].length; j++) {
				points[i][j] = transformPoint(transform, readInt16(data, offset), readInt16(data, offset + 2));
				offset += 4;
			}
		}
		return points;
	}

	private static Point[][] readPolyPoints32(byte[] data, double[] transform) {
		int polygons = readInt32(data, 16);
		int[] counts = new int[polygons];
		int offset = 24;
		for (int i = 0; i < counts.length; i++) {
			counts[i] = readInt32(data, offset);
			offset += 4;
		}

		Point[][] points = new Point[polygons][];
		for (int i = 0; i < points.length; i++) {
			points[i] = new Point[counts[i]];
			for (int j = 0; j < points[i].length; j++) {
				points[i][j] = transformPoint(transform, readInt32(data, offset), readInt32(data, offset + 4));
				offset += 8;
			}
		}
		return points;
	}

	private static Point transformPoint(double[] transform, int x, int y) {
		double tx = transform[0] * x + transform[2] * y + transform[4];
		double ty = transform[1] * x + transform[3] * y + transform[5];
		return new Point((int)Math.round(tx), (int)Math.round(ty));
	}

	private static int[] transformRect(double[] transform, byte[] data, int offset) {
		return transformRect(transform, readInt32(data, offset), readInt32(data, offset + 4),
				readInt32(data, offset + 8), readInt32(data, offset + 12));
	}

	private static int[] transformRect(double[] transform, int leftValue, int topValue, int rightValue, int bottomValue) {
		Point p1 = transformPoint(transform, leftValue, topValue);
		Point p2 = transformPoint(transform, rightValue, topValue);
		Point p3 = transformPoint(transform, rightValue, bottomValue);
		Point p4 = transformPoint(transform, leftValue, bottomValue);

		int left = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
		int top = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
		int right = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x));
		int bottom = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y));
		return new int[] { left, top, right, bottom };
	}

	private static double[] readTransform(byte[] data, int offset) {
		return new double[] {
			readFloat32(data, offset),
			readFloat32(data, offset + 4),
			readFloat32(data, offset + 8),
			readFloat32(data, offset + 12),
			readFloat32(data, offset + 16),
			readFloat32(data, offset + 20)
		};
	}

	private static float[] toXForm(double[] transform) {
		return new float[] {
			(float)transform[0],
			(float)transform[1],
			(float)transform[2],
			(float)transform[3],
			(float)transform[4],
			(float)transform[5]
		};
	}

	private static double[] identity() {
		return new double[] {1.0, 0.0, 0.0, 1.0, 0.0, 0.0};
	}

	private static double[] copy(double[] transform) {
		double[] copy = new double[transform.length];
		System.arraycopy(transform, 0, copy, 0, transform.length);
		return copy;
	}

	private static double[] modifyWorldTransform(double[] current, double[] next, int mode) {
		switch (mode) {
		case MWT_IDENTITY:
			return identity();
		case MWT_LEFTMULTIPLY:
			return multiply(next, current);
		case MWT_RIGHTMULTIPLY:
			return multiply(current, next);
		default:
			return current;
		}
	}

	private static double[] multiply(double[] left, double[] right) {
		return new double[] {
			left[0] * right[0] + left[2] * right[1],
			left[1] * right[0] + left[3] * right[1],
			left[0] * right[2] + left[2] * right[3],
			left[1] * right[2] + left[3] * right[3],
			left[0] * right[4] + left[2] * right[5] + left[4],
			left[1] * right[4] + left[3] * right[5] + left[5]
		};
	}

	private static int readInt16(byte[] data, int offset) {
		return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8));
	}

	private static int readUInt16(byte[] data, int offset) {
		return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
	}

	private static int readUInt8(byte[] data, int offset) {
		return data[offset] & 0xFF;
	}

	private static int readInt32(byte[] data, int offset) {
		return (data[offset] & 0xFF)
				| ((data[offset + 1] & 0xFF) << 8)
				| ((data[offset + 2] & 0xFF) << 16)
				| ((data[offset + 3] & 0xFF) << 24);
	}

	private static long readUInt32(byte[] data, int offset) {
		return readInt32(data, offset) & 0xFFFFFFFFL;
	}

	private static float readFloat32(byte[] data, int offset) {
		return Float.intBitsToFloat(readInt32(data, offset));
	}
}

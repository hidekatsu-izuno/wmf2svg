/*
 * Copyright 2007-2015 Hidekatsu Izuno
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
package net.arnx.wmf2svg.gdi.wmf;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.logging.Logger;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiObject;
import net.arnx.wmf2svg.gdi.GdiPalette;
import net.arnx.wmf2svg.gdi.GdiRegion;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.emf.EmfParser;
import net.arnx.wmf2svg.io.DataInput;
import net.arnx.wmf2svg.io.Parser;

/**
 * @author Hidekatsu Izuno
 * @author Shunsuke Mori
 */
public class WmfParser implements Parser, WmfConstants {

	private static Logger log = Logger.getLogger(WmfParser.class.getName());

	public WmfParser() {
	}

	private boolean isObjectIndex(int index, GdiObject[] objects) {
		return index >= 0 && index < objects.length;
	}

	public void parse(InputStream is, Gdi gdi) throws IOException, WmfParseException {
		DataInput in = null;
		boolean isEmpty = true;

		try {
			in = new DataInput(new BufferedInputStream(is), ByteOrder.LITTLE_ENDIAN);

			int mtType = 0;
			int mtHeaderSize = 0;

			long key = in.readUint32();
			isEmpty = false;
			if (key == 0x9AC6CDD7) {
				in.readInt16(); // hmf
				int vsx = in.readInt16();
				int vsy = in.readInt16();
				int vex = in.readInt16();
				int vey = in.readInt16();
				int dpi = in.readUint16();
				in.readUint32(); // reserved
				in.readUint16(); // checksum

				gdi.placeableHeader(vsx, vsy, vex, vey, dpi);

				mtType = in.readUint16();
				mtHeaderSize = in.readUint16();
			} else {
				mtType = (int) (key & 0x0000FFFF);
				mtHeaderSize = (int) ((key & 0xFFFF0000) >> 16);
			}

			in.readUint16(); // mtVersion
			in.readUint32(); // mtSize
			int mtNoObjects = in.readUint16();
			in.readUint32(); // mtMaxRecord
			in.readUint16(); // mtNoParameters

			if (mtType != 1 || mtHeaderSize != 9) {
				throw new WmfParseException("invalid file format.");
			}

			gdi.header();

			GdiObject[] objs = new GdiObject[mtNoObjects];
			boolean enhancedMetafileComment = false;

			while (true) {
				int size = (int) in.readUint32() - 3;
				int id = in.readUint16();

				if (id == META_EOF) {
					break; // Last record
				}

				in.setCount(0);

				if (enhancedMetafileComment && id != META_ESCAPE && !isEnhancedMetafileStateRecord(id)) {
					in.readBytes(size * 2);
					continue;
				}

				switch (id) {
					case META_REALIZEPALETTE : {
						gdi.realizePalette();
						break;
					}
					case META_SETPALENTRIES : {
						int[] entries = new int[in.readUint16()];
						int startIndex = in.readUint16();
						int objID = in.readUint16();
						for (int i = 0; i < entries.length; i++) {
							entries[i] = in.readInt32();
						}
						gdi.setPaletteEntries((GdiPalette) objs[objID], startIndex, entries);
						break;
					}
					case META_SETBKMODE : {
						int mode = in.readInt16();
						gdi.setBkMode(mode);
						break;
					}
					case META_SETMAPMODE : {
						int mode = in.readInt16();
						gdi.setMapMode(mode);
						break;
					}
					case META_SETROP2 : {
						int mode = in.readInt16();
						gdi.setROP2(mode);
						break;
					}
					case META_SETRELABS : {
						int mode = in.readInt16();
						gdi.setRelAbs(mode);
						break;
					}
					case META_SETPOLYFILLMODE : {
						int mode = in.readInt16();
						gdi.setPolyFillMode(mode);
						break;
					}
					case META_SETSTRETCHBLTMODE : {
						int mode = in.readInt16();
						gdi.setStretchBltMode(mode);
						break;
					}
					case META_SETTEXTCHAREXTRA : {
						int extra = in.readInt16();
						gdi.setTextCharacterExtra(extra);
						break;
					}
					case META_RESTOREDC : {
						int dc = in.readInt16();
						gdi.restoreDC(dc);
						break;
					}
					case META_RESIZEPALETTE : {
						int entries = in.readUint16();
						gdi.resizePalette(null, entries);
						break;
					}
					case META_DIBCREATEPATTERNBRUSH : {
						int usage = in.readInt32();
						byte[] image = in.readBytes(size * 2 - in.getCount());

						for (int i = 0; i < objs.length; i++) {
							if (objs[i] == null) {
								objs[i] = gdi.dibCreatePatternBrush(image, usage);
								break;
							}
						}
						break;
					}
					case META_SETLAYOUT : {
						long layout = in.readUint32();
						gdi.setLayout(layout);
						break;
					}
					case META_SETBKCOLOR : {
						int color = in.readInt32();
						gdi.setBkColor(color);
						break;
					}
					case META_SETTEXTCOLOR : {
						int color = in.readInt32();
						gdi.setTextColor(color);
						break;
					}
					case META_OFFSETVIEWPORTORG : {
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.offsetViewportOrgEx(x, y, null);
						break;
					}
					case META_LINETO : {
						int ey = in.readInt16();
						int ex = in.readInt16();
						gdi.lineTo(ex, ey);
						break;
					}
					case META_MOVETO : {
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.moveToEx(x, y, null);
						break;
					}
					case META_OFFSETCLIPRGN : {
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.offsetClipRgn(x, y);
						break;
					}
					case META_FILLREGION : {
						int rgnID = in.readUint16();
						int brushID = in.readUint16();
						if (isObjectIndex(rgnID, objs) && isObjectIndex(brushID, objs)
								&& objs[rgnID] instanceof GdiRegion && objs[brushID] instanceof GdiBrush) {
							gdi.fillRgn((GdiRegion) objs[rgnID], (GdiBrush) objs[brushID]);
						}
						break;
					}
					case META_SETMAPPERFLAGS : {
						long flag = in.readUint32();
						gdi.setMapperFlags(flag);
						break;
					}
					case META_SELECTPALETTE : {
						boolean mode = (in.readInt16() != 0);
						if ((size * 2 - in.getCount()) > 0) {
							int objID = in.readUint16();
							gdi.selectPalette((GdiPalette) objs[objID], mode);
						}
						break;
					}
					case META_POLYGON : {
						Point[] points = new Point[in.readInt16()];
						for (int i = 0; i < points.length; i++) {
							points[i] = new Point(in.readInt16(), in.readInt16());
						}
						gdi.polygon(points);
						break;
					}
					case META_POLYLINE : {
						Point[] points = new Point[in.readInt16()];
						for (int i = 0; i < points.length; i++) {
							points[i] = new Point(in.readInt16(), in.readInt16());
						}
						gdi.polyline(points);
						break;
					}
					case META_SETTEXTJUSTIFICATION : {
						int breakCount = in.readInt16();
						int breakExtra = in.readInt16();
						gdi.setTextJustification(breakExtra, breakCount);
						break;
					}
					case META_SETWINDOWORG : {
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.setWindowOrgEx(x, y, null);
						break;
					}
					case META_SETWINDOWEXT : {
						int height = in.readInt16();
						int width = in.readInt16();
						gdi.setWindowExtEx(width, height, null);
						break;
					}
					case META_SETVIEWPORTORG : {
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.setViewportOrgEx(x, y, null);
						break;
					}
					case META_SETVIEWPORTEXT : {
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.setViewportExtEx(x, y, null);
						break;
					}
					case META_OFFSETWINDOWORG : {
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.offsetWindowOrgEx(x, y, null);
						break;
					}
					case META_SCALEWINDOWEXT : {
						int yd = in.readInt16();
						int y = in.readInt16();
						int xd = in.readInt16();
						int x = in.readInt16();
						gdi.scaleWindowExtEx(x, xd, y, yd, null);
						break;
					}
					case META_SCALEVIEWPORTEXT : {
						int yd = in.readInt16();
						int y = in.readInt16();
						int xd = in.readInt16();
						int x = in.readInt16();
						gdi.scaleViewportExtEx(x, xd, y, yd, null);
						break;
					}
					case META_EXCLUDECLIPRECT : {
						int ey = in.readInt16();
						int ex = in.readInt16();
						int sy = in.readInt16();
						int sx = in.readInt16();
						gdi.excludeClipRect(sx, sy, ex, ey);
						break;
					}
					case META_INTERSECTCLIPRECT : {
						int ey = in.readInt16();
						int ex = in.readInt16();
						int sy = in.readInt16();
						int sx = in.readInt16();
						gdi.intersectClipRect(sx, sy, ex, ey);
						break;
					}
					case META_ELLIPSE : {
						int ey = in.readInt16();
						int ex = in.readInt16();
						int sy = in.readInt16();
						int sx = in.readInt16();
						gdi.ellipse(sx, sy, ex, ey);
						break;
					}
					case META_FLOODFILL : {
						int color = in.readInt32();
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.floodFill(x, y, color);
						break;
					}
					case META_FRAMEREGION : {
						int rgnID = in.readUint16();
						int brushID = in.readUint16();
						int height = in.readInt16();
						int width = in.readInt16();
						if (isObjectIndex(rgnID, objs) && isObjectIndex(brushID, objs)
								&& objs[rgnID] instanceof GdiRegion && objs[brushID] instanceof GdiBrush) {
							gdi.frameRgn((GdiRegion) objs[rgnID], (GdiBrush) objs[brushID], width, height);
						}
						break;
					}
					case META_ANIMATEPALETTE : {
						int[] entries = new int[in.readUint16()];
						int startIndex = in.readUint16();
						int objID = in.readUint16();
						for (int i = 0; i < entries.length; i++) {
							entries[i] = in.readInt32();
						}
						gdi.animatePalette((GdiPalette) objs[objID], startIndex, entries);
						break;
					}
					case META_TEXTOUT : {
						int count = in.readInt16();
						byte[] text = in.readBytes(count);
						if (count % 2 == 1) {
							in.readByte();
						}
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.textOut(x, y, text);
						break;
					}
					case META_POLYPOLYGON : {
						Point[][] points = new Point[in.readInt16()][];
						for (int i = 0; i < points.length; i++) {
							points[i] = new Point[in.readInt16()];
						}
						for (int i = 0; i < points.length; i++) {
							for (int j = 0; j < points[i].length; j++) {
								points[i][j] = new Point(in.readInt16(), in.readInt16());
							}
						}
						gdi.polyPolygon(points);
						break;
					}
					case META_EXTFLOODFILL : {
						int type = in.readUint16();
						int color = in.readInt32();
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.extFloodFill(x, y, color, type);
						break;
					}
					case META_RECTANGLE : {
						int ey = in.readInt16();
						int ex = in.readInt16();
						int sy = in.readInt16();
						int sx = in.readInt16();
						gdi.rectangle(sx, sy, ex, ey);
						break;
					}
					case META_SETPIXEL : {
						int color = in.readInt32();
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.setPixel(x, y, color);
						break;
					}
					case META_ROUNDRECT : {
						int rh = in.readInt16();
						int rw = in.readInt16();
						int ey = in.readInt16();
						int ex = in.readInt16();
						int sy = in.readInt16();
						int sx = in.readInt16();
						gdi.roundRect(sx, sy, ex, ey, rw, rh);
						break;
					}
					case META_PATBLT : {
						long rop = in.readUint32();
						int height = in.readInt16();
						int width = in.readInt16();
						int y = in.readInt16();
						int x = in.readInt16();
						gdi.patBlt(x, y, width, height, rop);
						break;
					}
					case META_SAVEDC : {
						gdi.seveDC();
						break;
					}
					case META_PIE : {
						int eyr = in.readInt16();
						int exr = in.readInt16();
						int syr = in.readInt16();
						int sxr = in.readInt16();
						int ey = in.readInt16();
						int ex = in.readInt16();
						int sy = in.readInt16();
						int sx = in.readInt16();
						gdi.pie(sx, sy, ex, ey, sxr, syr, exr, eyr);
						break;
					}
					case META_STRETCHBLT : {
						long rop = in.readUint32();
						int sh = in.readInt16();
						int sw = in.readInt16();
						int sy = in.readInt16();
						int sx = in.readInt16();
						int dh = in.readInt16();
						int dw = in.readInt16();
						int dy = in.readInt16();
						int dx = in.readInt16();

						byte[] image = in.readBytes(size * 2 - in.getCount());

						gdi.stretchBlt(image, dx, dy, dw, dh, sx, sy, sw, sh, rop);
						break;
					}
					case META_ESCAPE : {
						byte[] data = in.readBytes(2 * size);
						if (!EmfParser.parseEscape(data, gdi)) {
							gdi.escape(data);
						} else {
							enhancedMetafileComment = true;
						}
						break;
					}
					case META_INVERTREGION : {
						int rgnID = in.readUint16();
						gdi.invertRgn((GdiRegion) objs[rgnID]);
						break;
					}
					case META_PAINTREGION : {
						int objID = in.readUint16();
						gdi.paintRgn((GdiRegion) objs[objID]);
						break;
					}
					case META_SELECTCLIPREGION : {
						int objID = in.readUint16();
						if (objID == 0 || objID == 0xFFFF) {
							gdi.selectClipRgn(null);
						} else if (isObjectIndex(objID, objs) && objs[objID] instanceof GdiRegion) {
							gdi.selectClipRgn((GdiRegion) objs[objID]);
						}
						break;
					}
					case META_SELECTOBJECT : {
						int objID = in.readUint16();
						gdi.selectObject(objs[objID]);
						break;
					}
					case META_SETTEXTALIGN : {
						int align = in.readInt16();
						gdi.setTextAlign(align);
						break;
					}
					case META_ARC : {
						int eya = in.readInt16();
						int exa = in.readInt16();
						int sya = in.readInt16();
						int sxa = in.readInt16();
						int eyr = in.readInt16();
						int exr = in.readInt16();
						int syr = in.readInt16();
						int sxr = in.readInt16();
						gdi.arc(sxr, syr, exr, eyr, sxa, sya, exa, eya);
						break;
					}
					case META_CHORD : {
						int eya = in.readInt16();
						int exa = in.readInt16();
						int sya = in.readInt16();
						int sxa = in.readInt16();
						int eyr = in.readInt16();
						int exr = in.readInt16();
						int syr = in.readInt16();
						int sxr = in.readInt16();
						gdi.chord(sxr, syr, exr, eyr, sxa, sya, exa, eya);
						break;
					}
					case META_BITBLT : {
						long rop = in.readUint32();
						int sy = in.readInt16();
						int sx = in.readInt16();
						int height = in.readInt16();
						int width = in.readInt16();
						int dy = in.readInt16();
						int dx = in.readInt16();

						byte[] image = in.readBytes(size * 2 - in.getCount());

						gdi.bitBlt(image, dx, dy, width, height, sx, sy, rop);
						break;
					}
					case META_EXTTEXTOUT : {
						int rsize = size;

						int y = in.readInt16();
						int x = in.readInt16();
						int count = in.readInt16();
						int options = in.readUint16();
						rsize -= 4;

						int[] rect = null;
						if ((options & 0x0006) > 0) {
							rect = new int[]{in.readInt16(), in.readInt16(), in.readInt16(), in.readInt16()};
							rsize -= 4;
						}
						byte[] text = in.readBytes(count);
						if (count % 2 == 1) {
							in.readByte();
						}
						rsize -= (count + 1) / 2;

						int[] dx = null;
						if (rsize > 0) {
							dx = new int[rsize];
							for (int i = 0; i < dx.length; i++) {
								dx[i] = in.readInt16();
							}
						}
						gdi.extTextOut(x, y, options, rect, text, dx);
						break;
					}
					case META_SETDIBTODEV : {
						int colorUse = in.readUint16();
						int scanlines = in.readUint16();
						int startscan = in.readUint16();
						int sy = in.readInt16();
						int sx = in.readInt16();
						int dh = in.readInt16();
						int dw = in.readInt16();
						int dy = in.readInt16();
						int dx = in.readInt16();

						byte[] image = in.readBytes(size * 2 - in.getCount());

						gdi.setDIBitsToDevice(dx, dy, dw, dh, sx, sy, startscan, scanlines, image, colorUse);
						break;
					}
					case META_DIBBITBLT : {
						boolean isRop = false;

						long rop = in.readUint32();
						int sy = in.readInt16();
						int sx = in.readInt16();
						int height = in.readInt16();
						if (height == 0) {
							height = in.readInt16();
							isRop = true;
						}
						int width = in.readInt16();
						int dy = in.readInt16();
						int dx = in.readInt16();

						if (isRop) {
							gdi.dibBitBlt(null, dx, dy, width, height, sx, sy, rop);
						} else {
							byte[] image = in.readBytes(size * 2 - in.getCount());

							gdi.dibBitBlt(image, dx, dy, width, height, sx, sy, rop);
						}
						break;
					}
					case META_DIBSTRETCHBLT : {
						long rop = in.readUint32();
						int sh = in.readInt16();
						int sw = in.readInt16();
						int sx = in.readInt16();
						int sy = in.readInt16();
						int dh = in.readInt16();
						int dw = in.readInt16();
						int dy = in.readInt16();
						int dx = in.readInt16();

						byte[] image = in.readBytes(size * 2 - in.getCount());

						gdi.dibStretchBlt(image, dx, dy, dw, dh, sx, sy, sw, sh, rop);
						break;
					}
					case META_STRETCHDIB : {
						long rop = in.readUint32();
						int usage = in.readUint16();
						int sh = in.readInt16();
						int sw = in.readInt16();
						int sy = in.readInt16();
						int sx = in.readInt16();
						int dh = in.readInt16();
						int dw = in.readInt16();
						int dy = in.readInt16();
						int dx = in.readInt16();

						byte[] image = in.readBytes(size * 2 - in.getCount());

						gdi.stretchDIBits(dx, dy, dw, dh, sx, sy, sw, sh, image, usage, rop);
						break;
					}
					case META_DELETEOBJECT : {
						int objID = in.readUint16();
						gdi.deleteObject(objs[objID]);
						objs[objID] = null;
						break;
					}
					case META_CREATEPALETTE : {
						int version = in.readUint16();
						int[] entries = new int[in.readUint16()];
						for (int i = 0; i < entries.length; i++) {
							entries[i] = in.readInt32();
						}

						for (int i = 0; i < objs.length; i++) {
							if (objs[i] == null) {
								objs[i] = gdi.createPalette(version, entries);
								break;
							}
						}
						break;
					}
					case META_CREATEPATTERNBRUSH : {
						byte[] image = in.readBytes(size * 2 - in.getCount());

						for (int i = 0; i < objs.length; i++) {
							if (objs[i] == null) {
								objs[i] = gdi.createPatternBrush(image);
								break;
							}
						}
						break;
					}
					case META_CREATEPENINDIRECT : {
						int style = in.readUint16();
						int width = in.readInt16();
						in.readInt16();
						int color = in.readInt32();
						for (int i = 0; i < objs.length; i++) {
							if (objs[i] == null) {
								objs[i] = gdi.createPenIndirect(style, width, color);
								break;
							}
						}
						break;
					}
					case META_CREATEFONTINDIRECT : {
						int height = in.readInt16();
						int width = in.readInt16();
						int escapement = in.readInt16();
						int orientation = in.readInt16();
						int weight = in.readInt16();
						boolean italic = (in.readByte() == 1);
						boolean underline = (in.readByte() == 1);
						boolean strikeout = (in.readByte() == 1);
						int charset = in.readByte();
						int outPrecision = in.readByte();
						int clipPrecision = in.readByte();
						int quality = in.readByte();
						int pitchAndFamily = in.readByte();
						byte[] faceName = in.readBytes(size * 2 - in.getCount());

						GdiObject obj = gdi.createFontIndirect(height, width, escapement, orientation, weight, italic,
								underline, strikeout, charset, outPrecision, clipPrecision, quality, pitchAndFamily,
								faceName);

						for (int i = 0; i < objs.length; i++) {
							if (objs[i] == null) {
								objs[i] = obj;
								break;
							}
						}
						break;
					}
					case META_CREATEBRUSHINDIRECT : {
						int style = in.readUint16();
						int color = in.readInt32();
						int hatch = in.readUint16();
						for (int i = 0; i < objs.length; i++) {
							if (objs[i] == null) {
								objs[i] = gdi.createBrushIndirect(style, color, hatch);
								break;
							}
						}
						break;
					}
					case META_CREATEREGION : {
						if (size >= 11) {
							in.readInt16();
							int objectType = in.readInt16();
							in.readInt32();
							in.readInt16();
							in.readInt16();
							in.readInt16();
							int left = in.readInt16();
							int top = in.readInt16();
							int right = in.readInt16();
							int bottom = in.readInt16();
							if (objectType == 0x0006) {
								for (int i = 0; i < objs.length; i++) {
									if (objs[i] == null) {
										objs[i] = gdi.createRectRgn(left, top, right, bottom);
										break;
									}
								}
							}
						}
						break;
					}
					default : {
						log.fine("unsuppored id find: " + id + " (size=" + size + ")");
					}
				}

				int rest = size * 2 - in.getCount();
				for (int i = 0; i < rest; i++) {
					in.readByte();
				}
			}
			in.close();

			gdi.footer();
		} catch (EOFException e) {
			if (isEmpty)
				throw new WmfParseException("input file size is zero.");
		}
	}

	private boolean isEnhancedMetafileStateRecord(int id) {
		switch (id) {
			case META_SETMAPMODE :
			case META_SETWINDOWORG :
			case META_SETWINDOWEXT :
			case META_SETVIEWPORTORG :
			case META_SETVIEWPORTEXT :
			case META_OFFSETWINDOWORG :
			case META_OFFSETVIEWPORTORG :
			case META_SCALEWINDOWEXT :
			case META_SCALEVIEWPORTEXT :
				return true;
			default :
				return false;
		}
	}
}

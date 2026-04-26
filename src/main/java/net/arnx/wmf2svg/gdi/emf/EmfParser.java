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
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiObject;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.io.DataInput;

/**
 * Minimal EMF parser scaffold focused on overlay geometry stored in
 * META_ESCAPE_ENHANCED_METAFILE comments.
 */
public class EmfParser {
	private static final int EMR_HEADER = 1;
	private static final int EMR_SETMAPMODE = 17;
	private static final int EMR_SETBKMODE = 18;
	private static final int EMR_SETPOLYFILLMODE = 19;
	private static final int EMR_SETTEXTALIGN = 22;
	private static final int EMR_SETTEXTCOLOR = 24;
	private static final int EMR_SETBKCOLOR = 25;
	private static final int EMR_SAVEDC = 33;
	private static final int EMR_RESTOREDC = 34;
	private static final int EMR_MODIFYWORLDTRANSFORM = 36;
	private static final int EMR_SELECTOBJECT = 37;
	private static final int EMR_CREATEPEN = 38;
	private static final int EMR_CREATEBRUSHINDIRECT = 39;
	private static final int EMR_DELETEOBJECT = 40;
	private static final int EMR_LINETO = 54;
	private static final int EMR_BEGINPATH = 59;
	private static final int EMR_ENDPATH = 60;
	private static final int EMR_CLOSEFIGURE = 61;
	private static final int EMR_FILLPATH = 62;
	private static final int EMR_STROKEANDFILLPATH = 63;
	private static final int EMR_STROKEPATH = 64;
	private static final int EMR_SELECTCLIPPATH = 67;
	private static final int EMR_GDICOMMENT = 70;
	private static final int EMR_POLYBEZIER16 = 85;
	private static final int EMR_POLYGON16 = 86;
	private static final int EMR_POLYLINE16 = 87;
	private static final int EMR_EXTCREATEPEN = 95;
	private static final int EMR_EOF = 14;

	private static final int META_ESCAPE_ENHANCED_METAFILE = 0x000F;
	private static final long WMF_COMMENT_IDENTIFIER = 0x43464D57L;
	private static final long ENHANCED_METAFILE_COMMENT = 0x00000001L;

	private static final int MWT_IDENTITY = 1;
	private static final int MWT_LEFTMULTIPLY = 2;
	private static final int MWT_RIGHTMULTIPLY = 3;

	private static final int STOCK_WHITE_BRUSH = 0x80000000;
	private static final int STOCK_LTGRAY_BRUSH = 0x80000001;
	private static final int STOCK_GRAY_BRUSH = 0x80000002;
	private static final int STOCK_DKGRAY_BRUSH = 0x80000003;
	private static final int STOCK_BLACK_BRUSH = 0x80000004;
	private static final int STOCK_NULL_BRUSH = 0x80000005;
	private static final int STOCK_WHITE_PEN = 0x80000006;
	private static final int STOCK_BLACK_PEN = 0x80000007;
	private static final int STOCK_NULL_PEN = 0x80000008;

	public EmfParser() {
	}

	public void parse(InputStream is, Gdi gdi) throws IOException {
		DataInput in = new DataInput(new BufferedInputStream(is), ByteOrder.LITTLE_ENDIAN);
		Map<Integer, GdiObject> objects = new HashMap<Integer, GdiObject>();
		Map<Integer, GdiObject> stockObjects = new HashMap<Integer, GdiObject>();
		LinkedList<double[]> transforms = new LinkedList<double[]>();
		double[] transform = identity();
		List<Point[]> path = null;

		while (true) {
			int type = (int)in.readUint32();
			int size = (int)in.readUint32();
			if (size < 8) {
				throw new IOException("Invalid EMF record size: " + size);
			}

			byte[] data = in.readBytes(size - 8);
			switch (type) {
			case EMR_HEADER:
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
			case EMR_SETTEXTALIGN:
				gdi.setTextAlign(readInt32(data, 0));
				break;
			case EMR_SETTEXTCOLOR:
				gdi.setTextColor(readInt32(data, 0));
				break;
			case EMR_SETBKCOLOR:
				gdi.setBkColor(readInt32(data, 0));
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
			case EMR_MODIFYWORLDTRANSFORM:
				transform = modifyWorldTransform(transform, readTransform(data, 0), readInt32(data, 24));
				break;
			case EMR_SELECTOBJECT:
				selectObject(gdi, objects, stockObjects, readInt32(data, 0));
				break;
			case EMR_CREATEPEN:
				objects.put(readInt32(data, 0), gdi.createPenIndirect(readInt32(data, 4) & 0xFF, Math.max(1, Math.abs(readInt32(data, 8))), readInt32(data, 16)));
				break;
			case EMR_CREATEBRUSHINDIRECT:
				objects.put(readInt32(data, 0), gdi.createBrushIndirect(readInt32(data, 4), readInt32(data, 8), readInt32(data, 12)));
				break;
			case EMR_DELETEOBJECT: {
				GdiObject obj = objects.remove(readInt32(data, 0));
				if (obj != null) {
					gdi.deleteObject(obj);
				}
				break;
			}
			case EMR_LINETO: {
				Point point = transformPoint(transform, readInt32(data, 0), readInt32(data, 4));
				gdi.lineTo(point.x, point.y);
				break;
			}
			case EMR_POLYGON16:
				if (path != null) {
					path.add(readPoints16(data, transform));
				} else {
					gdi.polygon(readPoints16(data, transform));
				}
				break;
			case EMR_POLYLINE16:
				if (path != null) {
					path.add(readPoints16(data, transform));
				} else {
					gdi.polyline(readPoints16(data, transform));
				}
				break;
			case EMR_POLYBEZIER16:
				if (path != null) {
					path.add(readPoints16(data, transform));
				} else {
					gdi.polyline(readPoints16(data, transform));
				}
				break;
			case EMR_EXTCREATEPEN:
				objects.put(readInt32(data, 0), gdi.createPenIndirect(readInt32(data, 20) & 0xFF, Math.max(1, Math.abs(readInt32(data, 24))), readInt32(data, 32)));
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
			case EMR_BEGINPATH:
				path = new LinkedList<Point[]>();
				break;
			case EMR_ENDPATH:
				break;
			case EMR_CLOSEFIGURE:
			case EMR_FILLPATH:
			case EMR_STROKEANDFILLPATH:
			case EMR_STROKEPATH:
				path = null;
				break;
			case EMR_SELECTCLIPPATH:
				if (path != null && !path.isEmpty()) {
					gdi.selectClipPath(path.toArray(new Point[path.size()][]));
				}
				path = null;
				break;
			case EMR_EOF:
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

	private void selectObject(Gdi gdi, Map<Integer, GdiObject> objects, Map<Integer, GdiObject> stockObjects, int id) {
		GdiObject obj = objects.get(id);
		if (obj == null) {
			obj = stockObjects.get(id);
			if (obj == null) {
				obj = createStockObject(gdi, id);
				if (obj != null) {
					stockObjects.put(id, obj);
				}
			}
		}
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

	private static Point transformPoint(double[] transform, int x, int y) {
		double tx = transform[0] * x + transform[2] * y + transform[4];
		double ty = transform[1] * x + transform[3] * y + transform[5];
		return new Point((int)Math.round(tx), (int)Math.round(ty));
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

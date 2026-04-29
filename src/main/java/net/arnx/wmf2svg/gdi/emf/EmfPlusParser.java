package net.arnx.wmf2svg.gdi.emf;

public class EmfPlusParser implements EmfPlusConstants {
	public interface Handler {
		void handleEmfPlusRecord(int type, int flags, byte[] payload,
				boolean continuableObject, int totalObjectSize);
	}

	public static boolean isEmfPlusComment(byte[] data) {
		return data.length >= 4
				&& data[0] == 'E'
				&& data[1] == 'M'
				&& data[2] == 'F'
				&& data[3] == '+';
	}

	public void parse(byte[] data, Handler handler) {
		int offset = 4;
		while (offset + EMF_PLUS_HEADER_SIZE <= data.length) {
			int type = readUInt16(data, offset);
			int flags = readUInt16(data, offset + 2);
			int size = readInt32(data, offset + 4);
			boolean continuableObject = type == EMF_PLUS_OBJECT
					&& (flags & EMF_PLUS_OBJECT_CONTINUABLE) != 0;
			int headerSize = continuableObject ? EMF_PLUS_HEADER_SIZE + 4 : EMF_PLUS_HEADER_SIZE;
			if (offset + headerSize > data.length) {
				break;
			}
			int totalObjectSize = continuableObject ? readInt32(data, offset + 8) : -1;
			int dataSize = readInt32(data, offset + headerSize - 4);
			if (size < headerSize || dataSize < 0 || dataSize > size - headerSize
					|| offset + size > data.length) {
				break;
			}

			byte[] payload = new byte[dataSize];
			System.arraycopy(data, offset + headerSize, payload, 0, dataSize);
			handler.handleEmfPlusRecord(type, flags, payload, continuableObject, totalObjectSize);
			offset += size;
		}
	}

	private static int readUInt16(byte[] data, int offset) {
		return ((data[offset] & 0xFF)
				| ((data[offset + 1] & 0xFF) << 8));
	}

	private static int readInt32(byte[] data, int offset) {
		return ((data[offset] & 0xFF)
				| ((data[offset + 1] & 0xFF) << 8)
				| ((data[offset + 2] & 0xFF) << 16)
				| ((data[offset + 3] & 0xFF) << 24));
	}
}

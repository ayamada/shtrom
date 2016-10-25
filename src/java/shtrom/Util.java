package shtrom;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Util {
	public static ByteBuffer genByteBuffer (int size) {
		return ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
	}

	public static byte[] httpBodyToBytes (InputStream input, int len) throws IOException {
		ByteBuffer bb = genByteBuffer(len);
		int bufSize = 4096;
		byte[] buf = new byte[bufSize];
		while (true) {
			int l = input.read(buf, 0, bufSize);
			if (l < 0) { break; }
			bb.put(buf, 0, l);
		}
		return bb.array();
	}

	public static int[] byteArrayToData (byte[] src, int len) {
		ByteBuffer bb = genByteBuffer(len);
		bb.put(src, 0, len);
		bb.position(0);
		int resultLength = len / 4;
		int[] result = new int[resultLength];
		for (int i = 0; i < resultLength; i++) {
			result[i] = bb.getInt();
		}
		return result;
	}

	private static void readBytes (DataInputStream rdr, byte[] buf, int offset, int l) throws EOFException, IOException {
		int totalRead = 0;
		while (totalRead < l) {
			int n = rdr.read(buf, offset + totalRead, l - totalRead);
			if (n < 0) { throw new EOFException("Premature EOF"); }
			totalRead += n;
		}
	}

	private static void readByteBuffer (DataInputStream rdr, ByteBuffer bb, int l) throws EOFException, IOException {
		int cap = bb.capacity();
		readBytes(rdr, bb.array(), 0, l);
		bb.limit(cap);
		bb.position(l);
	}

	private static int breadInteger (DataInputStream rdr) throws EOFException, IOException {
		ByteBuffer bb = genByteBuffer(8);
		readByteBuffer(rdr, bb, 4);
		bb.flip();
		return bb.getInt();
	}

	public static int[] bistRead (File f) throws IOException {
		int len = (int)(f.length() / 4);
		int[] result = new int[len];
		DataInputStream rdr = new DataInputStream(new FileInputStream(f));
		for (int i = 0; i < len; i++) {
			result[i] = breadInteger(rdr);
		}
		rdr.close();
		return result;
	}

	public static int[] bistReadWithRange (File f, int left, int right) throws IOException {
		int len = (int)(f.length() / 4);
		if (right <= left) { return new int[0]; }
		int[] result = new int[right - left];
		DataInputStream rdr = new DataInputStream(new FileInputStream(f));
		rdr.skipBytes(left * 4);
		for (int i = 0; i < len; i++) {
			result[i] = breadInteger(rdr);
		}
		rdr.close();
		return result;
	}

	/* TODO:
	 * - bist-write
	 * - values->content
	 */

  public static int[] reduce (int[] values) {
		int srcSize = values.length;
		int resultSize = (srcSize + 1) / 2;
		int [] result = new int[resultSize];
		for (int i = 0; i < srcSize; i = i + 2) {
			int v1 = values[i];
			int i2 = i + 1;
			int v2 = (i2 < srcSize) ? values[i2] : 0;
			result[i/2] = v1 + v2;
		}
		return result;
  }
}

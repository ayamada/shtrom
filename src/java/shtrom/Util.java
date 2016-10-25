package shtrom;

//import java.io.File;
//import java.io.FileReader;
//import java.io.RandomAccessFile;
import java.io.InputStream;
//import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Util {
	public static ByteBuffer genByteBuffer (int size) {
		return ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
	}

	public static byte[] httpBodyToBytes (InputStream input, int len) throws Throwable {
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

	/* TODO:
	 * - byte-array->data
	 * - bist-read
	 * - bist-write
	 * - values->content
	 */

  public static int[] reduce (int[] values) {
		// (int-array (map (fn [v] (apply + v)) (partition-all 2 (seq values))))
		int srcSize = values.length;
		int resultSize = (srcSize + 1) / 2;
		int [] result = new int[resultSize];
		int i = 0;
		while (i < srcSize) {
			int v1 = values[i];
			int i2 = i + 1;
			int v2 = (i2 < srcSize) ? values[i2] : 0;
			result[i/2] = v1 + v2;
			i = i + 2;
		}
		return result;
  }
}

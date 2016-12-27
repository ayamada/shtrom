package shtrom.util;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.BufferedOutputStream;

public class IOUtil {
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

    public static int valuesToContentLength (int[] values) {
        return 16 + (4 * values.length);
    }

    public static byte[] valuesToContent (long start, long end, int[] values) {
        int len = values.length;
        int bbLen = valuesToContentLength(values);
        ByteBuffer bb = genByteBuffer(bbLen);
        bb.putLong(start);
        bb.putLong(end);
        for (int i = 0; i < len; i++) {
            bb.putInt(values[i]);
        }
        return bb.array();
    }

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

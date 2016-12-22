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

    // TODO: このgzip展開に、gzip圧縮時の三倍程度の時間がかかっている。
    //       そんな時間はかからない(普通は圧縮時の方が時間がかかる)と思うので、
    //       もっと効率を良くできないか、あとで調査する。
    public static void bistGunzip (String gzPath, String resultPath) throws IOException {
        GZIPInputStream gis = new GZIPInputStream(new FileInputStream(new File(gzPath)));
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(resultPath)));
        byte[] buf = new byte[1024];
        try {
            while (true) {
                int len = gis.read(buf);
                if (len <= 0) { break; }
                os.write(buf, 0, len);
            }
        }
        finally {
            gis.close();
            os.close();
        }
    }

    public static void bistWriteGzip (String path, int[] values) throws IOException {
        File f = new File(path);
        FileOutputStream fos = new FileOutputStream(f);
        GZIPOutputStream gzos = new GZIPOutputStream(fos);
        int len = values.length;
        try {
            for (int i = 0; i < len; i++) {
                ByteBuffer bb = genByteBuffer(8);
                bb.putInt(values[i]);
                gzos.write(bb.array(), 0, 4);
            }
        }
        finally {
            gzos.close();
        }
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

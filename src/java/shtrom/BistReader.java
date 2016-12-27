package shtrom;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Closeable;
import java.io.IOException;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import shtrom.util.IOUtil;
import shtrom.GzipStore;

public class BistReader implements Closeable {
    private String path;
    private File file;

    public void close () throws IOException {
    }

    public BistReader (String path) throws IOException {
        File file = new File(path);
        this.path = path;
        this.file = file;
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

    private static int bReadInteger (DataInputStream rdr) throws EOFException, IOException {
        ByteBuffer bb = IOUtil.genByteBuffer(8);
        readByteBuffer(rdr, bb, 4);
        bb.flip();
        return bb.getInt();
    }

    private void prepareRead () throws IOException {
        GzipStore.gunzipBist(path);
        if (! file.exists()) {
            throw new IOException("File " + path + " not found");
        }
    }

    public int[] read () throws IOException {
        prepareRead();
        int len = (int)(file.length() / 4);
        int[] result = new int[len];
        DataInputStream rdr = new DataInputStream(new FileInputStream(file));
        try {
            for (int i = 0; i < len; i++) {
                result[i] = bReadInteger(rdr);
            }
        }
        finally {
            rdr.close();
        }
        return result;
    }

    public int[] readWithRange (int left, int right) throws IOException {
        prepareRead();
        if (right <= left) { return new int[0]; }
        int len = right - left;
        int[] result = new int[len];
        DataInputStream rdr = new DataInputStream(new FileInputStream(file));
        try {
            rdr.skipBytes(left * 4);
            for (int i = 0; i < len; i++) {
                result[i] = bReadInteger(rdr);
            }
        }
        finally {
            rdr.close();
        }
        return result;
    }

    public long length () throws IOException {
        prepareRead();
        return file.length() / 4;
    }
}

package shtrom;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Closeable;
import java.io.IOException;
import java.io.EOFException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;
import java.util.ArrayList;
import shtrom.util.IOUtil;
import shtrom.GzipStore;

public class BistWriter implements Closeable {
    private String path;

    public void close () throws IOException {
    }

    public BistWriter (String path) throws IOException {
        this.path = path;
    }

    public void writeGzip (int[] values) throws IOException {
        GzipStore.gzipBist(path, values);
    }

    public void write (int[] values) throws IOException {
        File f = new File(path);
        FileOutputStream fos = new FileOutputStream(f);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        try {
            int len = values.length;
            for (int i = 0; i < len; i++) {
                ByteBuffer bb = IOUtil.genByteBuffer(8);
                bb.putInt(values[i]);
                bos.write(bb.array(), 0, 4);
            }
        }
        finally {
            bos.close();
        }
    }
}

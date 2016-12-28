package shtrom;

import java.lang.System;
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
import java.util.Hashtable;
import java.util.Collections;
import java.io.BufferedOutputStream;
import shtrom.util.IOUtil;

public class GzipStore {
    public static long ttlMsec = 1 * 60 * 60 * 1000;
    public static long ttlCheckThresholdMsec = 1000;
    private static final Hashtable<String, Long> cacheTable = new Hashtable<String, Long>();
    public static long lastGcRunTimestamp = 0;

    public static String gzPath (String path) {
        return path + ".gz";
    }

    private static void _delete (String path) {
        cacheTable.remove(path);
        File f = new File(path);
        File gzF = new File(gzPath(path));
        if (gzF.exists() && f.exists()) {
            f.delete();
        }
    }

    public static void delete (String path) {
        synchronized (cacheTable) {
            _delete(path);
        }
    }

    public static void deleteAllForce () {
        for (String path : Collections.list(cacheTable.keys())) {
            _delete(path);
        }
    }

    public static void deleteAll () {
        synchronized (cacheTable) {
            deleteAllForce();
        }
    }

    public static void gc () {
        synchronized (cacheTable) {
            long now = System.currentTimeMillis();
            if ((lastGcRunTimestamp + ttlCheckThresholdMsec) < now) {
                lastGcRunTimestamp = now;
                for (String path : Collections.list(cacheTable.keys())) {
                    Long timestamp = cacheTable.get(path);
                    if ((timestamp + ttlMsec) < now) {
                        _delete(path);
                    }
                }
            }
        }
    }

    public static void gunzipBist (String path) throws IOException {
        synchronized (cacheTable) {
            String gzipPath = gzPath(path);
            File gzipF = new File(gzipPath);
            File targetF = new File(path);
            if (gzipF.exists() && ! targetF.exists()) {
                GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzipF));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetF));
                byte[] buf = new byte[1024];
                try {
                    while (true) {
                        int len = gis.read(buf);
                        if (len <= 0) { break; }
                        bos.write(buf, 0, len);
                    }
                }
                finally {
                    gis.close();
                    bos.close();
                }
            }
            cacheTable.put(path, System.currentTimeMillis());
        }
        gc();
    }

    public static void gzipBist (String path, int[] values) throws IOException {
        synchronized (cacheTable) {
            File f = new File(gzPath(path));
            FileOutputStream fos = new FileOutputStream(f);
            GZIPOutputStream gzos = new GZIPOutputStream(fos);
            try {
                int len = values.length;
                for (int i = 0; i < len; i++) {
                    ByteBuffer bb = IOUtil.genByteBuffer(8);
                    bb.putInt(values[i]);
                    gzos.write(bb.array(), 0, 4);
                }
            }
            finally {
                gzos.close();
            }
            _delete(path);
        }
        gc();
    }
}

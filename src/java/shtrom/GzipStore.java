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
import shtrom.BistWriter; // TODO: これが環状参照になると思う、あとでどうにかする事

public class GzipStore {
    public static long ttlMsec = 1 * 60 * 60 * 1000;
    public static long ttlCheckThresholdMsec = 1000;
    // TODO: このcacheTable回りはsynchronizedじゃ不完全で、一つのロックオブジェクトでロックする必要があるかも(ただしその場合は、deleteAll等からdeleteを呼ぶ部分に要注意)
    private static Hashtable<String, Long> cacheTable = new Hashtable<String, Long>();
    public static long lastGcRunTimestamp = 0;

    public static String gzPath (String path) {
        return path + ".gz";
    }

    synchronized public static void delete (String path) {
        cacheTable.remove(path);
        File f = new File(path);
        File gzF = new File(gzPath(path));
        // NB: gzFが存在せずにpathのみ存在している場合はpathを消してはならない
        //     (backward compatibilityにて、この状態になる場合がある為)
        if (gzF.exists() && f.exists()) {
            f.delete();
        }
    }

    synchronized public static void deleteAll () {
        for (String path : Collections.list(cacheTable.keys())) {
            delete(path);
        }
    }

    synchronized public static void gc () {
        long now = System.currentTimeMillis();
        if ((lastGcRunTimestamp + ttlCheckThresholdMsec) < now) {
            lastGcRunTimestamp = now;
            for (String path : Collections.list(cacheTable.keys())) {
                Long timestamp = cacheTable.get(path);
                if ((timestamp + ttlMsec) < now) {
                    delete(path);
                }
            }
        }
    }

    synchronized public static void touch (String path, boolean newEntry) {
        Long oldTimestamp = cacheTable.get(path);
        if (newEntry || (oldTimestamp != null)) {
            cacheTable.put(path, System.currentTimeMillis());
        }
    }

    // NB: このgunzip処理に、gzip圧縮時の三倍程度の時間がかかっている。
    //     圧縮時よりも展開時の方が時間がかからなさそうに思えるのだが、
    //     試行錯誤してみてもこれ以上良くならなかった。
    synchronized public static void gunzipBist (String path) throws IOException {
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
        touch(path, true);
        gc();
    }

    synchronized public static void gzipBist (String path, int[] values) throws IOException {
        try (BistWriter bw = new BistWriter(gzPath(path))) {
            bw.writeGzip(values);
        }
        delete(path);
        gc();
    }
}

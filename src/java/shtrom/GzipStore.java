package shtrom;

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

public class GzipStore {
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
}

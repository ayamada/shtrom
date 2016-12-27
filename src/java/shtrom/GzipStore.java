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
    // NB: このgunzip処理に、gzip圧縮時の三倍程度の時間がかかっている。
    //     圧縮時よりも展開時の方が時間がかからなさそうに思えるのだが、
    //     試行錯誤してみてもこれ以上良くならなかった。
    public static void bistGunzip (String gzipPath, String gunzipPath) throws IOException {
        GZIPInputStream gis = new GZIPInputStream(new FileInputStream(new File(gzipPath)));
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(gunzipPath)));
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

package shtrom;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Closeable;
import java.io.IOException;
import java.io.EOFException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import shtrom.util.IOUtil;

public class BistWriter implements Closeable {
    private String path;

    public void close () throws IOException {
    }

    public BistWriter (String path) throws IOException {
        this.path = path;
    }

    // TODO
}

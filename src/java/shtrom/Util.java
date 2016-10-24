package shtrom;

import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.io.InputStream;
import java.util.Arrays;

public class Util {
	// (int-array (map (fn [v] (apply + v)) (partition-all 2 (seq values))))
  public static int[] reduce (int[] values) {
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

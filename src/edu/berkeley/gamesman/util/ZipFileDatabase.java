package edu.berkeley.gamesman.util;

import java.io.*;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.gamesman.core.Configuration;

public class ZipFileDatabase {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		File readFrom = new File(args[0]);
		File writeTo = new File(args[0] + ".gz");
		if (writeTo.exists())
			writeTo.delete();
		writeTo.createNewFile();
		FileInputStream fis = new FileInputStream(readFrom);
		int confLength = 0;
		for (int i = 24; i >= 0; i -= 8) {
			confLength <<= 8;
			confLength |= fis.read();
		}
		byte[] confArray = new byte[confLength];
		fis.read(confArray);
		Configuration conf = Configuration.load(confArray);
		conf.setProperty("gamesman.database", "GZippedFileDatabase");
		conf.setProperty("gamesman.db.uri", args[0] + ".gz");
		OutputStream fos = new FileOutputStream(writeTo);
		confArray = conf.store();
		for (int i = 24; i >= 0; i -= 8)
			fos.write(confArray.length >> i);
		fos.write(confArray);
		fos = new GZIPOutputStream(fos);
		long numBytes = (conf.getHasher().numHashes() + conf.recordsPerGroup - 1)
				/ conf.recordsPerGroup * conf.recordGroupByteLength;
		byte[] tempArray = new byte[65536];
		long i = 0;
		while (i < numBytes) {
			int bytes = fis.read(tempArray);
			if (bytes < 0) {
				fos.write(tempArray, 0, (int) (numBytes - i));
				break;
			} else
				fos.write(tempArray, 0, bytes);
			i += bytes;
		}
		fis.close();
		fos.close();
	}
}

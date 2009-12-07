package edu.berkeley.gamesman.util;

import java.io.*;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.gamesman.core.Configuration;

public class ZipFileDatabase {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		File readFrom = new File(args[0]);
		File writeTo = new File(args[0] + ".gz");
		int bufferSize;
		long entrySize;
		if (args.length > 1) {
			entrySize = Long.parseLong(args[1]) << 10;
			if (args.length > 2)
				bufferSize = Integer.parseInt(args[2]) << 10;
			else
				bufferSize = 1 << 22;
		} else {
			entrySize = 0;
			bufferSize = 1 << 22;
		}
		bufferSize = (int) Math.min(bufferSize, entrySize);
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
		conf.setProperty("zip.entryKB", Long.toString(entrySize >> 10));
		conf.setProperty("zip.bufferKB", Integer.toString(bufferSize >> 10));
		long numBytes = (conf.getHasher().numHashes() + conf.recordsPerGroup - 1)
				/ conf.recordsPerGroup * conf.recordGroupByteLength;
		FileOutputStream fos = new FileOutputStream(writeTo);
		confArray = conf.store();
		for (int i = 24; i >= 0; i -= 8)
			fos.write(confArray.length >> i);
		fos.write(confArray);
		GZIPOutputStream gos;
		byte[] tempArray = new byte[bufferSize];
		if (entrySize == 0) {
			gos = new GZIPOutputStream(fos, bufferSize);
			long count = 0;
			while (count < numBytes) {
				int bytes = fis.read(tempArray);
				if (bytes < 0) {
					gos.write(tempArray, 0, (int) (numBytes - count));
					break;
				} else
					gos.write(tempArray, 0, bytes);
				count += bytes;
			}
			fis.close();
			gos.close();
		} else {
			int numPosits = (int) ((numBytes + entrySize - 1) / entrySize);
			long[] posits = new long[numPosits];
			long pos = fos.getChannel().position();
			long count = 0;
			fos.getChannel().position(pos + numPosits << 3);
			for (int i = 0; i < numPosits; i++) {
				pos = fos.getChannel().position();
				posits[i] = pos;
				gos = new GZIPOutputStream(fos);
				long tot = (i + 1) * entrySize;
				while (count < tot) {
					int bytes;
					if (bufferSize < tot - count)
						bytes = fis.read(tempArray);
					else
						bytes = fis.read(tempArray, 0, (int) (tot - count));
					if (bytes < 0) {
						gos.write(tempArray, 0, (int) (numBytes - count));
						count = numBytes;
						break;
					} else
						gos.write(tempArray, 0, bytes);
					count += bytes;
				}
				gos.finish();
			}
			fos.getChannel().position(confArray.length + 4);
			for (int i = 0; i < numPosits; i++) {
				for (int bit = 56; bit >= 0; bit -= 8)
					fos.write((int) (posits[i] >> bit));
			}
			fos.close();
		}
	}
}

package edu.berkeley.gamesman.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class TestingClass {
	public static void main(String[] args) throws IOException {
		File in = new File(args[0]);
		File out = new File(args[1]);
		FileInputStream fis = new FileInputStream(in);
		FileOutputStream fos = new FileOutputStream(out);
		byte[] b = new byte[22];
		Database.readFully(fis, b, 0, 22);
		DatabaseHeader header = new DatabaseHeader(b);
		int b2len = 0;
		for (int i = 18; i < 22; i++) {
			b2len <<= 8;
			b2len |= b[i] & 255;
		}
		byte[] b2 = new byte[b2len];
		Database.readFully(fis, b2, 0, b2len);
		Properties props = new Properties();
		ByteArrayInputStream inStream = new ByteArrayInputStream(b2);
		props.load(inStream);
		props.setProperty("gamesman.database", GZippedClosedFileDatabase.class
				.getName());
		int entrySize = Integer.parseInt(props.getProperty(
				"gamesman.db.zip.entryKB", "64")) << 10;
		int numEntries = (int) (((header.numRecords + header.recordsPerGroup - 1)
				/ header.recordsPerGroup
				* header.recordGroupByteLength
				+ entrySize - 1) / entrySize);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		props.store(baos, "");
		b2 = baos.toByteArray();
		int i = 18;
		for (int s = 24; s >= 0; s -= 8) {
			b[i++] = (byte) (b2.length >>> s);
		}
		fos.write(b);
		fos.write(b2);
		int dif = b2.length - b2len;
		long[] entryPoints = new long[numEntries + 1];
		byte[] entryBytes = new byte[entryPoints.length << 3];
		Database.readFully(fis, entryBytes, 0, entryBytes.length);
		int c = 0;
		for (i = 0; i < entryPoints.length; i++) {
			for (int s = 0; s < 8; s++) {
				entryPoints[i] <<= 8;
				entryPoints[i] |= entryBytes[c | s] & 255;
			}
			entryPoints[i] += dif;
			for (int s = 56; s >= 0; s -= 8) {
				entryBytes[c++] = (byte) (entryPoints[i] >>> s);
			}
		}
		fos.write(entryBytes);
		byte[] tBuffer = new byte[2048];
		int bRead = fis.read(tBuffer);
		while (bRead >= 0) {
			fos.write(tBuffer, 0, bRead);
			bRead = fis.read(tBuffer);
		}
		fis.close();
		fos.close();
	}
}

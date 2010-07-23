package edu.berkeley.gamesman.database;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;

public class ReadZippedRecords {
	public static final int BUFFER_SIZE = 4096;

	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		int i = 0;
		String databaseFile = args[i++];
		long firstRecord = Long.parseLong(args[i++]);
		long numRecords = Long.parseLong(args[i++]);
		GZippedFileDatabase db;
		db = (GZippedFileDatabase) Database.openDatabase(
				GZippedFileDatabase.class.getName(), databaseFile, firstRecord,
				numRecords);
		Configuration conf = db.getConfiguration();
		DatabaseHandle dh = db.getHandle();
		long firstByte = db.toByte(firstRecord);
		long lastByte = db.lastByte(firstRecord + numRecords);
		long numBytes = lastByte - firstByte;
		numBytes = db.prepareZippedRange(dh, firstByte, numBytes);
		int extraBytes = db.extraBytes(firstByte);
		byte[] arr = new byte[(int) Math.min(numBytes, BUFFER_SIZE)];
		for (i = 24; i >= 0; i -= 8) {
			System.out.write(extraBytes >>> i);
		}
		boolean first = true;
		while (numBytes > 0) {
			int bytesRead = db.getZippedBytes(dh, arr, 0, BUFFER_SIZE
					- (first ? 4 : 0));
			first = false;
			System.out.write(arr, 0, bytesRead);
			System.out.flush();
			numBytes -= bytesRead;
		}
		db.close();
	}
}

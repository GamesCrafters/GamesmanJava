package edu.berkeley.gamesman.database;

import java.io.IOException;

/**
 * Reads a set of GZipped records to System.out. It is expected this will be called by
 * another instance of the program using ssh
 * 
 * @author dnspies
 */
public class ReadZippedRecords {

	/**
	 * @param args
	 *            The (optional) job file, the database file, the index of the
	 *            first record to read, the number of records to read
	 * @throws ClassNotFoundException
	 *             If the configuration object is bad
	 * @throws IOException
	 *             If there's an error writing to System.out
	 */
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
		DatabaseHandle dh = db.getHandle();
		long firstByte = db.toByte(firstRecord);
		long lastByte = db.lastByte(firstRecord + numRecords);
		long numBytes = lastByte - firstByte;
		numBytes = db.prepareZippedRange(dh, firstByte, numBytes);
		int extraBytes = db.extraBytes(firstByte);
		byte[] arr = new byte[(int) Math.min(numBytes, ReadRecords.BUFFER_SIZE)];
		for (i = 24; i >= 0; i -= 8) {
			System.out.write(extraBytes >>> i);
		}
		boolean first = true;
		while (numBytes > 0) {
			int bytesRead = db.getZippedBytes(dh, arr, 0, ReadRecords.BUFFER_SIZE
					- (first ? 4 : 0));
			first = false;
			System.out.write(arr, 0, bytesRead);
			System.out.flush();
			numBytes -= bytesRead;
		}
		db.close();
	}
}

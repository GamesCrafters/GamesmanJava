package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.gamesman.core.Configuration;

public class ReadRecords {
	private static final int BUFFER_SIZE = 4096;

	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		String jobFile = args[0];
		String databaseFile = args[1];
		long firstRecord = Long.parseLong(args[2]);
		long numRecords = Long.parseLong(args[3]);
		Configuration conf = new Configuration(jobFile);
		conf.setProperty("gamesman.db.uri", databaseFile);
		Database db = Database.openDatabase(databaseFile, conf, false,
				firstRecord, numRecords);
		long firstByte = db.toByte(firstRecord);
		long numBytes = db.numBytes(firstRecord, numRecords);
		byte[] arr = new byte[(int) Math.min(numBytes, BUFFER_SIZE)];
		db.seek(firstByte);
		OutputStream out = System.out;
		if (conf.getBoolean("gamesman.zippedTransfer", false))
			out = new GZIPOutputStream(out);
		while (numBytes > 0) {
			int bytesToRead = (int) Math.min(numBytes, BUFFER_SIZE);
			db.getBytes(arr, 0, bytesToRead);
			out.write(arr, 0, bytesToRead);
			numBytes -= bytesToRead;
		}
		out.close();
	}
}

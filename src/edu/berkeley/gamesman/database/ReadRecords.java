package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.gamesman.core.Configuration;

public class ReadRecords {
	private static final int BUFFER_SIZE = 4096;

	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		int i = 0;
		String jobFile = null;
		if (args.length > 3)
			jobFile = args[i++];
		String databaseFile = args[i++];
		long firstRecord = Long.parseLong(args[i++]);
		long numRecords = Long.parseLong(args[i++]);
		Configuration conf;
		Database db;
		if (jobFile == null) {
			db = Database.openDatabase(databaseFile, false, firstRecord,
					numRecords);
			conf = db.getConfiguration();
		} else {
			conf = new Configuration(jobFile);
			db = Database.openDatabase(databaseFile, conf, false, firstRecord,
					numRecords);
		}
		DatabaseHandle dh = db.getHandle();
		long firstByte = db.toByte(firstRecord);
		int firstNum = db.toNum(firstRecord);
		long lastByte = db.lastByte(firstRecord + numRecords);
		int lastNum = db.toNum(firstRecord + numRecords);
		long numBytes = lastByte - firstByte;
		byte[] arr = new byte[(int) Math.min(numBytes, BUFFER_SIZE)];
		db.seek(dh, firstByte);
		// TODO Figure out how to do this right
		OutputStream out = System.out;
		if (conf.getBoolean("gamesman.zippedTransfer", false))
			out = new GZIPOutputStream(out);
		while (numBytes > 0) {
			int bytesToRead = (int) Math.min(numBytes, BUFFER_SIZE);
			db.getBytes(dh, arr, 0, bytesToRead);
			out.write(arr, 0, bytesToRead);
			numBytes -= bytesToRead;
		}
		db.closeHandle(dh);
		out.close();
	}
}

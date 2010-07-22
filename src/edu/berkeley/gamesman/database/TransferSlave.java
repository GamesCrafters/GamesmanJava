package edu.berkeley.gamesman.database;

import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;

public final class TransferSlave {
	public static void main(String[] args) {
		GZippedFileDatabase db = (GZippedFileDatabase) Database
				.openDatabase(args[0]);
		long firstRecord = Long.parseLong(args[1]);
		long numRecords = Long.parseLong(args[2]);
		Configuration conf = db.getConfiguration();
		long gameLength = conf.getGame().numHashes();
		Scanner scan = new Scanner(System.in);
		DistributedDatabase allRecords = new DistributedDatabase(conf, db
				.getHeader(0, gameLength), scan, System.out);
		GZippedFileDatabase.GZipHandle dh = db.getHandle();
		long numBytes = db.prepareMoveRange(dh, firstRecord, numRecords,
				allRecords);
		allRecords.close();
		if (numBytes == 0) {
			System.out.println("skip");
		} else {
			System.out.print("ready\n");
			System.out.flush();
			while (!scan.nextLine().equals("go"))
				;
			byte[] arr = new byte[(int) Math.min(ReadZippedRecords.BUFFER_SIZE,
					numBytes)];
			while (numBytes > 0) {
				int bytesRead = db.getZippedBytes(dh, arr, 0,
						ReadZippedRecords.BUFFER_SIZE);
				System.out.write(arr, 0, bytesRead);
				System.out.flush();
				numBytes -= bytesRead;
			}
		}
		db.close();
	}
}

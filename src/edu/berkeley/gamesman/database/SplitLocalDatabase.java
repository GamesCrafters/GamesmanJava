package edu.berkeley.gamesman.database;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;

public class SplitLocalDatabase extends SplitDatabase {
	public SplitLocalDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException, ClassNotFoundException {
		super(new DataInputStream(new FileInputStream(uri)), uri, conf,
				firstRecordIndex, numRecords, reading, writing);
	}

	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		String jobFile = args[0];
		Configuration conf = new Configuration(jobFile);
		String dbListFile = args[1];
		String dbUri = args[2];
		final long firstRecordIndex, numRecords;
		if (args.length > 3) {
			firstRecordIndex = Integer.parseInt(args[3]);
			numRecords = Integer.parseInt(args[4]);
		} else {
			firstRecordIndex = 0L;
			numRecords = conf.getGame().numHashes();
		}
		Scanner dbScanner = new Scanner(new File(dbListFile));
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(dbUri));
		dos.writeLong(firstRecordIndex);
		dos.writeLong(numRecords);
		conf.store(dos);
		long currentRecord = firstRecordIndex;
		while (dbScanner.hasNext()) {
			dos.writeUTF(dbScanner.next());
			dos.writeUTF(dbScanner.next());
			dos.writeLong(currentRecord);
			long nextNum = dbScanner.nextLong();
			dos.writeLong(nextNum);
			currentRecord += nextNum;
		}
		if (currentRecord != firstRecordIndex + numRecords)
			throw new Error("Database is incomplete");
		dos.close();
	}
}

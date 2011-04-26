package edu.berkeley.gamesman.database;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;

/**
 * A database on the local file system which subclasses SplitDatabase
 * 
 * @author dnspies
 */
public class SplitLocalDatabase extends SplitDatabase {
	/**
	 * @param uri
	 *            The file containing the database headers for this split
	 *            database
	 * @param conf
	 *            The configuration object
	 * @param firstRecordIndex
	 *            The index of the first record contained in this database
	 * @param numRecords
	 *            The number of records contained in this database
	 * @param reading
	 *            Whether to open the database for reading
	 * @param writing
	 *            Whether to open the database for writing
	 * @throws IOException
	 *             If an IOException occurs while reading the header
	 * @throws ClassNotFoundException
	 *             If a ClassNotFoundException occurs while reading the
	 *             configuration for any underlying database
	 */
	public SplitLocalDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException, ClassNotFoundException {
		super(new DataInputStream(new FileInputStream(uri)), conf,
				firstRecordIndex, numRecords, reading, writing);
	}

	/**
	 * Creates a SplitLocalDatabase
	 * 
	 * @param args
	 *            The job file to use for the configuration, followed by the
	 *            file containing the list of databases followed by the uri of
	 *            the new database and finally firstRecordIndex and numRecords
	 *            (defaults to entire game)
	 * @throws ClassNotFoundException
	 *             If the job file contains a bad class name
	 * @throws IOException
	 *             If an IO Error occurs
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		String jobFile = args[0];
		Configuration conf = new Configuration(jobFile);
		String dbListFile = args[1];
		String dbUri = args[2];
		final long firstRecordIndex, numRecords;
		if (args.length > 3) {
			firstRecordIndex = Long.parseLong(args[3]);
			numRecords = Long.parseLong(args[4]);
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

package edu.berkeley.gamesman.database;

import java.io.IOException;

import edu.berkeley.gamesman.DebugSetup;
import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Progressable;
import edu.berkeley.gamesman.util.RandomAccessFileDataInputStream;
import edu.berkeley.gamesman.util.RandomAccessFileDataOutputStream;

public class GZippedFileDatabase extends GZippedDatabase {
	public GZippedFileDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException {
		super(writing ? null : new RandomAccessFileDataInputStream(uri),
				writing ? new RandomAccessFileDataOutputStream(uri, "rw")
						: null, conf, firstRecordIndex, numRecords, reading,
				writing);
	}

	/**
	 * @param args
	 *            The path to the old database to be read from, the path to the
	 *            new database to be zipped to, and possibly a boolean
	 *            indicating whether to synchronize reads from the old database
	 *            (defaults to true)
	 * @throws IOException
	 *             If an IO Error occurs while zipping
	 */
	public static void main(String[] args) throws IOException {
		Database readFrom;
		try {
			readFrom = Database.openDatabase(args[0]);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
		Configuration conf = readFrom.conf;
		Configuration conf2 = conf.cloneAll();
		DebugSetup.setup(conf2.props);
		conf2.setProperty("gamesman.database",
				GZippedFileDatabase.class.getName());
		String uri = args[1];
		zip(uri, conf2, readFrom,
				args.length > 2 ? Boolean.parseBoolean(args[2]) : true);
	}

	/**
	 * @param uri
	 *            The name of the file to zip to
	 * @param conf
	 *            The configuration object
	 * @param readFrom
	 *            The database to read bytes from
	 * @param synchronizedRead
	 *            If reading blocks should be done synchronized (should be true
	 *            unless the database supports multi-threaded simultaneous
	 *            reading (otherwise this will cause it to jump back and forth
	 *            and slow the zipping process))
	 * @throws IOException
	 *             If an IOException occurs while zipping
	 */
	public static void zip(String uri, Configuration conf, Database readFrom,
			boolean synchronizedRead) throws IOException {
		zip(uri, conf, readFrom, (Progressable) null, synchronizedRead);
	}

	/**
	 * @param uri
	 *            The name of the file to zip to
	 * @param conf
	 *            The configuration object
	 * @param readFrom
	 *            The database to read bytes from
	 * @param progress
	 *            A Progressable for the zip to report progress
	 * @param synchronizedRead
	 *            If reading blocks should be done synchronized (should be true
	 *            unless the database supports multi-threaded simultaneous
	 *            reading (otherwise this will cause it to jump back and forth
	 *            and slow the zipping process))
	 * @throws IOException
	 *             If an IOException occurs while zipping
	 */
	public static void zip(String uri, Configuration conf, Database readFrom,
			Progressable progress, boolean synchronizedRead) throws IOException {
		final GZippedDatabase gzfd = new GZippedFileDatabase(uri, conf,
				readFrom.firstRecordIndex, readFrom.numRecords, false, true);
		GZippedDatabase.zip(conf, readFrom, gzfd, progress, synchronizedRead);
		gzfd.close();
	}
}

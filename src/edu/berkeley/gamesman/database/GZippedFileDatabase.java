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

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		Database readFrom = Database.openDatabase(args[0]);
		Configuration conf = readFrom.conf;
		Configuration conf2 = conf.cloneAll();
		DebugSetup.setup(conf2.props);
		conf2.setProperty("gamesman.database",
				GZippedFileDatabase.class.getName());
		String uri = args[1];
		zip(uri, conf2, readFrom);
	}

	public static void zip(String uri, Configuration conf, Database readFrom)
			throws IOException {
		zip(uri, conf, readFrom, (Progressable) null);
	}

	public static void zip(String uri, Configuration conf, Database readFrom,
			Progressable progress) throws IOException {
		final GZippedDatabase gzfd = new GZippedFileDatabase(uri, conf,
				readFrom.firstRecordIndex, readFrom.numRecords, false, true);
		zip(conf, readFrom, gzfd, progress);
		gzfd.close();
	}
}

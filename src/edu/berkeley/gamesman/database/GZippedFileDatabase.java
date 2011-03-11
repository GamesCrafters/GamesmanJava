package edu.berkeley.gamesman.database;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Progressable;
import edu.berkeley.gamesman.util.RandomAccessFileDataInputStream;
import edu.berkeley.gamesman.util.RandomAccessFileDataOutputStream;

public class GZippedFileDatabase extends GZippedDatabase {
	private static final int INTERMEDIATE_BYTES = 65536;

	public GZippedFileDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException {
		super(writing ? null : new RandomAccessFileDataInputStream(uri),
				writing ? new RandomAccessFileDataOutputStream(uri, "rw")
						: null, uri, conf, firstRecordIndex, numRecords,
				reading, writing);
	}

	public static void zip(String uri, Configuration conf, Database readFrom)
			throws IOException {
		zip(uri, conf, readFrom, null);
	}

	public static void zip(String uri, Configuration conf, Database readFrom,
			Progressable progress) throws IOException {
		GZippedFileDatabase gzfd = new GZippedFileDatabase(uri, conf,
				readFrom.firstRecordIndex, readFrom.numRecords, false, true);
		byte[] intermediate = new byte[INTERMEDIATE_BYTES];
		DatabaseHandle dh = readFrom.getHandle(true);
		DatabaseHandle writeHandle = gzfd.getHandle(false);
		long firstByteIndex = readFrom.firstByteIndex();
		long numBytes = readFrom.numBytes();
		readFrom.prepareReadRange(dh, firstByteIndex, numBytes);
		gzfd.prepareWriteRange(writeHandle, firstByteIndex, numBytes);
		long bytesRead = 0;
		while (bytesRead < numBytes) {
			int nextRead = readFrom.readBytes(dh, intermediate, 0,
					INTERMEDIATE_BYTES);
			gzfd.writeFullBytes(writeHandle, intermediate, 0, nextRead);
			bytesRead += nextRead;
			if (nextRead > 0 && progress != null)
				progress.progress();
		}
		gzfd.close();
	}

	// TODO Multi-threaded zipping

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		Database readDb = Database.openDatabase(args[0]);
		Configuration conf = readDb.conf;
		Configuration conf2 = conf.cloneAll();
		conf2.setProperty("gamesman.database",
				GZippedFileDatabase.class.getName());
		String uri = args[1];
		zip(uri, conf2, readDb);
	}
}

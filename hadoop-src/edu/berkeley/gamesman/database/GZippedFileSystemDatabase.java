package edu.berkeley.gamesman.database;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;

public class GZippedFileSystemDatabase extends GZippedDatabase {
	public GZippedFileSystemDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException {
		super(new GZippedFileSystemDatabaseInputStream(uri), null, uri, conf,
				firstRecordIndex, numRecords, reading, writing);
	}
}

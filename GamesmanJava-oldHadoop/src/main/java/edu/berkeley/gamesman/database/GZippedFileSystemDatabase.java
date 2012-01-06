package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.net.URISyntaxException;

import edu.berkeley.gamesman.core.Configuration;

public class GZippedFileSystemDatabase extends GZippedDatabase {
	public GZippedFileSystemDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException, URISyntaxException {
		super(new SeekableFileSystemDatabaseInputStream(uri), null, conf,
				firstRecordIndex, numRecords, reading, writing);
	}
}

package edu.berkeley.gamesman.database;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.util.SeekableInputStream;

public class FileFileSystemDatabase extends SeekableDatabase {

	private static class FFSDArgs {
		public SeekableInputStream in;
		public String uri;
		public Configuration conf;
		public long firstRecordIndex;
		public long numRecords;
		public boolean reading;
		public boolean writing;

		public FFSDArgs(String uri, Configuration conf, long firstRecordIndex,
				long numRecords, boolean reading, boolean writing)
				throws IOException {
			this.uri = uri;
			this.conf = conf;
			this.firstRecordIndex = firstRecordIndex;
			this.numRecords = numRecords;
			this.reading = reading;
			this.writing = writing;
			in = new SeekableFileSystemDatabaseInputStream(uri);
		}
	}

	public FileFileSystemDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException {
		this(new FFSDArgs(uri, conf, firstRecordIndex, numRecords, reading,
				writing));
	}

	private FileFileSystemDatabase(FFSDArgs args) throws IOException {
		super(args.in, null, args.uri, args.conf, args.firstRecordIndex,
				args.numRecords, args.reading, args.writing);
		assert !writing;
	}

}

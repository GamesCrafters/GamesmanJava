package edu.berkeley.gamesman.database;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.util.SeekableInputStream;
import edu.berkeley.gamesman.database.util.SeekableOutputStream;
import edu.berkeley.gamesman.util.RandomAccessFileDataInputStream;
import edu.berkeley.gamesman.util.RandomAccessFileDataOutputStream;

public class FileDatabase extends SeekableDatabase {

	private static class FDArgs {

		public SeekableInputStream in;
		public SeekableOutputStream out;
		public String uri;
		public Configuration conf;
		public long firstRecordIndex;
		public long numRecords;
		public boolean reading;
		public boolean writing;

		public FDArgs(String uri, Configuration conf, long firstRecordIndex,
				long numRecords, boolean reading, boolean writing)
				throws FileNotFoundException {
			this.uri = uri;
			this.conf = conf;
			this.firstRecordIndex = firstRecordIndex;
			this.numRecords = numRecords;
			this.reading = reading;
			this.writing = writing;
			RandomAccessFile raf;
			if (writing)
				raf = new RandomAccessFile(uri, "rw");
			else
				raf = new RandomAccessFile(uri, "r");
			if (reading)
				in = new RandomAccessFileDataInputStream(raf);
			if (writing)
				out = new RandomAccessFileDataOutputStream(raf);
		}

		public FDArgs(String uri) throws IOException, ClassNotFoundException {
			this.uri = uri;
			RandomAccessFile raf;
			raf = new RandomAccessFile(uri, "rw");
			in = new RandomAccessFileDataInputStream(raf);
			firstRecordIndex = in.readLong();
			numRecords = in.readLong();
			conf = Configuration.load(in);
			in.reset();
			out = new RandomAccessFileDataOutputStream(raf);
			reading = true;
			writing = true;
		}
	}

	public FileDatabase(String uri, Configuration conf, long firstRecordIndex,
			long numRecords, boolean reading, boolean writing)
			throws IOException {
		this(new FDArgs(uri, conf, firstRecordIndex, numRecords, reading,
				writing), true);
	}

	private FileDatabase(FDArgs fdArgs, boolean overwrite) throws IOException {
		super(fdArgs.in, fdArgs.out, fdArgs.uri, fdArgs.conf,
				fdArgs.firstRecordIndex, fdArgs.numRecords, fdArgs.reading,
				fdArgs.writing, overwrite);
	}

	public FileDatabase(String uri) throws IOException, ClassNotFoundException {
		this(new FDArgs(uri), false);
	}

}

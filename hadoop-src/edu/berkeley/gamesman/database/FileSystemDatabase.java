package edu.berkeley.gamesman.database;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Pair;

public abstract class FileSystemDatabase extends Database {
	protected final FSDataInputStream is;

	private FileSystemDatabase(Path uri, Pair<DatabaseHeader, Configuration> p,
			FSDataInputStream is) {
		super(uri.toString(), p.cdr, false, p.car.firstRecord,
				p.car.numRecords, p.car);
		this.is = is;
	}

	protected FileSystemDatabase(Path uri, FSDataInputStream is) {
		this(uri, getConfHeaderPair(is), is);
	}

	private static Pair<DatabaseHeader, Configuration> getConfHeaderPair(
			FSDataInputStream is) {
		byte[] headBytes = new byte[18];
		try {
			readFully(is, headBytes, 0, 18);
			DatabaseHeader dh = new DatabaseHeader(headBytes);
			Configuration conf = Configuration.load(is);
			return new Pair<DatabaseHeader, Configuration>(dh, conf);
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void close() {
		try {
			closeDatabase();
			is.close();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	protected abstract void closeDatabase();

	public static Configuration readConfiguration(FileSystem fs, Path p)
			throws IOException {
		FSDataInputStream dbStream = fs.open(p);
		dbStream.skip(18);
		Configuration conf;
		try {
			conf = Configuration.load(dbStream);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
		dbStream.close();
		return conf;
	}

	@Override
	public long getSize() {
		throw new UnsupportedOperationException();
	}
}

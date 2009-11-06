package edu.berkeley.gamesman.database;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.util.Util;
import java.io.IOException;

/**
 * The HDFSInputDatabase is a database designed to read directly from a remote
 * file. Unlike HDFSSolidDatabase, does not support compression.
 * 
 * This database only implements the seek-less read() functions, so allows for
 * random access across all threads. Still, it is better to cache the database.
 * 
 * @author Patrick Horn
 * @deprecated HDFSSolidDatabase is more flexable, however this class is still
 *             useful for uncompressed hadoop databases.
 */
public class HDFSInputDatabase extends Database {

	protected FileSystem fs;
	protected Path myFile;

	protected FSDataInputStream fd;

	protected byte[] rawRecord;

	protected byte[] groups;

	protected int groupsLength;

	protected long offset;

	HDFSInputDatabase() {
	}

	HDFSInputDatabase(FileSystem fs) {
		this.fs = fs;
	}

	@Override
	public void close() {
		try {
			fd.close();
		} catch (IOException e) {
			Util.warn("Error while closing input stream for database: " + e);
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void getBytes(long loc, byte[] arr, int off, int len) {
		try {
			fd.readFully(offset+loc, arr, off, len);
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	public void seek(long position) {
		throw new UnsupportedOperationException("HDFSInputDatabase does not implement seek");
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException(
				"HDFSInputDatabase requires a position argument to getBytes");
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException(
				"putBytes unimplemented in read-only database");
	}

	/*
	 * TODO: I think you're using offset wrong. Since the byte index passed to
	 * HDFSInputDatabase is the byte index into the entire game (not just this
	 * file), offset should be a very negative number. Or is that handled
	 * elsewhere?
	 */
	@Override
	public void initialize(String loc) {
		try {
			myFile = new Path(new Path("file:///"), loc);
			fd = fs.open(myFile);
			int headerLen = fd.readInt();
			byte[] header = new byte[headerLen];
			fd.readFully(header);
			if (conf == null) {
				conf = Configuration.load(header);
			}
			offset = fd.getPos();
			rawRecord = new byte[conf.recordGroupByteLength];
		} catch (IOException e) {
			e.printStackTrace();
			Util.fatalError(e.toString());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Util.fatalError(e.toString());
		}
	}
}

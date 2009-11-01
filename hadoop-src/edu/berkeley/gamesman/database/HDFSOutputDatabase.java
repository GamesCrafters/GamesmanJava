package edu.berkeley.gamesman.database;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.hadoop.util.HadoopUtil;
import edu.berkeley.gamesman.util.Util;

import java.io.IOException;

/**
 * The HDFSOutputDatabase is a database designed to write directly to a remote
 * file.
 * 
 * This database only implements the sequential writes, so you must have one
 * HDFSOutputDatabase for each concurrent writing process at a time.
 * 
 * Create an HDFSOuptutDatabase by calling HadoopSplitDatabase.beginWrite() and
 * close/add to the reduce queue using HadoopSplitDatabase.endWrite()
 * 
 * @author Steven Schlansker
 */
public class HDFSOutputDatabase extends HadoopUtil.MapReduceDatabase {

	protected Path myFile;

	protected FSDataOutputStream fd;

	protected long offset;

	HDFSOutputDatabase() {
	}

	HDFSOutputDatabase(FileSystem fs) {
		super(fs);
	}

	@Override
	public void close() {
		try {
			fd.close();
		} catch (IOException e) {
			Util.warn("Error while closing input stream for database: " + e);
		}
	}

	/**
	 * TODO: What do flush and sync do? Are you sure they're necessary? Does
	 * this need to be synchronized?
	 */
	@Override
	public synchronized void flush() {
		try {
			fd.flush();
			fd.sync();
		} catch (IOException e) {
			Util.fatalError("Error while writing to database: " + e);
		}
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		throw new RuntimeException(
				"getBytes called in write-only database for len " + len);
	}

	/**
	 * @return the current file pointer of the sequential write database.
	 */
	public final long getPosition() {
		try {
			return fd.getPos();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public final void seek(long loc) {
		assert (loc + offset == getPosition());
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		try {
			fd.write(arr, off, len);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * TODO: I think you're using offset wrong. Since the byte index passed to
	 * HDFSOutputDatabase is the byte index into the entire game (not just this
	 * file), offset should be a very negative number. Or is that handled
	 * elsewhere?
	 */
	@Override
	public void initialize(String loc) {
		try {
			myFile = new Path(loc);
			boolean previouslyExisted = fs.exists(myFile);
			if (previouslyExisted) {
				Util.fatalError("Not overwriting existing output file "
						+ myFile);
			}
			if (conf == null) {
				Util
						.fatalError("No configuration, but the database is to be created");
			}
			fd = fs.create(myFile);
			byte[] b = conf.store();
			fd.writeInt(b.length);
			fd.write(b);
			offset = fd.getPos();
		} catch (IOException e) {
			e.printStackTrace();
			Util.fatalError(e.toString());
		}
	}
}

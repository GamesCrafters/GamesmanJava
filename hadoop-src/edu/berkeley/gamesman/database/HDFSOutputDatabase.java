package edu.berkeley.gamesman.database;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
public class HDFSOutputDatabase extends SolidDatabase {

	private FileSystem fs;

	protected long offset;

	HDFSOutputDatabase() {
	}

	HDFSOutputDatabase(FileSystem fs) {
		this.fs = fs;
	}
	
	/**
	 * All hadoop classes that need to access the disk need a FileSystem instance.
	 * Must be set before the database is used.
	 * @param fs The hadoop filesystem.
	 */
	public void setFilesystem(FileSystem fs) {
		this.fs = fs;
	}

	/**
	 * TODO: What do flush and sync do? Are you sure they're necessary? Does
	 * this need to be synchronized?
	 */
	@Override
	public synchronized void flush() {
		try {
			outputStream.flush();
			((FSDataOutputStream)outputStream).sync();
		} catch (IOException e) {
			Util.fatalError("Error while writing to database: " + e);
		}
	}

	/**
	 * @return the current file pointer of the sequential write database.
	 */
	public final long getPosition() {
		try {
			return ((FSDataOutputStream)outputStream).getPos();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public final void seek(long loc) {
		assert (loc + offset == getPosition());
	}

	/*
	 * TODO: I think you're using offset wrong. Since the byte index passed to
	 * HDFSOutputDatabase is the byte index into the entire game (not just this
	 * file), offset should be a very negative number. Or is that handled
	 * elsewhere?
	 */
	@Override
	public void initialize(String loc) {
		Path myFile = new Path(loc);
		boolean previouslyExisted = false;
		try {
			previouslyExisted = fs.exists(myFile);
		} catch (IOException e) {
			Util.fatalError("Unable to check if index file "+myFile+" exists",  e);
		}
		if (previouslyExisted) {
			Util.fatalError("Not overwriting existing output file "+myFile);
		}
		if (conf == null) {
			Util.fatalError("No configuration to create database");
		}
		super.initialize(loc);
	}


	@Override
	protected InputStream openInputStream(String uri) throws IOException {
		throw new RuntimeException(
			"openInputStream not supported for HDFSOuptutDatabase");
	}

	@Override
	protected OutputStream openOutputStream(String uri) throws IOException {
		return fs.create(new Path(new Path("file:///"), uri));
	}

}
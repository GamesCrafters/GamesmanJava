package edu.berkeley.gamesman.database;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The HDFSSolidDatabase is a database designed to write directly to a remote
 * file.
 * 
 * Note that the SolidDatabase class handles decompression and compression,
 * however each database is either read-only or write-only, so you need to
 * create a different instance for each.
 * 
 * Create a writable HDFSSolidDatabase by calling HadoopSplitDatabase
 * .beginWrite() and close/add to the reduce queue using HadoopSplitDatabase
 * .endWrite(). SolidDatabase only implements sequential writes, so you must
 * create one HDFSSolidDatabase for each concurrent thread at a time.
 *
 * HadoopSplitDatabase will also use this class to create a read database.
 * Unlike HDFSInputDatabase, SolidDatabase requires reading the whole file
 * into memory.
 * 
 * @author Steven Schlansker
 */
public class HDFSSolidDatabase extends SolidDatabase {

	private FileSystem fs;

	protected long offset;

	HDFSSolidDatabase() {
	}

	HDFSSolidDatabase(FileSystem fs) {
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

	@Override
	public void initialize(String loc) {
		super.initialize(loc);
	}


	@Override
	protected InputStream openInputStream(String uri) throws IOException {
		return fs.open(new Path(uri));
	}

	@Override
	protected OutputStream openOutputStream(String uri) throws IOException {
		if (conf == null) {
			Util.fatalError("No configuration to create database");
		}
		Path myFile = new Path(uri);
		boolean previouslyExisted = false;
		try {
			previouslyExisted = fs.exists(myFile);
		} catch (IOException e) {
			Util.fatalError("Unable to check if index file "+myFile+" exists",  e);
		}
		if (previouslyExisted) {
			//Util.fatalError("Not overwriting existing output file "+myFile);
			Util.warn("!!! Overwriting existing output file "+myFile);
		}
		return fs.create(myFile);
	}

}

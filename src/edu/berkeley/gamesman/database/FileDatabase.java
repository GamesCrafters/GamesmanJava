package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Util;

public final class FileDatabase extends Database {

	/**
	 * The file contained in this FileDatabase
	 */
	public File myFile;

	protected RandomAccessFile fd;

	protected int groupsLength;

	protected long offset;

	private final boolean storeConf;

	public FileDatabase() {
		this(true);
	}

	public FileDatabase(boolean storeConf) {
		this.storeConf = storeConf;
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
	protected synchronized void seek(long loc) {
		try {
			fd.seek(loc + offset);
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	protected synchronized void getBytes(byte[] arr, int off, int len) {
		try {
			fd.read(arr, off, len);
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	protected synchronized void putBytes(byte[] arr, int off, int len) {
		try {
			fd.write(arr, off, len);
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	public void initialize(String loc, boolean solve) {
		try {
			myFile = new File(loc);
			if (solve) {
				FileOutputStream fos = new FileOutputStream(myFile);
				if (conf == null)
					Util
							.fatalError("You must specify a configuration if the database is to be created");
				if (storeConf) {
					conf.store(fos);
				} else
					Configuration.storeNone(fos);
				offset = fos.getChannel().position();
				fos.close();
				fd = new RandomAccessFile(myFile, "rw");
				fd.setLength(offset + numRecords());
			} else {
				FileInputStream fis = new FileInputStream(myFile);
				if (conf == null)
					conf = Configuration.load(fis);
				else
					Configuration.skipConf(fis);
				offset = fis.getChannel().position();
				fis.close();
				fd = new RandomAccessFile(myFile, "r");
			}
			offset -= firstRecord();
		} catch (IOException e) {
			e.printStackTrace();
			Util.fatalError("IO Error", e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	protected synchronized void getBytes(DatabaseHandle dh, long loc,
			byte[] arr, int off, int len) {
		seek(loc);
		getBytes(arr, off, len);
	}

	@Override
	protected synchronized void putBytes(DatabaseHandle dh, long loc,
			byte[] arr, int off, int len) {
		seek(loc);
		putBytes(arr, off, len);
	}
}

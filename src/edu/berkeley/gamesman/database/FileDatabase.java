package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import edu.berkeley.gamesman.core.Configuration;

public final class FileDatabase extends Database {

	/**
	 * The file contained in this FileDatabase
	 */
	private final File myFile;

	private final RandomAccessFile fd;

	private final long offset;

	private DatabaseHandle lastUsed;

	public FileDatabase(String uri, Configuration config, boolean solve,
			long firstRecord, long numRecords) throws IOException {
		this(uri, config, solve, firstRecord, numRecords, true);
	}

	public FileDatabase(String uri, Configuration config, boolean solve,
			long firstRecord, long numRecords, boolean storeConf)
			throws IOException {
		super(uri, config, solve, firstRecord, numRecords);
		myFile = new File(uri);
		if (solve) {
			FileOutputStream fos = new FileOutputStream(myFile);
			if (storeConf) {
				store(fos);
			} else
				storeNone(fos);
			long lastByte = lastByte(firstContainedRecord + numContainedRecords);
			offset = fos.getChannel().position() - toByte(firstContainedRecord);
			fos.close();
			fd = new RandomAccessFile(myFile, "rw");
			fd.setLength(offset + lastByte);
		} else {
			FileInputStream fis = new FileInputStream(myFile);
			skipHeader(fis);
			offset = fis.getChannel().position() - toByte(firstContainedRecord);
			fis.close();
			fd = new RandomAccessFile(myFile, "r");
		}
	}

	@Override
	protected void closeDatabase() {
		try {
			fd.close();
		} catch (IOException e) {
			new Error("Error while closing input stream for database: ", e)
					.printStackTrace();
		}
	}

	@Override
	protected synchronized void seek(DatabaseHandle dh, long loc) {
		super.seek(dh, loc);
		lastUsed = dh;
		try {
			fd.seek(loc + offset);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	protected synchronized void getBytes(DatabaseHandle dh, byte[] arr,
			int off, int len) {
		if (lastUsed != dh)
			seek(dh, dh.location);
		try {
			fd.read(arr, off, len);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	protected synchronized void putBytes(DatabaseHandle dh, byte[] arr,
			int off, int len) {
		if (lastUsed != dh)
			seek(dh, dh.location);
		try {
			fd.write(arr, off, len);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	protected synchronized void getBytes(DatabaseHandle dh, long loc,
			byte[] arr, int off, int len) {
		seek(dh, loc);
		getBytes(dh, arr, off, len);
	}

	@Override
	protected synchronized void putBytes(DatabaseHandle dh, long loc,
			byte[] arr, int off, int len) {
		seek(dh, loc);
		putBytes(dh, arr, off, len);
	}
}

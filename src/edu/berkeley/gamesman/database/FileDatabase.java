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
			long firstRecord, long numRecords, DatabaseHeader header)
			throws IOException {
		super(uri, config, solve, firstRecord, numRecords, header);
		myFile = new File(uri);
		if (solve) {
			FileOutputStream fos = new FileOutputStream(myFile);
			store(fos);
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
	public void close() {
		try {
			fd.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected synchronized void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
		lastUsed = dh;
		try {
			fd.seek(byteIndex + offset);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	protected synchronized int getBytes(DatabaseHandle dh, byte[] arr, int off,
			int maxLen, boolean overwriteEdgesOk) {
		if (lastUsed != dh) {
			lastUsed = dh;
			try {
				fd.seek(dh.location + offset);
			} catch (IOException e) {
				throw new Error(e);
			}
		}
		if (!overwriteEdgesOk)
			return super.getBytes(dh, arr, off, maxLen, false);
		try {
			int numBytes = (int) Math.min(maxLen, dh.lastByteIndex
					- dh.location);
			fd.read(arr, off, numBytes);
			dh.location += numBytes;
			return numBytes;
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	protected synchronized void putRecordsAsBytes(DatabaseHandle dh,
			long byteIndex, int recordNum, byte[] arr, int off, int numBytes,
			int lastNum, boolean edgesAreCorrect) {
		super.putRecordsAsBytes(dh, byteIndex, recordNum, arr, off, numBytes,
				lastNum, edgesAreCorrect);
	}

	@Override
	protected synchronized int putBytes(DatabaseHandle dh, byte[] arr, int off,
			int maxLen, boolean edgesAreCorrect) {
		if (lastUsed != dh) {
			lastUsed = dh;
			try {
				fd.seek(dh.location + offset);
			} catch (IOException e) {
				throw new Error(e);
			}
		}
		if (!edgesAreCorrect)
			return super.putBytes(dh, arr, off, maxLen, false);
		try {
			int numBytes = (int) Math.min(maxLen, dh.lastByteIndex
					- dh.location);
			fd.write(arr, off, numBytes);
			dh.location += numBytes;
			return numBytes;
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	protected synchronized void getRecordsAsBytes(DatabaseHandle dh,
			long byteIndex, int recordNum, byte[] arr, int off, int numBytes,
			int lastNum, boolean overwriteEdgesOk) {
		super.getRecordsAsBytes(dh, byteIndex, recordNum, arr, off, numBytes,
				lastNum, overwriteEdgesOk);
	}

	@Override
	protected synchronized void getBytes(DatabaseHandle dh, long loc,
			byte[] arr, int off, int len) {
		getRecordsAsBytes(dh, loc, 0, arr, off, len, 0, true);
	}

	@Override
	protected synchronized void putBytes(DatabaseHandle dh, long loc,
			byte[] arr, int off, int len) {
		putRecordsAsBytes(dh, loc, 0, arr, off, len, 0, true);
	}
}

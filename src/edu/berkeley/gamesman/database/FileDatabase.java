package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import edu.berkeley.gamesman.core.Configuration;

/**
 * The most basic database. Simply writes the bytes out to a file
 * 
 * @author dnspies
 */
public final class FileDatabase extends Database {

	public FileDatabase(String uri, Configuration conf, long firstRecordIndex,
			long numRecords, boolean reading, boolean writing)
			throws IOException {
		super(conf, firstRecordIndex, numRecords, reading, writing);
		myFile = new File(uri);
		if (writing) {
			myRaf = new RandomAccessFile(myFile, "rw");
			headerLen = writeHeader(myRaf);
		} else {
			myRaf = new RandomAccessFile(myFile, "r");
			headerLen = skipHeader(myRaf);
		}
		firstByteIndex = myLogic.getByteIndex(firstRecordIndex);
	}

	/**
	 * The file contained in this FileDatabase
	 */
	private final File myFile;

	private final RandomAccessFile myRaf;

	private final long headerLen;

	private final long firstByteIndex;

	private long lastByteIndex = -1L;

	@Override
	protected synchronized int readBytes(DatabaseHandle dh, long location,
			byte[] array, int off, int len) throws IOException {
		if (lastByteIndex != location) {
			myRaf.seek(location - firstByteIndex + headerLen);
		}
		int bytesRead = myRaf.read(array, off, len);
		lastByteIndex = location + bytesRead;
		return bytesRead;
	}

	@Override
	protected synchronized int writeBytes(DatabaseHandle dh, long location,
			byte[] array, int off, int len) throws IOException {
		if (lastByteIndex != location) {
			myRaf.seek(location - firstByteIndex + headerLen);
		}
		myRaf.write(array, off, len);
		lastByteIndex = location + len;
		return len;
	}

	@Override
	public synchronized void close() throws IOException {
		myRaf.close();
	}
}

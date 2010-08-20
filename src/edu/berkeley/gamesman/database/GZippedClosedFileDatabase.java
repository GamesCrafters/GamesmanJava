package edu.berkeley.gamesman.database;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;

/**
 * A GZipFileDatabase which closes all file handles when not in use.
 * 
 * @author dnspies
 */
public class GZippedClosedFileDatabase extends GZippedDatabase {
	private int handlesOpen = 0;
	private long tableOffset = -1;

	/**
	 * The default constructor
	 * 
	 * @param uri
	 *            The name of the file
	 * @param conf
	 *            The configuration object
	 * @param solve
	 *            Should always be false
	 * @param firstRecord
	 *            The index of the first record contained in this database
	 * @param numRecords
	 *            The number of records contained in this database
	 * @param header
	 *            The header
	 * @throws IOException
	 *             If opening the file or reading the header throws an
	 *             IOException
	 */
	public GZippedClosedFileDatabase(String uri, Configuration conf,
			boolean solve, long firstRecord, long numRecords,
			DatabaseHeader header) throws IOException {
		super(uri, conf, solve, firstRecord, numRecords, header);
	}

	@Override
	protected synchronized void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		handlesOpen++;
		if (fis == null)
			try {
				fis = new FileInputStream(myFile);
			} catch (FileNotFoundException e) {
				throw new Error(e);
			}
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
	}

	@Override
	protected synchronized int getBytes(DatabaseHandle dh, byte[] arr, int off,
			int maxLen, boolean overwriteEdgesOk) {
		int result = super.getBytes(dh, arr, off, maxLen, overwriteEdgesOk);
		if (overwriteEdgesOk && dh.location == dh.lastByteIndex) {
			handlesOpen--;
			if (handlesOpen <= 0) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				fis = null;
			}
		}
		return result;
	}

	@Override
	public void close() {
	}

	@Override
	protected synchronized long[] getEntryPoints(long firstEntry, int numEntries) {
		long[] entryPoints = new long[numEntries + 1];
		byte[] entryBytes = new byte[(numEntries + 1) << 3];
		try {
			if (tableOffset < 0) {
				fis.getChannel().position(0);
				skipHeader(fis);
				tableOffset = fis.getChannel().position();
			}
			fis.getChannel().position(
					tableOffset + ((firstEntry - this.firstEntry) << 3));
			fis.read(entryBytes);
		} catch (IOException ioe) {
			throw new Error(ioe);
		}
		int c = 0;
		for (int i = 0; i < entryPoints.length; i++) {
			for (int s = 0; s < 8; s++) {
				entryPoints[i] <<= 8;
				entryPoints[i] |= entryBytes[c++] & 255;
			}
		}
		return entryPoints;
	}
}

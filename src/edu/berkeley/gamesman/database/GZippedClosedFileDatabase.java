package edu.berkeley.gamesman.database;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;

public class GZippedClosedFileDatabase extends GZippedFileDatabase {
	private int handlesOpen = 0;

	public GZippedClosedFileDatabase(String uri, Configuration conf,
			boolean solve, long firstRecord, long numRecords,
			DatabaseHeader header) throws IOException {
		super(uri, conf, solve, firstRecord, numRecords, header);
		fis.close();
	}

	@Override
	protected synchronized void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		handlesOpen++;
		try {
			if (fis == null)
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
}

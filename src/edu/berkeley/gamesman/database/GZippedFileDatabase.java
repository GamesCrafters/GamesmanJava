package edu.berkeley.gamesman.database;

import java.io.*;
import java.util.zip.GZIPInputStream;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.util.Util;

/**
 * For reading only
 * 
 * @author dnspies
 */
public class GZippedFileDatabase extends Database {
	private File myFile;

	private InputStream myStream;

	private long currentPos;

	private int confLength;

	@Override
	public void close() {
		if (myStream != null)
			try {
				myStream.close();
			} catch (IOException e) {
				Util.fatalError("IO Error", e);
			}
	}

	@Override
	public void flush() {
		throw new RuntimeException("GZippedFileDatabase is Read Only");
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		int bytesRead = 0;
		while (bytesRead < len)
			try {
				bytesRead += myStream.read(arr, off + bytesRead, len
						- bytesRead);
			} catch (IOException e) {
				Util.fatalError("IO Error", e);
			}
		currentPos += bytesRead;
	}

	@Override
	public void initialize(String location) {
		myFile = new File(location);
		try {
			InputStream fis = new FileInputStream(myFile);
			confLength = 0;
			for (int i = 24; i >= 0; i -= 8) {
				confLength <<= 8;
				confLength |= fis.read();
			}
			byte[] b = new byte[confLength];
			fis.read(b);
			if (conf == null)
				conf = Configuration.load(b);
			if (conf.getProperty("gamesman.db.compression", "none").equals(
					"gzip"))
				fis = new GZIPInputStream(fis);
			maxBytes = (int) getByteSize();
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		} catch (ClassNotFoundException e) {
			Util.fatalError("Class Not Found", e);
		}
	}

	public byte[][] getByteSets(long[] byteOffsets, int lengths) {
		byte[][] byteSets = new byte[byteOffsets.length][lengths];
		InputStream fis;
		try {
			fis = new FileInputStream(myFile);
			int toSkip = confLength + 4;
			while (toSkip > 0)
				toSkip -= fis.skip(toSkip);
			fis = new GZIPInputStream(fis);
			long atByte = 0;
			for (int i = 0; i < byteOffsets.length; i++) {
				if (atByte > byteOffsets[i]) {
					for (int n = 0; n < lengths; n++)
						byteSets[i][n] = byteSets[i - 1][n];
					continue;
				}
				while (atByte < byteOffsets[i])
					atByte += fis.skip(byteOffsets[i] - atByte);
				int lengthRead = 0;
				while (lengthRead < lengths)
					lengthRead += fis.read(byteSets[i]);
				atByte += lengthRead;
			}
			fis.close();
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
		return byteSets;
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void seek(long loc) {
		try {
			if (myStream == null || currentPos > loc) {
				if (myStream != null)
					myStream.close();
				myStream = new FileInputStream(myFile);
				int toSkip = confLength + 4;
				while (toSkip > 0)
					toSkip -= myStream.skip(toSkip);
				currentPos = 0L;
				myStream = new GZIPInputStream(myStream);
			}
			while (currentPos < loc)
				currentPos += myStream.skip(loc - currentPos);
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}
}

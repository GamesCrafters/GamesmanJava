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

	private FileInputStream fis;

	private GZIPInputStream myStream;

	private long currentPos;

	private long[] entryPoints;

	private long entrySize;

	private int bufferSize;

	@Override
	public void close() {
		try {
			if (myStream == null)
				fis.close();
			else
				myStream.close();
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	public void flush() {
		Util.fatalError("GZippedFileDatabase is Read Only");
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		int bytesRead = 0;
		int nextEntry = (int) (currentPos / entrySize) + 1;
		do {
			int sLen = (int) Math.min(len, nextEntry * entrySize - currentPos);
			while (bytesRead < sLen)
				try {
					bytesRead += myStream.read(arr, off + bytesRead, len
							- bytesRead);
				} catch (IOException e) {
					Util.fatalError("IO Error", e);
				}
			currentPos += bytesRead;
			off += bytesRead;
			len -= bytesRead;
			if (len > 0)
				seek(currentPos);
		} while (len > 0);
	}

	@Override
	public void initialize(String location) {
		myFile = new File(location);
		try {
			fis = new FileInputStream(myFile);
			int confLength = 0;
			for (int i = 24; i >= 0; i -= 8) {
				confLength <<= 8;
				confLength |= fis.read();
			}
			byte[] b = new byte[confLength];
			fis.read(b);
			if (conf == null)
				conf = Configuration.load(b);
			entrySize = conf.getLong("zip.entryKB", 0L) << 10;
			bufferSize = conf.getInteger("zip.bufferKB", 1 << 12) << 10;
			if (entrySize > 0L) {
				int numEntries = (int) ((conf.getGame().numHashes() + entrySize - 1) / entrySize);
				entryPoints = new long[numEntries];
				for (int i = 0; i < numEntries; i++) {
					for (int bit = 56; bit >= 0; bit -= 8) {
						entryPoints[i] <<= 8;
						entryPoints[i] |= fis.read();
					}
				}
			} else {
				entrySize = getByteSize();
				entryPoints = new long[1];
				entryPoints[0] = confLength + 4;
			}
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		} catch (ClassNotFoundException e) {
			Util.fatalError("Class Not Found", e);
		}
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void seek(long loc) {
		try {
			int entryNum = (int) (loc / entrySize);
			fis.getChannel().position(entryPoints[entryNum]);
			currentPos = loc - loc % entrySize;
			myStream = new GZIPInputStream(fis, bufferSize);
			while (currentPos < loc)
				currentPos += myStream.skip(loc - currentPos);
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}
}

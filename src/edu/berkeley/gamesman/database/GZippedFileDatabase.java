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

	private int confLength;

	@Override
	public void close() {
	}

	@Override
	public void flush() {
		throw new RuntimeException("GZippedFileDatabase is Read Only");
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	public void getBytes(long loc, byte[] arr, int off, int len) {
		InputStream fis;
		try {
			fis = new FileInputStream(myFile);
			int toSkip = confLength + 4;
			while (toSkip > 0)
				toSkip -= fis.skip(toSkip);
			fis = new GZIPInputStream(fis);
			while (loc > 0)
				loc -= fis.skip(loc);
			fis.read(arr, off, len);
			fis.close();
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
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

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void seek(long loc) {
		throw new UnsupportedOperationException();
	}
}

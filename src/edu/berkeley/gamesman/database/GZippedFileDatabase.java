package edu.berkeley.gamesman.database;

import java.io.*;
import java.util.zip.GZIPInputStream;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.database.util.RemoteDatabaseFile;
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

	private long[] entryPoints;

	private long entrySize;

	private int bufferSize;

	private boolean isRemote;

	private RemoteDatabaseFile rdf;

	private long filePos;

	@Override
	public void close() {
		if (isRemote)
			rdf.close();
		else {
			try {
				if (myStream == null)
					fis.close();
				else
					myStream.close();
			} catch (IOException e) {
				Util.fatalError("IO Error", e);
			}
		}
	}

	@Override
	public void flush() {
		Util.fatalError("GZippedFileDatabase is Read Only");
	}

	@Override
	public void getBytes(long loc, byte[] arr, int off, int len) {
		while (len > 0) {
			long filePos = 0;
			try {
				int entryNum = (int) (loc / entrySize);
				if (isRemote) {
					filePos = entryPoints[entryNum];
					int readInsurance = Math.max(
							(int) ((len + loc % entrySize) * 2), bufferSize);
					byte[] readFrom = new byte[readInsurance];
					rdf.getBytes(filePos, readFrom, 0, readInsurance);
					myStream = new GZIPInputStream(new ByteArrayInputStream(
							readFrom), bufferSize);
				} else {
					fis.getChannel().position(entryPoints[entryNum]);
					myStream = new GZIPInputStream(fis, bufferSize);
				}
				long currentPos = loc - loc % entrySize;
				while (currentPos < loc)
					currentPos += myStream.skip(loc - currentPos);
			} catch (IOException e) {
				Util.fatalError("IO Error", e);
			}
			int bytesRead = 0;
			int nextEntry = (int) (loc / entrySize) + 1;
			int sLen = (int) Math.min(len, nextEntry * entrySize - loc);
			while (bytesRead < sLen)
				try {
					bytesRead += myStream.read(arr, off + bytesRead, len
							- bytesRead);
				} catch (IOException e) {
					Util.fatalError("IO Error", e);
				}
			loc += bytesRead;
			off += bytesRead;
			len -= bytesRead;
		}
	}

	@Override
	public void initialize(String location) {
		try {
			if (location.contains(":")) {
				isRemote = true;
				rdf = new RemoteDatabaseFile();
				rdf.initialize(location);
			} else {
				isRemote = false;
				myFile = new File(location);
				fis = new FileInputStream(myFile);
			}
			int confLength = 0;
			int count = 0;
			byte[] confLengthArr = null;
			if (isRemote) {
				confLengthArr = new byte[4];
				rdf.getBytes(0, confLengthArr, 0, 4);
			}
			for (int i = 24; i >= 0; i -= 8) {
				confLength <<= 8;
				if (isRemote)
					confLength |= confLengthArr[count++];
				else
					confLength |= fis.read();
			}
			byte[] b = new byte[confLength];
			if (isRemote)
				rdf.getBytes(4, b, 0, confLength);
			else
				fis.read(b);
			if (conf == null)
				conf = Configuration.load(b);
			entrySize = conf.getLong("zip.entryKB", 0L) << 10;
			bufferSize = conf.getInteger("zip.bufferKB", 1 << 12) << 10;
			if (entrySize > 0L) {
				int numEntries = (int) ((conf.getGame().numHashes() + entrySize - 1) / entrySize);
				entryPoints = new long[numEntries];
				byte[] entryBytes = new byte[numEntries << 3];
				if (isRemote)
					rdf.getBytes(confLength + 4, entryBytes, 0,
							entryBytes.length);
				else {
					int bytesRead = 0;
					while (bytesRead < entryBytes.length)
						bytesRead += fis.read(entryBytes, bytesRead,
								entryBytes.length - bytesRead);
				}
				count = 0;
				for (int i = 0; i < numEntries; i++) {
					for (int bit = 56; bit >= 0; bit -= 8) {
						entryPoints[i] <<= 8;
						entryPoints[i] |= ((int) entryBytes[count++]) & 255;
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
	public void getBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void seek(long loc) {
		throw new UnsupportedOperationException();
	}
}

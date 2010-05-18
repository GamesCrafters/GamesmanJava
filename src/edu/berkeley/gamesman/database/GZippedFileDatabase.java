package edu.berkeley.gamesman.database;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.gamesman.core.Configuration;
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
	public synchronized void getBytes(DatabaseHandle dh, long loc, byte[] arr,
			int off, int len) {
		while (len > 0) {
			int entryNum = (int) (loc / entrySize);
			try {
				fis.getChannel().position(entryPoints[entryNum]);
				myStream = new GZIPInputStream(fis, bufferSize);
				long currentPos = loc - loc % entrySize;
				while (currentPos < loc)
					currentPos += myStream.skip(loc - currentPos);
			} catch (IOException e) {
				Util.fatalError("IO Error", e);
			}
			int bytesRead = 0;
			int nextEntry = entryNum + 1;
			int sLen = (int) Math.min(len, nextEntry * entrySize - loc);
			while (bytesRead < sLen)
				try {
					bytesRead += myStream.read(arr, off + bytesRead, sLen
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
	public void initialize(String location, boolean solve) {
		try {
			myFile = new File(location);
			fis = new FileInputStream(myFile);
			conf = Configuration.load(fis);
			long numBytes = 0;
			for (int i = 56; i >= 0; i -= 8) {
				numBytes <<= 8;
				numBytes |= fis.read();
			}
			if (numBytes < getByteSize())
				setRange(firstByte(), numBytes);
			entrySize = conf.getLong("zip.entryKB", 1 << 6) << 10;
			bufferSize = conf.getInteger("zip.bufferKB", 1 << 6) << 10;
			int numEntries = (int) ((numBytes + entrySize - 1) / entrySize);
			entryPoints = new long[numEntries];
			byte[] entryBytes = new byte[numEntries << 3];
			int bytesRead = 0;
			while (bytesRead < entryBytes.length) {
				int fisRead = fis.read(entryBytes, bytesRead, entryBytes.length
						- bytesRead);
				if (fisRead == -1)
					break;
				bytesRead += fisRead;
			}
			int count = 0;
			for (int i = 0; i < numEntries; i++) {
				for (int bit = 56; bit >= 0; bit -= 8) {
					entryPoints[i] <<= 8;
					entryPoints[i] |= ((int) entryBytes[count++]) & 255;
				}
			}
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		} catch (ClassNotFoundException e) {
			Util.fatalError("Class Not Found", e);
		}
	}

	/**
	 * Creates a new GZippedFileDatabase from an existing FileDatabase
	 * 
	 * @param readFrom
	 *            A FileDatabase containing the records to be GZipped
	 * @param writeTo
	 *            The file to write the GZipped database to
	 * @param storeConf
	 *            Should the configuration be stored as well?
	 * @param entrySize
	 *            The size of each GZipped portion of the database
	 * @param bufferSize
	 *            The buffer size for the GZippedOutputStream
	 * @param writeProgress
	 *            An output stream to write progress to
	 * @param totalBytes
	 *            The total number of bytes to store (If -1, store all
	 *            available)
	 * @param firstByte
	 *            The first byte to store
	 */
	public static void createFromFile(Database readFrom, File writeTo,
			boolean storeConf, long entrySize, int bufferSize,
			PrintStream writeProgress, long firstByte, long totalBytes) {
		bufferSize = (int) Math.min(bufferSize, entrySize);
		if (writeTo.exists())
			writeTo.delete();
		try {
			writeTo.createNewFile();
			FileOutputStream fos = new FileOutputStream(writeTo);
			if (storeConf) {
				Configuration conf = readFrom.getConfiguration();
				Configuration outConf = conf.cloneAll();
				outConf.setProperty("gamesman.database", "GZippedFileDatabase");
				outConf.setProperty("gamesman.db.uri", writeTo.getPath());
				outConf.setProperty("zip.entryKB", Long
						.toString(entrySize >> 10));
				outConf.setProperty("zip.bufferKB", Integer
						.toString(bufferSize >> 10));
				outConf.store(fos);
			} else {
				Configuration.storeNone(fos);
			}
			if (totalBytes < 0) {
				firstByte = readFrom.firstByte();
				totalBytes = readFrom.getByteSize();
			}
			long numBytes = totalBytes;
			for (int bit = 56; bit >= 0; bit -= 8)
				fos.write((int) (numBytes >>> bit));
			GZIPOutputStream gos;
			byte[] tempArray = new byte[bufferSize];
			int numPosits = (int) ((numBytes + entrySize - 1) / entrySize);
			long[] posits = new long[numPosits];
			long tablePos = fos.getChannel().position();
			long count = 0;
			fos.getChannel().position(tablePos + (numPosits << 3));
			long byteNum = firstByte;
			readFrom.seek(byteNum);
			long div = 0;
			for (int i = 0; i < numPosits; i++) {
				long pos = fos.getChannel().position();
				posits[i] = pos;
				gos = new GZIPOutputStream(fos);
				long tot = Math.min((i + 1) * entrySize, numBytes);
				long lastDiv = div;
				while (count < tot) {
					int bytes;
					if (bufferSize < tot - count) {
						bytes = tempArray.length;
					} else {
						bytes = (int) (tot - count);
					}
					readFrom.getBytes(tempArray, 0, bytes);
					byteNum += bytes;
					gos.write(tempArray, 0, bytes);
					count += bytes;
				}
				if (writeProgress != null) {
					div = count / 1000000;
					if (div > lastDiv) {
						writeProgress.println(count * 100F / numBytes
								+ "% done");
					}
				}
				gos.finish();
			}
			fos.getChannel().position(tablePos);
			for (int i = 0; i < numPosits; i++) {
				for (int bit = 56; bit >= 0; bit -= 8)
					fos.write((int) (posits[i] >> bit));
			}
			fos.close();
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		}
	}

	@Override
	public void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}
}

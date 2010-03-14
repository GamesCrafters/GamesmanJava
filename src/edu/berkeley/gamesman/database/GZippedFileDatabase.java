package edu.berkeley.gamesman.database;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

	private long[] entryPoints;

	private long entrySize;

	private int bufferSize;

	private boolean isRemote;

	private RemoteDatabase rdf;

	private long numBytes;

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
					int endEntry = (int) ((loc + len + entrySize - 1) / entrySize);
					int readBytes;
					if (endEntry >= entryPoints.length) {
						readBytes = (int) (rdf.fileSize() - filePos);
					} else
						readBytes = (int) (entryPoints[endEntry] - filePos);
					byte[] readFrom = new byte[readBytes];
					synchronized (rdf) {
						rdf.seekInFile(filePos);
						rdf.getBytes(readFrom, 0, readBytes);
					}
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
	public void initialize(String location, boolean solve) {
		try {
			if (location.contains(":")) {
				isRemote = true;
				rdf = new RemoteDatabase();
				rdf.initialize(location, conf, false);
			} else {
				isRemote = false;
				myFile = new File(location);
				fis = new FileInputStream(myFile);
			}
			int confLength = 0;
			if (isRemote) {
				if (conf == null)
					conf = rdf.getConfiguration();
			} else {
				for (int i = 24; i >= 0; i -= 8) {
					confLength <<= 8;
					confLength |= fis.read();
				}
				byte[] b = new byte[confLength];
				fis.read(b);
				if (conf == null)
					conf = Configuration.load(b);
			}
			numBytes = 0;
			if (isRemote) {
				byte[] numBytesBytes = new byte[8];
				rdf.getBytes(0, numBytesBytes, 0, 8);
				for (int i = 0; i < 8; i++) {
					numBytes <<= 8;
					numBytes |= (numBytesBytes[i] & 255);
				}
			} else {
				for (int i = 56; i >= 0; i -= 8) {
					numBytes <<= 8;
					numBytes |= fis.read();
				}
			}
			entrySize = conf.getLong("zip.entryKB", 1 << 6) << 10;
			bufferSize = conf.getInteger("zip.bufferKB", 1 << 6) << 10;
			if (entrySize > 0L) {
				int numEntries = (int) ((getByteSize() + entrySize - 1) / entrySize);
				entryPoints = new long[numEntries];
				byte[] entryBytes = new byte[numEntries << 3];
				if (isRemote)
					rdf.getBytes(8, entryBytes, 0, entryBytes.length);
				else {
					int bytesRead = 0;
					while (bytesRead < entryBytes.length) {
						int numBytes = fis.read(entryBytes, bytesRead,
								entryBytes.length - bytesRead);
						if (numBytes == -1)
							break;
						bytesRead += numBytes;
					}
				}
				int count = 0;
				for (int i = 0; i < numEntries; i++) {
					for (int bit = 56; bit >= 0; bit -= 8) {
						entryPoints[i] <<= 8;
						entryPoints[i] |= ((int) entryBytes[count++]) & 255;
					}
				}
			} else {
				entrySize = getByteSize();
				entryPoints = new long[1];
				if (isRemote)
					entryPoints[0] = 0;
				else
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

	@Override
	public long getByteSize() {
		return numBytes;
	}

	/**
	 * Creates a new GZippedFileDatabase from an existing FileDatabase. This is
	 * equivalent to createFromFile(readFrom, writeTo, storeConf, 1 << 16)
	 * 
	 * @param readFrom
	 *            A FileDatabase containing the records to be GZipped
	 * @param writeTo
	 *            The file to write the GZipped database to
	 * @param storeConf
	 *            Should the configuration be stored as well?
	 */
	public static void createFromFile(FileDatabase readFrom, File writeTo,
			boolean storeConf) {
		createFromFile(readFrom, writeTo, storeConf, 1 << 16);
	}

	/**
	 * Creates a new GZippedFileDatabase from an existing FileDatabase. This is
	 * equivalent to createFromFile(readFrom, writeTo, storeConf, entrySize,
	 * 1<<16)
	 * 
	 * @param readFrom
	 *            A FileDatabase containing the records to be GZipped
	 * @param writeTo
	 *            The file to write the GZipped database to
	 * @param storeConf
	 *            Should the configuration be stored as well?
	 * @param entrySize
	 *            The size of each GZipped portion of the database
	 */
	public static void createFromFile(FileDatabase readFrom, File writeTo,
			boolean storeConf, long entrySize) {
		createFromFile(readFrom, writeTo, storeConf, entrySize, 1 << 16);
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
	 */
	public static void createFromFile(FileDatabase readFrom, File writeTo,
			boolean storeConf, long entrySize, int bufferSize) {
		bufferSize = (int) Math.min(bufferSize, entrySize);
		if (writeTo.exists())
			writeTo.delete();
		try {
			writeTo.createNewFile();
			FileInputStream fis = new FileInputStream(readFrom.myFile);
			FileOutputStream fos = new FileOutputStream(writeTo);
			int confLength = 0;
			for (int i = 24; i >= 0; i -= 8) {
				confLength <<= 8;
				confLength |= fis.read();
			}
			byte[] confArray = null;
			if (confLength > 0) {
				confArray = new byte[confLength];
				fis.read(confArray);
			} else
				storeConf = false;
			if (storeConf) {
				Configuration conf = Configuration.load(confArray);
				conf.setProperty("gamesman.database", "GZippedFileDatabase");
				conf.setProperty("gamesman.db.uri", writeTo.getPath());
				conf.setProperty("zip.entryKB", Long.toString(entrySize >> 10));
				conf.setProperty("zip.bufferKB", Integer
						.toString(bufferSize >> 10));
				confArray = conf.store();
				for (int i = 24; i >= 0; i -= 8)
					fos.write(confArray.length >> i);
				fos.write(confArray);
			} else {
				for (int i = 0; i < 4; i++)
					fos.write(0);
			}
			long numBytes = readFrom.getByteSize();
			for (int bit = 56; bit >= 0; bit -= 8)
				fos.write((int) (numBytes >>> bit));
			GZIPOutputStream gos;
			byte[] tempArray = new byte[bufferSize];
			if (entrySize == 0) {
				gos = new GZIPOutputStream(fos, bufferSize);
				long count = 0;
				while (count < numBytes) {
					int bytes = fis.read(tempArray);
					if (bytes < 0) {
						gos.write(tempArray, 0, (int) (numBytes - count));
						break;
					} else
						gos.write(tempArray, 0, bytes);
					count += bytes;
				}
				fis.close();
				gos.close();
			} else {
				int numPosits = (int) ((numBytes + entrySize - 1) / entrySize);
				long[] posits = new long[numPosits];
				long pos = fos.getChannel().position();
				long count = 0;
				fos.getChannel().position(pos + (numPosits << 3));
				/*
				 * The parentheses were in the wrong place when I solved 6x6 and
				 * 7x5. Now the databases contain a few extra empty bytes
				 */
				for (int i = 0; i < numPosits; i++) {
					pos = fos.getChannel().position();
					posits[i] = pos;
					gos = new GZIPOutputStream(fos);
					long tot = (i + 1) * entrySize;
					while (count < tot) {
						int bytes;
						if (bufferSize < tot - count)
							bytes = fis.read(tempArray);
						else
							bytes = fis.read(tempArray, 0, (int) (tot - count));
						if (bytes < 0) {
							if (numBytes > count)
								gos.write(tempArray, 0,
										(int) (numBytes - count));
							count = numBytes;
							break;
						} else
							gos.write(tempArray, 0, bytes);
						count += bytes;
					}
					gos.finish();
				}
				if (storeConf)
					fos.getChannel().position(confArray.length + 12);
				else
					fos.getChannel().position(12);
				for (int i = 0; i < numPosits; i++) {
					for (int bit = 56; bit >= 0; bit -= 8)
						fos.write((int) (posits[i] >> bit));
				}
				fos.close();
			}
		} catch (IOException e) {
			Util.fatalError("IO Error", e);
		} catch (ClassNotFoundException e) {
			Util.fatalError("This shouldn't happen", e);
		}
	}
}

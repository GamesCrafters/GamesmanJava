package edu.berkeley.gamesman.database;

import java.io.*;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Semaphore;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.ChunkInputStream;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

/**
 * A file database whose records are GZipped in chunks. Each chunk contains
 * gamesman.db.zip.entryKB (default 64) raw kilobytes of records (record groups
 * may be split across chunk boundaries). The first numEntries*8 bytes after the
 * header is a list of offsets into the file at which each chunk begins. This is
 * loaded into an array when the database is opened and held in memory until it
 * is garbage collected.
 * 
 * @author dnspies
 */
public class GZippedFileDatabase extends GZippedDatabase implements Runnable {

	// Writing only

	/**
	 * This constructor is used when converting a database of another type
	 * (usually a FileDatabase) into a GZippedFileDatabase
	 * 
	 * @param uri
	 *            The name of the database file to be created
	 * @param conf
	 *            The configuration object
	 * @param readFrom
	 *            The database to read input from
	 * @param maxMem
	 *            The maximum amount of memory available to be used while
	 *            GZipping
	 * @throws IOException
	 *             If there's an IOException creating the file or storing the
	 *             header
	 */
	public GZippedFileDatabase(String uri, final Configuration conf,
			final Database readFrom, long maxMem) throws IOException {
		super(uri, conf, true, readFrom.firstRecord(), readFrom.numRecords(),
				readFrom.getHeader());
		gzipWorst = entrySize + ((entrySize + ((1 << 15) - 1)) >> 15) * 5 + 18;
		// As I understand, this is worst-case performance for gzip
		handleEntry = thisEntry = firstEntry;
		lastByteIndex = lastByte(firstContainedRecord + numContainedRecords);
		long lastEntry = (lastByteIndex + entrySize - 1) / entrySize;
		numEntries = (int) (lastEntry - firstEntry);
		entryPoints = new long[numEntries + 1];
		fis = null;
		waitingCaches = new QuickLinkedList<Pair<ByteArrayOutputStream, Long>>();
		waitingCachesIter = waitingCaches.listIterator();
		fos = new FileOutputStream(myFile);
		store(fos, uri);
		tableOffset = fos.getChannel().position();
		fos.getChannel().position(tableOffset + (entryPoints.length << 3));
		zippedStoragePool = new Pool<ByteArrayOutputStream>(
				new Factory<ByteArrayOutputStream>() {

					public ByteArrayOutputStream newObject() {
						return new ByteArrayOutputStream(gzipWorst);
					}

					public void reset(ByteArrayOutputStream t) {
						t.reset();
					}
				});
		memoryConstraint = new Semaphore((int) (maxMem / entrySize));
		handlePool = new Pool<WriteHandle>(new Factory<WriteHandle>() {

			public WriteHandle newObject() {
				return new WriteHandle();
			}

			public void reset(WriteHandle t) {
				t.zippedStorage = null;
			}

		});
		this.readFrom = readFrom;
		nextStart = firstByteIndex;
		writeBuffer = null;
	}

	private final QuickLinkedList<Pair<ByteArrayOutputStream, Long>> waitingCaches;
	private final QuickLinkedList<Pair<ByteArrayOutputStream, Long>>.QLLIterator waitingCachesIter;
	private final long tableOffset;
	private final Pool<ByteArrayOutputStream> zippedStoragePool;
	private final Pool<WriteHandle> handlePool;
	private final Semaphore memoryConstraint;
	private final Database readFrom;
	private final FileOutputStream fos;
	private final int numEntries;
	private long thisEntry;
	private long handleEntry;
	private long nextStart;
	private final byte[] writeBuffer;

	@Override
	public void close() {
		if (solve) {
			byte[] entryBytes = new byte[entryPoints.length << 3];
			int count = 0;
			for (int entry = 0; entry < entryPoints.length; entry++) {
				if (entry > 0
						&& entryPoints[entry] - entryPoints[entry - 1] == 0) {
					throw new Error(new EOFException("No bytes in block "
							+ entry + "/" + numEntries + " ("
							+ (entry + firstEntry) + " total)"));
				}
				for (int i = 56; i >= 0; i -= 8)
					entryBytes[count++] = (byte) (entryPoints[entry] >>> i);
			}
			try {
				fos.getChannel().position(tableOffset);
				fos.write(entryBytes);
				fos.close();
			} catch (IOException e) {
				throw new Error(e);
			}
		} else {
			try {
				fis.close();
			} catch (IOException e) {
				throw new Error(e);
			}
		}
	}

	private class WriteHandle extends DatabaseHandle {
		public final byte[] myStorage;
		public ByteArrayOutputStream zippedStorage;
		public long entry;
		private DatabaseHandle myHandle;
		public int numBytes;

		WriteHandle() {
			super(null);
			myStorage = new byte[entrySize];
			myHandle = readFrom.getHandle();
		}

		// TODO Combine all calls into the prepareRange, getBytes paradigm
		void setRange(ByteArrayOutputStream baos, long entry, long firstByte,
				int numBytes) {
			this.numBytes = numBytes;
			this.zippedStorage = baos;
			this.entry = entry;
			if (entry == firstEntry) {
				if (numEntries == 1) {
					readFrom.getRecordsAsBytes(myHandle, firstByte,
							toNum(firstRecord()), myStorage, 0, numBytes,
							toNum(firstRecord() + numRecords()), true);
				} else
					readFrom.getRecordsAsBytes(myHandle, firstByte,
							toNum(firstRecord()), myStorage, 0, numBytes, 0,
							true);
			} else if (entry == firstEntry + numEntries - 1) {
				readFrom.getRecordsAsBytes(myHandle, firstByte, 0, myStorage,
						0, numBytes, toNum(firstRecord() + numRecords()), true);
			} else
				readFrom.getBytes(myHandle, firstByte, myStorage, 0, numBytes);
		}
	}

	public WriteHandle getNextHandle() {
		if (handleEntry == firstEntry + numEntries)
			return null;
		try {
			memoryConstraint.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		WriteHandle retVal;
		long firstByteIndex, lastByteIndex;
		long entry;
		ByteArrayOutputStream baos;
		synchronized (this) {
			if (handleEntry == firstEntry + numEntries) {
				memoryConstraint.release();
				return null;
			}
			entry = handleEntry++;
			firstByteIndex = nextStart;
			if (handleEntry == firstEntry + numEntries)
				nextStart = this.lastByteIndex;
			else if (entry == firstEntry)
				nextStart = handleEntry * entrySize;
			else
				nextStart += entrySize;
			lastByteIndex = nextStart;
			retVal = handlePool.get();
			baos = zippedStoragePool.get();
		}
		retVal.setRange(baos, entry, firstByteIndex,
				(int) (lastByteIndex - firstByteIndex));
		return retVal;
	}

	@Override
	public void closeHandle(DatabaseHandle dh) {
		if (dh instanceof WriteHandle) {
			WriteHandle wh = (WriteHandle) dh;
			Pair<ByteArrayOutputStream, Long> thisCache = new Pair<ByteArrayOutputStream, Long>(
					wh.zippedStorage, wh.entry);
			handlePool.release(wh);
			synchronized (this) {
				if (thisEntry != thisCache.cdr) {
					waitingCachesIter.toIndex(0);
					boolean foundPoint = false;
					while (waitingCachesIter.hasNext()) {
						Pair<ByteArrayOutputStream, Long> testCache = waitingCachesIter
								.next();
						if (thisCache.cdr < testCache.cdr) {
							foundPoint = true;
							break;
						}
					}
					if (foundPoint)
						waitingCachesIter.previous();
					waitingCachesIter.add(thisCache);
					return;
				}
			}
			while (true) {
				if ((thisEntry - firstEntry) % 1000 == 0) {
					System.out.println("Starting entry "
							+ (thisEntry - firstEntry) + "/" + numEntries);
				}
				try {
					entryPoints[(int) (thisEntry - firstEntry)] = fos
							.getChannel().position();
					if (thisCache.car.size() == 0)
						throw new RuntimeException("Zipped size cannot be zero");
					thisCache.car.writeTo(fos);
					synchronized (this) {
						thisEntry++;
						zippedStoragePool.release(thisCache.car);
						memoryConstraint.release();
						if (waitingCaches.isEmpty()) {
							if (thisEntry - firstEntry == numEntries) {
								entryPoints[numEntries] = fos.getChannel()
										.position();
							}
							break;
						}
						thisCache = waitingCaches.getFirst();
						if (thisEntry != thisCache.cdr)
							break;
						waitingCaches.removeFirst();
					}
				} catch (IOException e) {
					throw new Error(e);
				}
			}
		} else
			super.closeHandle(dh);
	}

	public void run() {
		WriteHandle wh = getNextHandle();
		while (wh != null) {
			try {
				GZIPOutputStream gzo = new GZIPOutputStream(wh.zippedStorage,
						entrySize);
				gzo.write(wh.myStorage, 0, wh.numBytes);
				gzo.finish();
			} catch (IOException e) {
				throw new Error(e);
			}
			closeHandle(wh);
			wh = getNextHandle();
		}
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		long time = System.currentTimeMillis();
		String db1 = args[0];
		String zipDb = args[1];
		int entryKB;
		int numThreads;
		if (args.length > 2)
			numThreads = Integer.parseInt(args[2]);
		else
			numThreads = 1;
		if (args.length > 3)
			entryKB = Integer.parseInt(args[3]);
		else
			entryKB = 64;
		long maxMem;
		if (args.length > 4)
			maxMem = ((long) Integer.parseInt(args[4])) << 10;
		else
			maxMem = 1 << 25;
		Database readFrom = Database.openDatabase(db1);
		Configuration outConf = readFrom.getConfiguration().cloneAll();
		outConf.setProperty("gamesman.database", GZippedFileDatabase.class
				.getName());
		outConf.setProperty("gamesman.db.uri", zipDb);
		outConf.setProperty("gamesman.db.zip.entryKB", Integer
				.toString(entryKB));
		GZippedFileDatabase writeTo = new GZippedFileDatabase(zipDb, outConf,
				readFrom, maxMem);
		Thread[] threadList = new Thread[numThreads];
		DatabaseHandle[] readHandle = new DatabaseHandle[numThreads];
		for (int i = 0; i < numThreads; i++) {
			readHandle[i] = readFrom.getHandle();
			threadList[i] = new Thread(writeTo);
			threadList[i].start();
		}
		for (int i = 0; i < numThreads; i++) {
			while (threadList[i].isAlive())
				try {
					threadList[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		readFrom.close();
		writeTo.close();
		System.out.println("Zipped in "
				+ Util.millisToETA(System.currentTimeMillis() - time));
	}

	// For reading and writing

	public GZippedFileDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords, DatabaseHeader header)
			throws IOException {
		super(uri, conf, solve, firstRecord, numRecords, header);
		gzipWorst = entrySize + ((entrySize + ((1 << 15) - 1)) >> 15) * 5 + 18;
		lastByteIndex = lastByte(firstContainedRecord + numContainedRecords);
		long lastEntry = (lastByteIndex + entrySize - 1) / entrySize;
		numEntries = (int) (lastEntry - firstEntry);
		entryPoints = new long[numEntries + 1];
		fis = new FileInputStream(myFile);
		skipHeader(fis);
		tableOffset = fis.getChannel().position();
		byte[] entryBytes = new byte[entryPoints.length << 3];
		readFully(fis, entryBytes, 0, entryBytes.length);
		int count = 0;
		for (int i = 0; i < entryPoints.length; i++) {
			for (int bit = 56; bit >= 0; bit -= 8) {
				entryPoints[i] <<= 8;
				entryPoints[i] |= ((int) entryBytes[count++]) & 255;
			}
			if (i > 0 && entryPoints[i] - entryPoints[i - 1] == 0)
				throw new EOFException("No bytes in block " + i + "/"
						+ numEntries + " (" + (i + firstEntry) + " total)");
		}
		zippedStoragePool = null;
		waitingCachesIter = null;
		waitingCaches = null;
		readFrom = null;
		memoryConstraint = null;
		handlePool = null;
		fos = null;
		writeBuffer = null;
	}

	public GZippedFileDatabase(String uri, Configuration conf,
			DatabaseHeader header) throws IOException {
		super(uri, conf, true, header.firstRecord, header.numRecords, header);
		gzipWorst = entrySize + ((entrySize + ((1 << 15) - 1)) >> 15) * 5 + 18;
		// As I understand, this is worst-case performance for gzip
		thisEntry = firstEntry;
		lastByteIndex = lastByte(firstContainedRecord + numContainedRecords);
		long lastEntry = (lastByteIndex + entrySize - 1) / entrySize;
		numEntries = (int) (lastEntry - firstEntry);
		entryPoints = new long[numEntries + 1];
		fis = null;
		waitingCaches = null;
		waitingCachesIter = null;
		fos = new FileOutputStream(myFile);
		store(fos, uri);
		tableOffset = fos.getChannel().position();
		fos.getChannel().position(tableOffset + (entryPoints.length << 3));
		zippedStoragePool = null;
		memoryConstraint = null;
		handlePool = null;
		readFrom = null;
		nextStart = firstByteIndex;
		writeBuffer = new byte[gzipWorst];
	}

	private final long[] entryPoints;

	private final int gzipWorst;

	private final long lastByteIndex;

	// For remote work

	protected long prepareZippedRange(DatabaseHandle dh, long byteIndex,
			long numBytes) {
		long startEntry = byteIndex / entrySize;
		long endEntry = (byteIndex + numBytes + entrySize - 1) / entrySize;
		GZipHandle gzh = (GZipHandle) dh;
		lastUsed = gzh;
		try {
			fis.getChannel().position(
					entryPoints[(int) (startEntry - firstEntry)]);
		} catch (IOException e) {
			throw new Error(e);
		}
		gzh.cwis = new ChunkWrapInputStream(fis, entryPoints,
				(int) (startEntry - firstEntry));
		gzh.remainingBytes = entryPoints[(int) (endEntry - firstEntry)]
				- entryPoints[(int) (startEntry - firstEntry)]
				+ ((endEntry - startEntry) << 2);
		return gzh.remainingBytes;
	}

	public int extraBytes(long firstByte) {
		if (firstByte < (firstEntry + 1) * entrySize) {
			return (int) (firstByte - firstByteIndex);
		} else {
			return (int) (firstByte % entrySize);
		}
	}

	public int getZippedBytes(DatabaseHandle dh, byte[] arr, int off, int maxLen) {
		GZipHandle gzh = (GZipHandle) dh;
		if (lastUsed != gzh)
			throw new ConcurrentModificationException();
		final int numBytes = (int) Math.min(maxLen, gzh.remainingBytes);
		try {
			readFully(gzh.cwis, arr, off, numBytes);
		} catch (IOException e) {
			throw new Error(e);
		}
		gzh.remainingBytes -= numBytes;
		return numBytes;
	}

	public void putZippedBytes(ChunkInputStream is, long numBytes)
			throws IOException {
		if (entryPoints[(int) (thisEntry - firstEntry)] == 0)
			entryPoints[(int) (thisEntry - firstEntry)] = fos.getChannel()
					.position();
		while (numBytes != 0) {
			int bytesRead;
			if (numBytes < 0)
				bytesRead = is.read(writeBuffer);
			else
				bytesRead = is.read(writeBuffer, 0, (int) Math.min(numBytes,
						writeBuffer.length));
			if (bytesRead < 0) {
				is.nextChunk();
				entryPoints[(int) ((++thisEntry) - firstEntry)] = fos
						.getChannel().position();
				if (numBytes < 0)
					bytesRead = is.read(writeBuffer);
				else
					bytesRead = is.read(writeBuffer, 0, (int) Math.min(
							numBytes, writeBuffer.length));
				if (bytesRead < 0)
					break;
			}
			fos.write(writeBuffer, 0, bytesRead);
			if (numBytes > 0)
				numBytes -= bytesRead;
		}
		System.out.println("Entries put: " + (thisEntry - firstEntry));
	}

	public void putZippedBytes(ChunkInputStream is) throws IOException {
		putZippedBytes(is, -1);
	}

	public long prepareMoveRange(DatabaseHandle dh, long firstRecord,
			long numRecords, DistributedDatabase allRecords) {
		byte[] zippedInitialBytes, zippedFinalBytes;
		long firstTransferByte = firstByteIndex - firstByteIndex % entrySize;
		long firstTransferRecord = toFirstRecord(firstTransferByte);
		boolean rezipStart = false;
		if (firstTransferRecord < firstRecord) {
			firstTransferRecord = firstRecord;
			long newFirstByte = toByte(firstRecord);
			if (newFirstByte > firstTransferByte) {
				firstTransferByte = newFirstByte;
				if (firstTransferByte % entrySize > 0)
					rezipStart = true;
			}
		}
		long lastTransferByte = lastByteIndex - lastByteIndex % entrySize;
		long lastTransferRecord = toLastRecord(lastTransferByte);
		boolean rezipEnd = false;
		if (lastTransferRecord > firstRecord + numRecords) {
			lastTransferRecord = firstRecord + numRecords;
			long newLastByte = lastByte(lastTransferRecord);
			if (newLastByte < lastTransferByte) {
				lastTransferByte = newLastByte;
				if (lastTransferByte % entrySize > 0)
					rezipEnd = true;
			}
		} else if (firstRecord + numRecords <= firstRecord() + numRecords()) {
			lastTransferRecord = firstRecord + numRecords;
			lastTransferByte = lastByte(lastTransferRecord);
			if (lastTransferByte < lastByteIndex)
				rezipEnd = true;
		}
		if (firstTransferByte >= lastTransferByte)
			return 0L;
		if (rezipStart || firstTransferRecord < firstRecord()) {
			long firstReadByte = toByte(firstTransferRecord);
			long lastUseByte = firstTransferByte + entrySize
					- firstTransferByte % entrySize;
			long lastReadRecord = toLastRecord(lastUseByte);
			if (lastTransferRecord < lastReadRecord) {
				lastReadRecord = lastTransferRecord;
				lastUseByte = Math.min(lastUseByte, lastByte(lastReadRecord));
				rezipEnd = false;
			}
			long lastReadByte = lastByte(lastReadRecord);
			byte[] initialBytes = new byte[(int) (lastReadByte - firstReadByte)];
			if (rezipStart) {
				getRecordsAsBytes(dh, firstReadByte,
						toNum(firstTransferRecord), initialBytes, 0,
						(int) (lastReadByte - firstReadByte),
						toNum(lastReadRecord), true);
			} else if (lastReadRecord > firstRecord()) {
				DatabaseHandle allHandle = allRecords.getHandle();
				int firstRecordNum = toNum(firstRecord());
				allRecords.getRecordsAsBytes(allHandle, firstReadByte,
						toNum(firstTransferRecord), initialBytes, 0,
						(int) (lastByte(firstRecord()) - firstReadByte),
						firstRecordNum, true);
				getRecordsAsBytes(dh, firstByteIndex, firstRecordNum,
						initialBytes, (int) (firstByteIndex - firstReadByte),
						(int) (lastReadByte - firstByteIndex),
						toNum(lastReadRecord), false);
			} else {
				new Exception("This shouldn't happen").printStackTrace();
				DatabaseHandle allHandle = allRecords.getHandle();
				allRecords.getRecordsAsBytes(allHandle, firstReadByte,
						toNum(firstTransferRecord), initialBytes, 0,
						(int) (lastReadByte - firstReadByte),
						toNum(lastReadRecord), true);
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream(gzipWorst);
			try {
				GZIPOutputStream gzo = new GZIPOutputStream(baos, entrySize);
				gzo.write(initialBytes,
						(int) (firstTransferByte - firstReadByte),
						(int) (lastUseByte - firstTransferByte));
				gzo.close();
				zippedInitialBytes = baos.toByteArray();
			} catch (IOException e) {
				throw new Error(e);
			}
		} else
			zippedInitialBytes = null;
		if (rezipEnd) {
			long firstUseByte = lastTransferByte - lastTransferByte % entrySize;
			long firstEndRecord = toFirstRecord(firstUseByte);
			if (firstTransferRecord > firstEndRecord) {
				firstEndRecord = firstTransferRecord;
				firstUseByte = Math.max(firstUseByte, toByte(firstEndRecord));
			}
			long firstEndByte = toByte(firstEndRecord);
			long lastEndByte = lastByte(lastTransferRecord);
			byte[] finalBytes = new byte[(int) (lastEndByte - firstEndByte)];
			getRecordsAsBytes(dh, firstEndByte, toNum(firstEndRecord),
					finalBytes, 0, (int) (lastEndByte - firstEndByte),
					toNum(lastTransferRecord), true);
			ByteArrayOutputStream baos = new ByteArrayOutputStream(gzipWorst);
			try {
				GZIPOutputStream gzo = new GZIPOutputStream(baos, entrySize);
				gzo.write(finalBytes, (int) (firstUseByte - firstEndByte),
						(int) (lastTransferByte - firstUseByte));
				gzo.close();
				zippedFinalBytes = baos.toByteArray();
			} catch (IOException e) {
				throw new Error(e);
			}
		} else
			zippedFinalBytes = null;
		long firstTransferEntry = firstTransferByte / entrySize;
		long lastTransferEntry = (lastTransferByte + entrySize - 1) / entrySize;
		long[] myPoints = new long[(int) (lastTransferEntry - firstTransferEntry) + 1];
		System
				.arraycopy(entryPoints,
						(int) (firstTransferEntry - firstEntry), myPoints, 0,
						(int) (lastTransferEntry - firstTransferEntry) + 1);
		try {
			if (zippedInitialBytes != null) {
				myPoints[0] = myPoints[1] - zippedInitialBytes.length;
				fis.getChannel().position(myPoints[1]);
			} else
				fis.getChannel().position(myPoints[0]);
		} catch (IOException e) {
			throw new Error(e);
		}
		if (zippedFinalBytes != null) {
			myPoints[myPoints.length - 1] = myPoints[myPoints.length - 2]
					+ zippedFinalBytes.length;
		}
		long totalBytes = myPoints[myPoints.length - 1] - myPoints[0];
		MoveInputStream moveStream = new MoveInputStream(zippedInitialBytes,
				fis, zippedFinalBytes, totalBytes);
		GZipHandle gzh = (GZipHandle) dh;
		lastUsed = gzh;
		gzh.cwis = new ChunkWrapInputStream(moveStream, myPoints, 0);
		gzh.remainingBytes = totalBytes
				+ ((lastTransferEntry - firstTransferEntry) << 2);
		return gzh.remainingBytes;
	}

	@Override
	protected long[] getEntryPoints(long firstEntry, int numEntries) {
		long[] res = new long[numEntries + 1];
		System.arraycopy(entryPoints, (int) (firstEntry - this.firstEntry),
				res, 0, numEntries + 1);
		return res;
	}
}

final class MoveInputStream extends InputStream {
	final byte[] initialBytes, finalBytes;
	final InputStream innerStream;
	int inRead = 0, finRead = 0;
	long innerLen;

	public MoveInputStream(byte[] initialBytes, InputStream innerStream,
			byte[] finalBytes, long totalBytes) {
		this.initialBytes = initialBytes;
		this.finalBytes = finalBytes;
		this.innerStream = innerStream;
		this.innerLen = totalBytes
				- ((initialBytes == null ? 0 : initialBytes.length) + (finalBytes == null ? 0
						: finalBytes.length));
	}

	@Override
	public int read(byte[] arr, int off, int len) throws IOException {
		if (initialBytes != null && inRead < initialBytes.length) {
			int bytesToRead = Math.min(len, initialBytes.length - inRead);
			System.arraycopy(initialBytes, inRead, arr, off, bytesToRead);
			inRead += bytesToRead;
			return bytesToRead;
		} else if (innerLen > 0) {
			int bytesToRead = innerStream.read(arr, off, (int) Math.min(len,
					innerLen));
			if (bytesToRead < 0)
				throw new EOFException();
			innerLen -= bytesToRead;
			return bytesToRead;
		} else if (finalBytes != null && finRead < finalBytes.length) {
			int bytesToRead = Math.min(len, finalBytes.length - finRead);
			System.arraycopy(finalBytes, finRead, arr, off, bytesToRead);
			finRead += bytesToRead;
			return bytesToRead;
		} else
			return -1;
	}

	@Override
	public int read() throws IOException {
		if (initialBytes != null && inRead < initialBytes.length) {
			return initialBytes[inRead++];
		} else if (innerLen > 0) {
			int byteRead = innerStream.read();
			if (byteRead < 0)
				throw new EOFException();
			innerLen--;
			return byteRead;
		} else if (finalBytes != null && finRead < finalBytes.length) {
			return finalBytes[finRead++];
		} else
			return -1;
	}

	@Override
	public long skip(long n) throws IOException {
		if (initialBytes != null && inRead < initialBytes.length) {
			int bytesToSkip = (int) Math.min(n, initialBytes.length - inRead);
			inRead += bytesToSkip;
			return bytesToSkip;
		} else if (innerLen > 0) {
			long bytesToSkip = innerStream.skip(Math.min(n, innerLen));
			if (bytesToSkip < 0)
				throw new EOFException();
			innerLen -= bytesToSkip;
			return bytesToSkip;
		} else if (finalBytes != null && finRead < finalBytes.length) {
			int bytesToSkip = (int) Math.min(n, finalBytes.length - finRead);
			finRead += bytesToSkip;
			return bytesToSkip;
		} else
			return -1;
	}
}

package edu.berkeley.gamesman.database;

import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

/**
 * For reading only
 * 
 * @author dnspies
 */
public class GZippedFileDatabase extends Database implements Runnable {
	public GZippedFileDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords) throws IOException {
		super(uri, conf, solve, firstRecord, numRecords);
		myFile = new File(uri);
		int confEntrySize = conf.getInteger("zip.entryKB", 64) << 10;
		entrySize = confEntrySize - confEntrySize % recordGroupByteLength;
		// A necessary evil. Try thinking about what happens if this wasn't
		// here.
		bufferSize = conf.getInteger("zip.bufferKB", 4) << 10;
		firstEntry = handleEntry = thisEntry = toByte(firstContainedRecord)
				/ entrySize;
		long lastEntry = (lastByte(firstContainedRecord + numContainedRecords)
				+ entrySize - 1)
				/ entrySize;
		numEntries = (int) (lastEntry - firstEntry);
		entryPoints = new long[numEntries];
		fos = null;
		fis = new FileInputStream(myFile);
		skipHeader(fis);
		tableOffset = fis.getChannel().position();
		int bytesRead = 0;
		byte[] entryBytes = new byte[numEntries << 3];
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
		waitingCaches = null;
		waitingCachesIter = null;
		zippedStoragePool = null;
		memoryConstraint = null;
		handlePool = null;
	}

	public GZippedFileDatabase(String uri, final Configuration conf,
			final Database readFrom, boolean storeConf, long maxMem)
			throws IOException {
		super(uri, conf, true, readFrom.firstRecord(), readFrom.numRecords(),
				readFrom);
		myFile = new File(uri);
		int confEntrySize = conf.getInteger("zip.entryKB", 64) << 10;
		entrySize = confEntrySize - confEntrySize % recordGroupByteLength;
		// A necessary evil. Try thinking about what happens if this wasn't
		// here.
		bufferSize = conf.getInteger("zip.bufferKB", 4) << 10;
		firstEntry = handleEntry = thisEntry = toByte(firstContainedRecord)
				/ entrySize;
		long lastEntry = (lastByte(firstContainedRecord + numContainedRecords)
				+ entrySize - 1)
				/ entrySize;
		numEntries = (int) (lastEntry - firstEntry);
		entryPoints = new long[numEntries];
		fis = null;
		waitingCaches = new QuickLinkedList<Pair<ByteArrayOutputStream, Long>>();
		waitingCachesIter = waitingCaches.listIterator();
		fos = new FileOutputStream(myFile);
		if (storeConf)
			store(fos);
		else
			storeNone(fos);
		tableOffset = fos.getChannel().position();
		fos.getChannel().position(tableOffset + (numEntries << 3));
		final GZippedFileDatabase gzfd = this;
		zippedStoragePool = new Pool<ByteArrayOutputStream>(
				new Factory<ByteArrayOutputStream>() {

					public ByteArrayOutputStream newObject() {
						return new ByteArrayOutputStream(entrySize
								+ ((entrySize + ((1 << 15) - 1)) >> 15) * 5
								+ 18);
						// As I understand, this is worst-case performance for
						// gzip
					}

					public void reset(ByteArrayOutputStream t) {
						t.reset();
					}
				});
		memoryConstraint = new Semaphore((int) (maxMem / entrySize));
		handlePool = new Pool<WriteHandle>(new Factory<WriteHandle>() {

			public WriteHandle newObject() {
				return new WriteHandle(readFrom);
			}

			public void reset(WriteHandle t) {
				t.zippedStorage = null;
			}

		});
	}

	private final File myFile;

	private final FileInputStream fis;

	private final FileOutputStream fos;

	private GZIPInputStream myStream;

	private final long[] entryPoints;

	private final int entrySize;

	private final int bufferSize;

	private long thisEntry;

	private long handleEntry;

	private final long firstEntry;

	private final int numEntries;

	private final QuickLinkedList<Pair<ByteArrayOutputStream, Long>> waitingCaches;

	private final QuickLinkedList<Pair<ByteArrayOutputStream, Long>>.QLLIterator waitingCachesIter;

	private boolean hasNextHandle = true;

	private final long tableOffset;

	private final Pool<ByteArrayOutputStream> zippedStoragePool;

	private final Pool<WriteHandle> handlePool;

	private final Semaphore memoryConstraint;

	@Override
	protected void closeDatabase() {
		if (solve) {
			byte[] entryBytes = new byte[numEntries << 3];
			int count = 0;
			for (int entry = 0; entry < numEntries; entry++) {
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
				if (myStream == null)
					fis.close();
				else
					myStream.close();
			} catch (IOException e) {
				throw new Error(e);
			}
		}
	}

	@Override
	protected synchronized void getBytes(DatabaseHandle dh, long loc,
			byte[] arr, int off, int len) {
		if (solve)
			throw new UnsupportedOperationException();
		while (len > 0) {
			long entryNum = loc / entrySize;
			try {
				fis.getChannel().position(
						entryPoints[(int) (entryNum - firstEntry)]);
				myStream = new GZIPInputStream(fis, bufferSize);
				long currentPos = loc - loc % entrySize;
				while (currentPos < loc)
					currentPos += myStream.skip(loc - currentPos);
			} catch (IOException e) {
				throw new Error(e);
			}
			int bytesRead = 0;
			long nextEntry = entryNum + 1;
			int sLen = (int) Math.min(len, nextEntry * entrySize - loc);
			while (bytesRead < sLen)
				try {
					bytesRead += myStream.read(arr, off + bytesRead, sLen
							- bytesRead);
				} catch (IOException e) {
					throw new Error(e);
				}
			loc += bytesRead;
			off += bytesRead;
			len -= bytesRead;
		}
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		WriteHandle wh = (WriteHandle) dh;
		wh.myStorage.putBytes(dh, loc, arr, off, len);
	}

	@Override
	public DatabaseHandle getHandle(long firstRecord, long numRecords) {
		return super.getHandle(firstRecord, numRecords);
	}

	@Override
	public DatabaseHandle getHandle() {
		return super.getHandle();
	}

	private class WriteHandle extends DatabaseHandle {
		private final MemoryDatabase myStorage;
		private ByteArrayOutputStream zippedStorage;
		public long entry;

		WriteHandle(Database readFrom) {
			super(null);
			myStorage = new MemoryDatabase(readFrom, null, conf, false, 0, 0,
					true);
		}

		void setRange(ByteArrayOutputStream baos, long firstRecord,
				int numRecords, long entry) {
			this.zippedStorage = baos;
			myStorage.setRange(firstRecord, numRecords);
			this.entry = entry;
		}
	}

	public WriteHandle getNextHandle() {
		if (!hasNextHandle)
			return null;
		try {
			memoryConstraint.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		WriteHandle retVal;
		long firstRecord, lastRecord;
		long entry;
		synchronized (this) {
			if (!hasNextHandle) {
				memoryConstraint.release();
				return null;
			}
			entry = handleEntry;
			firstRecord = toFirstRecord((handleEntry++) * entrySize);
			retVal = handlePool.get();
		}
		lastRecord = toFirstRecord(handleEntry * entrySize);
		if (lastRecord >= firstRecord() + numRecords()) {
			lastRecord = firstRecord() + numRecords();
			hasNextHandle = false;
		}
		retVal.setRange(zippedStoragePool.get(), firstRecord,
				(int) (lastRecord - firstRecord), entry);
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
				if (thisEntry > 0 && thisEntry % 100 == 0) {
					System.out.println("Starting entry " + thisEntry + "/"
							+ numEntries);
				}
				try {
					entryPoints[(int) ((thisEntry++) - firstEntry)] = fos
							.getChannel().position();
					thisCache.car.writeTo(fos);
				} catch (IOException e) {
					throw new Error(e);
				}
				synchronized (this) {
					zippedStoragePool.release(thisCache.car);
					memoryConstraint.release();
					if (waitingCaches.isEmpty())
						break;
					thisCache = waitingCaches.getFirst();
					if (thisEntry != thisCache.cdr)
						break;
					waitingCaches.removeFirst();
				}
			}
		} else
			super.closeHandle(dh);
	}

	public void run() {
		WriteHandle wh = getNextHandle();
		while (wh != null) {
			long firstByte = toByte(wh.myStorage.firstRecord());
			long lastByte = lastByte(wh.myStorage.firstRecord()
					+ wh.myStorage.numRecords());
			int numBytes = (int) (lastByte - firstByte);
			try {
				GZIPOutputStream gzo = new GZIPOutputStream(wh.zippedStorage,
						bufferSize);
				gzo.write(wh.myStorage.memoryStorage, 0, numBytes);
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
		int entryKB, bufferKB;
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
		if (args.length > 5)
			bufferKB = Integer.parseInt(args[5]);
		else
			bufferKB = 4;
		Database readFrom = Database.openDatabase(db1, false);
		Configuration outConf = readFrom.getConfiguration().cloneAll();
		outConf.setProperty("gamesman.database", "GZippedFileDatabase");
		outConf.setProperty("gamesman.db.uri", zipDb);
		outConf.setProperty("zip.entryKB", Integer.toString(entryKB));
		outConf.setProperty("zip.bufferKB", Integer.toString(bufferKB));
		GZippedFileDatabase writeTo = new GZippedFileDatabase(zipDb, outConf,
				readFrom, true, maxMem);
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
		writeTo.close();
		System.out.println("Zipped in "
				+ Util.millisToETA(System.currentTimeMillis() - time));
	}
}

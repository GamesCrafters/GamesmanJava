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
			long firstRecord, long numRecords, DatabaseHeader header)
			throws IOException {
		super(uri, conf, solve, firstRecord, numRecords, header);
		myFile = new File(uri);
		entrySize = conf.getInteger("zip.entryKB", 64) << 10;
		bufferSize = conf.getInteger("zip.bufferKB", 4) << 10;
		final long firstByteIndex = toByte(firstContainedRecord);
		firstEntry = handleEntry = thisEntry = firstByteIndex / entrySize;
		lastByteIndex = lastByte(firstContainedRecord + numContainedRecords);
		long lastEntry = (lastByteIndex + entrySize - 1) / entrySize;
		numEntries = (int) (lastEntry - firstEntry);
		entryPoints = new long[numEntries];
		fos = null;
		fis = new FileInputStream(myFile);
		skipHeader(fis);
		tableOffset = fis.getChannel().position();
		byte[] entryBytes = new byte[numEntries << 3];
		readFully(fis, entryBytes, 0, entryBytes.length);
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
		readFrom = null;
		nextStart = firstByteIndex;
	}

	public GZippedFileDatabase(String uri, final Configuration conf,
			final Database readFrom, long maxMem) throws IOException {
		super(uri, conf, true, readFrom.firstRecord(), readFrom.numRecords(),
				readFrom.getHeader());
		myFile = new File(uri);
		entrySize = conf.getInteger("zip.entryKB", 64) << 10;
		bufferSize = conf.getInteger("zip.bufferKB", 4) << 10;
		final long firstByteIndex = toByte(firstContainedRecord);
		firstEntry = handleEntry = thisEntry = firstByteIndex / entrySize;
		lastByteIndex = lastByte(firstContainedRecord + numContainedRecords);
		long lastEntry = (lastByteIndex + entrySize - 1) / entrySize;
		numEntries = (int) (lastEntry - firstEntry);
		entryPoints = new long[numEntries];
		fis = null;
		waitingCaches = new QuickLinkedList<Pair<ByteArrayOutputStream, Long>>();
		waitingCachesIter = waitingCaches.listIterator();
		fos = new FileOutputStream(myFile);
		store(fos, uri);
		tableOffset = fos.getChannel().position();
		fos.getChannel().position(tableOffset + (numEntries << 3));
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
				return new WriteHandle();
			}

			public void reset(WriteHandle t) {
				t.zippedStorage = null;
			}

		});
		this.readFrom = readFrom;
		nextStart = firstByteIndex;
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

	private final long tableOffset;

	private final Pool<ByteArrayOutputStream> zippedStoragePool;

	private final Pool<WriteHandle> handlePool;

	private final Semaphore memoryConstraint;

	private final Database readFrom;

	private long nextStart;

	private final long lastByteIndex;

	private DatabaseHandle lastUsed;

	@Override
	public void close() {
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
		getRecordsAsBytes(dh, loc, 0, arr, off, len, 0, true);
	}

	@Override
	protected synchronized void getRecordsAsBytes(DatabaseHandle dh,
			long byteIndex, int recordNum, byte[] arr, int off, int numBytes,
			int lastNum, boolean overwriteEdgesOk) {
		super.getRecordsAsBytes(dh, byteIndex, recordNum, arr, off, numBytes,
				lastNum, overwriteEdgesOk);
	}

	@Override
	protected synchronized int getBytes(DatabaseHandle dh, byte[] arr, int off,
			int maxLen, boolean overwriteEdgesOk) {
		if (lastUsed != dh) {
			lastUsed = dh;
			seek(dh);
		}
		if (!overwriteEdgesOk)
			return super.getBytes(dh, arr, off, maxLen, false);
		final int numBytes = (int) Math.min(maxLen, dh.lastByteIndex
				- dh.byteIndex);
		int len = numBytes;
		while (len > 0) {
			int bytesToRead = (int) Math.min(len, nextStart - dh.location);
			while (bytesToRead > 0) {
				try {
					int bytesRead = myStream.read(arr, off, bytesToRead);
					bytesToRead -= bytesRead;
					len -= bytesRead;
					dh.location += bytesRead;
					off += bytesRead;
				} catch (IOException e) {
					throw new Error(e);
				}
			}
			if (len > 0) {
				thisEntry++;
				nextStart += entrySize;
				try {
					fis.getChannel().position(
							entryPoints[(int) (thisEntry - firstEntry)]);
					myStream = new GZIPInputStream(fis, bufferSize);
				} catch (IOException e) {
					throw new Error(e);
				}
			}
		}
		return numBytes;
	}

	@Override
	protected synchronized void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
		seek(dh);
	}

	private synchronized void seek(DatabaseHandle dh) {
		lastUsed = dh;
		thisEntry = dh.location / entrySize;
		nextStart = (thisEntry + 1) * entrySize;
		try {
			fis.getChannel().position(
					entryPoints[(int) (thisEntry - firstEntry)]);
			myStream = new GZIPInputStream(fis, bufferSize);
			long curLoc = thisEntry * entrySize;
			while (curLoc < dh.location)
				curLoc += myStream.skip(dh.location - curLoc);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
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
			if (entry == firstEntry)
				nextStart = handleEntry * entrySize;
			else if (handleEntry == firstEntry + numEntries)
				nextStart = this.lastByteIndex;
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
			try {
				GZIPOutputStream gzo = new GZIPOutputStream(wh.zippedStorage,
						bufferSize);
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
		Database readFrom = Database.openDatabase(db1);
		Configuration outConf = readFrom.getConfiguration().cloneAll();
		outConf.setProperty("gamesman.database", "GZippedFileDatabase");
		outConf.setProperty("gamesman.db.uri", zipDb);
		outConf.setProperty("zip.entryKB", Integer.toString(entryKB));
		outConf.setProperty("zip.bufferKB", Integer.toString(bufferKB));
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
			readFrom.closeHandle(readHandle[i]);
		}
		writeTo.close();
		System.out.println("Zipped in "
				+ Util.millisToETA(System.currentTimeMillis() - time));
	}
}

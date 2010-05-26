package edu.berkeley.gamesman.database;

import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

/**
 * For reading only
 * 
 * @author dnspies
 */
public class GZippedFileDatabase extends Database {
	public GZippedFileDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords) throws IOException {
		super(uri, conf, solve, firstRecord, numRecords);
		myFile = new File(uri);
		int entrySize = conf.getInteger("zip.entryKB", 64) << 10;
		this.entrySize = entrySize - entrySize % recordGroupByteLength;
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
		handlePool = null;
		memoryConstraint = null;
	}

	public GZippedFileDatabase(String uri, final Configuration conf,
			Database readFrom, boolean storeConf, long maxMem)
			throws IOException {
		super(uri, conf, true, readFrom.firstRecord(), readFrom.numRecords(),
				readFrom);
		myFile = new File(uri);
		int entrySize = conf.getInteger("zip.entryKB", 64) << 10;
		this.entrySize = entrySize - entrySize % recordGroupByteLength;
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
		waitingCaches = new QuickLinkedList<MemoryDatabase>();
		waitingCachesIter = waitingCaches.listIterator();
		fos = new FileOutputStream(myFile);
		if (storeConf)
			store(fos);
		else
			storeNone(fos);
		tableOffset = fos.getChannel().position();
		fos.getChannel().position(tableOffset + (numEntries << 3));
		seek(toByte(firstRecord()));
		final GZippedFileDatabase gzfd = this;
		handlePool = new Pool<MemoryDatabase>(new Factory<MemoryDatabase>() {

			public MemoryDatabase newObject() {
				return new MemoryDatabase(gzfd, null, conf, true, 0, 0, false);
			}

			public void reset(MemoryDatabase t) {
			}
		});
		memoryConstraint = new Semaphore((int) (maxMem / entrySize));
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

	private final QuickLinkedList<MemoryDatabase> waitingCaches;

	private final QuickLinkedList<MemoryDatabase>.QLLIterator waitingCachesIter;

	private boolean hasNextHandle = true;

	private final long tableOffset;

	private final Pool<MemoryDatabase> handlePool;

	private GZIPOutputStream gzo;

	private final Semaphore memoryConstraint;

	private int threadsHere = 0;

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
	protected void putBytes(byte[] arr, int off, int len) {
		if (!solve)
			throw new UnsupportedOperationException();
		try {
			if (gzo == null) {
				if (thisEntry > 0 && thisEntry % 100 == 0) {
					System.out.println("Starting entry " + thisEntry + "/"
							+ numEntries);
				}
				entryPoints[(int) ((thisEntry++) - firstEntry)] = fos
						.getChannel().position();
				gzo = new GZIPOutputStream(fos, bufferSize);
			}
			gzo.write(arr, off, len);
		} catch (IOException e) {
			throw new Error(e);
		}
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

		WriteHandle(MemoryDatabase storage) {
			super(null);
			myStorage = storage;
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
		synchronized (this) {
			if (!hasNextHandle) {
				memoryConstraint.release();
				return null;
			}
			long firstRecord = toFirstRecord((handleEntry++) * entrySize);
			long lastRecord = toFirstRecord(handleEntry * entrySize);
			if (lastRecord >= firstRecord() + numRecords()) {
				lastRecord = firstRecord() + numRecords();
				hasNextHandle = false;
			}
			WriteHandle retVal = new WriteHandle(handlePool.get());
			retVal.myStorage.setRange(firstRecord,
					(int) (lastRecord - firstRecord));
			return retVal;
		}
	}

	@Override
	public void closeHandle(DatabaseHandle dh) {
		if (dh instanceof WriteHandle) {
			WriteHandle wh = (WriteHandle) dh;
			MemoryDatabase thisCache = wh.myStorage;
			long storByte = toByte(thisCache.firstRecord());
			synchronized (this) {
				if (location != storByte) {
					waitingCachesIter.toIndex(0);
					boolean foundPoint = false;
					while (waitingCachesIter.hasNext()) {
						MemoryDatabase testCache = waitingCachesIter.next();
						if (thisCache.firstRecord() < testCache.firstRecord()) {
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
			memoryConstraint.release();
			while (true) {
				thisCache.seek(storByte);
				long bytesToRead = numBytes(thisCache.firstRecord(), thisCache
						.numRecords());
				long bytesRead = 0;
				int off = 0;
				while (bytesRead < bytesToRead) {
					int numBytes = (int) Math.min(bytesToRead - bytesRead,
							bufferSize);
					putBytes(thisCache.memoryStorage, off, numBytes);
					bytesRead += numBytes;
					off += numBytes;
				}
				handlePool.release(thisCache);
				try {
					gzo.finish();
				} catch (IOException e) {
					throw new Error(e);
				}
				gzo = null;
				synchronized (this) {
					location += bytesRead;
					if (waitingCaches.isEmpty())
						break;
					thisCache = waitingCaches.getFirst();
					storByte = toByte(thisCache.firstRecord());
					if (location != storByte)
						break;
					waitingCaches.removeFirst();
					memoryConstraint.release();
				}
			}
		} else
			super.closeHandle(dh);
	}

	class ZipRunnable implements Runnable {
		Database readFrom;
		DatabaseHandle myHandle;

		public ZipRunnable(Database readFrom, DatabaseHandle readHandle) {
			this.readFrom = readFrom;
			myHandle = readHandle;
		}

		public void run() {
			WriteHandle wh = getNextHandle();
			while (wh != null) {
				long firstByte = toByte(wh.myStorage.firstRecord());
				long lastByte = lastByte(wh.myStorage.firstRecord()
						+ wh.myStorage.numRecords());
				int numBytes = (int) (lastByte - firstByte);
				readFrom.getBytes(myHandle, firstByte,
						wh.myStorage.memoryStorage, 0, numBytes);
				closeHandle(wh);
				wh = getNextHandle();
			}
		}
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
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
			threadList[i] = new Thread(writeTo.getRunnable(readFrom,
					readHandle[i]));
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
	}

	private ZipRunnable getRunnable(Database readFrom, DatabaseHandle readHandle) {
		return new ZipRunnable(readFrom, readHandle);
	}
}

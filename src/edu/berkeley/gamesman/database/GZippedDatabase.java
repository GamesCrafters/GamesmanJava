package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.util.GZippedDatabaseInputStream;
import edu.berkeley.gamesman.database.util.GZippedDatabaseOutputStream;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Progressable;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.ZipChunkInputStream;
import edu.berkeley.gamesman.util.ZipChunkOutputStream;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

/**
 * A GZippedDatabase contains bytes GZipped in chunks. Subclasses need only
 * instantiate the reader and writer arguments to the constructor. These should
 * not already be GZipped, the GZippedDatabase will wrap them appropriately.
 * 
 * @author dnspies
 */
public abstract class GZippedDatabase extends Database {
	private long currentByteIndex;
	private long remaining;
	private final int numEntries;
	private final long entrySize;
	private final long firstByteIndex;
	private final long numBytes;
	private final int tableOffset;
	private final ZipChunkOutputStream zcos;
	private ZipChunkInputStream zcis;
	private final long[] entryTable;
	private final GZippedDatabaseInputStream reader;
	private final GZippedDatabaseOutputStream writer;
	private final Pool<byte[]> bytePool;
	private int currentEntry;
	private long filePos;

	public GZippedDatabase(GZippedDatabaseInputStream reader,
			GZippedDatabaseOutputStream writer, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException {
		super(conf, firstRecordIndex, numRecords, reading, writing);
		this.reader = reader;
		this.writer = writer;
		firstByteIndex = myLogic.getByteIndex(firstRecordIndex);
		numBytes = myLogic.getNumBytes(numRecords);
		entrySize = conf.getNumBytes("entry.bytes", 1 << 16);
		numEntries = (int) (numBytes / entrySize + 1);
		entryTable = new long[numEntries];
		if (writing) {
			remaining = entrySize;
			tableOffset = this.writeHeader(writer);
			assert writer.getFilePointer() == tableOffset;
			for (int i = 0; i < numEntries; i++)
				writer.writeLong(0);
			filePos = tableOffset + 8 * numEntries;
			assert filePos == writer.getFilePointer();
			currentEntry = 0;
			entryTable[0] = filePos;
			zcos = new ZipChunkOutputStream(writer, (int) Math.min(
					Integer.MAX_VALUE, entrySize));
			currentByteIndex = firstByteIndex;
			bytePool = null;
		} else {
			tableOffset = skipHeader(reader);
			for (int i = 0; i < numEntries; i++)
				entryTable[i] = reader.readLong();
			final int bufferSize = (int) Math.min(Integer.MAX_VALUE, entrySize);
			bytePool = new Pool<byte[]>(new Factory<byte[]>() {

				@Override
				public byte[] newObject() {
					return new byte[bufferSize];
				}

				@Override
				public void reset(byte[] t) {
					Arrays.fill(t, (byte) 0);
				}
			});
			zcis = new ZipChunkInputStream(reader, bytePool);
			zcos = null;
			currentByteIndex = -1L;
		}
	}

	@Override
	protected synchronized int readBytes(DatabaseHandle dh, long location,
			byte[] array, int off, int len) throws IOException {
		if (writing)
			return len;
		if (location != currentByteIndex) {
			int readEntry = (int) ((location - firstByteIndex) / entrySize);
			long entryStartByteIndex = firstByteIndex + readEntry * entrySize;
			reader.seek(entryTable[readEntry]);
			if (zcis != null)
				zcis.finish();
			zcis = new ZipChunkInputStream(reader, bytePool);
			Util.skipFully(zcis, location - entryStartByteIndex);
			currentByteIndex = location;
		}
		int bytesRead = zcis.read(array, off, len);
		currentByteIndex += bytesRead;
		return bytesRead;
	}

	@Override
	protected synchronized int writeBytes(DatabaseHandle dh, long location,
			byte[] array, int off, int len) throws IOException {
		if (location != currentByteIndex)
			throw new UnsupportedOperationException(
					"Can only write sequentially");
		int atTime = (int) Math.min(len, remaining);
		zcos.write(array, off, atTime);
		len -= atTime;
		remaining -= atTime;
		currentByteIndex += atTime;
		if (remaining == 0) {
			filePos += zcos.nextChunk();
			zcos.flush();
			assert filePos == writer.getFilePointer();
			entryTable[++currentEntry] = filePos;
			remaining = entrySize;
		}
		return atTime;
	}

	@Override
	public void close() throws IOException {
		if (writing) {
			zcos.finish();
			writer.seek(tableOffset);
			for (int i = 0; i < numEntries; i++) {
				writer.writeLong(entryTable[i]);
			}
			writer.close();
		} else {
			if (zcis != null)
				zcis.close();
			reader.close();
		}
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param readFrom
	 *            The database to read bytes from
	 * @param writeTo
	 *            The new GZippedDatabase to write bytes to
	 * @param synchronizedRead
	 *            If reading blocks should be done synchronized (should be true
	 *            unless the database supports multi-threaded simultaneous
	 *            reading (otherwise this will cause it to jump back and forth
	 *            and slow the zipping process))
	 * @throws IOException
	 *             If an IOException occurs while zipping
	 */
	public static void zip(Configuration conf, Database readFrom,
			GZippedDatabase writeTo, boolean synchronizedRead)
			throws IOException {
		zip(conf, readFrom, writeTo, null, synchronizedRead);
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param readFrom
	 *            The database to read bytes from
	 * @param writeTo
	 *            The new GZippedDatabase to write bytes to
	 * @param progress
	 *            A Progressable for the zip to report progress
	 * @param synchronizedRead
	 *            If reading blocks should be done synchronized (should be true
	 *            unless the database supports multi-threaded simultaneous
	 *            reading (otherwise this will cause it to jump back and forth
	 *            and slow the zipping process))
	 * @throws IOException
	 *             If an IOException occurs while zipping
	 */
	public static void zip(Configuration conf, final Database readFrom,
			final GZippedDatabase writeTo, Progressable progress,
			boolean synchronizedRead) throws IOException {
		Zipper zip = new Zipper(conf, readFrom, writeTo, progress,
				synchronizedRead);
		zip.run();
	}

	private static class Zipper {
		private final Pool<ZipChunkOutputStream> chunkerPool = new Pool<ZipChunkOutputStream>(
				new Factory<ZipChunkOutputStream>() {
					@Override
					public ZipChunkOutputStream newObject() {
						try {
							return new ZipChunkOutputStream(writeTo.writer,
									bytePool, false);
						} catch (IOException e) {
							throw new Error(e);
						}
					}

					@Override
					public void reset(ZipChunkOutputStream t) {
					}
				});
		private final Pool<byte[]> bytePool = new Pool<byte[]>(
				new Factory<byte[]>() {

					@Override
					public byte[] newObject() {
						return new byte[entrySize];
					}

					@Override
					public void reset(byte[] t) {
					}

				});
		private final Pool<DatabaseHandle> handlePool = new Pool<DatabaseHandle>(
				new Factory<DatabaseHandle>() {

					@Override
					public DatabaseHandle newObject() {
						return readFrom.getHandle(true);
					}

					@Override
					public void reset(DatabaseHandle t) {
					}

				});

		private class ZipRunner implements Runnable {
			private final int j;

			private ZipRunner(int j) {
				this.j = j;
			}

			@Override
			public void run() {
				try {
					if (j > 0)
						memoryAcquired[j - 1].await();
					memoryChunks.acquire(3);
					memoryAcquired[j].countDown();
					/*
					 * If I were to instead acquire the permits separately
					 * (acquire, read, acquire, write), then I could have
					 * deadlock if all threads use up the permits reading and no
					 * one is able to write
					 */
					long thisByte = j * (long) entrySize;
					int len;
					if (j == writeTo.numEntries - 1) {
						len = (int) (numBytes - thisByte);
					} else {
						len = entrySize;
					}
					long byteIndex = firstByteIndex + thisByte;
					byte[] entryBytes = bytePool.get();
					try {
						if (synchronizedRead) {
							synchronizedReadBytes(len, byteIndex, entryBytes);
						} else {
							readBytes(len, byteIndex, entryBytes);
						}
						ZipChunkOutputStream chunker = chunkerPool.get();
						chunker.startChunk();
						chunker.write(entryBytes, 0, len);
						chunker.finishChunk();
						streams[j] = chunker;
					} catch (IOException e) {
						throw new Error(e);
					}
					bytesZipped += len;
					if (bytesZipped / STEP_SIZE > lastProgressPoint) {
						zipProgress();
					}
					threadsFinished[j].countDown();
					bytePool.release(entryBytes);
					memoryChunks.release();
				} catch (Throwable t) {
					if (failed == null) {
						failed = t;
						mainThread.interrupt();
					}
				}
			}
		}

		private static final int STEP_SIZE = 100000000;
		private final int entrySize;
		private volatile long bytesZipped = 0L;
		private final long firstByteIndex, numBytes;
		private final Semaphore memoryChunks;
		private final int nThreads;
		private final CountDownLatch[] threadsFinished;
		private final CountDownLatch[] memoryAcquired;
		private final ZipChunkOutputStream[] streams;
		private final Database readFrom;
		private final GZippedDatabase writeTo;
		private final Progressable progress;
		private final boolean synchronizedRead;
		private volatile long lastProgressPoint = 0;
		private volatile Throwable failed = null;
		private Thread mainThread;

		public Zipper(Configuration conf, final Database readFrom,
				final GZippedDatabase writeTo, Progressable progress,
				boolean synchronizedRead) {
			if (writeTo.entrySize > Integer.MAX_VALUE)
				throw new Error("Entry size is too large to fit in int");
			entrySize = (int) writeTo.entrySize;
			firstByteIndex = readFrom.firstByteIndex();
			numBytes = readFrom.numBytes();
			nThreads = conf.getInteger("gamesman.threads", 1);
			long availableMem = conf.getNumBytes("gamesman.memory", 1L << 25);
			int permits = (int) Math.min(Integer.MAX_VALUE,
					Math.max(3, availableMem / entrySize));
			memoryChunks = new Semaphore(permits);
			// No deadlock because newFixedThreadPool is a queue
			memoryAcquired = new CountDownLatch[writeTo.numEntries];
			threadsFinished = new CountDownLatch[writeTo.numEntries];
			streams = new ZipChunkOutputStream[writeTo.numEntries];
			this.readFrom = readFrom;
			this.writeTo = writeTo;
			this.progress = progress;
			this.synchronizedRead = synchronizedRead;
		}

		private void zipProgress() {
			lastProgressPoint = bytesZipped / STEP_SIZE;
			if (progress != null) {
				synchronized (this) {
					progress.progress();
				}
			}
			Util.debug(DebugFacility.DATABASE, bytesZipped * 10000 / numBytes
					/ 100F + "% finished zipping");
		}

		private void writeProgress(long bytesWritten) {
			if (progress != null) {
				synchronized (this) {
					progress.progress();
				}
			}
			Util.debug(DebugFacility.DATABASE, bytesWritten * 10000 / numBytes
					/ 100F + "% finished writing zipped bytes");
		}

		public void run() throws IOException {
			System.out.println("Started zipping");
			long startTime = System.currentTimeMillis();
			mainThread = Thread.currentThread();
			ExecutorService zipperService = Executors
					.newFixedThreadPool(nThreads);
			for (int i = 0; i < writeTo.numEntries; i++) {
				memoryAcquired[i] = new CountDownLatch(1);
				threadsFinished[i] = new CountDownLatch(1);
				zipperService.submit(new ZipRunner(i));
				if (failed != null) {
					throwError(failed);
				}
			}
			zipperService.shutdown();
			long bytesWritten = 0L;
			long lastStep = 0;
			for (int i = 0; i < writeTo.numEntries; i++) {
				writeTo.entryTable[i] = writeTo.writer.getFilePointer();
				try {
					threadsFinished[i].await();
				} catch (InterruptedException e) {
					if (failed == null)
						failed = e;
				}
				if (failed != null)
					throwError(failed);
				streams[i].finishAndWriteChunk();
				chunkerPool.release(streams[i]);
				memoryChunks.release(2);
				if (i < writeTo.numEntries - 1) {
					bytesWritten += entrySize;
					if (bytesWritten / STEP_SIZE > lastStep) {
						lastStep = bytesWritten / STEP_SIZE;
						writeProgress(bytesWritten);
					}
				}
			}
			if (failed != null)
				throwError(failed);
			System.out.println("Zipped in "
					+ Util.millisToETA(System.currentTimeMillis() - startTime));
		}

		private void readBytes(int len, long byteIndex, byte[] entryBytes)
				throws IOException {
			DatabaseHandle readDh = handlePool.get();
			readFrom.readFullBytes(readDh, byteIndex, entryBytes, 0, len);
			handlePool.release(readDh);
		}

		private synchronized void synchronizedReadBytes(int len,
				long byteIndex, byte[] entryBytes) throws IOException {
			readBytes(len, byteIndex, entryBytes);
		}

		private void throwError(Throwable failed) throws IOException {
			if (failed instanceof Error) {
				throw (Error) failed;
			} else if (failed instanceof RuntimeException) {
				throw (RuntimeException) failed;
			} else if (failed instanceof IOException) {
				throw (IOException) failed;
			} else
				throw new Error(failed);
		}
	}

}

package edu.berkeley.gamesman.solver;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.cache.RecordRangeCache;
import edu.berkeley.gamesman.database.cache.VICache;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

/**
 * @author dnspies
 * 
 */
public class CacheVISolver extends VISolver {
	private final long memPerThread;
	private final long readMem, writeMem;
	private final long writeRecords;

	private final Pool<RecordRangeCache> rangeCaches = new Pool<RecordRangeCache>(
			new Factory<RecordRangeCache>() {

				@Override
				public RecordRangeCache newObject() {
					return new RecordRangeCache(db);
				}

				@Override
				public void reset(RecordRangeCache t) {
				}
			});

	/**
	 * @param conf
	 * @param db
	 * @param firstHash
	 * @param numHashes
	 */
	public CacheVISolver(Configuration conf, Database db, long firstHash,
			long numHashes) {
		this(conf, db, firstHash, numHashes, false);
	}

	/**
	 * @param conf
	 * @param db
	 */
	public CacheVISolver(Configuration conf, Database db) {
		this(conf, db, 0, conf.getGame().numHashes(), true);
	}

	public CacheVISolver(Configuration conf, Database db, long firstHash,
			long numHashes, boolean withRepeats) {
		super(conf, db, firstHash, numHashes, withRepeats);
		memPerThread = conf.getNumBytes("gamesman.memory", 1L << 25) / nThreads;
		readMem = memPerThread / 2;
		writeMem = memPerThread / 2;
		writeRecords = db.recordsForBytes(memPerThread / 2);
	}

	/**
	 * @author dnspies
	 * 
	 */
	protected class CacheVISolveTask extends VISolveTask {
		private int[] whichChildren;
		private VICache readCache;
		private RecordRangeCache writeCache;

		/**
		 * @param firstIndex
		 * @param numIndices
		 */
		CacheVISolveTask(long firstIndex, int numIndices) {
			super(firstIndex, numIndices);
		}

		@Override
		protected int fetchChildren() {
			int numMoves = myGame.validMoves(childHashes, whichChildren);
			readCache.fetchChildren(hash, numMoves, childHashes, whichChildren,
					childRecords);
			for (int i = 0; i < numMoves; i++) {
				try {
					myGame.longToRecord(
							db.readRecord(readHandle, childHashes[i]),
							childRecords[i]);
					childRecords[i].previousPosition();
				} catch (IOException e) {
					throw new Error(e);
				}
			}
			return numMoves;
		}

		protected void initialize() {
			writeCache = rangeCaches.get();
			writeCache.ensureByteCapacity(
					(int) Math.min(Integer.MAX_VALUE, writeMem), false);
			writeCache.setRange(firstIndex,
					(int) Math.min(numIndices, writeRecords));
			whichChildren = new int[myGame.maxChildren()];
			readCache = myGame.getCache(db, readMem);
		}

		protected void finished() {
			try {
				writeCache.writeRecordsToDatabase(db, writeHandle, firstIndex,
						numIndices);
			} catch (IOException e) {
				throw new Error(e);
			}
			rangeCaches.release(writeCache);
		}

		@Override
		protected void placeBack(long hash, Record result) {
			if (!writeCache.containsRecord(hash)) {
				writeBack();
				writeCache.setRange(
						hash,
						(int) Math.min(Integer.MAX_VALUE, Math.min(numIndices
								- (hash - firstIndex), writeRecords)));
			}
			long storedResult = writeCache.readRecord(hash);
			long recordVal = myGame.recordToLong(result);
			if (storedResult != recordVal) {
				writeCache.writeRecord(hash, recordVal);
				iChanged = true;
			}
		}

		private void writeBack() {
			try {
				writeCache.writeNextRecordsToDatabase(db, writeHandle,
						writeCache.getFirstRecordIndex(),
						writeCache.getNumRecords());
			} catch (IOException e) {
				throw new Error(e);
			}
		}
	}

	@Override
	protected CacheVISolveTask getSolveTask(int i) {
		return new CacheVISolveTask(splitPlaces[i],
				(int) (splitPlaces[i + 1] - splitPlaces[i]));
	}
}

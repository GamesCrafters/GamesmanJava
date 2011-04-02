package edu.berkeley.gamesman.solver;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.cache.RecordRangeCache;
import edu.berkeley.gamesman.database.cache.TierCache;
import edu.berkeley.gamesman.util.Progressable;

/**
 * A tier solver which wraps the database with a game-specific cache. Presumably
 * this cache will know how to retrieve large numbers of child records at a time
 * which will be needed when running sequentially through the positions in a
 * particular tier
 * 
 * @author dnspies
 */
public class CacheTierSolver extends TierSolver {
	private final long memPerThread;
	private final long readMem, writeMem;
	private final long writeRecords;

	/**
	 * Use this constructor when solving the entire game
	 * 
	 * @param conf
	 *            The configuration object
	 * @param db
	 *            The database to read/write from/to
	 */
	public CacheTierSolver(Configuration conf, Database db) {
		super(conf, db);
		memPerThread = conf.getNumBytes("gamesman.memory", 1L << 25) / nThreads;
		readMem = memPerThread / 2;
		writeMem = memPerThread / 2;
		writeRecords = db.myLogic.getNumRecords(memPerThread / 2);
	}

	/**
	 * Use this constructor when solving only a portion of a single tier of the
	 * game
	 * 
	 * @param conf
	 *            The configuration object
	 * @param db
	 *            The database to read/write from/to
	 * @param tier
	 *            The tier currently being solved
	 * @param firstHash
	 *            The first hash to solve in that tier
	 * @param numHashes
	 *            The number of hashes to solve
	 * @param progress
	 *            An object to report progress to in order to indicate the job
	 *            hasn't frozen
	 */
	public CacheTierSolver(Configuration conf, Database db, int tier,
			long firstHash, long numHashes, Progressable progress) {
		super(conf, db, tier, firstHash, numHashes, progress);
		memPerThread = conf.getNumBytes("gamesman.memory", 1L << 25) / nThreads;
		readMem = memPerThread / 2;
		writeMem = memPerThread / 2;
		writeRecords = db.myLogic.getNumRecords(memPerThread / 2);
	}

	/**
	 * A solve task object which adds the extra cache layer in between the
	 * database and the solver
	 * 
	 * @author dnspies
	 */
	public class CacheTierSolveTask extends TierSolveTask {
		private TierCache readCache;
		private RecordRangeCache writeCache;
		private int[] hints;

		/**
		 * @param firstRecordIndex
		 *            The index of the first record which will be solved by this
		 *            task.
		 * @param numRecords
		 *            The number of records which will be solved by this task.
		 */
		public CacheTierSolveTask(long firstRecordIndex, long numRecords) {
			super(firstRecordIndex, numRecords);
		}

		@Override
		public void prepareSolve() {
			super.prepareSolve();
			hints = new int[myGame.maxChildren()];
			readCache = myGame.getCache(db, readMem);
			writeCache = new RecordRangeCache(db);
			writeCache.ensureByteCapacity(
					(int) Math.min(Integer.MAX_VALUE, writeMem), false);
			writeCache.setRange(
					firstRecordIndex,
					(int) Math.min(Integer.MAX_VALUE,
							Math.min(numRecords, writeRecords)));
		}

		@Override
		public void solvePartialTier() {
			super.solvePartialTier();
			writeBack();
		}

		@Override
		protected void evaluateAndFetchChildren(Record currentValue) {
			int numChildren = myGame.validMoves(childStates, hints);
			readCache.fetchChildren(currentState, numChildren, childStates,
					hints, childRecords);
			currentValue.set(combineChildren(numChildren));
		}

		@Override
		protected void store(long recordIndex, Record currentValue) {
			if (!writeCache.containsRecord(recordIndex)) {
				writeBack();
				writeCache.setRange(recordIndex, (int) Math.min(
						Integer.MAX_VALUE, Math.min(numRecords
								- (recordIndex - firstRecordIndex),
								writeRecords)));
			}
			writeCache.writeRecord(recordIndex,
					myGame.recordToLong(currentValue));
		}

		private void writeBack() {
			try {
				writeCache.writeRecordsToDatabase(db, myWriteHandle,
						writeCache.getFirstRecordIndex(),
						writeCache.getNumRecords());
			} catch (IOException e) {
				throw new Error(e);
			}
		}
	}

	@Override
	protected TierSolveTask getSolveTask(long firstRecordIndex, long numRecords) {
		return new CacheTierSolveTask(firstRecordIndex, numRecords);
	}
}

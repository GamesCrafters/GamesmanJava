package edu.berkeley.gamesman.solver;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.cache.RecordRangeCache;
import edu.berkeley.gamesman.database.cache.TierCache;
import edu.berkeley.gamesman.util.Progressable;

public class CacheTierSolver extends TierSolver {
	private final long memPerThread;
	private final long readMem, writeMem;
	private final long writeRecords;

	public CacheTierSolver(Configuration conf, Database db) {
		super(conf, db);
		memPerThread = conf.getNumBytes("gamesman.memory", 1L << 25) / nThreads;
		readMem = memPerThread / 2;
		writeMem = memPerThread / 2;
		writeRecords = db.myLogic.getNumRecords(memPerThread / 2);
	}

	public CacheTierSolver(Configuration conf, Database db, int tier,
			long firstHash, long numHashes, Progressable progress) {
		super(conf, db, tier, firstHash, numHashes, progress);
		memPerThread = conf.getNumBytes("gamesman.memory", 1L << 25) / nThreads;
		readMem = memPerThread / 2;
		writeMem = memPerThread / 2;
		writeRecords = db.myLogic.getNumRecords(memPerThread / 2);
	}

	public class CacheTierSolveTask extends TierSolveTask {
		private TierCache readCache;
		private RecordRangeCache writeCache;
		private int[] hints;

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
			for (int i = 0; i < numChildren; i++) {
				childRecords[i].previousPosition();
			}
			currentValue.set(myGame.combine(childRecords, numChildren));
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

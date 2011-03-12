package edu.berkeley.gamesman.solver;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Reversi;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies
 */
public class ReversiSolver extends TierSolver {

	public ReversiSolver(Configuration conf, Database db) {
		super(conf, db);
	}

	private class ReversiFixJob implements Runnable {
		private final long firstRecordIndex, numRecords;
		private final DatabaseHandle readHandle1, readHandle2, writeHandle;
		private final Configuration conf;
		private final Reversi myGame;

		private ReversiFixJob(long firstRecordIndex, long numRecords) {
			this.firstRecordIndex = firstRecordIndex;
			this.numRecords = numRecords;
			readHandle1 = db.getHandle(true);
			readHandle2 = db.getHandle(true);
			writeHandle = db.getHandle(false);
			conf = confPool.get();
			myGame = (Reversi) conf.getGame();
		}

		@Override
		public void run() {
			try {
				db.seek(readHandle1, firstRecordIndex);
				db.seek(readHandle2, firstRecordIndex + halfTier);
				myGame.setState(myGame.hashToState(firstRecordIndex));
				Record record1 = myGame.newRecord(), record2 = myGame
						.newRecord();
				long hash = firstRecordIndex;
				for (int i = 0; i < numRecords; i++) {
					myGame.longToRecord(db.readNextRecord(readHandle1), record1);
					myGame.longToRecord(db.readNextRecord(readHandle2), record2);
					if (record1.remoteness == 0 && record2.remoteness != 0) {
						record2.previousPosition();
						long newVal = myGame.recordToLong(record2);
						db.writeRecord(writeHandle, hash, newVal);
					} else if (record2.remoteness == 0
							&& record1.remoteness != 0) {
						record1.previousPosition();
						long newVal = myGame.recordToLong(record1);
						db.writeRecord(writeHandle, hash + halfTier, newVal);
					}
					if (i < numRecords - 1)
						myGame.nextHashInTier();
					hash++;
				}
			} catch (IOException e) {
				throw new Error(e);
			}
			confPool.release(conf);
			tasksFinished.countDown();
		}

	}

	private boolean fixJob = true;
	private long halfTier;

	@Override
	public Runnable nextAvailableJob() {
		if (currentSplit >= splits.length - 1) {
			if (fixJob && currentTier == 0)
				return null;
			if (!awaitOrFailUninterruptibly())
				return null;
			if (fixJob) {
				fixJob = false;
				decrTier();
			} else {
				fixJob = true;
				startFix();
			}
		}
		if (fixJob)
			return nextFix();
		else
			return nextJob();
	}

	protected Runnable nextFix() {
		long firstRecordIndex = splits[currentSplit], numRecords = splits[currentSplit + 1]
				- firstRecordIndex;
		currentSplit++;
		return getFixTask(firstRecordIndex, numRecords);
	}

	private Runnable getFixTask(long firstRecordIndex, long numRecords) {
		return new ReversiFixJob(firstRecordIndex, numRecords);
	}

	private void startFix() {
		System.out.println("Fixing tier " + currentTier);
		currentSplit = 0;
		halfTier = myGame.numHashesForTier(currentTier) / 2;
		splits = Util.getSplits(myGame.hashOffsetForTier(currentTier),
				halfTier, preferredSplits, minSplitSize);
		tasksFinished = new CountDownLatch(splits.length - 1);
	}
}
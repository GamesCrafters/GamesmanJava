package edu.berkeley.gamesman.solver;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Progressable;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

/**
 * A tier solver solves a TierGame by taking advantage of its tier structure. It
 * solves each tier in sequence starting from the highest-indexed tier and
 * working backwards to the beginning of the game. This solver is particularly
 * useful because of how easily it can be parallelized.
 * 
 * @author DNSpies
 */
public class TierSolver extends Solver {
	/**
	 * A TierSolveTask solves a single portion of a tier of a game.
	 * 
	 * @author dnspies
	 */
	protected class TierSolveTask implements Runnable {
		protected final long firstRecordIndex, numRecords;
		protected final DatabaseHandle myReadHandle, myWriteHandle;
		protected Configuration conf;
		protected TierGame myGame;
		protected TierState currentState;
		private Record currentValue;
		protected TierState[] childStates;
		protected Record[] childRecords;

		public TierSolveTask(long firstRecordIndex, long numRecords) {
			this.firstRecordIndex = firstRecordIndex;
			this.numRecords = numRecords;
			myReadHandle = db.getHandle(true);
			myWriteHandle = db.getHandle(false);
		}

		@Override
		public final void run() {
			prepareSolve();
			solvePartialTier();
			confPool.release(conf);
			tasksFinished.countDown();
		}

		public void prepareSolve() {
			conf = confPool.get();
			myGame = (TierGame) conf.getGame();
			currentState = myGame.hashToState(firstRecordIndex);
			myGame.setState(currentState);
			currentValue = myGame.newRecord();
			childStates = myGame.newStateArray(myGame.maxChildren());
			childRecords = myGame.newRecordArray(myGame.maxChildren());
			try {
				db.prepareWriteRecordRange(myWriteHandle, firstRecordIndex,
						numRecords);
			} catch (IOException e) {
				throw new Error(e);
			}
		}

		/**
		 * Solves the prepared portion of the game. This method contains the
		 * "main" for loop for a tier solve. Anything calling this method need
		 * not be particularly efficient. Anything this method calls must be as
		 * efficient as possible.
		 */
		public void solvePartialTier() {
			long hash = firstRecordIndex;
			int lastCount = (int) (firstRecordIndex % stepSize);
			int modCount = lastCount;
			for (long trial = 0; trial < numRecords; trial++) {
				if (modCount == stepSize) {
					addFinished(modCount - lastCount);
					modCount = 0;
					lastCount = 0;
				}
				myGame.getState(currentState);
				Value v = myGame.primitiveValue();
				if (v == Value.UNDECIDED) {
					int numChildren = fetchChildren(currentValue);
					currentValue.set(combineChildren(numChildren));
					store(hash, currentValue);
				} else {
					currentValue.value = v;
					currentValue.remoteness = 0;
					store(hash, currentValue);
				}
				if (trial < numRecords - 1)
					myGame.nextHashInTier();
				hash++;
				modCount++;
			}
			addFinished(modCount - lastCount);
		}

		/**
		 * Calculates the children of the current state and fetches them from
		 * the database.
		 * 
		 * @param currentValue
		 *            The record to store the result in
		 * @return The number of children for this position
		 */
		protected int fetchChildren(Record currentValue) {
			int numChildren = myGame.validMoves(childStates);
			for (int i = 0; i < numChildren; i++) {
				try {
					long recordLong = db.readRecord(myReadHandle,
							myGame.stateToHash(childStates[i]));
					myGame.longToRecord(childStates[i], recordLong,
							childRecords[i]);
				} catch (IOException e) {
					throw new Error(e);
				}
			}
			return numChildren;
		}

		/**
		 * Takes the list of children from childRecords and the number of
		 * children and flips and combines them to generate the corresponding
		 * record for the current position. Note that this method flips the
		 * children as well as choosing one
		 * 
		 * @param numChildren
		 *            The number of children to consider (starting from index 0)
		 * @return The best record (after flipping)
		 */
		protected Record combineChildren(int numChildren) {
			for (int i = 0; i < numChildren; i++) {
				childRecords[i].previousPosition();
			}
			return myGame.combine(childRecords, 0, numChildren);
		}

		protected void store(long recordIndex, Record currentValue) {
			long recordNum = myGame.recordToLong(currentValue);
			try {
				db.writeNextRecord(myWriteHandle, recordNum);
			} catch (IOException e) {
				throw new Error(e);
			}
		}

	}

	protected final long minSplitSize;
	protected int currentTier;
	protected TierGame myGame;
	protected CountDownLatch tasksFinished = new CountDownLatch(0);
	protected long[] splits = new long[0];
	protected int currentSplit;
	final Pool<Configuration> confPool = new Pool<Configuration>(
			new Factory<Configuration>() {

				@Override
				public Configuration newObject() {
					return conf.cloneAll();
				}

				@Override
				public void reset(Configuration t) {
				}
			});
	private long firstHash;
	private long numHashes;
	private final boolean wholeGame;
	private final Progressable progress;
	private volatile long recordsFinished;
	protected final int minSplits;
	protected final long preferredSplitSize;

	public TierSolver(Configuration conf, Database db) {
		super(conf, db);
		myGame = (TierGame) conf.getGame();
		currentTier = myGame.numberOfTiers();
		minSplits = conf.getInteger("gamesman.minimum.splits",
				conf.getInteger("gamesman.threads", 1));
		minSplitSize = conf.getLong("gamesman.minimum.split.size",
				DEFAULT_MIN_SPLIT_SIZE);
		preferredSplitSize = conf.getLong("gamesman.preferred.split.size",
				DEFAULT_PREFERRED_SPLIT_SIZE);
		wholeGame = true;
		progress = null;
	}

	private void addFinished(long hashes) {
		recordsFinished += hashes;
		Util.debug(DebugFacility.SOLVER, (wholeGame ? "Tier " + currentTier
				+ " " : "")
				+ recordsFinished * 10000 / numHashes / 100F + "% complete");
		if (progress != null)
			synchronized (progress) {
				progress.progress();
			}
	}

	/**
	 * To be used for solving a small range and not the entire game
	 * 
	 * @param conf
	 *            The configuration object
	 * @param db
	 *            The database
	 * @param tier
	 *            The tier I'm working in
	 * @param firstHash
	 *            The first hash of the range to solve
	 * @param numHashes
	 *            The number of positions to solve
	 * @param progress
	 *            An object to report progress to (or null if unnecessary)
	 */
	public TierSolver(Configuration conf, Database db, int tier,
			long firstHash, long numHashes, Progressable progress) {
		super(conf, db);
		myGame = (TierGame) conf.getGame();
		currentTier = tier;
		minSplits = conf.getInteger("gamesman.minimum.splits",
				conf.getInteger("gamesman.threads", 1));
		minSplitSize = conf.getLong("gamesman.minimum.split.size",
				DEFAULT_MIN_SPLIT_SIZE);
		preferredSplitSize = conf.getLong("gamesman.preferred.split.size",
				DEFAULT_PREFERRED_SPLIT_SIZE);
		this.firstHash = firstHash;
		this.numHashes = numHashes;
		wholeGame = false;
		splits = Util.getSplits(firstHash, numHashes, minSplitSize, minSplits,
				preferredSplitSize);
		currentSplit = 0;
		tasksFinished = new CountDownLatch(splits.length - 1);
		this.progress = progress;
	}

	@Override
	public Runnable nextAvailableJob() throws InterruptedException {
		if (currentSplit >= splits.length - 1) {
			if (currentTier == 0 || !wholeGame)
				return null;
			tasksFinished.await();
			decrTier();
		}
		return nextJob();
	}

	protected Runnable nextJob() {
		long firstRecordIndex = splits[currentSplit], numRecords = splits[currentSplit + 1]
				- firstRecordIndex;
		currentSplit++;
		return getSolveTask(firstRecordIndex, numRecords);
	}

	protected TierSolveTask getSolveTask(long firstRecordIndex, long numRecords) {
		return new TierSolveTask(firstRecordIndex, numRecords);
	}

	protected void decrTier() {
		currentTier--;
		System.out.println("Solving tier " + currentTier);
		currentSplit = 0;
		firstHash = myGame.hashOffsetForTier(currentTier);
		numHashes = myGame.numHashesForTier(currentTier);
		recordsFinished = 0L;
		splits = Util.getSplits(firstHash, numHashes, minSplitSize, minSplits,
				preferredSplitSize);
		tasksFinished = new CountDownLatch(splits.length - 1);
	}
}
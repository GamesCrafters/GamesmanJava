package edu.berkeley.gamesman.solver;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.FinitePrimitives;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.Undoable;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

/**
 * @author dnspies
 * @param <T>
 *            The game state
 */
public class BreadthFirstSolver<T extends State> extends Solver {

	public BreadthFirstSolver(Configuration conf, Database db) {
		super(conf, db);
		finitePrimitives = conf.getGame() instanceof FinitePrimitives;
	}

	private class BreadthFirstParentTask implements Runnable {
		private final long firstRecordIndex, numRecords;
		private final Configuration conf;
		private final Game<T> game;
		private final Undoable<T> uGame;
		private final T[] parentStates;
		private final DatabaseHandle readHandle, writeHandle, parentReadHandle;

		@SuppressWarnings("unchecked")
		private BreadthFirstParentTask(long firstRecordIndex, long numRecords) {
			this.firstRecordIndex = firstRecordIndex;
			this.numRecords = numRecords;
			conf = confPool.get();
			game = conf.getCheckedGame();
			uGame = (Undoable<T>) game;
			parentStates = game.newStateArray(uGame.maxParents());
			readHandle = db.getHandle(true);
			parentReadHandle = db.getHandle(true);
			writeHandle = db.getHandle(false);
		}

		@Override
		public void run() {
			long hash = firstRecordIndex;
			T state = game.newState();
			Record rec = game.newRecord();
			Record rightNow = game.newRecord();
			long positionsFound = 0;
			try {
				db.prepareReadRecordRange(readHandle, firstRecordIndex, numRecords);
			} catch (IOException e1) {
				throw new Error(e1);
			}
			for (long i = 0; i < numRecords; i++) {
				try {
					long recordLong = db.readNextRecord(readHandle);
					game.longToRecord(null, recordLong, rec);
				} catch (IOException e) {
					throw new Error(e);
				}
				if (rec.value != Value.UNDECIDED
						&& rec.remoteness == currentRemoteness) {
					state = game.hashToState(hash);
					positionsFound++;
					rec.previousPosition();
					int numParents = uGame.possibleParents(state, parentStates);
					for (int p = 0; p < numParents; p++) {
						T parent = parentStates[p];
						long parentHash = game.stateToHash(parent);
						try {
							long rightNowLong = db.readRecord(parentReadHandle,
									parentHash);
							game.longToRecord(parent, rightNowLong, rightNow);
							if (rightNow.value == Value.UNDECIDED
									|| rec.compareTo(rightNow) > 0) {
								long recLong = game.recordToLong(parent, rec);
								db.writeRecord(writeHandle, parentHash, recLong);
							}
						} catch (IOException e) {
							throw new Error(e);
						}
					}
				}
				hash++;
			}
			BreadthFirstSolver.this.positionsFound += positionsFound;
			confPool.release(conf);
			tasksFinished.countDown();
		}

	}

	private class BreadthFirstPrimitiveTask implements Runnable {
		private final long firstRecordIndex, numRecords;
		private final DatabaseHandle writeHandle;
		private final Game<T> game;
		private final Configuration conf;

		private BreadthFirstPrimitiveTask(long firstRecordIndex, long numRecords) {
			this.firstRecordIndex = firstRecordIndex;
			this.numRecords = numRecords;
			writeHandle = db.getHandle(false);
			conf = confPool.get();
			game = conf.getCheckedGame();
		}

		@Override
		public void run() {
			long hash = firstRecordIndex;
			T state = game.newState();
			Record rec = game.newRecord();
			long primitivesFound = 0;
			for (int i = 0; i < numRecords; i++) {
				game.hashToState(hash, state);
				Value v = game.primitiveValue(state);
				if (v != Value.UNDECIDED) {
					primitivesFound++;
					rec.value = v;
					rec.remoteness = 0;
					long recLong = game.recordToLong(state, rec);
					try {
						db.writeRecord(writeHandle, hash, recLong);
					} catch (IOException e) {
						throw new Error(e);
					}
				}
				hash++;
			}
			positionsFound += primitivesFound;
			confPool.release(conf);
			tasksFinished.countDown();
		}
	}

	private class BreadthFirstInitializeTask implements Runnable {
		@SuppressWarnings("unchecked")
		public void run() {
			Game<T> game = conf.getCheckedGame();
			int minSplits = conf.getInteger("gamesman.minimum.splits",
					conf.getInteger("gamesman.threads", 1));
			long minSplitSize = conf.getLong("gamesman.minimum.split.size",
					DEFAULT_MIN_SPLIT_SIZE);
			long preferredSplitSize = conf.getLong(
					"gamesman.preferred.split.size",
					DEFAULT_PREFERRED_SPLIT_SIZE);
			splits = Util.getSplits(0L, game.numHashes(), minSplitSize,
					minSplits, preferredSplitSize);
			DatabaseHandle dh = db.getHandle(false);
			Record r = game.newRecord();
			r.value = Value.UNDECIDED;
			long record = game.recordToLong(game.startingPositions().iterator()
					.next(), r);
			try {
				db.fill(dh, record);
				if (finitePrimitives) {
					FinitePrimitives<T> pGame;
					pGame = (FinitePrimitives<T>) game;
					Collection<T> primitives = pGame.getPrimitives();
					for (T primitive : primitives) {
						r.value = game.primitiveValue(primitive);
						r.remoteness = 0;
						record = game.recordToLong(primitive, r);
						db.writeRecord(dh, game.stateToHash(primitive), record);
					}
					if (primitives.size() > 0)
						positionsFound += primitives.size();
				}
			} catch (IOException e) {
				throw new Error(e);
			}
			tasksFinished.countDown();
		}
	}

	private int currentRemoteness = -1;
	private boolean firstTaskReturned = false;
	private boolean firstTaskFinished = false;
	private final boolean finitePrimitives;
	private long[] splits = new long[0];
	private int currentSplit = 0;
	private CountDownLatch tasksFinished = new CountDownLatch(1);
	private volatile long positionsFound = 0L;
	private final Pool<Configuration> confPool = new Pool<Configuration>(
			new Factory<Configuration>() {

				@Override
				public Configuration newObject() {
					return conf.cloneAll();
				}

				@Override
				public void reset(Configuration t) {
				}
			});

	@Override
	public Runnable nextAvailableJob() throws InterruptedException{
		if (!firstTaskReturned) {
			firstTaskReturned = true;
			return new BreadthFirstInitializeTask();
		}
		if (currentSplit >= splits.length - 1 || !firstTaskFinished) {
			tasksFinished.await();
			if (positionsFound == 0)
				return null;
			if (firstTaskFinished || finitePrimitives)
				addRemoteness();
			firstTaskFinished = true;
		}
		return nextJob();
	}

	private Runnable nextJob() {
		Runnable result;
		if (currentRemoteness == -1) {
			result = new BreadthFirstPrimitiveTask(splits[currentSplit],
					splits[currentSplit + 1]);
		} else {
			result = new BreadthFirstParentTask(splits[currentSplit],
					splits[currentSplit + 1]);
		}
		currentSplit++;
		return result;
	}

	private void addRemoteness() {
		if (currentRemoteness >= 0)
			System.out.println("Positions at remoteness " + currentRemoteness
					+ ": " + positionsFound);
		currentRemoteness++;
		currentSplit = 0;
		positionsFound = 0;
		tasksFinished = new CountDownLatch(splits.length - 1);
	}
}

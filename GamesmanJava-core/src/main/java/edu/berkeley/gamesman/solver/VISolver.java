package edu.berkeley.gamesman.solver;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.VIGame;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.Util;

/**
 * @author dnspies
 * 
 */
public class VISolver extends Solver {

	private class VIInitTask implements Runnable {
		@Override
		public void run() {
			Record rec = mainGame.newRecord();
			rec.value = Value.DRAW;
			long rNum = mainGame.recordToLong(rec);
			DatabaseHandle dh = db.getHandle(false);
			try {
				db.fill(dh, rNum);
			} catch (IOException e) {
				throw new Error(e);
			}
			passChanged = true;
			running.countDown();
		}
	}

	private final VIGame mainGame;
	private final int maxChildren;
	private volatile boolean passChanged = false;
	private final boolean withRepeats;
	private final LinkedList<Runnable> solveSplits;
	/**
	 * 
	 */
	protected final long[] splitPlaces;
	private CountDownLatch running = null;
	private int passCount = 0;

	private final Pool<VIGame> gamePool;

	/**
	 * @param conf
	 * @param db
	 */
	public VISolver(Configuration conf, Database db) {
		this(conf, db, 0, conf.getGame().numHashes(), true);
	}

	/**
	 * @param conf
	 * @param db
	 * @param firstHash
	 * @param numHashes
	 */
	public VISolver(Configuration conf, Database db, long firstHash,
			long numHashes) {
		this(conf, db, firstHash, numHashes, false);
	}

	protected VISolver(final Configuration conf, Database db, long firstHash,
			long numHashes, boolean withRepeats) {
		super(conf, db);
		mainGame = (VIGame) conf.getGame();
		maxChildren = mainGame.maxChildren();
		gamePool = new Pool<VIGame>(new Factory<VIGame>() {
			@Override
			public VIGame newObject() {
				return (VIGame) conf.cloneAll().getGame();
			}

			@Override
			public void reset(VIGame t) {
			}
		});
		this.withRepeats = withRepeats;
		assert !withRepeats || firstHash == 0
				&& numHashes == mainGame.numHashes();
		splitPlaces = Util.getSplits(firstHash, numHashes,
				conf.getLong("gamesman.minimum.split.size", 65536),
				conf.getInteger("gamesman.minimum.splits", nThreads),
				conf.getInteger("gamesman.preferred.split.size", 1 << 22));
		solveSplits = new LinkedList<Runnable>();
		if (withRepeats) {
			VIInitTask vi = new VIInitTask();
			solveSplits.add(vi);
			running = new CountDownLatch(1);
		} else {
			fillSplits();
		}
	}

	private void fillSplits() {
		for (int i = 0; i < splitPlaces.length - 1; i++) {
			assert splitPlaces[i + 1] - splitPlaces[i] < Integer.MAX_VALUE;
			VISolveTask next = getSolveTask(i);
			solveSplits.add(next);
		}
		passChanged = false;
		running = new CountDownLatch(splitPlaces.length - 1);
	}

	/**
	 * @param i
	 * @return
	 */
	protected VISolveTask getSolveTask(int i) {
		return new VISolveTask(splitPlaces[i],
				(int) (splitPlaces[i + 1] - splitPlaces[i]));
	}

	@Override
	public Runnable nextAvailableJob() throws InterruptedException {
		if (solveSplits.isEmpty()) {
			running.await();
			if (withRepeats && passChanged) {
				System.out.println("Pass " + passCount++);
				fillSplits();
				return solveSplits.remove();
			} else
				return null;
		} else {
			return solveSplits.remove();
		}
	}

	/**
	 * @author dnspies
	 * 
	 */
	protected class VISolveTask implements Runnable {
		/**
		 * 
		 */
		protected VIGame myGame;
		/**
		 * 
		 */
		protected final long firstIndex;
		/**
		 * 
		 */
		protected final int numIndices;
		/**
		 * 
		 */
		protected final long[] childHashes;
		/**
		 * 
		 */
		protected Record[] childRecords;
		private Record primRec;
		/**
		 * 
		 */
		protected DatabaseHandle readHandle;
		/**
		 * 
		 */
		protected DatabaseHandle writeHandle;
		/**
		 * 
		 */
		protected boolean iChanged = false;
		/**
		 * 
		 */
		protected long hash;

		/**
		 * @param firstIndex
		 * @param numIndices
		 */
		VISolveTask(long firstIndex, int numIndices) {
			this.firstIndex = firstIndex;
			this.numIndices = numIndices;
			this.childHashes = new long[maxChildren];
		}

		@Override
		public void run() {
			myGame = gamePool.get();
			readHandle = db.getHandle(true);
			childRecords = myGame.getPoolRecordArray();
			primRec = myGame.getPoolRecord();
			hash = firstIndex;
			initialize();
			myGame.setFromHash(firstIndex);
			boolean next = true;
			for (long count = 0; count < numIndices; count++) {
				assert next;
				hash = count + firstIndex;
				assert hash == myGame.getHash();
				primRec.value = myGame.primitiveValue();
				primRec.remoteness = 0;
				Record result = primRec;
				if (primRec.value == Value.UNDECIDED) {
					int numChildren = fetchChildren();
					result = myGame.combine(childRecords, 0, numChildren);
				}
				placeBack(hash, result);
				next = myGame.next();
			}
			writeHandle = db.getHandle(false);
			finished();
			myGame.release(primRec);
			myGame.release(childRecords);
			gamePool.release(myGame);
			passChanged |= iChanged;
			running.countDown();
		}

		/**
		 * 
		 */
		protected void initialize() {
		}

		/**
		 * 
		 */
		protected void finished() {
		}

		/**
		 * @param hash
		 * @param result
		 */
		protected void placeBack(long hash, Record result) {
			long storedResult;
			try {
				storedResult = db.readRecord(readHandle, hash);
				long recordVal = myGame.recordToLong(result);
				if (storedResult != recordVal) {
					db.writeRecord(writeHandle, hash, recordVal);
					iChanged = true;
				}
			} catch (IOException e) {
				throw new Error(e);
			}
		}

		/**
		 * @return
		 */
		protected int fetchChildren() {
			int numMoves = myGame.validMoves(childHashes);
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
	}
}

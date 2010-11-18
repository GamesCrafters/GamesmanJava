package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.MemoryDatabase;
import edu.berkeley.gamesman.game.Connect4;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

public class C4CachedSolver extends TierSolver {
	private final Pool<MemoryDatabase> readPagePool;
	private final Pool<MemoryDatabase> writePagePool;
	private int maxPage;
	private long times[] = new long[8];

	public C4CachedSolver(final Configuration conf) {
		super(conf);
		readPagePool = new Pool<MemoryDatabase>(new Factory<MemoryDatabase>() {

			public MemoryDatabase newObject() {
				MemoryDatabase md = new MemoryDatabase(readDb, null, conf,
						false, true);
				reset(md);
				return md;
			}

			public void reset(MemoryDatabase t) {
				if (splits > minSplits)
					t.ensureByteSize(maxPage);
			}
		});
		writePagePool = new Pool<MemoryDatabase>(new Factory<MemoryDatabase>() {

			public MemoryDatabase newObject() {
				return new MemoryDatabase(writeDb, null, conf, true, true);
			}

			public void reset(MemoryDatabase t) {
			}
		});
	}

	@Override
	protected void solvePartialTier(Configuration conf, long start,
			long hashes, TierSolverUpdater t, Database readDb,
			DatabaseHandle readDh, Database writeDb, DatabaseHandle writeDh) {
		final long firstNano;
		long nano = 0;
		final boolean debugSolver = Util.debug(DebugFacility.SOLVER);
		if (debugSolver) {
			for (int i = 0; i < 8; i++) {
				times[i] = 0;
			}
			firstNano = System.nanoTime();
			nano = firstNano;
		} else
			firstNano = 0;
		Connect4 game = (Connect4) conf.getGame();
		maxPage = (int) ((maxMem / (2 * numThreads) - writeDb.requiredMem(
				start, hashes)) / game.maxChildren());
		long current = start;
		long stepNum = current % STEP_SIZE;
		Record record = game.newRecord();
		Record bestRecord = game.newRecord();
		TierState[] children = new TierState[game.maxChildren()];
		for (int i = 0; i < children.length; i++)
			children[i] = game.newState();

		MemoryDatabase[] readPages = null;
		DatabaseHandle[] readHandles = null;
		long[] lastChildren = null;
		if (tier < game.numberOfTiers() - 1) {
			readPages = new MemoryDatabase[game.maxChildren()];
			readHandles = new DatabaseHandle[game.maxChildren()];
			game.setState(game.hashToState(start + hashes - 1));
			game.lastMoves(children);
			lastChildren = new long[children.length];
			for (int i = 0; i < children.length; i++)
				lastChildren[i] = game.stateToHash(children[i]);
		}
		MemoryDatabase writePage = writePagePool.get();
		writePage.setRange(start, (int) hashes);
		DatabaseHandle writePageDh = writePage.getHandle();

		TierState curState = game.hashToState(start);
		game.setState(curState);

		long lastNano;
		if (debugSolver) {
			lastNano = nano;
			nano = System.nanoTime();
			times[0] = nano - lastNano;
		}
		for (long count = 0L; count < hashes; count++) {
			if (stepNum == STEP_SIZE) {
				t.calculated(STEP_SIZE);
				stepNum = 0;
			}
			Value pv = game.primitiveValue();
			if (debugSolver) {
				lastNano = nano;
				nano = System.nanoTime();
				times[1] += nano - lastNano;
			}
			if (pv == Value.UNDECIDED) {
				int len = game.validMoves(children);
				if (debugSolver) {
					lastNano = nano;
					nano = System.nanoTime();
					times[2] += nano - lastNano;
				}
				bestRecord.value = Value.UNDECIDED;
				for (int i = 0; i < len; i++) {
					int col = game.openColumn[i];
					long childHash = game.stateToHash(children[i]);
					if (readPages[col] == null
							|| !readPages[col].containsRecord(childHash)) {
						if (readPages[col] != null) {
							readPages[col].flush();
							readPages[col].setRange(childHash, (int) Math.min(
									lastChildren[col] + 1 - childHash,
									readDb.recordsForMem(childHash, maxPage)));
						} else {
							readPages[col] = readPagePool.get();
							readPages[col].setRange(childHash, (int) Math.min(
									lastChildren[col] + 1 - childHash,
									readDb.recordsForMem(childHash, maxPage)));
							readHandles[col] = readPages[col].getHandle();
						}
					}
					game.longToRecord(children[i], readPages[col].getRecord(
							readHandles[col], childHash), record);
					record.previousPosition();
					if (bestRecord.value == Value.UNDECIDED
							|| record.compareTo(bestRecord) > 0)
						bestRecord.set(record);
				}
				if (debugSolver) {
					lastNano = nano;
					nano = System.nanoTime();
					times[3] += nano - lastNano;
				}
				writePage.putRecord(writePageDh, current,
						game.recordToLong(curState, bestRecord));
			} else if (pv == Value.IMPOSSIBLE) {
				break;
			} else {
				if (debugSolver) {
					lastNano = nano;
					nano = System.nanoTime();
					times[2] += nano - lastNano;
					lastNano = nano;
					nano = System.nanoTime();
					times[3] += nano - lastNano;
				}
				record.remoteness = 0;
				record.value = pv;
				writePage.putRecord(writePageDh, current,
						game.recordToLong(curState, record));
			}
			if (debugSolver) {
				lastNano = nano;
				nano = System.nanoTime();
				times[4] += nano - lastNano;
			}
			if (count < hashes - 1) {
				game.nextHashInTier();
				curState.hash++;
			}
			++current;
			++stepNum;
			if (debugSolver) {
				lastNano = nano;
				nano = System.nanoTime();
				times[5] += nano - lastNano;
				lastNano = nano;
				nano = System.nanoTime();
				times[6] += nano - lastNano;
			}
		}
		if (readPages != null)
			for (int i = 0; i < readPages.length; i++)
				if (readPages[i] != null) {
					readPages[i].flush();
					readPagePool.release(readPages[i]);
				}
		writePage.closeHandle(writePageDh);
		writePage.flush();
		writePagePool.release(writePage);
		if (debugSolver) {
			lastNano = nano;
			nano = System.nanoTime();
			times[7] = nano - lastNano;
			long sumTimes = nano - firstNano - times[6] * 6;
			Util.debug(DebugFacility.SOLVER, "Initializing: " + 1000 * times[0]
					/ sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Primitive Value: " + 1000
					* (times[1] - times[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Calculating Chilren: " + 1000
					* (times[2] - times[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Reading Children: " + 1000
					* (times[3] - times[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Storing records: " + 1000
					* (times[4] - times[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Stepping: " + 1000
					* (times[5] - times[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Finalizing: " + 1000 * times[7]
					/ sumTimes / 10D);
		}
	}

	@Override
	protected int numSplits(int tier, long maxMem, long numHashes) {
		TierGame game = (TierGame) conf.getGame();
		long tierHashes = game.numHashesForTier(tier);
		long thisTierSize = writeDb.requiredMem(game.hashOffsetForTier(tier),
				tierHashes);
		long lastTierSize = tier == game.numberOfTiers() - 1 ? 0 : readDb
				.requiredMem(game.hashOffsetForTier(tier + 1),
						game.numHashesForTier(tier + 1));
		if (tierHashes == numHashes) {
			return (int) ((thisTierSize + lastTierSize * game.maxChildren())
					* 2 * numThreads / maxMem);
		} else {
			double frac = (double) numHashes / tierHashes;
			return (int) ((thisTierSize + lastTierSize * game.maxChildren())
					* 2 * numThreads / maxMem * frac);
		}
	}
}

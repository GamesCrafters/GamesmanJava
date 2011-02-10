package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.MemoryDatabase;
import edu.berkeley.gamesman.database.RangeCache;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;

public class CacheTierSolver extends TierSolver {
	private boolean isChild;
	private int[] cachePlaces;

	public CacheTierSolver(Configuration conf) {
		this(conf, false);
	}

	public CacheTierSolver(Configuration conf, boolean isChild) {
		super(conf);
		this.isChild = isChild;
		if (isChild)
			cachePlaces = new int[conf.getGame().maxChildren()];
	}

	@Override
	protected void solvePartialTier(Configuration conf, long firstHash,
			long numHashes) {
		if (isChild) {
			super.solvePartialTier(conf, firstHash, numHashes);
			return;
		}
		CacheTierSolver innerSolver = new CacheTierSolver(conf, true);
		innerSolver.updater = updater;
		TierGame game = (TierGame) conf.getGame();
		TierState ts = game.hashToState(firstHash);
		game.setState(ts);
		long useMem = maxMem / (numThreads * 2)
				- writeDb.requiredMem(firstHash, numHashes);
		if (useMem < 0)
			throw new Error("Not enough memory to build cache");
		RangeCache readCache = null;
		if (readDb != null)
			readCache = game.getCache(readDb, numHashes, useMem);
		MemoryDatabase writeCache = new MemoryDatabase(writeDb, null, conf,
				true, firstHash, numHashes);
		innerSolver.writeDb = writeCache;
		if (readCache == null) {
			innerSolver.solvePartialTier(conf, firstHash, numHashes);
		} else {
			while (true) {
				innerSolver.readDb = readCache;
				innerSolver.solvePartialTier(conf, firstHash,
						readCache.numHashes());
				if (readCache.numHashes() < numHashes) {
					game.nextHashInTier();
					firstHash += readCache.numHashes();
					numHashes -= readCache.numHashes();
					game.getState(ts);
					if (game.stateToHash(ts) != firstHash) {
						throw new RuntimeException(game.stateToHash(ts)
								+ " != " + firstHash);
					}
					readCache = game.nextCache();
				} else
					break;
			}
		}
		writeCache.flush();
	}

	@Override
	protected int calculateAndFetchValues(TierGame game, TierState[] children,
			Record[] childRecords, DatabaseHandle readDh) {
		RangeCache readCache = (RangeCache) readDb;
		int numChildren = game.validMoves(children, cachePlaces);
		for (int i = 0; i < numChildren; i++) {
			long recordLong = readCache.getRecord(readDh,
					game.stateToHash(children[i]), cachePlaces[i]);
			game.longToRecord(children[i], recordLong, childRecords[i]);
			childRecords[i].previousPosition();
		}
		return numChildren;
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

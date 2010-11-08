package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.MemoryDatabase;
import edu.berkeley.gamesman.database.TierReadCache;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;

public class CacheTierSolver extends TierSolver {

	public CacheTierSolver(Configuration conf) {
		super(conf);
	}

	protected void solvePartialTier(Configuration conf, long start,
			long hashes, TierSolverUpdater t, Database readDb,
			DatabaseHandle readDh, Database writeDb, DatabaseHandle writeDh) {
		TierGame game = (TierGame) conf.getGame();
		TierState ts = game.hashToState(start);
		game.setState(ts);
		TierReadCache readCache = game.getCache(readDb, hashes, maxMem
				/ (numThreads * 2) - writeDb.requiredMem(start, hashes));
		MemoryDatabase writeCache = new MemoryDatabase(writeDb, null, conf,
				true, start, hashes);
		writeDh = writeCache.getHandle();
		while (true) {
			readDh = readCache.getHandle();
			super.solvePartialTier(conf, start, readCache.numHashes(), t,
					readCache, readDh, writeCache, writeDh);
			if (readCache.numHashes() < hashes) {
				game.nextHashInTier();
				start += readCache.numHashes();
				hashes -= readCache.numHashes();
				readCache = game.nextCache();
			} else
				break;
		}
		writeCache.closeHandle(writeDh);
		writeCache.flush();
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

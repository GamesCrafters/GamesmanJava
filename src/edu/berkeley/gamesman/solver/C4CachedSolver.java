package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.MemoryDatabase;
import edu.berkeley.gamesman.game.Connect4;
import edu.berkeley.gamesman.game.util.TierState;

public class C4CachedSolver extends TierSolver {

	public C4CachedSolver(Configuration conf) {
		super(conf);
	}

	@Override
	protected void solvePartialTier(Configuration conf, long start,
			long hashes, TierSolverUpdater t, Database readDb,
			DatabaseHandle readDh, Database writeDb, DatabaseHandle writeDh) {
		Connect4 game = (Connect4) conf.getGame();
		long current = start;
		long stepNum = current % STEP_SIZE;
		TierState[] children = new TierState[game.maxChildren()];
		for (int i = 0; i < children.length; i++)
			children[i] = new TierState();

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
		MemoryDatabase writePage = new MemoryDatabase(writeDb, null, conf,
				true, start, hashes);
		DatabaseHandle writePageDh = writePage.getHandle();
		int maxPage = (int) (maxMem / (numThreads * game.maxChildren()));

		TierState curState = game.hashToState(start);
		game.setState(curState);
		Record[] vals = new Record[game.maxChildren()];
		for (int i = 0; i < vals.length; i++)
			vals[i] = new Record(conf);
		Record prim = new Record(conf);
		for (long count = 0L; count < hashes; count++) {
			if (stepNum == STEP_SIZE) {
				t.calculated(STEP_SIZE);
				stepNum = 0;
			}
			PrimitiveValue pv = game.primitiveValue();
			switch (pv) {
			case UNDECIDED:
				int len = game.validMoves(children);
				for (int i = 0; i < len; i++) {
					int col = game.openColumn[i];
					long childHash = game.stateToHash(children[i]);
					if (readPages[col] == null
							|| !readPages[col].containsRecord(childHash)) {
						if (readPages[col] != null) {
							readPages[col].flush();
							readPages[col]
									.setRange(childHash, (int) Math.min(
											lastChildren[col] + 1 - childHash,
											maxPage));
						} else {
							readPages[col] = new MemoryDatabase(readDb, null,
									conf, false, childHash, Math.min(
											lastChildren[col] + 1 - childHash,
											maxPage));
							readHandles[col] = readPages[col].getHandle();
						}
					}
					game.recordFromLong(children[i], readPages[col].getRecord(
							readHandles[col], childHash), vals[i]);
					vals[i].previousPosition();
				}
				Record newVal = game.combine(vals, 0, len);
				writePage.putRecord(writePageDh, current, game.getRecord(
						curState, newVal));
				break;
			case IMPOSSIBLE:
				break;
			default:
				prim.remoteness = 0;
				prim.value = pv;
				writePage.putRecord(writePageDh, current, game.getRecord(
						curState, prim));
			}
			if (count < hashes - 1) {
				game.nextHashInTier();
				curState.hash++;
			}
			++current;
			++stepNum;
		}
		if (readPages != null)
			for (int i = 0; i < readPages.length; i++)
				if (readPages[i] != null) {
					readPages[i].closeHandle(readHandles[i]);
					readPages[i].flush();
				}
		writePage.closeHandle(writePageDh);
		writePage.flush();
	}
}

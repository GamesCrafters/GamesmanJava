package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.game.TieredIterGame;
import edu.berkeley.gamesman.game.util.ItergameState;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies
 */
public final class TierItergameSolver extends TierSolver<ItergameState> {
	@Override
	protected void solvePartialTier(Configuration conf, long start,
			long hashes, TierSolverUpdater t, Database inRead, Database inWrite) {
		TieredIterGame game = Util.checkedCast(conf.getGame());
		long current = start;
		game.setState(game.hashToState(start));
		Record[] vals = new Record[game.maxChildren()];
		for (int i = 0; i < vals.length; i++)
			vals[i] = game.newRecord();
		Record prim = game.newRecord();
		ItergameState[] children = new ItergameState[game.maxChildren()];
		for (int i = 0; i < children.length; i++)
			children[i] = new ItergameState();
		for (long count = 0L; count < hashes; count++) {
			if (current % STEP_SIZE == 0)
				t.calculated(STEP_SIZE);
			PrimitiveValue pv = game.primitiveValue();
			switch (pv) {
			case UNDECIDED:
				int len = game.validMoves(children);
				Record r;
				for (int i = 0; i < len; i++) {
					r = vals[i];
					inRead.getRecord(game.stateToHash(children[i]), r);
					r.previousPosition();
				}
				Record newVal = game.combine(vals, 0, len);
				inWrite.putRecord(current, newVal);
				break;
			case IMPOSSIBLE:
				prim.value = PrimitiveValue.LOSE;
				inWrite.putRecord(current, prim);
				break;
			default:
				if (conf.remotenessStates > 0)
					prim.remoteness = 0;
				prim.value = pv;
				inRead.putRecord(current, prim);
			}
			if (count < hashes - 1)
				game.nextHashInTier();
			++current;
		}
	}
}
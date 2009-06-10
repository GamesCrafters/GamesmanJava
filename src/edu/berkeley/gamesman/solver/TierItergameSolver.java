package edu.berkeley.gamesman.solver;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies
 */
public final class TierItergameSolver extends TierSolver<ItergameState> {
	@Override
	protected void solvePartialTier(TieredGame<ItergameState> tierGame,
			long start, long end, TierSolverUpdater t) {
		TieredIterGame game = Util.checkedCast(tierGame);
		long current = start;
		game.setState(game.hashToState(start));
		while (current <= end) {
			if (current % STEP_SIZE == 0)
				t.calculated(STEP_SIZE);
			PrimitiveValue pv = game.primitiveValue();
			if (pv.equals(PrimitiveValue.UNDECIDED)) {
				Collection<Pair<String, ItergameState>> children = game
						.validMoves();
				ArrayList<Record> vals = new ArrayList<Record>(children.size());
				Record r;
				for (Pair<String, ItergameState> child : children) {
					r = db.getRecord(game.stateToHash(child.cdr));
					r.previousPosition();
					vals.add(r);
				}
				Record newVal = Record.combine(conf, vals);
				db.putRecord(current, newVal);
			} else {
				Record prim = new Record(conf, pv);
				db.putRecord(current, prim);
			}
			if (current != end)
				game.nextHashInTier();
			current++;
		}
	}
}
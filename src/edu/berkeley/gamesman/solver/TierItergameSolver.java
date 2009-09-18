package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies
 */
public final class TierItergameSolver extends TierSolver<ItergameState> {
	@Override
	protected void solvePartialTier(Configuration conf, long start, long end,
			TierSolverUpdater t) {
		TieredIterGame game = Util.checkedCast(conf.getGame());
		long current = start;
		game.setState(game.hashToState(start));
		Record[] vals = new Record[game.maxChildren()];
		for (int i = 0; i < vals.length; i++)
			vals[i] = game.newRecord();
		Record prim = game.newRecord();
		ItergameState[] children = new ItergameState[game.maxChildren()];
		boolean hasRemoteness = conf.containsField(RecordFields.REMOTENESS);
		for (int i = 0; i < children.length; i++)
			children[i] = new ItergameState();
		while (current <= end) {
			if (current % STEP_SIZE == 0)
				t.calculated(STEP_SIZE);
			PrimitiveValue pv = game.primitiveValue();
			if (pv.equals(PrimitiveValue.UNDECIDED)) {
				int len = game.validMoves(children);
				Record r;
				for (int i = 0; i < len; i++) {
					r = vals[i];
					db.getRecord(game.stateToHash(children[i]), r);
					r.previousPosition();
				}
				Record newVal = game.combine(vals, 0, len);
				db.putRecord(current, newVal);
			} else {
				if (hasRemoteness)
					prim.set(RecordFields.REMOTENESS, 0);
				prim.set(RecordFields.VALUE, pv.value());
				db.putRecord(current, prim);
			}
			if (current != end){
				if(current==20)
					System.out.println("Here");
				game.nextHashInTier();
			}
			current++;
		}
	}
}
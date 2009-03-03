package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.game.connect4.ItergameState;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * TierSolver documentation stub
 * 
 * @author DNSpies
 * 
 */
public final class TierItergameSolver extends TierSolver<ItergameState> {

	@Override
	protected void solvePartialTier(TieredGame<ItergameState> game2, BigInteger start,
			BigInteger end, TierSolverUpdater t) {
		BigInteger current = start;
		TieredIterGame game=Util.checkedCast(game2);
		ItergameState state = game.hashToState(current);
		game.setState(state);
		while (current.compareTo(end) < 0) {

			if (current.mod(BigInteger.valueOf(10000)).compareTo(
					BigInteger.ZERO) == 0)
				t.calculated(10000);

			PrimitiveValue pv = game.primitiveValue();

			if (pv.equals(PrimitiveValue.Undecided)) {
				Collection<Pair<String, ItergameState>> children = game
						.validMoves();
				ArrayList<Record> vals = new ArrayList<Record>(children.size());

				for (Pair<String, ItergameState> child : children) {
					Record r = db.getRecord(game.stateToHash(child.cdr));
					vals.add(r);
				}

				Record newVal = Record.combine(conf, vals);
				db.putRecord(current, newVal);
			} else {
				Record prim = new Record(conf, pv);
				db.putRecord(current, prim);
			}
			current = current.add(BigInteger.ONE);
			if(game.hasNextHashInTier())
				game.nextHashInTier();
		}
	}
}
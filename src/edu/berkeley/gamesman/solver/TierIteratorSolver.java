package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.game.IteratorState;
import edu.berkeley.gamesman.game.TieredIteratorGame;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * TierSolver documentation stub
 * 
 * @author DNSpies
 * 
 */
public final class TierIteratorSolver extends TierSolver<IteratorState> {

	@Override
	protected void solvePartialTier(TieredGame<IteratorState> game2, BigInteger start,
			BigInteger end, TierSolverUpdater t) {
		BigInteger current = start;
		TieredIteratorGame game=Util.checkedCast(game2);
		IteratorState state = game.hashToState(current);
		game.setState(state);
		while (current.compareTo(end) < 0) {
			current = current.add(BigInteger.ONE);

			if (current.mod(BigInteger.valueOf(10000)).compareTo(
					BigInteger.ZERO) == 0)
				t.calculated(10000);

			PrimitiveValue pv = game.primitiveValue();

			if (pv.equals(PrimitiveValue.Undecided)) {
				Collection<Pair<String, IteratorState>> children = game
						.validMoves();
				ArrayList<Record> vals = new ArrayList<Record>(children.size());

				for (Pair<String, IteratorState> child : children) {
					vals.add(db.getRecord(game.stateToHash(child.cdr)));
				}

				Record newVal = Record.combine(conf, vals);
				db.putRecord(current, newVal);
			} else {
				Record prim = new Record(conf, pv);
				db.putRecord(current, prim);
			}
			
			if(game.hasNextHashInTier())
				game.nextHashInTier();
		}
	}
}
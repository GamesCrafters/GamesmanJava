package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.game.CycleState;
import edu.berkeley.gamesman.game.TieredCycleGame;
import edu.berkeley.gamesman.solver.TierSolver.TierSolverUpdater;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * TierSolver documentation stub
 * 
 * @author Steven Schlansker
 * 
 */
public final class TierCycleSolver extends TierSolver<CycleState> {

	@Override
	protected void solvePartialTier(TieredGame<CycleState> game2, BigInteger start,
			BigInteger end, TierSolverUpdater t) {
		BigInteger current = start;
		TieredCycleGame game=Util.checkedCast(game2);
		CycleState state = game.hashToState(current);
		game.setState(state);
		while (current.compareTo(end) < 0) {
			current = current.add(BigInteger.ONE);

			if (current.mod(BigInteger.valueOf(10000)).compareTo(
					BigInteger.ZERO) == 0)
				t.calculated(10000);

			PrimitiveValue pv = game.primitiveValue();

			if (pv.equals(PrimitiveValue.Undecided)) {
				Collection<Pair<String, CycleState>> children = game
						.validMoves();
				ArrayList<Record> vals = new ArrayList<Record>(children.size());

				for (Pair<String, CycleState> child : children) {
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
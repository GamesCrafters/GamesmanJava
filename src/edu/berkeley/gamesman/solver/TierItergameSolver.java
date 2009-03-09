package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * TierSolver documentation stub
 * 
 * @author DNSpies
 * 
 */
public final class TierItergameSolver extends TierSolver<ItergameState> {

	protected void solvePartialTier(TieredIterGame game, BigInteger start,
			BigInteger end, TierSolverUpdater t) {
		BigInteger current = start;
		game.hashToState(start);
		while (current.compareTo(end) <= 0) {
			current = current.add(BigInteger.ONE);
			if (current.mod(BigInteger.valueOf(10000)).compareTo(
					BigInteger.ZERO) == 0)
				t.calculated(10000);

			PrimitiveValue pv = game.primitiveValue();
			
			if (pv.equals(PrimitiveValue.UNDECIDED)) {
				Util.debug(DebugFacility.SOLVER,"Primitive value for state "+current+" is undecided");
				Collection<Pair<String,ItergameState>> children = game.validMoves();
				ArrayList<Record> vals = new ArrayList<Record>(children.size());
				for (Pair<String,ItergameState> child : children) {
					vals.add(db.getRecord(game.stateToHash(child.cdr)));
				}

				Record newVal = Record.combine(conf, vals);
				db.putRecord(current, newVal);
			} else {				
				Record prim = new Record(conf, pv);
				Util.debug(DebugFacility.SOLVER,"Primitive value for state "+current+" is "+prim);
				db.putRecord(current, prim);
			}
			if(game.hasNextHashInTier())
				game.nextHashInTier();
		}
		Util.debug(DebugFacility.SOLVER,"Reached end of partial tier at "+end);
	}
}
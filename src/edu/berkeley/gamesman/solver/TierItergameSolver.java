package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
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
            BigInteger start, BigInteger end, TierSolverUpdater t) {
        TieredIterGame game = Util.checkedCast(tierGame);
        BigInteger current = start;
        game.setState(game.hashToState(start));
        while (current.compareTo(end) <= 0) {
            if (current.mod(BigInteger.valueOf(STEP_SIZE)).compareTo(
                    BigInteger.ZERO) == 0)
                t.calculated(STEP_SIZE);
            PrimitiveValue pv = game.primitiveValue();
            if (pv.equals(PrimitiveValue.UNDECIDED)) {
                Collection<Pair<String, ItergameState>> children = game
                        .validMoves();
                ArrayList<Record> vals = new ArrayList<Record>(children.size());
                for (Pair<String, ItergameState> child : children)
                    vals.add(db.getRecord(game.stateToHash(child.cdr)));
                Record newVal = Record.combine(conf, vals);
                db.putRecord(current, newVal);
            } else {
                Record prim = new Record(conf, pv);
                db.putRecord(current, prim);
            }
            if (!current.equals(end))
                game.nextHashInTier();
            current = current.add(BigInteger.ONE);
        }
    }
}
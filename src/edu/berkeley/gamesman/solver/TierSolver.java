package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.DBValue;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.threading.Barrier;

/**
 * TierSolver documentation stub
 * 
 * @author Steven Schlansker
 *
 */
public final class TierSolver extends Solver {

	protected TieredGame<Object, DBValue> myGame;

	@Override
	public WorkUnit prepareSolve(Game<?, ?> game) {

		myGame = (TieredGame<Object, DBValue>) game;
		tier = myGame.numberOfTiers() - 1;
		offset = myGame.hashOffsetForTier(tier);
		updater = new TierSolverUpdater();

		return new TierSolverWorkUnit();
	}

	protected void solvePartialTier(TieredGame<Object, DBValue> game,
			BigInteger start, BigInteger end, TierSolverUpdater t) {
		BigInteger current = start.subtract(BigInteger.ONE);
		
		while (current.compareTo(end) < 0) {
			current = current.add(BigInteger.ONE);
			
			if (current.mod(BigInteger.valueOf(10000)).compareTo(
					BigInteger.ZERO) == 0)
				t.calculated(10000);
		
			
			Object state = game.hashToState(current);
			
			Collection<?> children = game.validMoves(state);
			
			if (children.size() == 0){
				DBValue prim = game.primitiveValue(state);

				db.setValue(current, prim);
			} else {
				ArrayList<DBValue> vals = new ArrayList<DBValue>(children.size());
				
				for (Object child : children) {
					vals.add(db.getValue(game.stateToHash(child)));
				}
				
				DBValue newVal = vals.get(0).fold(vals);
				db.setValue(current, newVal);
			}
		}
	}

	protected int nextIndex = 0;
	protected TierSolverUpdater updater;
	protected BigInteger offset;
	protected Barrier barr = new Barrier();
	protected int tier;

	private boolean needs2Sync;

	protected Pair<BigInteger, BigInteger> nextSlice() {
		if (needs2Sync) {
			Util.debug("Thread waiting to tier-sync");
			barr.sync();
		}

		synchronized (this) {
			if(tier < 0) return null;
			final BigInteger step = BigInteger.valueOf(1000);
			BigInteger ret = offset, end;
			offset = offset.add(step);
			end = ret.add(step);
			if (end.compareTo(myGame.lastHashValueForTier(tier)) >= 0) {
				Util.debug("Reached end of tier");
				end = myGame.lastHashValueForTier(tier);
				tier--;
				if(tier >= 0)
					offset = myGame.hashOffsetForTier(tier);
				needs2Sync = true;
			}
			return new Pair<BigInteger, BigInteger>(ret, end);
		}
	}

	private final class TierSolverWorkUnit implements WorkUnit {

		private int index;

		TierSolverWorkUnit() {

			// if(!(g instanceof TieredGame))
			// Util.fatalError("Attempted to use tiered solver on non-tiered game");
			this.index = nextIndex++;
		}

		public void conquer() {
			barr.enter();
			Util.debug("Started the solver... (" + index + ")");
			Thread.currentThread().setName("Solver ("+index+"): "+myGame.toString());

			Pair<BigInteger, BigInteger> slice;
			while ((slice = nextSlice()) != null) {
				Util.debug("Beginning to solve slice " + slice + " in thread "
						+ index);
				solvePartialTier(myGame, slice.car, slice.cdr, updater);
				db.flush();
			}

			if(barr.exit())
				updater.complete();
		}

		public List<WorkUnit> divide(int num) {
			ArrayList<WorkUnit> arr = new ArrayList<WorkUnit>(num);
			arr.add(this);
			for (int i = 1; i < num; i++)
				arr.add(new TierSolverWorkUnit());
			return arr;
		}
	}

	private final class TierSolverUpdater {

		private BigInteger total = BigInteger.ZERO;
		private Task t;
		private long lastUpdate = 0;

		TierSolverUpdater() {
			t = Task.beginTask("Tier solving \"" + myGame.toString() + "\"");
			t.setTotal(myGame.lastHashValueForTier(myGame.numberOfTiers() - 1));
		}

		synchronized void calculated(int howMuch) {
			total = total.add(BigInteger.valueOf(howMuch));
			if (t != null && System.currentTimeMillis() - lastUpdate > 1000){
				t.setProgress(total);
				lastUpdate = System.currentTimeMillis();
			}
		}

		public synchronized void complete() {
			if (t != null)
				t.complete();
			t = null;
		}
	}
}

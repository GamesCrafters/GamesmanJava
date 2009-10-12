package edu.berkeley.gamesman.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.util.*;

/**
 * TierSolver documentation stub
 * 
 * @author Steven Schlansker
 * @param <T>
 *            The state type for the game
 */
public class TierSolver<T> extends Solver {

	@Override
	public WorkUnit prepareSolve(Configuration conf, Game<Object> game) {

		updater = new TierSolverUpdater(Util
				.<TieredGame<T>, Game<Object>> checkedCast(game));

		return new TierSolverWorkUnit(conf, 0, false);
	}

	protected void solvePartialTier(Configuration conf, long start, long end,
			TierSolverUpdater t) {
		long current = start - 1;

		TieredGame<T> game = Util.checkedCast(conf.getGame());

		while (current < end) {
			current++;

			if (current % STEP_SIZE == 0)
				t.calculated(STEP_SIZE);

			T state = game.hashToState(current);

			PrimitiveValue pv = game.primitiveValue(state);

			if (pv.equals(PrimitiveValue.UNDECIDED)) {
				assert Util.debug(DebugFacility.SOLVER,
						"Primitive value for state " + current
								+ " is undecided");
				Collection<Pair<String, T>> children = game.validMoves(state);
				ArrayList<Record> vals = new ArrayList<Record>(children.size());
				for (Pair<String, T> child : children) {
					vals.add(db.getRecord(game.stateToHash(child.cdr)));
				}

				Record newVal = game.combine(vals);
				db.putRecord(current, newVal);
			} else {
				Record prim = game.newRecord(pv);
				assert Util.debug(DebugFacility.SOLVER,
						"Primitive value for state " + current + " is " + prim);
				db.putRecord(current, prim);
			}
		}
		assert Util.debug(DebugFacility.THREADING,
				"Reached end of partial tier at " + end);

	}

	private TierSolverUpdater updater;

	private final Runnable flusher = new Runnable() {
		public void run() {
			db.flush();
			barr.reset();
		}
	};

	private CyclicBarrier barr = new CyclicBarrier(1, flusher);

	private int division = 1;

	private synchronized Pair<Long, Long> getSlice(int tier, int index,
			Configuration conf) {
		if (tier < 0)
			return null;
		TieredGame<T> myGame = Util.checkedCast(conf.getGame());
		long tierStart = myGame.hashOffsetForTier(tier);
		long tierEnd = myGame.lastHashValueForTier(tier);
		long start, end;
		if (tierEnd - tierStart < division * conf.recordsPerGroup)
			if (index == 0)
				return new Pair<Long, Long>(tierStart, tierEnd);
			else
				return new Pair<Long, Long>(0L, -1L);
		if (index == 0)
			start = tierStart;
		else {
			start = (tierEnd - tierStart + 1) * index / division + tierStart;
			start -= start % conf.recordsPerGroup;
		}
		if (index == division - 1)
			end = tierEnd;
		else {
			end = (tierEnd - tierStart + 1) * (index + 1) / division
					+ tierStart - 1;
			end -= (end + 1) % conf.recordsPerGroup;
		}
		return new Pair<Long, Long>(start, end);
	}

	private final class TierSolverWorkUnit implements WorkUnit {

		private int index;

		private int tier;

		private TieredGame<T> game;

		private Configuration conf;

		TierSolverWorkUnit(Configuration conf, int index, boolean clone) {

			// if(!(g instanceof TieredGame))
			// Util.fatalError("Attempted to use tiered solver on non-tiered
			// game");
			if (clone)
				this.conf = conf.cloneAll();
			else
				this.conf = conf;
			game = Util.checkedCast(conf.getGame());
			this.tier = game.numberOfTiers() - 1;
			this.index = index;
		}

		public void conquer() {
			assert Util.debug(DebugFacility.SOLVER, "Started the solver... ("
					+ index + ")");
			Thread.currentThread().setName(
					"Solver (" + index + "): " + game.toString());
			Pair<Long, Long> slice;
			int arrived = 0;
			try {
				barr.await();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (BrokenBarrierException e1) {
				e1.printStackTrace();
			}
			while ((slice = getSlice(tier, index, conf)) != null) {
				assert Util.debug(DebugFacility.THREADING,
						"Beginning to solve slice " + slice + " for tier "
								+ tier);
				solvePartialTier(conf, slice.car, slice.cdr, updater);
				assert Util.debug(DebugFacility.THREADING, "Finished Slice");
				try {
					arrived = barr.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
				tier--;
			}
			if (arrived == 0)
				updater.complete();
		}

		public List<WorkUnit> divide(int num) {
			ArrayList<WorkUnit> arr = new ArrayList<WorkUnit>(num);
			division = num;
			barr = new CyclicBarrier(num);
			arr.add(this);
			for (int i = 1; i < num; i++)
				arr.add(new TierSolverWorkUnit(conf, i, true));
			return arr;
		}
	}

	final class TierSolverUpdater {

		private long total = 0;

		private Task t;

		TierSolverUpdater(TieredGame<T> myGame) {
			t = Task.beginTask("Tier solving \"" + myGame.describe() + "\"");
			t.setTotal(myGame.lastHashValueForTier(myGame.numberOfTiers() - 1));
		}

		synchronized void calculated(int howMuch) {
			total += howMuch;
			if (t != null) {
				t.setProgress(total);
			}
		}

		public synchronized void complete() {
			if (t != null)
				t.complete();
			t = null;
		}
	}
}

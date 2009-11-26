package edu.berkeley.gamesman.solver;

import java.util.*;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * The top down solver is a simple solver that simply builds a game tree in
 * memory and solves it with a graph search.
 * 
 * @author Steven Schlansker
 * @param <T>
 *            The game state
 */
public class TopDownSolver<T> extends Solver {

	Configuration conf;

	// Game<Object> g;

	@Override
	public WorkUnit prepareSolve(Configuration config) {
		conf = config;
		Game<?> game = config.getGame();
		long hashSpace = game.numHashes();
		Record defaultRecord = game.newRecord(PrimitiveValue.UNDECIDED);
		for (long index = 0; index < hashSpace; index++) {
			writeDb.putRecord(index, defaultRecord);
		}
		writeDb.flush();
		TopDownSolverWorkUnit wu = new TopDownSolverWorkUnit(Util
				.<Game<T>, Game<?>> checkedCast(game));
		return wu;
	}

	class TopDownSolverWorkUnit implements WorkUnit {

		final private Game<T> game;

		public TopDownSolverWorkUnit(Game<T> g) {
			game = g;
		}

		public void conquer() {
			// HashMap<T, BigInteger> cache = new HashMap<T, BigInteger>();
			HashSet<Long> seen = new HashSet<Long>();

			LinkedList<T> workList = new LinkedList<T>();
			LinkedList<T> dependencies = new LinkedList<T>();
			ArrayList<Record> recs = new ArrayList<Record>();
			int maxRemoteness = 0;
			for (T s : game.startingPositions()) {
				workList.add(s);
				seen.add(game.stateToHash(s));
			}
			while (!workList.isEmpty()
					|| (dependencies != null && !dependencies.isEmpty())) {
				recs.clear();
				T state;
				if (workList.isEmpty()) {
					workList = dependencies;
					dependencies = null;
				}
				state = workList.removeLast();
				Collection<Pair<String, T>> children = game.validMoves(state);
				assert Util.debug(DebugFacility.SOLVER, "Looking at state "
						+ game.stateToString(state));
				// assert Util.debug(DebugFacility.SOLVER, "Worklist is now " +
				// Util.mapStateToString(game,workList));

				boolean insertBefore = false;

				for (Pair<String, T> child : children) {
					long loc = game.stateToHash(child.cdr);
					Record r;

					r = readDb.getRecord(loc);
					if (r.value == PrimitiveValue.UNDECIDED) {
						assert Util.debug(DebugFacility.SOLVER,
								"Not seen child state "
										+ game.stateToString(child.cdr)
										+ " before, coming back later...");
						insertBefore = true;
						workList.add(child.cdr);
					} else {
						recs.add(r);
					}
				}

				if (insertBefore != false) {
					assert Util.debug(DebugFacility.SOLVER,
							"One of the children hasn't been solved yet, revisiting "
									+ game.stateToString(state) + " later");
					dependencies.add(state);
					continue;
				}

				long loc = game.stateToHash(state);
				Record next;
				PrimitiveValue prim = game.primitiveValue(state);
				if ((!prim.equals(PrimitiveValue.UNDECIDED))
						|| children.isEmpty()) {
					assert Util.debug(DebugFacility.SOLVER,
							"Getting primitive value for state "
									+ game.stateToString(state) + ": " + prim);
					next = game.newRecord(prim);
					next.score = game.primitiveScore(state);
				} else {
					next = game.combine(recs);
					int remoteness = (int) next.remoteness;
					if (remoteness > maxRemoteness) {
						System.out.println("Found remoteness: " + remoteness);
						maxRemoteness = remoteness;
					}
					assert Util.debug(DebugFacility.SOLVER, "COMBINE " + recs
							+ " => " + next + "; children size = "
							+ children.size());
				}
				writeDb.putRecord(loc, next);
				assert Util.debug(DebugFacility.SOLVER, "Solved state \n"
						+ game.displayState(state) + " to " + next);
			}
		}

		public List<WorkUnit> divide(int num) {
			List<WorkUnit> a = new ArrayList<WorkUnit>();
			a.add(this);
			return a;
		}

	}

}

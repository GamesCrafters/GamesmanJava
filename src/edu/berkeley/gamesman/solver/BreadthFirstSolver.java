package edu.berkeley.gamesman.solver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.util.*;

/**
 * A solver designed with puzzles in mind that will do a breadth first search
 * from all of the starting positions until every reachable position has been
 * hit. It is assumed that every starting position is a WIN and that the puzzle
 * is reversible.
 * 
 * @author Patrick Horn (with Jeremy Fleischman watching)
 * @param <T>
 *            The game state
 */
public class BreadthFirstSolver<T> extends Solver {

	Configuration conf;

	long hashSpace;

	protected CyclicBarrier barr;

	volatile int lastTier = 0;

	@Override
	public WorkUnit prepareSolve(Configuration config) {
		conf = config;
		Game<T> game = Util.checkedCast(config.getGame());
		int maxRemoteness = conf.getInteger("gamesman.solver.maxRemoteness",
				Integer.MAX_VALUE);
		hashSpace = game.numHashes();
		Record defaultRecord = game.newRecord(PrimitiveValue.UNDECIDED);
		writeDb.fill(defaultRecord, 0, hashSpace);

		long numPositionsZero = 0;
		long numPositionsOne = 0;
		for (T s : game.startingPositions()) {
			long hash = game.stateToHash(s);
			PrimitiveValue win = game.primitiveValue(s);
			Record rec = game.newRecord(win);
			writeDb.putRecord(hash, rec);
			for (Pair<String, T> child : game.validMoves(s)) {
				long childhash = game.stateToHash(child.cdr);
				// it is possible that some of our children are duplicates
				// so to count numPositionsOne correctly, we ignore those
				// duplicates
				if (readDb.getRecord(childhash).value == PrimitiveValue.UNDECIDED) {
					Record childrec = game.newRecord(win);
					childrec.remoteness = 1;
					writeDb.putRecord(childhash, childrec);
					numPositionsOne++;
				}
			}
			numPositionsZero++;
		}
		assert Util.debug(DebugFacility.SOLVER,
				"Number of states at remoteness 0: " + numPositionsZero);
		assert Util.debug(DebugFacility.SOLVER,
				"Number of states at remoteness 1: " + numPositionsOne);
		lastTier = 1;
		writeDb.flush();

		return new BreadthFirstWorkUnit(Util
				.<Game<T>, Game<?>> checkedCast(game), maxRemoteness);
	}

	class BreadthFirstWorkUnit implements WorkUnit {

		final private Game<T> game;

		final private int maxRemoteness;

		final private long firstHash;

		final private long lastHashPlusOne;

		// Constructs for the entire hash space.
		public BreadthFirstWorkUnit(Game<T> g, int maxRemoteness) {
			game = g;
			this.maxRemoteness = maxRemoteness;
			lastHashPlusOne = hashSpace;
			firstHash = 0;
		}

		// For divide()
		private BreadthFirstWorkUnit(BreadthFirstWorkUnit copy, long firstHash,
				long lastHashPlusOne) {
			game = copy.game;
			maxRemoteness = copy.maxRemoteness;
			this.firstHash = firstHash;
			this.lastHashPlusOne = lastHashPlusOne;
		}

		public void conquer() {
			long numPositionsInLevel = 0;
			long numPositionsSeen = numPositionsInLevel;
			int remoteness = lastTier;
			Task solveTask = Task.beginTask(String.format("BFS solving \"%s\"",
					game.describe()));
			solveTask.setTotal(lastHashPlusOne - firstHash);
			solveTask.setProgress(0);
			Record rec;
			Record childrec;
			while (lastTier >= remoteness && remoteness < maxRemoteness) {
				HashSet<Long> setValues = new HashSet<Long>();
				numPositionsInLevel = 0;
				for (long hash = firstHash; hash < lastHashPlusOne - 1; hash++) {
					rec = readDb.getRecord(hash);
					if (rec.value != PrimitiveValue.UNDECIDED
							&& rec.remoteness == remoteness) {
						// System.out.println("Found! "+hash+"="+rec);
						for (Pair<String, T> child : game.validMoves(game
								.hashToState(hash))) {
							long childhash = game.stateToHash(child.cdr);
							if (setValues.contains(childhash)) {
								continue;
							}
							childrec = readDb.getRecord(childhash);
							if (childrec.value == PrimitiveValue.UNDECIDED) {
								childrec.value = rec.value;
								childrec.remoteness = remoteness + 1;
								// System.out.println("Setting child "+childhash+"="+childrec);
								writeDb.putRecord(childhash, childrec);
								numPositionsInLevel++;
								numPositionsSeen++;
								setValues.add(childhash);
								lastTier = remoteness + 1;
								if (numPositionsSeen % STEP_SIZE == 0) {
									solveTask.setProgress(numPositionsSeen);
								}
							} else {
								// System.out.println("Get child "+childhash+"="+childrec);
							}
						}
					}
				}
				remoteness++;
				assert Util.debug(DebugFacility.SOLVER,
						"Number of states at remoteness " + remoteness + ": "
								+ numPositionsInLevel);
				try {
					writeDb.flush();
					if (barr != null)
						barr.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
			}
			solveTask.complete();
			assert Util.debug(DebugFacility.SOLVER,
					"Solving finished!!! Max remoteness is " + (remoteness - 1)
							+ ".");
		}

		public List<WorkUnit> divide(int num) {
			ArrayList<WorkUnit> arr = new ArrayList<WorkUnit>(num);
			// arr.add(this);
			long hashIncrement = hashSpace / num;
			long currentHash = 0;

			if (num < hashSpace) {
				for (int i = 0; i < num - 1; i++) {
					long endHash = currentHash + hashIncrement;
					arr
							.add(new BreadthFirstWorkUnit(this, currentHash,
									endHash));
					currentHash = endHash;
				}
			}
			// add the last one separately in case of rounding errors.
			arr.add(new BreadthFirstWorkUnit(this, currentHash, hashSpace));
			barr = new CyclicBarrier(arr.size());
			return arr;
		}

	}

}

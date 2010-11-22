package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.*;

public class RearrangeSolver extends TierSolver {
	private long debugTimes[] = new long[7];

	public RearrangeSolver(Configuration conf) {
		super(conf);
	}

	protected void solvePartialTier(Configuration conf, long start,
			long hashes, TierSolverUpdater t, DatabaseHandle readDh,
			DatabaseHandle writeDh) {
		final long firstNano;
		long nano = 0;
		// final boolean debugSolver = Util.debug(DebugFacility.SOLVER);
		final boolean debugSolver = true;
		if (debugSolver) {
			for (int i = 0; i < 7; i++) {
				debugTimes[i] = 0;
			}
			firstNano = System.nanoTime();
			nano = firstNano;
		} else
			firstNano = 0;
		TierGame game = (TierGame) conf.getGame();
		long current = start;
		long stepNum = current % STEP_SIZE;
		TierState curState = game.hashToState(start);
		game.setState(curState);
		Record[] vals = new Record[game.maxChildren()];
		for (int i = 0; i < vals.length; i++)
			vals[i] = new Record(conf);
		Record prim = new Record(conf);
		TierState[] children = new TierState[game.maxChildren()];
		for (int i = 0; i < children.length; i++)
			children[i] = game.newState();
		long lastNano;
		if (debugSolver) {
			lastNano = nano;
			nano = System.nanoTime();
			debugTimes[0] = nano - lastNano;
		}
		Value pv = game.primitiveValue();
		int boardSize = ((TierGame) conf.getGame()).numberOfTiers();
		long minorRearrangements = 0;
		int turnPieces;
		turnPieces = (tier + 1) / 2;
		minorRearrangements = Util.nCr(boardSize - 1 - turnPieces, tier
				- turnPieces);
		for (long count = 0L; count < hashes; count++) {
			if (curState.hash % minorRearrangements == 0) {
				pv = game.primitiveValue();
			}
			if (stepNum == STEP_SIZE) {
				t.calculated(STEP_SIZE);
				stepNum = 0;
			}
			if (debugSolver) {
				lastNano = nano;
				nano = System.nanoTime();
				debugTimes[1] += nano - lastNano;
			}
			if (pv == Value.UNDECIDED) {
				int len = game.validMoves(children);
				if (debugSolver) {
					lastNano = nano;
					nano = System.nanoTime();
					debugTimes[2] += nano - lastNano;
				}
				for (int i = 0; i < len; i++) {
					game.longToRecord(
							children[i],
							readDb.getRecord(readDh,
									game.stateToHash(children[i])), vals[i]);
					vals[i].previousPosition();
				}
				if (debugSolver) {
					lastNano = nano;
					nano = System.nanoTime();
					debugTimes[3] += nano - lastNano;
				}
				Record newVal = game.combine(vals);
				writeDb.putRecord(writeDh, current,
						game.recordToLong(curState, newVal));
			} else if (pv == Value.IMPOSSIBLE) {
				break;
			} else {
				if (debugSolver) {
					lastNano = nano;
					nano = System.nanoTime();
					debugTimes[2] += nano - lastNano;
					lastNano = nano;
					nano = System.nanoTime();
					debugTimes[3] += nano - lastNano;
				}
				prim.remoteness = 0;
				prim.value = pv;
				writeDb.putRecord(writeDh, current,
						game.recordToLong(curState, prim));
			}
			if (debugSolver) {
				lastNano = nano;
				nano = System.nanoTime();
				debugTimes[4] += nano - lastNano;
			}
			if (count < hashes - 1) {
				game.nextHashInTier();
				curState.hash++;
			}
			++current;
			++stepNum;
			if (debugSolver) {
				lastNano = nano;
				nano = System.nanoTime();
				debugTimes[5] += nano - lastNano;
				lastNano = nano;
				nano = System.nanoTime();
				debugTimes[6] += nano - lastNano;
			}
			if (curState.hash % 1000000 == 0) {
				long sumTimes = nano - firstNano - debugTimes[6] * 6;
				System.out.println("Initializing: " + 1000 * debugTimes[0]
						/ sumTimes / 10D);
				System.out.println("Primitive Value: " + 1000
						* (debugTimes[1] - debugTimes[6]) / sumTimes / 10D);
				System.out.println("Calculating Chilren: " + 1000
						* (debugTimes[2] - debugTimes[6]) / sumTimes / 10D);
				System.out.println("Reading Children: " + 1000
						* (debugTimes[3] - debugTimes[6]) / sumTimes / 10D);
				System.out.println("Storing records: " + 1000
						* (debugTimes[4] - debugTimes[6]) / sumTimes / 10D);
				System.out.println("Stepping: " + 1000
						* (debugTimes[5] - debugTimes[6]) / sumTimes / 10D);
			}
		}
		if (debugSolver) {
			long sumTimes = nano - firstNano - debugTimes[6] * 6;
			Util.debug(DebugFacility.SOLVER, "Initializing: " + 1000
					* debugTimes[0] / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Primitive Value: " + 1000
					* (debugTimes[1] - debugTimes[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Calculating Chilren: " + 1000
					* (debugTimes[2] - debugTimes[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Reading Children: " + 1000
					* (debugTimes[3] - debugTimes[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Storing records: " + 1000
					* (debugTimes[4] - debugTimes[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Stepping: " + 1000
					* (debugTimes[5] - debugTimes[6]) / sumTimes / 10D);
		}
	}
}

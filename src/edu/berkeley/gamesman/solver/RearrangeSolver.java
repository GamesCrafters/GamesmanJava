package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.RearrangeGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.*;

public class RearrangeSolver extends TierSolver {
	public RearrangeSolver(Configuration conf) {
		super(conf);
	}

	@Override
	protected void solvePartialTier(Configuration conf, long start,
			long hashes, TierSolverUpdater t, Database readDb,
			DatabaseHandle readDh, Database writeDb, DatabaseHandle writeDh) {
		final long firstNano;
		long nano = 0;
		final boolean debugSolver = Util.debug(DebugFacility.SOLVER);
		if (debugSolver) {
			for (int i = 0; i < 7; i++) {
				times[i] = 0;
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
		Record record = game.newRecord();
		Record bestRecord = game.newRecord();
		TierState[] children = new TierState[game.maxChildren()];
		for (int i = 0; i < children.length; i++)
			children[i] = game.newState();
		long lastNano;
		if (debugSolver) {
			lastNano = nano;
			nano = System.nanoTime();
			times[0] = nano - lastNano;
		}
		Value pv = game.primitiveValue();
		for (long count = 0L; count < hashes; count++) {
			if (stepNum == STEP_SIZE) {
				t.calculated(STEP_SIZE);
				stepNum = 0;
			}
			
			if(((RearrangeGame)game).majorChanged()){
				pv = game.primitiveValue();
			}
			if (debugSolver) {
				lastNano = nano;
				nano = System.nanoTime();
				times[1] += nano - lastNano;
			}
			if (pv == Value.UNDECIDED) {
				int len = game.validMoves(children);
				if (debugSolver) {
					lastNano = nano;
					nano = System.nanoTime();
					times[2] += nano - lastNano;
				}
				bestRecord.value = Value.UNDECIDED;
				for (int i = 0; i < len; i++) {
					game.longToRecord(
							children[i],
							readDb.getRecord(readDh,
									game.stateToHash(children[i])), record);
					record.previousPosition();
					if (bestRecord.value == Value.UNDECIDED
							|| record.compareTo(bestRecord) > 0)
						bestRecord.set(record);
				}
				if (debugSolver) {
					lastNano = nano;
					nano = System.nanoTime();
					times[3] += nano - lastNano;
				}
				writeDb.putRecord(writeDh, current,
						game.recordToLong(curState, bestRecord));
			} else if (pv != Value.IMPOSSIBLE) {
				if (debugSolver) {
					lastNano = nano;
					nano = System.nanoTime();
					times[2] += nano - lastNano;
					lastNano = nano;
					nano = System.nanoTime();
					times[3] += nano - lastNano;
				}
				record.remoteness = 0;
				record.value = pv;
				writeDb.putRecord(writeDh, current,
						game.recordToLong(curState, record));
			}
			if (debugSolver) {
				lastNano = nano;
				nano = System.nanoTime();
				times[4] += nano - lastNano;
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
				times[5] += nano - lastNano;
				lastNano = nano;
				nano = System.nanoTime();
				times[6] += nano - lastNano;
			}
		}
		if (debugSolver) {
			long sumTimes = nano - firstNano - times[6] * 6;
			Util.debug(DebugFacility.SOLVER, "Initializing: " + 1000 * times[0]
					/ sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Primitive Value: " + 1000
					* (times[1] - times[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Calculating Chilren: " + 1000
					* (times[2] - times[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Reading Children: " + 1000
					* (times[3] - times[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Storing records: " + 1000
					* (times[4] - times[6]) / sumTimes / 10D);
			Util.debug(DebugFacility.SOLVER, "Stepping: " + 1000
					* (times[5] - times[6]) / sumTimes / 10D);
		}
	}
}

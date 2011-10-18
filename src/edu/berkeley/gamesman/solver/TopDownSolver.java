package edu.berkeley.gamesman.solver;

import java.io.IOException;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.TopDownGame;
import edu.berkeley.gamesman.game.TopDownMutaGame;
import edu.berkeley.gamesman.util.*;

/**
 * A solver for top-down mutable games
 * 
 * @author dnspies
 */
public class TopDownSolver extends Solver {
	private boolean askedJob = false;
	private final boolean debugSolver;

	/**
	 * The default constructor
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public TopDownSolver(final Configuration conf, Database db) {
		super(conf, db);
		debugSolver = Util.debug(DebugFacility.SOLVER);
	}

	public class TopDownSolveTask implements Runnable {
		public void run() {
			Game<?> g = conf.getGame();
			TopDownMutaGame game;
			if (g instanceof TopDownMutaGame) {
				game = (TopDownMutaGame) g;
			} else {
				game = wrapGame(g);
			}
			Record defaultRecord = game.newRecord();
			defaultRecord.value = Value.UNDECIDED;
			DatabaseHandle readHandle = db.getHandle(true);
			DatabaseHandle writeHandle = db.getHandle(false);
			try {
				db.fill(writeHandle, game.recordToLong(defaultRecord));
			} catch (IOException e) {
				throw new Error(e);
			}
			for (int startNum = 0; startNum < game.numStartingPositions(); startNum++) {
				game.setStartingPosition(startNum);
				long currentTimeMillis = System.currentTimeMillis();
				solve(game, game.newRecord(), 0, readHandle, writeHandle);
				System.out.println(Util.millisToETA(System.currentTimeMillis()
						- currentTimeMillis)
						+ " time to complete");
			}
		}

		private <S extends State> TopDownMutaGame wrapGame(Game<S> g) {
			return new TopDownGame<S>(g);
		}

		private void solve(TopDownMutaGame game, Record value, int depth,
				DatabaseHandle readDh, DatabaseHandle writeDh) {
			if (depth < 3 && debugSolver) {
				if (Util.debug(DebugFacility.SOLVER)) {
					System.out.println(game.displayState());
				}
			}
			long hash = game.getHash();
			try {
				game.longToRecord(db.readRecord(readDh, hash), value);
			} catch (IOException e) {
				throw new Error(e);
			}
			if (value.value != Value.UNDECIDED)
				return;
			Value pv = game.primitiveValue();
			switch (pv) {
			case UNDECIDED:
				Record bestRecord = game.getPoolRecord();
				bestRecord.value = Value.UNDECIDED;
				int numChildren = game.makeMove();
				for (int child = 0; child < numChildren; child++) {
					solve(game, value, depth + 1, readDh, writeDh);
					value.previousPosition();
					if (bestRecord.value == Value.UNDECIDED
							|| value.compareTo(bestRecord) > 0)
						bestRecord.set(value);
					game.changeMove();
				}
				if (numChildren > 0)
					game.undoMove();
				value.set(bestRecord);
				game.release(bestRecord);
				break;
			case IMPOSSIBLE:
				throw new Error(
						"Top-down solve should not reach impossible positions");
			default:
				value.value = pv;
				value.remoteness = 0;
			}
			try {
				db.writeRecord(writeDh, hash, game.recordToLong(value));
			} catch (IOException e) {
				throw new Error(e);
			}
		}
	}

	@Override
	public Runnable nextAvailableJob() {
		if (askedJob) {
			return null;
		} else {
			askedJob = true;
			return new TopDownSolveTask();
		}
	}
}

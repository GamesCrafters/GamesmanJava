package edu.berkeley.gamesman.solver;

import java.io.IOException;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.TopDownGame;
import edu.berkeley.gamesman.game.TopDownMutaGame;
import edu.berkeley.gamesman.util.*;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

/**
 * A solver for top-down mutable games
 * 
 * @author dnspies
 */
public class TopDownSolver extends Solver {
	protected final Pool<Record> recordPool;
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
		final Game<?> game = conf.getGame();
		debugSolver = Util.debug(DebugFacility.SOLVER);
		recordPool = new Pool<Record>(new Factory<Record>() {
			public Record newObject() {
				return game.newRecord();
			}

			public void reset(Record t) {
			}
		});
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
				if (failed != null)
					return;
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
				Record bestRecord = recordPool.get();
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
				recordPool.release(bestRecord);
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

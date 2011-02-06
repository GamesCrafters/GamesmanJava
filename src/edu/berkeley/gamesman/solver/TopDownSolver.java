package edu.berkeley.gamesman.solver;

import java.util.List;

import edu.berkeley.gamesman.core.*;
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
	protected boolean containsRemoteness;
	protected final Pool<Record> recordPool;

	/**
	 * The default constructor
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public TopDownSolver(final Configuration conf) {
		super(conf);
		final Game<?> game = conf.getGame();
		recordPool = new Pool<Record>(new Factory<Record>() {
			public Record newObject() {
				return game.newRecord();
			}

			public void reset(Record t) {
			}
		});
		containsRemoteness = conf.hasRemoteness;
	}

	@Override
	public WorkUnit prepareSolve(final Configuration conf) {
		return new WorkUnit() {

			public void conquer() {
				solve(conf);
			}

			public List<WorkUnit> divide(int num) {
				throw new UnsupportedOperationException();
			}

		};
	}

	/**
	 * The method that solves the game
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public void solve(Configuration conf) {
		Game<?> g = conf.getGame();
		TopDownMutaGame game;
		if (g instanceof TopDownMutaGame) {
			game = (TopDownMutaGame) g;
		} else {
			game = wrapGame(g);
		}
		long hashSpace = game.numHashes();
		Record defaultRecord = game.newRecord();
		defaultRecord.value = Value.UNDECIDED;
		writeDb.fill(game.recordToLong(defaultRecord), 0, hashSpace);
		for (int startNum = 0; startNum < game.numStartingPositions(); startNum++) {
			game.setStartingPosition(startNum);
			long currentTimeMillis = System.currentTimeMillis();
			DatabaseHandle readHandle = readDb.getHandle();
			DatabaseHandle writeHandle = writeDb.getHandle();
			solve(game, game.newRecord(), 0, readHandle, writeHandle);
			writeDb.closeHandle(writeHandle);
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
		if (depth < 3)
			if (Util.debug(DebugFacility.SOLVER))
				System.out.println(game.displayState());
		long hash = game.getHash();
		game.longToRecord(readDb.getRecord(readDh, hash), value);
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
		writeDb.putRecord(writeDh, hash, game.recordToLong(value));
	}
}

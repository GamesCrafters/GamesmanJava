package edu.berkeley.gamesman.solver;

import java.util.List;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.TopDownGame;
import edu.berkeley.gamesman.game.TopDownMutaGame;
import edu.berkeley.gamesman.util.*;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.RecycleLinkedList;

/**
 * A solver for top-down mutable games
 * 
 * @author dnspies
 */
public class TopDownSolver extends Solver {
	protected boolean containsRemoteness;

	protected RecycleLinkedList<Record[]> recordList;

	/**
	 * The default constructor
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public TopDownSolver(final Configuration conf) {
		super(conf);
		final Game<?> game = conf.getGame();
		recordList = new RecycleLinkedList<Record[]>(new Factory<Record[]>() {
			public Record[] newObject() {
				Record[] retVal = new Record[game.maxChildren()];
				for (int i = 0; i < retVal.length; i++)
					retVal[i] = game.getRecord();
				return retVal;
			}

			public void reset(Record[] t) {
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
			game = wrapGame(conf, g);
		}
		long hashSpace = game.numHashes();
		Record defaultRecord = game.getRecord();
		defaultRecord.value = Value.UNDECIDED;
		writeDb.fill(game.recordToLong(defaultRecord), 0, hashSpace);
		for (int startNum = 0; startNum < game.numStartingPositions(); startNum++) {
			game.setStartingPosition(startNum);
			long currentTimeMillis = System.currentTimeMillis();
			DatabaseHandle readHandle = readDb.getHandle();
			DatabaseHandle writeHandle = writeDb.getHandle();
			solve(game, game.getRecord(), 0, readHandle, writeHandle);
			writeDb.closeHandle(writeHandle);
			System.out.println(Util.millisToETA(System.currentTimeMillis()
					- currentTimeMillis)
					+ " time to complete");
		}
	}

	private <S extends State> TopDownMutaGame wrapGame(Configuration conf,
			Game<S> g) {
		return new TopDownGame<S>(conf, g);
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
			Record[] recs = recordList.addFirst();
			boolean made = game.makeMove();
			int i = 0;
			while (made) {
				solve(game, value, depth + 1, readDh, writeDh);
				recs[i].set(value);
				recs[i].previousPosition();
				++i;
				made = game.changeMove();
			}
			if (i > 0)
				game.undoMove();
			Record best = game.combine(recs, 0, i);
			value.set(best);
			recordList.remove();
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

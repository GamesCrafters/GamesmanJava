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
 * 
 * @param <S>
 *            The state for the game
 */
public class TopDownSolver<S extends State> extends Solver {
	protected boolean containsRemoteness;

	protected RecycleLinkedList<Record[]> recordList;

	public void initialize(final Configuration conf) {
		super.initialize(conf);
		final Game<?> game = conf.getGame();
		recordList = new RecycleLinkedList<Record[]>(new Factory<Record[]>() {
			public Record[] newObject() {
				Record[] retVal = new Record[game.maxChildren()];
				for (int i = 0; i < retVal.length; i++)
					retVal[i] = new Record(conf);
				return retVal;
			}

			public void reset(Record[] t) {
			}
		});
		containsRemoteness = conf.remotenessStates > 0;
	}

	@Override
	public WorkUnit prepareSolve(final Configuration conf) {
		long hashSpace = conf.getGame().numHashes();
		Record defaultRecord = new Record(conf);
		defaultRecord.value = PrimitiveValue.UNDECIDED;
		writeDb.fill(conf.getGame().getRecord(null, defaultRecord), 0,
				hashSpace);

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
		TopDownMutaGame<S> game;
		if (g instanceof TopDownMutaGame<?>) {
			game = Util.checkedCast(g);
		} else {
			game = new TopDownGame<S>(conf, Util
					.<Game<S>, Game<?>> checkedCast(g));
		}
		for (S s : game.startingPositions()) {
			game.setToState(s);
			long currentTimeMillis = System.currentTimeMillis();
			solve(game, s, new Record(conf), 0, readDb.getHandle(), writeDb
					.getHandle());
			System.out.println(Util.millisToETA(System.currentTimeMillis()
					- currentTimeMillis)
					+ " time to complete");
		}
	}

	private void solve(TopDownMutaGame<S> game, S curState, Record value, int depth,
			DatabaseHandle readDh, DatabaseHandle writeDh) {
		if (depth < 3)
			assert Util.debug(DebugFacility.SOLVER, game.toString());
		long hash = game.getHash();
		game.recordFromLong(curState, readDb.getRecord(readDh, hash), value);
		if (value.value != PrimitiveValue.UNDECIDED)
			return;
		PrimitiveValue pv = game.primitiveValue();
		switch (pv) {
		case UNDECIDED:
			Record[] recs = recordList.addFirst();
			boolean made = game.makeMove(curState);
			int i = 0;
			while (made) {
				solve(game, curState, value, depth + 1, readDh, writeDh);
				recs[i].set(value);
				recs[i].previousPosition();
				++i;
				made = game.changeMove(curState);
			}
			if (i > 0)
				game.undoMove(curState);
			Record best = game.combine(recs, 0, i);
			value.set(best);
			recordList.remove();
			break;
		case IMPOSSIBLE:
			Util
					.fatalError("Top-down solve should not reach impossible positions");
		default:
			value.value = pv;
			value.remoteness = 0;
		}
		writeDb.putRecord(writeDh, hash, game.getRecord(curState, value));
	}
}

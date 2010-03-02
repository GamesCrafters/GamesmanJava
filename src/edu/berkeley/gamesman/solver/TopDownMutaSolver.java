package edu.berkeley.gamesman.solver;

import java.util.List;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.util.*;

/**
 * A solver for top-down mutable games
 * 
 * @author dnspies
 * 
 * @param <S>
 *            The state for the game
 */
public class TopDownMutaSolver<S extends State> extends Solver {
	protected boolean containsRemoteness;

	protected QuickLinkedList<Record[]> recordList;

	public void initialize(Configuration conf) {
		super.initialize(conf);
		final TopDownMutaGame<S> game = Util.checkedCast(conf.getGame());
		Record[][] recArray = new Record[game.maxRemoteness() + 1][];
		recordList = new QuickLinkedList<Record[]>(recArray,
				new Factory<Record[]>() {
					public Record[] newElement() {
						Record[] retVal = new Record[game.maxChildren()];
						for (int i = 0; i < retVal.length; i++)
							retVal[i] = game.newRecord();
						return retVal;
					}
				});
		containsRemoteness = conf.remotenessStates > 0;
	}

	@Override
	public WorkUnit prepareSolve(final Configuration conf) {
		long hashSpace = conf.getGame().numHashes();
		Record defaultRecord = conf.getGame().newRecord(
				PrimitiveValue.UNDECIDED);
		writeDb.fill(defaultRecord, 0, hashSpace);

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
		TopDownMutaGame<S> game = Util.checkedCast(conf.getGame());
		for (S s : game.startingPositions()) {
			game.setToState(s);
			long currentTimeMillis = System.currentTimeMillis();
			solve(game, game.newRecord(), 0);
			System.out.println(Util.millisToETA(System.currentTimeMillis()
					- currentTimeMillis)
					+ " time to complete");
		}
	}

	private void solve(TopDownMutaGame<S> game, Record value, int depth) {
		if (depth < 3)
			assert Util.debug(DebugFacility.SOLVER, game.toString());
		long hash = game.getHash();
		readDb.getRecord(hash, value);
		if (value.value != PrimitiveValue.UNDECIDED)
			return;
		PrimitiveValue pv = game.primitiveValue();
		switch (pv) {
		case UNDECIDED:
			Record[] recs = recordList.addFirst();
			boolean made = game.makeMove();
			int i = 0;
			while (made) {
				solve(game, value, depth + 1);
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
			Util
					.fatalError("Top-down solve should not reach impossible positions");
		default:
			value.value = pv;
			if (containsRemoteness)
				value.remoteness = 0;
		}
		writeDb.putRecord(hash, value);
	}
}

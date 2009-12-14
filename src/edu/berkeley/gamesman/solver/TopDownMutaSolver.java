package edu.berkeley.gamesman.solver;

import java.util.Iterator;
import java.util.List;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.util.*;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * A solver for top-down mutable games
 * 
 * @author dnspies
 *
 * @param <State> The state for the game
 */
public class TopDownMutaSolver<State> extends Solver {
	private boolean containsRemoteness;

	private QuickLinkedList<Record[]> recordList;

	public void initialize(Configuration conf) {
		super.initialize(conf);
		final TopDownMutaGame<State> game = Util.checkedCast(conf.getGame());
		Record[][] recArray = new Record[game.maxMoves() + 1][];
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
	 * @param conf The configuration object
	 */
	public void solve(Configuration conf) {
		TopDownMutaGame<State> game = Util.checkedCast(conf.getGame());
		Record[] undecideRecords = new Record[conf.recordsPerGroup];
		Record uRecord = game.newRecord(PrimitiveValue.UNDECIDED);
		for (int i = 0; i < conf.recordsPerGroup; i++)
			undecideRecords[i] = uRecord;
		if (conf.recordGroupUsesLong) {
			final long undecideGroup = RecordGroup.longRecordGroup(conf,
					undecideRecords, 0);
			writeDb
					.putRecordGroups(
							0,
							new LongIterator() {

								public boolean hasNext() {
									return true;
								}

								public long next() {
									return undecideGroup;
								}

							},
							(int) ((game.numHashes() + conf.recordsPerGroup - 1) / conf.recordsPerGroup));
		} else {
			final BigInteger undecideGroup = RecordGroup.bigIntRecordGroup(
					conf, undecideRecords, 0);
			writeDb
					.putRecordGroups(
							0,
							new Iterator<BigInteger>() {

								public boolean hasNext() {
									return true;
								}

								public BigInteger next() {
									return undecideGroup;
								}

								public void remove() {
									throw new UnsupportedOperationException();
								}

							},
							(int) ((game.numHashes() + conf.recordsPerGroup - 1) / conf.recordsPerGroup));

		}
		for (State s : game.startingPositions()) {
			game.setToState(s);
			long currentTimeMillis = System.currentTimeMillis();
			solve(game, game.newRecord(), 0);
			System.out.println(Util.millisToETA(System.currentTimeMillis()
					- currentTimeMillis)
					+ " time to complete");
		}
	}

	private void solve(TopDownMutaGame<State> game, Record value, int depth) {
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

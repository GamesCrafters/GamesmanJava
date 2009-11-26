package edu.berkeley.gamesman.solver;

import java.util.Iterator;
import java.util.List;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.util.*;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

public class TopDownMutaSolver<State> extends Solver {
	private boolean containsRemoteness;

	private QuickLinkedList<Record> recordList;

	private final Record[] tempRecords;

	public TopDownMutaSolver() {
		tempRecords = new Record[2];
	}

	public void initialize(Configuration conf) {
		super.initialize(conf);
		final TopDownMutaGame<State> game = Util.checkedCast(conf.getGame());
		Record[] recArray = new Record[game.maxMoves() + 1];
		recordList = new QuickLinkedList<Record>(recArray,
				new Factory<Record>() {
					public Record newElement() {
						return game.newRecord();
					}
				});
		containsRemoteness = conf.containsField(RecordFields.REMOTENESS);
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
			solve(game, recordList.addFirst(), 0);
			System.out.println(Util.millisToETA(System.currentTimeMillis()
					- currentTimeMillis)
					+ " time to complete");
			recordList.remove();
		}
	}

	private void solve(TopDownMutaGame<State> game, Record value, int depth) {
		if (depth < 3)
			assert Util.debug(DebugFacility.SOLVER, game.toString());
		long hash = game.getHash();
		readDb.getRecord(hash, value);
		if (value.get() != PrimitiveValue.UNDECIDED)
			return;
		PrimitiveValue pv = game.primitiveValue();
		switch (pv) {
		case UNDECIDED:
			Record best = recordList.addFirst();
			boolean made = game.makeMove();
			boolean isFirst = true;
			while (made) {
				solve(game, value, depth + 1);
				if (isFirst) {
					best.set(value);
					isFirst = false;
				} else {
					tempRecords[0] = best;
					tempRecords[1] = value;
					best = game.combine(tempRecords, 0, 2);
				}
				made = game.changeMove();
			}
			if (!isFirst)
				game.undoMove();
			value.set(best);
			recordList.remove();
			break;
		case IMPOSSIBLE:
			Util
					.fatalError("Top-down solve should not reach impossible positions");
		default:
			value.set(RecordFields.VALUE, pv.value());
			if (containsRemoteness)
				value.set(RecordFields.REMOTENESS, 0);
		}
		writeDb.putRecord(game.getHash(), value);
	}
}

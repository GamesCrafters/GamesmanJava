package edu.berkeley.gamesman.solver;

import java.util.List;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.util.Factory;
import edu.berkeley.gamesman.util.QuickLinkedList;
import edu.berkeley.gamesman.util.Util;

public class TopDownMutaSolver<State> extends Solver {
	private boolean containsRemoteness;

	private final QuickLinkedList<Record> recordList;

	private final Record[] tempRecords;

	public TopDownMutaSolver() {
		final TopDownMutaGame<State> game = Util.checkedCast(conf.getGame());
		Record[] recArray = new Record[game.maxMoves()];
		recordList = new QuickLinkedList<Record>(recArray,
				new Factory<Record>() {
					public Record newElement() {
						return game.newRecord();
					}
				});
		tempRecords = new Record[2];
	}

	public void initialize(Configuration conf) {
		super.initialize(conf);
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
		for (State s : game.startingPositions()) {
			game.setToState(s);
			solve(game, recordList.add());
			recordList.remove();
		}
	}

	private void solve(TopDownMutaGame<State> game, Record value) {
		long hash = game.getHash();
		value.set(readDb.getRecord(hash));
		if (value.get() != PrimitiveValue.UNDECIDED)
			return;
		PrimitiveValue pv = game.primitiveValue();
		Record best = recordList.add();
		switch (pv) {
		case UNDECIDED:
			boolean made = game.makeMove();
			boolean isFirst = true;
			while (made) {
				solve(game, value);
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
			break;
		case IMPOSSIBLE:
			Util
					.fatalError("Top-down solve should not reach impossible positions");
		default:
			value.set(RecordFields.VALUE, pv.value());
			if (containsRemoteness)
				value.set(RecordFields.REMOTENESS, 0);
			writeDb.putRecord(game.getHash(), value);
		}
		value.set(best);
		recordList.remove();
	}
}

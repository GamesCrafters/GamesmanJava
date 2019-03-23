package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

/**
 * An implementation of OneTwoTen optimized for the top-down solver
 *
 * @author dnspies
 */
public class OneTwoTenMutable extends TopDownMutaGame {
	private int value;
	private final QuickLinkedList<Integer> moveSequence = new QuickLinkedList<Integer>();

	/**
	 * The default constructor
	 *
	 * @param conf The configuration object
	 */
	public OneTwoTenMutable(Configuration conf) {
		super(conf);
	}

	@Override
	public String displayState() {
		return Integer.toString(value);
	}

	@Override
	public void setFromHash(long hash) {
		value = (int) hash;
	}

	@Override
	public Value primitiveValue() {
		if (value == 10)
			return Value.LOSE;
		else
			return Value.UNDECIDED;
	}

	@Override
	public long getHash() {
		return value;
	}

	@Override
	public void setFromString(String pos) {
		value = Integer.parseInt(pos);
	}

	@Override
	public int makeMove() {
		moveSequence.push(1);
		value++;
		if (value < 10)
			return 2;
		else
			return 1;
	}

	@Override
	public boolean changeMove() {
		if (value == 10 || moveSequence.peek() == 2)
			return false;
		else {
			moveSequence.pop();
			moveSequence.push(2);
			value++;
			return true;
		}
	}

	@Override
	public void undoMove() {
		value -= moveSequence.pop();
	}

	@Override
	public List<String> moveNames() {
		if (value == 9)
			return Collections.singletonList("1");
		else {
			ArrayList<String> moves = new ArrayList<String>(2);
			moves.add("1");
			moves.add("2");
			return moves;
		}
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public void setStartingPosition(int i) {
		value = 0;
	}

	@Override
	public void longToRecord(long record, Record toStore) {
		if (record == 11)
			toStore.value = Value.UNDECIDED;
		else {
			toStore.remoteness = (int) record;
			toStore.value = record % 2 == 0 ? Value.LOSE : Value.WIN;
		}
	}

	@Override
	public long recordToLong(Record fromRecord) {
		if (fromRecord.value == Value.UNDECIDED)
			return 11;
		else
			return fromRecord.remoteness;
	}

	@Override
	public int maxChildren() {
		return 2;
	}

	@Override
	public String describe() {
		return "1,2,...10";
	}

	@Override
	public long numHashes() {
		return 11;
	}

	@Override
	public long recordStates() {
		return 12;
	}

}

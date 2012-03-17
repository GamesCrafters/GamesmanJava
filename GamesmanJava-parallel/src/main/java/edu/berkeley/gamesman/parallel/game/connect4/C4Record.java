package edu.berkeley.gamesman.parallel.game.connect4;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;

public class C4Record implements FixedLengthWritable, Comparable<C4Record> {

	public C4Record() {
	}

	private final GameRecord myRecord = new GameRecord();

	@Override
	public void write(DataOutput out) throws IOException {
		GameValue value = myRecord.getValue();
		switch (value) {
		case TIE:
			out.writeByte(-1);
			break;
		case DRAW:
			out.writeByte(-2);
			break;
		default:
			int remoteness = myRecord.getRemoteness();
			assert value == getWLValue(remoteness);
			out.writeByte(remoteness);
			break;
		}
	}

	private GameValue getWLValue(int remoteness) {
		return (remoteness & 1) != 0 ? GameValue.WIN : GameValue.LOSE;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		int b = in.readByte();
		switch (b) {
		case -1:
			myRecord.set(GameValue.TIE, 0);
			break;
		case -2:
			myRecord.set(GameValue.DRAW);
			break;
		default:
			assert b >= 0;
			myRecord.set(getWLValue(b), b);
			break;
		}
	}

	public void set(GameValue value) {
		myRecord.set(value);
	}

	public void set(GameValue value, int remoteness) {
		myRecord.set(value, remoteness);
	}

	public void set(C4Record other) {
		myRecord.set(other.myRecord);
	}

	@Override
	public int compareTo(C4Record other) {
		return myRecord.compareTo(other.myRecord);
	}

	public void previousPosition(C4Record gr) {
		myRecord.previousPosition(gr.myRecord);
	}

	public GameValue getValue() {
		return myRecord.getValue();
	}

	public int getRemoteness() {
		if (getValue() != GameValue.TIE)
			return myRecord.getRemoteness();
		else
			throw new UnsupportedOperationException(
					"Remoteness can be calculated");
	}

	@Override
	public int size() {
		return 1;
	}
}

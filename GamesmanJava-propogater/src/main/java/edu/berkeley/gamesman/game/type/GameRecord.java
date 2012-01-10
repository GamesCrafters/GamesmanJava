package edu.berkeley.gamesman.game.type;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.propogater.writable.WritableSettable;

public class GameRecord implements Comparable<GameRecord>,
		WritableSettable<GameRecord> {
	public static final GameRecord DRAW = new GameRecord(GameValue.DRAW);
	private GameValue value;
	private int remoteness;

	public GameRecord() {
	}

	public GameRecord(GameValue v) {
		assert !v.hasRemoteness;
		this.value = v;
	}

	public GameRecord(GameValue v, int remoteness) {
		assert v.hasRemoteness;
		this.value = v;
		this.remoteness = remoteness;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(value.ordinal());
		if (value.hasRemoteness)
			out.writeInt(remoteness);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		value = GameValue.valueOf(in.readInt());
		if (value.hasRemoteness)
			remoteness = in.readInt();
	}

	@Override
	public void set(GameRecord t) {
		value = t.value;
		if (value.hasRemoteness)
			remoteness = t.remoteness;
	}

	@Override
	public int compareTo(GameRecord o) {
		int c1 = value.compareTo(o.value);
		if (c1 == 0 && value.hasRemoteness)
			return value.compareTo(GameValue.DRAW) > 0 ? o.remoteness
					- remoteness : remoteness - o.remoteness;
		else
			return c1;
	}

	public void combineWith(GameRecord t) {
		if (compareTo(t) < 0)
			set(t);
	}

	public void set(GameValue value) {
		if (value.hasRemoteness)
			throw new RuntimeException("Use other set with remoteness");
		this.value = value;
	}

	public void set(GameValue value, int remoteness) {
		if (!value.hasRemoteness)
			throw new RuntimeException("Use other set without remoteness");
		this.value = value;
		this.remoteness = remoteness;
	}

	public void previousPosition(GameRecord tVal) {
		value = tVal.value.opposite();
		if (value.hasRemoteness)
			remoteness = tVal.remoteness + 1;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GameRecord && equals((GameRecord) other);
	}

	public boolean equals(GameRecord other) {
		return value == other.value
				&& (!value.hasRemoteness || remoteness == other.remoteness);
	}

	@Override
	public int hashCode() {
		return (31 + value.hashCode()) * 31 + remoteness;
	}

	@Override
	public String toString() {
		return value == null ? "null" : (value + (value.hasRemoteness ? " in "
				+ remoteness : ""));
	}

	public GameValue getValue() {
		return value;
	}

	public int getRemoteness() {
		return remoteness;
	}

	public void previousPosition() {
		previousPosition(this);
	}
}

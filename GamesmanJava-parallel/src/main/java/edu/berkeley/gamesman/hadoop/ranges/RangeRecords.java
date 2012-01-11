package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;
import edu.berkeley.gamesman.propogater.writable.list.WritableArray;

public class RangeRecords implements WritableSettable<RangeRecords> {
	private WritableArray<GameRecord> arr = new WritableArray<GameRecord>(
			GameRecord.class, null);

	@Override
	public void readFields(DataInput in) throws IOException {
		arr.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		arr.write(out);
	}

	@Override
	public void set(RangeRecords t) {
		arr.set(t.arr);
	}

	@Override
	public String toString() {
		return arr.toString();
	}

	public void setLength(int numPositions) {
		arr.setLength(numPositions);
	}

	public GameRecord setHasAndGet(int i) {
		return arr.setHasAndGet(i);
	}

	public int numPositions() {
		return arr.length();
	}

	public GameRecord get(int i) {
		return arr.get(i);
	}

	public void set(int i, GameRecord rec) {
		arr.set(i, rec);
	}

	public boolean hasValue(int i) {
		return arr.hasValue(i);
	}
}

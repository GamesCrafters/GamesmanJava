package edu.berkeley.gamesman.hadoop.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;
import edu.berkeley.gamesman.propogater.writable.list.WritableArray;
import edu.berkeley.gamesman.propogater.writable.list.WritableTreeMap;

public class RangeRecords implements WritableSettable<RangeRecords> {
	static final boolean ARRAY = false, MAP = true;
	private WritableArray<GameRecord> arr = new WritableArray<GameRecord>(
			GameRecord.class, null);
	private WritableTreeMap<GameRecord> map = new WritableTreeMap<GameRecord>(
			GameRecord.class, null);
	private boolean type;

	@Override
	public void readFields(DataInput in) throws IOException {
		type = in.readBoolean();
		if (type == ARRAY)
			arr.readFields(in);
		else if (type == MAP)
			map.readFields(in);
		else
			throw new Error("WTF!!??");
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeBoolean(type);
		if (type == ARRAY)
			arr.write(out);
		else if (type == MAP)
			map.write(out);
		else
			throw new Error("WTF!!??");
	}

	@Override
	public void set(RangeRecords t) {
		type = t.type;
		if (type == ARRAY)
			arr.set(t.arr);
		else if (type == MAP)
			map.set(t.map);
		else
			throw new Error("WTF!!??");
	}

	@Override
	public String toString() {
		if (type == ARRAY)
			return arr.toString();
		else if (type == MAP)
			return map.toString();
		else
			return "WTF!!??";
	}

	public void clear(int numPositions, boolean type) {
		this.type = type;
		if (type == ARRAY)
			arr.setLength(numPositions);
		else if (type == MAP)
			map.clear();
		else
			throw new Error("WTF!!??");
	}

	public GameRecord setHasAndGet(int i) {
		if (type == ARRAY)
			return arr.setHasAndGet(i);
		else if (type == MAP)
			throw new UnsupportedOperationException();
		else
			throw new Error("WTF!!??");
	}

	public int numPositions() {
		if (type == ARRAY)
			return arr.length();
		else if (type == MAP)
			throw new UnsupportedOperationException();
		else
			throw new Error("WTF!!??");
	}

	public GameRecord get(int i) {
		if (type == ARRAY)
			return arr.get(i);
		else if (type == MAP)
			return map.get(i);
		else
			throw new Error("WTF!!??");
	}

	public void set(int i, GameRecord rec) {
		if (type == ARRAY)
			arr.set(i, rec);
		else if (type == MAP)
			throw new UnsupportedOperationException();
		else
			throw new Error("WTF!!??");
	}

	public GameRecord add(int i) {
		if (type == ARRAY)
			throw new UnsupportedOperationException();
		else if (type == MAP)
			return map.add(i);
		else
			throw new Error("WTF!!??");
	}
}

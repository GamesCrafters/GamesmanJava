package edu.berkeley.gamesman.propogater.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.factory.FactoryUtil;

public final class IntEntry<KEY extends WritableSettable<KEY>> implements
		WritableSettable<IntEntry<KEY>> {
	private int whichNum;
	private boolean seen = false;
	private final KEY parent;

	public IntEntry(Class<? extends KEY> keyClass, Configuration conf) {
		this(FactoryUtil.makeFactory(keyClass, conf));
	}

	public IntEntry(Factory<KEY> keyFact) {
		parent = keyFact.create();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(whichNum);
		out.writeBoolean(seen);
		parent.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		whichNum = in.readInt();
		seen = in.readBoolean();
		parent.readFields(in);
	}

	@Override
	public void set(IntEntry<KEY> t) {
		setOther(t);
	}

	public void setOther(IntEntry<? extends KEY> t) {
		whichNum = t.whichNum;
		seen = t.seen;
		parent.set(t.parent);
	}

	public void set(int i, KEY key) {
		whichNum = i;
		seen = false;
		parent.set(key);
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof IntEntry) && equals((IntEntry<?>) other);
	}

	public boolean equals(IntEntry<?> other) {
		return whichNum == other.whichNum && seen == other.seen
				&& parent.equals(other.parent);
	}

	@Override
	public int hashCode() {
		return ((whichNum + 31) * 31 + Boolean.valueOf(seen).hashCode()) * 31
				+ parent.hashCode();
	}

	@Override
	public String toString() {
		return whichNum + " : " + parent.toString()
				+ (seen ? " is seen" : " is not seen");
	}

	public KEY getKey() {
		return parent;
	}

	public int getInt() {
		return whichNum;
	}

	public boolean seen() {
		boolean s = seen;
		seen = true;
		return s;
	}

	public void setInt(int i) {
		whichNum = i;
	}
}

package edu.berkeley.gamesman.propogater.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.factory.FactoryUtil;

public final class IntEntry<KEY extends Writable> implements Writable {
	private int whichNum;
	private KEY parent;

	public IntEntry(KEY parent) {
		this.parent = parent;
	}

	public IntEntry(Class<? extends KEY> keyClass, Configuration conf) {
		this(FactoryUtil.makeFactory(keyClass, conf));
	}

	public IntEntry(Factory<KEY> keyFact) {
		this(keyFact.create());
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(whichNum);
		parent.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		whichNum = in.readInt();
		parent.readFields(in);
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof IntEntry) && equals((IntEntry<?>) other);
	}

	public boolean equals(IntEntry<?> other) {
		return whichNum == other.whichNum && parent.equals(other.parent);
	}

	@Override
	public int hashCode() {
		return (whichNum + 31) * 31 + parent.hashCode();
	}

	@Override
	public String toString() {
		return whichNum + " : " + parent.toString();
	}

	public KEY getKey() {
		return parent;
	}

	public int getInt() {
		return whichNum;
	}

	public void setInt(int i) {
		whichNum = i;
	}
}

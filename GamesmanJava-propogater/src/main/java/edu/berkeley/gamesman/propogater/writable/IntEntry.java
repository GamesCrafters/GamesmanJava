package edu.berkeley.gamesman.propogater.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.factory.FactoryUtil;

public final class IntEntry<V extends Writable> implements Writable, Resetable {
	private int key;
	private V value;

	public IntEntry(V parent) {
		this.value = parent;
	}

	public IntEntry(Class<? extends V> keyClass, Configuration conf) {
		this(FactoryUtil.makeFactory(keyClass, conf));
	}

	public IntEntry(Factory<V> keyFact) {
		this(keyFact.create());
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(key);
		value.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		key = in.readInt();
		value.readFields(in);
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof IntEntry) && equals((IntEntry<?>) other);
	}

	public boolean equals(IntEntry<?> other) {
		return key == other.key && value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return (key + 31) * 31 + value.hashCode();
	}

	@Override
	public String toString() {
		return key + " : " + value.toString();
	}

	public V getValue() {
		return value;
	}

	public int getKey() {
		return key;
	}

	public void setKey(int i) {
		key = i;
	}

	public void swapValues(Entry<?, V> entry) {
		V temp = value;
		value = entry.getValue();
		entry.setValue(temp);
	}

	@Override
	public void reset() {
		Resetables.reset(value);
	}

	@Override
	public boolean checkReset() {
		return Resetables.checkReset(value);
	}
}

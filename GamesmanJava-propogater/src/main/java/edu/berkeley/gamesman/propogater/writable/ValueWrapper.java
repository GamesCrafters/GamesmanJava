package edu.berkeley.gamesman.propogater.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.ReflectionUtils;

public class ValueWrapper<V extends Writable> implements Writable {
	private boolean hasValue;
	private final V myVal;

	public ValueWrapper(Class<? extends V> vClass, Configuration conf) {
		myVal = ReflectionUtils.newInstance(vClass, conf);
		hasValue = false;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		hasValue = in.readBoolean();
		if (hasValue)
			myVal.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeBoolean(hasValue);
		if (hasValue)
			myVal.write(out);
	}

	public V get() {
		if (hasValue)
			return myVal;
		else
			return null;
	}

	public V setHasAndGet() {
		hasValue = true;
		return myVal;
	}

	public boolean hasValue() {
		return hasValue;
	}

	public void clear() {
		hasValue = false;
	}

	@Override
	public String toString() {
		return hasValue ? myVal.toString() : "NONE";
	}
}

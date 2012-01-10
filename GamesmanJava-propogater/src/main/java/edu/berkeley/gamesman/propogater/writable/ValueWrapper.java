package edu.berkeley.gamesman.propogater.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.factory.FactoryUtil;

public class ValueWrapper<VALUE extends WritableSettable<VALUE>> implements
		WritableSettable<ValueWrapper<VALUE>> {

	private boolean hasValue = true;
	private final VALUE myValue;

	public ValueWrapper(Factory<? extends VALUE> valFact) {
		myValue = valFact.create();
	}

	public ValueWrapper(Class<? extends VALUE> valClass, Configuration conf) {
		this(FactoryUtil.makeFactory(valClass, conf));
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeBoolean(hasValue);
		if (hasValue)
			myValue.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		hasValue = in.readBoolean();
		if (hasValue)
			myValue.readFields(in);
	}

	@Override
	public void set(ValueWrapper<VALUE> t) {
		setOther(t);
	}

	public void setOther(ValueWrapper<? extends VALUE> t) {
		hasValue = t.hasValue;
		if (hasValue)
			myValue.set(t.myValue);
	}

	public boolean hasValue() {
		return hasValue;
	}

	public void clear() {
		hasValue = false;
	}

	public void set(VALUE value) {
		hasValue = value != null;
		if (hasValue)
			myValue.set(value);
	}

	public VALUE get() {
		return hasValue ? myValue : null;
	}

	public VALUE setHasAndGet() {
		hasValue = true;
		return myValue;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof ValueWrapper)
				&& equals((ValueWrapper<?>) other);
	}

	public boolean equals(ValueWrapper<?> other) {
		return hasValue ? myValue.equals(other.get()) : !other.hasValue;
	}

	@Override
	public int hashCode() {
		return hasValue ? myValue.hashCode() : 0;
	}

	@Override
	public String toString() {
		return hasValue ? myValue.toString() : "NULL";
	}
}

package edu.berkeley.gamesman.propogater.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.ReflectionUtils;

public class Entry<K extends Writable, V extends Writable> implements Writable {
	private K key;
	private V value;

	private K realKey = null;
	private V realValue = null;

	public Entry(Class<? extends K> kClass, Class<? extends V> vClass,
			Configuration conf) {
		key = ReflectionUtils.newInstance(kClass, conf);
		value = ReflectionUtils.newInstance(vClass, conf);
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		key.readFields(in);
		value.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		key.write(out);
		value.write(out);
	}

	public void setDummy(K key, V value) {
		setDummyKey(key);
		setDummyValue(value);
	}

	public void setDummyValue(V value) {
		if (realValue != null)
			throw new RuntimeException("Already a dummy value! Revert first");
		realValue = this.value;
		this.value = value;
	}

	public void setDummyKey(K key) {
		if (realKey != null)
			throw new RuntimeException("Already a dummy key! Revert first");
		realKey = this.key;
		this.key = key;
	}

	public void revert() {
		revertKey();
		revertValue();
	}

	public void revertValue() {
		this.value = realValue;
		realValue = null;
	}

	public void revertKey() {
		this.key = realKey;
		realKey = null;
	}

	@Override
	public String toString() {
		return Arrays.toString(new Object[] { key, value });
	}
}

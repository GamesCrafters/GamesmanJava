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

	public Entry(Class<? extends K> kClass, Class<? extends V> vClass,
			Configuration conf) {
		key = ReflectionUtils.newInstance(kClass, conf);
		value = ReflectionUtils.newInstance(vClass, conf);
	}

	public Entry() {
		key = null;
		value = null;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}

	public void setKey(K key) {
		this.key = key;
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

	@Override
	public String toString() {
		return Arrays.toString(new Object[] { key, value });
	}

	public void swapKeys(Entry<K, ?> other) {
		K temp = key;
		key = other.key;
		other.key = temp;
	}

	public void swapValues(Entry<?, V> other) {
		V temp = value;
		value = other.value;
		other.value = temp;
	}

	public void swap(Entry<K, V> other) {
		swapKeys(other);
		swapValues(other);
	}
}

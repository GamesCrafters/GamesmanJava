package edu.berkeley.gamesman.propogater.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.factory.FactoryUtil;

public class Entry<K extends Writable, V extends Writable> implements Writable,
		Resetable {
	private K key;
	private V value;

	public Entry(Class<? extends K> kClass, Class<? extends V> vClass,
			Configuration conf) {
		this(FactoryUtil.makeFactory(kClass, conf), FactoryUtil.makeFactory(
				vClass, conf));
	}

	public Entry() {
		key = null;
		value = null;
	}

	public Entry(Factory<K> kFactory, Factory<V> vFactory) {
		key = kFactory.create();
		value = vFactory.create();
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

	@Override
	public void reset() {
		Resetables.reset(key);
		Resetables.reset(value);
	}

	@Override
	public boolean checkReset() {
		return Resetables.checkReset(key) && Resetables.checkReset(value);
	}
}

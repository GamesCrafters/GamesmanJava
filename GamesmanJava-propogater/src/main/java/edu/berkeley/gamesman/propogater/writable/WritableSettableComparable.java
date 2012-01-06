package edu.berkeley.gamesman.propogater.writable;

import org.apache.hadoop.io.WritableComparable;


public interface WritableSettableComparable<T> extends WritableSettable<T>,
		WritableComparable<T> {
}

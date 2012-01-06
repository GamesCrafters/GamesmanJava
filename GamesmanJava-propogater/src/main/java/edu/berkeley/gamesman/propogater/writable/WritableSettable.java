package edu.berkeley.gamesman.propogater.writable;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.common.Settable;



public interface WritableSettable<T> extends Writable, Settable<T> {
}

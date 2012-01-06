package edu.berkeley.gamesman.propogater.writable;

import edu.berkeley.gamesman.propogater.common.Combinable;

public interface WritableSettableCombinable<T> extends WritableSettable<T>,
		Combinable<T> {
}

package edu.berkeley.gamesman.parallel.game.baghchal;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

public class BaghChalState<T extends BaghChalState<T>> implements
		WritableSettableComparable<T> {

	public BaghChalState(int width, int height, int tigers, int goats) {

	}

	@Override
	public void readFields(DataInput in) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void write(DataOutput out) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void set(T t) {
		// TODO Auto-generated method stub

	}

	@Override
	public int compareTo(T o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean equals(Object other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}
}

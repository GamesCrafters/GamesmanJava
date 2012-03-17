package edu.berkeley.gamesman.parallel.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;

public class FLIntWritable implements FixedLengthWritable {
	private int myVal;

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(myVal);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		myVal = in.readInt();
	}

	@Override
	public int size() {
		return 4;
	}

	public void set(int i) {
		myVal = i;
	}

	public int get() {
		return myVal;
	}

}

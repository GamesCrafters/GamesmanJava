package edu.berkeley.gamesman.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;

import org.apache.hadoop.io.WritableComparable;

public class BigIntegerWritable implements WritableComparable<BigIntegerWritable> {

	protected BigInteger value;
	
	public void readFields(DataInput in) throws IOException {
		byte[] data = new byte[in.readInt()];
		in.readFully(data);
		value = new BigInteger(data);
	}

	public void write(DataOutput out) throws IOException {
		byte[] data = value.toByteArray();
		out.writeInt(data.length);
		out.write(data, 0, data.length);
	}

	public int compareTo(BigIntegerWritable other) {
		return value.compareTo(other.value);
	}

}

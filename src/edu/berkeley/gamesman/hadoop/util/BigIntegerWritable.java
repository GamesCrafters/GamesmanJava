package edu.berkeley.gamesman.hadoop.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;

import org.apache.hadoop.io.WritableComparable;

/**
 * A BigIntegerWritable is a Hadoop WritableComparable much in the
 * same idea as an IntWritable or TextWritable
 * 
 * @see org.apache.hadoop.io.WritableComparable
 * @author Steven Schlansker
 */
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
	
	/**
	 * Set this Writable
	 * @param v the BigInteger to store
	 */
	public void set(BigInteger v){
		value = v;
	}
	
	/**
	 * @return the stored BigInteger
	 */
	public BigInteger get(){
		return value;
	}
	
	public String toString(){
		return value.toString();
	}

}

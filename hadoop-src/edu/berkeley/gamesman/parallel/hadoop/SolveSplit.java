package edu.berkeley.gamesman.parallel.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.mapred.InputSplit;

public class SolveSplit implements InputSplit {
	private final static String[] hosts = new String[0];

	long first;
	long num;

	public SolveSplit() {
	}

	public SolveSplit(long first, int num) {
		this.first = first;
		this.num = num;
	}

	public long getLength() throws IOException {
		return 16L;
	}

	public String[] getLocations() throws IOException {
		return hosts;
	}

	public void readFields(DataInput in) throws IOException {
		first = in.readLong();
		num = in.readLong();
	}

	public void write(DataOutput out) throws IOException {
		out.writeLong(first);
		out.writeLong(num);
	}
}

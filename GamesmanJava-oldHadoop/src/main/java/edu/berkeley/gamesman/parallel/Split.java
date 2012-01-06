package edu.berkeley.gamesman.parallel;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: Dan Date: Nov 30, 2010 Time: 10:14:16 AM To
 * change this template use File | Settings | File Templates.
 */
public class Split extends InputSplit implements Writable {
	private final static String[] hosts = new String[0];
	public final Range r = new Range();

	public Split() {
	}

	public Split(Range r) {
		this.r.set(r);
	}

	@Override
	public long getLength() throws IOException {
		return r.numRecords;
	}

	@Override
	public String[] getLocations() throws IOException {
		return hosts;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		r.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		r.readFields(in);
	}
}

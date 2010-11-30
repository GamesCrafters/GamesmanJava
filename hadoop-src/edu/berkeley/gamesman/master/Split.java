package edu.berkeley.gamesman.master;


import org.apache.hadoop.mapred.InputSplit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: Nov 30, 2010
 * Time: 10:14:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class Split implements InputSplit {
	private final static String[] hosts = new String[0];
	long first;
	long num;				//CHANGE TO LONG since HASHES IS LONG VAL
	public Split() {
	}
	public Split(long first, long num) {
		this.first = first;
		this.num = num;
	}
	public long getLength() throws IOException {
		return num;
	}
	public String[] getLocations() throws IOException {
		return hosts;
	}
	public void readFields(DataInput in) throws IOException {
		first = in.readLong();
		num = in.readInt();
	}
	public void write(DataOutput out) throws IOException {
		out.writeLong(first);
		out.writeLong(num);
	}
}

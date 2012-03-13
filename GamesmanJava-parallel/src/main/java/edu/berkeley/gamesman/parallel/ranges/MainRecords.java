package edu.berkeley.gamesman.parallel.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class MainRecords<GR extends Writable> extends Configured implements
		Writable {
	private WritableList<GR> myList;

	public MainRecords() {
	}

	public MainRecords(Configuration conf) {
		super(conf);
	}

	@Override
	public void setConf(Configuration conf) {
		super.setConf(conf);
		if (conf == null)
			return;
		myList = new WritableList<GR>(RangeTree.<GR> getRunGRClass(conf), conf);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		myList.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		myList.readFields(in);
	}

	public void clear() {
		myList.clear();
	}

	public GR add() {
		return myList.add();
	}

	public int length() {
		return myList.length();
	}

	public GR get(int pos) {
		return myList.get(pos);
	}

	public boolean isEmpty() {
		return myList.isEmpty();
	}
}

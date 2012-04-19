package edu.berkeley.gamesman.parallel.ranges;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;
import edu.berkeley.gamesman.propogater.writable.list.FLWritList;

public class MainRecords<GR extends FixedLengthWritable> extends Configured
		implements Writable {
	private FLWritList<GR> myList;

	public MainRecords() {
	}

	public MainRecords(Configuration conf) {
		super(conf);
	}

	@Override
	public void setConf(Configuration conf) {
		if (conf == getConf())
			return;
		else if (conf == null)
			return;
		super.setConf(conf);
		myList = new FLWritList<GR>(ReflectionUtils.newInstance(
				RangeTree.<GR> getRunGRClass(conf), conf));
	}

	@Override
	public void write(DataOutput out) throws IOException {
		myList.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		myList.readFields(in);
	}

	public void add(GR gr) {
		myList.add(gr);
	}

	public int length() {
		return myList.length();
	}

	public GR get(int pos) {
		return myList.get(pos);
	}

	public void writeBack(int pos, GR value) {
		myList.writeBack(pos, value);
	}

	public void reset(boolean adding) {
		myList.reset(adding);
	}

	public void ensureSize(int records) {
		myList.ensureSize(records);
	}

	public void setCopyOfRange(MainRecords<? extends GR> other, int offset,
			int size) {
		myList.setCopyOfRange(other.myList, offset, size);
	}
}

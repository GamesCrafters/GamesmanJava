package edu.berkeley.gamesman.parallel.ranges;

import org.apache.hadoop.io.IntWritable;

import edu.berkeley.gamesman.propogater.writable.list.WritableTreeMap;

public class ChildMap extends WritableTreeMap<IntWritable> {
	public ChildMap() {
		super(IntWritable.class, null);
	}
}

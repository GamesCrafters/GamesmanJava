package edu.berkeley.gamesman.parallel.ranges;

import org.apache.hadoop.io.IntWritable;

import edu.berkeley.gamesman.propogater.writable.list.WritableTreeMap;

public class ChildMap extends WritableTreeMap<IntWritable> {
	public ChildMap() {
		super(IntWritable.class, null);
	}

	public void set(ChildMap other) {
		clear();
		other.restart();
		int i = other.peekNext();
		while (i >= 0) {
			IntWritable x = add(i);
			x.set(other.getNext(i).get());
			i = other.peekNext();
		}
	}
}

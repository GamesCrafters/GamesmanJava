package edu.berkeley.gamesman.parallel.ranges;

import org.apache.hadoop.io.IntWritable;

import edu.berkeley.gamesman.parallel.writable.WritableTreeMap;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class ChildMap extends WritableTreeMap<IntWritable> {
	public ChildMap(QLLFactory<IntEntry<IntWritable>> fact,
			Pool<IntEntry<IntWritable>> pool) {
		super(fact, pool);
	}
}

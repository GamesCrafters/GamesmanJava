package edu.berkeley.gamesman.parallel.ranges;

import org.apache.hadoop.io.IntWritable;

import edu.berkeley.gamesman.parallel.writable.WritableTreeMap;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class ChildMap extends WritableTreeMap<IntWritable> {
	private static final QLLFactory<IntEntry<IntWritable>> fact = new QLLFactory<IntEntry<IntWritable>>();
	private static final Pool<IntEntry<IntWritable>> pool = new Pool<IntEntry<IntWritable>>(
			new Factory<IntEntry<IntWritable>>() {
				@Override
				public IntEntry<IntWritable> newObject() {
					return new IntEntry<IntWritable>(new IntWritable());
				}

				@Override
				public void reset(IntEntry<IntWritable> t) {
				}
			});

	public ChildMap() {
		super(fact, pool);
	}

}

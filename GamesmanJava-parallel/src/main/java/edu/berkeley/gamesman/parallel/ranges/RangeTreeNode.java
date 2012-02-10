package edu.berkeley.gamesman.parallel.ranges;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.tree.SimpleTreeNode;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class RangeTreeNode<S extends GenState> extends
		SimpleTreeNode<Range<S>, MainRecords, ChildMap, RecordMap> {
	@Override
	protected Factory<ChildMap> makePIFactory(Configuration conf) {
		return new Factory<ChildMap>() {
			private final QLLFactory<IntEntry<IntWritable>> fact = new QLLFactory<IntEntry<IntWritable>>();
			private final Pool<IntEntry<IntWritable>> pool = new Pool<IntEntry<IntWritable>>(
					new edu.berkeley.gamesman.util.qll.Factory<IntEntry<IntWritable>>() {
						@Override
						public IntEntry<IntWritable> newObject() {
							return new IntEntry<IntWritable>(new IntWritable());
						}

						@Override
						public void reset(IntEntry<IntWritable> t) {

						}
					});

			@Override
			public ChildMap create() {
				return new ChildMap(fact, pool);
			}
		};
	}

	@Override
	protected Factory<RecordMap> makeCIFactory(Configuration conf) {
		return new Factory<RecordMap>() {
			private final QLLFactory<IntEntry<GameRecord>> fact = new QLLFactory<IntEntry<GameRecord>>();
			private final Pool<IntEntry<GameRecord>> pool = new Pool<IntEntry<GameRecord>>(
					new edu.berkeley.gamesman.util.qll.Factory<IntEntry<GameRecord>>() {
						@Override
						public IntEntry<GameRecord> newObject() {
							return new IntEntry<GameRecord>(new GameRecord());
						}

						@Override
						public void reset(IntEntry<GameRecord> t) {

						}
					});

			@Override
			public RecordMap create() {
				return new RecordMap(fact, pool);
			}
		};
	}
}

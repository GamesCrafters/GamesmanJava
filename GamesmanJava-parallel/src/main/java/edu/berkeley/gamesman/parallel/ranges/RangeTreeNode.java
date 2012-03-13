package edu.berkeley.gamesman.parallel.ranges;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.parallel.writable.WritableTreeMap;
import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.tree.SimpleTreeNode;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class RangeTreeNode<S extends GenState, GR extends Writable>
		extends
		SimpleTreeNode<Suffix<S>, MainRecords<GR>, ChildMap, WritableTreeMap<GR>> {
	private Class<? extends GR> grClass;

	@Override
	protected void treeNodeConfigure(Configuration conf) {
		grClass = RangeTree.getRunGRClass(conf);
	}

	@Override
	protected Factory<ChildMap> makePIFactory(Configuration conf) {
		return new Factory<ChildMap>() {
			private final QLLFactory<IntWritable> fact = new QLLFactory<IntWritable>();
			private final Pool<IntWritable> pool = newPoolI();

			@Override
			public ChildMap create() {
				return new ChildMap(fact, pool);
			}
		};
	}

	private Pool<IntWritable> newPoolI() {
		return new Pool<IntWritable>(
				new edu.berkeley.gamesman.util.qll.Factory<IntWritable>() {
					@Override
					public IntWritable newObject() {
						return new IntWritable();
					}

					@Override
					public void reset(IntWritable t) {

					}
				});
	}

	@Override
	protected Factory<WritableTreeMap<GR>> makeCIFactory(
			final Configuration conf) {
		return new Factory<WritableTreeMap<GR>>() {
			private final QLLFactory<IntWritable> facti = new QLLFactory<IntWritable>();
			private final Pool<IntWritable> pooli = newPoolI();
			private final QLLFactory<GR> fact = new QLLFactory<GR>();
			private final Pool<GR> pool = new Pool<GR>(
					new edu.berkeley.gamesman.util.qll.Factory<GR>() {
						@Override
						public GR newObject() {
							return ReflectionUtils.newInstance(grClass, conf);
						}

						@Override
						public void reset(GR t) {

						}
					});

			@Override
			public WritableTreeMap<GR> create() {
				return new WritableTreeMap<GR>(facti, pooli, fact, pool);
			}
		};
	}
}

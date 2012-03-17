package edu.berkeley.gamesman.parallel.ranges;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ReflectionUtils;

import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.parallel.writable.WritableTreeMap;
import edu.berkeley.gamesman.propogater.factory.Factory;
import edu.berkeley.gamesman.propogater.tree.SimpleTreeNode;
import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;
import edu.berkeley.gamesman.propogater.writable.Resetables;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class RangeTreeNode<S extends GenState, GR extends FixedLengthWritable>
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

			@Override
			public ChildMap create() {
				return new ChildMap();
			}

			@Override
			public void reset(ChildMap obj) {
			}
		};
	}

	@Override
	protected Factory<WritableTreeMap<GR>> makeCIFactory(
			final Configuration conf) {
		return new Factory<WritableTreeMap<GR>>() {

			@Override
			public WritableTreeMap<GR> create() {
				return new WritableTreeMap<GR>(ReflectionUtils.newInstance(
						grClass, conf));
			}

			@Override
			public void reset(WritableTreeMap<GR> obj) {
			}
		};
	}
}

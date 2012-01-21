package edu.berkeley.gamesman.propogater.tree;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import edu.berkeley.gamesman.propogater.factory.Factory;

public class SimpleTreeNode<K extends WritableComparable<K>, V extends Writable, PI extends Writable, CI extends Writable>
		extends TreeNode<K, V, PI, CI, CI, PI> {
	@Override
	protected final Factory<CI> makeUMFactory(Configuration conf) {
		return makeCIFactory(conf);
	}

	@Override
	public final Factory<PI> makeDMFactory(Configuration conf) {
		return makePIFactory(conf);
	}
}

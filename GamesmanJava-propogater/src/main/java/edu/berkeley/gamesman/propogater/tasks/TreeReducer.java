package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.TreeNode;

public class TreeReducer<K extends WritableComparable<K>, V extends Writable, PI extends Writable, UM extends Writable, CI extends Writable, DM extends Writable>
		extends
		Reducer<K, TreeNode<K, V, PI, UM, CI, DM>, K, TreeNode<K, V, PI, UM, CI, DM>> {
	protected TreeNode<K, V, PI, UM, CI, DM> value;

	protected void setup(Context context) throws IOException,
			InterruptedException {
		Tree<K, V, PI, UM, CI, DM> tree = ConfParser.newTree(context
				.getConfiguration());
		value = tree.newNode();
	}

	@Override
	protected void reduce(K key,
			Iterable<TreeNode<K, V, PI, UM, CI, DM>> values, Context context)
			throws IOException, InterruptedException {
		value.clearMixes();
		for (TreeNode<K, V, PI, UM, CI, DM> node : values)
			value.combineWith(node);
		combine(key, value);
		context.write(key, value);
	}

	protected void combine(K key, TreeNode<K, V, PI, UM, CI, DM> value) {
	}
}

package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.propogater.tree.TreeNode;

public class TreeReducer<K extends WritableComparable<K>, V extends Writable, PI extends Writable, UM extends Writable, CI extends Writable, DM extends Writable>
		extends
		Reducer<K, TreeNode<K, V, PI, UM, CI, DM>, K, TreeNode<K, V, PI, UM, CI, DM>> {
	protected TreeNode<K, V, PI, UM, CI, DM> value;

	// This is heavy abuse of abstraction barriers, but hadoop gives no other
	// way to do this without unnecessary copying
	@Override
	protected void reduce(K key,
			Iterable<TreeNode<K, V, PI, UM, CI, DM>> values, Context context)
			throws IOException, InterruptedException {
		Iterator<TreeNode<K, V, PI, UM, CI, DM>> iter = values.iterator();
		value = iter.next();
		value.beginCombine();
		try {
			while (iter.hasNext()) {
				TreeNode<K, V, PI, UM, CI, DM> nextValue = iter.next();
				assert value == nextValue;
			}
		} finally {
			value.endCombine();
		}
		int newParentsStart = value.getParentList().length();
		combine(key, value);
		try {
			context.write(key, value);
		} finally {
			revertDummies(value, newParentsStart);
		}
	}

	protected void combine(K key, TreeNode<K, V, PI, UM, CI, DM> value) {
	}

	protected void revertDummies(TreeNode<K, V, PI, UM, CI, DM> value,
			int newParentsStart) {
	}
}

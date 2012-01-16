package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.propogater.tree.TreeNode;

public class CleanupMapper<K extends WritableComparable<K>, V extends Writable>
		extends Mapper<K, TreeNode<K, V, ?, ?, ?, ?>, K, V> {
	@Override
	protected void map(K key, TreeNode<K, V, ?, ?, ?, ?> node, Context context)
			throws IOException, InterruptedException {
		context.write(key, node.getValue());
	}
}

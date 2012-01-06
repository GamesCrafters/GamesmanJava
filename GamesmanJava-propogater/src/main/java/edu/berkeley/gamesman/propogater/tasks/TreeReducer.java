package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.propogater.tree.node.TreeNode;
import edu.berkeley.gamesman.propogater.writable.WritableSettableCombinable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;


public class TreeReducer<KEY extends WritableSettableComparable<KEY>, VALUE extends WritableSettableCombinable<VALUE>>
		extends Reducer<KEY, TreeNode<KEY, VALUE>, KEY, TreeNode<KEY, VALUE>> {
	protected TreeNode<KEY, VALUE> curNode;

	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {
		Configuration conf = context.getConfiguration();
		curNode = new TreeNode<KEY, VALUE>(conf);
	}

	@Override
	protected void reduce(KEY key, Iterable<TreeNode<KEY, VALUE>> values,
			Context context) throws IOException, InterruptedException {
		curNode.clear();
		for (TreeNode<KEY, VALUE> value : values)
			curNode.combineWith(value);
		context.write(key, curNode);
	}
}

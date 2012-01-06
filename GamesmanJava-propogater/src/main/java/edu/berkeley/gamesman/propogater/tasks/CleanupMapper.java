package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;

import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.propogater.tree.node.TreeNode;
import edu.berkeley.gamesman.propogater.writable.WritableSettableCombinable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;


public class CleanupMapper<KEY extends WritableSettableComparable<KEY>, VALUE extends WritableSettableCombinable<VALUE>>
		extends Mapper<KEY, TreeNode<KEY, VALUE>, KEY, VALUE> {
	@Override
	protected void map(KEY key, TreeNode<KEY, VALUE> node, Context context)
			throws IOException, InterruptedException {
		context.write(key, node.getValue());
	}
}

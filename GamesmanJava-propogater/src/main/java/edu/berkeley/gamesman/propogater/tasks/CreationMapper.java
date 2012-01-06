package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.node.TreeNode;
import edu.berkeley.gamesman.propogater.writable.WritableSettableCombinable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;


public class CreationMapper<KEY extends WritableSettableComparable<KEY>, VALUE extends WritableSettableCombinable<VALUE>>
		extends Mapper<KEY, TreeNode<KEY, VALUE>, KEY, TreeNode<KEY, VALUE>> {
	private Tree<KEY, VALUE> tree;
	private TreeNode<KEY, VALUE> parNode;
	private WritableList<KEY> children;

	@Override
	protected void setup(Context context) {
		Configuration conf = context.getConfiguration();
		tree = ConfParser.<KEY, VALUE> getTree(conf);
		parNode = new TreeNode<KEY, VALUE>(conf);
		children = new WritableList<KEY>(ConfParser.<KEY> getKeyClass(conf),
				conf);
	}

	@Override
	protected void map(KEY key, TreeNode<KEY, VALUE> node, Context context)
			throws IOException, InterruptedException {
		if (node.isNew()) {
			if (node.makeInitial(tree, key)) {
				children.clear();
				tree.getChildren(key, children);
				for (int i = 0; i < children.length(); i++) {
					parNode.setParent(i, key);
					context.write(children.get(i), parNode);
				}
				node.setNumChildren(children.length());
			}
		}
		assert !node.isNew();
		context.write(key, node);
	}
}

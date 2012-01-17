package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.TreeNode;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class CreationMapper<K extends WritableComparable<K>, V extends Writable, PI extends Writable, UM extends Writable, CI extends Writable, DM extends Writable>
		extends
		Mapper<K, TreeNode<K, V, PI, UM, CI, DM>, K, TreeNode<K, V, PI, UM, CI, DM>> {
	private Tree<K, V, PI, UM, CI, DM> tree;
	private TreeNode<K, V, PI, UM, CI, DM> parNode;
	private IntEntry<Entry<K, DM>> childEntry;
	private Entry<K, DM> entry;
	private WritableList<DM> childMessageList;

	@Override
	protected void setup(Context context) {
		Configuration conf = context.getConfiguration();
		tree = ConfParser.<K, V, PI, UM, CI, DM> newTree(conf);
		parNode = new TreeNode<K, V, PI, UM, CI, DM>(conf);
		childEntry = parNode.getDownList().add();
		entry = childEntry.getValue();
		childMessageList = new WritableList<DM>(tree.getDmClass(), conf);
	}

	@Override
	protected void map(K key, TreeNode<K, V, PI, UM, CI, DM> node,
			Context context) throws IOException, InterruptedException {
		entry.setKey(key);
		if (node.hasValue())
			node.combineDown(tree, key);
		else {
			childMessageList.clear();
			node.firstVisit(tree, key, childMessageList);
			WritableList<Entry<K, CI>> children = node.getChildren();
			for (int i = 0; i < children.length(); i++) {
				childEntry.setKey(i);
				Entry<K, CI> nextChild = children.get(i);
				DM message = childMessageList.get(i);
				entry.setValue(message);
				context.write(nextChild.getKey(), parNode);
			}
		}
		context.write(key, node);
	}
}

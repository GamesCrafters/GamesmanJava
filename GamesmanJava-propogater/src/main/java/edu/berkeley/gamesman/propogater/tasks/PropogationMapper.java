package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.TreeNode;
import edu.berkeley.gamesman.propogater.writable.BitSetWritable;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class PropogationMapper<K extends WritableComparable<K>, V extends Writable, PI extends Writable, UM extends Writable, CI extends Writable, DM extends Writable>
		extends
		Mapper<K, TreeNode<K, V, PI, UM, CI, DM>, K, TreeNode<K, V, PI, UM, CI, DM>> {
	private Tree<K, V, PI, UM, CI, DM> tree;
	private TreeNode<K, V, PI, UM, CI, DM> parNode;
	private Set<IntWritable> workingSet;
	private IntEntry<UM> parPair;
	private final IntWritable writ = new IntWritable();

	@Override
	protected void setup(Context context) {
		Configuration conf = context.getConfiguration();
		tree = ConfParser.<K, V, PI, UM, CI, DM> newTree(conf);
		parNode = tree.newNode();
		WritableList<IntEntry<UM>> upList = parNode.getUpList();
		parPair = upList.add();
		workingSet = ConfParser.getWorkingSet(conf);
	}

	@Override
	protected void map(K key, TreeNode<K, V, PI, UM, CI, DM> node,
			Context context) throws IOException, InterruptedException {
		if (!node.hasValue()) {
			throw new RuntimeException(
					"No value found at too late a stage: key = \n"
							+ key.toString());
		}
		writ.set(tree.getDivision(key));
		if (workingSet.contains(writ)) {
			boolean nodeChanged = node.combineUp(tree, key);
			BitSetWritable cleanSet = node.getCleanSet();
			WritableList<IntEntry<Entry<K, PI>>> parentList = node
					.getParentList();
			for (int i = 0; i < parentList.length(); i++) {
				if (nodeChanged || !cleanSet.get(i)) {
					IntEntry<Entry<K, PI>> intEntry = parentList.get(i);
					Entry<K, PI> parent = intEntry.getValue();
					tree.sendUp(key, node.getValue(), parent.getKey(),
							parent.getValue(), parPair.getValue());
					parPair.setKey(intEntry.getKey());
					context.write(parent.getKey(), parNode);
					cleanSet.set(i);
				}
			}
		}
		context.write(key, node);
	}
}

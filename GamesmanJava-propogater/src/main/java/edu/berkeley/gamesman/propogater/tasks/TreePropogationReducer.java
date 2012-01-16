package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.common.IOCheckOperations;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.TreeNode;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class TreePropogationReducer<K extends WritableComparable<K>, V extends Writable, PI extends Writable, UM extends Writable, CI extends Writable, DM extends Writable>
		extends TreeReducer<K, V, PI, UM, CI, DM> {
	private Tree<K, V, PI, UM, CI, DM> tree;
	private final HashSet<Integer> changed = new HashSet<Integer>();

	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {
		super.setup(context);
		Configuration conf = context.getConfiguration();
		tree = ConfParser.<K, V, PI, UM, CI, DM> newTree(conf);
		changed.clear();
	}

	@Override
	protected void combine(K key, TreeNode<K, V, PI, UM, CI, DM> value) {
		WritableList<IntEntry<UM>> upList = value.getUpList();
		if (!upList.isEmpty()) {
			changed.add(tree.getDivision(key));
			WritableList<Entry<K, CI>> childList = value.getChildren();
			for (int i = 0; i < upList.length(); i++) {
				IntEntry<UM> mess = upList.get(i);
				Entry<K, CI> child = childList.get(mess.getInt());
				tree.receiveUp(key, value.getValue(), child.getKey(),
						mess.getKey(), child.getValue());
			}
		}
		upList.clear();
	}

	@Override
	protected void cleanup(Context context) throws IOException {
		if (!changed.isEmpty()) {
			Configuration conf = context.getConfiguration();
			for (Integer i : changed) {
				Path npp = ConfParser.getNeedsPropogationPath(conf, i);
				IOCheckOperations.createNewFile(npp.getFileSystem(conf), npp);
			}
		}
	}
}

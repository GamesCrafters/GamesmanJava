package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.TreeNode;
import edu.berkeley.gamesman.propogater.writable.Entry;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class TreeCreationReducer<K extends WritableComparable<K>, V extends Writable, PI extends Writable, UM extends Writable, CI extends Writable, DM extends Writable>
		extends TreeReducer<K, V, PI, UM, CI, DM> {
	private boolean hasNew;
	private int creationDivision;
	private Tree<K, V, PI, UM, CI, DM> tree;

	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {
		super.setup(context);
		hasNew = false;
		Configuration conf = context.getConfiguration();
		tree = ConfParser.<K, V, PI, UM, CI, DM> newTree(conf);
		creationDivision = ConfParser.getDivision(conf);
	}

	@Override
	protected void reduce(K key,
			Iterable<TreeNode<K, V, PI, UM, CI, DM>> values, Context context)
			throws IOException, InterruptedException {
		super.reduce(key, values, context);
		if (!value.hasValue() && tree.getDivision(key) == creationDivision)
			hasNew = true;
	}

	@Override
	protected void combine(K key, TreeNode<K, V, PI, UM, CI, DM> value) {
		WritableList<IntEntry<Entry<K, DM>>> downList = value.getDownList();
		WritableList<IntEntry<Entry<K, PI>>> parentList = value.getParentList();
		if (tree.copyDM()) {
			parentList.steal((WritableList) downList);
		} else {
			for (int i = 0; i < downList.length(); i++) {
				IntEntry<Entry<K, DM>> mess = downList.get(i);
				IntEntry<Entry<K, PI>> parent = parentList.add();
				parent.setKey(mess.getKey());
				Entry<K, PI> parentVal = parent.getValue();
				Entry<K, DM> messVal = mess.getValue();
				tree.receiveDown(key, value.getValue(), messVal.getKey(),
						messVal.getValue(), parentVal.getValue());
				parentVal.swapKeys(messVal);
			}
			downList.clear();
		}
	}

	@Override
	protected void cleanup(Context context) {
		if (hasNew) {
			Configuration conf = context.getConfiguration();
			Path hnPath = ConfParser.getNeedsCreationPath(conf,
					creationDivision);
			try {
				hnPath.getFileSystem(conf).createNewFile(hnPath);
				if (!hnPath.getFileSystem(conf).exists(hnPath))
					throw new IOException(hnPath + " not successfully created");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.node.TreeNode;
import edu.berkeley.gamesman.propogater.writable.WritableSettableCombinable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;


public class TreeCreationReducer<KEY extends WritableSettableComparable<KEY>, VALUE extends WritableSettableCombinable<VALUE>>
		extends TreeReducer<KEY, VALUE> {
	private boolean hasNew;
	private int creationDivision;
	private Tree<KEY, VALUE> tree;

	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {
		super.setup(context);
		hasNew = false;
		Configuration conf = context.getConfiguration();
		tree = ConfParser.<KEY, VALUE> getTree(conf);
		creationDivision = ConfParser.getDivision(conf);
	}

	@Override
	protected void reduce(KEY key, Iterable<TreeNode<KEY, VALUE>> values,
			Context context) throws IOException, InterruptedException {
		super.reduce(key, values, context);
		if (curNode.isNew() && tree.getDivision(key) == creationDivision)
			hasNew = true;
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

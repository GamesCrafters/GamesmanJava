package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.node.TreeNode;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

public class TreeCombineReducer<KEY extends WritableSettableComparable<KEY>, VALUE extends WritableSettable<VALUE>>
		extends TreeReducer<KEY, VALUE> {
	private boolean hasNew;

	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {
		super.setup(context);
		hasNew = false;
	}

	@Override
	protected void reduce(KEY key, Iterable<TreeNode<KEY, VALUE>> values,
			Context context) throws IOException, InterruptedException {
		super.reduce(key, values, context);
		if (curNode.isNew())
			hasNew = true;
	}

	@Override
	protected void cleanup(Context context) {
		if (hasNew) {
			Configuration conf = context.getConfiguration();
			Path hnPath = ConfParser.getNeedsCreationPath(conf,
					ConfParser.getDivision(conf));
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

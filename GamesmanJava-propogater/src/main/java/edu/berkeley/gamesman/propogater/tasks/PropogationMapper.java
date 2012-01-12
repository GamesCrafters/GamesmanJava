package edu.berkeley.gamesman.propogater.tasks;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.node.TreeNode;
import edu.berkeley.gamesman.propogater.writable.WritableSettable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

public class PropogationMapper<KEY extends WritableSettableComparable<KEY>, VALUE extends WritableSettable<VALUE>>
		extends Mapper<KEY, TreeNode<KEY, VALUE>, KEY, TreeNode<KEY, VALUE>> {
	private Tree<KEY, VALUE> tree;
	private TreeNode<KEY, VALUE> parNode;
	private final HashSet<Integer> changed = new HashSet<Integer>();
	private Set<Integer> workingSet;

	@Override
	protected void setup(Context context) {
		Configuration conf = context.getConfiguration();
		tree = ConfParser.<KEY, VALUE> newTree(conf);
		workingSet = ConfParser.getWorkingSet(conf);
		parNode = new TreeNode<KEY, VALUE>(conf);
		changed.clear();
	}

	@Override
	protected void map(KEY key, TreeNode<KEY, VALUE> node, Context context)
			throws IOException, InterruptedException {
		int division = tree.getDivision(key);
		if (workingSet.contains(division)) {
			final boolean needsToSend = node.combine(tree, key);
			boolean divisionSet = false;
			for (int i = 0; i < node.numParents(); i++) {
				if (!node.seenParent(i) || needsToSend) {
					KEY parent = node.getParentKey(i);
					if (!divisionSet) {
						division = tree.getDivision(parent);
						divisionSet = true;
						changed.add(division);
					}
					parNode.toParent(tree, key, node, i);
					context.write(parent, parNode);
				}
			}
		}
		context.write(key, node);
	}

	@Override
	protected void cleanup(Context context) throws IOException {
		for (int division : changed) {
			Configuration conf = context.getConfiguration();
			Path signalPath = ConfParser
					.getNeedsPropogationPath(conf, division);
			signalPath.getFileSystem(conf).createNewFile(signalPath);
			if (!signalPath.getFileSystem(conf).exists(signalPath))
				throw new IOException(signalPath + " not successfully created");
		}
	}
}

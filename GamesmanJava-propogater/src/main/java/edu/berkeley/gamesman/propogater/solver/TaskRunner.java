package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.common.Util;
import edu.berkeley.gamesman.propogater.tree.Tree;

abstract class TaskRunner implements Runnable {
	public static final int COMBINE = 0, CREATE = 1, PROPOGATE = 2,
			CLEANUP = 3;
	public static final int NUM_TYPES = 4;
	public final int type;
	protected final Tree<?, ?, ?, ?, ?, ?> tree;
	protected final TierGraph myGraph;

	TaskRunner(Configuration conf, int type, TierGraph graph) {
		assert type >= 0 && type <= 4;
		this.tree = ConfParser
				.<WritableComparable, Writable, Writable, Writable, Writable, Writable> newTree(conf);
		this.type = type;
		myGraph = graph;
	}

	protected void splitUp(Tier tier) throws IOException {
		while (true) {
			Path[] files = tier.getCreationOutputFiles(Solver.underscoreFilter);
			if (files.length == 0)
				break;
			Path file = files[0];
			String name = file.getName();
			String ext = name.substring(name.lastIndexOf("."));
			Tier nextTier = getTier(ext);
			if (!nextTier.filter.accept(file)) {
				throw new Error(nextTier.filter + " does not accept " + file);
			}
			updateEdges(tier, nextTier);
			tier.createTempFolder();
			Path[] sameTier = tier.getCreationOutputFiles(nextTier.filter);
			assert Util.contains(sameTier, file);
			for (Path n : sameTier)
				tier.moveToTemp(n);
			putBack(tier, nextTier);
		}
	}

	protected Tier getTier(String ext) throws IOException {
		throw new UnsupportedOperationException();
	}

	protected void putBack(Tier from, Tier to) throws IOException {
		throw new UnsupportedOperationException();
	}

	protected void updateEdges(Tier tier, Tier nextTier) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void run() {
		try {
			runTask();
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	protected abstract void runTask() throws Throwable;

	protected final int getNumReducers(Job j, Path inPath) throws IOException {
		return getNumReducers(j, new Path[] { inPath });
	}

	protected final int getNumReducers(Job j, Path[] allPaths)
			throws IOException {
		long maxSplitSize = FileInputFormat.getMaxSplitSize(j);
		long totalSize = 0L;
		for (Path p : allPaths) {
			totalSize += p.getFileSystem(tree.getConf()).getFileStatus(p)
					.getLen();
		}
		long numTasksL = (totalSize + maxSplitSize - 1) / maxSplitSize;
		int numTasks = (int) Math.min(Integer.MAX_VALUE, numTasksL);
		return Math.max(j.getNumReduceTasks(), numTasks);
	}
}
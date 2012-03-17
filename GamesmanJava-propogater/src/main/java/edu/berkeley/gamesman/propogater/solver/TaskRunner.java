package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Job;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.common.IOCheckOperations;
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
		long splitSize = j.getConfiguration().getLong(
				"propogate.reducer.split.size", 1000000);
		long totSize = 0L;
		for (Path p : allPaths) {
			FileSystem fs = p.getFileSystem(tree.getConf());
			FileStatus[] stati = fs.listStatus(p);
			for (FileStatus status : stati) {
				totSize += status.getLen();
			}
		}
		int numTasks = (int) ((totSize + splitSize - 1) / splitSize);
		System.out.println("totSize = " + totSize);
		System.out.println("splitSize = " + splitSize);
		System.out.println("numTasks = " + numTasks);
		return numTasks;
	}

	protected void makeFiles(Configuration jConf, Job j) throws IOException {
		CounterGroup g = j.getCounters().getGroup("file");
		for (Counter c : g) {
			if (c.getValue() > 0) {
				Path hnPath = new Path(c.getName());
				IOCheckOperations.createNewFile(hnPath.getFileSystem(jConf),
						hnPath);
			}
		}
	}
}
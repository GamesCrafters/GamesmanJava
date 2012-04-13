package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.common.Util;
import edu.berkeley.gamesman.propogater.tree.Tree;

public abstract class TaskRunner implements Runnable {
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

	protected void splitUp(Tier tier, Job j) throws IOException {
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
			long numPositions = recordsWritten(j, nextTier.num);
			putBack(tier, nextTier, numPositions);
		}
	}

	protected Tier getTier(String ext) throws IOException {
		throw new UnsupportedOperationException();
	}

	protected void putBack(Tier from, Tier to, long numPositions)
			throws IOException {
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

	protected final long recordsWritten(Job j, int tier) throws IOException {
		return j.getCounters().findCounter("num_records", "t" + tier)
				.getValue();
	}

	protected abstract void runTask() throws Throwable;

	protected int getNumReducers(Job job, Collection<Tier> allTiers)
			throws IOException {
		long totRecords = 0;
		for (Tier t : allTiers) {
			totRecords += t.getNumRecords();
		}
		return getNumTypeReducers(job.getConfiguration(), totRecords);
	}

	protected int getNumReducers(Job job, Tier toCombine) {
		return getNumTypeReducers(job.getConfiguration(),
				toCombine.getNumCombineRecords());
	}

	protected abstract int getNumTypeReducers(Configuration conf,
			long numRecords);

	public static final int numTypeReducersFromSplit(long totSize,
			long splitSize) {
		return (int) ((totSize + splitSize - 1) / splitSize);
	}

	public static void enableCompression(Job j,
			SequenceFile.CompressionType type) {
		Configuration conf = j.getConfiguration();
		conf.setBoolean("mapred.compress.map.output", true);
		FileOutputFormat.setCompressOutput(j, true);
		SequenceFileOutputFormat.setOutputCompressionType(j, type);
	}

	protected Set<Integer> getNeeds(String type, Counters counts) {
		CounterGroup group = counts.getGroup(type);
		HashSet<Integer> result = new HashSet<Integer>(group.size());
		for (Counter counter : group) {
			String name = counter.getName();
			assert name.startsWith("t");
			result.add(Integer.parseInt(name.substring(1)));
		}
		return result;
	}
}
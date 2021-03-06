package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tasks.CleanupMapper;
import edu.berkeley.gamesman.propogater.tree.Tree;

public class CleanupRunner extends TaskRunner {

	public CleanupRunner(Configuration conf, Tree<?, ?, ?, ?, ?, ?> tree,
			TierGraph graph) {
		super(conf, tree, CLEANUP, graph);
	}

	@Override
	protected void runTask() throws IOException, InterruptedException,
			ClassNotFoundException {
		Configuration treeConf = tree.getConf();
		Job job = new Job(treeConf, "Cleanup");
		job.setMapperClass(CleanupMapper.class);
		job.setReducerClass(tree.getCleanupReducerClass());
		Class<? extends Partitioner<?, ?>> partitionerClass = ConfParser
				.<WritableComparable, Writable> getCleanupPartitionerClass(treeConf);
		if (partitionerClass != null) {
			job.setPartitionerClass(partitionerClass);
		} else {
			partitionerClass = job.getPartitionerClass();
			ConfParser.addOutputParameter(treeConf,
					"propogater.cleanup.partitioner",
					partitionerClass.getName());
		}
		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setOutputFormatClass(tree.getCleanupOutputFormatClass());
		job.setJarByClass(Solver.class);
		job.setOutputKeyClass(tree.getKeyClass());
		job.setOutputValueClass(tree.getValClass());
		Collection<Tier> allTiers = myGraph.getTiers();
		Path[] allData = new Path[allTiers.size()];
		int i = 0;
		for (Tier t : allTiers) {
			allData[i++] = t.dataPath;
		}
		FileInputFormat.setInputPaths(job, allData);
		FileOutputFormat.setOutputPath(job, ConfParser.getOutputPath(treeConf));
		job.setNumReduceTasks(getNumReducers(job, allTiers));
		enableCompression(job, tree.getCleanupCompressionType());
		boolean succeeded = job.waitForCompletion(true);
		if (!succeeded)
			throw new RuntimeException("Job did not succeed: " + job);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CleanupRunner;
	}

	@Override
	public int hashCode() {
		return 93;
	}

	@Override
	protected int getNumTypeReducers(Configuration conf, long totSize) {
		long splitSize = tree.getCleanupSplitSize(conf);
		return numTypeReducersFromSplit(totSize, splitSize);
	}
}

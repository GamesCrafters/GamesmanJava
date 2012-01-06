package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tasks.CleanupMapper;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.writable.WritableSettableCombinable;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;


public class CleanupRunner extends TaskRunner {

	public CleanupRunner(Configuration conf, TierGraph graph) {
		super(conf, CLEANUP, graph);
	}

	@Override
	protected void runTask() throws IOException, InterruptedException,
			ClassNotFoundException {
		Tree<?, ?> tree = ConfParser
				.<WritableSettableComparable, WritableSettableCombinable> getTree(conf);
		Job job = new Job(conf, "Cleanup");
		job.setMapperClass(CleanupMapper.class);
		job.setReducerClass(tree.getCleanupReducerClass());
		Class<? extends Partitioner> partitionerClass = ConfParser
				.getCleanupPartitionerClass(conf);
		if (partitionerClass != null) {
			job.setPartitionerClass(partitionerClass);
		} else {
			partitionerClass = job.getPartitionerClass();
			ConfParser.addOutputParameter(conf,
					"propogater.cleanup.partitioner",
					partitionerClass.getName());
		}
		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setOutputFormatClass(tree.getCleanupOutputFormatClass());
		job.setJarByClass(Solver.class);
		job.setOutputKeyClass(ConfParser.getRawKeyClass(conf));
		job.setOutputValueClass(ConfParser.getRawValueClass(conf));
		Collection<Tier> allTiers = myGraph.getTiers();
		Path[] allData = new Path[allTiers.size()];
		int i = 0;
		for (Tier t : allTiers) {
			allData[i++] = t.dataPath;
		}
		FileInputFormat.setInputPaths(job, allData);
		FileOutputFormat.setOutputPath(job, ConfParser.getOutputPath(conf));
		SequenceFileOutputFormat.setOutputCompressionType(job,
				SequenceFile.CompressionType.BLOCK);
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
}

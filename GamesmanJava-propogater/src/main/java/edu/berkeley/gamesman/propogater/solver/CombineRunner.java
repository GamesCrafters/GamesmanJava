package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.common.Util;
import edu.berkeley.gamesman.propogater.tasks.TreeCreationReducer;
import edu.berkeley.gamesman.propogater.tasks.TreeReducer;

public class CombineRunner extends TaskRunner {
	public final Tier tier;

	public CombineRunner(Configuration conf, Tier tier, TierGraph graph) {
		super(conf, COMBINE, graph);
		this.tier = tier;
	}

	@Override
	protected void runTask() {
		tier.lock();
		try {
			if (quickCombine())
				return;
			Path oldDataPath = tier.makeCombinePath();
			tier.renameDataPath(oldDataPath);
			Configuration jConf = new Configuration(tree.getConf());
			ConfParser.setDivision(jConf, tier.num);
			Job j = new Job(jConf, String.format(
					ConfParser.COMBINATION_JOB_FORMAT, tier.num));
			j.setCombinerClass(TreeReducer.class);
			j.setReducerClass(TreeCreationReducer.class);
			j.setInputFormatClass(SequenceFileInputFormat.class);
			j.setOutputFormatClass(SequenceFileOutputFormat.class);
			j.setOutputKeyClass(tree.getKeyClass());
			j.setOutputValueClass(tree.getTreeNodeClass());
			j.setJarByClass(Solver.class);
			Path[] allPaths = tier.getCombinePaths();
			assert Util.contains(
					allPaths,
					oldDataPath.getFileSystem(tree.getConf())
							.getFileStatus(oldDataPath).getPath());
			FileInputFormat.setInputPaths(j, allPaths);
			FileOutputFormat.setOutputPath(j, tier.dataPath);
			boolean succeeded = j.waitForCompletion(true);
			if (!succeeded)
				throw new RuntimeException("Job did not succeed " + j);
			tier.deleteCombinedPaths(allPaths);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		} finally {
			tier.unlock();
		}
	}

	public boolean quickCombine() throws IOException {
		Path[] combinePaths = tier.getCombinePaths();
		if (combinePaths.length == 0)
			return true;
		else
			return false;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CombineRunner
				&& tier == ((CombineRunner) other).tier;
	}

	@Override
	public int hashCode() {
		return 31 + tier.num;
	}
}

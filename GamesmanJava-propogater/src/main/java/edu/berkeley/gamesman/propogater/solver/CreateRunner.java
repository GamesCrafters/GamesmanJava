package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tasks.CreationMapper;
import edu.berkeley.gamesman.propogater.tasks.DividedSequenceFileOutputFormat;
import edu.berkeley.gamesman.propogater.tasks.TreeReducer;

public class CreateRunner extends TaskRunner {
	public final Tier tier;

	public CreateRunner(Configuration conf, Tier tier, TierGraph graph) {
		super(conf, CREATE, graph);
		this.tier = tier;
	}

	@Override
	protected void runTask() throws IOException, InterruptedException,
			ClassNotFoundException {
		tier.lock();
		try {
			if (!tier.startCreation())
				return;
			Configuration jConf = new Configuration(tree.getConf());
			ConfParser.setDivision(jConf, tier.num);
			Job j = new Job(jConf, String.format(
					ConfParser.CREATION_JOB_FORMAT, tier.num));
			j.setMapperClass(CreationMapper.class);
			j.setCombinerClass(TreeReducer.class);
			j.setReducerClass(TreeReducer.class);
			j.setInputFormatClass(SequenceFileInputFormat.class);
			j.setOutputFormatClass(DividedSequenceFileOutputFormat.class);
			j.setOutputKeyClass(tree.getKeyClass());
			j.setOutputValueClass(tree.getTreeNodeClass());
			j.setJarByClass(Solver.class);
			FileInputFormat.setInputPaths(j, tier.dataPath);
			FileOutputFormat.setOutputPath(j, tier.outputFolder);
			j.setNumReduceTasks(getNumReducers(j, tier.dataPath));
			boolean succeeded = j.waitForCompletion(true);
			if (!succeeded)
				throw new RuntimeException("Job did not succeed " + j);
			splitUp(tier);
			tier.deleteOutputFolder();
			tier.setNeedsPropogation();
		} finally {
			tier.unlock();
		}
	}

	@Override
	protected void updateEdges(Tier tier, Tier nextTier) throws IOException {
		if (nextTier != tier || tier.needsToCreate()) {
			tier.addChild(nextTier.num);
			nextTier.addParent(tier.num);
		}
	}

	@Override
	protected void putBack(Tier from, Tier to) throws IOException {
		if (from == to)
			to.replaceDataPath(from.getTempFolder());
		else
			to.addCombine(from.getTempFolder());
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CreateRunner
				&& tier == ((CreateRunner) other).tier;
	}

	@Override
	public int hashCode() {
		return tier.num;
	}

	@Override
	protected Tier getTier(String ext) throws IOException {
		return myGraph.getTier(ext);
	}
}

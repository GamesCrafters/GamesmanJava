package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tasks.DividedSequenceFileOutputFormat;
import edu.berkeley.gamesman.propogater.tasks.PropogationMapper;
import edu.berkeley.gamesman.propogater.tasks.TreePropogationReducer;
import edu.berkeley.gamesman.propogater.tasks.TreeReducer;

public class PropogateRunner extends TaskRunner {
	private final Set<Tier> wholeSet;
	public final SortedSet<Tier> cycleSet;
	public final Tier headTier;

	public PropogateRunner(Configuration conf, TierGraph graph,
			SortedSet<Tier> cycleSet) throws IOException {
		super(conf, PROPOGATE, graph);
		this.wholeSet = new HashSet<Tier>();
		for (Tier t : cycleSet) {
			wholeSet.add(t);
			for (Tier parent : t.getParents())
				wholeSet.add(parent);
		}
		this.cycleSet = cycleSet;
		headTier = cycleSet.first();
	}

	@Override
	protected void updateEdges(Tier tier, Tier nextTier) throws IOException {
	}

	@Override
	protected void runTask() throws InterruptedException, IOException,
			ClassNotFoundException {
		for (Tier t : wholeSet) {
			t.lock();
		}
		try {
			boolean changed = false;
			for (Tier t : cycleSet) {
				changed |= t.startPropogation();
			}
			if (!changed)
				return;
			Configuration jConf = new Configuration(tree.getConf());
			ConfParser.setWorkingSet(jConf, cycleSet);
			Job j = new Job(jConf, String.format(
					ConfParser.PROPOGATION_JOB_FORMAT, headTier.num));
			j.setMapperClass(PropogationMapper.class);
			j.setCombinerClass(TreeReducer.class);
			j.setReducerClass(TreePropogationReducer.class);
			j.setInputFormatClass(SequenceFileInputFormat.class);
			j.setOutputFormatClass(DividedSequenceFileOutputFormat.class);
			j.setOutputKeyClass(tree.getKeyClass());
			j.setOutputValueClass(tree.getTreeNodeClass());
			j.setJarByClass(Solver.class);
			Path[] mixedPaths = TierGraph.mixPaths(wholeSet);
			FileInputFormat.setInputPaths(j, mixedPaths);
			FileOutputFormat.setOutputPath(j, headTier.outputFolder);
			j.setNumReduceTasks(getNumReducers(j, mixedPaths));
			boolean succeeded = j.waitForCompletion(true);
			if (!succeeded)
				throw new RuntimeException("Job did not succeed " + j);
			splitUp(headTier);
			headTier.deleteOutputFolder();
		} finally {
			for (Tier t : wholeSet) {
				t.unlock();
			}
		}
	}

	@Override
	protected void putBack(Tier from, Tier to) throws IOException {
		to.replaceDataPath(from.getTempFolder());
	}

	public boolean needsToPropogate() throws IOException {
		for (Tier t : cycleSet) {
			if (t.needsToPropogate())
				return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof PropogateRunner
				&& headTier == ((PropogateRunner) other).headTier;
	}

	@Override
	public int hashCode() {
		return 62 + headTier.num;
	}

	@Override
	protected Tier getTier(String ext) throws IOException {
		return myGraph.getTierOrNull(ext);
	}
}

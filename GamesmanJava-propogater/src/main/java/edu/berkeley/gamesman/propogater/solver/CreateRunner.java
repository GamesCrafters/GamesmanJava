package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;
import java.util.Collections;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tasks.CreationMapper;
import edu.berkeley.gamesman.propogater.tasks.DividedSequenceFileOutputFormat;
import edu.berkeley.gamesman.propogater.tasks.TreeReducer;
import edu.berkeley.gamesman.propogater.tree.Tree;

public class CreateRunner extends TaskRunner {
	public final Tier tier;

	public CreateRunner(Configuration conf, Tree<?, ?, ?, ?, ?, ?> tree,
			Tier tier, TierGraph graph) {
		super(conf, tree, CREATE, graph);
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
			j.setReducerClass(TreeReducer.class);
			j.setInputFormatClass(SequenceFileInputFormat.class);
			j.setOutputFormatClass(DividedSequenceFileOutputFormat.class);
			j.setOutputKeyClass(tree.getKeyClass());
			j.setOutputValueClass(tree.getTreeNodeClass());
			j.setJarByClass(Solver.class);
			FileInputFormat.setInputPaths(j, tier.dataPath);
			FileOutputFormat.setOutputPath(j, tier.outputFolder);
			j.setNumReduceTasks(getNumReducers(j,
					Collections.<Tier> singleton(tier)));
			long mapperMaxSplitSize = tree.getMapperMaxSplitSize(jConf,
					tier.num);
			if (mapperMaxSplitSize >= 0) {
				long mapperSplitByteSize = (long) (mapperMaxSplitSize
						* ((double) byteSize(tier.dataPath)) / tier
						.getNumRecords());
				System.out
						.println("Mapper split byte size: " + mapperSplitByteSize);
				FileInputFormat.setMaxInputSplitSize(j, mapperSplitByteSize);
			}
			enableCompression(j, SequenceFile.CompressionType.BLOCK);
			boolean succeeded = j.waitForCompletion(true);
			if (!succeeded)
				throw new RuntimeException("Job did not succeed " + j);
			splitUp(tier, j);
			tier.deleteOutputFolder();
			tier.setNeedsPropogation();
			tier.printStats();
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		} finally {
			tier.unlock();
		}
	}

	private long byteSize(Path dataPath) throws IOException {
		FileSystem fs = dataPath.getFileSystem(tree.getConf());
		long res = 0;
		for (FileStatus f : fs.listStatus(dataPath)) {
			res += f.getLen();
		}
		return res;
	}

	@Override
	protected void updateEdges(Tier tier, Tier nextTier) throws IOException {
		if (nextTier != tier || tier.needsToCreate()) {
			tier.addChild(nextTier.num);
			nextTier.addParent(tier.num);
		}
	}

	@Override
	protected void putBack(Tier from, Tier to, long numPositions)
			throws IOException {
		if (from == to) {
			to.replaceDataPath(from.getTempFolder(), numPositions);
		} else {
			to.addCombine(from.getTempFolder(), numPositions);
		}
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

	@Override
	protected int getNumTypeReducers(Configuration conf, long totSize) {
		long splitSize = tree.getCreateSplitSize(conf, tier.num);
		return numTypeReducersFromSplit(totSize, splitSize);
	}
}

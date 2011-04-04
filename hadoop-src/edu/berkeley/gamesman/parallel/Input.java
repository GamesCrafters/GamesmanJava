package edu.berkeley.gamesman.parallel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.solver.Solver;
import edu.berkeley.gamesman.util.Util;

/**
 * An input format which deals with ranges of positions for a game
 * 
 * @author dnspies
 */
public class Input extends InputFormat<Range, IntWritable> {
	public class Reader extends RecordReader<Range, IntWritable> {
		private final IntWritable ZERO = new IntWritable(0);
		private Split mySplit;
		private boolean read = false;
		private Range key = null;

		@Override
		public void initialize(InputSplit s, TaskAttemptContext c) {
			mySplit = (Split) s;
		}

		@Override
		public void close() throws IOException {
		}

		@Override
		public float getProgress() throws IOException {
			return 0F;
		}

		@Override
		// First is the starting hash and num is the number of hashes to read
		public boolean nextKeyValue() throws IOException {
			if (read)
				return false;
			key = mySplit.r;
			read = true;
			return true;
		}

		@Override
		public Range getCurrentKey() {
			return key;
		}

		@Override
		public IntWritable getCurrentValue() {
			return ZERO;
		}

	}

	private static final long DEFAULT_MULTIPLE = 32;

	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		Configuration conf = job.getConfiguration();
		edu.berkeley.gamesman.core.Configuration gc;
		try {
			gc = edu.berkeley.gamesman.core.Configuration.deserialize(conf
					.get("gamesman.configuration"));
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}

		Game<?> game = gc.getGame();
		final long firstHash = 0L;
		final long numHashes = game.numHashes();

		return getSplits(conf, firstHash, numHashes);
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param firstHash
	 *            The first hash of the range to be solved
	 * @param numHashes
	 *            The number of hashes to be solved
	 * @return A list of disjointed splits which cover the range of hashes
	 */
	public List<InputSplit> getSplits(Configuration conf, long firstHash,
			long numHashes) {
		edu.berkeley.gamesman.core.Configuration gamesmanConf;
		try {
			gamesmanConf = edu.berkeley.gamesman.core.Configuration
					.deserialize(conf.get("gamesman.configuration"));
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
		int minSplits = gamesmanConf.getInteger(
				"gamesman.parallel.minimum.splits",
				gamesmanConf.getInteger("gamesman.parallel.numMachines", 1));
		long minSplitSize = gamesmanConf.getLong(
				"gamesman.parallel.minimum.split.size",
				DEFAULT_MULTIPLE
						* gamesmanConf.getLong("gamesman.minimum.split.size",
								Solver.DEFAULT_MIN_SPLIT_SIZE));
		long preferredSplitSize = gamesmanConf.getLong(
				"gamesman.parallel.preferred.split.size",
				DEFAULT_MULTIPLE
						* gamesmanConf.getLong("gamesman.preferred.split.size",
								Solver.DEFAULT_PREFERRED_SPLIT_SIZE));
		long[] splits = Util.getSplits(firstHash, numHashes, minSplitSize,
				minSplits, preferredSplitSize);
		ArrayList<InputSplit> splitList = new ArrayList<InputSplit>(
				splits.length - 1);
		for (int i = 0; i < splits.length - 1; i++) {
			splitList.add(new Split(new Range(splits[i], splits[i + 1]
					- splits[i])));
		}
		return splitList;
	}

	@Override
	public RecordReader<Range, IntWritable> createRecordReader(InputSplit is,
			TaskAttemptContext context) throws IOException {
		Reader rr = new Reader();
		return rr;
	}

}

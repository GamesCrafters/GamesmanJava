package edu.berkeley.gamesman.parallel.tier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.berkeley.gamesman.game.TierGame;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.*;

/**
 * Created by IntelliJ IDEA. User: user Date: Nov 30, 2010 Time: 10:03:34 AM To
 * change this template use File | Settings | File Templates.
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
			if (read)
				return 1F;
			else
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

	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		Configuration conf = job.getConfiguration();
		int tier = conf.getInt("tier", -1);
		edu.berkeley.gamesman.core.Configuration gc;
		try {
			gc = edu.berkeley.gamesman.core.Configuration.deserialize(conf
					.get("gamesman.configuration"));
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
		if (tier < 0)
			throw new Error("No tier specified");
		int splits;
		try {
			edu.berkeley.gamesman.core.Configuration gamesmanConf = edu.berkeley.gamesman.core.Configuration
					.deserialize(conf.get("gamesman.configuration"));
			int numMachines = gamesmanConf.getInteger(
					"gamesman.parallel.numMachines", 1);
			splits = numMachines
					* gamesmanConf.getInteger("gamesman.parallel.multiple", 1);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
		TierGame game = (TierGame) gc.getGame();
		List<InputSplit> is = new ArrayList<InputSplit>();
		final long firstHash = game.hashOffsetForTier(tier);
		long numHashes = game.numHashesForTier(tier);
		long prevHash = firstHash;
		for (int i = 0; i < splits; i++) {
			long nextHash = firstHash + numHashes * (i + 1) / splits;
			is.add(new Split(new Range(prevHash, nextHash - prevHash)));
			prevHash = nextHash;
		}
		return is;
	}

	@Override
	public RecordReader<Range, IntWritable> createRecordReader(InputSplit is,
			TaskAttemptContext context) throws IOException {
		Reader rr = new Reader();
		return rr;
	}

}

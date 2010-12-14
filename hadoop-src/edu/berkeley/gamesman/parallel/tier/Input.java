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
		int numMachines = 1; // = conf.getInteger("numMachines", 8); //Default
								// of 8
		TierGame game = (TierGame) gc.getGame();
		long numPos = game.numHashesForTier(tier);
		int splits = numMachines;
		List<InputSplit> is = new ArrayList<InputSplit>();
		long First = game.hashOffsetForTier(tier);
		long increment = (int) (((float) numPos) / numMachines);
		for (int i = 0; i < splits; i++) {
			long next = First + increment; // Not sure if hashes work this way
			is.add(new Split(new Range(First, increment)));
			First = next;
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

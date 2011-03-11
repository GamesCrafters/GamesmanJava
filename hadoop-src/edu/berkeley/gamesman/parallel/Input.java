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
import edu.berkeley.gamesman.parallel.tier.Range;
import edu.berkeley.gamesman.parallel.tier.Split;

public class Input extends InputFormat<Range, IntWritable>
{
	public class Reader extends RecordReader<Range, IntWritable>
	{
		private final IntWritable ZERO = new IntWritable(0);
		private Split mySplit;
		private boolean read = false;
		private Range key = null;

		@Override
		public void initialize(InputSplit s, TaskAttemptContext c)
		{
			mySplit = (Split) s;
		}

		@Override
		public void close() throws IOException
		{
		}

		@Override
		public float getProgress() throws IOException
		{
			return 0F;
		}

		@Override
		// First is the starting hash and num is the number of hashes to read
		public boolean nextKeyValue() throws IOException
		{
			if (read)
				return false;
			key = mySplit.r;
			read = true;
			return true;
		}

		@Override
		public Range getCurrentKey()
		{
			return key;
		}

		@Override
		public IntWritable getCurrentValue()
		{
			return ZERO;
		}

	}

	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException
	{
		Configuration conf = job.getConfiguration();
		int tier = conf.getInt("tier", -1);
		edu.berkeley.gamesman.core.Configuration gc;
		try
		{
			gc = edu.berkeley.gamesman.core.Configuration.deserialize(conf
					.get("gamesman.configuration"));
		} catch (ClassNotFoundException e)
		{
			throw new Error(e);
		}
		if (tier < 0)
			throw new Error("No tier specified");
		Game<?> game = gc.getGame();
		final long firstHash = 0L;
		final long numHashes = game.numHashes();

		return getSplits(conf, firstHash, numHashes);
	}

	public List<InputSplit> getSplits(Configuration conf, long firstHash,
			long numHashes) throws IOException
	{
		int splits;
		try
		{
			edu.berkeley.gamesman.core.Configuration gamesmanConf = edu.berkeley.gamesman.core.Configuration
					.deserialize(conf.get("gamesman.configuration"));
			int numMachines = gamesmanConf.getInteger(
					"gamesman.parallel.numMachines", 1);
			splits = (int) (numMachines * gamesmanConf.getFloat(
					"gamesman.parallel.multiple", 1F));
			long averageSplit = numHashes / splits;
			long minSplit = gamesmanConf.getLong(
					"gamesman.parallel.minimum.split", 1L << 20);
			if (averageSplit < minSplit)
			{
				splits = Math.max((int) (numHashes / minSplit), 1);
			}
		} catch (ClassNotFoundException e)
		{
			throw new Error(e);
		}
		List<InputSplit> is = new ArrayList<InputSplit>();
		long prevHash = firstHash;
		for (int i = 0; i < splits; i++)
		{
			long nextHash = firstHash + numHashes * (i + 1) / splits;
			is.add(new Split(new Range(prevHash, nextHash - prevHash)));
			prevHash = nextHash;
		}

		return is;
	}

	@Override
	public RecordReader<Range, IntWritable> createRecordReader(InputSplit is,
			TaskAttemptContext context) throws IOException
	{
		Reader rr = new Reader();
		return rr;
	}

}

package edu.berkeley.gamesman.hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.hadoop.util.*;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

public class TieredHadoopTool extends Configured implements Tool {
	edu.berkeley.gamesman.core.Configuration myConf;

	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		assert Util.debug(DebugFacility.HADOOP,
				"Hadoop launching with configuration " + args[0]);
		myConf = edu.berkeley.gamesman.core.Configuration.load(Util
				.decodeBase64(args[0]));

		conf.set("configuration_data", args[0]);
		conf.setStrings("args", args);
		String solverName = myConf.getProperty("gamesman.solver");

		// Determine last index
		TieredGame<?> g = Util.checkedCast(myConf.getGame());

		for (int tier = g.numberOfTiers() - 1; tier >= 0; tier--) {
			Util.debug(DebugFacility.HADOOP, "Processing tier " + tier);
			processRun(conf, tier);
		}

		return 0;
	}

	private void processRun(Configuration conf, int tier) throws IOException {
		Game<?> g = myConf.getGame();
		TieredHasher<?> h = Util.checkedCast(myConf.getHasher());
		long firstHash = h.hashOffsetForTier(tier);
		long endHash = h.lastHashValueForTier(tier) + 1;
		long incr = 1000;

		JobConf job = new JobConf(conf, TierMap.class);

		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(SplitDatabaseWritable.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(SplitDatabaseWritableList.class);

		job.set("tier", Integer.toString(tier));

		job.setJobName("Tier Map-Reduce");
		FileInputFormat.setInputPaths(job, new Path("in"));
		job.setInputFormat(SequenceInputFormat.class);
		job.setOutputFormat(SplitDatabaseOutputFormat.class);
		FileOutputFormat.setOutputPath(job, new Path(String.format(
				"tier%02d/tier.hdb", tier)));
		job.setMapperClass(TierMap.class);
		job.setNumMapTasks(myConf.getInteger("gamesman.hadoop.split", 60));
		job.setNumReduceTasks(1);
		job.setReducerClass(SplitDatabaseReduce.class);

		JobClient.runJob(job);
		return;
	}

}

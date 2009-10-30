package edu.berkeley.gamesman.hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
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

/**
 * The TieredHadoopTool is the code that runs on the master node. It loops over
 * all tiers, and for each tier, it sets "tier" in the JobConf. Then, it uses
 * SequenceInputFormat to subdivide the hash space into a set of inputs for each
 * mapper.
 * 
 * @author Patrick Horn
 */
@SuppressWarnings("deprecation")
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

		// Determine last index
		TieredGame<?> g = Util.checkedCast(myConf.getGame());

		for (int tier = g.numberOfTiers() - 1; tier >= 0; tier--) {
			Util.debug(DebugFacility.HADOOP, "Processing tier " + tier);
			processRun(conf, tier);
		}

		return 0;
	}

	private void processRun(Configuration conf, int tier) throws IOException {
		TieredHasher<?> h = Util.checkedCast(myConf.getHasher());
		long firstHash = h.hashOffsetForTier(tier);
		long endHash = h.hashOffsetForTier(tier + 1);
		int incr = myConf.getInteger("gamesman.hadoop.incr", 1000);

		JobConf job = new JobConf(conf, TierMap.class);

		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(SplitDatabaseWritable.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(SplitDatabaseWritableList.class);

		job.set("first", Long.toString(firstHash));
		job.set("end", Long.toString(endHash));
		// job.set("tasks", Integer.toString(incr));
		job.set("tier", Integer.toString(tier));
		job.set("recordsPerGroup", Integer.toString(myConf.recordsPerGroup));

		job.setJobName("Tier Map-Reduce");
		FileInputFormat.setInputPaths(job, new Path("in"));
		job.setInputFormat(SequenceInputFormat.class);
		job.setOutputFormat(SplitDatabaseOutputFormat.class);
		FileOutputFormat.setOutputPath(job, new Path(new Path(myConf
				.getProperty("gamesman.db.uri")), String.format("tier%02d",
				tier)));
		FileSystem.get(job).mkdirs(FileOutputFormat.getOutputPath(job));
		job.setMapperClass(TierMap.class);
		job.setNumMapTasks(myConf.getInteger("gamesman.hadoop.numMappers", 60));
		job.setNumReduceTasks(1);
		job.setReducerClass(SplitDatabaseReduce.class);

		JobClient.runJob(job);
		return;
	}

}

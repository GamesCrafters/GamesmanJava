package edu.berkeley.gamesman.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.Tool;

import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.database.HadoopDatabase;
import edu.berkeley.gamesman.hadoop.util.SplitDatabaseOutputFormat;
import edu.berkeley.gamesman.hadoop.util.SplitDatabaseWritableList;
import edu.berkeley.gamesman.hadoop.util.SplitDatabaseWritable;
import edu.berkeley.gamesman.hadoop.util.SequenceInputFormat;
import edu.berkeley.gamesman.master.LocalMaster;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;


public class TieredHadoopTool extends Configured implements Tool {
	edu.berkeley.gamesman.core.Configuration myConf;

	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		assert Util.debug(DebugFacility.HADOOP,"Hadoop launching with configuration "+args[0]);
		myConf = edu.berkeley.gamesman.core.Configuration.load(Util.decodeBase64(args[0]));

		conf.set("configuration_data",args[0]);
		conf.setStrings("args", args);
		String solverName = myConf.getProperty("gamesman.solver");

		// Determine last index
		LocalMaster m = new LocalMaster();
		TieredGame<?> g = (TieredGame)(myConf.getGame());
		Hasher<?> h = myConf.getHasher();
		Class<? extends Solver> s =
			Util.typedForName("edu.berkeley.gamesman.solver." + solverName, Solver.class);

		m.initialize(myConf, s, HadoopDatabase.class, false); // arg 4 is 'cached'.

		for(int tier = g.numberOfTiers()-1 ; tier >= 0; tier--){
			Util.debug(DebugFacility.HADOOP,"Processing tier "+tier);
			processRun(conf, m.getSolver(), tier);
		}
		
		return 0;
	}
	
	private void processRun(Configuration conf, Solver solver, int tier) throws IOException {
		long firstTask = 0;
		long lastTask = solver.numberOfTasksForTier(myConf, tier);
		long incr = 1000;

		Game<?> g = myConf.getGame();

		JobConf job = new JobConf(conf, TierMap.class);

		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(SplitDatabaseWritable.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(SplitDatabaseWritableList.class);

		job.set("tier", ""+tier);

		job.setJobName("Tier Map-Reduce");
		FileInputFormat.setInputPaths(job, new Path("in"));
		job.setInputFormat(SequenceInputFormat.class);
		job.setOutputFormat(SplitDatabaseOutputFormat.class);
		FileOutputFormat.setOutputPath(job, new Path(
			String.format("tier%02d/tier.hdb",tier)));
		job.setMapperClass(TierMap.class);
		job.setNumMapTasks(60);
		job.setNumReduceTasks(1);
		job.setReducerClass(SplitDatabaseReduce.class);

		JobClient.runJob(job);
		return;
	}

}


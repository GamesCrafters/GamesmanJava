package edu.berkeley.gamesman.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.Tool;

import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.database.NullDatabase;
import edu.berkeley.gamesman.hadoop.util.BigIntegerWritable;
import edu.berkeley.gamesman.hadoop.util.SequenceInputFormat;
import edu.berkeley.gamesman.master.LocalMaster;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.OptionProcessor;
import edu.berkeley.gamesman.util.Util;

public class TieredHadoopTool extends Configured implements Tool {

	public int run(String[] args) throws Exception {
		Configuration conf = getConf();

		conf.set("gameclass", OptionProcessor.checkOption("game"));
		conf.set("databaseclass", OptionProcessor.checkOption("database"));
		conf.set("hasherclass", OptionProcessor.checkOption("hasher"));
		conf.set("dburi", OptionProcessor.checkOption("uri"));
		conf.setStrings("args", args);

		// Determine last index
		LocalMaster m = new LocalMaster();
		m.initialize(
				(Class<? extends Game<?>>) Class
						.forName("edu.berkeley.gamesman.game."
								+ conf.get("gameclass")), TierSolver.class,
				(Class<? extends Hasher<?>>) Class
						.forName("edu.berkeley.gamesman.hasher."
								+ conf.get("hasherclass")), NullDatabase.class);

		TieredGame<?> game = (TieredGame<?>) m.getGame();
		
			conf.set("firsthash", "0");
			conf.set("lasthash", game.lastHashValueForTier(game.numberOfTiers()-1).toString());
			//Util.debug("Tier goes from "+conf.get("firsthash")+"-"+conf.get("lasthash"));

			JobConf job = new JobConf(conf, TierMapReduce.class);

			job.setMapOutputKeyClass(BigIntegerWritable.class);
			job.setMapOutputValueClass(BigIntegerWritable.class);

			job.setJobName("Tier Map-Reduce");
			FileInputFormat.setInputPaths(job, new Path("in"));
			job.setInputFormat(SequenceInputFormat.class);
			FileOutputFormat.setOutputPath(job, new Path("out_0"));
			job.setMapperClass(TierMapReduce.class);
			job.setReducerClass(TierMapReduce.class);

			JobClient.runJob(job);
		return 0;
	}

}

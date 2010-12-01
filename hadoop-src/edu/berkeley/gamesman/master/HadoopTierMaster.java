package edu.berkeley.gamesman.master;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.GamesmanConf;
import edu.berkeley.gamesman.game.TierGame;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;

public class HadoopTierMaster implements Runnable {
	private final Configuration hadoopConf;
	private final GamesmanConf gamesmanConf;
	private final TierGame game;
        private final Job job;

	public HadoopTierMaster(String confFile) throws ClassNotFoundException,
			IOException {
		hadoopConf = new Configuration();
		hadoopConf.addResource(new Path(confFile));
		gamesmanConf = new GamesmanConf(hadoopConf);
		game = (TierGame) gamesmanConf.getGame();
                job = new Job(hadoopConf, "hadoop tier solver");


		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(FileStatus.class);
		job.setMapperClass(HadoopTierMapper.class);
		job.setReducerClass(HadoopTierReducer.class);
		job.setInputFormatClass(Input.class);
                //TODO: job.setOutputFormatClass(HadoopTierReducer.class);
	}

	@Override
	public void run() {
		for (int tier = game.numberOfTiers() - 1; tier >= 0; tier--) {
			hadoopConf.setInt("tier", tier);
			solve(tier);
		}
	}

	private void solve(int tier) {
            boolean Success = false;
            while (true) {
                try {
		    Success = job.waitForCompletion(true);
                } catch( Exception e) { continue;}
                if (Success) {
                    break;
                }
            }
	}
}

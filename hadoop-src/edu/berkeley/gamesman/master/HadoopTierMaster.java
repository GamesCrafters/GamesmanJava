package edu.berkeley.gamesman.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.GamesmanConf;
import edu.berkeley.gamesman.game.TierGame;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class HadoopTierMaster implements Runnable {
	private final Configuration hadoopConf;
	private final GamesmanConf gamesmanConf;
	private final TierGame game;
	private final FileSystem fs;
	private final Path outputDirectory = new Path("temp_output");
	private Job job;
	public static Configuration theConf;

	public HadoopTierMaster(GenericOptionsParser gop, String confFile)
			throws IOException, ClassNotFoundException {
		hadoopConf = gop.getConfiguration();
		File f = new File(confFile);
		FileInputStream fis = new FileInputStream(f);
		Properties props = new Properties();
		props.load(fis);
		fis.close();
		for (Entry<Object, Object> e : props.entrySet())
			hadoopConf.set(e.getKey().toString(), e.getValue().toString());
		fs = FileSystem.get(hadoopConf);
		gamesmanConf = new GamesmanConf(hadoopConf);
		game = (TierGame) gamesmanConf.getGame();
	}

	@Override
	public void run() {
		for (int tier = game.numberOfTiers() - 1; tier >= 0; tier--) {
			solve(tier);
		}
	}

	private void solve(int tier) {
		try {
			if (fs.exists(outputDirectory))
				fs.delete(outputDirectory, true);
			hadoopConf.setInt("tier", tier);
			theConf = hadoopConf;
			job = new Job(hadoopConf, "hadoop tier solver");
			job.setJarByClass(HadoopTierMaster.class);
			job.setOutputKeyClass(IntWritable.class);
			job.setOutputValueClass(FileStatus.class);
			job.setMapperClass(HadoopTierMapper.class);
			job.setReducerClass(HadoopTierReducer.class);
			job.setInputFormatClass(Input.class);
			FileOutputFormat.setOutputPath(job, outputDirectory);
			boolean success = false;
			do {
				try {
					success = job.waitForCompletion(true);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				} catch (ClassNotFoundException e) {
					throw new Error(e);
				}
				String dbUri = gamesmanConf
						.getProperty("gamesman.hadoop.tierDb");
				dbUri = dbUri + "_" + tier + ".db";
				hadoopConf.set("lastTierDb", dbUri);
			} while (!success);
		} catch (IOException e1) {
			throw new Error(e1);
		}
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		args = gop.getRemainingArgs();
		HadoopTierMaster htm = new HadoopTierMaster(gop, args[0]);
		new Thread(htm).start();
	}
}

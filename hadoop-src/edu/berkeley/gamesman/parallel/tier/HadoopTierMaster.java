package edu.berkeley.gamesman.parallel.tier;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHeader;
import edu.berkeley.gamesman.database.SplitFileSystemDatabase;
import edu.berkeley.gamesman.game.TierGame;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class HadoopTierMaster implements Runnable {
	private final org.apache.hadoop.conf.Configuration hadoopConf;
	private final Configuration gamesmanConf;
	private final TierGame game;
	private final FileSystem fs;
	private final Path outputDirectory = new Path("temp_output");
	private final SplitFileSystemDatabase outputDB;
	private Job job;

	public HadoopTierMaster(GenericOptionsParser gop, String confFile)
			throws IOException, ClassNotFoundException {
		hadoopConf = gop.getConfiguration();
		gamesmanConf = new Configuration(confFile);
		fs = FileSystem.get(hadoopConf);
		game = (TierGame) gamesmanConf.getGame();
		DatabaseHeader.setHeaderInfo(gamesmanConf,
				gamesmanConf.getFloat("record.compression", 0F),
				game.recordStates());
		hadoopConf.set("gamesman.configuration", gamesmanConf.serialize());
		String uri = gamesmanConf.getProperty("gamesman.db.uri");
		outputDB = new SplitFileSystemDatabase(uri, gamesmanConf, fs, true,
				new DatabaseHeader(gamesmanConf, 0, -1));
	}

	@Override
	public void run() {
		try {
			if (fs.exists(outputDirectory))
				fs.delete(outputDirectory, true);
			for (int tier = game.numberOfTiers() - 1; tier >= 0; tier--) {
				solve(tier);
			}
			outputDB.close();
			String lastLastTier = hadoopConf.get("gamesman.hadoop.lastTierDb");
			if (lastLastTier != null) {
				fs.delete(new Path(lastLastTier), false);
			}
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	private void solve(int tier) throws IOException {
		hadoopConf.setInt("tier", tier);
		job = new Job(hadoopConf, "hadoop tier solver");
		job.setJarByClass(HadoopTierMapper.class);
		job.setMapOutputValueClass(RangeFile.class);
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
				fs.delete(outputDirectory, true);
				System.out
						.println("On tier " + tier + ", success = " + success);
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			} catch (ClassNotFoundException e) {
				throw new Error(e);
			}
			String dbUri = gamesmanConf.getProperty("gamesman.hadoop.tierDb");
			dbUri = dbUri + "_" + tier + ".db";
			FSDataInputStream in = fs.open(new Path(dbUri));
			Database.skipFully(in, 18);
			Configuration.skipConf(in);
			outputDB.addTierFirstAndCloseStream(in);
			String lastLastTier = hadoopConf.get("gamesman.hadoop.lastTierDb");
			if (lastLastTier != null) {
				fs.delete(new Path(lastLastTier), false);
			}
			hadoopConf.set("gamesman.hadoop.lastTierDb", dbUri);
		} while (!success);
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		args = gop.getRemainingArgs();
		HadoopTierMaster htm = new HadoopTierMaster(gop, args[0]);
		new Thread(htm).start();
	}
}

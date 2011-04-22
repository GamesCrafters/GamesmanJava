package edu.berkeley.gamesman.parallel.tier;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.HDFSSplitDatabase;
import edu.berkeley.gamesman.database.SplitDBMaker;
import edu.berkeley.gamesman.database.SplitLocalDatabase;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.parallel.RangeFile;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

/**
 * The master class for any parallel tier solve
 * 
 * @author dnspies
 */
public class HadoopTierMaster implements Runnable {
	private final org.apache.hadoop.conf.Configuration hadoopConf;
	private final Configuration gamesmanConf;
	private final TierGame game;
	private final FileSystem fs;
	private final Path outputDirectory = new Path("temp_output");
	private final String dbUri;
	private Job job;

	public HadoopTierMaster(GenericOptionsParser gop, String confFile)
			throws IOException, ClassNotFoundException {
		hadoopConf = gop.getConfiguration();
		gamesmanConf = new Configuration(confFile);
		gamesmanConf.setProperty("gamesman.database",
				SplitLocalDatabase.class.getName());
		fs = FileSystem.get(hadoopConf);
		game = (TierGame) gamesmanConf.getGame();
		hadoopConf.set("gamesman.configuration", gamesmanConf.serialize());
		dbUri = gamesmanConf.getProperty("gamesman.hadoop.tierDb");
	}

	@Override
	public void run() {
		try {
			if (fs.exists(outputDirectory))
				fs.delete(outputDirectory, true);
			for (int tier = game.numberOfTiers() - 1; tier >= 0; tier--) {
				solve(tier);
			}
			String uri = gamesmanConf.getProperty("gamesman.db.uri");
			SplitDBMaker outputDB = new SplitDBMaker(uri, gamesmanConf);
			for (int tier = 0; tier < game.numberOfTiers(); tier++) {
				String tierUri = dbUri + "_" + tier + ".db";
				outputDB.addDb(HDFSSplitDatabase.class.getName(), tierUri,
						game.hashOffsetForTier(tier),
						game.numHashesForTier(tier));
			}
			outputDB.close();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	private void solve(int tier) throws IOException {
		String tierUri = dbUri + "_" + tier + ".db";
		boolean doneAlready = fs.exists(new Path(tierUri));
		if (doneAlready) {
			setLastTierUri(tier);
			return;
		}
		hadoopConf.setInt("tier", tier);
		job = new Job(hadoopConf, game.getClass().getSimpleName()
				+ " solver for tier " + tier);
		job.setJarByClass(HadoopTierMapper.class);
		job.setMapOutputValueClass(RangeFile.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		job.setMapperClass(HadoopTierMapper.class);
		job.setReducerClass(HadoopTierReducer.class);
		job.setInputFormatClass(TierInput.class);
		FileOutputFormat.setOutputPath(job, outputDirectory);
		boolean interrupted;
		do {
			interrupted = false;
			try {
				boolean successful = job.waitForCompletion(true);
				if (!successful)
					throw new Error("Tier " + tier + " failed");
			} catch (InterruptedException e) {
				e.printStackTrace();
				interrupted = true;
			} catch (ClassNotFoundException e) {
				throw new Error(e);
			}
		} while (interrupted);
		System.out.println("Tier " + tier + " successful");
		fs.delete(outputDirectory, true);
		setLastTierUri(tier);
	}

	private void setLastTierUri(int tier) {
		hadoopConf.set("gamesman.hadoop.lastTierDb",
				gamesmanConf.getProperty("gamesman.hadoop.tierDb") + "_" + tier
						+ ".db");
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		args = gop.getRemainingArgs();
		HadoopTierMaster htm = new HadoopTierMaster(gop, args[0]);
		new Thread(htm).start();
	}
}

package edu.berkeley.gamesman.parallel.tier;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.HDFSInfo;
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
public class HadoopTierMaster {
	private final org.apache.hadoop.conf.Configuration hadoopConf;
	private final Configuration gamesmanConf;
	private final TierGame game;
	private final String outputDirectory = "temp_output";
	private final String dbUri;
	private Job job;

	/**
	 * Constructs a master using options pulled from a hadoop
	 * GenericOptionsParser
	 * 
	 * @param gop
	 *            The Parser used on the command line args
	 * @param confFile
	 *            The configuration file to read in game-solving info from
	 * @throws IOException
	 *             If an IOException occurs while accessing hdfs
	 * @throws ClassNotFoundException
	 *             If creating the gamesman Configuration can't find game,
	 *             solver, etc.
	 */
	public HadoopTierMaster(GenericOptionsParser gop, String confFile)
			throws IOException, ClassNotFoundException {
		hadoopConf = gop.getConfiguration();
		HDFSInfo.initialize(hadoopConf);
		gamesmanConf = new Configuration(confFile);
		gamesmanConf.setProperty("gamesman.database",
				SplitLocalDatabase.class.getName());
		game = (TierGame) gamesmanConf.getGame();
		hadoopConf.set("gamesman.configuration", gamesmanConf.serialize());
		dbUri = gamesmanConf.getProperty("gamesman.hadoop.tierDb");
	}

	public void run() {
		try {
			FileSystem fs = HDFSInfo.getHDFS(outputDirectory);
			Path outputPath = new Path(outputDirectory);
			if (fs.exists(outputPath))
				fs.delete(outputPath, true);
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
		} catch (URISyntaxException e) {
			throw new Error(e);
		}
	}

	private void solve(int tier) throws IOException, URISyntaxException {
		String tierUri = dbUri + "_" + tier + ".db";
		FileSystem tierFS = HDFSInfo.getHDFS(tierUri);
		boolean doneAlready = tierFS.exists(new Path(tierUri));
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
		Path outputPath = new Path(outputDirectory);
		FileOutputFormat.setOutputPath(job, outputPath);
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
		FileSystem outputFs = HDFSInfo.getHDFS(outputDirectory);
		outputFs.delete(outputPath, true);
		setLastTierUri(tier);
	}

	private void setLastTierUri(int tier) {
		hadoopConf.set("gamesman.hadoop.lastTierDb",
				gamesmanConf.getProperty("gamesman.hadoop.tierDb") + "_" + tier
						+ ".db");
	}

	/**
	 * Runs a parallel tier-solve
	 * 
	 * @param args
	 *            hadoop options and job-file for solving
	 * @throws IOException
	 *             If an IOException occurs while accessing hdfs
	 * @throws ClassNotFoundException
	 *             If a ClassNotFoundException occurs while reading from job
	 *             file
	 */
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		args = gop.getRemainingArgs();
		HadoopTierMaster htm = new HadoopTierMaster(gop, args[0]);
		htm.run();
	}
}

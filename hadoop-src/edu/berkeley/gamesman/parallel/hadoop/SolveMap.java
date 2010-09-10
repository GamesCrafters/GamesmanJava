package edu.berkeley.gamesman.parallel.hadoop;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.database.GZippedFileDatabase;
import edu.berkeley.gamesman.database.SplitFileSystemDatabase;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.Util;

public class SolveMap extends MapReduceBase implements
		Mapper<LongWritable, LongWritable, IntWritable, LocFileWritable> {
	private SplitFileSystemDatabase previousTier;
	private TierGame game;
	private Database writeDb;
	private String writeUri;
	private String zipUri;
	private Configuration conf;
	private TierSolver solver;
	private int tier;
	private long firstHash;
	private long numHashes;
	private FileSystem hdfs;
	private IntWritable tierWritable;

	public void map(LongWritable firstWritable, LongWritable numWritable,
			OutputCollector<IntWritable, LocFileWritable> output,
			Reporter reporter) throws IOException {
		firstHash = firstWritable.get();
		numHashes = numWritable.get();
		writeDb = Database.openDatabase(FileDatabase.class.getName(), writeUri,
				conf, true, previousTier.getHeader(firstHash, numHashes));
		solver.setWriteDb(writeDb);
		int threads = conf.getInteger("gamesman.threads", 1);
		List<WorkUnit> list = null;
		WorkUnit wu = solver.prepareSolve(conf, tier, firstHash, numHashes);
		if (threads > 1)
			list = wu.divide(threads);
		else {
			list = new ArrayList<WorkUnit>(1);
			list.add(wu);
		}
		ArrayList<Thread> myThreads = new ArrayList<Thread>(list.size());
		ThreadGroup solverGroup = new ThreadGroup("Solver Group: "
				+ game.describe());
		for (WorkUnit w : list) {
			Thread t = new Thread(solverGroup, new TierSlaveRunnable(w));
			t.start();
			myThreads.add(t);
		}
		for (Thread t : myThreads)
			while (t.isAlive())
				try {
					t.join();
				} catch (InterruptedException e) {
					new Exception("Interrupted while joined on thread " + t)
							.printStackTrace();
				}
		previousTier.close();
		//TODO Don't do this
		long maxMem = conf.getLong("gamesman.memory", Integer.MAX_VALUE);
		Database readFrom = writeDb;
		GZippedFileDatabase writeTo;
		try {
			writeTo = new GZippedFileDatabase(zipUri, conf, readFrom, maxMem);
		} catch (IOException e) {
			throw new Error(e);
		}
		Thread[] threadList = new Thread[threads];
		DatabaseHandle[] readHandle = new DatabaseHandle[threads];
		for (int i = 0; i < threads; i++) {
			readHandle[i] = readFrom.getHandle();
			threadList[i] = new Thread(writeTo);
			threadList[i].start();
		}
		for (int i = 0; i < threads; i++) {
			while (threadList[i].isAlive())
				try {
					threadList[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		readFrom.close();
		writeTo.close();
		new File(writeUri).delete();
		Path p = new Path(zipUri);
		hdfs.copyFromLocalFile(p, p);
		new File(zipUri).delete();
		output.collect(tierWritable, new LocFileWritable(firstWritable, hdfs
				.getFileStatus(p)));
	}

	@Override
	public void configure(JobConf job) {
		Path prevTierPath = new Path(conf.getProperty("gamesman.db.uri")
				+ ".tmp");
		try {
			hdfs = FileSystem.get(job);
			FSDataInputStream prevTierInputStream = hdfs.open(prevTierPath);
			previousTier = new SplitFileSystemDatabase(prevTierPath,
					prevTierInputStream, hdfs);
			conf = previousTier.getConfiguration();
			this.tier = job.getInt("tierNum", -1);
			if (tier < 0)
				throw new Error("Tier not set");
			tierWritable = new IntWritable(tier);
			game = (TierGame) conf.getGame();
			String solverName;
			solverName = conf.getProperty("gamesman.solver", "TierSolver");
			Class<? extends TierSolver> solverc = Class.forName(
					"edu.berkeley.gamesman.solver." + solverName).asSubclass(
					TierSolver.class);
			solver = solverc.getConstructor(Configuration.class).newInstance(
					conf);
			solver.setReadDb(previousTier);
			String foldUri = conf.getProperty("gamesman.parallel.dbfolder");
			LocalFileSystem lfs = FileSystem.getLocal(job);
			File f = lfs.pathToFile(new Path(foldUri));
			if (!f.exists())
				f.mkdir();
			zipUri = foldUri + File.separator + "s" + firstHash + ".db";
			writeUri = zipUri + ".uz";
		} catch (IllegalAccessException e) {
			throw new Error(e);
		} catch (InstantiationException e) {
			throw new Error(e);
		} catch (IllegalArgumentException e) {
			throw new Error(e);
		} catch (SecurityException e) {
			throw new Error(e);
		} catch (InvocationTargetException e) {
			throw new Error(e.getCause());
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	public Task createTask(String name) {
		return new TierSlaveTextTask(name);
	}

	private class TierSlaveRunnable implements Runnable {
		WorkUnit w;

		TierSlaveRunnable(WorkUnit u) {
			w = u;
		}

		public void run() {
			w.conquer();
		}
	}

	private class TierSlaveTextTask extends Task {
		private String name;

		TierSlaveTextTask(String name) {
			this.name = name;
		}

		private long start;

		@Override
		protected void begin() {
			start = System.currentTimeMillis();
		}

		@Override
		public void complete() {
			System.out.println("\nCompleted task " + name + " in "
					+ Util.millisToETA(System.currentTimeMillis() - start)
					+ ".");
		}

		@Override
		public void update() {
			long elapsedMillis = System.currentTimeMillis() - start;
			double fraction = (double) completed / total;
			System.out.print("Task: "
					+ name
					+ ", "
					+ String.format("%4.02f", fraction * 100)
					+ "% ETA "
					+ Util.millisToETA((long) (elapsedMillis / fraction)
							- elapsedMillis) + " remains\n");
		}
	}
}

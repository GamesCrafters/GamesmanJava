package edu.berkeley.gamesman.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.hadoop.util.SplitDatabaseWritable;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

public class TierMap<S> implements
		Mapper<LongWritable, LongWritable, LongWritable, SplitDatabaseWritable> {

	public static abstract class MapReduceDatabase extends Database {
		public MapReduceDatabase() {
		}

		public MapReduceDatabase(FileSystem fs) {
			this.fs = fs;
		}

		public void setFilesystem(FileSystem fs) {
			this.fs = fs;
		}

		void setDelegate(TierMap tmr) {
			this.delegate = tmr;
		}

		public void setOutputDirectory(Path dir) {
			outputFilenameBase = dir;
			// dir contains a trailing slash
		}

		protected TierMap<?> delegate;

		protected FileSystem fs;

		protected Path outputFilenameBase;
	}

	protected TieredGame<S> game;

	protected Hasher<S> hasher;

	protected TierSolver solver;

	protected MapReduceDatabase db;

	int tier;

	private Reporter reporter;

	private OutputCollector<LongWritable, SplitDatabaseWritable> outRec;

	public static Configuration config;

	public static JobConf jobconf;

	public void configure(JobConf conf) {
		// Class<TieredGame<Object>> gc = null;
		// Class<Database> gd = null;
		// Class<Hasher<?>> gh = null;
		final String base = "edu.berkeley.gamesman.";
		// Properties props = new Properties(System.getProperties());

		try {
			config = Configuration.load(Util.decodeBase64(conf
					.get("configuration_data")));
			jobconf = conf;
			db = Util.typedInstantiate(base + "database."
					+ config.getProperty("gamesman.database"),
					MapReduceDatabase.class);
		} catch (ClassNotFoundException e) {
			Util.fatalError("failed to load configuration class!", e);
			return;
		}

		// db.initialize(conf.get("dburi"),config);
		try {
			db.setFilesystem(FileSystem.get(jobconf));
		} catch (IOException e) {
			Util.fatalError("Unable to get filesystem", e);
		}
		db.setOutputDirectory(FileOutputFormat.getWorkOutputPath(jobconf));
		db.initialize(FileOutputFormat.getOutputPath(conf).toString(), config);
		tier = Integer.parseInt(jobconf.get("tier"));
		game = Util.checkedCast(config.getGame());
		hasher = Util.checkedCast(config.getHasher());
		try {
			solver = Util.typedInstantiate(base + "solver."
					+ config.getProperty("gamesman.solver"), TierSolver.class);
		} catch (ClassNotFoundException e) {
			Util.fatalError("failed to load configuration class!", e);
			return;
		}

		assert Util.debugFormat(DebugFacility.HADOOP,
				"Hadoop is ready to work! (%s, %s)", game.describe(), hasher
						.toString());
	}

	public void close() throws IOException {
		db.close();
	}

	public void map(LongWritable startHash, LongWritable endHash,
			OutputCollector<LongWritable, SplitDatabaseWritable> outRec,
			Reporter reporter) throws IOException {
		this.reporter = reporter;
		this.outRec = outRec;
		solve(startHash.get(), endHash.get());
	}

	private void solve(long startHash, long endHash) {
		int threads = config.getInteger("gamesman.threads", 1);
		assert Util.debug(DebugFacility.MASTER, "Launching " + threads
				+ " threads for " + startHash + "-" + (endHash - 1));
		List<WorkUnit> list = solver.prepareSolve(config, tier, startHash,
				endHash).divide(threads);

		ArrayList<Thread> myThreads = new ArrayList<Thread>(threads);

		ThreadGroup solverGroup = new ThreadGroup("Solver Group: "
				+ config.getGame());
		for (WorkUnit w : list) {
			Thread t = new Thread(solverGroup, new HadoopMasterRunnable(w));
			t.start();
			myThreads.add(t);
		}

		for (Thread t : myThreads)
			try {
				t.join();
			} catch (InterruptedException e) {
				Util.warn("Interrupted while joined on thread " + t);
			}
		assert Util.debug(DebugFacility.MASTER, "Finished master run");
	}

	public void started(int tier, String filename, long startRecord,
			long stopRecord) {
		reporter.setStatus("Started " + tier + " [" + startRecord + "-"
				+ stopRecord + "]");
	}

	public void finished(int tier, String filename, long startRecord,
			long stopRecord) {
		reporter.progress();
		SplitDatabaseWritable w = new SplitDatabaseWritable(tier);
		w.set(filename, startRecord, stopRecord);
		try {
			outRec.collect(new LongWritable(startRecord), w);
		} catch (IOException e) {
			Util.warn("Failed to collect finished database " + tier + "@"
					+ filename, e);
		}
	}

	private class HadoopMasterRunnable implements Runnable {
		WorkUnit w;

		private HadoopMasterRunnable(WorkUnit u) {
			w = u;
		}

		public void run() {
			assert Util.debug(DebugFacility.MASTER,
					"HadoopMasterRunnable begin");
			w.conquer();
			assert Util.debug(DebugFacility.MASTER, "HadoopMasterRunnable end");
		}
	}

}

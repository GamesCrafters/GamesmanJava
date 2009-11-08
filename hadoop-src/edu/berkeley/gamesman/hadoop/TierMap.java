package edu.berkeley.gamesman.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.hadoop.util.HadoopUtil;
import edu.berkeley.gamesman.hadoop.util.HadoopSplitDatabaseWritable;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * The Mapper in the hadoop solver. Maps a set of ranges to databases that contain
 * each range. The set of ranges is further divided up into WorkUnits that are
 * used in each thread (WorkUnit.divide(), WorkUnit.conquer()
 *
 * @param <S> The state that the Game holds as it is being solved.
 * 
 * @author Patrick Horn
 */
@SuppressWarnings("deprecation")
public class TierMap<S> implements
		Mapper<LongWritable, LongWritable, IntWritable, HadoopSplitDatabaseWritable> {

	protected TieredGame<S> game;

	protected Hasher<S> hasher;

	protected TierSolver<S> solver;

	protected HadoopUtil.MapReduceDatabase db;

	int tier;

	private Reporter reporter;

	private OutputCollector<IntWritable, HadoopSplitDatabaseWritable> outRec;

	private Configuration config;

	private JobConf jobconf;

	private boolean initialized = false;

	public void configure(JobConf conf) {
		// Class<TieredGame<Object>> gc = null;
		// Class<Database> gd = null;
		// Class<Hasher<?>> gh = null;
		final String base = "edu.berkeley.gamesman.";
		// Properties props = new Properties(System.getProperties());
		jobconf = conf;
		if (jobconf == null) {
			Util.fatalError("JobConf is null in TierMap.configure");
		}
		try {
			config = Configuration.load(Util.decodeBase64(conf
					.get("configuration_data")));
			db = Util.typedInstantiate(base + "database."
					+ config.getProperty("gamesman.database"),
					HadoopUtil.MapReduceDatabase.class);
		} catch (ClassNotFoundException e) {
			Util.fatalError("failed to load configuration class!", e);
			return;
		}

		tier = Integer.parseInt(jobconf.get("tier"));
		game = Util.checkedCast(config.getGame());
		hasher = Util.checkedCast(config.getHasher());
		try {
			solver = Util.checkedCast(Util.typedInstantiate(base + "solver."
					+ config.getProperty("gamesman.solver"), TierSolver.class));
			config.db = db;
			solver.initialize(config);
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
			OutputCollector<IntWritable, HadoopSplitDatabaseWritable> outRec,
			Reporter reporter) throws IOException {
		try {
			Util.debug(DebugFacility.HADOOP,
					"Map "+startHash+" - "+endHash);
			if (!initialized) {
				initialized = true;
				// db.initialize(conf.get("dburi"),config);
				try {
					FileSystem fs = FileSystem.get(jobconf);
					if (fs == null) {
						Util.fatalError("Null filesystem in TierMap.configure!");
					}
					db.setFilesystem(fs);
				} catch (IOException e) {
					Util.fatalError("Unable to get filesystem", e);
				}
				Util.debug(DebugFacility.HADOOP,
						"Work output path is "+
						FileOutputFormat.getWorkOutputPath(jobconf));
				Util.debug(DebugFacility.HADOOP,
						"Output path is "+
						FileOutputFormat.getOutputPath(jobconf));
				db.setOutputDirectory(FileOutputFormat.getWorkOutputPath(jobconf));
				db.setDelegate(this);
				db.initialize(jobconf.get("previousTierDb", null), config);
			}
			this.reporter = reporter;
			this.outRec = outRec;
			solve(startHash.get(), endHash.get());
		} catch (Util.FatalError fe) {
			throw new RuntimeException(fe);
		}
	}

	private void solve(long startHash, long endHash) {
		int threads = config.getInteger("gamesman.threads", 1);
		assert Util.debug(DebugFacility.HADOOP, "Launching " + threads
				+ " threads for " + startHash + "-" + endHash);
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
		assert Util.debug(DebugFacility.HADOOP, "Finished hadoop run");
	}

	/** Called by the HadoopSplitDatabase when a child database is opened.
	 *  Currently, just writes to the log.
	 * 
	 * @param tier Tier number (constant for whole Mapper)
	 * @param filename Database filename that was just closed.
	 * @param startRecord First record in database
	 * @param stopRecord 1 + Last record in database.
	 */
	public synchronized void started(int tier, String filename, long startRecord,
			long stopRecord) {
		reporter.setStatus("Started " + tier + " [" + startRecord + "-"
				+ stopRecord + "]");
	}

	/** Called by the HadoopSplitDatabase when a child database file is closed
	 * 
	 * @param tier Tier number (constant for whole Mapper)
	 * @param filename Database filename that was just closed.
	 * @param startRecord First record in database
	 * @param stopRecord 1 + Last record in database.
	 */
	public synchronized void finished(int tier, String filename, long startRecord,
			long stopRecord) {
		try {
		FileSystem fs = FileSystem.get(jobconf);
		System.out.println("Testing Output file at "+filename);
		System.out.println("; "+FileOutputFormat.getWorkOutputPath(jobconf));
		if (!fs.exists(new org.apache.hadoop.fs.Path(FileOutputFormat.getWorkOutputPath(jobconf), filename))) {
			System.out.println("Fatal error: Output file at "+filename+" was never created!");
			boolean reallyExists= fs.exists(new org.apache.hadoop.fs.Path(HadoopUtil.getTierPath(jobconf, config, tier), filename));
			Util.fatalError("Failed to make "+filename+"; tier="+tier+"; sR="+startRecord+"; eR="+stopRecord+"; exists outside tempdir="+reallyExists);
		}
		} catch (IOException e){
			throw new RuntimeException(e);
		}
		reporter.progress();
		HadoopSplitDatabaseWritable w = new HadoopSplitDatabaseWritable();
		w.set(filename, startRecord, stopRecord);
		try {
			outRec.collect(new IntWritable(tier), w);
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
			assert Util.debug(DebugFacility.HADOOP,
					"HadoopMasterRunnable begin");
			try {
				w.conquer();
			} catch (Exception ee) {
				System.out.println("[TierMap] Exception in WorkUnit "+w);
				ee.printStackTrace(System.out);
			} catch (Util.FatalError fe) {
				System.out.println("[TierMap] FatalError in WorkUnit "+w);
				fe.printStackTrace(System.out);
				throw fe;
			}
			assert Util.debug(DebugFacility.HADOOP, "HadoopMasterRunnable end");
		}
	}

}

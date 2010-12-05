package edu.berkeley.gamesman.master;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHeader;
import edu.berkeley.gamesman.database.GZippedFileDatabase;
import edu.berkeley.gamesman.database.SplitFileSystemDatabase;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.solver.Solver;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.Util;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.GamesmanConf;

public class HadoopTierMapper extends
		Mapper<Range, IntWritable, IntWritable, RangeFile> {
	private final RangeFile mapValue = new RangeFile();
	private final IntWritable tier = new IntWritable();
	private TierSolver solver;
	private TierGame game;
	private Database writeDb;
	private SplitFileSystemDatabase readDb;
	private boolean doZip;
	private String writeURI;

	Configuration conf;
	FileSystem fs;
	FSDataInputStream is;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration conf = context
					.getConfiguration();
			this.conf = new GamesmanConf(conf);
			game = (TierGame) this.conf.getGame();

			fs = FileSystem.get(conf);
			tier.set(conf.getInt("tier", -1));
			if (tier.get() == -1)
				throw new Error("No tier specified");
			String solverName = this.conf.getProperty("gamesman.solver");
			Class<? extends TierSolver> solverc = Util.typedForName(
					"edu.berkeley.gamesman.solver." + solverName, Solver.class)
					.asSubclass(TierSolver.class);
			try {
				solver = solverc.getConstructor(Configuration.class)
						.newInstance(this.conf);
			} catch (IllegalArgumentException e) {
				throw new Error(e);
			} catch (SecurityException e) {
				throw new Error(e);
			} catch (InstantiationException e) {
				throw new Error(e);
			} catch (IllegalAccessException e) {
				throw new Error(e);
			} catch (InvocationTargetException e) {
				throw new Error(e.getCause());
			} catch (NoSuchMethodException e) {
				throw new Error(e);
			}
			doZip = conf.getBoolean("gamesman.remote.zipped", false);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	private class HadoopMapperRunnable implements Runnable {
		WorkUnit w;

		HadoopMapperRunnable(WorkUnit u) {
			w = u;
		}

		public void run() {
			w.conquer();
		}
	}

	@Override
	public void map(Range key, IntWritable value, Context context)
			throws IOException {
		long firstHash = key.firstRecord;
		long numHashes = key.numRecords;
		DatabaseHeader head = new DatabaseHeader(conf, firstHash, numHashes);
		int prevTier = tier.get() + 1;
		if (prevTier < game.numberOfTiers() - 1) {
			String readUri = conf.getProperty("gamesman.hadoop.lastTierDb");
			Path readPath = new Path(readUri);
			is = fs.open(readPath);
			readDb = new SplitFileSystemDatabase(readPath, is, fs);
		} else {
			readDb = null;
		}

		// setting write database file name, path etc and initializing it*******
		String foldUri = conf.getProperty("gamesman.hadoop.dbfolder");
		doZip = conf.getBoolean("gamesman.hadoop.zipped", false);
		writeURI = foldUri + File.separator + "s" + tier.get() + "_"
				+ firstHash + ".db";
		String zipURI = null;
		if (doZip) {
			zipURI = writeURI;
			writeURI = writeURI + ".uz";
		}
		writeDb = Database.openDatabase(writeURI, conf, true, head);
		solver.setWriteDb(writeDb);
		// ***********************************************************************

		int tier = this.tier.get();
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
			Thread t = new Thread(solverGroup, new HadoopMapperRunnable(w));
			t.start();
			myThreads.add(t);
		}
		for (Thread t : myThreads)
			while (t.isAlive())
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		if (readDb != null) {
			readDb.close();
		}
		if (doZip) {
			long maxMem = conf.getLong("gamesman.memory", Integer.MAX_VALUE);
			Database readFrom = writeDb;
			GZippedFileDatabase writeTo;
			try {
				writeTo = new GZippedFileDatabase(zipURI, conf, readFrom,
						maxMem);
			} catch (IOException e) {
				throw new Error(e);
			}
			Thread[] threadList = new Thread[threads];
			for (int i = 0; i < threads; i++) {
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
			writeTo.close();
		}
		writeDb.close();
		Path p;
		if (zipURI == null)
			p = new Path(writeURI);
		else
			p = new Path(zipURI);
		fs.copyFromLocalFile(p, p);
		FileStatus finalFile = fs.getFileStatus(p);
		boolean successful = false;
		while (!successful) {
			try {
				mapValue.set(key, finalFile);
				context.write(this.tier, mapValue);
				successful = true;
			} catch (InterruptedException e) {
				successful = false;
				e.printStackTrace();
			}
		}
	}
}

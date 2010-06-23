package edu.berkeley.gamesman.parallel;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.DatabaseHeader;
import edu.berkeley.gamesman.database.DistributedDatabase;
import edu.berkeley.gamesman.database.GZippedFileDatabase;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.TaskFactory;
import edu.berkeley.gamesman.util.Util;

public class TierSlave implements TaskFactory, Runnable {
	private final TierGame game;
	private final DistributedDatabase readDb;
	private final Database writeDb;
	private final String writeUri;
	private final String zipUri;
	private final Configuration conf;
	private final TierSolver solver;
	private final int tier;
	private final long firstHash;
	private final long numHashes;
	private final boolean zipOnFinish;

	public TierSlave(Configuration conf, Class<? extends TierSolver> solverc,
			int tier) {
		Task.setTaskFactory(this);
		try {
			this.tier = tier;
			game = (TierGame) conf.getGame();
			this.conf = conf;
			solver = solverc.getConstructor(Configuration.class).newInstance(
					conf);
			int prevTier = tier + 1;
			byte[] headBytes = new byte[18];
			Database.readFully(System.in, headBytes, 0, 18);
			DatabaseHeader head = new DatabaseHeader(headBytes);
			firstHash = head.firstRecord;
			numHashes = head.numRecords;
			if (prevTier < game.numberOfTiers()) {
				long firstTierRecord = game.hashOffsetForTier(prevTier);
				long numTierRecords = game.numHashesForTier(prevTier);
				readDb = new DistributedDatabase(conf, firstTierRecord,
						numTierRecords, head.getHeader(firstTierRecord,
								numTierRecords), new Scanner(System.in),
						System.out);
				solver.setReadDb(readDb);
			} else {
				readDb = null;
			}
			String foldUri = conf.getProperty("gamesman.parallel.dbfolder");
			File f = new File(foldUri);
			if (!f.exists())
				f.mkdir();
			String path = conf.getProperty("gamesman.remote.path");
			if (!(foldUri.startsWith("/") || foldUri.startsWith(path)))
				foldUri = path + "/" + foldUri;
			zipOnFinish = conf.getBoolean("gamesman.remote.zipped", false);
			if (zipOnFinish) {
				zipUri = foldUri + "/s" + firstHash + ".db";
				writeUri = zipUri + ".uz";
			} else {
				writeUri = foldUri + "/s" + firstHash + ".db";
				zipUri = null;
			}
			writeDb = Database.openDatabase(writeUri, conf, true, head);
			solver.setWriteDb(writeDb);
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
		}
	}

	public static void main(String[] args) throws ClassNotFoundException {
		String jobFile = null;
		jobFile = args[0];
		int tier = Integer.parseInt(args[1]);
		Properties props = Configuration.readProperties(jobFile);
		EnumSet<DebugFacility> debugOpts = EnumSet.noneOf(DebugFacility.class);
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		cl.setDefaultAssertionStatus(false);
		for (DebugFacility f : DebugFacility.values()) {
			if (Boolean.valueOf(props.getProperty("gamesman.debug."
					+ f.toString(), "false"))) {
				debugOpts.add(f);
				f.setupClassloader(cl);
			}
		}
		if (!debugOpts.isEmpty()) {
			debugOpts.add(DebugFacility.CORE);
			DebugFacility.CORE.setupClassloader(cl);
			Util.enableDebuging(debugOpts);
		}
		Configuration conf = new Configuration(props);
		Thread.currentThread().setName("Gamesman");

		String solverName;
		solverName = conf.getProperty("gamesman.solver", "TierSolver");
		if (solverName == null)
			throw new Error(
					"You must specify a solver with the property gamesman.solver");

		Class<? extends TierSolver> s = null;
		try {
			s = Class.forName("edu.berkeley.gamesman.solver." + solverName)
					.asSubclass(TierSolver.class);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		TierSlave m = new TierSlave(conf, s, tier);
		m.run();
	}

	public Task createTask(String name) {
		return new TierSlaveTextTask(name);
	}

	public void run() {
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
		if (readDb != null)
			readDb.close();
		if (zipOnFinish) {
			long time = System.currentTimeMillis();
			int entryKB = conf.getInteger("gamesman.db.zip.entryKB", 64);
			long maxMem = conf.getLong("gamesman.memory", Integer.MAX_VALUE);
			Database readFrom = writeDb;
			GZippedFileDatabase writeTo;
			try {
				writeTo = new GZippedFileDatabase(zipUri, conf, readFrom,
						maxMem);
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
			System.out.println("Zipped in "
					+ Util.millisToETA(System.currentTimeMillis() - time));
		}
		System.out.println("finished: " + writeUri + " " + firstHash + " "
				+ numHashes);
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

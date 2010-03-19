package edu.berkeley.gamesman.parallel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.DistributedDatabase;
import edu.berkeley.gamesman.game.TieredGame;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.TaskFactory;
import edu.berkeley.gamesman.util.Util;

/**
 * The main class for slave computers in a parallel solve
 * 
 * @author dnspies
 */
public class TierSlave {
	/**
	 * Called by TierMaster via ssh
	 * 
	 * @param args
	 *            Job file, tier, first hash, number of hashes
	 * @throws ClassNotFoundException
	 *             When initializing the configuration
	 * @throws IllegalAccessException
	 *             When instantiating the solver
	 * @throws InstantiationException
	 *             When instantiating the solver
	 * @throws IOException
	 *             Everywhere
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			IllegalAccessException, InstantiationException, IOException {
		String jobFile = args[0];
		int tier;
		long firstHash, numHashes;
		tier = Integer.parseInt(args[1]);
		firstHash = Long.parseLong(args[2]);
		numHashes = Long.parseLong(args[3]);
		Properties props = Configuration.readProperties(jobFile);
		EnumSet<DebugFacility> debugOpts = EnumSet.noneOf(DebugFacility.class);
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		cl.setDefaultAssertionStatus(false);
		for (DebugFacility f : DebugFacility.values()) {
			if (parseBoolean(props.getProperty(
					"gamesman.debug." + f.toString(), "false"))) {
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
		Task.setTaskFactory(new TaskFactory() {
			public Task createTask(String name) {
				return new SlaveTextTask(name);
			}
		});
		String solverName = conf.getProperty("gamesman.solver");
		Class<? extends Solver> s = Util.typedForName(
				"edu.berkeley.gamesman.solver." + solverName, Solver.class);
		TierSolver<? extends State> solver = Util.checkedCast(s.newInstance());
		solver.initialize(conf);
		DistributedDatabase readDb = new DistributedDatabase(System.in,
				System.out);
		readDb.setTier(tier + 1);
		String parentUri = conf.getProperty("gamesman.slaveDbFolder");
		readDb.initialize(parentUri, conf, true);
		solver.setReadDb(readDb);
		SplitDatabaseCreator writeDb = new SplitDatabaseCreator();
		writeDb.initialize(parentUri, conf, true);
		writeDb.setTier(tier);
		solver.setWriteDb(writeDb);
		int threads = conf.getInteger("gamesman.threads", 1);
		List<WorkUnit> list = null;
		WorkUnit wu = solver.prepareSolve(conf, tier, firstHash, firstHash
				+ numHashes);
		if (threads > 1)
			list = wu.divide(threads);
		else {
			list = new ArrayList<WorkUnit>(1);
			list.add(wu);
		}
		ArrayList<Thread> myThreads = new ArrayList<Thread>(list.size());
		TieredGame<? extends State> game = (TieredGame<? extends State>) conf
				.getGame();
		ThreadGroup solverGroup = new ThreadGroup("Solver Group: " + game);
		for (WorkUnit w : list) {
			Thread t = new Thread(solverGroup, new TierSlaveRunnable(w));
			t.start();
			myThreads.add(t);
		}

		for (Thread t : myThreads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				Util.warn("Interrupted while joined on thread " + t);
			}
		}
		Runtime.getRuntime().exec("sync");
		System.out.println("finished with files: " + writeDb.getStartList());
	}

	// This is a copy of Util.parseBoolean().
	// It needs to be here to avoid loading the Util class before we're ready to
	private static boolean parseBoolean(String s) {
		return s != null && !s.equalsIgnoreCase("false")
				&& !s.equalsIgnoreCase("0");
	}
}

class TierSlaveRunnable implements Runnable {
	WorkUnit w;

	TierSlaveRunnable(WorkUnit u) {
		w = u;
	}

	public void run() {
		w.conquer();
	}
}

class SlaveTextTask extends Task {
	private String name;

	SlaveTextTask(String name) {
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
				+ Util.millisToETA(System.currentTimeMillis() - start) + ".");
	}

	@Override
	public void update() {
		long elapsedMillis = System.currentTimeMillis() - start;
		double fraction = (double) completed / total;
		System.out.println("Task: "
				+ name
				+ ", "
				+ String.format("%4.02f", fraction * 100)
				+ "% ETA "
				+ Util.millisToETA((long) (elapsedMillis / fraction)
						- elapsedMillis) + " remains");
	}
}

package edu.berkeley.gamesman.solver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.util.Util;

/**
 * A Solver is responsible for solving a Game and storing the result to a
 * Database
 * 
 * @author Steven Schlansker
 */
public abstract class Solver {

	/**
	 * The number of positions to go through between each update/reset
	 */
	public static final int STEP_SIZE = 10000000;
	protected final int nThreads;

	protected Database db;
	protected Configuration conf;

	/**
	 * Set the Database to use for this solver
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public Solver(Configuration conf, Database db) {
		this.db = db;
		this.conf = conf;
		nThreads = conf.getInteger("gamesman.threads", 1);
	}

	public abstract Runnable nextAvailableJob();

	public final void solve() {
		System.out.println("Beginning solve for " + conf.getGame().describe()
				+ " using " + getClass().getSimpleName());
		long startTime = System.currentTimeMillis();
		ExecutorService solverService = Executors.newFixedThreadPool(nThreads);
		Runnable nextJob = nextAvailableJob();
		while (nextJob != null) {
			solverService.execute(nextJob);
			nextJob = nextAvailableJob();
		}
		solverService.shutdown();
		while (!solverService.isTerminated()) {
			try {
				solverService.awaitTermination(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Solve completed in "
				+ Util.millisToETA(System.currentTimeMillis() - startTime));
	}
}

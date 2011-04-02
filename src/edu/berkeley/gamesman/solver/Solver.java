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
 * @author David Spies
 */
public abstract class Solver {

	private class RunWrapper implements Runnable {
		private final Runnable inner;

		private RunWrapper(Runnable inner) {
			this.inner = inner;
		}

		@Override
		public void run() {
			try {
				inner.run();
			} catch (Throwable t) {
				if (failed == null) {
					failed = t;
					mainThread.interrupt();
				}
			}
		}

	}

	/**
	 * The number of positions to go through between each update/reset
	 */
	public static final int STEP_SIZE = 10000000;
	public static final long DEFAULT_MIN_SPLIT_SIZE = 1L << 12;
	public static final long DEFAULT_PREFERRED_SPLIT_SIZE = 1L << 23;
	protected final int nThreads;
	private volatile Throwable failed = null;
	private Thread mainThread;

	protected Database db;
	protected Configuration conf;
	private ExecutorService solverService;

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

	private final Runnable getNextJob() throws InterruptedException {
		Runnable nextJob = nextAvailableJob();
		if (nextJob == null)
			return null;
		else
			return new RunWrapper(nextJob);
	}

	/**
	 * Returns the next job which must be executed before completing the solve.
	 * This method may block while waiting for other jobs to finish.
	 * 
	 * @return The next job necessary to solve this game or null if all jobs
	 *         have been returned already
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	public abstract Runnable nextAvailableJob() throws InterruptedException;

	/**
	 * Starts solving the game using a FixedThreadPool
	 */
	public final void solve() {
		System.out.println("Beginning solve for " + conf.getGame().describe()
				+ " using " + getClass().getSimpleName());
		long startTime = System.currentTimeMillis();
		mainThread = Thread.currentThread();
		solverService = Executors.newFixedThreadPool(nThreads);
		Runnable nextJob = null;
		while (true) {
			try {
				nextJob = getNextJob();
			} catch (InterruptedException e) {
				if (failed == null)
					failed = e;
			}
			if (failed != null)
				error();
			if (nextJob == null)
				break;
			else
				solverService.execute(nextJob);
		}
		solverService.shutdown();
		while (!solverService.isTerminated()) {
			try {
				solverService.awaitTermination(20, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				if (failed == null)
					failed = e;
			}
			if (failed != null)
				error();
		}
		System.out.println("Solve completed in "
				+ Util.millisToETA(System.currentTimeMillis() - startTime));
	}

	private void error() throws Error {
		solverService.shutdownNow();
		throw new Error(failed);
	}
}

package edu.berkeley.gamesman.solver;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.util.Util;

/**
 * A Solver is responsible for solving a Game and storing the result to a
 * Database
 *
 * Subclasses are required to implement the following method,
 * @see #nextAvailableJob()
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
				fail(t);
				mainThread.interrupt();
			}
		}

	}

	private synchronized void fail(Throwable t) {
		if (failed == null) {
			failed = t;
		}
	}

	/**
	 * The number of positions to go through between each update/reset
	 */
	private static final int DEFAULT_STEP_SIZE = 10000000;
	public static final long DEFAULT_MIN_SPLIT_SIZE = 1L << 12;
	public static final long DEFAULT_PREFERRED_SPLIT_SIZE = 1L << 23;
	protected final int nThreads;
	private Throwable failed = null;
	private Thread mainThread;
	public final int stepSize;

	protected Database db;
	protected Configuration conf;
	private ExecutorService solverService;

	/**
	 * Set the Database to use for this solver
	 *
	 * @param conf The configuration object
	 * @param db A backing database
	 */
	public Solver(Configuration conf, Database db) {
		this.db = db;
		this.conf = conf;
		nThreads = conf.getInteger("gamesman.threads", 1);
		stepSize = conf.getInteger("gamesman.solver.step.size", DEFAULT_STEP_SIZE);
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
	 * have been returned already
	 * @throws InterruptedException If the thread is interrupted while waiting.
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
		LinkedList<Future<?>> fQueue = new LinkedList<Future<?>>();
		while (true) {
			try {
				nextJob = getNextJob();
			} catch (InterruptedException e) {
				fail(e);
			}
			if (failed != null)
				error();
			if (nextJob == null)
				break;
			else
				fQueue.add(solverService.submit(nextJob));
		}
		solverService.shutdown();
		try {
			while (!fQueue.isEmpty()) {
				fQueue.remove().get();
			}
		} catch (InterruptedException e) {
			fail(e);
		} catch (ExecutionException e) {
			fail(e.getCause());
		}
		if (failed != null)
			error();
		else
			System.out.println("Solve completed in "
					+ Util.millisToETA(System.currentTimeMillis() - startTime));
	}

	private void error() {
		solverService.shutdownNow();
		throw new Error(failed);
	}
}

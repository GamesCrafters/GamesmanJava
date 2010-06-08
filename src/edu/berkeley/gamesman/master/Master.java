package edu.berkeley.gamesman.master;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseWrapper;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.solver.Solver;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.TaskFactory;
import edu.berkeley.gamesman.util.Util;

/**
 * LocalMaster runs one or more threads on the local machine to solve a game
 * 
 * @author Steven Schlansker
 */
public final class Master implements TaskFactory {

	private final Game<? extends State> game;

	private final Solver solver;

	private final Database database;

	private final Configuration conf;

	public Master(Configuration conf, Class<? extends Solver> solverc) {

		Task.setTaskFactory(this);

		try {
			game = conf.getGame();
			this.database = Database.openDatabase(conf, true);
			conf.db = database;
			this.conf = conf;
			solver = solverc.getConstructor(Configuration.class).newInstance(
					conf);
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
		}
		assert Util.debug(DebugFacility.MASTER, "Done initializing Master");
	}

	public void run(boolean closeDB) {
		int threads = conf.getInteger("gamesman.threads", 1);
		assert Util.debug(DebugFacility.MASTER, "Launching " + threads
				+ " threads...");
		List<WorkUnit> list = null;
		WorkUnit wu = solver.prepareSolve(conf);
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
			Thread t = new Thread(solverGroup, new LocalMasterRunnable(w));
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
		if (closeDB)
			database.close();
		assert Util.debug(DebugFacility.MASTER, "Finished master run");
	}

	public void run() {
		run(true);
	}

	private class LocalMasterRunnable implements Runnable {
		WorkUnit w;

		LocalMasterRunnable(WorkUnit u) {
			w = u;
		}

		public void run() {
			assert Util
					.debug(DebugFacility.MASTER, "LocalMasterRunnable begin");
			w.conquer();
			assert Util.debug(DebugFacility.MASTER, "LocalMasterRunnable end");
		}
	}

	private class LocalMasterTextTask extends Task {
		private String name;

		LocalMasterTextTask(String name) {
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
							- elapsedMillis) + " remains\r");
		}
	}

	public Task createTask(String name) {
		return new LocalMasterTextTask(name);
	}

	/**
	 * @return The solver
	 */
	public Solver getSolver() {
		return solver;
	}

	/**
	 * @return the game
	 */
	public Game<?> getGame() {
		return game;
	}

	/**
	 * @return the database
	 */
	public Database getDatabase() {
		return database;
	}

}

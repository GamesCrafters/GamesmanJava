package edu.berkeley.gamesman.master;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Master;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * TieredHadoopMaster
 * 
 * @author dnspies
 */
public final class TieredHadoopMaster implements Master {

	private Configuration conf;

	private TierSolver<?> solver;

	private Database database;

	public void initialize(Configuration conf, Class<? extends Solver> solver,
			Class<? extends Database> database, boolean cached) {
		try {
			this.conf = conf;
			this.solver = (TierSolver<?>) solver.newInstance();
			this.database = database.newInstance();
		} catch (InstantiationException e) {
			Util.fatalError("Not Instantiated", e);
		} catch (IllegalAccessException e) {
			Util.fatalError("Not Instantiated", e);
		}
	}

	public void run() {
		run(true);
	}

	public void run(boolean closeDB) {
		Pair<Integer, Pair<Long, Long>> job = getNextJob();
		while (job != null) {
			int threads = conf.getInteger("gamesman.threads", 1);
			assert Util.debug(DebugFacility.MASTER, "Launching " + threads
					+ " threads for " + job.car + "-" + job.cdr);
			List<WorkUnit> list = solver.prepareSolve(conf, job.car, job.cdr)
					.divide(threads);

			ArrayList<Thread> myThreads = new ArrayList<Thread>();

			ThreadGroup solverGroup = new ThreadGroup("Solver Group: "
					+ conf.getGame());
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
			if (closeDB) {
				database.close();
			} else {
				database.flush();
			}
			assert Util.debug(DebugFacility.MASTER, "Finished master run");
			job = getNextJob();
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

	private Pair<Integer, Pair<Long, Long>> getNextJob() {
		// TODO: Retrieve job from master computer
		return null;
	}
}
package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class TaskManager {
	private static final boolean ALLOWS_MULTIPLE = true;

	private final HashMap<Tier, CreateRunner> creationRunnerMap = new HashMap<Tier, CreateRunner>();
	private final HashMap<Tier, CombineRunner> combiningRunnerMap = new HashMap<Tier, CombineRunner>();
	private final HashMap<Tier, PropogateRunner> propogationRunnerMap = new HashMap<Tier, PropogateRunner>();
	private final HashSet<Tier> inUse = new HashSet<Tier>();

	private final HashSet<TaskRunner> waitingJobs = new HashSet<TaskRunner>();
	private final HashSet<TaskRunner> runningJobs = new HashSet<TaskRunner>();

	private boolean cleanedUp = false;

	private boolean eventOccurred = true;

	private final boolean singleLinear;

	public TaskManager(boolean singleLinear) {
		this.singleLinear = singleLinear;
	}

	public synchronized void add(TaskRunner runner) throws IOException {
		waitingJobs.add(runner);
		switch (runner.type) {
		case TaskRunner.CREATE:
			add((CreateRunner) runner);
			break;
		case TaskRunner.COMBINE:
			add((CombineRunner) runner);
			break;
		case TaskRunner.PROPOGATE:
			add((PropogateRunner) runner);
			break;
		case TaskRunner.CLEANUP:
			add((CleanupRunner) runner);
			break;
		default:
			throw new Error("runner type not recognized: " + runner.type);
		}
	}

	private synchronized void add(CreateRunner runner) throws IOException {
		creationRunnerMap.put(runner.tier, runner);
	}

	private synchronized void add(CombineRunner runner) throws IOException {
		combiningRunnerMap.put(runner.tier, runner);
	}

	private synchronized void add(PropogateRunner runner) throws IOException {
		propogationRunnerMap.put(runner.headTier, runner);
	}

	private void add(CleanupRunner runner) throws IOException {
		// Do nothing
	}

	private synchronized boolean run(TaskRunner runner) throws IOException {
		if (!canRun(runner)) {
			waitingJobs.add(runner);
			return false;
		}
		if (!needsToRun(runner))
			return false;
		runningJobs.add(runner);
		switch (runner.type) {
		case TaskRunner.CREATE:
			run((CreateRunner) runner);
			break;
		case TaskRunner.COMBINE:
			run((CombineRunner) runner);
			break;
		case TaskRunner.PROPOGATE:
			run((PropogateRunner) runner);
			break;
		case TaskRunner.CLEANUP:
			run((CleanupRunner) runner);
			break;
		default:
			throw new Error("runner type not recognized: " + runner.type);
		}
		new CheckerThread(runner).start();
		return true;
	}

	private synchronized void run(CreateRunner runner) throws IOException {
		boolean changed = inUse.add(runner.tier);
		assert changed;
	}

	private synchronized void run(CombineRunner runner) throws IOException {
		boolean changed = inUse.add(runner.tier);
		assert changed;
	}

	private synchronized void run(PropogateRunner runner) throws IOException {
		for (Tier t : runner.cycleSet) {
			boolean changed = inUse.add(t);
			assert changed;
		}
	}

	private void run(CleanupRunner runner) throws IOException {
		// Do nothing
	}

	private boolean needsToRun(TaskRunner runner) throws IOException {
		switch (runner.type) {
		case TaskRunner.CREATE:
			return needsToRun((CreateRunner) runner);
		case TaskRunner.COMBINE:
			return needsToRun((CombineRunner) runner);
		case TaskRunner.PROPOGATE:
			return needsToRun((PropogateRunner) runner);
		case TaskRunner.CLEANUP:
			return needsToRun((CleanupRunner) runner);
		default:
			throw new Error("runner type not recognized: " + runner.type);
		}
	}

	private boolean needsToRun(CreateRunner runner) throws IOException {
		return runner.tier.needsToCreate();
	}

	private boolean needsToRun(CombineRunner runner) throws IOException {
		return runner.tier.needsToCombine();
	}

	private boolean needsToRun(PropogateRunner runner) throws IOException {
		return runner.needsToPropogate();
	}

	private boolean needsToRun(CleanupRunner runner) {
		return !cleanedUp;
	}

	private synchronized void finished(TaskRunner runner) throws IOException {
		runningJobs.remove(runner);
		switch (runner.type) {
		case TaskRunner.CREATE:
			finished((CreateRunner) runner);
			break;
		case TaskRunner.COMBINE:
			finished((CombineRunner) runner);
			break;
		case TaskRunner.PROPOGATE:
			finished((PropogateRunner) runner);
			break;
		case TaskRunner.CLEANUP:
			finished((CleanupRunner) runner);
			break;
		default:
			throw new Error("runner type not recognized: " + runner.type);
		}
		event();
	}

	private synchronized void finished(CreateRunner runner) throws IOException {
		boolean changed = inUse.remove(runner.tier);
		assert changed;
	}

	private synchronized void finished(CombineRunner runner) throws IOException {
		boolean changed = inUse.remove(runner.tier);
		assert changed;
	}

	private synchronized void finished(PropogateRunner runner)
			throws IOException {
		for (Tier t : runner.cycleSet) {
			boolean changed = inUse.remove(t);
			assert changed;
		}
	}

	private synchronized void finished(CleanupRunner runner) throws IOException {
		cleanedUp = true;
	}

	private synchronized void event() {
		eventOccurred = true;
		notify();
	}

	private boolean canRun(TaskRunner runner) throws IOException {
		switch (runner.type) {
		case TaskRunner.CREATE:
			return canRun((CreateRunner) runner);
		case TaskRunner.COMBINE:
			return canRun((CombineRunner) runner);
		case TaskRunner.PROPOGATE:
			return canRun((PropogateRunner) runner);
		case TaskRunner.CLEANUP:
			return canRun((CleanupRunner) runner);
		default:
			throw new Error("runner type not recognized: " + runner.type);
		}
	}

	private boolean canRun(CreateRunner runner) throws IOException {
		return !runner.tier.needsToCombine() && canRun(runner.tier);
	}

	private boolean canRun(CombineRunner runner) throws IOException {
		return canRun(runner.tier);
	}

	private boolean canRun(PropogateRunner runner) throws IOException {
		if (inUse.contains(runner.headTier)) {
			return false;
		}
		Set<Tier> reversedependencies = runner.headTier.getReverseDependences();
		for (Tier depend : reversedependencies) {
			if (inUse.contains(depend) || depend.needsToPropogate()) {
				return false;
			}
		}
		return true;
	}

	private boolean canRun(CleanupRunner runner) throws IOException {
		return !running();
	}

	private boolean canRun(Tier tier) throws IOException {
		if (inUse.contains(tier)) {
			return false;
		}
		Set<Tier> dependencies = tier.getDependences();
		for (Tier depend : dependencies) {
			if (inUse.contains(depend) || depend.needsToCombine()
					|| depend.needsToCreate()) {
				return false;
			}
		}
		return true;
	}

	public synchronized boolean running() {
		return runningJobs.size() > 0;
	}

	public synchronized void nextEvent() throws InterruptedException {
		while (!eventOccurred) {
			wait();
		}
		eventOccurred = false;
	}

	private class CheckerThread extends Thread {
		private final TaskRunner runner;

		public CheckerThread(TaskRunner runner) {
			this.runner = runner;
		}

		@Override
		public void run() {
			try {
				runner.run();
			} finally {
				try {
					finished(runner);
				} catch (Throwable t) {
					t.printStackTrace();
					System.exit(-1);
				}
			}
		}
	}

	public synchronized boolean run() throws IOException {
		boolean result = running();
		LinkedList<TaskRunner> acceptedRunners = new LinkedList<TaskRunner>();
		for (TaskRunner runner : waitingJobs) {
			if (canRun(runner)) {
				acceptedRunners.add(runner);
			}
		}
		if (ALLOWS_MULTIPLE && !singleLinear) {
			for (TaskRunner runner : acceptedRunners) {
				waitingJobs.remove(runner);
				result |= run(runner);
			}
		} else {
			do {
				TaskRunner choice = null;
				int minTier = Integer.MAX_VALUE;
				if (singleLinear) {
					Iterator<TaskRunner> iter = acceptedRunners.iterator();
					while (iter.hasNext()) {
						TaskRunner runner = iter.next();
						if (runner instanceof CreateRunner) {
							if (!result) {
								int t = ((CreateRunner) runner).tier.num;
								if (t < minTier) {
									minTier = t;
									choice = runner;
								} else if (t == minTier)
									assert runner.equals(choice);
							} else
								assert choice == null;
						} else if (runner instanceof CombineRunner) {
							if (!result) {
								int t = ((CombineRunner) runner).tier.num;
								if (t < minTier) {
									minTier = t;
									choice = runner;
								} else if (t == minTier)
									assert runner.equals(choice);
							} else
								assert choice == null;
						} else if (ALLOWS_MULTIPLE) {
							assert choice == null;
							waitingJobs.remove(runner);
							iter.remove();
							result |= run(runner);
						} else if (!result && choice == null) {
							choice = runner;
						} else if (result)
							assert choice == null;
						else
							assert minTier == Integer.MAX_VALUE
									&& choice != null;
					}
				} else if (!result) {
					if (!acceptedRunners.isEmpty())
						choice = acceptedRunners.getFirst();
				}
				if (choice != null) {
					waitingJobs.remove(choice);
					acceptedRunners.remove(choice);
					result |= run(choice);
				}
			} while (!result && !acceptedRunners.isEmpty());
		}
		if (!result)
			assert waitingJobs.isEmpty();
		assert result == running();
		return result;
	}

	public synchronized boolean hasNone() {
		return waitingJobs.isEmpty() && runningJobs.isEmpty();
	}
}

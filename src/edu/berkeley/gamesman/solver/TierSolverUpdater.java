package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.util.Task;

public final class TierSolverUpdater {
	private long total = 0;
	private final long totalProgress;

	protected final Task t;

	TierSolverUpdater(Configuration conf) {
		this(conf, conf.getGame().numHashes());
	}

	public TierSolverUpdater(Configuration conf, long totalProgress) {
		this.totalProgress = totalProgress;
		TierGame myGame = (TierGame) conf.getGame();
		t = Task.beginTask("Tier solving \"" + myGame.describe() + "\"");
		t.setTotal(totalProgress);
	}

	protected synchronized void calculated(int howMuch) {
		total += howMuch;
		if (t != null) {
			t.setProgress(total);
		}
	}

	public synchronized void complete() {
		if (t != null) {
			t.setProgress(totalProgress);
			t.complete();
		}
	}
}

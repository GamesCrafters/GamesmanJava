package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.util.Task;

public class TierSolverUpdater {
	private long total = 0;

	protected Task t;

	TierSolverUpdater(Configuration conf) {
		this(conf, conf.getGame().numHashes());
	}

	public TierSolverUpdater(Configuration conf, long totalProgress) {
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

	public void complete() {
		if (t != null)
			t.complete();
		t = null;
	}
}

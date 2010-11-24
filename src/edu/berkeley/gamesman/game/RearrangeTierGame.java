package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;

public abstract class RearrangeTierGame extends TierGame {
	public RearrangeTierGame(Configuration conf) {
		super(conf);
	}

	public abstract boolean majorChanged();
}

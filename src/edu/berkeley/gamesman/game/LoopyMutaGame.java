package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;

public abstract class LoopyMutaGame extends TopDownMutaGame {

	public LoopyMutaGame(Configuration conf) {
		super(conf);
	}

	public abstract int unmakeMove();

	public abstract boolean changeUnmakeMove();

	public abstract void remakeMove();
}

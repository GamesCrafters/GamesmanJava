package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.State;

public abstract class LoopyMutaGame<S extends State> extends TopDownMutaGame {

	public LoopyMutaGame(Configuration conf) {
		super(conf);
	}

	public abstract int unmakeMove();

	public abstract boolean changeUnmakeMove();

	public abstract void remakeMove();
	
	public abstract int possibleParents(S pos, S[] children);
}

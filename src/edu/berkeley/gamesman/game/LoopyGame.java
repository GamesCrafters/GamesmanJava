package edu.berkeley.gamesman.game;

public interface LoopyGame<S> {
	public int possibleParents(S pos, S[] children);
}

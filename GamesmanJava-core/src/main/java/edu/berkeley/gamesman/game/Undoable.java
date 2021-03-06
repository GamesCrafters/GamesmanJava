package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.State;

public interface Undoable<S extends State<S>> {
	public int possibleParents(S pos, S[] parents);

	public int maxParents();
}

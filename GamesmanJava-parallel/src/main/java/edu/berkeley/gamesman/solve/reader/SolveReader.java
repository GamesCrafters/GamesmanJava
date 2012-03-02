package edu.berkeley.gamesman.solve.reader;

import java.util.Collection;

import edu.berkeley.gamesman.util.Pair;

/**
 * This is for the web interface to talk to the game. These methods are not
 * called by the solver
 * 
 * @author dnspies
 * 
 * @param <KEY>
 */
public interface SolveReader<KEY> {
	/**
	 * Unhashes a string to a position.
	 * 
	 * @param board
	 *            The string to unhash
	 * @return The resulting position
	 */
	public abstract KEY getPosition(String board);

	/**
	 * This returns a collection of the children of a given position together
	 * with the string naming the move that leads to that position.
	 * 
	 * @param position
	 * @return
	 */
	public abstract Collection<Pair<String, KEY>> getChildren(KEY position);

	/**
	 * Hashes a position to a string
	 * 
	 * @param position
	 *            The position to hash
	 * @return The resulting string
	 */
	public abstract String getString(KEY position);
}

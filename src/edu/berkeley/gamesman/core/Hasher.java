package edu.berkeley.gamesman.core;

import edu.berkeley.gamesman.game.util.AlignmentState;

/**
 * A Hasher converts between a game board and a long hash in a space- and
 * time-efficient manner.
 * 
 * @author Steven Schlansker
 * @param <S>
 *            The type of Board that this hasher can hash
 */
public abstract class Hasher<S extends State> {
	protected Configuration conf;

	/**
	 * Initialize this Hasher
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public void initialize(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * Convert a previously hashed board back into its canonical char-array
	 * representation
	 * 
	 * @param hash
	 *            The hashed representation of a board
	 * @return The board
	 */
	public abstract S unhash(long hash);

	/**
	 * Convert a board into a compact hash representation
	 * 
	 * @param state
	 *            The board to hash
	 * @return Hash of the board
	 */
	public abstract long hash(S state);

	/**
	 * @return Maximum hash that the Hasher could return via a call to hash()
	 * @see Hasher#hash(State)
	 */
	public abstract long numHashes();

	/**
	 * @return a String that uniquely identifies this Hasher (including valid
	 *         pieces if appropriate, etc)
	 */
	public abstract String describe();

	public void unhash(long hash, AlignmentState state) {
		state.set(unhash(hash));
	}

}

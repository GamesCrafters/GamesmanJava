package edu.berkeley.gamesman.core;

import java.math.BigInteger;

import edu.berkeley.gamesman.util.Util;


/**
 * A Hasher converts between a game board and a BigInteger hash in a space- and time-efficient manner.
 * @author Steven Schlansker
 * @param <Board> The type of Board that this hasher can hash
 */
public abstract class Hasher<Board> {
	protected Game<Board> game;
	protected Configuration conf;
	
	/**
	 * Default constructor
	 * @param conf the configuration object
	 */
	public Hasher(Configuration conf){
		game = Util.checkedCast(conf.getGame());
		this.conf = conf;
	}
	
	/**
	 * Convert a previously hashed board back into its canonical char-array representation
	 * @param hash The hashed representation of a board
	 * @return The board
	 */
	public abstract Board unhash(BigInteger hash);

	/**
	 * Convert a board into a compact hash representation
	 * @param board The board to hash
	 * @param l The size of the Board
	 * @return Hash of the board
	 */
	public abstract BigInteger hash(Board board);
	
	/**
	 * @return Maximum hash that the Hasher could return via a call to hash()
	 * @see Hasher#hash(Object, int)
	 */
	public abstract BigInteger maxHash();
	
	/**
	 * @return a String that uniquely identifies this Hasher (including valid pieces if appropriate, etc)
	 */
	public abstract String describe();
	
}

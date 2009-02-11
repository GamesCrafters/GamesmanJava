package edu.berkeley.gamesman.core;

import java.io.Serializable;
import java.math.BigInteger;

import edu.berkeley.gamesman.util.Util;


/**
 * A Hasher converts between a game board and a BigInteger hash in a space- and time-efficient manner.
 * @author Steven Schlansker
 * @param <Board> The type of Board that this hasher can hash
 */
public abstract class Hasher<Board> implements Serializable {
	private static final long serialVersionUID = -6537920117712373326L;
	protected Game<Board> game;
	protected Configuration conf;
	protected final char[] pieces;
	
	/**
	 * Default constructor
	 * @param conf the configuration object
	 */
	public Hasher(Configuration conf){
		game = Util.checkedCast(conf.getGame());
		this.conf = conf;
		pieces = game.pieces();
	}
	
	/**
	 * Create a Hasher with a specified piece set
	 * @param conf the configuration
	 * @param pieces the pieces
	 */
	public Hasher(final Configuration conf,final char[] pieces){
		this.pieces = pieces;
		this.conf = conf;
		game = Util.checkedCast(conf.getGame());
	}
	
	/**
	 * Convert a previously hashed board back into its canonical char-array representation
	 * @param hash The hashed representation of a board
	 * @return The board
	 */
	public Board unhash(BigInteger hash){
		return unhash(hash,game.getGameWidth()*game.getGameHeight());
	}
	/**
	 * Convert a hashed board given its length
	 * @param hash The hashed board
	 * @param l Size of the board
	 * @return The board
	 */
	public abstract Board unhash(BigInteger hash, int l);
	/**
	 * Convert a board into a compact hash representation
	 * @param board The board to hash
	 * @param l The size of the Board
	 * @return Hash of the board
	 */
	public abstract BigInteger hash(Board board, int l);
	
	/**
	 * @param boardlen Size of the board
	 * @return Maximum hash that the Hasher could return via a call to hash()
	 * @see Hasher#hash(Object, int)
	 */
	public abstract BigInteger maxHash(int boardlen);
	
	/**
	 * @return a String that uniquely identifies this Hasher (including valid pieces if appropriate, etc)
	 */
	public abstract String describe();
	
}

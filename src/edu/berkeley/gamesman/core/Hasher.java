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
	protected final char[] pieces;
	protected final int gameWidth, gameHeight;
	
	/**
	 * Default constructor
	 * @param conf the configuration object
	 */
	public Hasher(Configuration conf){
		game = Util.checkedCast(conf.getGame());
		if(!(game instanceof BoardGame2D))
			Util.fatalError("This hasher only works for a BoardGame2D!");
		BoardGame2D boardGame = (BoardGame2D) game;
		gameWidth = boardGame.getGameWidth();
		gameHeight = boardGame.getGameHeight();
		pieces = boardGame.getPieces();
		this.conf = conf;
	}
	
	/**
	 * Convert a previously hashed board back into its canonical char-array representation
	 * @param hash The hashed representation of a board
	 * @return The board
	 */
	public Board unhash(BigInteger hash){
		return unhash(hash,gameWidth*gameHeight);
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

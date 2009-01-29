package edu.berkeley.gamesman.core;

import java.math.BigInteger;


/**
 * A Hasher converts between a game board and a BigInteger hash in a space- and time-efficient manner.
 * @author Steven Schlansker
 * @param <Board> The type of Board that this hasher can hash
 */
public abstract class Hasher<Board> {

	protected Game<Board> game;
	protected char[] pieces;
	
	/**
	 * Give the hasher a reference to the game it's hashing for
	 * @param g The game we're playing
	 * @param p The valid pieces for this hasher
	 */
	public void setGame(Game<Board> g, char[] p){
		game = g;
		pieces = p;
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

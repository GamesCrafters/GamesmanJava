package edu.berkeley.gamesman.core;

import java.math.BigInteger;


/**
 * A Hasher converts between a game board and a BigInteger hash in a space- and time-efficient manner.
 * @author Steven Schlansker
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
	public abstract Board unhash(BigInteger hash, int l);
	/**
	 * Convert a board into a compact hash representation
	 * @param board The board to hash
	 * @return Hash of the board
	 */
	public abstract BigInteger hash(Board board, int l);
	
	public abstract BigInteger maxHash(int boardlen);
	
	public abstract String describe();
	
}

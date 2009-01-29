package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.game.Game;

/**
 * A Hasher converts between a game board and a BigInteger hash in a space- and time-efficient manner.
 * This hasher is restricted to 1 or 2-d game boards for now - this limitation should be lifted in the future
 * @author Steven Schlansker
 */
public abstract class Hasher {

	protected Game<char[][], ?> game;
	
	/**
	 * Give the hasher a reference to the game it's hashing for
	 * @param g The game we're playing
	 */
	public void setGame(Game<char[][], ?> g){
		game = g;
	}
	
	/**
	 * Convert a previously hashed board back into its canonical char-array representation
	 * @param hash The hashed representation of a board
	 * @return The board
	 */
	public abstract char[][] unhash(BigInteger hash);
	/**
	 * Convert a board into a compact hash representation
	 * @param board The board to hash
	 * @return Hash of the board
	 */
	public abstract BigInteger hash(char[][] board);
	
}

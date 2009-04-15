package edu.berkeley.gamesman.core;

/**
 * @author Jeremy Fleischman
 *
 */
public abstract class Board1D {
	/**
	 * The key in configuration files used to set the game width
	 */
	public static final String WIDTH_KEY = "gamesman.game.width";
	/**
	 * @return the board width
	 */
	public abstract int getBoardWidth();
	/**
	 * @return the pieces used
	 */
	public abstract char[] getPieces();
	
	/**
	 * @return the board represented
	 */
	public abstract char[] getBoardChars();
}

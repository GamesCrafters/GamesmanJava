package edu.berkeley.gamesman.core;

/**
 * @author Jeremy Fleischman
 *
 */
public abstract class Board1D {
	public static final String WIDTH_KEY = "gamesman.game.width";
	/**
	 * @return
	 */
	public abstract int getBoardWidth();
	/**
	 * @return
	 */
	public abstract char[] getPieces();
	
	public abstract char[] getBoardChars();
}

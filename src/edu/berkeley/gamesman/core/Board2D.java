package edu.berkeley.gamesman.core;

/**
 * @author Jeremy Fleischman
 *
 */
public abstract class Board2D {
	public static final String WIDTH_KEY = "gamesman.game.width";
	public static final String HEIGHT_KEY = "gamesman.game.height";
	/**
	 * @return
	 */
	public abstract int getBoardHeight();
	/**
	 * @return
	 */
	public abstract int getBoardWidth();
	/**
	 * @return
	 */
	public abstract char[] getPieces();
}

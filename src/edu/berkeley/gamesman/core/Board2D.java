package edu.berkeley.gamesman.core;

/**
 * @author Jeremy Fleischman
 */
public abstract class Board2D {
	/**
	 * The key used to retrieve game width from configurations
	 */
	public static final String WIDTH_KEY = "gamesman.game.width";
	/**
	 * The key used to retrieve game height from configurations
	 */
	public static final String HEIGHT_KEY = "gamesman.game.height";
	/**
	 * @return the board height
	 */
	public abstract int getBoardHeight();
	/**
	 * @return the board width
	 */
	public abstract int getBoardWidth();
	/**
	 * @return the pieces used
	 */
	public abstract char[] getPieces();
}

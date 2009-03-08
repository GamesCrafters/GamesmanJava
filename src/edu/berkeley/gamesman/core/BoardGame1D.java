package edu.berkeley.gamesman.core;

/**
 * @author Jeremy Fleischman
 *
 */
public interface BoardGame1D {
	public static final String WIDTH_KEY = "gamesman.game.width";
	/**
	 * @return
	 */
	public abstract int getGameWidth();
	/**
	 * @return
	 */
	public abstract char[] getPieces();
}

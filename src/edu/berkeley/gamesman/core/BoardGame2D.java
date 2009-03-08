package edu.berkeley.gamesman.core;

/**
 * @author Jeremy Fleischman
 *
 */
public interface BoardGame2D {
	public static final String WIDTH_KEY = "gamesman.game.width";
	public static final String HEIGHT_KEY = "gamesman.game.height";
	/**
	 * @return
	 */
	public abstract int getGameHeight();
	/**
	 * @return
	 */
	public abstract int getGameWidth();
	/**
	 * @return
	 */
	public abstract char[] getPieces();
}

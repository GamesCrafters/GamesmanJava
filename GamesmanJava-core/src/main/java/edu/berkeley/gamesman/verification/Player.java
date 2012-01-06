package edu.berkeley.gamesman.verification;

/**
 * 
 * @author adegtiar
 * @author rchengyue
 */
public interface Player {

	/**
	 * Swaps the player, returning the opposite one (X if called on O and vice
	 * versa).
	 * 
	 * @return the opposite player.
	 */
	public Player getOppositePlayer();
}

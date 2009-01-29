package edu.berkeley.gamesman.game;

/**
 * 
 * Standard position values
 * @author Steven Schlansker
 *
 */
public enum Values {

	/**
	 * Value hasn't been computed yet
	 */
	Unseen,
	/**
	 * Position has no value
	 */
	Undecided,
	/**
	 * Position is not valid
	 * N.B. This is generally an indicator of a suboptimal hasher
	 */
	Invalid,
	/**
	 * Position is a win
	 */
	Win,
	/**
	 * Position is a lose
	 */
	Lose,
	/**
	 * Position is a tie
	 */
	Tie
	
}

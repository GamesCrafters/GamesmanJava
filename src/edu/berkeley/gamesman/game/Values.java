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
	Unseen(0),
	/**
	 * Position has no value
	 */
	Undecided(0),
	/**
	 * Position is not valid
	 * N.B. This is generally an indicator of a suboptimal hasher
	 */
	Invalid(0),
	/**
	 * Position is a win
	 */
	Win(1),
	/**
	 * Position is a lose
	 */
	Lose(2),
	/**
	 * Position is a tie
	 */
	Tie(3);

	private int val;
	private Values(int val) { this.val = val; }
	public int value() { return val; }
	
	public int maxValue() { int max = 0; for(Values v : this.values()) max = Math.max(v.value(),max); return max; }
	
}

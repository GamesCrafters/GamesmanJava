package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.database.DBValue;


/**
 * 
 * Standard position values
 * @author Steven Schlansker
 *
 */
public enum Values implements DBValue {

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

	private byte val;
	private Values(int val) { this.val = (byte)val; }
	//public int value() { return val; }
	
	public int maxValue() { int max = 0; for(Values v : this.values()) max = Math.max(v.byteValue(),max); return max; }
	public byte byteValue() {
		return val;
	}
	public DBValue wrapValue(byte b) {
		switch(b){
		case 0:
			return Undecided;
		case 1:
			return Win;
		case 2:
			return Lose;
		case 3:
			return Tie;
		}
		
		return Invalid;
	}
	
}

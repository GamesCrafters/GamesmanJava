package edu.berkeley.gamesman.core;

import java.util.Collection;

import edu.berkeley.gamesman.database.DBValue;
import edu.berkeley.gamesman.util.Util;


/**
 * 
 * Standard position values.  All relative to the current player.
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
	 * Stored the same as undecided in the database for compression
	 */
	Invalid(0),
	/**
	 * Position is a win (The /next player to move/ has won the game)
	 * Equiv: the /player who just had his turn on the previous board/ has lost
	 */
	Win(1),
	/**
	 * Position is a lose (The /next player to move/ has lost the game)
	 * Equiv: the /player who just had his turn on the previous board/ has won
	 */
	Lose(2),
	/**
	 * Position is a tie (Neither player can win from this position if both players play optimally)
	 */
	Tie(3);

	private byte val;
	private Values(int val) { this.val = (byte)val; }
	//public int value() { return val; }
	
	public int maxValue() { int max = 0; for(Values v : Values.values()) max = Math.max(v.byteValue(),max); return max; }
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

	public DBValue fold(Collection<DBValue> vals) {
		boolean seentie = false;
		
		for(DBValue v : vals){
			Values va = (Values)v;
			switch(va){
			case Invalid:
				Util.fatalError("Trying to fold over an invalid value");
				return Invalid;
			case Undecided:
				Util.fatalError("Trying to fold over an undecided value");
				return Invalid;
			case Unseen:
				Util.fatalError("Trying to fold over an unseen value");
				return Invalid;
			case Lose:
				return Win;
			case Tie:
				seentie = true;
				break;
			case Win:
				break;
			}
		}
		if(seentie) return Tie;
		return Lose;
	}
	
}

package edu.berkeley.gamesman.core;

/**
 * A PrimitiveValue represents the value of a state in the Gamesman world
 * @author Steven Schlansker
 */
public enum PrimitiveValue {
	/**
	 * This value hasn't been computed yet
	 */
	Undecided(0),
	/**
	 * The next player to move will win in perfect play
	 */
	Win(1),
	/**
	 * The next player to move will lose even playing perfectly
	 */
	Lose(2),
	/**
	 * The game will eventually end in a tie
	 */
	Tie(3);
	
	int value;
	PrimitiveValue(int v){
		value = v;
	}
}

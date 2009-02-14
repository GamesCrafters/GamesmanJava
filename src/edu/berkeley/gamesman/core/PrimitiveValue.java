package edu.berkeley.gamesman.core;

/**
 * A PrimitiveValue represents the value of a state in the Gamesman world
 * @author Steven Schlansker
 */
public enum PrimitiveValue {
	/**
	 * This value hasn't been computed yet
	 */
	Undecided(3),
	/**
	 * The next player to move will win in perfect play
	 */
	Win(2),
	/**
	 * The next player to move will lose even playing perfectly
	 */
	Lose(0),
	/**
	 * The game will eventually end in a tie
	 */
	Tie(1);
	
	private int value;
	PrimitiveValue(int v){
		value = v;
	}
	
	/**
	 * @return the numeric value of this primitive
	 */
	public int value(){
		return value;
	}
	
	public String toString(){
		return this.name();
	}
	
	/**
	 * @param otherValue The value to compare this one with
	 * @return Is true only if this PrimitiveValue is better than otherValue.
	 * Order of values from worst to best: Lose, Tie, Win, Undecided 
	 */
	public boolean isPreferableTo(PrimitiveValue otherValue){
		return (value > otherValue.value);
	}
}

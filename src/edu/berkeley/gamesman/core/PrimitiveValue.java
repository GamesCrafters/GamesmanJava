package edu.berkeley.gamesman.core;

/**
 * A PrimitiveValue represents the value of a state in the Gamesman world
 * 
 * @author Steven Schlansker
 */
public enum PrimitiveValue {
	// These must be in order so that PrimitiveValue.values()[x].value() == x
	/**
	 * The next player to move will lose even playing perfectly
	 */
	LOSE(0),
	/**
	 * The game will eventually end in a tie
	 */
	TIE(1),
	/**
	 * The next player to move will win in perfect play
	 */
	WIN(2),
	/**
	 * The game is not over yet
	 */
	UNDECIDED(3),
	/**
	 * This is not a legal position
	 */
	IMPOSSIBLE(4);

	public final int value;

	/**
	 * The same as PrimitiveValue.values(), but without needing to allocate new
	 * space every time it's called
	 */
	public final static PrimitiveValue[] values = PrimitiveValue.values();

	PrimitiveValue(int v) {
		value = v;
	}

	public String toString() {
		return this.name();
	}

	/**
	 * @param otherValue
	 *            The value to compare this one with
	 * @return Is true only if this PrimitiveValue is better than otherValue.
	 *         Order of values from worst to best: Lose, Tie, Win, Undecided
	 */
	public boolean isPreferableTo(PrimitiveValue otherValue) {
		return (value > otherValue.value);
	}

	/**
	 * @return The value of this move from the perspective of the other player.
	 */
	public PrimitiveValue flipValue() {
		switch (this) {
		case LOSE:
			return WIN;
		case WIN:
			return LOSE;
		default:
			return this;
		}
	}
}

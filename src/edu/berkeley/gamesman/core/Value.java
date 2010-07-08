package edu.berkeley.gamesman.core;

/**
 * A PrimitiveValue represents the value of a state in the Gamesman world
 * 
 * @author dnspies
 */
public enum Value {
	/**
	 * The player who just moved can force a win
	 */
	LOSE(0),
	/**
	 * Neither player can force a win nor end the game
	 */
	DRAW(1),
	/**
	 * Neither player can force a win, but it is possible to end the game
	 */
	TIE(2),
	/**
	 * A placeholder for solvers to use when they haven't yet calculated the
	 * children of the position. Additionally, if a game is not completely
	 * solved, this may be used to indicate that the value of the position is
	 * unknown.
	 */
	UNDECIDED(3),
	/**
	 * The player who's turn it is can force a win
	 */
	WIN(4),
	/**
	 * This is not a legal position
	 */
	IMPOSSIBLE(5);

	/**
	 * The numeric value of this primitive value (same as ordinal())
	 */
	public final int value;

	/**
	 * The same as PrimitiveValue.values(), but without needing to allocate new
	 * space every time it's called
	 */
	public final static Value[] values = Value.values();

	private Value(int v) {
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
	public boolean isPreferableTo(Value otherValue) {
		return (value > otherValue.value);
	}

	/**
	 * @return The value of this move from the perspective of the other player.
	 */
	public Value flipValue() {
		if (this == LOSE)
			return WIN;
		else if (this == WIN)
			return LOSE;
		else
			return this;
	}
}

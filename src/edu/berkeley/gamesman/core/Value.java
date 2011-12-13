package edu.berkeley.gamesman.core;

/**
 * A PrimitiveValue represents the value of a state in the Gamesman world
 * 
 * @author dnspies
 */
public enum Value implements Comparable<Value> {
	/**
	 * The player who just moved can force a win
	 */
	LOSE(true),
	/**
	 * Neither player can force a win nor end the game
	 */
	DRAW(false),
	/**
	 * Neither player can force a win, but it is possible to end the game
	 */
	TIE(true),
	/**
	 * A placeholder for solvers to use when they haven't yet calculated the
	 * children of the position. Additionally, if a game is not completely
	 * solved, this may be used to indicate that the value of the position is
	 * unknown.
	 */
	UNDECIDED(false),
	/**
	 * The player who's turn it is can force a win
	 */
	WIN(true),
	/**
	 * This is not a legal position
	 */
	IMPOSSIBLE(false);

	/**
	 * Does it make sense to include remoteness with this value?
	 */
	public final boolean hasRemoteness;
	private Value opposite;
	static {
		LOSE.opposite = WIN;
		DRAW.opposite = DRAW;
		TIE.opposite = TIE;
		UNDECIDED.opposite = UNDECIDED;
		WIN.opposite = LOSE;
		IMPOSSIBLE.opposite = IMPOSSIBLE;
	}
	/**
	 * The same as PrimitiveValue.values(), but without needing to allocate new
	 * space every time it's called
	 */
	private final static Value[] values = Value.values();

	private Value(boolean hasRemoteness) {
		this.hasRemoteness = hasRemoteness;
	}

	public String toString() {
		return this.name();
	}

	public static Value getValue(int vNum) {
		return values[vNum];
	}

	public Value opposite() {
		return opposite;
	}
}

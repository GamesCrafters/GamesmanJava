package edu.berkeley.gamesman.game;

/**
 * @author DNSpies Represents a piece in Connect4
 */
public enum C4Piece {
	/**
	 * Unfilled board slot
	 */
	EMPTY,
	/**
	 * Black Piece
	 */
	BLACK,
	/**
	 * Red Piece
	 */
	RED;

	/**
	 * @return The opposite color piece (or Empty for Empty)
	 */
	public C4Piece opposite() {
		if (this == BLACK)
			return RED;
		else if (this == RED)
			return BLACK;
		else
			return this;
	}

	public String toString() {
		if (this == RED)
			return "X";
		else if (this == BLACK)
			return "O";
		else if (this == EMPTY)
			return " ";
		else{
			new Exception("Bad piece").printStackTrace();
			return null;
		}
	}
}

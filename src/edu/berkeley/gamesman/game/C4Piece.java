package edu.berkeley.gamesman.game;

/**
 * @author DNSpies Represents a piece in Connect4
 */
public enum C4Piece{
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
		return String.valueOf(toChar());
	}

	/**
	 * @return The char representation of this piece 'X' for Red, 'O' for Black,
	 *         and ' ' for Empty
	 */
	public char toChar() {
		switch (this) {
		case RED:
			return 'X';
		case BLACK:
			return 'O';
		case EMPTY:
			return ' ';
		default:
			new Exception("Bad piece").printStackTrace();
			return 0;
		}
	}
	
	/**
	 * @param c The character to be converted
	 * @return The piece represented by that character
	 */
	public static C4Piece toPiece(char c){
		switch(c){
		case 'X':
			return RED;
		case 'O':
			return BLACK;
		case ' ':
			return EMPTY;
		default:
			new Exception("Bad piece").printStackTrace();
			return null;
		}
	}
}

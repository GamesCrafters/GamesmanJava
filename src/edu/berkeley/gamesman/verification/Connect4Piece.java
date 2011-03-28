package edu.berkeley.gamesman.verification;

/**
 * Represents a <tt>Piece</tt> on the Connect 4 board (X for red, O for black,
 * and BLANK for no piece).
 * 
 * @author adegtiar
 * @author rchengyue
 */
public enum Connect4Piece implements Piece {
	X, O, BLANK;

	public static Connect4Piece fromChar(char piece) {
		switch (piece) {
		case ' ':
			return Connect4Piece.BLANK;
		case 'X':
			return Connect4Piece.X;
		case 'O':
			return Connect4Piece.O;
		default:
			throw new IllegalArgumentException("Unsupported piece character: "
					+ piece);
		}
	}

	public char toChar() {
		switch (this) {
		case X:
			return 'X';
		case O:
			return 'O';
		case BLANK:
			return ' ';
		default:
			throw new IllegalStateException(
					"Could not find char representation from unknown Piece: "
							+ this);
		}
	}

	@Override
	public String toString() {
		return String.valueOf(toChar());
	}
}

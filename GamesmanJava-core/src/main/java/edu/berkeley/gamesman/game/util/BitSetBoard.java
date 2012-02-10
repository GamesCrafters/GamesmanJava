package edu.berkeley.gamesman.game.util;

/**
 * @author dnspies
 */
public class BitSetBoard {

	private long xPlayer = 0L, oPlayer = 0L;

	private final int height, width;

	/**
	 * @param gameHeight
	 *            The height of the board
	 * @param gameWidth
	 *            The width of the board
	 */
	public BitSetBoard(int gameHeight, int gameWidth) {
		height = gameHeight;
		width = gameWidth;
		if ((height + 1) * width > 64)
			throw new IllegalArgumentException(
					"Only works when (height+1)*width<=64");
	}

	private int getBit(int row, int col) {
		return col * (height + 1) + row;
	}

	/**
	 * Changes the color of the specified piece
	 * 
	 * @param row
	 *            The row of the piece
	 * @param col
	 *            The column of the piece
	 */
	public void flipPiece(int row, int col) {
		int bit = getBit(row, col);
		flipPiece(bit);
	}

	public void flipPiece(int bit) {
		assert oneHas(bit);
		long xorWith = 1L << bit;
		xPlayer ^= xorWith;
		oPlayer ^= xorWith;
	}

	private boolean oneHas(int bit) {
		return ((xPlayer ^ oPlayer) & (1L << bit)) != 0;
	}

	/**
	 * Removes specified piece from this position on the board
	 * 
	 * @param row
	 *            The row of the piece
	 * @param col
	 *            The column of the piece
	 */
	public void removePiece(int row, int col) {
		int bit = getBit(row, col);
		removePiece(bit);
	}

	public void removePiece(int bit) {
		assert oneHas(bit);
		long andWith = ~(1L << bit);
		xPlayer &= andWith;
		oPlayer &= andWith;
	}

	/**
	 * Adds specified piece to this position on the board
	 * 
	 * @param row
	 *            The row to put it in
	 * @param col
	 *            The column to put it in
	 * @param color
	 *            The color of the piece
	 */
	public void addPiece(int row, int col, char color) {
		int bit = getBit(row, col);
		addPiece(bit, color);
	}

	private boolean noneHas(int bit) {
		return ((xPlayer | oPlayer) & (1L << bit)) == 0;
	}

	public void addPiece(int bit, char color) throws Error {
		assert noneHas(bit);
		long shamt = 1L << bit;
		switch (color) {
		case 'X':
			xPlayer |= shamt;
			break;
		case 'O':
			oPlayer |= shamt;
			break;
		default:
			throw new Error("Bad piece");
		}
	}

	/**
	 * Switches X with O
	 */
	public void switchColors() {
		long tmp = xPlayer;
		xPlayer = oPlayer;
		oPlayer = tmp;
	}

	/**
	 * @param x
	 *            Number of pieces
	 * @param color
	 *            Color of pieces
	 * @return Whether there are x pieces of color color in a straight line on
	 *         the board. 0 for false, 1 for true.
	 */
	public boolean xInALine(int x, char color) {
		long board = (color == 'X' ? xPlayer : oPlayer);
		return checkDirection(x, 1, board) || checkDirection(x, height, board)
				|| checkDirection(x, height + 1, board)
				|| checkDirection(x, height + 2, board);
	}

	private boolean checkDirection(int x, int direction, long board) {
		int dist = direction * x;
		int checked = direction;
		while (checked << 1 < dist) {
			board &= board >> checked;
			checked <<= 1;
		}
		int lastCheck = dist - checked;
		board &= board >> lastCheck;
		return board != 0;
	}

	/**
	 * Clears the board
	 */
	public void clear() {
		xPlayer = 0L;
		oPlayer = 0L;
	}

	public String toString() {
		StringBuilder str = new StringBuilder(width * 2 + 1);
		for (int row = height - 1; row >= 0; row--) {
			str.append('|');
			for (int col = 0; col < width; col++) {
				if ((xPlayer & (1L << getBit(row, col))) > 0L)
					str.append('X');
				else if ((oPlayer & (1L << getBit(row, col))) > 0L)
					str.append('O');
				else
					str.append(' ');
				str.append('|');
			}
			str.append('\n');
		}
		return str.toString();
	}

	public void setPiece(int bit, char c) throws Error {
		long orWith = 1L << bit;
		long andWith = ~orWith;
		switch (c) {
		case ' ':
			xPlayer &= andWith;
			oPlayer &= andWith;
			break;
		case 'X':
			oPlayer &= andWith;
			xPlayer |= orWith;
			break;
		case 'O':
			xPlayer &= andWith;
			oPlayer |= orWith;
			break;
		default:
			throw new Error("Bad piece");
		}
	}
}
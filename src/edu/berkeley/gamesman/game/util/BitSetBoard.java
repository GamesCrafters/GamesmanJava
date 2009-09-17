package edu.berkeley.gamesman.game.util;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Board2D;

/**
 * @author dnspies
 */
public final class BitSetBoard extends Board2D {
	private BigInteger xPlayer = BigInteger.ZERO, oPlayer = BigInteger.ZERO;

	private long xPlayerLong = 0, oPlayerLong = 0;

	private int height, width;

	private boolean usesLong;

	/**
	 * @param gameHeight
	 *            The height of the board
	 * @param gameWidth
	 *            The width of the board
	 */
	public BitSetBoard(int gameHeight, int gameWidth) {
		height = gameHeight;
		width = gameWidth;
		usesLong = (width + 1) * height <= 64;
	}

	@Override
	public int getBoardHeight() {
		return height;
	}

	@Override
	public int getBoardWidth() {
		return width;
	}

	@Override
	public char[] getPieces() {
		return new char[] { 'X', 'O' };
	}

	private int getBit(int row, int col) {
		return row * (width + 1) + col;
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
		if (usesLong) {
			xPlayerLong = xPlayerLong ^ (1L << bit);
			oPlayerLong = oPlayerLong ^ (1L << bit);
		} else {
			xPlayer = xPlayer.flipBit(bit);
			oPlayer = oPlayer.flipBit(bit);
		}
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
		if (usesLong) {
			xPlayerLong = xPlayerLong & (-1L ^ (1L << bit));
			oPlayerLong = oPlayerLong & (-1L ^ (1L << bit));
		} else {
			xPlayer = xPlayer.clearBit(bit);
			oPlayer = oPlayer.clearBit(bit);
		}
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
		switch (color) {
		case 'X':
			if (usesLong)
				xPlayerLong |= (1L << getBit(row, col));
			else
				xPlayer = xPlayer.setBit(getBit(row, col));
			break;
		case 'O':
			if (usesLong)
				oPlayerLong |= (1L << getBit(row, col));
			else
				oPlayer = oPlayer.setBit(getBit(row, col));
			break;
		}
	}

	/**
	 * Switches X with O
	 */
	public void switchColors() {
		if (usesLong) {
			long tmp = xPlayerLong;
			xPlayerLong = oPlayerLong;
			oPlayerLong = tmp;
		} else {
			BigInteger tmp = xPlayer;
			xPlayer = oPlayer;
			oPlayer = tmp;
		}
	}

	/**
	 * @param x
	 *            Number of pieces
	 * @param color
	 *            Color of pieces
	 * @return Whether there are x pieces of color color in a straight line on
	 *         the board.
	 */
	public boolean xInALine(int x, char color) {
		if (usesLong) {
			long board = (color == 'X' ? xPlayerLong : oPlayerLong);
			return checkDirection(x, 1, board)
					|| checkDirection(x, width, board)
					|| checkDirection(x, width + 1, board)
					|| checkDirection(x, width + 2, board);
		} else {
			BigInteger board = (color == 'X' ? xPlayer : oPlayer);
			return checkDirection(x, 1, board)
					|| checkDirection(x, width, board)
					|| checkDirection(x, width + 1, board)
					|| checkDirection(x, width + 2, board);
		}
	}

	private boolean checkDirection(int x, int direction, long board) {
		int dist = direction * x;
		int checked = direction;
		while (checked << 1 < dist) {
			board = board & (board >> checked);
			checked <<= 1;
		}
		int lastCheck = dist - checked;
		board = board & (board >> lastCheck);
		return board != 0;
	}

	/*
	 * A rather complicated mathematical function. Runs in log time with respect
	 * to x to see if there are x 1's anywhere in the number evenly spaced at
	 * intervals of length direction.
	 */
	private static boolean checkDirection(int x, int direction, BigInteger board) {
		int dist = direction * x;
		int checked = direction;
		while (checked << 1 < dist) {
			board = board.and(board.shiftRight(checked));
			checked <<= 1;
		}
		int lastCheck = dist - checked;
		board = board.and(board.shiftRight(lastCheck));
		return !board.equals(BigInteger.ZERO);
	}

	/**
	 * Clears the board
	 */
	public void clear() {
		if (usesLong) {
			xPlayerLong = 0L;
			oPlayerLong = 0L;
		} else {
			xPlayer = BigInteger.ZERO;
			oPlayer = BigInteger.ZERO;
		}
	}

	public String toString() {
		StringBuilder str = new StringBuilder(width * 2 + 1);
		if (usesLong) {
			for (int row = height - 1; row >= 0; row--) {
				str.append('|');
				for (int col = 0; col < width; col++) {
					if ((xPlayerLong & (1L << getBit(row, col))) > 0L)
						str.append('X');
					else if ((oPlayerLong & (1L << getBit(row, col))) > 0L)
						str.append('O');
					else
						str.append(' ');
					str.append('|');
				}
				str.append('\n');
			}
		} else {
			for (int row = height - 1; row >= 0; row--) {
				str.append('|');
				for (int col = 0; col < width; col++) {
					if (xPlayer.testBit(getBit(row, col)))
						str.append('X');
					else if (oPlayer.testBit(getBit(row, col)))
						str.append('O');
					else
						str.append(' ');
					str.append('|');
				}
				str.append('\n');
			}
		}
		return str.toString();
	}
}
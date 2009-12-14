package edu.berkeley.gamesman.game.util;

import java.math.BigInteger;

/**
 * When xInALine is called, it returns false if the other player has four in a
 * row or if the four-in-a-row are not on top. If you can think of any other
 * ways to quickly check for impossible positions, please let me know
 * 
 * EDIT: This class doesn't seem to help reduce database size... It may be
 * useless
 * 
 * @author dnspies
 */
public class Connect4ReducerBoard extends BitSetBoard {
	boolean badPosition;

	/**
	 * @param gameHeight
	 *            The height of the game
	 * @param gameWidth
	 *            The width of the game
	 */
	public Connect4ReducerBoard(int gameHeight, int gameWidth) {
		super(gameHeight, gameWidth);
		badPosition = false;
	}

	@Override
	protected boolean checkDirection(int x, int direction, long board) {
		int dist = direction * x;
		int checked = direction;
		while (checked << 1 < dist) {
			board &= (board >> checked);
			checked <<= 1;
		}
		int lastCheck = dist - checked;
		board &= (board >> lastCheck);
		if (board == 0)
			return false;
		else {
			dist = direction * x;
			checked = direction;
			while (checked << 1 < dist) {
				board |= (board << checked);
				checked <<= 1;
			}
			lastCheck = dist - checked;
			board |= (board << lastCheck);
			long sky = ~(xPlayerLong | oPlayerLong);
			board = (board << 1) & sky;
			if (board == 0)
				badPosition = true;
			return true;
		}
	}

	@Override
	protected boolean checkDirection(int x, int direction, BigInteger board) {
		int dist = direction * x;
		int checked = direction;
		while (checked << 1 < dist) {
			board = board.and(board.shiftRight(checked));
			checked <<= 1;
		}
		int lastCheck = dist - checked;
		board = board.and(board.shiftRight(lastCheck));
		if (board.equals(BigInteger.ZERO))
			return false;
		else {
			dist = direction * x;
			checked = direction;
			while (checked << 1 < dist) {
				board = board.or(board.shiftLeft(checked));
				checked <<= 1;
			}
			lastCheck = dist - checked;
			board = board.or(board.shiftLeft(lastCheck));
			BigInteger sky = xPlayer.or(oPlayer).not();
			board = (board.shiftLeft(1)).and(sky);
			if (board.equals(BigInteger.ZERO))
				badPosition = true;
			return true;
		}
	}

	@Override
	public int xInALine(int x, char color) {
		boolean isWin;
		if (usesLong) {
			long board = (color == 'X' ? oPlayerLong : xPlayerLong);
			if (checkDirection(x, 1, board) || checkDirection(x, height, board)
					|| checkDirection(x, height + 1, board)
					|| checkDirection(x, height + 2, board)) {
				badPosition = false;
				return -1;
			}
			board = (color == 'X' ? xPlayerLong : oPlayerLong);
			isWin = checkDirection(x, 1, board);
			isWin = checkDirection(x, height, board) || isWin;
			isWin = checkDirection(x, height + 1, board) || isWin;
			isWin = checkDirection(x, height + 2, board) || isWin;
			if (badPosition) {
				badPosition = false;
				return -1;
			} else if (isWin)
				return 1;
			else
				return 0;
		}
		return 0;
	}
}
package edu.berkeley.gamesman.game.util;

import java.math.BigInteger;

/**
 * When xInALine is called, it returns false if the other player has four in a
 * row or if the four-in-a-row are not on top. If you can think of any other
 * ways to quickly check for impossible positions, please let me know
 * 
 * @author dnspies
 */
public class Connect4ReducerBoard extends BitSetBoard {
	boolean badPosition;

	public Connect4ReducerBoard(int gameHeight, int gameWidth) {
		super(gameHeight, gameWidth);
		badPosition = false;
	}

	@Override
	protected boolean checkDirection(int x, int direction, long board) {
		int absDir = Math.abs(direction);
		int sign = direction > 0 ? 1 : -1;
		int dist = absDir * x;
		int checked = absDir;
		while (checked << 1 < dist) {
			if (sign > 0)
				board &= (board << checked);
			else
				board &= (board >> checked);
			checked <<= 1;
		}
		int lastCheck = dist - checked;
		if (sign > 0)
			board &= (board << lastCheck);
		else
			board &= (board >> lastCheck);
		if (board == 0)
			return false;
		else {
			long sky = ~(xPlayerLong & oPlayerLong);
			board = (board << 1) & sky;
			if (board == 0)
				badPosition = true;
			return true;
		}
	}

	@Override
	protected boolean checkDirection(int x, int direction, BigInteger board) {
		int absDir = Math.abs(direction);
		int sign = direction > 0 ? 1 : -1;
		int dist = absDir * x;
		int checked = absDir;
		while (checked << 1 < dist) {
			if (sign > 0)
				board = board.and(board.shiftLeft(checked));
			else
				board = board.and(board.shiftRight(checked));
			checked <<= 1;
		}
		int lastCheck = dist - checked;
		if (sign > 0)
			board = board.and(board.shiftLeft(lastCheck));
		else
			board = board.and(board.shiftRight(lastCheck));
		if (board.equals(BigInteger.ZERO))
			return false;
		else {
			BigInteger sky = xPlayer.and(oPlayer).not();
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
			long board = (color == 'X' ? xPlayerLong : oPlayerLong);
			isWin = checkDirection(x, 1, board)
					|| checkDirection(x, -height, board)
					|| checkDirection(x, height + 1, board)
					|| checkDirection(x, height + 2, board);
			board = (color == 'X' ? oPlayerLong : xPlayerLong);
			if (isWin) {
				if (checkDirection(x, 1, board)
						|| checkDirection(x, -height, board)
						|| checkDirection(x, height + 1, board)
						|| checkDirection(x, height + 2, board) || badPosition) {
					badPosition = false;
					return -1;
				} else
					return 1;
			} else if (badPosition) {
				badPosition = false;
				return -1;
			} else
				return 0;
		} else {
			BigInteger board = (color == 'X' ? xPlayer : oPlayer);
			isWin = checkDirection(x, 1, board)
					|| checkDirection(x, -height, board)
					|| checkDirection(x, height + 1, board)
					|| checkDirection(x, height + 2, board);
			board = (color == 'X' ? oPlayer : xPlayer);
			if (isWin) {
				if (checkDirection(x, 1, board)
						|| checkDirection(x, -height, board)
						|| checkDirection(x, height + 1, board)
						|| checkDirection(x, height + 2, board) || badPosition) {
					badPosition = false;
					return -1;
				} else
					return 1;
			} else if (badPosition) {
				badPosition = false;
				return -1;
			} else
				return 0;
		}
	}
}
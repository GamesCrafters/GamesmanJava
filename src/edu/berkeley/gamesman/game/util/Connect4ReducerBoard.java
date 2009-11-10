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

	public Connect4ReducerBoard(int gameHeight, int gameWidth) {
		super(gameHeight, gameWidth);
	}

	private boolean checkDirection(int x, int direction, long board,
			boolean useSky) {
		int dist = direction * x;
		int checked = direction;
		while (checked << 1 < dist) {
			board = board & (board << checked);
			checked <<= 1;
		}
		int lastCheck = dist - checked;
		board = board & (board << lastCheck);
		if (useSky) {
			long sky = ~(xPlayerLong & oPlayerLong);
			board = (board << 1) & sky;
		}
		return board != 0;
	}

	private boolean checkDirection(int x, int direction, BigInteger board,
			boolean useSky) {
		int dist = direction * x;
		int checked = direction;
		while (checked << 1 < dist) {
			board = board.and(board.shiftLeft(checked));
			checked <<= 1;
		}
		int lastCheck = dist - checked;
		board = board.and(board.shiftLeft(lastCheck));
		if (useSky) {
			BigInteger sky = xPlayer.or(oPlayer).not();
			board = board.shiftLeft(1).and(sky);
		}
		return !board.equals(BigInteger.ZERO);
	}

	@Override
	public boolean xInALine(int x, char color) {
		boolean isWin;
		if (usesLong) {
			long board = (color == 'X' ? xPlayerLong : oPlayerLong);
			isWin = checkDirection(x, 1, board, true)
					|| checkDirection(x, height, board, true)
					|| checkDirection(x, height + 1, board, true)
					|| checkDirection(x, height + 2, board, true);
			board = (color == 'X' ? oPlayerLong : xPlayerLong);
			isWin &= !(checkDirection(x, 1, board, false)
					|| checkDirection(x, height, board, false)
					|| checkDirection(x, height + 1, board, false) || checkDirection(
					x, height + 2, board, false));
		} else {
			BigInteger board = (color == 'X' ? xPlayer : oPlayer);
			isWin = checkDirection(x, 1, board, true)
					|| checkDirection(x, height, board, true)
					|| checkDirection(x, height + 1, board, true)
					|| checkDirection(x, height + 2, board, true);
			board = (color == 'X' ? oPlayer : xPlayer);
			isWin &= !(checkDirection(x, 1, board, false)
					|| checkDirection(x, height, board, false)
					|| checkDirection(x, height + 1, board, false) || checkDirection(
					x, height + 2, board, false));
		}
		return isWin;
	}
}
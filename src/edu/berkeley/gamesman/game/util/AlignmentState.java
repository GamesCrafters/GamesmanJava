package edu.berkeley.gamesman.game.util;

import edu.berkeley.gamesman.core.State;

public class AlignmentState implements State {
	public char[][] board; // chars are 'X', 'O' and ' ' (X plays first) should be
	// char[] to accomodate Dead_squares and no corners
	public char lastMove;
	public int xDead;
	public int oDead;

	public AlignmentState(char[][] board, int xDead, int oDead, char lastMove) {
		this.board = board;
		this.xDead = xDead;
		this.oDead = oDead;
		this.lastMove = lastMove;
	}

	public AlignmentState(AlignmentState pos) {
		this.board = pos.board;
		this.xDead = pos.xDead;
		this.oDead = pos.oDead;
		this.lastMove = pos.lastMove;
	}

	public void set(State s) {
		AlignmentState as = (AlignmentState) s;
		for (int row = 0; row < board.length; row++) {
			for (int col = 0; col < board[row].length; col++) {
				board[row][col] = as.board[row][col];
			}
		}
		xDead = as.xDead;
		oDead = as.oDead;
		lastMove = as.lastMove;
	}

	public void set(char[][] board, int xDead, int oDead, char lastMove) {
		this.board = board;
		this.xDead = xDead;
		this.oDead = oDead;
		this.lastMove = lastMove;
	}

	public char get(int row, int col) {
		return board[row][col];
	}

	public Boolean full() {
		for (int row = 0; row < board.length; row++) {
			for (int col = 0; col < board[0].length; col++) {
				if (board[row][col] == ' ') {
					return false;
				}
			}
		}
		return true;
	}

	public void put(int row, int col, char piece) {
		board[row][col] = piece;
	}

	/** Returns true if the piece at (x0,y0) can be moved to (x1,y1)) */
	public Boolean legalMove(int row0, int col0, int row1, int col1) {
		return adjacent(row0, col0, row1, col1) && (board[row1][col1] == ' ');
	}

	/** true if the square (x0,y0) is one of 8 points adjacent to (x1, y1) */
	static Boolean adjacent(int row0, int col0, int row1, int col1) {
		return (Math.abs(row1 - row0) <= 1 && Math.abs(col1 - col0) <= 1 && !(col1 == col0 && row1 == row0));
	}
}

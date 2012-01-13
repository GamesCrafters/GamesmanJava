package edu.berkeley.gamesman.parallel.game.tictactoe;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;


public class TTTState implements WritableSettableComparable<TTTState> {
	private final int[][] board = new int[3][3];
	private int turn = 1;

	@Override
	public void write(DataOutput out) throws IOException {
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				out.writeInt(board[row][col]);
			}
		}
		out.writeInt(turn);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				board[row][col] = in.readInt();
			}
		}
		turn = in.readInt();
	}

	@Override
	public void set(TTTState t) {
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				board[row][col] = t.board[row][col];
			}
		}
		turn = t.turn;
	}

	@Override
	public int compareTo(TTTState o) {
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int diff = board[row][col] - o.board[row][col];
				if (diff != 0)
					return diff;
			}
		}
		return 0;
	}

	public int get(int row, int col) {
		return exists(row, col) ? board[row][col] : -1;
	}

	private boolean exists(int row, int col) {
		return row >= 0 && row < 3 && col >= 0 && col < 3;
	}

	public void play(int row, int col) {
		if (board[row][col] != 0)
			throw new RuntimeException("Space " + row + "," + col
					+ " is not empty");
		board[row][col] = turn;
		turn = opposite(turn);
	}

	public static int opposite(int piece) {
		return 3 - piece;
	}

	public boolean isFull() {
		return numPieces() == 9;
	}

	public int numPieces() {
		int count = 0;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				if (board[row][col] != 0)
					count++;
			}
		}
		return count;
	}

	public boolean isWin() {
		int checking = opposite(turn);
		boolean foundD1 = true, foundD2 = true;
		for (int i = 0; i < 3; i++) {
			boolean foundUD = true;
			boolean foundLR = true;
			for (int j = 0; j < 3; j++) {
				if (board[i][j] != checking)
					foundLR = false;
				if (board[j][i] != checking)
					foundUD = false;
			}
			if (foundUD || foundLR)
				return true;
			if (board[i][i] != checking)
				foundD1 = false;
			if (board[i][2 - i] != checking)
				foundD2 = false;
		}
		return foundD1 || foundD2;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TTTState && equals((TTTState) other);
	}

	@Override
	public int hashCode() {
		return Arrays.deepHashCode(board);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(32);
		for (int row = 2; row >= 0; row--) {
			sb.append(row + 1);
			for (int col = 0; col < 3; col++) {
				sb.append(" " + getChar(row, col));
			}
			sb.append("\n");
		}
		sb.append("  A B C\n");
		return sb.toString();
	}

	public char getChar(int row, int col) {
		return getChar(get(row, col));
	}

	private static char getChar(int piece) {
		switch (piece) {
		case 0:
			return ' ';
		case 1:
			return 'X';
		case 2:
			return 'O';
		default:
			return '?';
		}
	}

	public boolean equals(TTTState other) {
		return compareTo(other) == 0;
	}

	public void set(int row, int col, char val) {
		int piece = pieceFor(val);
		if ((piece == 0) != (board[row][col] == 0))
			turn = opposite(turn);
		board[row][col] = piece;
	}

	public static int pieceFor(char val) {
		switch (val) {
		case ' ':
			return 0;
		case 'X':
			return 1;
		case 'O':
			return 2;
		default:
			throw new RuntimeException("Unknown piece: " + val);
		}
	}
}
package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.*;

/**
 * The game Y
 * 
 * @author dnspies
 */
public class YGame extends ConnectGame {
	private final class Space {
		// t = triangle, r = row c = column
		final int t, r, c;
		final int charNum;
		final boolean[] isOnEdge = new boolean[3];
		final Space[] connectedSpaces;

		Space(int t, int r, int c, int charNum) {
			this.t = t;
			this.r = r;
			this.c = c;
			// index into board
			this.charNum = charNum;
			connectedSpaces = null; // TODO Correct this. First initiate with
			// correct size, then fill it in (in
			// constructor of YGame) when all spaces
			// have been initialized
		}

		char getChar() {
			return board[charNum];
		}

		void setChar(char c) {
			board[charNum] = c;
		}
	}

	private Space[][][] yBoard;

	private char[] board;

	private int boardSide;

	private int boardSize;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public void initialize(Configuration conf) {
		super.initialize(conf);
		boardSide = conf.getInteger("game.sideLength", 4);
		yBoard = new Space[3][boardSide - 1][];
		int n = 0;
		for (int t = 0; t < 3; t++) {
			int i;
			for (i = 0; i < boardSide - 1; i++) {
				yBoard[t][i] = new Space[i + 1];
				for (int c = 0; c <= i; c++)
					yBoard[t][i][c] = new Space(t, i, c, n++);
			}
			i = boardSide - 2;
			for (int c = 0; c <= i; c++) {
				yBoard[t][i][c].isOnEdge[t] = true;
			}
			yBoard[t][i][i].isOnEdge[(t + 1) % 3] = true;
		}
		boardSize = boardSide * (boardSide - 1) / 2 * 3;
		board = new char[boardSize];
	}

	/**
	 * Given any two spaces who know their position, this method tells whether
	 * they are connected.
	 * 
	 * @param s1
	 *            First Space
	 * @param s2
	 *            Second Space
	 * @return Are they connected?
	 */
	private boolean connected(Space s1, Space s2) {
		if (s1.t == s2.t) {
			if (s1.r == s2.r && Math.abs(s1.c - s2.c) == 1)
				return true;
			else if (s1.c == s2.c && Math.abs(s1.r - s2.r) == 1)
				return true;
			else
				// correct diagonal
				return Math.abs(s1.r - s2.r) == 1
						&& (s1.r - s2.r == s1.c - s2.c);
		}
		// if they are in separate triangles, switch them.
		else if ((s2.t + 3 - s1.t) % 3 == 2) {
			Space temp = s2;
			s2 = s1;
			s1 = temp;
		}
		// between triangles
		return s2.c == 0 && s1.c == s1.r && s2.r >= s1.r && s2.r - s1.r <= 1;
	}

	@Override
	protected int getBoardSize() {
		return boardSize;
	}

	@Override
	protected char[] getCharArray() {
		return board;
	}

	@Override
	protected boolean isWin(char c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void setToCharArray(char[] myPieces) {
		if (board != myPieces)
			for (int i = 0; i < boardSize; i++)
				board[i] = myPieces[i];
	}

	@Override
	public String displayState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String describe() {
		return "Y - " + boardSide;
	}
}

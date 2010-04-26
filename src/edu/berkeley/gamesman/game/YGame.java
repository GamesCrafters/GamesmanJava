package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.util.Util;

/**
 * The game Y
 * 
 * @author dnspies
 */
public class YGame extends ConnectGame {
	private final class Space {
		// t = triangle, r = row c = column
		final int t, r, c;
		private final int charNum;
		int iter;
		final boolean[] isOnEdge = new boolean[3];
		Space[] connectedSpaces;

		Space(int t, int r, int c, int charNum) {
			this.t = t;
			this.r = r;
			this.c = c;
			// index into board
			this.charNum = charNum;
			this.iter = 0;
			connectedSpaces = new Space[6];
		}

		char getChar() {
			return board[charNum];
		}

		void setChar(char c) {
			board[charNum] = c;
		}
	}

	// Added a stack data structure for use in the isWin() function. - Rohit
	private final class Stack {

		// stackNode object. Contains the Space and next pointer.
		private final class stackNode {
			Space data;
			stackNode next;

			stackNode(Space s) {
				data = s;
				next = null;
			}
		}

		stackNode top;

		// Stack constructor
		Stack() {
			top = null;
		}

		void push(Space s) {
			if (top == null) {
				top = new stackNode(s);
				top.next = null;
			} else {
				stackNode temp = new stackNode(s);
				temp.next = top;
				top = temp;
			}
		}

		Space pop() {
			if (top == null)
				return null;
			stackNode temp = top;
			top = top.next;
			return temp.data;
		}
	} // End stack class

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
		for (int t = 0; t < 3; t++) {
			for (int i = 0; i < boardSide - 1; i++) {
				for (int c = 0; c <= i; c++) {
					yBoard[t][i][c].connectedSpaces = getNeighbors(t, i, c);
				}
			}
		}
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
		// add 3 because we don't want to mod a negative value.
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

	protected Space[] getNeighbors(int t, int r, int c) {
		Space[] neighbors;
		if (c == 0 && r == 0) {
			neighbors = new Space[5];
			neighbors[0] = getSpace(Util.nonNegativeModulo(t - 1, 3), 0, 0);
			neighbors[1] = getSpace(Util.nonNegativeModulo(t + 1, 3), 0, 0);
			neighbors[2] = getSpace(t, 1, 1);
			neighbors[3] = getSpace(t, 1, 0);
			neighbors[4] = getSpace(Util.nonNegativeModulo(t - 1, 3), 1, 1);
		} else if (c == 0) {
			neighbors = new Space[6];
			neighbors[0] = getSpace(t, r - 1, c);
			neighbors[1] = getSpace(t, r, c + 1);
			neighbors[2] = getSpace(t, r + 1, c + 1);
			neighbors[3] = getSpace(t, r + 1, c);
			neighbors[4] = getSpace(Util.nonNegativeModulo(t - 1, 3), r, r);
			neighbors[5] = getSpace(Util.nonNegativeModulo(t - 1, 3), r - 1,
					r - 1);
		} else if (c == r) {
			neighbors = new Space[6];
			neighbors[0] = getSpace(t, r - 1, c - 1);
			neighbors[1] = getSpace(Util.nonNegativeModulo(t + 1, 3), r - 1, 0);
			neighbors[2] = getSpace(Util.nonNegativeModulo(t + 1, 3), r, 0);
			neighbors[3] = getSpace(t, r + 1, c + 1);
			neighbors[4] = getSpace(t, r + 1, c);
			neighbors[5] = getSpace(t, r, c - 1);
		} else {
			neighbors = new Space[6];
			neighbors[0] = getSpace(t, r - 1, c);
			neighbors[1] = getSpace(t, r, c + 1);
			neighbors[2] = getSpace(t, r + 1, c + 1);
			neighbors[3] = getSpace(t, r + 1, c);
			neighbors[4] = getSpace(t, r, c - 1);
			neighbors[5] = getSpace(t, r - 1, c - 1);
		}
		return neighbors;
	}

	private Space getSpace(int t, int r, int c) {
		if (t >= 0 && t < 3 && r < boardSide - 1 && r >= 0 && c >= 0 && c <= r)
			return yBoard[t][r][c];
		else
			return null;
	}

	@Override
	// Uses the stack class in order to avoid recursion.
	protected boolean isWin(char player) {
		// start point
		boolean left = false;
		Space x = yBoard[0][boardSize - 2][0];
		Space y;

		int col = 0;
		while (true) {
			for (; col < boardSize - 1; col++) {
				x = yBoard[0][boardSize - 2][col];
				if (x.getChar() == player) {
					break;
				}
			}
			if (col == boardSize - 1) {
				x = yBoard[1][boardSize - 2][0];
				if (x.getChar() != player) {
					return false;
				}
			}
			y = x.connectedSpaces[x.connectedSpaces.length - 1];
			OUTER: while (true) {
				int i;
				for (i = 0; i < x.connectedSpaces.length; i++) {
					if (y == x.connectedSpaces[i]) {
						break;
					}
				}
				do {
					i++;
					if (i == x.connectedSpaces.length) {
						i = 0;
					}
					y = x.connectedSpaces[i];
					if (y == null) {
						if (x.isOnEdge[2]) {
							return left || x.isOnEdge[1];
						} else if (x.isOnEdge[1]) {
							left = true;
							while (y == null) {
								i++;
								if (i == x.connectedSpaces.length)
									i = 0;
								y = x.connectedSpaces[i];
							}
						} else {
							col = Math.max(col, x.c);
							left = false;
							break OUTER;
						}
					}
				} while (y.getChar() != player);
				Space temp = x;
				x = y;
				y = temp;
			}
		}
	}

	@Override
	protected void setToCharArray(char[] myPieces) {
		if (board != myPieces)
			for (int i = 0; i < boardSize; i++)
				board[i] = myPieces[i];
	}

	@Override
	public String displayState() {
		StringBuffer s = new StringBuffer();
		s
				.append("    " + yBoard[0][2][0] + " ----- " + yBoard[0][2][1]
						+ " ----- " + yBoard[0][2][2] + "----"
						+ yBoard[2][2][0] + "\n");
		s.append("   /  \\   /   \\   /  \\ / |\n");
		s.append("   " + yBoard[1][2][2] + "--- " + yBoard[0][1][0] + "------ "
				+ yBoard[0][1][1] + "    " + yBoard[2][1][0] + "   /\n");
		s.append("   | \\ /  \\   /    /    /\n");
		s.append("   \\   " + yBoard[1][1][1] + "    " + yBoard[0][0][0]
				+ "----" + yBoard[2][0][0] + "     /\n");
		s.append("    \\    \\ /  /  |    " + yBoard[2][2][1] + "\n");
		s.append("     \\     " + yBoard[1][0][0] + "-----" + yBoard[2][1][1]
				+ "   /\n");
		s.append("      " + yBoard[1][2][1] + "    |  /  |  /\n");
		s.append("       \\   " + yBoard[1][1][0] + "     | /\n");
		s.append("        \\  |     " + yBoard[2][2][2] + "\n");
		s.append("         \\ |  /\n");
		s.append("           " + yBoard[1][2][0] + "  \n");

		return s.toString();
	}

	@Override
	public String describe() {
		return "Y - " + boardSide;
	}
}

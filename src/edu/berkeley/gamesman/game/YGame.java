package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * The game Y
 * 
 * @author dnspies
 */
public class YGame extends ConnectGame {
	private int[] translateInArray;

	private int[] translateOutArray;

	String formatString = "\n                 220_\n"
			+ "                /| \\\n" + "               / |  \\\n"
			+ "              /  |   \\\n" + "             /   |    \\\n"
			+ "            /    |     \\\n" + "           /    /       \\\n"
			+ "          221----210__-------122\n"
			+ "         /|  /  \\ \\_____ |\\\n"
			+ "        / | / __200_______\\| \\\n"
			+ "       /  |/_/ / \\_      111_ \\\n"
			+ "      /  _211   /    \\_    |\\\\_\\\n"
			+ "     /__/ |\\ /       \\_ /  \\ \\\\\n"
			+ "    222   _/  000__--------100__ | _=121\n"
			+ "   / \\ /___/   \\__ __/    \\|/   \\\n"
			+ "  / __010---------__011__------110____ \\\n"
			+ " /_/     \\__ __/     \\__ _/     \\_\\\n"
			+ "020-----------021-----------022----------120\n";

	private final class Space {
		// t = triangle, r = row c = column
		final int t, r, c;
		private final int charNum;
		final boolean[] isOnEdge = new boolean[3];
		Space[] connectedSpaces;

		Space(int t, int r, int c, int charNum) {
			this.t = t;
			this.r = r;
			this.c = c;
			// index into board
			this.charNum = charNum;
			connectedSpaces = new Space[6];
		}

		char getChar() {
			return board[charNum];
		}

		@Override
		public String toString() {
			return String.valueOf(getChar());
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
		initialize(conf.getInteger("game.sideLength", 4));
	}

	private void initialize(int boardSide) {
		this.boardSide = boardSide;
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
			yBoard[t][i][0].isOnEdge[t == 0 ? 2 : (t - 1)] = true;
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
		int triangleSize = boardSide * (boardSide - 1) / 2;
		int i = 0;
		translateInArray = new int[boardSize];
		for (int t = 0; t < 3; t++) {
			translateInArray[i++] = t * triangleSize;
		}
		for (int row = boardSide - 2; row > 0; row--) {
			for (int t = 2; t >= 0; t--) {
				translateInArray[i++] = Util.nonNegativeModulo(t + 1, 3)
						* triangleSize + row * (row + 1) / 2;
				for (int col = row; col > 0; col--) {
					translateInArray[i++] = t * triangleSize + row * (row + 1)
							/ 2 + col;
				}
			}
		}

		translateOutArray = new int[boardSize];
		for (i = 0; i < boardSize; i++) {
			translateOutArray[translateInArray[i]] = i;
		}
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
			neighbors[0] = getSpace(Util.nonNegativeModulo(t - 1, 3), 1, 1);
			neighbors[1] = getSpace(Util.nonNegativeModulo(t - 1, 3), 0, 0);
			neighbors[2] = getSpace(Util.nonNegativeModulo(t + 1, 3), 0, 0);
			neighbors[3] = getSpace(t, 1, 1);
			neighbors[4] = getSpace(t, 1, 0);
		} else if (c == 0) {
			neighbors = new Space[6];
			neighbors[0] = getSpace(Util.nonNegativeModulo(t - 1, 3), r + 1,
					r + 1);
			neighbors[1] = getSpace(Util.nonNegativeModulo(t - 1, 3), r, r);
			neighbors[2] = getSpace(t, r - 1, c);
			neighbors[3] = getSpace(t, r, c + 1);
			neighbors[4] = getSpace(t, r + 1, c + 1);
			neighbors[5] = getSpace(t, r + 1, c);
		} else if (c == r) {
			neighbors = new Space[6];
			neighbors[0] = getSpace(t, r, c - 1);
			neighbors[1] = getSpace(t, r - 1, c - 1);
			neighbors[2] = getSpace(Util.nonNegativeModulo(t + 1, 3), r - 1, 0);
			neighbors[3] = getSpace(Util.nonNegativeModulo(t + 1, 3), r, 0);
			neighbors[4] = getSpace(t, r + 1, c + 1);
			neighbors[5] = getSpace(t, r + 1, c);
		} else {
			neighbors = new Space[6];
			neighbors[0] = getSpace(t, r, c - 1);
			neighbors[1] = getSpace(t, r - 1, c - 1);
			neighbors[2] = getSpace(t, r - 1, c);
			neighbors[3] = getSpace(t, r, c + 1);
			neighbors[4] = getSpace(t, r + 1, c + 1);
			neighbors[5] = getSpace(t, r + 1, c);
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
	protected boolean isWin(char player) {
		assert Util.debug(DebugFacility.GAME, displayState());
		boolean left = false;
		Space x = yBoard[0][boardSide - 2][0];
		Space y;

		int col = -1;
		while (true) {
			for (col++; col < boardSide - 1; col++) {
				x = yBoard[0][boardSide - 2][col];
				if (x.getChar() == player) {
					break;
				}
			}
			if (col == boardSide - 1) {
				x = yBoard[1][boardSide - 2][0];
				if (x.getChar() != player) {
					return false;
				}
			}
			if (col == 0)
				left = true;
			y = x.connectedSpaces[x.t]; // Test each case. It makes sense
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
						if (x.isOnEdge[1]) {
							return left || x.isOnEdge[2];
						} else if (x.isOnEdge[0]) {
							col = Math.max(col, x.c);
							left = false;
							break OUTER;
						} else {
							left = true;
							while (y == null) {
								i++;
								if (i == x.connectedSpaces.length)
									i = 0;
								y = x.connectedSpaces[i];
							}
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
		StringBuilder sb = new StringBuilder(formatString.length() - 2
				* boardSize);
		for (int i = 0; i < formatString.length(); i++) {
			if (formatString.charAt(i) >= '0' && formatString.charAt(i) <= '9') {
				sb.append(yBoard[formatString.charAt(i) - '0'][formatString
						.charAt(i + 1) - '0'][formatString.charAt(i + 2) - '0']
						.getChar());
				i += 2;
			} else {
				sb.append(formatString.charAt(i));
			}
		}
		return sb.toString();
	}

	@Override
	public String describe() {
		return "Y" + boardSide;
	}

	public String toString() {
		return displayState();
	}

	@Override
	public char[] convertInString(String s) {
		char[] charArray = new char[boardSize];
		for (int i = 0; i < boardSize; i++)
			charArray[translateInArray[i]] = s.charAt(i);
		return charArray;
	}

	@Override
	public String convertOutString(char[] charArray) {
		StringBuilder sb = new StringBuilder(boardSize);
		for (int i = 0; i < boardSize; i++)
			sb.append(charArray[translateInArray[i]]);
		return sb.toString();
	}

	@Override
	public int translateOut(int i) {
		return translateOutArray[i];
	}
}

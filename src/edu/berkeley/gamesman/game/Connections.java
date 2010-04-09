package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;

/**
 * The game Connections
 * 
 * @author dnspies
 */
public class Connections extends ConnectGame {
	private class Edge {
		Point[] xPoints;
		Point[] oPoints;
		private int charNum;

		char getChar() {
			return board[charNum];
		}

		void setChar(char c) {
			board[charNum] = c;
		}
	}

	private class Point {
		private char color;
		private Edge[] edges;
		// List in clockwise order
	}

	private int boardSide;
	private int boardSize;
	private Point[][] xPoints;
	private Point[][] oPoints;
	private Edge[][] edges;
	private char[] board;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public void initialize(Configuration conf) {
		super.initialize(conf);
		boardSide = conf.getInteger("game.sideLength", 4);
		boardSize = boardSide * boardSide;
		xPoints = new Point[boardSide + 1][boardSide];
		oPoints = new Point[boardSide + 1][boardSide];
		edges = new Edge[boardSide][boardSide];
		board = new char[boardSize];
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
	protected void setToCharArray(char[] myPieces) {
		if (board != myPieces) {
			for (int i = 0; i < board.length; i++)
				board[i] = myPieces[i];
		}
	}

	@Override
	public String displayState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String describe() {
		return "Connections " + boardSide + "x" + boardSide;
	}

	@Override
	protected boolean isWin(char c) {
		// TODO Auto-generated method stub
		return false;
	}
}

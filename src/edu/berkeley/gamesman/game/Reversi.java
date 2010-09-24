package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.Pair;

public class Reversi extends TierGame {
	private final int width;
	private final int height;
	private final int boardSize;
	private final Cell[][] board;
	private final char[] pieces;
	private int numPieces = 0;

	private class Cell {
		final int row, col;
		final int boardNum;

		public Cell(int row, int col, int boardNum) {
			this.row = row;
			this.col = col;
			this.boardNum = boardNum;
			pieces[boardNum] = ' ';
		}

		public char getPiece() {
			return pieces[boardNum];
		}

		public void setPiece(char p) {
			if (pieces[boardNum] != ' ' && p == ' ')
				numPieces--;
			else if (pieces[boardNum] == ' ' && p != ' ')
				numPieces++;
			if (!(p == ' ' || p == 'X' || p == 'O'))
				throw new Error("Bad piece: " + p);
			pieces[boardNum] = p;
		}
	}

	public Reversi(Configuration conf) {
		super(conf);
		width = conf.getInteger("gamesman.game.width", 8);
		height = conf.getInteger("gamesman.game.height", 8);
		boardSize = width * height;
		pieces = new char[boardSize];
		board = new Cell[height][width];
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				board[row][col] = new Cell(row, col, row * width + col);
			}
		}
		board[height / 2][width / 2].setPiece('X');
		board[height / 2 + 1][width / 2].setPiece('O');
		board[height / 2][width / 2 + 1].setPiece('O');
		board[height / 2 + 1][width / 2 + 1].setPiece('X');
	}

	@Override
	public void setState(TierState pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public Value primitiveValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTier() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String stateToString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFromString(String pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public TierState getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long numHashesForTier(int tier) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String displayState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setStartingPosition(int n) {
		// TODO Auto-generated method stub

	}

	@Override
	public int numStartingPositions() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasNextHashInTier() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void nextHashInTier() {
		// TODO Auto-generated method stub

	}

	@Override
	public int numberOfTiers() {
		return boardSize + 1;
	}

	@Override
	public int maxChildren() {
		return boardSize;
	}

	@Override
	public int validMoves(TierState[] moves) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String describe() {
		return width +"x"+height+" Reversi";
	}

	@Override
	public long recordStates() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void longToRecord(TierState recordState, long record, Record toStore) {
		// TODO Auto-generated method stub

	}

	@Override
	public long recordToLong(TierState recordState, Record fromRecord) {
		// TODO Auto-generated method stub
		return 0;
	}

}

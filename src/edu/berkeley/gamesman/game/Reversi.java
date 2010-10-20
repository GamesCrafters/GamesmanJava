package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.DartboardHasher;
import edu.berkeley.gamesman.util.Pair;

public class Reversi extends TierGame {
	private final int width;
	private final int height;
	private final int boardSize;
	private final Cell[][] board;
	private final char[] pieces;
	private int numPieces = 0;
	private final DartboardHasher dbh;
	private int turn; //1 for black, 0 for white.
	private final static int BLACK = 1;
	private final static int WHITE = 0;
	private final int[][] offsetTable;
	private TierState[] children;
	private boolean isChildrenValid;

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
		dbh = new DartboardHasher(boardSize, ' ', 'O', 'X');
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				board[row][col] = new Cell(row, col, row * width + col);
			}
		}
		board[height / 2][width / 2].setPiece('X');
		board[height / 2 + 1][width / 2].setPiece('O');
		board[height / 2][width / 2 + 1].setPiece('O');
		board[height / 2 + 1][width / 2 + 1].setPiece('X');
		
		turn = BLACK;
		isChildrenValid = false;
		children = new TierState[0];
		
		offsetTable = new int[boardSize+1][];
		//initalize offset table
	}

	@Override
	public void setState(TierState pos) {
		numPieces = pos.tier;
		dbh.unhash(pos.hash);
		setBoard();
	}
	
	// Sets the internal board based on the current DartBboardHasher dbh.
	private void setBoard() {
		
	}

	@Override
	public Value primitiveValue() {
		Value value = Value.UNDECIDED;
		if (!(isChildrenValid)) {
			children = getChildren();
			isChildrenValid = true;
		}
		if ((isChildrenValid && children.length == 0)) {
			int black = 0;
			int white = 0;
			for (int boardNumber = 0;boardNumber < boardSize - 1;boardNumber++) {
				if (pieces[boardNumber] == 'O')
					white++;
				if (pieces[boardNumber] == 'X')
					black++;
			}
			if (black == white) {
				value = Value.TIE;
			} else if ((turn == BLACK && black > white) || (turn == WHITE && white > black)) {
				value = Value.WIN;
			} else if ((turn == BLACK && black < white) || (turn == WHITE && white < black)) {
				value = Value.LOSE;
			}
		}
		return value;
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTier() {
		return numPieces;
	}

	@Override
	public String stateToString() {
		String answer = "";
		for (int boardNumber = 0;boardNumber < boardSize - 1;boardNumber++) {
			answer += pieces[boardNumber];
		}
		return answer;
	}

	@Override
	public void setFromString(String pos) {
		if (pos.length() != boardSize - 1)
			throw new Error("Bad String: wrong length");
		else {
			for (int row = 0; row < height; row++) {
				for (int col = 0; col < width; col++) {
					board[row][col].setPiece(pos.charAt(row * width + col));
				}
			}
		}
	}

	@Override
	public void getState(TierState state) {
		state.tier = numPieces;
		state.hash = dbh.hash(pieces);
	}

	@Override
	public long numHashesForTier(int tier) {
		return offsetTable[tier][offsetTable[tier].length - 1];
	}

	@Override
	public String displayState() {
		String s = stateToString();
		StringBuilder str = new StringBuilder((width + 3) * height);
		for (int row = height - 1; row>=0; row--) 
			str.append("|" + s.substr(row,(row+1)*width) + "|\n");
		return str.toString();
	}

	@Override
	public void setStartingPosition(int n) {
		for (int boardNumber = 0;boardNumber < boardSize - 1;boardNumber++) {
			pieces[boardNumber] = ' ';
		}
		board[height / 2][width / 2].setPiece('X');
		board[height / 2 + 1][width / 2].setPiece('O');
		board[height / 2][width / 2 + 1].setPiece('O');
		board[height / 2 + 1][width / 2 + 1].setPiece('X');
		
		turn = BLACK;
		isChildrenValid = false;
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public boolean hasNextHashInTier() {
		long currentHash = dbh.getHash();
		return (currentHash	!= numHashesForTier(getTier()) - 1);
	}

	@Override
	public void nextHashInTier() {
		if (this.hasNextHashInTier()) {
			TierState transition = new TierState();
			this.getState(transition);
			transition.hash++;
			this.setState(transition);
		} else
			throw new Error("End of Tier");
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
		if (!(isChildrenValid)) {
			children = getChildren();
			isChildrenValid = true;
		}
		System.arraycopy(children, 0, moves, 0, children.length);
		return children.length;
	}
	
	private TierState[] getChildren() {
		//not finished!
		return new TierState[0];
	}

	@Override
	public String describe() {
		return width + "x" + height + " Reversi";
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

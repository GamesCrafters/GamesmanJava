package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.CoefTable;
import edu.berkeley.gamesman.util.Pair;

public final class TicTacToe extends Game<TicTacToeState> {
	private final int width;
	private final int height;
	private final int boardSize;
	private final int piecesToWin;
	private final long[] hashOffsetTable;
	private final CoefTable ct = new CoefTable();

	public TicTacToe(Configuration conf) {
		super(conf);
		width = conf.getInteger("game.width", 3);
		height = conf.getInteger("game.height", 3);
		boardSize = width * height;
		piecesToWin = conf.getInteger("game.pieces", 3);
		hashOffsetTable = new long[boardSize + 2];
		long total = 0;
		for (int i = 0; i <= boardSize; i++) {
			hashOffsetTable[i] = total;
			total += ct.get(boardSize, i) * ct.get(i, i / 2);
		}
		hashOffsetTable[boardSize + 1] = total;
	}

	@Override
	public Collection<TicTacToeState> startingPositions() {
		ArrayList<TicTacToeState> returnList = new ArrayList<TicTacToeState>(1);
		TicTacToeState returnState = newState();
		returnList.add(returnState);
		return returnList;
	}

	@Override
	public Collection<Pair<String, TicTacToeState>> validMoves(
			TicTacToeState pos) {
		ArrayList<Pair<String, TicTacToeState>> moves = new ArrayList<Pair<String, TicTacToeState>>(
				boardSize - pos.numPieces);
		TicTacToeState[] children = new TicTacToeState[boardSize
				- pos.numPieces];
		String[] childNames = new String[children.length];
		for (int i = 0; i < children.length; i++) {
			children[i] = newState();
		}
		validMoves(pos, children);
		int moveCount = 0;
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (pos.getPiece(row, col) == ' ')
					childNames[moveCount++] = String
							.valueOf((char) ('A' + col))
							+ Integer.toString(row + 1);
			}
		}
		return moves;
	}

	@Override
	public int validMoves(TicTacToeState pos, TicTacToeState[] children) {
		int numMoves = 0;
		char turn = pos.numPieces % 2 == 0 ? 'X' : 'O';
		for (int i = 0; i < boardSize; i++) {
			if (pos.getPiece(i) == ' ') {
				children[numMoves].set(pos);
				children[numMoves].setPiece(i, turn);
				numMoves++;
			}
		}
		return numMoves;
	}

	@Override
	public int maxChildren() {
		return boardSize;
	}

	@Override
	public Value primitiveValue(TicTacToeState pos) {
		char lastTurn = pos.numPieces % 2 == 0 ? 'O' : 'X';
		for (int row = 0; row < height; row++) {
			int inRowFound = 0;
			for (int col = 0; col < width; col++) {
				if (pos.getPiece(row, col) == lastTurn) {
					inRowFound++;
					if (inRowFound == piecesToWin)
						return Value.LOSE;
				} else
					inRowFound = 0;
			}
		}
		for (int col = 0; col < width; col++) {
			int inColFound = 0;
			for (int row = 0; row < height; row++) {
				if (pos.getPiece(row, col) == lastTurn) {
					inColFound++;
					if (inColFound == piecesToWin)
						return Value.LOSE;
				} else
					inColFound = 0;
			}
		}

		for (int row = 0; row < height - piecesToWin; row++) {
			for (int col = 0; col < width - piecesToWin; col++) {
				int dif;
				for (dif = 0; dif < piecesToWin; dif++) {
					if (pos.getPiece(row + dif, col + dif) != lastTurn)
						break;
				}
				if (dif == piecesToWin)
					return Value.LOSE;
			}
			for (int col = piecesToWin; col < width; col++) {
				int dif;
				for (dif = 0; dif < piecesToWin; dif++) {
					if (pos.getPiece(row + dif, col - dif) != lastTurn)
						break;
				}
				if (dif == piecesToWin)
					return Value.LOSE;
			}
		}
		if (pos.numPieces == boardSize)
			return Value.TIE;
		else
			return Value.UNDECIDED;
	}

	@Override
	public long stateToHash(TicTacToeState pos) {
		long offset = hashOffsetTable[pos.numPieces];
		long multiplier = ct.get(pos.numPieces, pos.numPieces / 2);
		long majorHash = 0;
		long minorHash = 0;
		int piecesCounted = 0;
		int xsCounted = 0;
		for (int i = 0; i < boardSize; i++) {
			if (pos.getPiece(i) != ' ') {
				piecesCounted++;
				majorHash += ct.get(i, piecesCounted);
				if (pos.getPiece(i) == 'X') {
					xsCounted++;
					minorHash += ct.get(piecesCounted - 1, xsCounted);
				} else if (pos.getPiece(i) != 'O')
					throw new Error("Bad piece: " + pos.getPiece(i));
			}
		}
		return offset + majorHash * multiplier + minorHash;
	}

	@Override
	public String stateToString(TicTacToeState pos) {
		return pos.toString();
	}

	@Override
	public String displayState(TicTacToeState pos) {
		return pos.toString();
	}

	@Override
	public TicTacToeState stringToState(String pos) {
		return new TicTacToeState(width, pos.toCharArray());
	}

	@Override
	public String describe() {
		return width + "X" + height + " Tic Tac Toe with " + piecesToWin
				+ " pieces";
	}

	@Override
	public long numHashes() {
		return hashOffsetTable[boardSize + 1];
	}

	@Override
	public long recordStates() {
		return 12;
	}

	@Override
	public void hashToState(long hash, TicTacToeState s) {
		int numPieces = Arrays.binarySearch(hashOffsetTable, hash);
		if (numPieces < 0)
			numPieces = -numPieces - 2;
		hash -= hashOffsetTable[numPieces];
		long multiplier = ct.get(numPieces, numPieces / 2);
		long majorHash = hash / multiplier;
		long minorHash = hash % multiplier;
		int remainingPieces = numPieces;
		int remainingXs = (numPieces + 1) / 2;
		for (int i = boardSize - 1; i >= 0; i--) {
			long pieceHash = ct.get(i, remainingPieces);
			if (majorHash >= pieceHash) {
				majorHash -= pieceHash;
				remainingPieces--;
				long xHash = ct.get(remainingPieces, remainingXs);
				if (minorHash >= xHash) {
					minorHash -= xHash;
					remainingXs--;
					s.setPiece(i, 'X');
				} else
					s.setPiece(i, 'O');
			} else
				s.setPiece(i, ' ');
		}
	}

	@Override
	public TicTacToeState newState() {
		return new TicTacToeState(width, height);
	}

	@Override
	public void longToRecord(TicTacToeState recordState, long record,
			Record toStore) {
		if (record == 10) {
			toStore.value = Value.TIE;
			toStore.remoteness = boardSize - recordState.numPieces;
		} else if (record == 11) {
			toStore.value = Value.UNDECIDED;
		} else {
			toStore.value = record % 2 == 0 ? Value.LOSE : Value.WIN;
			toStore.remoteness = (int) record;
		}
	}

	@Override
	public long recordToLong(TicTacToeState recordState, Record fromRecord) {
		if (fromRecord.value == Value.TIE)
			return 10;
		else if (fromRecord.value == Value.UNDECIDED)
			return 11;
		else if (fromRecord.value == Value.LOSE
				|| fromRecord.value == Value.WIN)
			return fromRecord.remoteness;
		else
			throw new Error("Bad Value: " + fromRecord.value);
	}

}

class TicTacToeState implements State {
	private final char[] board;
	private final int width;
	int numPieces = 0;

	public TicTacToeState(int width, int height) {
		this.width = width;
		board = new char[width * height];
		for (int i = 0; i < board.length; i++) {
			board[i] = ' ';
		}
	}

	public TicTacToeState(int width, char[] charArray) {
		this.width = width;
		board = charArray;
	}

	public void set(State s) {
		TicTacToeState ttts = (TicTacToeState) s;
		if (board.length != ttts.board.length)
			throw new Error("Different Length Boards");
		int boardLength = board.length;
		System.arraycopy(ttts.board, 0, board, 0, boardLength);
		numPieces = ttts.numPieces;
	}

	public void setPiece(int row, int col, char piece) {
		setPiece(row * width + col, piece);
	}

	public void setPiece(int index, char piece) {
		if (board[index] != ' ')
			numPieces--;
		board[index] = piece;
		if (piece == 'X' || piece == 'O') {
			numPieces++;
		} else if (piece != ' ')
			throw new Error("Invalid piece: " + piece);
	}

	public char getPiece(int row, int col) {
		return getPiece(row * width + col);
	}

	public char getPiece(int index) {
		return board[index];
	}

	public String toString() {
		return Arrays.toString(board);
	}
}
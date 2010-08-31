package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class TicTacToe extends Game<TTTBoard> {
	public final long[] offsetTable = new long[11];

	public TicTacToe(Configuration conf) {
		super(conf);
		long total = 0L;
		offsetTable[0] = 0L;
		for (int i = 1; i <= 10; i++) {
			offsetTable[i] = total;
			total += Util.nCr(9, i) * Util.nCr(i, i / 2);
		}
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String displayState(TTTBoard pos) {
		StringBuilder sb = new StringBuilder(12);
		for (int row = 2; row >= 0; row--) {
			for (int col = 0; col < 3; col++) {
				sb.append(pos.get(row, col));
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	public void hashToState(long hash, TTTBoard s) {
		s.piecesCount = Arrays.binarySearch(offsetTable, hash);
		if (s.piecesCount < 0) {
			s.piecesCount = -s.piecesCount - 2;
		}
		hash -= offsetTable[s.piecesCount];
		int os = s.piecesCount / 2;
		long divider = Util.nCr(s.piecesCount, os);
		long largeHash = hash / divider;
		long smallHash = hash % divider;
		int k = s.piecesCount;
		int r = os;
		for (int n = 8; n >= 0; n--) {
			long large = Util.nCr(n, k);
			if (largeHash >= large && k > 0) {
				largeHash -= large;
				k--;
				long small = Util.nCr(k, r);
				if (smallHash >= small && r > 0) {
					s.board[n] = 'O';
					smallHash -= small;
					r--;
				} else {
					s.board[n] = 'X';
				}
			} else {
				s.board[n] = ' ';
			}
		}
	}

	@Override
	public int maxChildren() {
		return 9;
	}

	@Override
	public TTTBoard newState() {
		return new TTTBoard();
	}

	@Override
	public long numHashes() {
		return offsetTable[10];
	}

	@Override
	public Value primitiveValue(TTTBoard pos) {
		for (int row = 0; row < 3; row++) {
			if (pos.get(row, 0) != ' ' && pos.get(row, 0) == pos.get(row, 1)
					&& pos.get(row, 1) == pos.get(row, 2))
				return Value.LOSE;
		}
		for (int col = 0; col < 3; col++) {
			if (pos.get(0, col) != ' ' && pos.get(0, col) == pos.get(1, col)
					&& pos.get(1, col) == pos.get(2, col))
				return Value.LOSE;
		}
		if (pos.get(1, 1) != ' '
				&& (pos.get(0, 0) == pos.get(1, 1)
						&& pos.get(1, 1) == pos.get(2, 2) || pos.get(0, 2) == pos
						.get(1, 1)
						&& pos.get(1, 1) == pos.get(2, 0)))
			return Value.LOSE;
		if (pos.piecesCount == 9)
			return Value.TIE;
		else
			return Value.UNDECIDED;
	}

	@Override
	public Collection<TTTBoard> startingPositions() {
		ArrayList<TTTBoard> tBoard = new ArrayList<TTTBoard>(1);
		tBoard.add(new TTTBoard(true));
		return tBoard;
	}

	@Override
	public long stateToHash(TTTBoard pos) {
		long largeHash = 0L;
		long smallHash = 0L;
		int k = 0;
		int r = 0;
		for (int n = 0; n < 9; n++) {
			if (pos.board[n] != ' ') {
				if (pos.board[n] == 'O') {
					r++;
					smallHash += Util.nCr(k, r);
				}
				k++;
				largeHash += Util.nCr(n, k);
			}
		}
		return offsetTable[pos.piecesCount] + largeHash
				* Util.nCr(pos.piecesCount, pos.piecesCount / 2) + smallHash;
	}

	@Override
	public String stateToString(TTTBoard pos) {
		return new String(pos.board);
	}

	@Override
	public TTTBoard stringToState(String pos) {
		TTTBoard s = new TTTBoard(pos);
		return s;
	}

	@Override
	public Collection<Pair<String, TTTBoard>> validMoves(TTTBoard pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int validMoves(TTTBoard pos, TTTBoard[] children) {
		int moveCount = 0;
		char turn = (pos.piecesCount & 1) == 0 ? 'X' : 'O';
		for (int i = 0; i < 9; i++) {
			if (pos.board[i] == ' ') {
				children[moveCount].set(pos);
				children[moveCount].piecesCount++;
				children[moveCount++].board[i] = turn;
			}
		}
		return moveCount;
	}

	@Override
	public void recordFromLong(TTTBoard recordState, long record, Record toStore) {
		if (record == 11) {
			toStore.value = Value.UNDECIDED;
		} else if (record == 10) {
			toStore.remoteness = 9 - recordState.piecesCount;
			toStore.value = Value.TIE;
		} else {
			toStore.remoteness = (int) record;
			toStore.value = (record & 1) == 1 ? Value.WIN : Value.LOSE;
		}
	}

	@Override
	public long getRecord(TTTBoard recordState, Record r) {
		if (r.value == Value.UNDECIDED)
			return 11L;
		else if (r.value == Value.TIE)
			return 10L;
		else
			return r.remoteness;
	}

	@Override
	public long recordStates() {
		return 12;
	}
}

class TTTBoard implements State {
	final char[] board;
	int piecesCount;

	public TTTBoard() {
		this(false);
	}

	public TTTBoard(boolean initialPosition) {
		board = new char[9];
		if (initialPosition) {
			piecesCount = 0;
			for (int i = 0; i < 9; i++) {
				board[i] = ' ';
			}
		}
	}

	public TTTBoard(String pos) {
		board = pos.toCharArray();
		piecesCount = 0;
		for (int i = 0; i < 9; i++) {
			if (board[i] != ' ')
				piecesCount++;
		}
	}

	public void set(State s) {
		TTTBoard ts = (TTTBoard) s;
		System.arraycopy(ts.board, 0, board, 0, 9);
		piecesCount = ts.piecesCount;
	}

	public char get(int row, int col) {
		return board[row * 3 + col];
	}

}

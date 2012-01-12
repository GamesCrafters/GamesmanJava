package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.hasher.RestrictionlessHasher;
import edu.berkeley.gamesman.hasher.cachehasher.CacheHasher;
import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.util.Pair;

public class VIQuickCross extends VIGame {

	private final RestrictionlessHasher myHasher;
	private final CacheHasher<GenState> myCacher;
	private final int width, height, boardSize, piecesInARow;
	private final BitSetBoard myBoard;

	public VIQuickCross(Configuration conf) {
		super(conf);
		width = conf.getInteger("gamesman.game.width", 4);
		height = conf.getInteger("gamesman.game.height", 4);
		piecesInARow = conf.getInteger("gamesman.game.pieces", 4);
		boardSize = width * height;
		this.myHasher = new RestrictionlessHasher(makeArr());
		CacheMove[] allMoves = new CacheMove[boardSize * 4];
		for (int i = 0; i < boardSize; i++) {
			allMoves[4 * i] = new CacheMove(i, 0, 1);
			allMoves[4 * i + 1] = new CacheMove(i, 0, 2);
			allMoves[4 * i + 2] = new CacheMove(i, 1, 2);
			allMoves[4 * i + 3] = new CacheMove(i, 2, 1);
		}
		this.myCacher = new CacheHasher<GenState>(myHasher, myHasher, allMoves,
				true);
		myBoard = new BitSetBoard(height, width);
	}

	private int[] makeArr() {
		int[] board = new int[boardSize];
		Arrays.fill(board, 3);
		return board;
	}

	@Override
	public String displayState() {
		return myBoard.toString();
	}

	@Override
	public void setToHash(long hash) {
		myCacher.unhash(hash);
		setBoard();
	}

	private void setBoard() {
		myBoard.clear();
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				switch (get(row, col)) {
				case 1:
					myBoard.addPiece(row, col, 'X');
					break;
				case 2:
					myBoard.addPiece(row, col, 'O');
					break;
				}
			}
		}
	}

	private int get(int row, int col) {
		return myCacher.get(getIndex(row, col));
	}

	private int getIndex(int row, int col) {
		return row * width + col;
	}

	@Override
	public Value primitiveValue() {
		int xInALine = myBoard.xInALine(piecesInARow, 'X');
		assert xInALine == 0 || xInALine == 1;
		int oInALine = myBoard.xInALine(piecesInARow, 'O');
		assert oInALine == 0 || oInALine == 1;
		return (xInALine | oInALine) == 1 ? Value.LOSE : Value.UNDECIDED;
	}

	@Override
	public long getHash() {
		return myCacher.getHash();
	}

	@Override
	public void setFromString(String pos) {
		char[] pieces = pos.toCharArray();
		assert pieces.length == boardSize;
		int[] seq = new int[boardSize];
		for (int i = 0; i < boardSize; i++) {
			seq[i] = getPiece(pieces[i]);
		}
		myCacher.set(seq);
	}

	private static int getPiece(char c) {
		switch (c) {
		case ' ':
			return 0;
		case 'X':
			return 1;
		case 'O':
			return 2;
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public void setStartingPosition(int i) {
		hashToState(0);
	}

	@Override
	public void longToRecord(long record, Record toStore) {
		if (record == boardSize + 1)
			toStore.value = Value.DRAW;
		else {
			toStore.remoteness = (int) record;
			toStore.value = record % 2 == 1 ? Value.WIN : Value.LOSE;
		}
	}

	@Override
	public long recordToLong(Record fromRecord) {
		if (fromRecord.value == Value.DRAW)
			return boardSize + 1;
		else
			return fromRecord.remoteness;
	}

	@Override
	public int maxChildren() {
		return boardSize * 2;
	}

	@Override
	public String describe() {
		return "Quick Cross " + width + "x" + height + ": " + piecesInARow
				+ " in a row";
	}

	@Override
	public long numHashes() {
		return myHasher.totalPositions();
	}

	@Override
	public long recordStates() {
		return boardSize + 2;
	}

	@Override
	public int validMoves(long[] childHashes) {
		return myCacher.getChildren(null, childHashes);
	}

	@Override
	public Collection<Pair<String, HashState>> validMoves() {
		ArrayList<Pair<String, HashState>> moveList = new ArrayList<Pair<String, HashState>>();
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				switch (get(row, col)) {
				case 0:
					moveList.add(new Pair<String, HashState>("X"
							+ Character.toString((char) (col + 'a'))
							+ (row + 1), newState(myCacher
							.getChild(4 * getIndex(row, col)))));
					moveList.add(new Pair<String, HashState>("O"
							+ Character.toString((char) (col + 'a'))
							+ (row + 1), newState(myCacher
							.getChild(4 * getIndex(row, col) + 1))));
					break;
				case 1:
					moveList.add(new Pair<String, HashState>("F"
							+ Character.toString((char) (col + 'a'))
							+ (row + 1), newState(myCacher
							.getChild(4 * getIndex(row, col) + 2))));
					break;
				case 2:
					moveList.add(new Pair<String, HashState>("F"
							+ Character.toString((char) (col + 'a'))
							+ (row + 1), newState(myCacher
							.getChild(4 * getIndex(row, col) + 3))));
					break;
				default:
					throw new Error("Not a valid piece: " + get(row, col));
				}
			}
		}
		return moveList;
	}

	@Override
	public boolean next() {
		int lastChanged = myCacher.next();
		if (lastChanged == -1)
			return false;
		int i = 0;
		OUTER: for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (i > lastChanged)
					break OUTER;
				int piece = myCacher.get(i);
				myBoard.setPiece(row, col, getChar(piece));
				i++;
			}
		}
		return true;
	}

	private static char getChar(int i) {
		switch (i) {
		case 0:
			return ' ';
		case 1:
			return 'X';
		case 2:
			return 'O';
		default:
			throw new UnsupportedOperationException();
		}
	}
}

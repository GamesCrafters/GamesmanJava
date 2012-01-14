package edu.berkeley.gamesman.parallel.game.connect4;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class C4State extends CountingState {
	private final int width, height, boardSize;
	private final BitSetBoard myBoard;

	public C4State(GenHasher<? extends CountingState> myHasher, int width,
			int height) {
		super(myHasher, width * height);
		this.width = width;
		this.height = height;
		this.boardSize = width * height;
		this.myBoard = new BitSetBoard(height, width);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(width * height);
		for (int row = height - 1; row >= 0; row--) {
			sb.append('|');
			for (int col = 0; col < width; col++) {
				sb.append(getChar(row, col));
				sb.append('|');
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	private char getChar(int row, int col) {
		int place = col * height + row;
		return place < getStart() ? '*' : Connect4.charFor(get(place));
	}

	@Override
	protected void addLS(int ls) {
		super.addLS(ls);
		int start = getStart();
		setPiece(start, ls);
	}

	private void setPiece(int place, int val) {
		if (place < boardSize) {
			int row = place % height, col = place / height;
			myBoard.setPiece(row, col, Connect4.charFor(val));
		}
	}

	@Override
	protected boolean incr(int dir) {
		if (super.incr(dir)) {
			setLS();
			return true;
		} else
			return false;
	}

	@Override
	protected void matchSeq() {
		super.matchSeq();
		matchAll();
	}

	@Override
	protected void set(int place, int val) {
		super.set(place, val);
		setPiece(place, val);
	}

	private void matchAll() {
		for (int i = getStart(); i < boardSize; i++)
			setPiece(i);
	}

	private void setLS() {
		setPiece(getStart());
	}

	private void setPiece(int place) {
		setPiece(place, get(place));
	}

	public GameValue getValue(int inALine, int lastTurn) {
		return myBoard.xInALine(inALine, Connect4.charFor(lastTurn)) != 0 ? GameValue.LOSE
				: (numPieces() == boardSize ? GameValue.TIE : null);
	}

	private int numPieces() {
		return get(boardSize);
	}
}

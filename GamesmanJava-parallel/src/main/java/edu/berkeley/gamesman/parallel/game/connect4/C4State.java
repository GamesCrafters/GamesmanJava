package edu.berkeley.gamesman.parallel.game.connect4;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.hasher.counting.CountingState;

/**
 * Holds the game as a list of pieces in col-major order. The highest-index
 * element in the array is the number of pieces on the board. This extra bit of
 * information ensures that tiers are grouped together. The length of a position
 * array is gameSize+1 (+1 for that extra value)
 * 
 * @author dnspies
 * 
 */
public class C4State extends CountingState {
	private final int width, height, boardSize;
	private final BitSetBoard myBoard;
	private int changePlace;

	public C4State(C4Hasher myHasher, int width, int height) {
		super(myHasher, width * height);
		this.width = width;
		this.height = height;
		this.boardSize = width * height;
		changePlace = boardSize - 1;
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
		return place < getStart() ? '*' : getChar(place);
	}

	private char getChar(int place) {
		return Connect4.charFor(get(place));
	}

	@Override
	protected void addLS(int ls) {
		super.addLS(ls);
		upChange(getStart());
	}

	/**
	 * Indicates that the highest-index changed element is at least place. Next
	 * time getValue is called, it will modify the BitSetBoard to match all the
	 * pieces up to changePlace.
	 * 
	 * @param place
	 */
	private void upChange(int place) {
		changePlace = Math.max(changePlace, place);
	}

	@Override
	protected boolean incr(int dir) {
		if (super.incr(dir)) {
			upChange(getStart());
			return true;
		} else
			return false;
	}

	@Override
	protected void matchSeq() {
		super.matchSeq();
		changePlace = boardSize - 1;
	}

	@Override
	protected void set(int place, int val) {
		super.set(place, val);
		upChange(place);
	}

	/**
	 * Returns the primitive value of a position given who played last together
	 * with the number of pieces needed to get connect 4.
	 * 
	 * @param inALine
	 *            The number of pieces you need to get
	 * @param lastTurn
	 *            Who played last
	 * @return
	 */
	public GameValue getValue(int inALine, int lastTurn) {
		assert isComplete();
		int col = 0;
		int boardCounter = 0;
		int stopAt = Math.min(changePlace, boardSize - 1);
		for (int i = 0; i <= stopAt; i++) {
			if (col == height) {
				col = 0;
				boardCounter++;
			}
			myBoard.setPiece(boardCounter, getChar(i));
			boardCounter++;
			col++;
		}
		changePlace = 0;
		return myBoard.xInALine(inALine, Connect4.charFor(lastTurn)) ? GameValue.LOSE
				: (numPieces() == boardSize ? GameValue.TIE : null);
	}

	private int numPieces() {
		return get(boardSize);
	}
}

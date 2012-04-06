package edu.berkeley.gamesman.hasher.genhasher;

import edu.berkeley.gamesman.hasher.counting.CountingState;

/**
 * This class takes a "dartboard" state (ie pieces followed by -XO board) and
 * returns a single unique number corresponding to pieces together with the
 * number of each piece on the board from 0 to 1<<24 - 1. Useful for an
 * invariant hasher for a dartboard type game
 * 
 * @author dnspies
 * 
 */
public final class DBInvCalculator {
	private final int boardSize;

	/**
	 * @param boardSize
	 *            The board size
	 */
	public DBInvCalculator(int boardSize) {
		this.boardSize = boardSize;
	}

	public long getInv(CountingState s) {
		return getInv(s.get(boardSize), s);
	}

	/**
	 * @param s
	 *            The state to find invariant for
	 * @return A single value from 0 to 1<<24 - 1
	 */
	public long getInv(long first48, CountingState s) {
		assert (first48 >>> 48) == 0;
		if (s.isEmpty())
			return 0;
		else
			return (first48 << 16L) | getPieceInv(s);
	}

	long getPieceInv(CountingState s) {
		return (s.numPieces(1) << 8L) | s.numPieces(2);
	}
}

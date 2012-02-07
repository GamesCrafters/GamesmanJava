package edu.berkeley.gamesman.hasher.genhasher;

import edu.berkeley.gamesman.hasher.counting.CountingState;

public class DBInvCalculator {
	private final int boardSize;

	public DBInvCalculator(int boardSize) {
		this.boardSize = boardSize;
	}

	public long getInv(CountingState s) {
		if (s.isEmpty())
			return 0;
		else
			return (((s.get(boardSize) << 8L) | s.numPieces(1)) << 8L)
					| s.numPieces(2);
	}
}

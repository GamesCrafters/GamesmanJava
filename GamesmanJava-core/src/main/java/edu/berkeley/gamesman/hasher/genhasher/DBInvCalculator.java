package edu.berkeley.gamesman.hasher.genhasher;

import edu.berkeley.gamesman.hasher.counting.CountingState;

public class DBInvCalculator {
	private final int boardSize;
	private final int firstMult;
	private final int secondMult;
	public final int numValues;

	public DBInvCalculator(int boardSize) {
		this.boardSize = boardSize;
		this.firstMult = boardSize + 1;
		this.secondMult = firstMult * firstMult;
		this.numValues = secondMult * firstMult;
	}

	public long getInv(CountingState s) {
		if (s.isEmpty())
			return 0;
		else
			return s.get(boardSize) * secondMult + s.numPieces(1) * firstMult
					+ s.numPieces(2);
	}
}

package edu.berkeley.gamesman.hasher;

import java.util.Arrays;

import edu.berkeley.gamesman.hasher.invhasher.OptimizingInvariantHasher;

/**
 * @author dnspies
 * 
 */
public final class DBHasher extends OptimizingInvariantHasher<DBState> {
	private final int boardSize;
	private final int numInvariants;

	/**
	 * @param numElements
	 * @param digitBase
	 * @param numPieces
	 */
	public DBHasher(int boardSize) {
		super(makeParams(boardSize));
		this.boardSize = boardSize;
		numInvariants = boardSize * boardSize;
	}

	private static int[] makeParams(int boardSize) {
		int[] result = new int[boardSize + 1];
		Arrays.fill(result, 3);
		result[boardSize] = boardSize + 1;
		return result;
	}

	@Override
	protected DBState innerNewState() {
		return new DBState(this, boardSize);
	}

	@Override
	protected int getInvariant(DBState state) {
		return state.numPieces(1) * boardSize + state.numPieces(2);
	}

	@Override
	protected boolean valid(DBState state) {
		int num1 = state.numPieces(1);
		int num2 = state.numPieces(2);
		return num1 + num2 == state.get(boardSize)
				&& (num1 - num2 == 1 || num1 - num2 == 0);
	}

	@Override
	protected int numInvariants(int startPoint) {
		return numInvariants;
	}
}

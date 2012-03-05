package edu.berkeley.gamesman.hasher;

import java.util.Arrays;

import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.DBInvCalculator;
import edu.berkeley.gamesman.hasher.invhasher.OptimizingInvariantHasher;

/**
 * @author dnspies
 * 
 */
public final class DBHasher extends OptimizingInvariantHasher<CountingState> {
	private final int boardSize;
	private final DBInvCalculator calc;

	/**
	 * @param numElements
	 * @param digitBase
	 * @param numPieces
	 */
	public DBHasher(int boardSize) {
		super(makeParams(boardSize));
		this.boardSize = boardSize;
		calc = new DBInvCalculator(boardSize);
	}

	private static int[] makeParams(int boardSize) {
		int[] result = new int[boardSize + 1];
		Arrays.fill(result, 3);
		result[boardSize] = boardSize + 1;
		return result;
	}

	@Override
	protected CountingState genHasherNewState() {
		return new CountingState(this, boardSize);
	}

	@Override
	protected long getInvariant(CountingState state) {
		return calc.getInv(state);
	}

	@Override
	protected boolean valid(CountingState state) {
		return dbValid(state, boardSize);
	}

	public static boolean dbValid(CountingState state, int boardSize) {
		int num1 = state.numPieces(1);
		int num2 = state.numPieces(2);
		return num1 + num2 == state.get(boardSize)
				&& (num1 - num2 == 1 || num1 - num2 == 0);
	}
}

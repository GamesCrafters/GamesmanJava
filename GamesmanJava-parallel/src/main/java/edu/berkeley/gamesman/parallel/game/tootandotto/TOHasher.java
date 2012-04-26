package edu.berkeley.gamesman.parallel.game.tootandotto;

import java.util.Arrays;

import edu.berkeley.gamesman.hasher.DBHasher;
import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.DBInvCalculator;
import edu.berkeley.gamesman.hasher.invhasher.OptimizingInvariantHasher;

public class TOHasher extends OptimizingInvariantHasher<CountingState> {
	private final int width, height;
	public final int boardSize; // TODO: why not private?
	private final DBInvCalculator calc;

	public TOHasher(int width, int height, int maxPieces) {
		this(width, height, maxPieces, 0);
	}

	/**
	 * @param width
	 *            The width of the board
	 * @param height
	 *            The height of the board
	 * @param countingPlace
	 *            The size of the ranges which we wish to count. <-- Should only
	 *            be used for evaluating the average number of states per range
	 *            per state
	 */
	public TOHasher(int width, int height, int maxPieces, int countingPlace) {
		super(makeDigitBase(width, height, maxPieces), countingPlace);
		this.width = width;
		this.height = height;
		boardSize = width * height;
		calc = new DBInvCalculator(boardSize);
	}

	/**
	 * Creates the array specifying the number of possible values for each
	 * position in the sequence
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	private static int[] makeDigitBase(int width, int height, int maxPieces) {
		int boardSize = width * height;
		int[] digitBase = new int[boardSize + 5];
		Arrays.fill(digitBase, 3);
		Arrays.fill(digitBase, boardSize - 4, boardSize - 1, maxPieces + 1);
		digitBase[boardSize] = boardSize + 1;
		return digitBase;
	}

	@Override
	protected long getInvariant(CountingState state) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected boolean valid(CountingState state) {
		return getInvariant(state) >= 0 && DBHasher.dbValid(state, boardSize);
	}

	@Override
	protected CountingState genHasherNewState() {
		return new CountingState(this, boardSize);
	}

}

package edu.berkeley.gamesman.parallel.game.tootandotto;

import java.util.Arrays;

import edu.berkeley.gamesman.hasher.DBHasher;
import edu.berkeley.gamesman.hasher.genhasher.GravityHashUtil;
import edu.berkeley.gamesman.hasher.invhasher.OptimizingInvariantHasher;

public class TOHasher extends OptimizingInvariantHasher<TootNOttoState> {
	final int width, height;
	public final int boardSize; // TODO: why not private?
	private final GravityHashUtil<TootNOttoState> myUtil;

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
		myUtil = new GravityHashUtil<TootNOttoState>(width, height);
	}

	/**
	 * Creates the array specifying the number of possible values for each
	 * position in the sequence
	 * 
	 * @param width
	 * @param height
	 * @param maxPieces
	 *            the max Ts and Os each can have
	 * @return
	 */
	private static int[] makeDigitBase(int width, int height, int maxPieces) {
		int boardSize = width * height;
		int[] digitBase = new int[boardSize + 5];
		Arrays.fill(digitBase, 3);
		Arrays.fill(digitBase, boardSize, boardSize + 3, maxPieces);
		digitBase[boardSize + 4] = boardSize + 1;
		return digitBase;
	}

	@Override
	protected long getInvariant(TootNOttoState state) {
		// TODO Change 0 to a unique value based on the highest four entries
		return isEmpty(state) ? 0 : myUtil.getInv(this, 0L, state);
	}

	@Override
	protected boolean valid(TootNOttoState state) {
		return getInvariant(state) >= 0 && DBHasher.dbValid(state, boardSize);
	}

	@Override
	protected TootNOttoState genHasherNewState() {
		return new TootNOttoState(this, boardSize);
	}

}

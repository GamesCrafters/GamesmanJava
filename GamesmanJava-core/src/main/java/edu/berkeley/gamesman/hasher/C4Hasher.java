package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.hasher.fixed.FixedHasher;
import edu.berkeley.gamesman.hasher.fixed.FixedState;

/**
 * @author dnspies
 * 
 */
public final class C4Hasher extends FixedHasher<FixedState> {

	public final int gameSize;
	public final int height;
	private final int addOn, numInvariants;

	/**
	 * @param width
	 * @param height
	 * @param tier
	 */
	public C4Hasher(int width, int height, int tier) {
		super(width * height, 3, new int[] { width * height - tier,
				(tier + 1) / 2, tier / 2 });
		this.height = height;
		this.gameSize = width * height;
		addOn = super.numInvariants;
		numInvariants = addOn + super.numInvariants;
	}

	@Override
	protected FixedState innerNewState() {
		return new FixedState(this, gameSize);
	}

	@Override
	protected boolean totalValid(FixedState state) {
		if (!valid(state))
			return false;
		for (int i = 0; i < numElements - 1; i++) {
			if (!validStart(state, i))
				return false;
		}
		return true;
	}

	@Override
	protected boolean valid(FixedState state) {
		assert isComplete(state);
		return validStart(state) && baseValid(state);
	}

	@Override
	protected int getInvariant(FixedState state) {
		if (!validStart(state)) {
			return -1;
		}
		int bi = baseGetInvariant(state);
		if (bi == -1)
			return -1;
		return bi + (!isEmpty(state) && leastSig(state) != 0 ? addOn : 0);
	}

	private boolean validStart(FixedState state) {
		return validStart(state, getStart(state));
	}

	private boolean validStart(FixedState state, int start) {
		return start == numElements || state.get(start) != 0 || isTop(start)
				|| state.get(start + 1) == 0;
	}

	private boolean isTop(int n) {
		return n % height == height - 1;
	}

	@Override
	protected int lastInvariant(FixedState state) {
		int bi = baseLastInvariant(state);
		assert bi >= 0;
		return bi
				+ (getStart(state) < numElements - 1
						&& state.get(getStart(state) + 1) != 0 ? addOn : 0);
	}

	@Override
	protected int numInvariants(int startPoint) {
		return numInvariants;
	}

	/**
	 * @param row
	 * @param col
	 * @return
	 */
	public int indexOf(int row, int col) {
		return col * height + row;
	}
}

package edu.berkeley.gamesman.hasher.genhasher;

import edu.berkeley.gamesman.hasher.counting.CountingState;

public class GravityHashUtil<T extends CountingState> {
	private final int height;
	private final int boardSize;
	private final DBInvCalculator calc;

	public GravityHashUtil(int width, int height) {
		this.height = height;
		boardSize = width * height;
		calc = new DBInvCalculator(boardSize);
	}

	public long getInv(GenHasher<T> hasher, T state) {
		return getInv(hasher, state.get(boardSize), state);
	}

	public long getInv(GenHasher<T> hasher, long first46, T state) {
		assert (first46 >>> 46) == 0;
		int start = hasher.getStart(state);
		if (start == boardSize + 1)
			return 0;
		else if (start == boardSize)
			return state.get(boardSize);
		boolean startEmpty = hasher.leastSig(state) == 0;
		if (!isTop(start) && startEmpty && state.get(start + 1) != 0)
			return -1;
		else {
			return calc.getInv(first46 << 1, state)
					| (startEmpty ? 0 : 1 << 16);
		}
	}

	private boolean isTop(int place) {
		return (place + 1) % height == 0;
	}
}

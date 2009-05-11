package edu.berkeley.gamesman.core;

import edu.berkeley.gamesman.util.Pair;

/**
 * All Itergames use this as the state.
 * @author DNSpies
 */
public final class ItergameState extends Pair<Integer, Long> {

	private static final long serialVersionUID = -2453498572553759443L;

	/**
	 * @param tier The tier
	 * @param hashVal The hash
	 */
	public ItergameState(int tier, long hashVal) {
		super(tier, hashVal);
	}

	/**
	 * @return The tier
	 */
	public int tier() {
		return car;
	}

	/**
	 * @return The hash
	 */
	public long hash() {
		return cdr;
	}

}

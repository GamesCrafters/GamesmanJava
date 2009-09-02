package edu.berkeley.gamesman.core;

import edu.berkeley.gamesman.util.MutablePair;

/**
 * All Itergames use this as the state.
 * 
 * @author DNSpies
 */
public final class ItergameState extends MutablePair<Integer, Long> {

	private static final long serialVersionUID = -2453498572553759443L;

	/**
	 * @param tier
	 *            The tier
	 * @param hashVal
	 *            The hash
	 */
	public ItergameState(int tier, long hashVal) {
		super(tier, hashVal);
	}

	/**
	 * Creates an empty state
	 */
	public ItergameState() {
		this(0, 0);
	}

	public void setTier(int tier) {
		car = tier;
	}

	/**
	 * @return The tier
	 */
	public int getTier() {
		return car;
	}

	public void setHash(long hash) {
		cdr = hash;
	}

	/**
	 * @return The hash
	 */
	public long getHash() {
		return cdr;
	}

}

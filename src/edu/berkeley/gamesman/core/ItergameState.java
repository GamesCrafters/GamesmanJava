package edu.berkeley.gamesman.core;

/**
 * All Itergames use this as the state.
 * 
 * @author DNSpies
 */
public final class ItergameState {

	/**
	 * The tier
	 */
	public int tier;

	/**
	 * The hash
	 */
	public long hash;

	/**
	 * @param tier
	 *            The tier
	 * @param hash
	 *            The hash
	 */
	public ItergameState(int tier, long hash) {
		this.tier = tier;
		this.hash = hash;
	}

	/**
	 * Creates an empty state
	 */
	public ItergameState() {
		this(0, 0);
	}
}

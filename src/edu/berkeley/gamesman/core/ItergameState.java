package edu.berkeley.gamesman.core;

/**
 * All Itergames use this as the state.
 * 
 * @author DNSpies
 */
public final class ItergameState implements State {

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

	@Override
	public String toString() {
		return tier + "." + hash;
	}

	public void set(State s) {
		if (s instanceof ItergameState) {
			ItergameState is = (ItergameState) s;
			tier = is.tier;
			hash = is.hash;
		} else
			throw new RuntimeException("Wrong State passed");
	}
}

package edu.berkeley.gamesman.game.util;

import edu.berkeley.gamesman.core.State;

/**
 * All Itergames use this as the state.
 * 
 * @author DNSpies
 */
public final class ItergameState implements State, Cloneable {

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
	 * Creates a new state exactly like state
	 * 
	 * @param state
	 *            Another state to copy
	 */
	public ItergameState(ItergameState state) {
		set(state);
	}

	/**
	 * Creates a new emptyState
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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ItergameState) {
			ItergameState other = (ItergameState) obj;
			return tier == other.tier && hash == other.hash;
		} else
			return false;
	}

	public ItergameState clone() {
		return new ItergameState(this);
	}
}

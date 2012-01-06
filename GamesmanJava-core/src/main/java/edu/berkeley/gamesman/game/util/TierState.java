package edu.berkeley.gamesman.game.util;

import edu.berkeley.gamesman.core.State;

/**
 * All Itergames use this as the state.
 * 
 * @author DNSpies
 */
public final class TierState implements State<TierState>, Cloneable {

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
	public TierState(int tier, long hash) {
		this.tier = tier;
		this.hash = hash;
	}

	/**
	 * Creates a new state exactly like state
	 * 
	 * @param state
	 *            Another state to copy
	 */
	public TierState(TierState state) {
		set(state);
	}

	/**
	 * Creates a new emptyState
	 */
	public TierState() {
		this(0, 0);
	}

	@Override
	public String toString() {
		return tier + "." + hash;
	}

	@Override
	public void set(TierState is) {
		tier = is.tier;
		hash = is.hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TierState) {
			TierState other = (TierState) obj;
			return tier == other.tier && hash == other.hash;
		} else
			return false;
	}

	public TierState clone() {
		return new TierState(this);
	}
}

package edu.berkeley.gamesman.core;

import java.util.Arrays;

import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A TieredHasher is a specialized Hasher that can optimize knowing that a Game
 * can be broken up into discrete Tiers. See TieredGame for a more complete
 * overview of Tiered games.
 * 
 * @author Steven Schlansker
 * @see TieredGame
 * @param <State>
 *            The class that represents a State
 */
public abstract class TieredHasher<State> extends Hasher<State> {
	/**
	 * Default constructor
	 * 
	 * @param conf
	 *            the configuration
	 */
	public TieredHasher(Configuration conf) {
		super(conf);
	}

	@Override
	public long hash(State board) {
		Util.fatalError("Not implemented"); // TODO
		return 0;
	}

	@Override
	public long maxHash() {
		Util.fatalError("Not implemented"); // TODO
		return 0;
	}

	@Override
	public State unhash(long hash) {
		Util.fatalError("Not implemented"); // TODO
		return null;
	}

	/**
	 * Indicate the number of tiers that this game has (Note how this is
	 * distinct from the /last/ tier)
	 * 
	 * @return Number of tiers. Limited to java primitive int type for now
	 */
	public abstract int numberOfTiers();

	long tierEnds[]; // For tier i, tierEnds[i] is the /last/ hash value for

	// that tier.

	int cacheNumTiers = -1;

	/**
	 * Return the first hashed value in a given tier
	 * 
	 * @param tier
	 *            The tier we're interested in
	 * @return The first hash value that will be in this tier
	 * @see #tierIndexForState
	 */
	public final long hashOffsetForTier(int tier) {
		if (tier == 0)
			return 0;
		if (tierEnds == null)
			lastHashValueForTier(numberOfTiers() - 1);
		return tierEnds[tier - 1] + 1;
	}

	/**
	 * Return the last hash value a tier represents
	 * 
	 * @param tier
	 *            The tier we're interested in
	 * @return The last hash that will be in the given tier
	 */
	public final long lastHashValueForTier(int tier) {
		if (tierEnds == null) {
			tierEnds = new long[numberOfTiers()];
			for (int i = 0; i < tierEnds.length; i++) {
				tierEnds[i] = hashOffsetForTier(i) + numHashesForTier(i) - 1;
			}
			assert Util.debug(DebugFacility.HASHER, "Created offset table: "
					+ Arrays.toString(tierEnds));
		}
		return tierEnds[tier];
	}

	/**
	 * Return the number of hashes in a tier
	 * 
	 * @param tier
	 *            The tier we're interested in
	 * @return Size of the tier
	 */
	public abstract long numHashesForTier(int tier);

	/**
	 * Convert a tier/index pair into a State Analogous to unhash for a Tiered
	 * game
	 * 
	 * @param tier
	 *            The tier we're in
	 * @param index
	 *            The hash offset into that tier (relative to the start - 0 is
	 *            the first board in the tier)
	 * @return The represented GameState
	 */
	public abstract State gameStateForTierAndOffset(int tier, long index);

	/**
	 * Convert a State into a tier/index pair Analogous to hash for a Tiered
	 * game
	 * 
	 * @param state
	 *            The game state we have
	 * @return a Pair with tier/offset enclosed
	 */
	public abstract Pair<Integer, Long> tierIndexForState(State state);
}

package edu.berkeley.gamesman.hasher;

import java.util.Arrays;

import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies Hasher for Tiered Cycle Games
 */
public final class TierHasher {
	private long tierOffsets[] = null;

	private final TierGame myGame;

	public TierHasher(TierGame g) {
		myGame = g;
	}

	/**
	 * Return the first hashed value in a given tier
	 * 
	 * @param tier
	 *            The tier we're interested in
	 * @return The first hash value that will be in this tier
	 * @see #tierIndexForState
	 */
	public final long hashOffsetForTier(int tier) {
		if (tierOffsets == null) {
			int tiers = myGame.numberOfTiers();
			tierOffsets = new long[tiers + 1];
			tierOffsets[0] = 0;
			for (int i = 0; i < tiers; i++)
				tierOffsets[i + 1] = tierOffsets[i] + myGame.numHashesForTier(i);
			assert Util.debug(DebugFacility.HASHER, "Created offset table: "
					+ Arrays.toString(tierOffsets));
		}
		return tierOffsets[tier];
	}

	public long numHashes() {
		return hashOffsetForTier(myGame.numberOfTiers());
	}

	public void gameStateForTierAndOffset(int tier, long index, TierState s) {
		s.tier = tier;
		s.hash = index;
	}

	public Pair<Integer, Long> tierIndexForState(TierState state) {
		return new Pair<Integer, Long>(state.tier, state.hash);
	}
}

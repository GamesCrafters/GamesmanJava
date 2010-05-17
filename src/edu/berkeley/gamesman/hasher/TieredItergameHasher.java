package edu.berkeley.gamesman.hasher;

import java.util.Arrays;

import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.game.TieredGame;
import edu.berkeley.gamesman.game.util.ItergameState;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies Hasher for Tiered Cycle Games
 */
public final class TieredItergameHasher extends Hasher<ItergameState> {
	private long tierOffsets[] = null;

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
			int tiers = numberOfTiers();
			tierOffsets = new long[tiers + 1];
			tierOffsets[0] = 0;
			for (int i = 0; i < tiers; i++)
				tierOffsets[i + 1] = tierOffsets[i] + numHashesForTier(i);
			assert Util.debug(DebugFacility.HASHER, "Created offset table: "
					+ Arrays.toString(tierOffsets));
		}
		return tierOffsets[tier];
	}

	@Override
	public long numHashes() {
		return hashOffsetForTier(numberOfTiers());
	}

	public void gameStateForTierAndOffset(int tier, long index, ItergameState s) {
		s.tier = tier;
		s.hash = index;
	}

	public long numHashesForTier(int tier) {
		return ((TieredGame) conf.getGame()).numHashesForTier(tier);
	}

	public int numberOfTiers() {
		return ((TieredGame) conf.getGame()).numberOfTiers();
	}

	public Pair<Integer, Long> tierIndexForState(ItergameState state) {
		return new Pair<Integer, Long>(state.tier, state.hash);
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long hash(ItergameState state) {
		return hashOffsetForTier(state.tier) + state.hash;
	}

	@Override
	public ItergameState unhash(long hash) {
		return ((TieredGame) conf.getGame()).hashToState(hash);
	}
}

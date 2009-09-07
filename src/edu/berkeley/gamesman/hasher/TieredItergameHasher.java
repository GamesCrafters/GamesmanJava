package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.util.Pair;

/**
 * @author DNSpies Hasher for Tiered Cycle Games
 */
public final class TieredItergameHasher extends TieredHasher<ItergameState> {

	/**
	 * @param conf
	 *            The configuration object
	 */
	public TieredItergameHasher(Configuration conf) {
		super(conf);
	}

	@Override
	public ItergameState gameStateForTierAndOffset(int tier, long index) {
		return new ItergameState(tier, index);
	}

	@Override
	public long numHashesForTier(int tier) {
		return ((TieredIterGame) conf.getGame()).numHashesForTier(tier);
	}

	@Override
	public int numberOfTiers() {
		return ((TieredIterGame) conf.getGame()).numberOfTiers();
	}

	@Override
	public Pair<Integer, Long> tierIndexForState(ItergameState state) {
		return new Pair<Integer, Long>(state.tier, state.hash);
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

}

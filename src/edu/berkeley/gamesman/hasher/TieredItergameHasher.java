package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.game.TieredIterGame;
import edu.berkeley.gamesman.game.util.ItergameState;
import edu.berkeley.gamesman.util.Pair;

/**
 * @author DNSpies Hasher for Tiered Cycle Games
 */
public final class TieredItergameHasher extends TieredHasher<ItergameState> {

	@Override
	public void gameStateForTierAndOffset(int tier, long index, ItergameState s) {
		s.tier = tier;
		s.hash = index;
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

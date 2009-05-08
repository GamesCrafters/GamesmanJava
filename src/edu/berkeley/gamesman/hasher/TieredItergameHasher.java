package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.util.Pair;

/**
 * @author DNSpies
 * Hasher for Tiered Cycle Games
 */
public class TieredItergameHasher extends TieredHasher<ItergameState> {

	/**
	 * @param conf The configuration object
	 */
	public TieredItergameHasher(Configuration conf) {
		super(conf);
	}

	@Override
	public ItergameState gameStateForTierAndOffset(int tier, BigInteger index) {
		return new ItergameState(tier,index.longValue());
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		return ((TieredIterGame) conf.getGame()).numHashesForTier(tier);
	}

	@Override
	public int numberOfTiers() {
		return ((TieredIterGame) conf.getGame()).numberOfTiers();
	}

	@Override
	public Pair<Integer, BigInteger> tierIndexForState(ItergameState state) {
		return new Pair<Integer,BigInteger>(state.car,BigInteger.valueOf(state.cdr));
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

}

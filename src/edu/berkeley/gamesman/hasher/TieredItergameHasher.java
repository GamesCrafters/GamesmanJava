package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.game.connect4.ItergameState;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

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
		return new ItergameState(tier,index);
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		return ((TieredIterGame) conf.getGame()).numHashesForTier(tier);
	}

	@Override
	public int numberOfTiers() {
		return (int) Util.longpow((conf.getGame().getGameHeight() + 1), conf.getGame().getGameWidth());
	}

	@Override
	public Pair<Integer, BigInteger> tierIndexForState(ItergameState state) {
		return state;
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

}

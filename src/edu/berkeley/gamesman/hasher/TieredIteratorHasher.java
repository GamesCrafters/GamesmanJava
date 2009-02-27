package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.game.IteratorState;
import edu.berkeley.gamesman.game.TieredIteratorGame;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies
 * Hasher for Tiered Cycle Games
 */
public class TieredIteratorHasher extends TieredHasher<IteratorState> {

	/**
	 * @param conf The configuration object
	 */
	public TieredIteratorHasher(Configuration conf) {
		super(conf);
	}

	@Override
	public IteratorState gameStateForTierAndOffset(int tier, BigInteger index) {
		return new IteratorState(tier,index);
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		return ((TieredIteratorGame) conf.getGame()).numHashesForTier(tier);
	}

	@Override
	public int numberOfTiers() {
		return (int) Util.longpow((conf.getGame().getGameHeight() + 1), conf.getGame().getGameWidth());
	}

	@Override
	public Pair<Integer, BigInteger> tierIndexForState(IteratorState state) {
		return state;
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

}

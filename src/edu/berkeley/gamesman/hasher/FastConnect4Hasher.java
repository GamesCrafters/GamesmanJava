package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.game.connect4.C4State;
import edu.berkeley.gamesman.game.connect4.FastConnect4;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies
 * Hasher for Connect 4
 */
public class FastConnect4Hasher extends TieredHasher<C4State> {

	/**
	 * @param conf The configuration object
	 */
	public FastConnect4Hasher(Configuration conf) {
		super(conf);
	}

	@Override
	public C4State gameStateForTierAndOffset(int tier, BigInteger index) {
		return new C4State(tier,index);
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		return ((FastConnect4) conf.getGame()).numHashesForTier(tier);
	}

	@Override
	public int numberOfTiers() {
		return (int) Util.longpow((conf.getGame().getGameHeight() + 1), conf.getGame().getGameWidth());
	}

	@Override
	public Pair<Integer, BigInteger> tierIndexForState(C4State state) {
		return state;
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

}

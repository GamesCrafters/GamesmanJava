package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.game.CycleState;
import edu.berkeley.gamesman.game.TieredCycleGame;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies
 * Hasher for Tiered Cycle Games
 */
public class TieredCycleHasher extends TieredHasher<CycleState> {

	/**
	 * @param conf The configuration object
	 */
	public TieredCycleHasher(Configuration conf) {
		super(conf);
	}

	@Override
	public CycleState gameStateForTierAndOffset(int tier, BigInteger index) {
		return new CycleState(tier,index);
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		return ((TieredCycleGame) conf.getGame()).numHashesForTier(tier);
	}

	@Override
	public int numberOfTiers() {
		return (int) Util.longpow((conf.getGame().getGameHeight() + 1), conf.getGame().getGameWidth());
	}

	@Override
	public Pair<Integer, BigInteger> tierIndexForState(CycleState state) {
		return state;
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

}

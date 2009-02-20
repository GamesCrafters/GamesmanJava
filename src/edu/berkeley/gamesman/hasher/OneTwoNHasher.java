package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.util.Pair;

public class OneTwoNHasher extends TieredHasher<Integer> {
	
	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public OneTwoNHasher(Configuration conf) {
		super(conf);
		
		gameWidth = conf.getGame().getGameWidth();
		gameHeight = conf.getGame().getGameHeight();
	}

	private int gameWidth, gameHeight;
	
	@Override
	public BigInteger numHashesForTier(int tier) {
		return BigInteger.ONE;
	}

	@Override
	public Integer gameStateForTierAndOffset(int tier, BigInteger index) {
		return tier + 1;
		
	}

	@Override
	public int numberOfTiers() {
		return gameHeight;
	}
	
	@Override
	public Pair<Integer, BigInteger> tierIndexForState(Integer state) {
		return new Pair<Integer, BigInteger>(state / gameWidth, 
				new BigInteger(String.valueOf(state % gameWidth)));
	}

	@Override
	public String describe() {
		return "OTNH";
	}
}

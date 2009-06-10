package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.game.OneTwoN;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author Wesley Hart
 * 
 */
public class OneTwoNHasher extends TieredHasher<Integer> {
	private final OneTwoN game;

	/**
	 * Default constructor
	 * 
	 * @param conf the configuration
	 */
	public OneTwoNHasher(Configuration conf) {
		super(conf);
		if (!(conf.getGame() instanceof OneTwoN))
			Util.fatalError("This hasher is only designed for OneTwoN");
		game = (OneTwoN) conf.getGame();
	}

	@Override
	public long numHashesForTier(int tier) {
		return 1;
	}

	@Override
	public Integer gameStateForTierAndOffset(int tier, long index) {
		Util.assertTrue(index == 0, "Index must always be 0");
		return tier;
	}

	@Override
	public int numberOfTiers() {
		return game.maxNumber + 1;
	}

	@Override
	public Pair<Integer, Long> tierIndexForState(Integer state) {
		return new Pair<Integer, Long>(state, 0L);
	}

	@Override
	public String describe() {
		return "OTNH";
	}
}

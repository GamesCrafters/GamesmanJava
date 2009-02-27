package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.game.connect4.C4Board;
import edu.berkeley.gamesman.util.Pair;

/**
 * The Connect 4 hasher from GamesmanClassic
 * @author Steven Schlansker
 */
public class ClassicConnect4Hash extends TieredHasher<C4Board> {

	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public ClassicConnect4Hash(Configuration conf) {
		super(conf);
	}
	
	@Override
	public C4Board gameStateForTierAndOffset(int tier, BigInteger index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int numberOfTiers() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Pair<Integer, BigInteger> tierIndexForState(C4Board state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

}

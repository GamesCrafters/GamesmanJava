package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.util.Pair;

/**
 * The Connect 4 hasher from GamesmanClassic
 * @author Steven Schlansker
 */
public class ClassicConnect4Hash extends TieredHasher<char[][]> {

	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public ClassicConnect4Hash(Configuration conf) {
		super(conf);
	}

	private static final long serialVersionUID = -5178988467392378350L;

	@Override
	public char[][] gameStateForTierAndOffset(int tier, BigInteger index) {
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
	public Pair<Integer, BigInteger> tierIndexForState(char[][] state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

}

package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.util.Pair;

/**
 * NullHasher is a placeholder for games that do not allow use of a custom hasher.
 * It does nothing interesting.
 * @author Steven Schlansker
 */
public class NullHasher extends TieredHasher<char[]> {

	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public NullHasher(Configuration conf) {
		super(conf);
	}

	private static final long serialVersionUID = -5302283345311520545L;

	@Override
	public char[] unhash(BigInteger hash, int l) {
		return null;
	}

	@Override
	public BigInteger hash(char[] board, int l) {
		return null;
	}

	@Override
	public BigInteger maxHash(int boardlen) {
		return null;
	}

	@Override
	public char[] gameStateForTierAndOffset(int tier, BigInteger index) {
		return null;
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		return null;
	}

	@Override
	public int numberOfTiers() {
		return 0;
	}

	@Override
	public Pair<Integer, BigInteger> tierIndexForState(char[] state) {
		return null;
	}

	@Override
	public String describe() {
		return "";
	}

}

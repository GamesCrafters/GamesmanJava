package edu.berkeley.gamesman.core;

import java.math.BigInteger;

import edu.berkeley.gamesman.util.Pair;

/**
 * @author DNSpies
 */
public class ItergameState extends Pair<Integer, BigInteger> {

	/**
	 * @param tier The tier
	 * @param hash The hash
	 */
	public ItergameState(Integer tier, BigInteger hash) {
		super(tier, hash);
	}

	/**
	 * @return The tier
	 */
	public int tier() {
		return car;
	}

	/**
	 * @return The hash
	 */
	public BigInteger hash() {
		return cdr;
	}

}

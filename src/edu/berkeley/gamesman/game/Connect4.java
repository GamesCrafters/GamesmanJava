/**
 * 
 */
package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.gamesman.OptionProcessor;
import edu.berkeley.gamesman.Pair;
import edu.berkeley.gamesman.core.SingletonIterator;
import edu.berkeley.gamesman.Util;

/**
 * @author Steven Schlansker
 *
 */
public final class Connect4 extends Game<BigInteger,Values> implements TieredGame<BigInteger, Values> {

	static {
		OptionProcessor.acceptOption("p", "pieces", true, "The number of pieces in a row to win (default 4)", "4");
		OptionProcessor.nextGroup();
	}
	

	private BigInteger tierOffsets[];
	
	/**
	 * New game of Connect4
	 */
	public Connect4(){
		super();
		tierOffsets = new BigInteger[lastTier()];
		tierOffsets[0] = BigInteger.ZERO;
		for(int t = 1; t < tierOffsets.length; t++)
			tierOffsets[t] = tierOffsets[t-1].add(lastHashValueForTier(t-1));
		System.out.println(Arrays.toString(tierOffsets));
	}
	
	@Override
	public Values positionValue(BigInteger pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<BigInteger> startingPositions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<BigInteger> validMoves(BigInteger pos) {
		// TODO Auto-generated method stub
		return null;
	}

	public BigInteger gameStateForTierIndex(int tier, BigInteger index) {
		return tierOffsets[tier].add(index);
	}

	public BigInteger lastHashValueForTier(int tier) {
		return BigInteger.valueOf(gameWidth).pow(tier);
	}

	public int lastTier() {
		return (gameWidth*gameHeight)-1;
	}

	public BigInteger hashOffestForTier(int tier) {
		return tierOffsets[tier];
	}
	
	public Pair<Integer, BigInteger> tierIndexForState(BigInteger state) {
		for(int t = tierOffsets.length - 1; t > 0; t++)
			if(state.compareTo(tierOffsets[t]) <= 0)
				return new Pair<Integer,BigInteger>(t,state.subtract(tierOffsets[t]));
		Util.fatalError("Invaild state index");
		return null;
	}
	
}

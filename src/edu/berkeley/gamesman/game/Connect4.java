/**
 * 
 */
package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.gamesman.OptionProcessor;
import edu.berkeley.gamesman.Pair;
import edu.berkeley.gamesman.core.SingletonIterator;

/**
 * @author Steven Schlansker
 *
 */
public final class Connect4 extends Game<String,Values> implements TieredGame<String, Values> {

	static {
		OptionProcessor.acceptOption("p", "pieces", true, "The number of pieces in a row to win (default 4)", "4");
		OptionProcessor.nextGroup();
	}
	
	@Override
	public Values positionValue(String pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> startingPositions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<String> validMoves(String pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String gameStateForTierIndex(Number tier, Number index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number lastHashValueForTier(Number tier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number lastTier() {
		return gameWidth*gameHeight;
	}

	@Override
	public Pair<Number, Number> tierIndexForState(String state) {
		long pieces = 0;
		BigInteger rearr = BigInteger.ZERO;
		for(char c : state.toCharArray()){
			pieces++;
		}
		return null;
	}
	

}

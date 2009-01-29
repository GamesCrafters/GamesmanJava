package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.gamesman.Util;

public class NullGame extends Game<Object,Object> {

	@Override
	public Collection<Object> startingPositions() {
		fail();
		return null;
	}

	@Override
	public Object positionValue(Object pos) {
		fail();
		return null;
	}

	@Override
	public Iterator<Object> validMoves(Object pos) {
		fail();
		return null;
	}

	@Override
	public String stateToString(Object pos) {
		fail();
		return null;
	}

	@Override
	public Object hashToState(BigInteger hash) {
		fail();
		return null;
	}

	@Override
	public BigInteger stateToHash(Object state) {
		fail();
		return null;
	}
	
	private void fail(){
		Util.fatalError("Please specify a game with --game");
	}

}

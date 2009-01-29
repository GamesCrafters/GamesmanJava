package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.database.DBValue;
import edu.berkeley.gamesman.util.Util;

public class NullGame extends Game<Object,DBValue> {

	@Override
	public Collection<Object> startingPositions() {
		fail();
		return null;
	}

	@Override
	public DBValue primitiveValue(Object pos) {
		fail();
		return null;
	}

	@Override
	public Collection<Object> validMoves(Object pos) {
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

	@Override
	public BigInteger stringToState(String pos) {
		fail();
		return null;
	}

	@Override
	public int getDefaultBoardHeight() {
		fail();
		return 0;
	}

	@Override
	public int getDefaultBoardWidth() {
		fail();
		return 0;
	}

	@Override
	public DBValue getDBValueExample() {
		fail();
		return null;
	}
	
	private void fail(){
		Util.fatalError("Please specify a game with --game");
	}

}

package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Collection;

import edu.berkeley.gamesman.core.DBRecord;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.hasher.NullHasher;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Util;

public class NullGame extends Game<Object,DBRecord> {

	static {
		DependencyResolver.allowHasher(NullGame.class, NullHasher.class);
	}
	
	@Override
	public Collection<Object> startingPositions() {
		fail();
		return null;
	}

	@Override
	public DBRecord primitiveValue(Object pos) {
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
		return 0;
	}

	@Override
	public int getDefaultBoardWidth() {
		return 0;
	}

	@Override
	public DBRecord getDBValueExample() {
		fail();
		return null;
	}
	
	private void fail(){
		Util.fatalError("Please specify a game with --game");
	}

	@Override
	public String describe() {
		fail();
		return null;
	}

}

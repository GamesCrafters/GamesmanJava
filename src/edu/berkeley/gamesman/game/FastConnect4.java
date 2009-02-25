package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

class Connect4Board {
	
}

public class FastConnect4 extends Game<Connect4Board> {

	public FastConnect4(Configuration conf) {
		super(conf);
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String displayState(Connect4Board pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getDefaultBoardHeight() {
		return 6;
	}

	@Override
	public int getDefaultBoardWidth() {
		return 7;
	}

	@Override
	public Connect4Board hashToState(BigInteger hash) {
		Util.fatalError("No unhash yet");
		return null;
	}

	@Override
	public BigInteger lastHash() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[] pieces() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrimitiveValue primitiveValue(Connect4Board pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Connect4Board> startingPositions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigInteger stateToHash(Connect4Board pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String stateToString(Connect4Board pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Connect4Board stringToState(String pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Pair<String, Connect4Board>> validMoves(Connect4Board pos) {
		// TODO Auto-generated method stub
		return null;
	}

}

package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.game.connect4.OneTierC4Board;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class FastConnect4 extends Game<OneTierC4Board> {

	public FastConnect4(Configuration conf) {
		super(conf);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String displayState(OneTierC4Board pos) {
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
	public OneTierC4Board hashToState(BigInteger hash) {
		// TODO Auto-generated method stub
		Util.fatalError("Not implemented");
		return null;
	}

	@Override
	public BigInteger lastHash() {
		new OneTierC4Board
		return null;
	}

	@Override
	public char[] pieces() {
		return new char[] {'X','O'};
	}

	@Override
	public PrimitiveValue primitiveValue(OneTierC4Board pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<OneTierC4Board> startingPositions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigInteger stateToHash(OneTierC4Board pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String stateToString(OneTierC4Board pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OneTierC4Board stringToState(String pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Pair<String, OneTierC4Board>> validMoves(
			OneTierC4Board pos) {
		// TODO Auto-generated method stub
		return null;
	}
	
}

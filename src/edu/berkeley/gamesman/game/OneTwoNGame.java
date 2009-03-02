package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.util.Pair;

/**
 * OneTwoTen modified to be a game. This is for testing the TopDownSolver
 * 
 * @author Jeremy Fleischman
 */
public class OneTwoNGame extends Game<Integer> {
	private final int MAX_NUMBER, MAX_STEP;
	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public OneTwoNGame(Configuration conf) {
		super(conf);
		MAX_NUMBER = Integer.parseInt(conf.getProperty("maxNumber", "10"));
		MAX_STEP = Integer.parseInt(conf.getProperty("maxStep", "2"));
	}
	
	@Override
	public Collection<Integer> startingPositions() {
		return Arrays.asList(0);
	}
	
	@Override
	public PrimitiveValue primitiveValue(Integer pos) {
		if (pos == MAX_NUMBER)
			return PrimitiveValue.Lose;		
		return PrimitiveValue.Undecided;
	}
	
	@Override
	public String displayState(Integer pos) {
		return pos + "";
	}
	
	@Override
	public Integer stringToState(String pos) {
		return Integer.parseInt(pos);
	}

	@Override
	public String stateToString(Integer pos) {
		return pos + "";
	}

	@Override
	public Collection<Pair<String,Integer>> validMoves(Integer pos) {
		ArrayList<Pair<String,Integer>> next = new ArrayList<Pair<String,Integer>>();		
		for(int move = 1; move <= MAX_STEP; move++)
			if(move + pos <= MAX_NUMBER)
				next.add(new Pair<String,Integer>("+"+move,pos + move));
		return next;
	}
	
	@Override
	public String describe() {
		return "OneTwo"+MAX_NUMBER+" ("+MAX_STEP+" max spaces per move)";
	}

	@Override
	public char[] pieces() {
		return null;
	}

	@Override
	public Integer hashToState(BigInteger hash) {
		return hash.intValue();
	}

	@Override
	public BigInteger lastHash() {
		return BigInteger.valueOf(MAX_NUMBER);
	}

	@Override
	public BigInteger stateToHash(Integer pos) {
		return BigInteger.valueOf(pos);
	}

	@Override
	public int getDefaultBoardHeight() {
		return 0;
	}

	@Override
	public int getDefaultBoardWidth() {
		return 0;
	}
}

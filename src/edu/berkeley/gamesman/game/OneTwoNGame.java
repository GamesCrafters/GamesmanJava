package edu.berkeley.gamesman.game;

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
		MAX_NUMBER = conf.getInteger("maxNumber", 10);
		MAX_STEP = conf.getInteger("maxStep", 2);
	}
	
	@Override
	public Collection<Integer> startingPositions() {
		return Arrays.asList(0);
	}
	
	@Override
	public PrimitiveValue primitiveValue(Integer pos) {
		if (pos == MAX_NUMBER)
			return PrimitiveValue.LOSE;		
		return PrimitiveValue.UNDECIDED;
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
	public Integer hashToState(long hash) {
		return (int)hash;
	}

	@Override
	public long lastHash() {
		return MAX_NUMBER;
	}

	@Override
	public long stateToHash(Integer pos) {
		return pos.longValue();
	}
}

package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.hasher.OneTwoNHasher;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Pair;

/**
 * OneTwoTen
 * 
 * @author Wesley Hart
 */
public class OneTwoN extends TieredGame<Integer> {
	/**
	 * The last number you can get to while playing OneTwoN
	 */
	public final int maxNumber;
	/**
	 * Each move must add a number in the range [1, maxStep]
	 */
	public final int maxStep;
	
	static {
		DependencyResolver.allowHasher(OneTwoN.class, OneTwoNHasher.class);
	}
	
	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public OneTwoN(Configuration conf) {
		super(conf);
		maxNumber = Integer.parseInt(conf.getProperty("gamesman.game.maxNumber", "10"));
		maxStep = Integer.parseInt(conf.getProperty("gamesman.game.maxStep", "2"));
	}
	
	@Override
	public Collection<Integer> startingPositions() {
		ArrayList<Integer> boards = new ArrayList<Integer>();
		boards.add(0);
		return boards;
	}
	
	@Override
	public PrimitiveValue primitiveValue(Integer pos) {
		if(pos == maxNumber)
			return PrimitiveValue.LOSE;		
		return PrimitiveValue.UNDECIDED;
	}
	
	@Override
	public String displayState(Integer pos) {
		return pos.toString();
	}
	
	@Override
	public Integer stringToState(String pos) {
		return Integer.parseInt(pos);
	}
	
	@Override
	public String stateToString(Integer pos) {
		return pos.toString();
	}

	@Override
	public Collection<Pair<String,Integer>> validMoves(Integer pos) {
		ArrayList<Pair<String,Integer>> nextBoards = new ArrayList<Pair<String,Integer>>();		
	
		if (primitiveValue(pos) != PrimitiveValue.UNDECIDED)
			return nextBoards;
		
		for (int move = 1; pos+move <= maxNumber; move++)
			nextBoards.add(new Pair<String,Integer>("+"+move,pos + move));
		
		return nextBoards;
	}
	
	@Override
	public String describe() {
		return "OneTwoN|"+maxNumber+"|"+maxStep;
	}

}

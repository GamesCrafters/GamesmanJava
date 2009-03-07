package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.hasher.OneTwoNHasher;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * OneTwoTen
 * 
 * @author Wesley Hart
 */
public class OneTwoN extends TieredGame<Integer> {
	final char piece = 'X';
	
	static {
		DependencyResolver.allowHasher(OneTwoN.class, OneTwoNHasher.class);
	}
	
	/**
	 * Default constructor
	 * @param conf the configuration
	 */
	public OneTwoN(Configuration conf) {
		super(conf);
		//myHasher.initialize(null);
	}
	
	@Override
	public Collection<Integer> startingPositions() {
		ArrayList<Integer> boards = new ArrayList<Integer>();
		boards.add(0);
		return boards;
	}
	
	@Override
	public int getDefaultBoardHeight() {
		return 10;
	}
	
	@Override
	public int getDefaultBoardWidth() {
		return 2;
	}
	
	@Override
	public PrimitiveValue primitiveValue(Integer pos) {
		if (pos == gameHeight)
			return PrimitiveValue.LOSE;		
		return PrimitiveValue.UNDECIDED;
	}
	
	@Override
	public String displayState(Integer pos) {
		//StringBuilder str = new StringBuilder();
		//int i;
		//for (i = 1; i < gameHeight; i++)
		//	str.append((i == pos ? piece : i) + ' ');
		//str.append((++i == pos ? piece : i) + '\n');		
		//return str.toString();
		return pos.toString();
	}
	
	@Override
	public Integer stringToState(String pos) {
		if (pos.indexOf(piece) == pos.length() - 2)
			return gameHeight;
		String afterPiece = pos.substring(pos.indexOf(piece) + 2);
		int pieceIndex = Integer.parseInt(afterPiece.substring(0, afterPiece.indexOf(' '))) - 1;
		return pieceIndex;
	}	

	@Override
	public Collection<Pair<String,Integer>> validMoves(Integer pos) {
		ArrayList<Pair<String,Integer>> nextBoards = new ArrayList<Pair<String,Integer>>();		
	
		if (primitiveValue(pos) != PrimitiveValue.UNDECIDED)
			return nextBoards;
		
		for (int move = 1; move <= gameWidth && pos+move <= gameHeight; move++)
			nextBoards.add(new Pair<String,Integer>("+"+move,pos + move));
		
		return nextBoards;
	}
	
	@Override
	public String toString() {
		return "OneTwo"+gameHeight+" ("+gameWidth+" max spaces per move)";
	}
	
	@Override
	public String describe() {
		return "OneTwoN|"+gameWidth+"|"+gameHeight;
	}

	@Override
	public char[] pieces() {
		return new char[] {};
	}

	@Override
	public String stateToString(Integer pos) {
		Util.fatalError("Not written yet");
		return null;
	}
}

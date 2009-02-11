package edu.berkeley.gamesman.game;

import java.util.Collection;
import java.util.ArrayList;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.hasher.OneTwoNHasher;
import edu.berkeley.gamesman.util.DependencyResolver;

/**
 * OneTwoTen
 * 
 * @author Wesley Hart
 */
public class OneTwoN extends TieredGame<Integer> {

	private static final long serialVersionUID = -6034946489055318659L;
	final char piece = 'X';
	
	static {
		DependencyResolver.allowHasher(OneTwoN.class, OneTwoNHasher.class);
	}
	
	/**
	 * Default constructor
	 */
	public OneTwoN() {
		super();
	}
	
	@Override
	public void initialize(Configuration conf){
		super.initialize(conf);
		myHasher.setGame(this, null);
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
			return PrimitiveValue.Lose;		
		return PrimitiveValue.Undecided;
	}
	
	@Override
	public String stateToString(Integer pos) {
		StringBuilder str = new StringBuilder();
		int i;
		for (i = 1; i < gameHeight; i++)
			str.append((i == pos ? piece : i) + ' ');
		str.append((++i == pos ? piece : i) + '\n');		
		return str.toString();
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
	public Collection<Integer> validMoves(Integer pos) {
		ArrayList<Integer> nextBoards = new ArrayList<Integer>();		
	
		if (primitiveValue(pos) != PrimitiveValue.Undecided)
			return nextBoards;
		
		for (int move = 1; move <= gameWidth; move++)
			nextBoards.add(pos + move);
		
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
}

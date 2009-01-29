package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.DependencyResolver;
import edu.berkeley.gamesman.OptionProcessor;
import edu.berkeley.gamesman.hasher.PC4Hash;

/**
 * Connect 4!
 * @author Steven Schlansker
 */
public class Connect4 extends HashedGame<Values> {

	static {
		OptionProcessor.acceptOption("p", "pieces", true, "The number of pieces in a row to win (default 4)", "4");
		OptionProcessor.nextGroup();
		DependencyResolver.allowHasher(Connect4.class, PC4Hash.class);
	}

	@Override
	public Values positionValue(char[][] pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<char[][]> startingPositions() {
		ArrayList<char[][]> boards = new ArrayList<char[][]>();
		boards.add(new char[gameWidth][gameHeight]);
		return boards;
	}

	@Override
	public Collection<char[][]> validMoves(char[][] pos) {
		// TODO Auto-generated method stub
		return null;
	}
	
}

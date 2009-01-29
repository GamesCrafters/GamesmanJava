package edu.berkeley.gamesman.game;

import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.gamesman.Util;

public class NullGame extends Game {

	@Override
	public Collection<?> startingPositions() {
		Util.fatalError("Please specify a game with --game");
		return null;
	}

	@Override
	public Object positionValue(Object pos) {
		return null;
	}

	@Override
	public Iterator validMoves(Object pos) {
		return null;
	}

}

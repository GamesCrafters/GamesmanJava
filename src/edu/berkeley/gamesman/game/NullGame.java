package edu.berkeley.gamesman.game;

import java.util.Iterator;

import edu.berkeley.gamesman.Util;

public class NullGame extends Game {

	@Override
	public Iterator<GameState> startingPositions() {
		Util.fatalError("Null game initialized");
		return null;
	}

}

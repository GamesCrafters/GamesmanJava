package edu.berkeley.gamesman.mapreduce;

import java.util.List;
import java.util.ArrayList;

import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.State;

public class LoopyGameAdapter implements LoopyGame {
	private final Game game;
	private final State[] tmp;
	private final List<Long> hashes;

	public LoopyGameAdapter(Game g) {
		game = g;
		tmp = game.newStateArray(game.maxChildren());
		hashes = new ArrayList<Long>(tmp.length);
	}

	public Iterable<Long> getSuccessors(long hash) {
		int number = game.validMoves(game.hashToState(hash), tmp);
		hashes.clear();
		for (int i=0; i < number; i++)
			hashes.add(game.stateToHash(tmp[i]));
		return hashes;
	}

	public boolean isPrimitive(long hash) {
		return game.primitiveValue(game.hashToState(hash)) != Value.UNDECIDED;
	}

	public int evalPrimitive(long hash) {
		Value val = game.primitiveValue(game.hashToState(hash));
		switch (val) {
			case LOSE: return Node.LOSE;
			case DRAW: return Node.DRAW;
			case TIE: return Node.TIE;
			case WIN: return Node.WIN;
			default: throw new AssertionError();
		}
	}

	public Iterable<Long> getStartingPositions() {
		List<Long> init = new ArrayList<Long>();
		for (Object s : game.startingPositions())
			init.add(game.stateToHash((State)s));
		return init;
	}
}

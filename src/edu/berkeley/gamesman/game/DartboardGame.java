package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public abstract class DartboardGame extends Game<Number, Values> implements
		TieredGame<Number, Values> {

	@Override
	public Values positionValue(Number pos) {
		return Values.Undecided;
	}

	@Override
	public Collection<Number> startingPositions() {
		ArrayList<Number> a = new ArrayList<Number>();
		a.add(0);
		return a;
	}

	@Override
	public Iterator<Number> validMoves(Number pos) {
		// TODO Auto-generated method stub
		return null;
	}

	public Integer gameStateForTierIndex(Number tier, Number index) {
		// TODO Auto-generated method stub
		return null;
	}

	public Number lastHashValueForTier(Number tier) {
		// TODO Auto-generated method stub
		return null;
	}

	public Number lastTier() {
		// TODO Auto-generated method stub
		return null;
	}

}

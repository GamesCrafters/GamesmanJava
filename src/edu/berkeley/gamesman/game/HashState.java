package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.State;

public class HashState implements State {

	public long hash;

	public HashState() {
	}

	public HashState(long hash) {
		this.hash = hash;
	}

	public void set(State s) {
		hash = ((HashState) s).hash;
	}

}

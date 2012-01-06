package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.State;

public class HashState implements State<HashState> {

	public long hash;

	public HashState() {
	}

	public HashState(long hash) {
		this.hash = hash;
	}

	@Override
	public void set(HashState s) {
		hash = s.hash;
	}

}

package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class DBState extends CountingState {

	public DBState(GenHasher<? extends DBState> myHasher, int countTo) {
		super(myHasher, countTo);
	}

}

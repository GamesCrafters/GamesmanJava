package edu.berkeley.gamesman.hadoop.game.connect4;

import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class C4State extends CountingState {
	public C4State(GenHasher<? extends CountingState> myHasher, int boardSize) {
		super(myHasher, boardSize);
	}
}

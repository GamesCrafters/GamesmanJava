package edu.berkeley.gamesman.parallel.game.connections;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class ConnectionsState extends CountingState{

	public ConnectionsState(GenHasher<? extends CountingState> myHasher,
			int countTo) {
		super(myHasher, countTo);
		// TODO Auto-generated constructor stub
	}
	
}

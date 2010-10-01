package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;

public abstract class LoopyMutaGame<S extends State> extends TopDownMutaGame<S> {

	public LoopyMutaGame(Configuration conf) {
		super(conf);
	}

	@Override
	public LoopyRecord getRecord() {
		return new LoopyRecord(conf);
	}

	public abstract boolean unmakeMove();

	public abstract boolean changeUnmakeMove();

	public abstract void remakeMove();
}

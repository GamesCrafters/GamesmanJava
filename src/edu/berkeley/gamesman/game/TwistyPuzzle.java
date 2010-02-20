package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.game.util.TwistyPuzzleRecord;

public abstract class TwistyPuzzle<S extends State> extends Game<S> {
	public TwistyPuzzle(Configuration conf) {
		super(conf);
	}

	@Override
	public Record newRecord() {
		return new TwistyPuzzleRecord(conf);
	}

	@Override
	public Record newRecord(long val) {
		return new TwistyPuzzleRecord(conf, val);
	}

	@Override
	public Record newRecord(PrimitiveValue pv) {
		return new TwistyPuzzleRecord(conf, pv);
	}

	@Override
	public long recordStates() {
		return conf.remotenessStates + 1;
	}

}

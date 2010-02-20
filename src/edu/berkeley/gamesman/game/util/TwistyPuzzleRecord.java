package edu.berkeley.gamesman.game.util;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;

public class TwistyPuzzleRecord extends Record {
	public TwistyPuzzleRecord(Configuration conf) {
		super(conf);
	}

	public TwistyPuzzleRecord(Configuration conf, long state) {
		super(conf);
		set(state);
	}

	public TwistyPuzzleRecord(Configuration conf, int remoteness) {
		super(conf);
		this.remoteness = remoteness;
	}

	public TwistyPuzzleRecord(Configuration conf, PrimitiveValue pv) {
		super(conf);
		value = pv;
	}

	@Override
	public long getState() {
		if (value.equals(PrimitiveValue.UNDECIDED))
			return 0;
		else
			return remoteness + 1;
	}

	@Override
	public void set(long state) {
		if (state == 0)
			value = PrimitiveValue.UNDECIDED;
		else {
			value = PrimitiveValue.WIN;
			remoteness = (int) (state - 1);
		}
	}
}

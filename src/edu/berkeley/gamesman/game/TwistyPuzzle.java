package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;

/**
 * A superclass for twisty puzzles
 * 
 * @author dnspies
 * 
 * @param <S>
 *            The state for the puzzle
 */
public abstract class TwistyPuzzle<S extends State> extends Game<S> {

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

class TwistyPuzzleRecord extends Record {
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
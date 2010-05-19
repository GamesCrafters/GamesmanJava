package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.database.Record;

/**
 * A superclass for twisty puzzles
 * 
 * @author dnspies
 * 
 * @param <S>
 *            The state for the puzzle
 */
public abstract class TwistyPuzzle<S extends State> extends Game<S> {

	public TwistyPuzzle(Configuration conf) {
		super(conf);
	}

	@Override
	public int getPlayerCount() {
		return 1;
	}

	@Override
	public long recordStates() {
		return conf.remotenessStates + 1;
	}

	@Override
	public long getRecord(S recordState, Record fromRecord) {
		if (fromRecord.value.equals(PrimitiveValue.UNDECIDED))
			return 0;
		else
			return fromRecord.remoteness + 1;
	}

	@Override
	public void recordFromLong(S recordState, long record, Record toStore) {
		if (record == 0)
			toStore.value = PrimitiveValue.UNDECIDED;
		else {
			toStore.value = PrimitiveValue.WIN;
			toStore.remoteness = (int) (record - 1);
		}
	}
}

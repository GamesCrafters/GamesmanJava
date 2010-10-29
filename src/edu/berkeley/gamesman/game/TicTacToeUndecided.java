package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.TierState;

public class TicTacToeUndecided extends TicTacToe {

	public TicTacToeUndecided(Configuration conf) {
		super(conf);
	}

	@Override
	public long recordStates() {
		return super.recordStates() + 1;
	}

	@Override
	public void longToRecord(TierState recordState, long record, Record toStore) {
		if (record == super.recordStates())
			toStore.value = Value.UNDECIDED;
		else
			super.longToRecord(recordState, record, toStore);
	}

	@Override
	public long recordToLong(TierState recordState, Record fromRecord) {
		if (fromRecord.value == Value.UNDECIDED)
			return super.recordStates();
		else
			return super.recordToLong(recordState, fromRecord);
	}

}

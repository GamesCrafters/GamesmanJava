package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.TierState;

public class TicTacToeLoopy extends TicTacToe implements Undoable<TierState> {

	public TicTacToeLoopy(Configuration conf) {
		super(conf);
	}

	@Override
	public long recordStates() {
		return super.recordStates() + 2;
	}

	@Override
	public void longToRecord(TierState recordState, long record, Record toStore) {
		if (record == super.recordStates()) {
			toStore.value = Value.IMPOSSIBLE;
		} else if (record == super.recordStates() + 1) {
			toStore.value = Value.DRAW;
		} else {
			super.longToRecord(recordState, record, toStore);
		}
	}

	@Override
	public long recordToLong(TierState recordState, Record fromRecord) {
		if (fromRecord.value == Value.IMPOSSIBLE)
			return super.recordStates();
		else if (fromRecord.value == Value.DRAW)
			return super.recordStates() + 1;
		else
			return super.recordToLong(recordState, fromRecord);
	}

	public int possibleParents(TierState pos, TierState[] parents) {
		setState(pos);
		int count = 0;
		int tier = getTier();
		char lastTurn = tier % 2 == 0 ? 'O' : 'X';
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				if (get(row, col) == lastTurn) {
					parents[count].tier = tier - 1;
					set(row, col, ' ');
					parents[count].hash = myHasher.getHash();
					set(row, col, lastTurn);
					count++;
				}
			}
		}
		return count;
	}

	@Override
	public int maxParents() {
		return gameSize;
	}
}

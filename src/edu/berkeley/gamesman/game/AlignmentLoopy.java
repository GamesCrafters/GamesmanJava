package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;

/**
 * @author Kevin, Nancy
 * 
 */

public class AlignmentLoopy extends Alignment implements LoopyGame<AlignmentState> {

	
	public AlignmentLoopy(Configuration conf) {
		super(conf);
		
	}

	@Override
	public long recordStates() {
		return super.recordStates() + 2;
	}

	@Override
	public void longToRecord(AlignmentState recordState, long record, Record toStore) {
		if (record == super.recordStates()) {
			toStore.value = Value.IMPOSSIBLE;
		} else if (record == super.recordStates() + 1) {
			toStore.value = Value.DRAW;
		} else {
			super.longToRecord(recordState, record, toStore);
		}
	}

	@Override
	public long recordToLong(AlignmentState recordState, Record fromRecord) {
		if (fromRecord.value == Value.IMPOSSIBLE)
			return super.recordStates();
		else if (fromRecord.value == Value.DRAW)
			return super.recordStates() + 1;
		else
			return super.recordToLong(recordState, fromRecord);
	}

	public int possibleParents(AlignmentState pos, AlignmentState[] parents) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int maxParents() {
		// TODO Auto-generated method stub
		return 0;
	}
}

package edu.berkeley.gamesman.parallel.ranges;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.propogater.writable.list.WritableTreeMap;

public class RecordMap extends WritableTreeMap<GameRecord> {
	public RecordMap() {
		super(GameRecord.class, null);
	}
}

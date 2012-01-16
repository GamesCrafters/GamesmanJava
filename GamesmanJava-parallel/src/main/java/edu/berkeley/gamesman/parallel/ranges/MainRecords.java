package edu.berkeley.gamesman.parallel.ranges;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class MainRecords extends WritableList<GameRecord> {
	public MainRecords() {
		super(GameRecord.class, null);
	}
}

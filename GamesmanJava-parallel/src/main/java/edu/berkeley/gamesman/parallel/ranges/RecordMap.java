package edu.berkeley.gamesman.parallel.ranges;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.propogater.writable.list.WritableTreeMap;

public class RecordMap extends WritableTreeMap<GameRecord> {
	public RecordMap() {
		super(GameRecord.class, null);
	}

	public void set(RecordMap other) {
		clear();
		other.restart();
		int i = other.peekNext();
		while (i >= 0) {
			GameRecord x = add(i);
			x.set(other.getNext(i));
			i = other.peekNext();
		}
	}
}

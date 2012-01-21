package edu.berkeley.gamesman.parallel.ranges;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.parallel.writable.WritableTreeMap;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class RecordMap extends WritableTreeMap<GameRecord> {
	public RecordMap(QLLFactory<IntEntry<GameRecord>> fact,
			Pool<IntEntry<GameRecord>> pool) {
		super(fact, pool);
	}
}

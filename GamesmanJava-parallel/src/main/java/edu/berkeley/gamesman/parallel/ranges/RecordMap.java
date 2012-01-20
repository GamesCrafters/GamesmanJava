package edu.berkeley.gamesman.parallel.ranges;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.parallel.writable.WritableTreeMap;
import edu.berkeley.gamesman.propogater.writable.IntEntry;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class RecordMap extends WritableTreeMap<GameRecord> {
	private static final QLLFactory<IntEntry<GameRecord>> fact = new QLLFactory<IntEntry<GameRecord>>();
	private static final Pool<IntEntry<GameRecord>> pool = new Pool<IntEntry<GameRecord>>(
			new Factory<IntEntry<GameRecord>>() {
				@Override
				public IntEntry<GameRecord> newObject() {
					return new IntEntry<GameRecord>(new GameRecord());
				}

				@Override
				public void reset(IntEntry<GameRecord> t) {
				}
			});

	public RecordMap() {
		super(fact, pool);
	}
}

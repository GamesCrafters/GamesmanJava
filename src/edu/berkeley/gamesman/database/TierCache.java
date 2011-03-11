package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.game.util.TierState;

public abstract class TierCache {
	protected final Database db;
	protected final long availableMemory;

	public TierCache(Database db, long availableMemory) {
		this.db = db;
		this.availableMemory = availableMemory;
	}

	public abstract void fetchChildren(TierState position, int numChildren,
			TierState[] children, int[] hints, Record[] values);
}

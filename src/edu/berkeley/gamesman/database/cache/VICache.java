package edu.berkeley.gamesman.database.cache;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;

public abstract class VICache {
	protected final Database db;
	protected final long availableMemory;

	public VICache(Database db, long availableMemory) {
		this.db = db;
		this.availableMemory = availableMemory;
	}

	public abstract void fetchChildren(long position, int numChildren,
			long[] children, int[] hints, Record[] values);
}

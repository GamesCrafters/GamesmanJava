package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;

public abstract class TierReadCache extends DatabaseWrapper {

	public TierReadCache(Database db, Configuration conf) {
		this(db, conf, -1, 0);
	}

	public TierReadCache(Database db, Configuration conf, long firstRecord,
			long numRecords) {
		super(db, null, conf, false, firstRecord, numRecords);
	}

	public abstract long numHashes();
}

package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;

public abstract class TierCache extends DatabaseWrapper {

	public TierCache(Database db, Configuration conf) {
		super(db, null, conf, false, -1, 0);
	}

	public abstract long numHashes();

	@Override
	protected final void putBytes(DatabaseHandle dh, long loc, byte[] arr,
			int off, int len) {
		throw new Error("Cache is read only");
	}

	public long getRecord(DatabaseHandle dh, long recordIndex, int hint) {
		return getRecord(dh, recordIndex);
	}
}

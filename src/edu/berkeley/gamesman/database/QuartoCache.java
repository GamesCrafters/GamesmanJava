package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;

public final class QuartoCache extends TierCache {
	private long numHashes = 0L;
	private Database innerDb;

	public QuartoCache(Database db, Configuration conf) {
		super(db, conf);
		innerDb = db;
	}

	public void setNumHashes(long numHashes) {
		this.numHashes = numHashes;
	}

	@Override
	public long numHashes() {
		return numHashes;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		// TODO Auto-generated method stub

	}

	@Override
	public long getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean checkDB(Database db) {
		return innerDb == db;
	}
}

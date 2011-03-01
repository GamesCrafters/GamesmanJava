package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;

public final class QuartoCache extends TierCache {
	private long numHashes = 0L;
	private final MemoryDatabase[] smallRanges;
	private final MemoryDatabase[] largeRanges;

	public QuartoCache(Database db, Configuration conf, int maxSmallRanges,
			int maxLargeRanges, long totalMem) {
		super(db, conf);
		smallRanges = new MemoryDatabase[maxSmallRanges];
		largeRanges = new MemoryDatabase[maxLargeRanges];
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
}

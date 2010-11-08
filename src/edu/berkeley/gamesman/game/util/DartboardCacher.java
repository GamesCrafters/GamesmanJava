package edu.berkeley.gamesman.game.util;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.RangeCache;
import edu.berkeley.gamesman.hasher.DartboardHasher;

public class DartboardCacher {
	private RangeCache myCache;
	private long availableMem;
	private long endPosition;
	private long childTierOffset;
	private int tier;
	private final DartboardHasher myHasher;
	private final Configuration conf;

	public DartboardCacher(Configuration conf, DartboardHasher myHasher) {
		this.conf = conf;
		this.myHasher = myHasher;
	}

	public RangeCache getCache(Database db, long numPositions,
			long availableMem, int tier, long childTierOffset) {
		this.endPosition = myHasher.getHash() + numPositions;
		this.availableMem = availableMem;
		myCache = new RangeCache(db, conf);
		this.tier = tier;
		this.childTierOffset = childTierOffset;
		return nextCache();
	}

	public RangeCache nextCache() {
		long[] firstChildren = new long[myHasher.size()];
		long[] lastChildren = new long[myHasher.size()];
		long curHash = myHasher.getHash();
		char turn = tier % 2 == 0 ? 'X' : 'O';
		myHasher.nextChildren(' ', turn, firstChildren);
		long numPositions = this.endPosition - curHash;
		while (true) {
			myHasher.unhash(curHash + numPositions - 1);
			myHasher.previousChildren(' ', turn, lastChildren);
			for (int i = 0; i < myHasher.size(); i++) {
				if (firstChildren[i] < 0 || lastChildren[i] < 0
						|| firstChildren[i] > lastChildren[i])
					lastChildren[i] = -1L;
			}
			if (RangeCache.memFor(myCache, firstChildren, lastChildren) <= availableMem)
				break;
			else
				numPositions /= 2;
		}
		myHasher.unhash(curHash);
		for (int i = 0; i < myHasher.size(); i++) {
			if (firstChildren[i] >= 0 && lastChildren[i] >= 0) {
				firstChildren[i] += childTierOffset;
				lastChildren[i] += childTierOffset;
			} else
				firstChildren[i] = lastChildren[i] = -1L;
		}
		myCache.setRanges(firstChildren, lastChildren, numPositions);
		return myCache;
	}
}
